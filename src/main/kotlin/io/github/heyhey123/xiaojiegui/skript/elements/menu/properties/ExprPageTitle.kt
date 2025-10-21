package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import org.bukkit.event.Event


@Name("Page Title")
@Description("Get or set the title of a specific page in a menu.")
@Examples(
    "set the title of page 2 in menu {_menu} to \"New Title\"",
    "send \"The title of page 1 is %the title of page 1 in menu {_menu}%\" to player"
)
@Since("1.0-SNAPSHOT")
class ExprPageTitle : SimpleExpression<Any?>() {

    companion object {
        init {
            @Suppress("UNCHECKED_CAST")
            Skript.registerExpression(
                ExprPageTitle::class.java,
                ComponentHelper.componentWrapperType as Class<Any>,
                ExpressionType.COMBINED,
                "[the] title of page [(number|index)] %number% in [(menu|gui)] %menu%"
            )
        }
    }

    private lateinit var page: Expression<Number>

    private lateinit var menu: Expression<Menu>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        page = expressions?.get(0) as Expression<Number>
        menu = expressions[1] as Expression<Menu>

        return true
    }

    override fun get(event: Event?): Array<out Any?>? {
        val menu = menu.getSingle(event) ?: return null
        val page = page.getAll(event)
        return page.map { singlePage ->
            val title = menu.pages[singlePage.toInt()].title
            ComponentHelper.wrapComponent(title)
        }.toTypedArray()
    }

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?>? =
        if (mode == Changer.ChangeMode.SET) arrayOf(ComponentHelper.componentWrapperType::class.java)
        else null

    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        if (mode != Changer.ChangeMode.SET) return
        val menu = menu.getSingle(event) ?: return
        val pages = page.getAll(event)
        val component = delta?.get(0) ?: return
        val wrapped = ComponentHelper.extractComponent(component, event) ?: return
        for (singlePage in pages) {
            val singlePage = singlePage.toInt()
            if (singlePage !in 0..<menu.size) continue
            menu.pages[singlePage].title = wrapped
        }
    }

    override fun toString(event: Event?, debug: Boolean) =
        "the title of page [(number|index)] $page in [(menu|gui)] $menu"

    override fun isSingle() = false

    override fun getReturnType() = ComponentHelper.componentWrapperType
}
