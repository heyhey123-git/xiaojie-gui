package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.inventory.InventoryType
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu Inventory Type")
@Description("The inventory type of a menu.")
@Examples(
    // The property takes the menu expression directly: `of menu {_menu}` parses `menu {_menu}` as a
    // lookup by id instead.
    "set {_type} to the inventory type of {_menu}",
    "send \"The inventory type is %{_type}%\" to player"
)
@Since("1.0.0")
class ExprInventoryType : SimplePropertyExpression<Menu, InventoryType>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
                ExprInventoryType::class.java,
                InventoryType::class.java,
                "inventory type",
                "menu"
            )
        }
    }

    override fun convert(from: Menu?): InventoryType? = from?.inventoryType

    override fun getPropertyName() = "inventory type"

    override fun getReturnType() = InventoryType::class.java
}
