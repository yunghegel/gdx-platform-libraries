package dev.jamiecrown.gdx.state.eventbus

import java.lang.reflect.Method

/**
 * Annotation placed on methods that should receive events posted to the EventBus.
 * Methods must have exactly one parameter which is the event type.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe

/**
 * Simple reflection-based EventBus.
 *
 * - Register any object; all of its @Subscribe methods will be called when a matching event is posted.
 * - Also supports named events via @Listen("name") and post(name, data).
 * - Unregister to stop receiving events.
 * - Posting walks the class hierarchy of the event to deliver to subscribers of supertypes as well.
 */
object EventBus {
    private data class Subscriber(val target: Any, val method: Method, val eventType: Class<*>)
    private data class NamedSubscriber(val target: Any, val method: Method, val name: String, val paramType: Class<*>?)

    // Map event raw class to list of subscribers
    private val subscribersByEvent: MutableMap<Class<*>, MutableList<Subscriber>> = mutableMapOf()

    // Map event name to list of named subscribers
    private val subscribersByName: MutableMap<String, MutableList<NamedSubscriber>> = mutableMapOf()

    // Cache of scanned listener classes
    private data class ScanResult(val typed: List<Subscriber>, val named: List<NamedSubscriber>)
    private val cache: MutableMap<Class<*>, ScanResult> = mutableMapOf()

    @Synchronized
    fun register(listener: Any) {
        val cls = listener.javaClass
        val scanned = cache.getOrPut(cls) { scan(listener) }
        for (sub in scanned.typed) {
            subscribersByEvent.getOrPut(sub.eventType) { mutableListOf() }.add(sub)
        }
        for (ns in scanned.named) {
            subscribersByName.getOrPut(ns.name) { mutableListOf() }.add(ns)
        }
    }

    @Synchronized
    fun unregister(listener: Any) {
        subscribersByEvent.values.forEach { list -> list.removeIf { it.target === listener } }
        subscribersByName.values.forEach { list -> list.removeIf { it.target === listener } }
    }

    fun post(event: Any) {
        // Traverse event class hierarchy, delivering to subscribers of each supertype
        var c: Class<*>? = event.javaClass
        val visited = HashSet<Class<*>>()
        while (c != null && visited.add(c)) {
            val list = synchronized(this) { subscribersByEvent[c]?.toList() ?: emptyList() }
            for (sub in list) {
                try {
                    sub.method.invoke(sub.target, event)
                } catch (t: Throwable) {
                    // Swallow to keep EventBus robust; callers can observe via logs.
                    System.err.println("[EventBus] Error invoking subscriber ${sub.target.javaClass.name}#${sub.method.name} for event ${event.javaClass.name}: ${t.message}")
                }
            }
            c = c.superclass
        }
    }

    fun post(name: String, data: Any? = null) {
        val list = synchronized(this) { subscribersByName[name]?.toList() ?: emptyList() }
        for (sub in list) {
            try {
                val pt = sub.paramType
                if (pt == null) {
                    sub.method.invoke(sub.target)
                } else if (data == null) {
                    if (!pt.isPrimitive) sub.method.invoke(sub.target, null) else Unit
                } else if (pt.isAssignableFrom(data.javaClass)) {
                    sub.method.invoke(sub.target, data)
                }
            } catch (t: Throwable) {
                System.err.println("[EventBus] Error invoking named subscriber ${sub.target.javaClass.name}#${sub.method.name} for '$name': ${t.message}")
            }
        }
    }

    private fun scan(target: Any): ScanResult {
        val typed = mutableListOf<Subscriber>()
        val named = mutableListOf<NamedSubscriber>()
        var c: Class<*>? = target.javaClass
        while (c != null && c != Any::class.java) {
            for (m in c.declaredMethods) {
                if (m.getAnnotation(Subscribe::class.java) != null) {
                    val params = m.parameterTypes
                    if (params.size == 1) {
                        m.isAccessible = true
                        typed += Subscriber(target, m, params[0])
                    }
                }
                val listen = m.getAnnotation(Listen::class.java)
                if (listen != null) {
                    val params = m.parameterTypes
                    if (params.size == 0) {
                        m.isAccessible = true
                        named += NamedSubscriber(target, m, listen.value, null)
                    } else if (params.size == 1) {
                        // Only allow reference types for safety
                        if (!params[0].isPrimitive) {
                            m.isAccessible = true
                            named += NamedSubscriber(target, m, listen.value, params[0])
                        }
                    }
                }
            }
            c = c.superclass
        }
        return ScanResult(typed, named)
    }
}
