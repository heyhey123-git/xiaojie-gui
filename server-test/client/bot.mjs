// The client layer of the test suite: a real Minecraft client that joins the disposable Paper server
// `clientTest` starts, sees the menu `server-test/skript/11-client.sk` opens for it, clicks that menu,
// and asserts what it was shown. It is run by the `clientTest` Gradle task and is not part of the plugin.
//
// What only this side can see: the window the client was given (its title, its type, which item is in
// each known slot and what that item is called) and the title it is shown after a page turn. What it
// cannot see is what the server did with a click; that half is logged by `11-client.sk` and asserted by
// the Gradle task.
//
// --- the protocol, and what it costs -----------------------------------------------------------------
//
// The server is Paper 26.2, protocol 776, and its status ping is off (`enable-status=false` in
// `server-test/server.properties`), so mineflayer cannot ask it what it speaks and has to be told.
// minecraft-data -- the data mineflayer gets its protocol, item and window definitions from -- has no
// 26.2 data: the newest data directory it ships is 26.1, protocol 775, which is also the newest version
// mineflayer itself lists as tested. So this client is a 26.1 client and says so honestly: it sends
// protocol 775 in the handshake and nothing rewrites it, and the server translates.
//
// Translating is `clientTest`'s job, not this file's: that task copies ViaVersion and ViaBackwards into
// the server's `plugins/` for its run, so Paper 26.2 talks 26.2 to Via and Via talks 26.1 to this
// client. Nothing here lies about its version, which is what lets the login succeed as a normal 26.1
// login, and it is also what makes the item types below assertable: ViaBackwards rewrites the item
// registry 26.2 -> 26.1, so the ids that arrive are 26.1 ids and `prismarine-item`'s 26.1 table names
// the item the server actually put in the slot. Without the layer, a 26.1 client decodes 26.2's ids with
// the wrong table -- 26.2 inserted items, so 26.2's diamond id (926) reads as `stone_hoe` here.
//
// What that still does not prove is in `build.gradle.kts` next to the `clientTest` task: this is a 26.1
// client seeing Via's translation of the addon's packets, so it shows the packets survive a real
// translation layer rather than what a real 26.2 client would see.

import mineflayer from 'mineflayer'
import { createRequire } from 'node:module'

// prismarine-item is mineflayer's own dependency, and what mineflayer itself uses to turn its item
// objects into the shape a packet wants. The drag scenario below sends raw packets (mineflayer's window
// model has no drag and refuses to build one), so it needs that conversion for `cursorItem` too.
const require = createRequire(import.meta.url)
const prismarineItem = require('prismarine-item')

const HOST = process.env.XIAOJIE_CLIENT_HOST ?? '127.0.0.1'
// The port `server-test/server.properties` sets. The task that runs this bot starts a server from that
// file, so the two agree; the variable is there for driving the bot against another copy of the server.
const PORT = Number(process.env.XIAOJIE_CLIENT_PORT ?? 25598)
const USERNAME = 'SelftestBot'
// The version this client speaks. It is what mineflayer picks its data from, what it states in its
// handshake, and -- because the run puts Via in front of the server -- what the server ends up
// translating for.
const DATA_VERSION = '26.1'
const CLICK_PAUSE_MS = 150
const STEP_TIMEOUT_MS = 30_000
const RUN_TIMEOUT_MS = 90_000

// The client slots of page 1 of the scenario's menu, the item each holds, the label it carries, and the
// click that follows it. The layout is "ABCN     ", so these are the first four cells of a chest's first
// row.
//
// `type` is the item id the server sent, read through this client's own 26.1 registry, and it is the
// assertion the translation layer makes possible: it names the item the menu mapped to that key, which
// is a fact about the addon rather than about the label the addon also wrote on it. `label` is the item's
// `custom_name` component, which is a string and so survives any translation; it is kept because it says
// the *right* item arrived with a name the client can read, and because it is the half of "which item"
// that does not depend on either version's registry at all.
//
// The two click numbers are the protocol's: `mode` is the inventory operation (0 a normal click, 1 a
// shift click, 2 a number key) and `button` is which button -- the mouse button for a normal click, and
// the hotbar index 0..8 for a number key, so button 2 is number key 3. `bot.clickWindow` takes them in
// the same order as the packet: slot, button, mode.
const PAGE_ONE = [
  { slot: 0, type: 'minecraft:stone', label: 'slot-a-stone', click: 'a plain left click', button: 0, mode: 0 },
  { slot: 1, type: 'minecraft:diamond', label: 'slot-b-diamond', click: 'a shift click', button: 0, mode: 1 },
  { slot: 2, type: 'minecraft:emerald', label: 'slot-c-emerald', click: 'number key 3', button: 2, mode: 2 },
  { slot: 3, type: 'minecraft:clock', label: 'slot-n-clock', click: 'the page 2 button', button: 0, mode: 0 }
]
const PAGE_ONE_TITLE = 'Client Page One'
const PAGE_TWO_TITLE = 'Client Page Two'
const PAGE_TWO_SLOT = 0
const PAGE_TWO_TYPE = 'minecraft:apple'
const PAGE_TWO_LABEL = 'slot-z-apple'

class CheckFailed extends Error {}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

function log (message) {
  console.log(`[client] ${message}`)
}

function check (condition, message) {
  if (!condition) throw new CheckFailed(message)
}

/** Fails unless [actual] is [expected], naming what was being looked at in the message. */
function checkEqual (what, expected, actual) {
  check(
    actual === expected,
    `${what}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`
  )
}

/** Waits for [predicate] to return something other than null, undefined or false. */
async function waitFor (what, predicate, timeoutMs = STEP_TIMEOUT_MS) {
  const deadline = Date.now() + timeoutMs
  for (;;) {
    const value = predicate()
    if (value !== undefined && value !== null && value !== false) return value
    if (Date.now() > deadline) {
      throw new CheckFailed(`timed out after ${timeoutMs} ms waiting for ${what}`)
    }
    await sleep(50)
  }
}

/**
 * The text of a chat component, whichever of its shapes it arrives in: a plain string, a prismarine-chat
 * message, a `{ text, extra }` component, or a prismarine-nbt value, which wraps every field as
 * `{ type, value }`. Window titles and item names both arrive as components.
 */
function flattenText (value) {
  if (value === null || value === undefined) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  if (typeof value.toMotd === 'function') {
    const text = value.toMotd()
    if (typeof text === 'string') return text
  }
  if (value.type !== undefined && value.value !== undefined) return flattenText(value.value)
  if (Array.isArray(value)) return value.map(flattenText).join('')
  if (typeof value === 'object') {
    const parts = []
    if (value.text !== undefined) parts.push(flattenText(value.text))
    for (const extra of value.extra ?? []) parts.push(flattenText(extra))
    if (parts.length > 0) return parts.join('')
    try {
      return JSON.stringify(value)
    } catch {
      return ''
    }
  }
  return String(value)
}

/** The window's title as text. */
function titleOf (window) {
  return flattenText(window?.title)
}

/**
 * The label the item in a slot carries, from its `custom_name` component, or null when the slot is empty
 * or its item has no name. Read by value, so it survives translation between versions; kept alongside
 * [typeOf] because it says what the addon *called* the icon, not only which item it used.
 */
function labelOf (item) {
  if (item === null || item === undefined) return null
  for (const component of item.components ?? []) {
    const type = String(component?.type ?? '').replace(/^minecraft:/, '')
    if (type === 'custom_name') return flattenText(component.data)
  }
  return null
}

/**
 * The item type in a slot as this client's own registry names it -- `minecraft:diamond` -- or null when
 * the slot is empty. `prismarine-item` answers this from the id the server sent, so it only means
 * anything because ViaBackwards rewrote that id into 26.1's registry (see the header).
 */
function typeOf (item) {
  if (item === null || item === undefined) return null
  const name = item.name
  if (typeof name !== 'string' || name.length === 0 || name === 'unknown') return null
  return name.includes(':') ? name : `minecraft:${name}`
}

/** A readable one-line description of a window, for the run's own output. */
function describeWindow (window) {
  const raw = JSON.stringify(window.title)
  return (
    `type=${window.type} slots=${window.slots.length} title=${JSON.stringify(titleOf(window))}` +
    ` (as sent: ${raw.length > 120 ? `${raw.slice(0, 120)}…` : raw})`
  )
}

/** The first row of the container region as item types and labels, so the report shows what arrived. */
function describeFirstRow (window) {
  const row = []
  for (let slot = 0; slot < 9; slot++) {
    const item = window?.slots?.[slot]
    row.push(item ? `${typeOf(item)} "${labelOf(item) ?? ''}" (id ${item.type})` : '-')
  }
  return row.join(', ')
}

async function main () {
  log(`connecting to ${HOST}:${PORT} as ${USERNAME} (a ${DATA_VERSION} client, no handshake patch)`)

  const bot = mineflayer.createBot({
    host: HOST,
    port: PORT,
    username: USERNAME,
    auth: 'offline',
    version: DATA_VERSION,
    // Paper logs the client's own errors instead of hiding them; a failure only visible there is exactly
    // what this layer exists to find.
    hideErrors: false
  })

  // What ended the run, and whether it has ended: the client's own events can end it from outside the
  // steps below, and only the first failure is reported. `aborted` is what lets one of those events stop a
  // `waitFor` that is still polling -- without it, a kicked client would wait for a window that can no
  // longer arrive and report a timeout instead of the kick.
  let failure = null
  let finished = false
  let reportFailure = () => {}
  const aborted = new Promise((_, reject) => {
    reportFailure = reject
  })
  // The rejection is read through the race below; this keeps Node from reporting it as unhandled when the
  // scenario finished first and nothing is awaiting it any more.
  aborted.catch(() => {})

  function fail (message) {
    if (finished) return
    failure = new CheckFailed(message)
    finished = true
    reportFailure(failure)
    try {
      bot.quit('client test failed')
    } catch {
      // The client is already gone; the failure recorded above is the one worth reporting.
    }
  }

  // Every window the server shows this client, in order: the menu the plugin opens on join and then the
  // same window id re-opened with the title of page 2 after the page turn.
  const windows = []
  bot.on('windowOpen', (window) => {
    windows.push(window)
    log(`window ${windows.length}: ${describeWindow(window)}`)
    log(`  first row: ${describeFirstRow(window)}`)
  })
  bot.on('kicked', (reason) => fail(`the server kicked the client: ${JSON.stringify(reason)}`))
  bot.on('error', (error) => fail(`the client failed: ${error.message}`))
  bot.on('end', () => fail('the server closed the connection before the scenario finished'))

  const overallTimeout = setTimeout(
    () => fail(`the scenario did not finish within ${RUN_TIMEOUT_MS} ms`),
    RUN_TIMEOUT_MS
  )

  try {
    await Promise.race([scenario(bot, windows), aborted])
    finished = true
    log('all assertions passed')
    bot.quit('client test finished')
  } catch (error) {
    failure = failure ?? error
    finished = true
  } finally {
    clearTimeout(overallTimeout)
  }

  if (failure !== null) {
    console.error(`[client] FAIL: ${failure.message}`)
    // Give the failed run's output a moment to reach the file the task reads.
    await sleep(100)
    process.exit(1)
  }
}

/** The scenario itself: what the client has to be shown, and the clicks it has to make. */
async function scenario (bot, windows) {
  await waitFor('the client to log in', () => bot.entity != null)
  log(`logged in as ${bot.username}`)

  // 1. The menu the server opened for this player, and the items the layout puts in it.
  const menu = await waitFor('the menu window', () =>
    windows.find((window) => titleOf(window).includes(PAGE_ONE_TITLE))
  )
  check(
    titleOf(menu).includes(PAGE_ONE_TITLE),
    `the menu's title: expected it to contain ${JSON.stringify(PAGE_ONE_TITLE)}, got ${JSON.stringify(titleOf(menu))}`
  )
  log(`the menu arrived with title ${JSON.stringify(titleOf(menu))}`)

  for (const { slot, type, label } of PAGE_ONE) {
    // The type first: it is the item the addon mapped to the key, named by this client's own registry,
    // and the only one of the two checks that can be wrong about *which item* arrived rather than about
    // what it was called.
    checkEqual(`the type of the item in slot ${slot}`, type, typeOf(menu.slots[slot]))
    checkEqual(`the label of the item in slot ${slot}`, label, labelOf(menu.slots[slot]))
    checkEqual(`the stack size of the item in slot ${slot}`, 1, menu.slots[slot]?.count)
  }
  // Four different items, not the same one written four times: a translation layer that collapsed the
  // registry would still pass the per-slot checks above only by coincidence, and this catches it.
  const types = PAGE_ONE.map(({ slot }) => typeOf(menu.slots[slot]))
  checkEqual('the number of distinct item types in slots 0 to 3', PAGE_ONE.length, new Set(types).size)
  log(`slots 0 to 3 hold four distinct items: ${types.join(', ')}`)

  // The rest of the first row is empty: an item that lost its slot, or one that appeared from nowhere,
  // changes this. Together with the four above it pins the whole row.
  for (let slot = PAGE_ONE.length; slot < 9; slot++) {
    checkEqual(`the item in slot ${slot}`, null, menu.slots[slot])
  }
  log('page 1 shows exactly the four items the layout puts there, in slots 0 to 3')

  // 2. The clicks the server records. `11-client.sk` names the same clicks in its detail lines, so the
  //    two halves of the scenario describe one set of three clicks.
  await click(bot, PAGE_ONE[0])
  await click(bot, PAGE_ONE[1])
  await click(bot, PAGE_ONE[2])

  // 3. The click that turns the page, and the window the client is shown afterwards. The server keeps the
  //    same window id, so this is a second window rather than a changed one, and only the client can say
  //    that it still shows a menu, and with which title.
  const windowsBefore = windows.length
  await click(bot, PAGE_ONE[3])

  const turned = await waitFor('the window of page 2', () =>
    windows.slice(windowsBefore).find((window) => titleOf(window).includes(PAGE_TWO_TITLE))
  )
  checkEqual('the type of the item in slot 0 after the page turn', PAGE_TWO_TYPE, typeOf(turned.slots[PAGE_TWO_SLOT]))
  checkEqual('the label of the item in slot 0 after the page turn', PAGE_TWO_LABEL, labelOf(turned.slots[PAGE_TWO_SLOT]))
  checkEqual('the item in slot 1 after the page turn', null, turned.slots[1])
  log(
    `the page turn reached the client: title ${JSON.stringify(titleOf(turned))}, ` +
      `slot ${PAGE_TWO_SLOT} carries ${typeOf(turned.slots[PAGE_TWO_SLOT])} ` +
      `named ${JSON.stringify(labelOf(turned.slots[PAGE_TWO_SLOT]))}`
  )
  log(
    'those item types are what `11-client.sk` maps its keys to, read through this client\'s 26.1 ' +
      'registry: ViaBackwards rewrote the 26.2 ids into it before they arrived'
  )

  // 4. The drag half, in a menu of its own, opened by a command rather than on join so that it cannot
  //    put a second window in front of the first half of the run.
  await dragScenario(bot, windows)
}

/**
 * What a drag does, which is the one thing about a click a client has to be involved in.
 *
 * A drag is not a packet: it is a run of click packets whose *button* carries the phase and the button
 * the drag was made with (low two bits: 0 start, 1 continue, 2 end; high bits: 0 left, 1 right, 2
 * middle). The game collects the slots from the continue packets and applies the whole thing at the end,
 * and that is why the same gesture can mean two different things: a cursor holding two items fills two
 * slots, and a cursor holding one can only fill one, which the game then delivers as an ordinary click.
 *
 * mineflayer's high-level `clickWindow` cannot send any of this -- its window model has no drag and
 * throws `invalid operation` -- so the packets go out raw here, each step followed by the `_syncWindow`
 * mineflayer exposes for exactly that, so its model of the window matches the server's again.
 *
 * The four assertions, in order: a drag onto the two protected slots is refused and both keep their
 * item; the same drag onto free slots happens and fills both; a drag of a single item fills one; and a
 * click on a protected slot is refused the same way a drag is.
 */
async function dragScenario (bot, windows) {
  const DRAG_TITLE = 'Drag Test'
  const PROTECTED = [
    { slot: 0, type: 'minecraft:diamond', label: 'drag-a' },
    { slot: 1, type: 'minecraft:emerald', label: 'drag-b' }
  ]

  const before = windows.length
  bot.chat('/dragtest')
  const window = await waitFor('the drag test window', () =>
    windows.slice(before).find((opened) => titleOf(opened).includes(DRAG_TITLE))
  )
  log(`the drag menu arrived with title ${JSON.stringify(titleOf(window))}`)
  log(`  first row: ${describeFirstRow(window)}`)

  /** Fails unless the first slots of the container hold exactly the types and labels given. */
  function checkRow (what, expected) {
    expected.forEach(([type, label], slot) => {
      checkEqual(`the type of the item in slot ${slot} ${what}`, type, typeOf(window.slots[slot]))
      if (label !== null) {
        checkEqual(`the label of the item in slot ${slot} ${what}`, label, labelOf(window.slots[slot]))
      }
    })
  }

  const Item = prismarineItem(bot.registry)
  function rawClick (slot, button, mode) {
    bot._client.write('window_click', {
      windowId: window.id,
      stateId: window.stateId ?? -1,
      slot,
      mouseButton: button,
      mode,
      changedSlots: [],
      cursorItem: Item.toNotch(window.selectedItem)
    })
  }

  /** Waits for the server's answer to the last packets, then re-reads the window through mineflayer. */
  async function settle (what) {
    await sleep(400)
    await bot._syncWindow(window)
    log(`  after ${what}: ${describeFirstRow(window)}`)
  }

  /** One left-button drag: a start, one continue per slot, then the end. */
  async function drag (slots) {
    rawClick(slots[0], 0, 5)
    await sleep(150)
    for (const slot of slots) {
      rawClick(slot, 1, 5)
      await sleep(150)
    }
    rawClick(slots[0], 2, 5)
  }

  checkRow('at open', [
    [PROTECTED[0].type, PROTECTED[0].label],
    [PROTECTED[1].type, PROTECTED[1].label],
    ['minecraft:stone', 'drag-two'],
    ['minecraft:apple', 'drag-one']
  ])

  // 1. The stack of two, dragged onto the two goods: slot 1 holds an emerald, which cannot take stone, so
  //    the game collects only slot 0 and delivers that one as a click -- and the script refuses it. Only a
  //    client can send a drag at all, which is why this scenario is here and not in the unit tests.
  await bot.clickWindow(2, 0, 0)
  await settle('picking up the stack of two stone')
  await drag([0, 1])
  await settle('dragging that stack onto the protected slots 0 and 1')
  checkRow('after the refused drag on the goods', [
    [PROTECTED[0].type, PROTECTED[0].label],
    [PROTECTED[1].type, PROTECTED[1].label]
  ])

  // 2. The same stack onto slot 5, which is empty and reserved: both slots can take the item, so this one
  //    really is a drag, and refusing it has to refuse every slot it reached at once.
  await drag([5, 6])
  await settle('dragging that stack onto the protected slots 5 and 6')
  checkEqual('the item in slot 5 after the refused drag', null, window.slots[5])
  checkEqual('the item in slot 6 after the refused drag', null, window.slots[6])

  // 3. The same drag onto free slots: it happens, and one item of the stack lands in each of them.
  //    `selectedItem` is mineflayer's view of the cursor, so this also checks that a refused drag left
  //    the stack where it was.
  if (window.selectedItem === null) {
    await bot.clickWindow(2, 0, 0)
    await settle('picking the stack up again, the refusals having left it in its slot')
  }
  await drag([13, 14])
  await settle('dragging the stack over the free slots 13 and 14')
  checkEqual('the type in slot 13 after the drag', 'minecraft:stone', typeOf(window.slots[13]))
  checkEqual('the type in slot 14 after the drag', 'minecraft:stone', typeOf(window.slots[14]))
  checkEqual('the count in slot 13 after the drag', 1, window.slots[13]?.count)
  checkEqual('the count in slot 14 after the drag', 1, window.slots[14]?.count)

  // 4. A single item dragged over two slots fills one of them: the game delivers that as a click, and the
  //    window is the same either way, which is what lets a script written for clicks keep working.
  await bot.clickWindow(3, 0, 0)
  await settle('picking up the single apple')
  await drag([11, 12])
  await settle('dragging one item over the free slots 11 and 12')
  checkEqual('the type in slot 11 after the one-item drag', 'minecraft:apple', typeOf(window.slots[11]))
  checkEqual('the item in slot 12 after the one-item drag', null, window.slots[12])

  log('the drag scenario passed: refused drags leave every slot alone, and accepted ones fill every slot they reached')
}

/** Clicks one slot and reports it, naming the mode and button the protocol carries for that click. */
async function click (bot, target) {
  await bot.clickWindow(target.slot, target.button, target.mode)
  log(
    `clicked slot ${target.slot} (${target.label}) with ${target.click} ` +
      `(mode ${target.mode}, button ${target.button})`
  )
  // The server refreshes the window after every click; a short pause keeps the packet order readable in
  // the log and lets each click's detail line be written before the next one arrives.
  await sleep(CLICK_PAUSE_MS)
}

main().catch(async (error) => {
  console.error(`[client] FAIL: unexpected error: ${error?.stack ?? error}`)
  await sleep(100)
  process.exit(1)
})
