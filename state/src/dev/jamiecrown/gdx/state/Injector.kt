package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.app.AppStateNode
import dev.jamiecrown.gdx.state.app.StateReader
import dev.jamiecrown.gdx.state.app.StateWriter
import dev.jamiecrown.gdx.state.eventbus.EventBus
import java.lang.reflect.Field
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

/** Marks a field to be persisted by the injector/AppState system. */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Persist(val key: String = "")

/** Optional override for the persistence node id; default is the class' qualified name. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StateId(val value: String)

/** Called after construction and wiring. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PostConstruct

/** Called before shutdown persistence flush. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PreDestroy

/** A simple key for singleton registry. */
data class Key(val type: KClass<*>, val name: String?) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Key) return false
        if (type != other.type) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

}

/**
 * A minimal reflection-based injector/factory that unifies EventBus registration and AppState persistence.
 * - Creates objects using the best matching constructor for supplied args (by type & position).
 * - Auto-registers instances with EventBus.
 * - Scans fields annotated with @Persist and binds them to AppStateManager as a node. Values are loaded immediately.
 * - Provides property delegates for new(), provision(), inject(), lazyInject().
 */
object Injector {
    @Volatile private var configured = false
    @JvmStatic lateinit var state: AppStateManager
    @JvmStatic var eventBus: EventBus = EventBus

    private val singletons: MutableMap<Key, Any> = mutableMapOf()
    private val managedNodes: MutableMap<Any, AppStateNode> = mutableMapOf()

    /** Configure the injector with required collaborators. Call before usage. */
    fun configure(stateManager: AppStateManager, eventBus: EventBus = EventBus) {
        this.state = stateManager
        this.eventBus = eventBus
        if (!configured) {
            // Register a JVM shutdown hook to save all state
            Runtime.getRuntime().addShutdownHook(Thread {
                try { saveAll() } catch (_: Throwable) {}
            })
            configured = true
        }
    }

    /** Saves all managed nodes via AppStateManager. */
    fun saveAll() {
        state.saveAll()
        // Trigger @PreDestroy for managed instances
        managedNodes.keys.forEach { invokeAnnotated(it, PreDestroy::class) }
    }

    /**
     * Provision a singleton of [T] with optional name, creating it from args if not present.
     */
    fun <T: Any> provision(type: KClass<T>, name: String? = null, vararg args: Any?): T {
        val key = Key(type, name)
        val existing = singletons[key]
        if (existing != null) return existing as T
        val created = create(type, args.asList())
        singletons[key] = created
        return created
    }

    /** Retrieve an existing provided singleton. Optionally auto-create if possible. */
    fun <T: Any> get(type: KClass<T>, name: String? = null, autoCreate: Boolean = false): T {
        val key = Key(type, name)
        @Suppress("UNCHECKED_CAST")
        val existing = singletons[key] as T?
        if (existing != null) return existing
        require(autoCreate) { "No singleton provided for ${type.qualifiedName}${if (name!=null) " name='$name'" else ""}. Use provision() first or enable autoCreate." }
        return provision(type, name)
    }




     operator fun contains(key: Key): Boolean {
        return singletons.containsKey(key)
    }
    /** Create a new instance and wire it up. */
    fun <T: Any> create(type: KClass<T>, args: List<Any?> = emptyList()): T {
        val instance = construct(type, args)
        // Wire: EventBus + Persistence + lifecycle
        try {
            eventBus.register(instance)
        } catch (_: Throwable) {}
        setupPersistence(instance)
        invokeAnnotated(instance, PostConstruct::class)
        // Optional hook: auto-register CLI commands if CLI module is present
        try {
            val reg = Class.forName("dev.jamiecrown.gdx.cli.CommandRegistry")
            val m = reg.getMethod("registerFromInjector", Any::class.java)
            m.invoke(null, instance)
        } catch (_: Throwable) { }
        return instance
    }

    // region Persistence wiring
    private fun setupPersistence(instance: Any) {
        if (!::state.isInitialized) return
        val cls = instance::class
        val preferredId = (cls.annotations.filterIsInstance<StateId>().firstOrNull()?.value)
            ?: (cls.simpleName ?: cls.qualifiedName ?: instance.hashCode().toString())

        // If the instance itself is an AppStateNode, we do not auto-register a wrapper.
        // This avoids duplicate registrations and ensures the node's own id is used.
        if (instance is AppStateNode) {
            // Leave registration to the application code; do not auto-register here.
            // This prevents creating parallel files like Fully.Qualified.Name.properties.
            return
        }

        val fields = mutableListOf<Field>()
        var c: Class<*>? = instance.javaClass
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (f.getAnnotation(Persist::class.java) != null) {
                    f.isAccessible = true
                    fields += f
                }
            }
            c = c.superclass
        }
        if (fields.isEmpty()) return

        val node = object: AppStateNode {
            override val id: String = preferredId
            override fun writeState(writer: StateWriter) {
                for (f in fields) {
                    val key = f.getAnnotation(Persist::class.java)?.key?.takeIf { it.isNotBlank() } ?: f.name
                    val v = f.get(instance)
                    when (v) {
                        null -> {}
                        is String -> writer.put(key, v)
                        is Int -> writer.putInt(key, v)
                        is Float -> writer.putFloat(key, v)
                        is Boolean -> writer.putBoolean(key, v)
                        is Long, is Double, is Short, is Byte -> writer.put(key, v.toString())
                        is Enum<*> -> writer.put(key, v.name)
                        else -> writer.put(key, v.toString())
                    }
                }
            }

            override fun readState(reader: StateReader) {
                for (f in fields) {
                    val key = f.getAnnotation(Persist::class.java)?.key?.takeIf { it.isNotBlank() } ?: f.name
                    val type = f.type
                    val value: Any? = when {
                        type == String::class.java -> reader.get(key)
                        type == Int::class.javaPrimitiveType || type == Int::class.java -> reader.getInt(key)
                        type == Float::class.javaPrimitiveType || type == Float::class.java -> reader.getFloat(key)
                        type == Boolean::class.javaPrimitiveType || type == Boolean::class.java -> reader.getBoolean(key)
                        type == Long::class.javaPrimitiveType || type == Long::class.java -> reader.get(key)?.toLongOrNull()
                        type == Double::class.javaPrimitiveType || type == Double::class.java -> reader.get(key)?.toDoubleOrNull()
                        type == Short::class.javaPrimitiveType || type == Short::class.java -> reader.get(key)?.toShortOrNull()
                        type == Byte::class.javaPrimitiveType || type == Byte::class.java -> reader.get(key)?.toByteOrNull()
                        type.isEnum -> reader.get(key)?.let { n -> java.lang.Enum.valueOf(type as Class<out Enum<*>>, n) }
                        else -> reader.get(key)
                    }
                    if (value != null) f.set(instance, value)
                }
            }
        }
        // Register and load now (only this node will have any persisted values affecting this instance)
        try { state.unregister(node.id) } catch (_: Throwable) {}
        state.register(node)
        state.loadAll()
        managedNodes[instance] = node
    }
    // endregion

    // region Constructor Selection
    private fun <T: Any> construct(type: KClass<T>, args: List<Any?>): T {
        // Compute raw Java classes for provided arguments (boxed primitives where applicable)
        val argClasses: List<Class<*>?> = args.map { it?.let { v -> v::class.java } }
        val ctors = type.constructors.toList()
        val matches = mutableListOf<Pair<KFunction<T>, Int>>()
        for (ctor in ctors) {
            val params = ctor.parameters
            if (params.size != args.size) continue
            val score = matchScore(params, argClasses)
            if (score != null) matches += ctor to score
        }
        if (matches.isEmpty()) {
            throw IllegalArgumentException("No suitable constructor found for ${type.qualifiedName} with args ${args.map { it?.let { v -> v::class.qualifiedName } ?: "null" }}")
        }
        matches.sortBy { it.second }
        val best = matches.first().first
        return best.call(*args.toTypedArray())

    }

    private fun matchScore(params: List<KParameter>, argClasses: List<Class<*>?>): Int? {
        var score = 0
        for (i in params.indices) {
            val p = params[i]
            val t = p.type
            val targetClass = rawClassOf(t) ?: return null
            val aClass = argClasses[i]
            if (aClass == null) {
                // null only allowed if parameter is nullable and not a primitive type
                if (!t.isMarkedNullable || targetClass.isPrimitive) return null
                score += 5
                continue
            }
            val ok = isAssignable(targetClass, aClass)
            if (!ok) return null
            // Lower score is better; exact match gets 0, superclass gets higher
            score += distance(targetClass, aClass)
        }
        return score
    }

    private fun rawClassOf(type: KType): Class<*>? = (type.javaType as? Class<*>)

    private val primitiveToWrapper: Map<Class<*>, Class<*>> = mapOf(
        Integer.TYPE to Integer::class.java,
        java.lang.Float.TYPE to java.lang.Float::class.java,
        java.lang.Double.TYPE to java.lang.Double::class.java,
        java.lang.Long.TYPE to java.lang.Long::class.java,
        java.lang.Short.TYPE to java.lang.Short::class.java,
        java.lang.Byte.TYPE to java.lang.Byte::class.java,
        Character.TYPE to Character::class.java,
        java.lang.Boolean.TYPE to java.lang.Boolean::class.java
    )

    private val wrapperToPrimitive: Map<Class<*>, Class<*>> = primitiveToWrapper.entries.associate { it.value to it.key }

    private fun isAssignable(targetClass: Class<*>, actualClass: Class<*>): Boolean {
        if (targetClass.isAssignableFrom(actualClass)) return true
        // Handle primitive-wrapper equivalence in both directions
        val targetIsPrimitive = targetClass.isPrimitive
        val actualIsPrimitive = actualClass.isPrimitive
        if (targetIsPrimitive) {
            val wrapper = primitiveToWrapper[targetClass]
            if (wrapper != null && wrapper == actualClass) return true
        } else if (actualIsPrimitive) {
            val wrapper = primitiveToWrapper[actualClass]
            if (wrapper != null && targetClass == wrapper) return true
        } else {
            // target is wrapper, actual is wrapper: already handled by isAssignableFrom
            // target is wrapper, actual is primitive -> covered above
        }
        return false
    }

    private fun distance(targetClass: Class<*>, actualClass: Class<*>): Int {
        // Exact or primitive-wrapper match has distance 0
        if (targetClass == actualClass) return 0
        val targetIsPrimitive = targetClass.isPrimitive
        val actualIsPrimitive = actualClass.isPrimitive
        if (targetIsPrimitive) {
            val wrapper = primitiveToWrapper[targetClass]
            if (wrapper == actualClass) return 0
        }
        if (actualIsPrimitive) {
            val wrapper = primitiveToWrapper[actualClass]
            if (wrapper == targetClass) return 0
        }
        // Otherwise, walk up superclass chain to estimate distance
        var c: Class<*>? = actualClass
        var d = 0
        while (c != null && !targetClass.isAssignableFrom(c)) {
            c = c.superclass
            d += 1
        }
        return d
    }
    // endregion

    // region Lifecycle
    private fun invokeAnnotated(target: Any, annotation: KClass<out Annotation>) {
        try {
            var k: KClass<*>? = target::class
            while (k != null && k != Any::class) {
                for (m in k.declaredMemberFunctions) {
                    if (m.annotations.any { it.annotationClass == annotation } && m.parameters.size == 1) {
                        try { m.call(target) } catch (_: Throwable) {}
                    }
                }
                k = k.supertypes.firstOrNull()?.classifier as? KClass<*>
            }
        } catch (_: Throwable) {}
    }



        fun key(type: KClass<*>, name: String? = null) = Key(type, name)


    // endregion
}

// region Property Delegates
abstract class BaseDelegate<T: Any> : ReadOnlyProperty<Any?, T> {
    protected var cached: T? = null
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val v = cached
        if (v != null) return v
        val type = (property.returnType.classifier as? KClass<T>)
            ?: throw IllegalStateException("Cannot resolve property type for '${property.name}'")
        val created = resolve(type)
        cached = created
        return created
    }
    abstract fun <X: Any> resolve(type: KClass<X>): X
}

/** Create a new instance for this property using provided args, wiring event/state. */
class NewDelegate(private vararg val args: Any?) : BaseDelegate<Any>() {
    override fun <X: Any> resolve(type: KClass<X>): X = Injector.create(type, args.toList())
}

/** Provision a singleton and assign it to this property. */
class ProvisionDelegate(private val name: String? = null, private vararg val args: Any?) : BaseDelegate<Any>() {
    override fun <X: Any> resolve(type: KClass<X>): X = Injector.provision(type, name, *args)
}

/** Inject an already provided singleton and assign it immediately (autoCreate optional). */
class InjectDelegate(private val name: String? = null, private val autoCreate: Boolean = false) : BaseDelegate<Any>() {
    override fun <X: Any> resolve(type: KClass<X>): X = Injector.get(type, name, autoCreate)
}

/** Inject lazily on first access; functionally same as InjectDelegate but named for clarity. */
class LazyInjectDelegate(private val name: String? = null, private val autoCreate: Boolean = false) : BaseDelegate<Any>() {
    override fun <X: Any> resolve(type: KClass<X>): X = Injector.get(type, name, autoCreate)
}

// Top-level helpers for fluent DSL
fun <T: Any> new(vararg args: Any?): ReadOnlyProperty<Any?, T> = NewDelegate(*args) as ReadOnlyProperty<Any?, T>
fun <T: Any> provision(vararg args: Any?): ReadOnlyProperty<Any?, T> = ProvisionDelegate(null, *args) as ReadOnlyProperty<Any?, T>
fun <T: Any> provisionNamed(name: String, vararg args: Any?): ReadOnlyProperty<Any?, T> = ProvisionDelegate(name, *args) as ReadOnlyProperty<Any?, T>
fun <T: Any> inject(name: String? = null, autoCreate: Boolean = false): ReadOnlyProperty<Any?, T> = InjectDelegate(name, autoCreate) as ReadOnlyProperty<Any?, T>
fun <T: Any> lazyInject(name: String? = null, autoCreate: Boolean = false): ReadOnlyProperty<Any?, T> = LazyInjectDelegate(name, autoCreate) as ReadOnlyProperty<Any?, T>
// endregion
