package io.github.heyhey123.xiaojiegui.utils

import io.github.heyhey123.xiaojiegui.XiaojieGUI
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

object TaskUtil {
    abstract class Cancellable : Runnable {

        var cancelled: Boolean = false
            protected set

        fun cancel() {
            cancelled = true
        }
    }

    fun sync(
        period: Long = 0L,
        now: Boolean = false,
        delay: Long = 0L,
        block: Cancellable.() -> Unit
    ): BukkitTask {
        val scheduler = Bukkit.getScheduler()
        if (period <= 0L) {
            return scheduler.runTaskLater(XiaojieGUI.Companion.instance, Runnable {
                val task = object : Cancellable() {
                    override fun run() {
                        block()
                    }
                }
                task.run()
            }, delay)
        }
        return scheduler.runTaskTimer(XiaojieGUI.Companion.instance, Runnable {
            val task = object : Cancellable() {
                override fun run() {
                    block()
                }
            }
            task.run()
        }, if (now) 0L else delay, period)
    }

    fun async(
        period: Long = 0L,
        now: Boolean = false,
        delay: Long = 0L,
        block: Cancellable.() -> Unit
    ): BukkitTask {
        val scheduler = Bukkit.getScheduler()
        if (period <= 0L) {
            return scheduler.runTaskLaterAsynchronously(XiaojieGUI.Companion.instance, Runnable {
                val task = object : Cancellable() {
                    override fun run() {
                        block()
                    }
                }
                task.run()
            }, delay)
        }
        return scheduler.runTaskTimerAsynchronously(XiaojieGUI.Companion.instance, Runnable {
            val task = object : Cancellable() {
                override fun run() {
                    block()
                }
            }
            task.run()
        }, if (now) 0L else delay, period)
    }

    fun submit(async: Boolean, period: Long = 0L, delay: Long = 0L, block: Cancellable.() -> Unit): BukkitTask =
        if (async) {
            async(period = period, delay = delay, block = block)
        } else {
            sync(period = period, delay = delay, block = block)
        }
}
