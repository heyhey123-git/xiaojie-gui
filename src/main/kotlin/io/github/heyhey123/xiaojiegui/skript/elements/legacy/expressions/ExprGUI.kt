package io.github.heyhey123.xiaojiegui.skript.elements.legacy.expressions

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player


@Name("GUI of Player")
@Description("The GUI that the player currently has open.")
@Examples("edit the player's gui:", "\tmake gui 1 with dirt named \"Edited Slot\"")
@Since("1.0.0-SNAPSHOT")
class ExprGUI : SimplePropertyExpression<Player?, Menu?>() {
    override fun convert(player: Player?): Menu? =
        player?.let { MenuSession.querySession(it)?.menu }

    override fun getReturnType() = Menu::class.java

    override fun getPropertyName() = "gui"

    companion object {
        init {
            register<Menu?>(ExprGUI::class.java, Menu::class.java, "gui", "players")
        }
    }
}
