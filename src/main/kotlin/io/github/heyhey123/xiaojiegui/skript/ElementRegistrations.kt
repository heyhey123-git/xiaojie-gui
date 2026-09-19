package io.github.heyhey123.xiaojiegui.skript

import io.github.heyhey123.xiaojiegui.skript.elements.SkriptTypes
import io.github.heyhey123.xiaojiegui.skript.elements.button.expressions.ExprAllButtons
import io.github.heyhey123.xiaojiegui.skript.elements.button.sections.SecDefineButton
import io.github.heyhey123.xiaojiegui.skript.elements.menu.conditions.CondMenuDestroyed
import io.github.heyhey123.xiaojiegui.skript.elements.menu.conditions.CondPlayerInvHidden
import io.github.heyhey123.xiaojiegui.skript.elements.menu.effects.EffCloseMenu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.effects.EffDestroyMenu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.effects.EffHidePlayerInv
import io.github.heyhey123.xiaojiegui.skript.elements.menu.effects.EffInsertPage
import io.github.heyhey123.xiaojiegui.skript.elements.menu.effects.EffOpenMenu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.effects.EffTurnPage
import io.github.heyhey123.xiaojiegui.skript.elements.menu.effects.EffUpdatePageTitle
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.EvtMenuClose
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.EvtMenuInteract
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.EvtMenuOpen
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.EvtPageTurn
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprAllMenuIds
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprAllMenus
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprCurrentPage
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprEventIcon
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprEventNumberKey
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprEventPage
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprEventSlot
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprEventTitle
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprKey2Slot
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprMenuById
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprMenuClickType
import io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions.ExprSlot2Key
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprDefaultLayout
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprDefaultPage
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprDefaultTitle
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprInventoryType
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprMenuId
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprMenuMode
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprMenuViewers
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprMinClickDelay
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprPageNumber
import io.github.heyhey123.xiaojiegui.skript.elements.menu.properties.ExprPageTitle
import io.github.heyhey123.xiaojiegui.skript.elements.menu.sections.EffSecCreateMenu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.sections.EffSecEditMenu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.sections.EffSecMapKey2Icon
import io.github.heyhey123.xiaojiegui.skript.elements.menu.sections.EffSecOverrideSlot
import io.github.heyhey123.xiaojiegui.skript.elements.menu.sections.EffSecSlotCallback
import io.github.heyhey123.xiaojiegui.skript.elements.menu.sections.SecBuildMenu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.sections.SecInsertPage
import io.github.heyhey123.xiaojiegui.skript.elements.session.effects.EffClearSession
import io.github.heyhey123.xiaojiegui.skript.elements.session.effects.EffCloseSession
import io.github.heyhey123.xiaojiegui.skript.elements.session.effects.EffRefreshSession
import io.github.heyhey123.xiaojiegui.skript.elements.session.effects.EffUpdateSessionTitle
import io.github.heyhey123.xiaojiegui.skript.elements.session.expressions.ExprGetPlayerSession
import io.github.heyhey123.xiaojiegui.skript.elements.session.properties.ExprSessionIcon
import io.github.heyhey123.xiaojiegui.skript.elements.session.properties.ExprSessionMenu
import io.github.heyhey123.xiaojiegui.skript.elements.session.properties.ExprSessionPage
import io.github.heyhey123.xiaojiegui.skript.elements.session.properties.ExprSessionTitle
import io.github.heyhey123.xiaojiegui.skript.elements.session.properties.ExprSessionViewer
import org.skriptlang.skript.addon.SkriptAddon

/**
 * Registers every element this addon provides.
 *
 * The registered syntax API asks for the elements to be listed rather than their package to be
 * loaded, and listing them makes an element that was written but never added here visible: the
 * server test drives every element it expects, and the example gate parses every `@Example`, so a
 * missing registration fails there instead of quietly not existing.
 */
internal fun registerElements(addon: SkriptAddon) {
    SkriptTypes.register()

    ExprAllButtons.register(addon)
    SecDefineButton.register(addon)
    CondMenuDestroyed.register(addon)
    CondPlayerInvHidden.register(addon)
    EffCloseMenu.register(addon)
    EffDestroyMenu.register(addon)
    EffHidePlayerInv.register(addon)
    EffInsertPage.register(addon)
    EffOpenMenu.register(addon)
    EffTurnPage.register(addon)
    EffUpdatePageTitle.register(addon)
    EvtMenuClose.register(addon)
    EvtMenuInteract.register(addon)
    EvtMenuOpen.register(addon)
    EvtPageTurn.register(addon)
    ProvideMenuEvent.register(addon)
    ExprAllMenuIds.register(addon)
    ExprAllMenus.register(addon)
    ExprCurrentPage.register(addon)
    ExprEventIcon.register(addon)
    ExprEventNumberKey.register(addon)
    ExprEventPage.register(addon)
    ExprEventSlot.register(addon)
    ExprEventTitle.register(addon)
    ExprKey2Slot.register(addon)
    ExprMenuById.register(addon)
    ExprMenuClickType.register(addon)
    ExprSlot2Key.register(addon)
    ExprDefaultLayout.register(addon)
    ExprDefaultPage.register(addon)
    ExprDefaultTitle.register(addon)
    ExprInventoryType.register(addon)
    ExprMenuId.register(addon)
    ExprMenuMode.register(addon)
    ExprMenuViewers.register(addon)
    ExprMinClickDelay.register(addon)
    ExprPageNumber.register(addon)
    ExprPageTitle.register(addon)
    EffSecCreateMenu.register(addon)
    EffSecEditMenu.register(addon)
    EffSecMapKey2Icon.register(addon)
    EffSecOverrideSlot.register(addon)
    EffSecSlotCallback.register(addon)
    SecBuildMenu.register(addon)
    SecInsertPage.register(addon)
    EffClearSession.register(addon)
    EffCloseSession.register(addon)
    EffRefreshSession.register(addon)
    EffUpdateSessionTitle.register(addon)
    ExprGetPlayerSession.register(addon)
    ExprSessionIcon.register(addon)
    ExprSessionMenu.register(addon)
    ExprSessionPage.register(addon)
    ExprSessionTitle.register(addon)
    ExprSessionViewer.register(addon)
}
