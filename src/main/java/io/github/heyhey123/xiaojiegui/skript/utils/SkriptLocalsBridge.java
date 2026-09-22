package io.github.heyhey123.xiaojiegui.skript.utils;

import ch.njol.skript.variables.Variables;
import org.bukkit.event.Event;

/**
 * The one Skript call Kotlin cannot make cleanly.
 * <p>
 * {@link Variables#removeLocals(Event)} returns Skript's own {@code VariablesMap}, a package-private
 * type. Java is allowed to discard a value whose type it cannot name, and this wrapper does exactly
 * that, so the Kotlin side sees a {@code void} method and has no type to infer. Kotlin has to infer it,
 * and 2.3 warns that the inferred type is not visible in that scope, with the promise of an error in a
 * future release (KTLC-14).
 * <p>
 * The call itself is still Skript's, rather than the {@code setLocalVariables(event, null)} spelling
 * that means the same thing: removing is what is meant here, and this way the intent does not rest on
 * the null branch of that method.
 */
public final class SkriptLocalsBridge {

    private SkriptLocalsBridge() {
    }

    /**
     * Removes the local variables an event holds.
     *
     * @param event the event whose local variables to remove.
     */
    public static void removeLocals(Event event) {
        Variables.removeLocals(event);
    }
}
