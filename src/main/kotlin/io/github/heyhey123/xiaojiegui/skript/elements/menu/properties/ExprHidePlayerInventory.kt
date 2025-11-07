package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event

@Name("Hide Player Inventory")
@Description("Whether to hide the player's inventory when the menu is opened.")
@Examples(
    "set hide player inventory of menu {_menu} to true",
    "set hide player inventory of menu {_menu} to false"
)
@Since("1.0-SNAPSHOT")
class ExprHidePlayerInventory : SimplePropertyExpression<Menu, Boolean>() {

    companion object {
        init {
            register(
                ExprHidePlayerInventory::class.java,
                Boolean::class.java,
                "hide player inventory",
                "menu"
            )
        }
    }

    override fun convert(from: Menu?): Boolean? = from?.properties?.hidePlayerInventory

    override fun getPropertyName() = "hide player inventory"

    override fun getReturnType() = Boolean::class.java

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?>? =
        if (mode == Changer.ChangeMode.SET) arrayOf(Boolean::class.java)
        else emptyArray()

    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        if (mode != Changer.ChangeMode.SET) return
        val menu = expr.getSingle(event) ?: return
        val hidePlayerInventoryInput = delta?.firstOrNull()
        if (hidePlayerInventoryInput == null) {
            Skript.error("Value for hide player inventory cannot be null")
        }
        if (hidePlayerInventoryInput !is Boolean) {
            Skript.error("A boolean value is required for hide player inventory")
            return
        }
        menu.properties.hidePlayerInventory = hidePlayerInventoryInput
    }

}
