package io.github.heyhey123.xiaojiegui.gui.utils

import org.bukkit.craftbukkit.inventory.CraftAbstractInventoryView
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

/**
 * Reflection utilities for accessing and manipulating private fields and methods in CraftBukkit classes.
 */
internal object Reflection {

    object CraftContainerViewProxy {
        private val titleFieldSetter = object : ClassValue<MethodHandle>() {
            override fun computeValue(type: Class<*>): MethodHandle {
                val clazz = type.asSubclass(CraftAbstractInventoryView::class.java)
                val lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup())
                // Paper has run on Mojang mappings since 1.20.5, so what CraftBukkit calls `title` in
                // its own source is still called `title` at runtime and needs no remapping.
                return lookup.findSetter(clazz, "title", String::class.java)
            }
        }

        fun setTitle(
            craftInventoryViewObject: CraftAbstractInventoryView,
            newValue: String
        ) {
            titleFieldSetter.get(craftInventoryViewObject::class.java).invoke(craftInventoryViewObject, newValue)
        }
    }
}
