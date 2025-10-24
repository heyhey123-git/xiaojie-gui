package io.github.heyhey123.xiaojiegui.it.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.heyhey123.xiaojiegui.gui.receptacle.PhantomReceptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewLayout
import io.github.heyhey123.xiaojiegui.it.command.CommandsRegistry.subcommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CmdPhantomReceptacle : Subcommand {
    override fun attach(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.subcommand("ptrec", null).apply {
            subcommand("openhidepi", ::`create & open a new receptacle and hide player inv`)
            subcommand("opennohidepi", ::`create & open a new receptacle and no hide player inv`)
            subcommand("title", ::`set a new title and render`)
            subcommand("refreshsingle", ::`set an element and refresh single slot`)
            subcommand("refreshmultiple", ::`do something and refresh contents`)
        }
    }

    private lateinit var receptacle: PhantomReceptacle

    private fun `create & open a new receptacle and hide player inv`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle = PhantomReceptacle(
            Component.text("Test Phantom Receptacle"),
            ViewLayout.Chest.GENERIC_9X3
        ).apply {
            setElement(3, ItemStack(Material.GOLDEN_APPLE))
            setElement(4, ItemStack(Material.DIAMOND_SWORD))
            setElement(5, ItemStack(Material.SHIELD))
            hidePlayerInventory = true
        }
        // use `executor` to ensure /execute works correctly
        val player = ctx.source.executor as? Player
        if (player == null) {
            ctx.source.sender.sendMessage("This command can only be executed by a player.")
            return 0
        }

        receptacle.open(player)
        ctx.source.sender.sendMessage("Opened a phantom receptacle.")
        return Command.SINGLE_SUCCESS
    }

    private fun `create & open a new receptacle and no hide player inv`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle = PhantomReceptacle(
            Component.text("Test Phantom Receptacle"),
            ViewLayout.Chest.GENERIC_9X3
        ).apply {
            setElement(3, ItemStack(Material.GOLDEN_APPLE))
            setElement(6, ItemStack(Material.DIAMOND_SWORD))
            setElement(17, ItemStack(Material.SHIELD))
        }
        // use `executor` to ensure /execute works correctly
        val player = ctx.source.executor as? Player
        if (player == null) {
            ctx.source.sender.sendMessage("This command can only be executed by a player.")
            return 0
        }

        receptacle.open(player)
        ctx.source.sender.sendMessage("Opened a phantom receptacle.")
        return Command.SINGLE_SUCCESS
    }

    private fun `set a new title and render`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle.title(Component.text("New Title!"), true)
        ctx.source.sender.sendMessage("Set a new title and rendered.")
        return Command.SINGLE_SUCCESS
    }

    private fun `set an element and refresh single slot`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle.apply {
            setElement(10, ItemStack(Material.BEACON))
            refresh(10)
        }
        ctx.source.sender.sendMessage("Set an element and refreshed single slot.")
        return Command.SINGLE_SUCCESS
    }

    private fun `do something and refresh contents`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle.apply {
            setElement(13, ItemStack(Material.EMERALD))
            setElement(4, ItemStack(Material.DIAMOND_SWORD))
            refresh()
        }
        ctx.source.sender.sendMessage("Did something and refreshed contents.")
        return Command.SINGLE_SUCCESS
    }

    private fun `close receptacle`(ctx: CommandContext<CommandSourceStack>): Int {
        receptacle.close(render = true)
        ctx.source.sender.sendMessage("Closed the phantom receptacle.")
        return Command.SINGLE_SUCCESS
    }
}
