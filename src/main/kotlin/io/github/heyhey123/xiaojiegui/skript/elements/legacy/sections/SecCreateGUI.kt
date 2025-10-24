package io.github.heyhey123.xiaojiegui.skript.elements.legacy.sections

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.EffectSection
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import net.kyori.adventure.text.Component
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

@Name("Create / Edit GUI")
@Description("The base of creating and editing GUIs.")
@Examples(
    "create a gui with virtual chest inventory with 3 rows named \"My GUI\"",
    "edit gui last gui:",
    "\tset the gui-inventory-name to \"New GUI Name!\""
)
@Since("1.0.0")
class SecCreateGUI : EffectSection() {
    private var inception = false

    private var inv: Expression<Inventory?>? = null

    private var shape: Expression<String?>? = null

    private var id: Expression<String?>? = null

    private var removableItems = false

    private var gui: Expression<Menu?>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        exprs: Array<Expression<*>?>,
        matchedPattern: Int,
        kleenean: Kleenean?,
        parseResult: SkriptParser.ParseResult,
        sectionNode: SectionNode?,
        triggerItems: MutableList<TriggerItem?>?
    ): Boolean {
        if (matchedPattern == 1) {
            if (!hasSection()) {
                Skript.error("You can't edit a gui inventory using an empty section, you need to change at least a slot or a property.")
                return false
            }
            gui = exprs[0] as Expression<Menu?>?
        } else {
            id = exprs[0] as Expression<String?>?
            inv = exprs[1] as Expression<Inventory?>
            shape = exprs[2] as Expression<String?>?
            removableItems = parseResult.hasTag("removable")
        }

        inception = parser.isCurrentSection(SecCreateGUI::class.java)

        if (hasSection()) {
            checkNotNull(sectionNode)
            loadOptionalCode(sectionNode)
        }

        return true
    }

    public override fun walk(e: Event?): TriggerItem? {
        if (this.gui == null) { // Creating a new GUI.
            val inv = this.inv!!.getSingle(e) ?: // Don't run the section if the GUI can't be created
            return walk(e, false)

            val invType = inv.type
            if (invType == InventoryType.CRAFTING || invType == InventoryType.PLAYER) { // We don't want to run this section as this is an invalid GUI type
                Skript.error("Unable to create an inventory of type: " + invType.name)
                return walk(e, false)
            }

            val invContents = inv.contents
            val id = this.id?.getSingle(e)
            val shape = this.shape?.getAll(e)
            if (shape.isNullOrEmpty()) {
                Skript.error(
                    "Shape cannot be empty if provided."
                )
                return walk(e, false)
            }
            val mode = if (removableItems) Receptacle.Mode.STATIC else Receptacle.Mode.PHANTOM
            val properties = MenuProperties(
                Component.text(id ?: "gui_${System.currentTimeMillis()}"),
                true,
                mode,
                0,
                0,
                shape.filterNotNull()
            )
            val gui = Menu(id, properties, inventoryType = invType)
            gui.insertPage(
                page = null,
                layoutPattern = null,
                title = null,
                playerInventoryPattern = null
            )
            for ((index, item) in invContents.withIndex()) {
                if (item == null) continue
                gui.pages[0].slotOverrides[index] = item
            }

        }


        if (!hasSection()) { // No section to run, we can skip the code below (no code to run with "new" gui)
            return walk(e, false)
        }

        assert(first != null && last != null) //？
        val lastNext = last!!.next
        last!!.setNext(null)
        walk(first, e)
        last!!.setNext(lastNext)


        // Don't run the section, we ran it above if needed
        return walk(e, false)
    }

    override fun toString(e: Event?, debug: Boolean): String {
        val creation = StringBuilder("create a gui")
        if (id != null) {
            creation.append(" with id ").append(id!!.toString(e, debug))
        }
        creation.append(" with ").append(inv!!.toString(e, debug))
        if (removableItems) {
            creation.append(" with removable items")
        }
        if (shape != null) {
            creation.append(" and shape ").append(shape!!.toString(e, debug))
        }
        return creation.toString()
    }

    companion object {
        init {
            Skript.registerSection<SecCreateGUI?>(
                SecCreateGUI::class.java,
                "create [a] [new] gui [[with id[entifier]] %-string%] with %inventory% [removable:(and|with) ([re]move[e]able|stealable) items] [(and|with) shape %-strings%]"
            )
        }
    }
}
