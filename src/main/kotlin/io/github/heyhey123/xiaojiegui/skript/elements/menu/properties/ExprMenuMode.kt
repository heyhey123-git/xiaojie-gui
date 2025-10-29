package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle

@Name("Menu Mode")
@Description("The mode of a menu, either `PHANTOM` or `STATIC`.")
@Examples(
    "set {_mode} to the mode of menu {_menu}",
    "if the mode of menu {_menu} is PHANTOM:",
    "\tsend \"This menu is in phantom mode!\" to player"
)
@Since("1.0-SNAPSHOT")
class ExprMenuMode : SimplePropertyExpression<Menu, Receptacle.Mode>() {
    companion object {
        init {
            register(
                ExprMenuMode::class.java,
                Receptacle.Mode::class.java,
                "mode",
                "menu"
            )
        }
    }

    override fun convert(e: Menu?): Receptacle.Mode? = e?.properties?.mode

    override fun getReturnType() = Receptacle.Mode::class.java

    override fun getPropertyName() = "mode"
}
