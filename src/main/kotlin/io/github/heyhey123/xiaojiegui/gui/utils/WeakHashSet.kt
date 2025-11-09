package io.github.heyhey123.xiaojiegui.gui.utils

import java.util.*

/**
 * A [MutableSet] implementation backed by a [WeakHashMap].
 * Used to hold elements with weak references, allowing them to be garbage collected when no strong references exist.
 *
 * @param T the type of elements contained in the set
 * @property delegate the underlying mutable set
 * @constructor Creates a new [WeakHashSet] with an optional initial collection of elements.
 */
class WeakHashSet<T : Any>(
    private val delegate: MutableSet<T> = Collections.newSetFromMap(WeakHashMap())
) : MutableSet<T> by delegate
