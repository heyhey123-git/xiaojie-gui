package io.github.heyhey123.xiaojiegui.gui

import org.bukkit.craftbukkit.inventory.CraftAbstractInventoryView
import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import java.lang.reflect.Field

/**
 * Reflection utilities for accessing and manipulating private fields and methods in CraftBukkit classes.
 *
 */
internal object Reflection {

    val reflectionRemapper: ReflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar()

//    val reflectionProxyFactory: ReflectionProxyFactory =
//        ReflectionProxyFactory.create(reflectionRemapper, Reflection::class.java.getClassLoader())


    object CraftContainerViewProxy {
        private val titleField = object : ClassValue<Field>() {
            override fun computeValue(type: Class<*>): Field {
                val clazz = type.asSubclass(CraftAbstractInventoryView::class.java)
                val runtimeName = reflectionRemapper.remapFieldName(clazz, "title")
                return clazz.getDeclaredField(runtimeName).apply { isAccessible = true }
            }
        }

        fun getFieldTitle(clazz: Class<out CraftAbstractInventoryView>): Field =
            titleField.get(clazz)
    }
}
