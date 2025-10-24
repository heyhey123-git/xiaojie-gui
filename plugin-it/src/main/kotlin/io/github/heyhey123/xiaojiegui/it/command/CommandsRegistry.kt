package io.github.heyhey123.xiaojiegui.it.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.heyhey123.xiaojiegui.it.ItPlugin
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

object CommandsRegistry {
    val root: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("guiit")

    /**
     * Adds a subcommand to the given command builder.
     *
     * @param name The name of the subcommand.
     * @param logic The logic to execute when the subcommand is invoked.
     * @return The newly created subcommand builder.
     */
    fun LiteralArgumentBuilder<CommandSourceStack>.subcommand(
        name: String,
        logic: ((CommandContext<CommandSourceStack>) -> Int)? = null
    ): LiteralArgumentBuilder<CommandSourceStack> {
        val sub = Commands.literal(name).executes(logic)
        this.then(sub)
        if (logic != null) {
            sub.executes(logic)
        }
        return sub
    }

    /**
     * Registers all commands to the server.
     * Invoke it when the server is enabled.
     */
    fun register() {
        ItPlugin.instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(root.build())
        }
    }

    init {
        val subcommands: Set<Subcommand> = setOf(
            CmdPhantomReceptacle,
            CmdStaticReceptacle,
            CmdMenu
        )
        subcommands.forEach { it.attach(root) }
    }
}
