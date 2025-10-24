package io.github.heyhey123.xiaojiegui.it.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.heyhey123.xiaojiegui.it.ItPlugin
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

object CommandsRegistry {

    /**
     * Registers all commands to the server.
     * Invoke it when the server is enabled.
     */
    fun register() {
        val root: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("guiit")

        val subcommands: Set<Subcommand> = setOf(
            CmdPhantomReceptacle,
            CmdStaticReceptacle,
            CmdMenu
        )
        subcommands.forEach { it.attach(root) }
        ItPlugin.instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(root.build())
        }
    }
}
