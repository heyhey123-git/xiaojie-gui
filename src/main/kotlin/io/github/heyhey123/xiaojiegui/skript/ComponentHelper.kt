package io.github.heyhey123.xiaojiegui.skript

import ch.njol.skript.lang.Expression
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.Event

object ComponentHelper {

    /**
     * Extract a Component from an Expression.
     * Support String, TextComponent(in skbee), and other types that can be converted to String.
     *
     * @param expr the expression to extract from
     * @param event the event context
     * @return the extracted Component, or null if extraction fails
     */
    fun extractComponent(expr: Expression<Any>, event: Event?): Component? {
        when {
            expr.returnType == String::class.java -> {
                val str = expr.getSingle(event) as String?
                return str?.let { Component.text(it) }
            }

            expr.returnType.name == "com.shanebeestudios.skbee.api.wrapper.ComponentWrapper" -> {
                val wrapper = expr.getSingle(event)
                val field = wrapper?.javaClass?.getDeclaredField("component")
                field?.isAccessible = true
                return field?.get(wrapper) as Component?
            }

            else -> {
                val convertedExpr = expr.getConvertedExpression(String::class.java)
                val str = convertedExpr?.getSingle(event)
                return str?.let { Component.text(it) }
            }
        }
    }

    fun extractComponent(obj: Any): Component? {
        when {
            obj is String -> {
                val str = obj as String?
                return str?.let { Component.text(it) }
            }

            obj::class.java.name == "com.shanebeestudios.skbee.api.wrapper.ComponentWrapper" -> {
                return filedComponent!!.get(obj) as Component?
            }

            else -> {
                return Component.text(obj.toString())
            }
        }
    }

    /**
     * Wrap a Component into a suitable object for Skript.
     *
     * @param component the Component to wrap
     * @return the wrapped object, either a ComponentWrapper (if skbee is present) or a legacy string
     */
    fun wrapComponent(component: Component): Any =
        if (hasSkBee) methodFromComponent!!.invoke(null, component)
        else LegacyComponentSerializer.legacySection().serialize(component)

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

    /**
     * The type of object used to represent components in Skript.
     * Either ComponentWrapper (if skbee is present) or String.
     */
    val componentWrapperType: Class<*> = skbeeComponentWrapper ?: String::class.java

    /**
     * The "fromComponent" method in the ComponentWrapper class, or null if SkBee is not present.
     */
    private val methodFromComponent by lazy {
        val method = skbeeComponentWrapper?.getDeclaredMethod("fromComponent", Component::class.java)
        method?.isAccessible = true
        method
    }

    /**
     * The "component" field in the ComponentWrapper class, or null if SkBee is not present.
     */
    private val filedComponent by lazy {
        val field = skbeeComponentWrapper?.getDeclaredField("component")
        field?.isAccessible = true
        field
    }

    /**
     * Whether SKBee is present in the server.
     */
    val hasSkBee: Boolean = skbeeComponentWrapper != null
}
