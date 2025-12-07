package io.github.heyhey123.xiaojiegui.gui.utils

import org.bukkit.craftbukkit.inventory.CraftAbstractInventoryView
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

/**
 * Reflection utilities for accessing and manipulating private fields and methods in CraftBukkit classes.
 *
 */
internal object Reflection {

    val reflectionRemapper: ReflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar()

//    val reflectionProxyFactory: ReflectionProxyFactory =
//        ReflectionProxyFactory.create(reflectionRemapper, Reflection::class.java.getClassLoader())


    object CraftContainerViewProxy {
        private val titleFieldSetter = object : ClassValue<MethodHandle>() {
            override fun computeValue(type: Class<*>): MethodHandle {
                val clazz = type.asSubclass(CraftAbstractInventoryView::class.java)
                val lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup())
                val runtimeName = reflectionRemapper.remapFieldName(clazz, "title")
                val setter = lookup.findSetter(
                    clazz,
                    runtimeName,
                    String::class.java
                )
                return setter
            }
        }

        fun setTitle(
            craftInventoryViewObject: CraftAbstractInventoryView,
            newValue: String,
        ) {
            titleFieldSetter.get(craftInventoryViewObject::class.java).invoke(craftInventoryViewObject, newValue)
        }
    }
}
