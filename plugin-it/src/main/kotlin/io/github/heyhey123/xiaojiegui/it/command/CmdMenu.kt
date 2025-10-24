package io.github.heyhey123.xiaojiegui.it.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.heyhey123.xiaojiegui.it.command.CommandsRegistry.subcommand
import io.papermc.paper.command.brigadier.CommandSourceStack

object CmdMenu : Subcommand {
    override fun attach(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.subcommand("menu", null).apply {
//            subcommand("open", ::openMenu)
        }
    }

//    fun openMenu(ctx: CommandContext<CommandSourceStack>): Int {
//
//    }
}
