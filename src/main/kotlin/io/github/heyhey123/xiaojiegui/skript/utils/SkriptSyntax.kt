package io.github.heyhey123.xiaojiegui.skript.utils

import ch.njol.skript.expressions.base.PropertyExpression
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptEvent
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.lang.converter.Converter
import org.skriptlang.skript.registration.DefaultSyntaxInfos
import org.skriptlang.skript.registration.SyntaxInfo
import org.skriptlang.skript.registration.SyntaxRegistry
import org.skriptlang.skript.util.Priority

/**
 * Registers this addon's syntax with Skript.
 *
 * `Skript.registerEffect`, `registerSection`, `registerExpression`, `registerCondition`,
 * `registerEvent` and `EventValues.registerEventValue` are all deprecated for removal. Their
 * replacements are the addon's own [SyntaxRegistry] and the server's [EventValueRegistry], which is
 * what these helpers use.
 *
 * Registering through the addon's registry also attributes the syntax to this addon, so the `@Name`,
 * `@Description`, `@Examples` and `@Since` annotations stay the documentation and are not restated
 * here.
 *
 * Each element keeps its own patterns and calls these from a `register` function of its own, which
 * [io.github.heyhey123.xiaojiegui.skript.registerElements] calls for every element while the plugin
 * enables: Skript reads the registry while it loads scripts, so registration cannot be spread out.
 */
internal object SkriptSyntax {

    fun section(addon: SkriptAddon, type: Class<out Section>, vararg patterns: String) {
        addon.syntaxRegistry().register(
            SyntaxRegistry.SECTION,
            SyntaxInfo.builder(type).addPatterns(*patterns).build()
        )
    }

    fun effect(addon: SkriptAddon, type: Class<out Effect>, vararg patterns: String) {
        addon.syntaxRegistry().register(
            SyntaxRegistry.EFFECT,
            SyntaxInfo.builder(type).addPatterns(*patterns).build()
        )
    }

    fun condition(addon: SkriptAddon, type: Class<out Condition>, vararg patterns: String) {
        addon.syntaxRegistry().register(
            SyntaxRegistry.CONDITION,
            SyntaxInfo.builder(type).addPatterns(*patterns).build()
        )
    }

    /**
     * Registers an expression.
     *
     * The priority is a parameter because the deprecated `registerExpression` took an `ExpressionType`
     * and every one of them carried its own, which decides which of two matching patterns Skript tries
     * first. [SyntaxInfo.SIMPLE] is what `ExpressionType.SIMPLE` meant and is the default; a combined
     * expression (one that reads `all X`) passes [SyntaxInfo.COMBINED], and one that is really a property
     * without using the property patterns passes [PropertyExpression.DEFAULT_PRIORITY]. The builder lives
     * on [DefaultSyntaxInfos], which declares the expression shape that [SyntaxRegistry.EXPRESSION] holds;
     * Kotlin does not reach it through [SyntaxInfo].
     */
    fun <E : Expression<R>, R> expression(
        addon: SkriptAddon,
        type: Class<E>,
        returnType: Class<R>,
        vararg patterns: String,
        priority: Priority = SyntaxInfo.SIMPLE
    ) {
        addon.syntaxRegistry().register(
            SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(type, returnType)
                .priority(priority)
                .addPatterns(*patterns)
                .build()
        )
    }

    /**
     * Registers a property expression along with the two patterns every property expression has:
     * `[the] <property> of %<from>%` and `%<from>%'[s] <property>`.
     *
     * The patterns and the priority come from [PropertyExpression] itself, so they are exactly the
     * ones the deprecated `register` shortcut produced.
     */
    fun <E : Expression<R>, R> property(
        addon: SkriptAddon,
        type: Class<E>,
        returnType: Class<R>,
        property: String,
        fromType: String
    ) {
        addon.syntaxRegistry().register(
            SyntaxRegistry.EXPRESSION,
            PropertyExpression.infoBuilder(type, returnType, property, fromType, false).build()
        )
    }

    /**
     * Registers a Skript event that listens to one Bukkit event.
     *
     * @param name The name Skript shows for the event; the `@Name` annotation says the same thing and
     * the two are expected to agree.
     * @param eventType The Bukkit event the Skript event listens to.
     */
    fun event(
        addon: SkriptAddon,
        type: Class<out SkriptEvent>,
        name: String,
        eventType: Class<out Event>,
        vararg patterns: String
    ) {
        addon.syntaxRegistry().register(
            BukkitSyntaxInfos.Event.KEY,
            BukkitSyntaxInfos.Event.builder(type, name)
                .addEvent(eventType)
                .addPatterns(*patterns)
                .build()
        )
    }

    /**
     * Publishes one event value, the replacement for `EventValues.registerEventValue`.
     *
     * `EventValue.simple` takes the same three arguments the deprecated method took: the event, the type
     * scripts see, and how to read it.
     *
     * The registry comes from the *addon*, not from `Skript.instance()`: Skript hands an addon a registry
     * it may write to and gives `Skript.instance()` an unmodifiable view of the same one (registering on
     * that view throws `Cannot register event values with an unmodifiable event value registry`). Only the
     * syntax registry gets the same treatment from either side.
     */
    fun <E : Event, V> eventValue(
        addon: SkriptAddon,
        eventType: Class<E>,
        valueType: Class<V>,
        getter: Converter<E, V>
    ) {
        addon.registry(EventValueRegistry::class.java)
            .register(EventValue.simple(eventType, valueType, getter))
    }
}
