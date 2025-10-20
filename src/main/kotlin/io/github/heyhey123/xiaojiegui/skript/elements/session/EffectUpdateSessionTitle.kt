package io.github.heyhey123.xiaojiegui.skript.elements.session

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import org.bukkit.event.Event


@Name("Update Menu Session Title")
@Description(
    "Update the title of a menu session.",
    "If 'and refresh' is included, the inventory view will be refreshed to reflect the new title immediately."
)
@Examples(
    "update title of menu session of player to \"New Title\" and refresh",
    "update title of {_session} to \"Another Title\""
)
@Since("1.0-SNAPSHOT")
class EffectUpdateSessionTitle : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffectUpdateSessionTitle::class.java,
                "update title of %menusession% to %object% [and refresh]"
            )
        }
    }

    private lateinit var session: Expression<MenuSession>

    private lateinit var title: Expression<Any>

    private var refresh: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        session = expressions?.get(0) as Expression<MenuSession>
        title = expressions[1] as Expression<Any>
        refresh = parseResult?.hasTag("and refresh") ?: false

        return true
    }

    override fun execute(event: Event?) {
        val session = session.getSingle(event) ?: return
        val title = title.getSingle(event) ?: return

        val component = ComponentHelper.extractComponent(title, event) ?: return
        session.title(component, refresh)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "update title of ${session.toString(event, debug)} to ${
            title.toString(
                event,
                debug
            )
        }${if (refresh) " and refresh" else ""}"
}
