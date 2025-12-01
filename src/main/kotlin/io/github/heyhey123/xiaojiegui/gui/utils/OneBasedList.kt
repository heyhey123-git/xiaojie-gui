package io.github.heyhey123.xiaojiegui.gui.utils

/**
 * A 1-based index view over a mutable list.
 */
class OneBasedList<T>(
    private val backing: MutableList<T> = mutableListOf()
) : Iterable<T> {

    /**
     * The size of the list.
     */
    val size: Int
        get() = backing.size

    operator fun get(index: Int): T = backing[index - 1]

    operator fun set(index: Int, element: T) {
        backing[index - 1] = element
    }

    /**
     * Adds an element to the end of the list.
     *
     * @param element The element to add.
     */
    fun add(element: T) = backing.add(element)

    /**
     * Adds all elements from the given collection to the end of the list.
     *
     * @param elements The collection of elements to add.
     */
    fun addAll(elements: Collection<T>) = backing.addAll(elements)

    /**
     * Adds an element at the specified 1-based index.
     *
     * @param index The 1-based index where the element should be added.
     * @param element The element to add.
     */
    fun add(index: Int, element: T) = backing.add(index - 1, element)

    /**
     * Clears the list.
     */
    fun clear() = backing.clear()

    override fun iterator(): Iterator<T> = backing.iterator()
}
