package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.skriptlang.skript.addon.SkriptAddon

@Name("Minimum Click Delay")
@Description("The minimum click delay (in ms) between two clicks in a menu.")
@Examples(
    // The property takes the menu expression directly: after `of menu` the bare word `menu` would have
    // to be the expression, and no expression is named `menu`.
    "set {_delay} to the minimum click delay of {_menu}",
    "send \"The minimum click delay is %{_delay}% ms\" to player"
)
@Since("1.0.0")
class ExprMinClickDelay : SimplePropertyExpression<Menu, Number>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
                ExprMinClickDelay::class.java,
                Number::class.java,
                "min(imum) click delay",
                "menu"
            )
        }
    }

    override fun convert(from: Menu?): Number? = from?.properties?.minClickDelay

    override fun getPropertyName() = "minimum click delay"

    override fun getReturnType() = Number::class.java
}
