package io.github.heyhey123.xiaojiegui.gui.menu

import io.github.heyhey123.xiaojiegui.gui.menu.component.Cooldown
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CooldownTest {
    private lateinit var player: Player
    private lateinit var properties: MenuProperties

    @BeforeEach
    fun setUp() {
        player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        properties = mockk<MenuProperties>(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun setClickDelay(delay: Int = 50) {
        every { properties.minClickDelay } returns delay
    }

    @Test
    fun `tryConsumeCooldown test`() {
        val cooldown = Cooldown(properties)
        val delay = 80
        setClickDelay(delay)

        assertTrue(cooldown.tryConsumeCooldown(player, delay)) // First attempt should succeed
        assertFalse(cooldown.tryConsumeCooldown(player, delay))
        Thread.sleep(delay + 50L)
        assertTrue(cooldown.tryConsumeCooldown(player, delay))
    }

//    @Test
//    fun `remainingCooldown reports correctly`() {
//        val cooldown = Cooldown(properties)
//        setClickDelay(30)
//        val delay = 100
//
//        assertTrue(cooldown.tryConsumeCooldown(player, delay))
//        val rem1 = cooldown.remainingCooldown(player, delay)
//        assertTrue(rem1 in 1..delay.toLong())
//
//        Thread.sleep(delay + 30L)
//        assertEquals(0, cooldown.remainingCooldown(player, delay))
//    }
}
