package io.github.heyhey123.xiaojiegui.it.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.heyhey123.xiaojiegui.gui.receptacle.PhantomReceptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewLayout
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CmdPhantomReceptacle : Subcommand {
    override fun attach(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.then(
            Commands.literal("ptrec")
                .then(Commands.literal("openhidepi").executes(::`create & open a new receptacle and hide player inv`))
                .then(
                    Commands.literal("opennohidepi").executes(::`create & open a new receptacle and no hide player inv`)
                )
//                .then(
//                    Commands.literal("openinterruptclick")
//                        .executes(::`create & open a new receptacle and interrupt all click`)
//                )
                .then(Commands.literal("title").executes(::`set a new title and render`))
                .then(Commands.literal("refreshsingle").executes(::`set an element and refresh single slot`))
                .then(Commands.literal("refreshmultiple").executes(::`do something and refresh contents`))
                .then(Commands.literal("close").executes(::`close receptacle`))
        )
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

//    private fun `create & open a new receptacle and interrupt all click`(ctx: CommandContext<CommandSourceStack>): Int {
//        receptacle = PhantomReceptacle(
//            Component.text("Test Phantom Receptacle"),
//            ViewLayout.Chest.GENERIC_9X3
//        ).apply {
//            setElement(10, ItemStack(Material.GOLDEN_APPLE))
//            setElement(15, ItemStack(Material.DIAMOND_SWORD))
//            setElement(25, ItemStack(Material.SHIELD))
//
//        }
//        receptacle.onClick { e ->
//            if (e.clickType.isItemMoveable()) {
//                receptacle.refresh()
//            } else {
//                receptacle.refresh(e.slot)
//            }
//        }
//        // use `executor` to ensure /execute works correctly
//        val player = ctx.source.executor as? Player
//        if (player == null) {
//            ctx.source.sender.sendMessage("This command can only be executed by a player.")
//            return 0
//        }
//
//        receptacle.open(player)
//        ctx.source.sender.sendMessage("Opened a phantom receptacle.")
//        return Command.SINGLE_SUCCESS
//    }
    // occurred an unexplained issue, this test cannot be run expectedly

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
