package dev.jamiecrown.gdx.ui.editor

/**
 * Marks a property/field to be included in reflection-based tools.
 *
 * Usage:
 * - UI: ReflectionEditor inspects fields/properties annotated with @Keep and builds editors for many common types
 *   including primitives, strings, enums, Color, and LibGDX math types.
 * - State: May be used by persistence utilities to indicate fields intended for persistence.
 *
 * Apply to var/val or backing field. The editor will generate an appropriate control to edit the value at runtime.
 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Keep
