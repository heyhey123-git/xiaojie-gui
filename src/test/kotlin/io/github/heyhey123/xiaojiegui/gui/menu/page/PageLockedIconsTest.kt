package io.github.heyhey123.xiaojiegui.gui.menu.page

import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.menu.component.Cooldown
import io.github.heyhey123.xiaojiegui.gui.menu.component.IconProducer
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * `locked icons` is what makes a shop a shop: the slots a layout gives an icon to belong to the menu, so
 * nothing can be taken out of them, put into them, swapped with them, dropped from them or collected
 * from them -- and a drag that touches one is refused as a whole, because a drag cannot be applied
 * halfway. The slots the layout leaves empty stay the player's, which is what makes the same menu a
 * backpack when it has no icons at all.
 *
 * The rule is enforced around the callbacks rather than instead of them: a callback written for a locked
 * slot still runs (`map key "S" to icon diamond ... and when clicked: buy(...)` is a shop's whole idea),
 * and only the item movement is refused.
 *
 * `iconSlots` is also what a page turn clears, so the second half of this file is about a page keeping
 * its hands off the slots it did not put anything in.
 */
class PageLockedIconsTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `a page owns the slots it gives an icon to, and nothing else`() {
        val setup = Setup(lockedIcons = true, layout = "aa       ")

        assertEquals(setOf(0, 1), setup.page.iconSlots())
    }

    @Test
    fun `an override with an item is a slot the page owns, a cleared one is not`() {
        val setup = Setup(lockedIcons = true, layout = "aa       ")
        setup.page.slotOverrides[5] = mockk<ItemStack>(relaxed = true)
        assertEquals(setOf(0, 1, 5), setup.page.iconSlots())

        // `override slot 5 to air` is how a script clears an icon, and a cleared slot is free again.
        val cleared = mockk<ItemStack>(relaxed = true)
        every { cleared.isEmpty } returns true
        setup.page.slotOverrides[6] = cleared
        assertEquals(setOf(0, 1, 5), setup.page.iconSlots())
    }

    @Test
    fun `clearing a page leaves the slots it does not own alone`() {
        val setup = Setup(lockedIcons = true, layout = "aa       ")

        setup.page.clearIcons(setup.session)

        verify(exactly = 1) { setup.session.setIcon(0, null, false) }
        verify(exactly = 1) { setup.session.setIcon(1, null, false) }
        // Slot 5 is a slot the player filled, and a page turn must not touch it.
        verify(exactly = 0) { setup.session.setIcon(5, any(), any()) }
    }

    @Test
    fun `a click on an icon slot is refused, and its callback still runs`() {
        val setup = Setup(lockedIcons = true, layout = "aa       ")

        setup.interact(ClickType.LEFT, slot = 0)

        verify { setup.clickedEvent.isCancelled = true }
        assertEquals(true, setup.callbackRan[0], "the callback of the slot is the shop's chance to sell")
    }

    @Test
    fun `a click on a free slot is the player's`() {
        val setup = Setup(lockedIcons = true, layout = "aa       ")

        setup.interact(ClickType.LEFT, slot = 5)

        verify(exactly = 0) { setup.clickedEvent.isCancelled = true }
    }

    @Test
    fun `every way of taking from an icon slot is refused`() {
        for (clickType in listOf(
            ClickType.LEFT,
            ClickType.RIGHT,
            ClickType.SHIFT_LEFT,
            ClickType.SHIFT_RIGHT,
            ClickType.NUMBER_KEY_1,
            ClickType.SWAP_OFFHAND,
            ClickType.MIDDLE,
            ClickType.DROP,
            ClickType.CONTROL_DROP,
            ClickType.DOUBLE_CLICK
        )) {
            val setup = Setup(lockedIcons = true, layout = "aa       ")
            setup.interact(clickType, slot = 1)
            verify { setup.clickedEvent.isCancelled = true }
        }
    }

    @Test
    fun `a drag that touches an icon slot is refused as a whole`() {
        val setup = Setup(lockedIcons = true, layout = "aa       ")

        setup.interact(ClickType.LEFT, slots = listOf(1, 5, 6))

        verify { setup.clickedEvent.isCancelled = true }
    }

    @Test
    fun `a drag over free slots is the player's`() {
        val setup = Setup(lockedIcons = true, layout = "aa       ")

        setup.interact(ClickType.LEFT, slots = listOf(5, 6))

        verify(exactly = 0) { setup.clickedEvent.isCancelled = true }
    }

    @Test
    fun `a shift click in the player's own half reaches a free slot, which is what selling is`() {
        // A shop that buys from the player's backpack: the container's empty slots are where the goods go,
        // and nothing the menu owns can take the item, so the click happens.
        val setup = Setup(lockedIcons = true, layout = "aa       ")

        setup.interact(ClickType.SHIFT_LEFT, slot = 12, item = item(similarTo = false))

        verify(exactly = 0) { setup.clickedEvent.isCancelled = true }
    }

    @Test
    fun `a shift click is refused when a slot the menu owns could merge the item`() {
        // The game merges into a matching stack first, wherever that stack is, so a matching stack in a
        // slot the menu owns is the one case where a shift click would reach somewhere it may not.
        val setup = Setup(lockedIcons = true, layout = "aa       ")
        setup.holds(0, item(similarTo = true, amount = 1, max = 64))

        setup.interact(ClickType.SHIFT_LEFT, slot = 12, item = item(similarTo = false))

        verify { setup.clickedEvent.isCancelled = true }
    }

    @Test
    fun `a full stack in a slot the menu owns is not something a shift click can merge into`() {
        val setup = Setup(lockedIcons = true, layout = "aa       ")
        setup.holds(0, item(similarTo = true, amount = 64, max = 64))

        setup.interact(ClickType.SHIFT_LEFT, slot = 12, item = item(similarTo = false))

        verify(exactly = 0) { setup.clickedEvent.isCancelled = true }
    }

    @Test
    fun `a double click is refused only when the menu's own slots hold that item`() {
        val nothing = Setup(lockedIcons = true, layout = "aa       ")
        nothing.interact(ClickType.DOUBLE_CLICK, slot = 12, item = item(similarTo = false))
        verify(exactly = 0) { nothing.clickedEvent.isCancelled = true }

        val held = Setup(lockedIcons = true, layout = "aa       ")
        held.holds(1, item(similarTo = true))
        held.interact(ClickType.DOUBLE_CLICK, slot = 12, item = item(similarTo = false))
        verify { held.clickedEvent.isCancelled = true }
    }

    @Test
    fun `the rest of the player's own half is their own business`() {
        for (clickType in listOf(ClickType.LEFT, ClickType.RIGHT, ClickType.NUMBER_KEY_3, ClickType.DROP)) {
            val setup = Setup(lockedIcons = true, layout = "aa       ")
            setup.holds(0, item(similarTo = true))
            setup.interact(clickType, slot = 12, item = item(similarTo = false))
            verify(exactly = 0) { setup.clickedEvent.isCancelled = true }
        }
    }

    @Test
    fun `a menu without locked icons refuses nothing`() {
        val setup = Setup(lockedIcons = false, layout = "aa       ")

        setup.interact(ClickType.LEFT, slot = 0)
        setup.interact(ClickType.SHIFT_LEFT, slot = 12)
        setup.interact(ClickType.LEFT, slots = listOf(0, 5))

        verify(exactly = 0) { setup.clickedEvent.isCancelled = true }
    }

    /** One page with a menu and a session behind it, and a way to make an interaction happen. */
    private class Setup(lockedIcons: Boolean, layout: String) {

        val callbackRan = mutableMapOf<Int, Boolean>()
        val clickedEvent = mockk<ReceptacleInteractEvent>(relaxed = true)

        private val properties = mockk<MenuProperties>(relaxed = true).also {
            every { it.mode } returns Receptacle.Mode.STATIC
            every { it.hidePlayerInventory } returns false
            every { it.lockedIcons } returns lockedIcons
        }

        val page = Page(InventoryType.CHEST, Component.text("Title"), listOf(layout), emptyList(), properties)

        val session = mockk<MenuSession>(relaxed = true)

        private val onClick = slot<(ReceptacleInteractEvent) -> Unit>()

        init {
            val icon = mockk<ItemStack>(relaxed = true)
            every { icon.clone() } returns icon
            page.iconMapper["a"] = IconProducer.SingleIconProducer(icon) to null
            page.clickCallbacks[0] = { callbackRan[0] = true }
            page.clickCallbacks[1] = { callbackRan[1] = true }

            val menu = mockk<Menu>(relaxed = true)
            every { menu.properties } returns properties
            every { menu.cooldownManager } returns mockk<Cooldown>(relaxed = true).also {
                every { it.tryConsumeCooldown(any()) } returns true
            }

            val receptacle = mockk<ViewReceptacle>(relaxed = true)
            every { receptacle.title(any(), any()) } just Runs
            every { receptacle.setElement(any(), any()) } just Runs
            every { receptacle.onClose(any()) } just Runs
            every { receptacle.onClick(capture(onClick)) } just Runs
            every { receptacle.getElement(any()) } returns null

            every { session.menu } returns menu
            every { session.component1() } returns mockk(relaxed = true)
            every { session.component2() } returns menu
            every { session.component3() } returns receptacle

            mockkConstructor(MenuInteractEvent::class)
            every { anyConstructed<MenuInteractEvent>().callEvent() } returns true

            page.loadInPage(session)
        }

        fun interact(
            clickType: ClickType,
            slot: Int,
            item: ItemStack? = null,
            cursor: ItemStack? = null
        ) = interact(clickType, listOf(slot), item, cursor)

        fun interact(
            clickType: ClickType,
            slots: List<Int>,
            item: ItemStack? = null,
            cursor: ItemStack? = null
        ) {
            every { clickedEvent.clickType } returns clickType
            every { clickedEvent.slot } returns slots.first()
            every { clickedEvent.slots } returns slots
            every { clickedEvent.player } returns mockk(relaxed = true)
            every { clickedEvent.receptacle } returns mockk(relaxed = true)
            every { clickedEvent.clickedItem } returns item
            every { clickedEvent.cursor } returns cursor
            onClick.captured.invoke(clickedEvent)
        }

        /** Puts an item into one of the menu's own slots, as the container really holds it. */
        fun holds(slot: Int, item: ItemStack) {
            every { session.getIcon(slot) } returns item
        }
    }

    /** An item, and the answer its `isSimilar` gives for whatever is being moved. */
    private fun item(similarTo: Boolean, amount: Int = 1, max: Int = 64): ItemStack =
        mockk<ItemStack>(relaxed = true).also {
            every { it.isSimilar(any()) } returns similarTo
            every { it.amount } returns amount
            every { it.maxStackSize } returns max
        }
}
