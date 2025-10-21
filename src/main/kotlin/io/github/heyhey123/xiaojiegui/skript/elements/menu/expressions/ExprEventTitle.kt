package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuOpenEvent
import io.github.heyhey123.xiaojiegui.gui.event.PageTurnEvent
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import org.bukkit.event.Event

class ExprEventTitle : SimpleExpression<Any>() {

    companion object {
        init {
            @Suppress("UNCHECKED_CAST")
            Skript.registerExpression(
                ExprEventTitle::class.java,
                ComponentHelper.componentWrapperType::class.java as Class<Any>,
                ExpressionType.SIMPLE,
                "[the] [event-]title"
            )
        }
    }

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean = parser.isCurrentEvent(MenuOpenEvent::class.java)

    override fun get(event: Event?): Array<Any> {
        if (event !is PageTurnEvent) return arrayOf()
        return arrayOf(ComponentHelper.wrapComponent(event.title))
    }

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?> =
        if (mode == Changer.ChangeMode.SET)
            arrayOf(ComponentHelper.componentWrapperType::class.java)
        else arrayOf()

    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        if (event !is PageTurnEvent) return
        if (mode != Changer.ChangeMode.SET) return
        val newTitle = delta?.get(0) ?: return
        val component = ComponentHelper.extractComponent(newTitle) ?: return
        event.title = component
    }


    override fun toString(event: Event?, debug: Boolean) =
        "the event-title"

    override fun isSingle() = true

    override fun getReturnType() = ComponentHelper.componentWrapperType::class.java
}
