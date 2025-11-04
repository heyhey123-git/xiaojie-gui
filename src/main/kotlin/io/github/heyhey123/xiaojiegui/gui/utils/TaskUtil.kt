package io.github.heyhey123.xiaojiegui.gui.utils

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

    /**
     * Submit a synchronous task (on the main server thread).
     *
     * @param period The period between task executions in ticks. If 0 or less, the task will run only once.
     * @param now  Whether to run the task immediately (0 ticks delay) or after the specified delay.
     * @param delay The delay before the first execution in ticks.
     * @param block The code block to execute.
     * @return The BukkitTask representing the scheduled task.
     */
    fun sync(
        period: Long = 0L,
        now: Boolean = false,
        delay: Long = 0L,
        block: Cancellable.() -> Unit
    ): BukkitTask {
        val scheduler = Bukkit.getScheduler()
        if (period <= 0L) {
            return scheduler.runTaskLater(XiaojieGUI.instance, Runnable {
                val task = object : Cancellable() {
                    override fun run() {
                        block()
                    }
                }
                task.run()
            }, delay)
        }
        return scheduler.runTaskTimer(XiaojieGUI.instance, Runnable {
            val task = object : Cancellable() {
                override fun run() {
                    block()
                }
            }
            task.run()
        }, if (now) 0L else delay, period)
    }

    /**
     * Submit an asynchronous task (off the main server thread).
     *
     * @param period The period between task executions in ticks. If 0 or less, the task will run only once.
     * @param now Whether to run the task immediately (0 ticks delay) or after the specified delay.
     * @param delay The delay before the first execution in ticks.
     * @param block The code block to execute.
     * @return The BukkitTask representing the scheduled task.
     */
    fun async(
        period: Long = 0L,
        now: Boolean = false,
        delay: Long = 0L,
        block: Cancellable.() -> Unit
    ): BukkitTask {
        val scheduler = Bukkit.getScheduler()
        if (period <= 0L) {
            return scheduler.runTaskLaterAsynchronously(XiaojieGUI.instance, Runnable {
                val task = object : Cancellable() {
                    override fun run() {
                        block()
                    }
                }
                task.run()
            }, delay)
        }
        return scheduler.runTaskTimerAsynchronously(XiaojieGUI.instance, Runnable {
            val task = object : Cancellable() {
                override fun run() {
                    block()
                }
            }
            task.run()
        }, if (now) 0L else delay, period)
    }

    /**
     * Submit a task, either synchronous or asynchronous based on the `async` parameter.
     *
     * @param async Whether to run the task asynchronously (true) or synchronously (false).
     * @param period The period between task executions in ticks. If 0 or less, the task will run only once.
     * @param delay The delay before the first execution in ticks.
     * @param block The code block to execute.
     * @return The BukkitTask representing the scheduled task.
     */
    fun submit(async: Boolean, period: Long = 0L, delay: Long = 0L, block: Cancellable.() -> Unit): BukkitTask =
        if (async) {
            async(period = period, delay = delay, block = block)
        } else {
            sync(period = period, delay = delay, block = block)
        }
}
