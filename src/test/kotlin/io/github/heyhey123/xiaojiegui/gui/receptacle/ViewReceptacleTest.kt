package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.StaticInventory
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleCloseEvent
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.mockk.*
import io.mockk.junit5.MockKExtension
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class ViewReceptacleTest {

    private lateinit var player: Player
    private lateinit var layout: ViewLayout

    @BeforeEach
    fun setUp() {
        ViewReceptacle.viewingReceptacleMap.clear()
        player = mockk(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        layout = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        ViewReceptacle.viewingReceptacleMap.clear()
        unmockkAll()
    }

    private fun newReceptacle(title: String = "T") =
        TestViewReceptacle(Component.text(title), layout, Receptacle.Mode.PHANTOM)

    @Test
    fun `open sets viewer map and triggers onOpen and doOpen`() {
        val r = newReceptacle()
        var onOpenCalled = 0
        r.onOpen { p, rec ->
            onOpenCalled++
            assertSame(player, p)
            assertSame(r, rec)
        }

        r.open(player)

        assertEquals(1, onOpenCalled)
        assertEquals(1, r.doOpenCount)
        assertSame(player, r.lastDoOpenPlayer)
        assertSame(r, ViewReceptacle.viewingReceptacleMap[player.uniqueId])
    }

    @Test
    fun `close true cleans map triggers onClose and doClose`() {
        val r = newReceptacle()
        r.open(player)

        var onCloseCalled = 0
        r.onClose { p, rec ->
            onCloseCalled++
            assertSame(player, p)
            assertSame(r, rec)
        }

        r.close(true)

        assertEquals(1, onCloseCalled)
        assertEquals(1, r.doCloseCount)
        assertNull(ViewReceptacle.viewingReceptacleMap[player.uniqueId])
    }

    @Test
    fun `close false cleans map triggers onClose but not doClose`() {
        val r = newReceptacle()
        r.open(player)

        var onCloseCalled = 0
        r.onClose { _, _ -> onCloseCalled++ }

        r.close(false)

        assertEquals(1, onCloseCalled)
        assertEquals(0, r.doCloseCount)
        assertNull(ViewReceptacle.viewingReceptacleMap[player.uniqueId])
    }

    @Test
    fun `closed delegates to close false and fires ReceptacleCloseEvent`() {
        val r = newReceptacle()
        r.open(player)

        mockkConstructor(ReceptacleCloseEvent::class)
        every { anyConstructed<ReceptacleCloseEvent>().callEvent() } returns true

        r.closed()

        // 先通过 close(false) 的副作用验证
        assertEquals(0, r.doCloseCount)
        assertNull(ViewReceptacle.viewingReceptacleMap[player.uniqueId])

        // 再验证事件发布
        verify(exactly = 1) { anyConstructed<ReceptacleCloseEvent>().callEvent() }
    }

    @Test
    fun `factory create returns concrete types`() {
        val phantom = ViewReceptacle.create(Component.text("p"), layout, Receptacle.Mode.PHANTOM)
        assertTrue(phantom is PhantomReceptacle)

        val holder = mockk<StaticInventory.Holder>(relaxed = true)
        mockkObject(StaticInventory)
        every { StaticInventory.create(any(), any()) } returns holder

        val static = ViewReceptacle.create(Component.text("s"), layout, Receptacle.Mode.STATIC)
        assertTrue(static is StaticReceptacle)
    }
}

/**
 * 测试用子类：记录 doOpen、doClose 调用情况
 */
private class TestViewReceptacle(
    title: Component,
    layout: ViewLayout,
    mode: Mode
) : ViewReceptacle(title, layout, mode) {

    var doOpenCount = 0
        private set
    var doCloseCount = 0
        private set
    var lastDoOpenPlayer: Player? = null
        private set

    override fun doOpen(player: Player) {
        doOpenCount++
        lastDoOpenPlayer = player
    }

    override fun doClose() {
        doCloseCount++
    }

    override fun clicked(clickType: ClickType, slot: Int, staticInventoryEvent: InventoryClickEvent?) {
        // 不在此处断言点击逻辑
    }

    override fun getElement(slot: Int): ItemStack {
        TODO("Not yet implemented")
    }

    override fun setElement(slot: Int, item: ItemStack?) {
        TODO("Not yet implemented")
    }

    override fun title(title: Component, render: Boolean) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun refresh(slot: Int) {
        TODO("Not yet implemented")
    }

    override fun interruptItemDrag(event: ReceptacleInteractEvent) {
        TODO("Not yet implemented")
    }
}
