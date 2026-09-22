package io.github.heyhey123.xiaojiegui.skript.utils

import ch.njol.skript.lang.TriggerItem
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import org.bukkit.event.Event

object MenuCallbackUtils {

    /**
     * Builds a click handler function for a menu button or trigger.
     *
     * @param button The button whose callback to invoke.
     * @param trigger The trigger item to run if button is null.
     * @param sourceEvent The source event to pass to the trigger.
     * @param runTrigger Function to run the trigger with the event.
     * @param onError Function to handle any errors that occur.
     * @return A function that handles menu interaction events, or null if both button and trigger are null.
     */
    fun buildClickHandler(
        button: Button?,
        trigger: TriggerItem?,
        sourceEvent: Event?,
        runTrigger: (TriggerItem?, Event?) -> Unit,
        onError: (Throwable) -> Unit
    ): ((MenuInteractEvent) -> Unit)? {
        if (button == null && trigger == null) return null

        val executor: LocalsScopeRunner? =
            if (button == null) {
                // The callback runs later, in the menu's own event, and reads the local variables of the
                // event it was registered in. There are none to copy when that event is missing, and
                // Skript holds its locals in a map that rejects a null key, so it is refused here with a
                // sentence instead of failing inside Skript.
                val provider = checkNotNull(sourceEvent) {
                    "A click callback was registered without an event, so it has no local variables to copy."
                }
                LocalsScopeRunner(provider) { menuEvent ->
                    runTrigger(trigger, menuEvent)
                }
            } else {
                null
            }

        return { menuEvent ->
            try {
                if (button != null) {
                    button.callback.invoke(menuEvent)
                } else {
                    executor!!.invoke(menuEvent)
                }
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }
}
