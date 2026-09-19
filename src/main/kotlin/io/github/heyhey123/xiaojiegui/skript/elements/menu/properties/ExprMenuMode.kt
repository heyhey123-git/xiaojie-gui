package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu Mode")
@Description(
    "The mode of a menu as text, either `phantom` or `static`.",
    "These are the same two words the `mode:` entry of `build a menu` takes, so a menu can be checked",
    "with a plain text comparison."
)
@Examples(
    "set {_mode} to the mode of {_menu}",
    "if the mode of {_menu} is \"phantom\":",
    "\tsend \"This menu is in phantom mode!\" to player"
)
@Since("1.0.0")
class ExprMenuMode : SimplePropertyExpression<Menu, String>() {
    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
                ExprMenuMode::class.java,
                String::class.java,
                "mode",
                "menu"
            )
        }
    }

    override fun convert(e: Menu?): String? = e?.properties?.mode?.id

    override fun getReturnType() = String::class.java

    override fun getPropertyName() = "mode"
}
