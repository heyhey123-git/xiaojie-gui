package io.github.heyhey123.xiaojiegui.gui.menu.component

import org.bukkit.inventory.ItemStack

/**
 * Icon producer, produces ItemStacks for menu icons.
 *
 */
sealed class IconProducer {
    enum class Type {
        SINGLE, MULTIPLE
    }

    /**
     * Produce the next ItemStack.
     *
     * @return the next ItemStack
     */
    abstract fun produceNext(): ItemStack?

    abstract val type: Type

    /**
     * Single icon producer.
     *
     * @property item The single item to produce.
     *
     */
    class SingleIconProducer(private val item: ItemStack) : IconProducer() {
        override val type = Type.SINGLE
        override fun produceNext(): ItemStack = item
    }

    /**
     * Multiple icon producer.
     *
     * @property items The list of items to produce.
     *
     */
    class MultipleIconProducer(private val items: List<ItemStack?>) : IconProducer() {
        override val type = Type.MULTIPLE

        private var currentIndex = 0

        override fun produceNext(): ItemStack? {
            if (currentIndex >= items.size) {
                return null
            }
            return items[currentIndex++]
        }
    }
}
