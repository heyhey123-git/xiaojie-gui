package io.github.heyhey123.xiaojiegui.skript.elements.menu.property

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.inventory.InventoryType

@Name("Menu Inventory Type")
@Description("The inventory type of a menu.")
@Examples("set {_type} to inventory type of menu with id \"main_menu\"")
@Since("1.0-SNAPSHOT")
class ExprInventoryType : SimplePropertyExpression<Menu, InventoryType>() {

    companion object {
        init {
            register(
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
