package io.github.heyhey123.xiaojiegui.skript.elements.menu.sections

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
import ch.njol.skript.lang.util.SectionUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import org.bukkit.event.Event

@Name("Edit Menu")
@Description(
    "Edit an existing menu.",
    "You can define the menu's contents and behavior in the section below this effect."
)
@Examples(
    "edit menu with id \"main_menu\":",
    "    set slot 0 in page 0 of menu with id \"main_menu\" to diamond named \"New Item\""
)
@Since("1.0-SNAPSHOT")
class EffSecEditMenu : EffectSection() {

    companion object {
        init {
            Skript.registerSection(
                EffSecEditMenu::class.java,
                "(edit|change) [the] [(menu|gui)] %menu%"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var menuExpr: Expression<Menu>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?,
        sectionNode: SectionNode?,
        triggerItems: List<TriggerItem?>?
    ): Boolean {
        menuExpr = expressions?.get(0) as Expression<Menu>

        if (hasSection()) {
            val trigger = SectionUtils.loadLinkedCode(
                "create menu"
            ) { beforeLoading: Runnable?, afterLoading: Runnable? ->
                loadCode(
                    sectionNode,
                    "create menu",
                    beforeLoading,
                    afterLoading,
                    ProvideMenuEvent::class.java
                )
            }

            this.trigger = trigger ?: return false
        }

        return true
    }

    override fun walk(event: Event?): TriggerItem? {
        val menu = menuExpr.getSingle(event)
        if (menu == null) {
            Skript.error("Menu cannot be null in edit menu effect.")
            return walk(event, false)
        }

        if (trigger != null) {
            val menuProvider = ProvideMenuEvent(menu)
            Variables.withLocalVariables(event, menuProvider) {
                walk(trigger, menuProvider)
            }
        }

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "edit menu ${menuExpr.toString(event, debug)}"

}
