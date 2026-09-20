package io.github.heyhey123.xiaojiegui.gui.menu.page

import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.menu.component.IconProducer
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.mockk.mockk
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A list page is what a browser needs: one key holds the results, so the page can be filled from a list in
 * one call and a click can be turned back into a position in that list.
 *
 * The declarative half matters as much: a page becomes a list page by *mapping a list of items to a key*
 * (`map key "L" to icon {results::*}`), so there is no extra syntax to learn and no marker to keep in step
 * with the layout.
 */
class PageListTest {

    @Test
    fun `the list slots are the slots of the key a list of items was mapped to`() {
        val page = listPage()

        // Three cells of the layout use the list key, and one cell uses another key: only the list key's
        // cells are the list, and they are in layout order.
        assertEquals(listOf(0, 1, 2), page.listSlots())
    }

    @Test
    fun `a page without a list key has no list`() {
        val properties = mockk<MenuProperties>(relaxed = true)
        val page = Page(InventoryType.CHEST, Component.text("Title"), listOf("AA       "), emptyList(), properties)
        val icon = mockk<ItemStack>(relaxed = true)
        page.iconMapper["A"] = IconProducer.SingleIconProducer(icon) to null

        assertEquals(emptyList(), page.listSlots())
    }

    @Test
    fun `a position is the slot's place in the list, and nothing for a slot that is not one`() {
        val page = listPage()

        assertEquals(1, page.listIndex(0))
        assertEquals(3, page.listIndex(2))
        // The button's own slot is not a result, and neither is an empty cell.
        assertNull(page.listIndex(5))
        assertNull(page.listIndex(8))
    }

    @Test
    fun `filling a list writes the slots in order, clears what is left over and updates once`() {
        val page = listPage()
        val session = mockk<MenuSession>(relaxed = true)
        val first = mockk<ItemStack>(relaxed = true)
        val second = mockk<ItemStack>(relaxed = true)

        page.setList(session, listOf(first, second))

        verify(exactly = 1) { session.setIcon(0, first, false) }
        verify(exactly = 1) { session.setIcon(1, second, false) }
        // Fewer items than slots is the normal case for the last page of a search: the rest go blank.
        verify(exactly = 1) { session.setIcon(2, null, false) }
        // One update for the whole page, not one per slot: that is what makes a page turn cheap.
        verify(exactly = 1) { session.refresh() }
    }

    /** A page whose layout gives key "L" three cells and whose list producer holds two items. */
    private fun listPage(): Page {
        val properties = mockk<MenuProperties>(relaxed = true)
        val page = Page(InventoryType.CHEST, Component.text("Title"), listOf("LLL      "), emptyList(), properties)
        val icon = mockk<ItemStack>(relaxed = true)
        page.iconMapper["L"] = IconProducer.MultipleIconProducer(listOf(icon)) to null
        page.iconMapper["X"] = IconProducer.SingleIconProducer(icon) to null
        return page
    }
}
