package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu

@Name("Minimum Click Delay")
@Description("The minimum click delay (in ms) between two clicks in a menu.")
@Examples("set {_delay} to minimum click delay of menu with id \"main_menu\"")
@Since("1.0-SNAPSHOT")
class ExprMinClickDelay : SimplePropertyExpression<Menu, Number>() {

    companion object {
        init {
            register(
                ExprMinClickDelay::class.java,
                Number::class.java,
                "min(imum) click delay",
                "menu"
            )
        }
    }

    override fun convert(from: Menu?): Number? = from?.properties?.minClickDelay

    override fun getPropertyName() = "minimum click delay"

    override fun getReturnType() = Number::class.java
}
