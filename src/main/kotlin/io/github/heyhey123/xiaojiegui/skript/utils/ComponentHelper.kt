package io.github.heyhey123.xiaojiegui.skript.utils

import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.UnparsedLiteral
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.Event
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

object ComponentHelper {
    /**
     * Extract a Component from a ComponentWrapper(in skbee).
     *
     * @param obj the ComponentWrapper to extract from
     * @return the extracted Component, or null if extraction failed
     */
    fun extractComponent(obj: Any): Component =
        fieldComponentGetter!!.invoke(obj) as Component

    /**
     * Try to extract a Component from an object, which can be a String, a plain Component or the
     * ComponentWrapper SkBee registers.
     *
     * A title that names a component is declared as `%-object%` rather than SkBee's `textcomponent`
     * type on purpose: a pattern that names an unregistered type fails to compile as a whole, so
     * `textcomponent` would take the `string:` alternative down with it on any server without SkBee.
     * Which object arrived is decided here instead.
     *
     * @param obj the object to extract from
     * @return the extracted Component, or null if extraction failed
     */
    fun extractComponentOrNull(obj: Any): Component? =
        when {
            // A title written as text is legacy text: `&` and `§` codes are formatting rather than
            // characters that end up on screen. A title travels to the client as a component, so it
            // carries colour; the translation is the one Spigot applies to item names.
            obj is String -> LegacyComponentSerializer.legacySection()
                .deserialize(translateAmpersandColorCodes(obj))
            obj is Component -> obj
            skbeeComponentWrapper?.isInstance(obj) == true -> extractComponent(obj)
            else -> null
        }

    /**
     * Wrap a Component into a suitable object for Skript.
     *
     * @param component the Component to wrap
     * @return the wrapped object, either a ComponentWrapper (if skbee is present)
     */
    fun wrapComponent(component: Component): Any =
        methodFromComponent!!.invoke(component)

    /**
     * Try to wrap a Component into a suitable object for Skript, or serialize it to a legacy string if skbee is not present.
     *
     * @param component the Component to wrap
     * @return the wrapped object, either a ComponentWrapper (if skbee is present) or a legacy string
     */
    fun wrapComponentOrString(component: Component): Any =
        when {
            hasSkBee -> wrapComponent(component)
            else -> LegacyComponentSerializer.legacySection().serialize(component)
        }

    /**
     * Read a title from an expression and turn it into a Component: a String is read as legacy text, a
     * Component is taken as it is, and SkBee's ComponentWrapper is unwrapped.
     *
     * The expression is converted before it is read, because Skript may hand a literal over unparsed —
     * reading one with `getSingle` first throws "UnparsedLiterals must be converted before use" at
     * runtime.
     *
     * @param titleExpr the expression holding the title, null when the syntax has no title
     * @param event the event context
     * @param reporter the element reporting the problem, whose runtime channel carries the message
     * @return the title as a Component, or null if there is no title or its type is unknown
     */
    fun resolveTitleComponentOrNull(
        titleExpr: Expression<Any>?,
        event: Event?,
        reporter: RuntimeErrorProducer
    ): Component? {
        if (titleExpr == null) {
            return null
        }

        val value = titleExpr.getConvertedExpression(Any::class.java)?.getSingle(event)
        // Older documentation told users to write `string:"Title"` or `component:"Title"`. Those prefixes
        // are parse tags inside the registered patterns, not text a script writes, so what is left over
        // reaches this slot as an UnparsedLiteral that cannot be read. Without this guard the title is
        // simply dropped, which is the worst kind of failure: it looks like it worked. Say what is wrong
        // through the caller's own runtime channel, so the message is framed with the syntax that ran
        // rather than the bare trigger line, and report no title so the caller stops instead of using
        // something else.
        val text = value as? String ?: (titleExpr as? UnparsedLiteral)?.data
        if (text != null && (text.startsWith("string:") || text.startsWith("component:"))) {
            reporter.error(
                "A title cannot start with `string:` or `component:`: those are parse tags in the syntax, " +
                    "not something a script writes. Write the title on its own, e.g. `to \"My Title\"`."
            )
            return null
        }

        return value?.let { extractComponentOrNull(it) }
    }

    /**
     * The SKBee ComponentWrapper class, or null if SKBee is not present.
     */
    val skbeeComponentWrapper: Class<*>? = run {
        try {
            return@run Class.forName("com.shanebeestudios.skbee.api.wrapper.ComponentWrapper")
        } catch (_: Throwable) {
            return@run null
        }
    }

    private val lookup = skbeeComponentWrapper?.let { clazz ->
        MethodHandles.privateLookupIn(clazz, MethodHandles.lookup())
    }

    /**
     * The "fromComponent" method in the ComponentWrapper class, or null if SkBee is not present.
     */
    private val methodFromComponent by lazy {
        if (skbeeComponentWrapper == null) return@lazy null
        val handle = lookup!!.findStatic(
            skbeeComponentWrapper,
            "fromComponent",
            MethodType.methodType(skbeeComponentWrapper, Component::class.java)
        )
        return@lazy handle
    }

    /**
     * The "component" field in the ComponentWrapper class, or null if SkBee is not present.
     */
    private val fieldComponentGetter by lazy {
        if (skbeeComponentWrapper == null) return@lazy null
        val handle = lookup!!.findGetter(
            skbeeComponentWrapper,
            "component",
            Component::class.java
        )
        return@lazy handle
    }

    /**
     * Whether SKBee is present in the server.
     */
    val hasSkBee: Boolean = skbeeComponentWrapper != null

    /**
     * The appropriate return type for title expressions, either ComponentWrapper (if skbee is present) or String.
     */
    val titleReturnType = if (hasSkBee) skbeeComponentWrapper!! else String::class.java

    /**
     * The appropriate return types for title expressions, either `ComponentWrapper` (if skbee is present) or `ComponentWrapper` and [String].
     */
    val titleReturnTypes: Array<Class<*>> =
        if (hasSkBee) {
            arrayOf(skbeeComponentWrapper!!, String::class.java)
        } else {
            arrayOf(String::class.java)
        }
}
