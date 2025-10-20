package io.github.heyhey123.xiaojiegui.gui.menu

import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle
import io.mockk.*
import io.mockk.junit5.MockKExtension
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class MenuSessionTest {

    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        // clear all sessions before each test
        MenuSession.clearSessions()

        player = mockk(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getSession returns same instance per player`() {
        val s1 = MenuSession.getSession(player)
        val s2 = MenuSession.getSession(player)

        assertSame(s1, s2)
        assertSame(s1, MenuSession.querySession(player))
    }

    @Test
    fun `querySession returns null for non-existing session`() {
        assertNull(MenuSession.querySession(player))
    }

    @Test
    fun `removeSession removes stored session`() {
        MenuSession.getSession(player)
        assertNotNull(MenuSession.querySession(player))

        MenuSession.removeSession(player)
        assertNull(MenuSession.querySession(player))
    }

    @Test
    fun `getIcon delegates to receptacle and returns item`() {
        val receptacle = mockk<ViewReceptacle>()
        val item = mockk<ItemStack>()
        every { receptacle.getElement(5) } returns item

        val session = MenuSession(player, null, -1, receptacle)
        assertEquals(item, session.getIcon(5))

        // 无容器时返回 null
        val sessionNoRec = MenuSession(player, null, -1, null)
        assertNull(sessionNoRec.getIcon(1))
    }

    @Test
    fun `setIcon sets and refreshes when flag true`() {
        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        val item = mockk<ItemStack>()
        every { receptacle.setElement(7, item) } just Runs
        every { receptacle.refresh(7) } just Runs

        val session = MenuSession(player, null, -1, receptacle)
        session.setIcon(7, item, refresh = true)

        verify(exactly = 1) { receptacle.setElement(7, item) }
        verify(exactly = 1) { receptacle.refresh(7) }
    }

    @Test
    fun `setIcons sets multiple and refreshes all when flag true`() {
        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        val i1 = mockk<ItemStack>()
        every { receptacle.setElement(any(), any()) } just Runs
        every { receptacle.refresh() } just Runs

        val session = MenuSession(player, null, -1, receptacle)
        session.setIcons(mapOf(1 to i1, 2 to null), refresh = true)

        verify { receptacle.setElement(1, i1) }
        verify { receptacle.setElement(2, null) }
        verify { receptacle.refresh() }
    }

    @Test
    fun `title delegates to receptacle`() {
        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        val session = MenuSession(player, null, -1, receptacle)

        val title = Component.text("Test Title")
        every { receptacle.title(title, true) } just Runs

        session.title(title, refresh = true)
        verify { receptacle.title(title, true) }
    }

    @Test
    fun `refresh delegates to receptacle for default and specific slot`() {
        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        val session = MenuSession(player, null, -1, receptacle)

        every { receptacle.refresh(-1) } just Runs
        every { receptacle.refresh(3) } just Runs

        session.refresh()
        session.refresh(3)

        verify { receptacle.refresh(-1) }
        verify { receptacle.refresh(3) }
    }

    @Test
    fun `close closes receptacle and updates viewer inventory`() {
        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        every { receptacle.close(true) } just Runs
        every { player.updateInventory() } just Runs

        val session = MenuSession(player, null, -1, receptacle)
        session.close()

        verify { receptacle.close(true) }
        verify { player.updateInventory() }
    }

    @Test
    fun `shut clears fields removes from menu viewers and registry`() {
        // 先注册会话
        val session = MenuSession.getSession(player)

        // 模拟 menu 与其 viewers
        val menu = mockk<Menu>()
        val viewers = mutableSetOf<UUID>()
        every { menu.viewers } returns viewers

        viewers.add(player.uniqueId)

        // 准备状态
        session.menu = menu
        session.page = 3
        session.receptacle = mockk(relaxed = true)

        // 断言准备正确
        assertSame(session, MenuSession.querySession(player))
        assertTrue(player.uniqueId in viewers)

        // 调用
        session.shut()

        // 字段被清空
        assertNull(session.menu)
        assertEquals(-1, session.page)
        assertNull(session.receptacle)

        // 从全局注册表与 menu.viewers 移除
        assertNull(MenuSession.querySession(player))
        assertFalse(player.uniqueId in viewers)
    }
}

