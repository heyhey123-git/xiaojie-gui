package io.github.heyhey123.xiaojiegui.skript.utils

import ch.njol.skript.variables.Variables
import org.bukkit.event.Event

/**
 * A utility class to execute an action with a copied user context.
 * It's useful for maintaining local variables not only across different event contexts,
 * but also across different space-time.
 *
 * @param provider The event providing the user context, which has to exist: an event that is not there
 * holds no local variables to copy, and Skript's lookup rejects a null one rather than answering nothing.
 * @param action The action to execute with the provided user context.
 * The action receives the event as a parameter, which used as the user context.
 * @see Variables.withLocalVariables
 */
class LocalsScopeRunner(
    private val provider: Event,
    private val action: (Event) -> Unit
) {
    val context = Variables.copyLocalVariables(provider)

    /**
     * Execute the action with the provided user context.
     *
     * @param user The event user context to set local variables for.
     */
    fun execute(user: Event) {
        Variables.setLocalVariables(user, context)
        try {
            action(user)
        } finally {
            // `Variables.removeLocals` cannot be called from here directly: it returns Skript's
            // package-private `VariablesMap`, so Kotlin infers a type it is not allowed to name and warns
            // that this will become an error (KTLC-14). Discarding such a value is legal in Java, which is
            // what the bridge does, so the call stays the removal it looks like.
            SkriptLocalsBridge.removeLocals(user)
        }
    }

    /**
     * Invoke the executor with the provided user context.
     *
     * @param user The event user context to set local variables for.
     */
    operator fun invoke(user: Event) = execute(user)
}
