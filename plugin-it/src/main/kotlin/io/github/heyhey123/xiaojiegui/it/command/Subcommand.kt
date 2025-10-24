package io.github.heyhey123.xiaojiegui.it.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack

interface Subcommand {
    /**
     * Attaches the subcommand to the command registry.
     *
     */
    fun attach(root: LiteralArgumentBuilder<CommandSourceStack>)
}
