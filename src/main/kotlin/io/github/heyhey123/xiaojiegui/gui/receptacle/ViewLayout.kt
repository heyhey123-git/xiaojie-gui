// The object names below deliberately mirror the inventory constants they stand for
// (GENERIC_9X3, BLAST_FURNACE, ...): reading a `when` block against Bukkit's InventoryType and the
// client's MenuType is the whole point of these layouts. ktlint's class-naming rule asks for camel
// case, which would break that correspondence, so it is suppressed for this file only.
@file:Suppress("ktlint:standard:class-naming")

package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.layout.Layout
import io.github.heyhey123.xiaojiegui.gui.layout.LayoutType
import org.bukkit.event.inventory.InventoryType

/**
 * ViewLayout defines the slot arrangement for a view GUI.
 *
 * `slotRange` is not a free choice: it is the set of slots the client draws for the window this layout
 * opens, so it has to match the size of [inventoryType]. The player's 27 main slots and 9 hotbar slots
 * follow immediately after it (see [Layout]), which is how vanilla numbers a window; a range that is one
 * slot too short does not fail, it moves every player slot in the window by one and clicks land on the
 * wrong item. `ViewLayoutTest` pins each range to Bukkit's own size for the type.
 *
 * The derivation of that player half is one promise about every menu but one, so [hasPlayerInventory] is
 * the exception written down: the lectern's menu has a single slot and no player inventory, so its
 * layout carries no player half and `Layout` builds none. A window whose client menu has no player slots
 * but whose layout claims them is not an off-by-one -- the content packet is longer than the client's
 * menu and the vanilla client disconnects on the first slot past its own, which is what `LecternMenu` did
 * before this was separated out.
 *
 * @param type the layout type
 * @param inventoryType the Bukkit inventory type
 * @param slotRange the range of valid slot indices for this layout
 * @property hasPlayerInventory whether the client's menu for this window has the player's 27 main slots
 * and 9 hotbar slots below the container; inherited from [Layout], and false for the lectern
 */
sealed class ViewLayout(
    val type: LayoutType,
    val inventoryType: InventoryType,
    slotRange: IntRange
) : Layout(slotRange) {

    /**
     * Chest layouts (9x1 to 9x6)
     */
    sealed class Chest(type: LayoutType, slotRange: IntRange) : ViewLayout(type, InventoryType.CHEST, slotRange) {
        object GENERIC_9X1 : Chest(LayoutType.GENERIC_9X1, 0..8)
        object GENERIC_9X2 : Chest(LayoutType.GENERIC_9X2, 0..17)
        object GENERIC_9X3 : Chest(LayoutType.GENERIC_9X3, 0..26)
        object GENERIC_9X4 : Chest(LayoutType.GENERIC_9X4, 0..35)
        object GENERIC_9X5 : Chest(LayoutType.GENERIC_9X5, 0..44)
        object GENERIC_9X6 : Chest(LayoutType.GENERIC_9X6, 0..53)
    }

    /**
     * Fixed-size container layouts
     *
     * Every one of these windows carries the player's half except [FixedContainer.LECTERN], whose menu
     * has no player inventory to carry; see that layout for what the exception costs.
     */
    sealed class FixedContainer(type: LayoutType, inventoryType: InventoryType, slotRange: IntRange) :
        ViewLayout(type, inventoryType, slotRange) {
        object GENERIC_3X3 : FixedContainer(LayoutType.GENERIC_3X3, InventoryType.DROPPER, 0..8)
        object WORKBENCH : FixedContainer(LayoutType.WORKBENCH, InventoryType.WORKBENCH, 0..9)
        object ANVIL : FixedContainer(LayoutType.ANVIL, InventoryType.ANVIL, 0..2)
        object BARREL : FixedContainer(LayoutType.GENERIC_9X3, InventoryType.BARREL, 0..26)
        object BEACON : FixedContainer(LayoutType.BEACON, InventoryType.BEACON, 0..0)
        object BLAST_FURNACE : FixedContainer(LayoutType.BLAST_FURNACE, InventoryType.BLAST_FURNACE, 0..2)
        object BREWING_STAND : FixedContainer(LayoutType.BREWING_STAND, InventoryType.BREWING, 0..4)
        object CRAFTER : FixedContainer(LayoutType.CRAFTER, InventoryType.CRAFTER, 0..8)
        object DISPENSER : FixedContainer(LayoutType.GENERIC_3X3, InventoryType.DISPENSER, 0..8)
        object ENCHANTMENT : FixedContainer(LayoutType.ENCHANTMENT, InventoryType.ENCHANTING, 0..1)
        object ENDER_CHEST : FixedContainer(LayoutType.GENERIC_9X3, InventoryType.ENDER_CHEST, 0..26)
        object FURNACE : FixedContainer(LayoutType.FURNACE, InventoryType.FURNACE, 0..2)
        object GRINDSTONE : FixedContainer(LayoutType.GRINDSTONE, InventoryType.GRINDSTONE, 0..2)
        object HOPPER : FixedContainer(LayoutType.HOPPER, InventoryType.HOPPER, 0..4)

        /**
         * The lectern: the one window whose client menu has no player inventory at all.
         *
         * 26.2's `LecternMenu` adds a single slot and calls `addStandardInventorySlots` nowhere, so the
         * client's menu is one slot long. A layout that claimed the usual 27 main slots and 9 hotbar
         * slots after it made the whole-window content packet 37 items long for a one-slot menu, which
         * the vanilla client answers with `IndexOutOfBoundsException: Index 1 out of bounds for length 1`
         * in `AbstractContainerMenu.initializeContents` and a Network Protocol Error disconnect. Only the
         * lectern is like this: the beacon is one container slot *plus* the player's 36, and every other
         * menu has them too. A window that has no player half cannot be given one, which is why
         * `hide player inventory` / `show player inventory` mean nothing on this menu.
         */
        object LECTERN : FixedContainer(LayoutType.LECTERN, InventoryType.LECTERN, 0..0) {
            override val hasPlayerInventory: Boolean = false
        }

        object LOOM : FixedContainer(LayoutType.LOOM, InventoryType.LOOM, 0..3)
        object MERCHANT : FixedContainer(LayoutType.MERCHANT, InventoryType.MERCHANT, 0..2)
        object SHULKER_BOX : FixedContainer(LayoutType.SHULKER_BOX, InventoryType.SHULKER_BOX, 0..26)
        object SMITHING : FixedContainer(LayoutType.SMITHING, InventoryType.SMITHING, 0..3)
        object SMOKER : FixedContainer(LayoutType.SMOKER, InventoryType.SMOKER, 0..2)
        object CARTOGRAPHY_TABLE : FixedContainer(LayoutType.CARTOGRAPHY_TABLE, InventoryType.CARTOGRAPHY, 0..2)
        object STONECUTTER : FixedContainer(LayoutType.STONECUTTER, InventoryType.STONECUTTER, 0..1)
    }

    companion object {
        /**
         * The layout for [type], or null when the client has no window the addon can build for it.
         *
         * A script author can name any inventory type, so "no such window" is their mistake to be told about, not
         * an exception: the callers that face a script ask this question and report; [fromInventoryType] is for
         * the paths where the type is already known to be buildable.
         */
        fun fromInventoryTypeOrNull(type: InventoryType): ViewLayout? = when (type) {
            InventoryType.CHEST -> Chest.GENERIC_9X3
            InventoryType.DROPPER -> FixedContainer.GENERIC_3X3
            InventoryType.WORKBENCH -> FixedContainer.WORKBENCH
            InventoryType.CRAFTER -> FixedContainer.CRAFTER
            InventoryType.ANVIL -> FixedContainer.ANVIL
            InventoryType.BARREL -> FixedContainer.BARREL
            InventoryType.BEACON -> FixedContainer.BEACON
            InventoryType.BLAST_FURNACE -> FixedContainer.BLAST_FURNACE
            InventoryType.BREWING -> FixedContainer.BREWING_STAND
            InventoryType.DISPENSER -> FixedContainer.DISPENSER
            InventoryType.ENCHANTING -> FixedContainer.ENCHANTMENT
            InventoryType.ENDER_CHEST -> FixedContainer.ENDER_CHEST
            InventoryType.FURNACE -> FixedContainer.FURNACE
            InventoryType.GRINDSTONE -> FixedContainer.GRINDSTONE
            InventoryType.HOPPER -> FixedContainer.HOPPER
            InventoryType.LECTERN -> FixedContainer.LECTERN
            InventoryType.LOOM -> FixedContainer.LOOM
            InventoryType.MERCHANT -> FixedContainer.MERCHANT
            InventoryType.SHULKER_BOX -> FixedContainer.SHULKER_BOX
            InventoryType.SMITHING -> FixedContainer.SMITHING
            InventoryType.SMOKER -> FixedContainer.SMOKER
            InventoryType.CARTOGRAPHY -> FixedContainer.CARTOGRAPHY_TABLE
            InventoryType.STONECUTTER -> FixedContainer.STONECUTTER
            else -> null
        }

        /** Whether [type] has a window this addon can build at all. */
        fun isBuildable(type: InventoryType): Boolean = fromInventoryTypeOrNull(type) != null

        /**
         * The layout for [type], for the paths where the type is already known to be buildable.
         *
         * A script author names the type, so the paths that face one ask [isBuildable] and report; this is what
         * the internals ask once a menu holds a type that has already been through that gate.
         */
        fun fromInventoryType(type: InventoryType): ViewLayout =
            fromInventoryTypeOrNull(type) ?: throw IllegalArgumentException("Unsupported inventory type: $type")
    }
}
