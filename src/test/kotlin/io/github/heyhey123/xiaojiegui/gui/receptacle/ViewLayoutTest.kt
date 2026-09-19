package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.layout.LayoutType
import org.bukkit.event.inventory.InventoryType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every [ViewLayout] is a promise about the window the client will draw: the container slots come
 * first, the player's 27 main slots and 9 hotbar slots follow. [io.github.heyhey123.xiaojiegui.gui.layout.Layout]
 * derives the player ranges from that promise, so a range that does not match what the client shows
 * does not fail loudly — it silently shifts the player's items and clicks by a few slots. These tests
 * pin the promise to Bukkit's own numbers, which is the only place the promise is written down twice.
 */
class ViewLayoutTest {

    private lateinit var server: ServerMock

    @BeforeEach
    fun setUp() {
        // InventoryType is registry backed: touching it at all needs a server, mocked or real.
        server = MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `every layout type has a view layout`() {
        val used = allLayouts().map { it.type }.toSet()
        assertEquals(
            LayoutType.entries.toSet(),
            used,
            "every LayoutType must be reachable through a ViewLayout"
        )
    }

    @Test
    fun `container size matches the inventory Bukkit builds for the layout`() {
        val wrong = allLayouts()
            .filter { it.inventoryType != InventoryType.CHEST }
            .filter { it.containerSize != it.inventoryType.defaultSize }
            .map { "${it.type} (${it.inventoryType}) is ${it.containerSize} slots, Bukkit says ${it.inventoryType.defaultSize}" }
        assertEquals(emptyList(), wrong, "layout slot ranges drifted from Bukkit's own inventory sizes")
    }

    @Test
    fun `layout inventory types can be opened as a window`() {
        // MERCHANT is the exception: Bukkit refuses to create a merchant inventory (the trades come from
        // the merchant API), so it only works in phantom mode, where the server draws the window itself.
        val notOpenable = allLayouts()
            .filterNot { it.inventoryType.isCreatable }
            .filterNot { it.inventoryType == InventoryType.MERCHANT }
            .map { "${it.type} (${it.inventoryType})" }
        assertEquals(emptyList(), notOpenable, "a layout whose inventory cannot be created cannot be shown to a player")
    }

    @Test
    fun `player slot ranges follow the container slots`() {
        allLayouts().forEach { layout ->
            assertEquals(
                layout.containerSize,
                layout.mainInvSlotRange.first(),
                "${layout.type}: main inventory must start right after the container slots"
            )
            assertEquals(
                layout.containerSize + 27,
                layout.hotBarSlotRange.first(),
                "${layout.type}: the hotbar must follow the 27 main inventory slots"
            )
            assertEquals(
                layout.containerSize + 36,
                layout.totalSize,
                "${layout.type}: a window is container + 27 main + 9 hotbar"
            )
        }
    }

    @Test
    fun `layout table`() {
        val actual = allLayouts().associate { (it::class.simpleName ?: "?") to it.containerSize }
        assertEquals(EXPECTED_SIZES, actual, "the layout table changed; update the docs that quote it")
    }

    @Test
    fun `unsupported container types are not menus at all`() {
        // Composter, chiseled bookshelf, decorated pot, shelf and jukebox have an InventoryType but no
        // menu type, so no window can be drawn for them: they are not "not implemented yet" here.
        listOf(
            InventoryType.COMPOSTER,
            InventoryType.CHISELED_BOOKSHELF,
            InventoryType.DECORATED_POT,
            InventoryType.SHELF,
            InventoryType.JUKEBOX
        ).forEach { type ->
            assertTrue(
                type.menuType == null || !type.isCreatable,
                "$type became a real window; it can be supported as a layout now"
            )
        }
    }

    private fun allLayouts(): List<ViewLayout> =
        ViewLayout::class.sealedSubclasses.flatMap { parent ->
            parent.sealedSubclasses.mapNotNull { it.objectInstance as? ViewLayout }
        }

    private companion object {
        /** The container slots of every layout, as the client draws them. */
        val EXPECTED_SIZES = mapOf(
            "GENERIC_9X1" to 9,
            "GENERIC_9X2" to 18,
            "GENERIC_9X3" to 27,
            "GENERIC_9X4" to 36,
            "GENERIC_9X5" to 45,
            "GENERIC_9X6" to 54,
            "GENERIC_3X3" to 9,
            "WORKBENCH" to 10,
            "ANVIL" to 3,
            "BARREL" to 27,
            "BEACON" to 1,
            "BLAST_FURNACE" to 3,
            "BREWING_STAND" to 5,
            "CRAFTER" to 9,
            "DISPENSER" to 9,
            "ENCHANTMENT" to 2,
            "ENDER_CHEST" to 27,
            "FURNACE" to 3,
            "GRINDSTONE" to 3,
            "HOPPER" to 5,
            "LECTERN" to 1,
            "LOOM" to 4,
            "MERCHANT" to 3,
            "SHULKER_BOX" to 27,
            "SMITHING" to 4,
            "SMOKER" to 3,
            "CARTOGRAPHY_TABLE" to 3,
            "STONECUTTER" to 2
        )
    }
}
