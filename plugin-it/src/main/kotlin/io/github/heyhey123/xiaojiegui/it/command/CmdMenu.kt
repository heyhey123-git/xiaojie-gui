package io.github.heyhey123.xiaojiegui.it.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

object CmdMenu : Subcommand {
    override fun attach(root: LiteralArgumentBuilder<CommandSourceStack>) {
        root.then(
            Commands.literal("menu")
                .then(Commands.literal("open").executes(::openMenu))
                .then(Commands.literal("turnpage").executes(::turnPage))
                .then(Commands.literal("updateicon").executes(::updateIcon))
                .then(Commands.literal("overrideslot").executes(::overrideSlot))
                .then(Commands.literal("insertpage").executes(::insertPage))
                .then(Commands.literal("close").executes(::closeMenu))
        )
    }

    private lateinit var testMenu: Menu

    private lateinit var player: Player

    private fun openMenu(ctx: CommandContext<CommandSourceStack>): Int {
        val executor = ctx.source.executor as? Player
        if (executor == null) {
            ctx.source.sender.sendMessage("This command can only be executed by a player.")
            return 0
        }

        player = executor

        val properties = MenuProperties(
            defaultTitle = Component.text("Test Menu"),
            defaultLayout = listOf(
                "AAAAAAAAA",
                "A       A",
                "AAAAAAAAA"
            ),
            mode = Receptacle.Mode.PHANTOM,
            hidePlayerInventory = false,
            minClickDelay = 1000,
            defaultPage = 0
        )

        testMenu = Menu(null, properties, InventoryType.CHEST).apply {
            iconMapper["A"] = ItemStack(Material.STONE)

            insertPage(0, null, Component.text("Page 1"), null)
            insertPage(1, null, Component.text("Page 2"), null)

            setSlotCallback(0, 10) { _ ->
                ctx.source.sender.sendMessage("Clicked slot 10 on page 1!")
            }
        }

        testMenu.open(player, 0)
        ctx.source.sender.sendMessage("Opened test menu.")
        return Command.SINGLE_SUCCESS
    }

    private fun turnPage(ctx: CommandContext<CommandSourceStack>): Int {
        val session = MenuSession.querySession(player)
        if (session == null|| session.menu != testMenu) {
            ctx.source.sender.sendMessage("You are not viewing the test menu.")
            return 0
        }

        val nextPage = (session.page + 1) % testMenu.size
        testMenu.turnPage(player, nextPage)
        ctx.source.sender.sendMessage("Turned to page ${nextPage + 1}.")
        return Command.SINGLE_SUCCESS
    }

    private fun updateIcon(ctx: CommandContext<CommandSourceStack>): Int {
        testMenu.updateIconForKey("A", ItemStack(Material.DIAMOND), refresh = true) { event ->
            event.viewer.sendMessage("Clicked updated icon!")
        }
        ctx.source.sender.sendMessage("Updated icon 'A' to diamond.")
        return Command.SINGLE_SUCCESS
    }

    private fun overrideSlot(ctx: CommandContext<CommandSourceStack>): Int {
        testMenu.overrideSlot(0, 13, ItemStack(Material.EMERALD), refresh = true) { event ->
            event.viewer.sendMessage("Clicked overridden slot!")
        }
        ctx.source.sender.sendMessage("Overridden slot 13 on page 1 with emerald.")
        return Command.SINGLE_SUCCESS
    }

    private fun insertPage(ctx: CommandContext<CommandSourceStack>): Int {
        testMenu.insertPage(
            2,
            listOf("BBBBBBBBB", "B       B", "BBBBBBBBB"),
            Component.text("Inserted Page"),
            null
        )
        testMenu.iconMapper["B"] = ItemStack(Material.GOLD_BLOCK)
        ctx.source.sender.sendMessage("Inserted a new page.")
        return Command.SINGLE_SUCCESS
    }

    private fun closeMenu(ctx: CommandContext<CommandSourceStack>): Int {
        val session = MenuSession.getSession(player)
        if (session.menu != testMenu) {
            ctx.source.sender.sendMessage("You are not viewing the test menu.")
            return 0
        }

        session.close()
        ctx.source.sender.sendMessage("Closed the test menu.")
        return Command.SINGLE_SUCCESS
    }
}

