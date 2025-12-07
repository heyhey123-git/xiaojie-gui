package io.github.heyhey123.xiaojiegui.skript.utils

import ch.njol.skript.lang.Expression
import io.github.heyhey123.xiaojiegui.skript.utils.TitleType.COMPONENT
import io.github.heyhey123.xiaojiegui.skript.utils.TitleType.STRING
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.Event
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
     * Try to extract a Component from an object, which can be either a String or a ComponentWrapper.
     *
     * @param obj the object to extract from
     * @return the extracted Component, or null if extraction failed
     */
    fun extractComponentOrNull(obj: Any): Component? =
        when {
            obj is String -> Component.text(obj)
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
     * Convert expressions to a Component based on the TitleType.
     *
     * @param strExpr the string expression
     * @param componentExpr the component expression
     * @param event the event context
     * @param type the TitleType
     * @return the resulting Component, or null if conversion failed
     */
    fun resolveTitleComponentOrNull(
        strExpr: Expression<String>?,
        componentExpr: Expression<Any>?,
        event: Event?,
        type: TitleType
    ): Component? =
        when (type) {
            STRING -> {
                val titleStr = strExpr?.getSingle(event)
                titleStr?.let { LegacyComponentSerializer.legacySection().deserialize(titleStr) }
            }

            COMPONENT -> {
                val titleObj = componentExpr?.getSingle(event)
                titleObj?.let { extractComponent(it) }
            }
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
        if (hasSkBee) arrayOf(skbeeComponentWrapper!!, String::class.java)
        else arrayOf(String::class.java)
}
