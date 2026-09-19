package io.github.heyhey123.xiaojiegui.skript.utils

import ch.njol.skript.variables.Variables
import org.bukkit.event.Event

/**
 * A utility class to execute an action with a copied user context.
 * It's useful for maintaining local variables not only across different event contexts,
 * but also across different space-time.
 *
 * @param provider The event providing the user context.
 * @param action The action to execute with the provided user context.
 * The action receives the event as a parameter, which used as the user context.
 * @see Variables.withLocalVariables
 */
class LocalsScopeRunner(
    private val provider: Event?,
    private val action: (Event?) -> Unit
) {
    val context = Variables.copyLocalVariables(provider)

    /**
     * Execute the action with the provided user context.
     *
     * @param user The event user context to set local variables for.
     */
    fun execute(user: Event?) {
        Variables.setLocalVariables(user, context)
        try {
            action(user)
        } finally {
            // Kept even though Kotlin 2.3 warns here (KTLC-14): `removeLocals` returns Skript's own
            // `VariablesMap`, a type this module cannot name, and the alternative -- `setLocalVariables(user,
            // null)` -- would leave an entry behind rather than remove the event's locals. The warning becomes
            // an error only if Kotlin makes the inference an error before Skript gives the method a void
            // return or a public type.
            Variables.removeLocals(user)
        }
    }

    /**
     * Invoke the executor with the provided user context.
     *
     * @param user The event user context to set local variables for.
     */
    operator fun invoke(user: Event?) = execute(user)
}
