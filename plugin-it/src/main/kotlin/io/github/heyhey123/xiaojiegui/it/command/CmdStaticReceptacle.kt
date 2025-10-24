package io.github.heyhey123.xiaojiegui.it.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.heyhey123.xiaojiegui.gui.receptacle.StaticReceptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewLayout
import io.github.heyhey123.xiaojiegui.it.command.CommandsRegistry.subcommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CmdStaticReceptacle : Subcommand {
    override fun attach(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.subcommand("strec").apply {
            subcommand("open", ::`create & open a new receptacle`)
            subcommand("title", ::`set a new title and render`)
            subcommand("setitem", ::`set an element`)
            subcommand("clear", ::`clear and refresh contents`)
        }
    }

    private lateinit var receptacle: StaticReceptacle

    private fun `create & open a new receptacle`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle = StaticReceptacle(
            Component.text("Test Phantom Receptacle"),
            ViewLayout.Chest.GENERIC_9X3
        ).apply {
            setElement(3, ItemStack(Material.GOLDEN_APPLE))
            setElement(4, ItemStack(Material.DIAMOND_SWORD))
            setElement(5, ItemStack(Material.SHIELD))
        }
        // use `executor` to ensure /execute works correctly
        val player = ctx.source.executor as? Player
        if (player == null) {
            ctx.source.sender.sendMessage("This command can only be executed by a player.")
            return 0
        }

        receptacle.open(player)
        ctx.source.sender.sendMessage("Opened a static receptacle.")
        return Command.SINGLE_SUCCESS
    }

    private fun `set a new title and render`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle.title(Component.text("New Title!"), true)
        ctx.source.sender.sendMessage("Set a new title and rendered.")
        return Command.SINGLE_SUCCESS
    }

    private fun `set an element`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle.setElement(10, ItemStack(Material.BEACON))
        ctx.source.sender.sendMessage("Set an element and refreshed single slot.")
        return Command.SINGLE_SUCCESS
    }

    private fun `clear and refresh contents`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle.clear()
        ctx.source.sender.sendMessage("Cleared contents.")
        return Command.SINGLE_SUCCESS
    }
}
