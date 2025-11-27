package io.github.heyhey123.xiaojiegui.skript.elements.button.expressions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.skript.utils.Button
import org.bukkit.event.Event

@Name("Get All Buttons")
@Description(
    "Get all button IDs.",
    "This expression returns a list of all button IDs that have been created."
)
@Examples(
    "set {_buttons::*} to all buttons",
    "loop {_buttons::*}:",
    "    send \"Button ID: %loop-value%\" to player"
)
@Since("1.0.4")
class ExprAllButtons: SimpleExpression<String>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprAllButtons::class.java,
                String::class.java,
                ExpressionType.SIMPLE,
                "all buttons"
            )
        }
    }

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ) = true

    override fun get(event: Event?): Array<String> =
        Button.buttons.keys.toTypedArray()

    override fun toString(event: Event?, debug: Boolean) = "all buttons"

    override fun isSingle() = false

    override fun getReturnType() = String::class.java

}
