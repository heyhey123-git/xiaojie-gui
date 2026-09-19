// The client layer of the test suite: a real Minecraft client that joins the disposable Paper server
// `clientTest` starts, sees the menu `server-test/skript/11-client.sk` opens for it, clicks that menu,
// and asserts what it was shown. It is run by the `clientTest` Gradle task and is not part of the plugin.
//
// What only this side can see: the window the client was given (its title, its type, the items in known
// slots) and the title it is shown after a page turn. What it cannot see is what the server did with a
// click; that half is logged by `11-client.sk` and asserted by the Gradle task.
//
// --- the protocol, and what it costs -----------------------------------------------------------------
//
// The server is Paper 26.2, protocol 776, and its status ping is off (`enable-status=false` in
// `server-test/server.properties`), so mineflayer cannot ask it what it speaks and has to be told.
// minecraft-data -- the data mineflayer gets its protocol, item and window definitions from -- has no
// 26.2 data: the newest data directory it ships is 26.1, protocol 775, and 26.2 appears only in its list
// of known versions. So this client speaks 26.1's packets and states 776 in the handshake, which is the
// number Paper checks, and every part of the scenario that 26.2 did not reshape works exactly as it does
// for a 26.2 client: login, the configuration phase, the window the plugin opens, clicks in it, and a
// second window title. Two play packets that 26.2 did reshape (login success and join game) are then
// parsed partially, which minecraft-protocol reports as "Chunk size is N but only M was read" and
// recovers from; the rest of the session is unaffected.
//
// The one thing this cannot fix is *which item* a slot holds. An item on the wire is a registry id, the
// item registry is part of the game rather than something a server sends, and 26.2 inserted items into
// it (26.1's diamond is id 899, 26.2's is 926), so a 26.1 client decodes 26.2's ids with the wrong table
// and names the wrong item -- `stone_hoe` where the server put a diamond. That is not a bug in this bot
// and not something a test can paper over: no released client data names 26.2's items. Translating on
// the server side instead is not an option either: ViaVersion 5.12.0 resets the socket of every client
// it has to translate against Paper 26.2, which was tried and observed.
//
// Item identity therefore travels as the item's `custom_name` component -- a string, read by value and
// identical for a 26.1 and a 26.2 client -- and `11-client.sk` names every icon it maps. The client
// asserts the label it was shown in each known slot, which is the item the menu decided to put there,
// and says plainly in the run's output that the item's type is what the label claims rather than
// something this client can look up.

import mineflayer from 'mineflayer'

const HOST = process.env.XIAOJIE_CLIENT_HOST ?? '127.0.0.1'
// The port `server-test/server.properties` sets. The task that runs this bot starts a server from that
// file, so the two agree; the variable is there for driving the bot against another copy of the server.
const PORT = Number(process.env.XIAOJIE_CLIENT_PORT ?? 25598)
const USERNAME = 'SelftestBot'
// The version whose protocol data the client uses, and the protocol number the server is told.
const DATA_VERSION = '26.1'
const PROTOCOL_VERSION = 776
const CLICK_PAUSE_MS = 150
const STEP_TIMEOUT_MS = 30_000
const RUN_TIMEOUT_MS = 90_000

// The client slots of page 1 of the scenario's menu, the label each item carries, and the click that
// follows it. The layout is "ABCN     ", so these are the first four cells of a chest's first row.
//
// The two click numbers are the protocol's: `mode` is the inventory operation (0 a normal click, 1 a
// shift click, 2 a number key) and `button` is which button -- the mouse button for a normal click, and
// the hotbar index 0..8 for a number key, so button 2 is number key 3. `bot.clickWindow` takes them in
// the same order as the packet: slot, button, mode.
const PAGE_ONE = [
  { slot: 0, label: 'slot-a-stone', click: 'a plain left click', button: 0, mode: 0 },
  { slot: 1, label: 'slot-b-diamond', click: 'a shift click', button: 0, mode: 1 },
  { slot: 2, label: 'slot-c-emerald', click: 'number key 3', button: 2, mode: 2 },
  { slot: 3, label: 'slot-n-clock', click: 'the page 2 button', button: 0, mode: 0 }
]
const PAGE_ONE_TITLE = 'Client Page One'
const PAGE_TWO_TITLE = 'Client Page Two'
const PAGE_TWO_SLOT = 0
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
 * or its item has no name. This is the only part of an item this client can read that does not depend on
 * the item registry, which it has no 26.2 data for (see the header).
 */
function labelOf (item) {
  if (item === null || item === undefined) return null
  for (const component of item.components ?? []) {
    const type = String(component?.type ?? '').replace(/^minecraft:/, '')
    if (type === 'custom_name') return flattenText(component.data)
  }
  return null
}

/** The item id the slot carries, or null when it is empty. */
function itemIdOf (window, slot) {
  return window?.slots?.[slot]?.type ?? null
}

/** A readable one-line description of a window, for the run's own output. */
function describeWindow (window) {
  const raw = JSON.stringify(window.title)
  return (
    `type=${window.type} slots=${window.slots.length} title=${JSON.stringify(titleOf(window))}` +
    ` (as sent: ${raw.length > 120 ? `${raw.slice(0, 120)}…` : raw})`
  )
}

/** The first row of the container region as item labels, so the report shows what the client saw. */
function describeFirstRow (window) {
  const row = []
  for (let slot = 0; slot < 9; slot++) {
    const item = window?.slots?.[slot]
    row.push(item ? `${labelOf(item) ?? `<id ${item.type}>`} (id ${item.type})` : '-')
  }
  return row.join(', ')
}

async function main () {
  log(`connecting to ${HOST}:${PORT} as ${USERNAME} (data ${DATA_VERSION}, handshake protocol ${PROTOCOL_VERSION})`)

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

  // minecraft-protocol fills `options.protocolVersion` from the data version (775) and writes the
  // handshake when the socket connects, which happens on a later tick: replacing the client's `write`
  // here, synchronously after `createBot` returns, catches the handshake before it leaves. Without this,
  // Paper answers "Outdated client!" and the bot never gets in.
  const clientWrite = bot._client.write.bind(bot._client)
  bot._client.write = (name, params) => {
    if (name === 'set_protocol') return clientWrite(name, { ...params, protocolVersion: PROTOCOL_VERSION })
    return clientWrite(name, params)
  }

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

  for (const { slot, label } of PAGE_ONE) {
    checkEqual(`the label of the item in slot ${slot}`, label, labelOf(menu.slots[slot]))
    checkEqual(`the stack size of the item in slot ${slot}`, 1, menu.slots[slot]?.count)
  }
  // Four different items, not the same one written four times: this is the part of "which item" a client
  // without 26.2 item data can still check, because the registry ids come from the server.
  const ids = PAGE_ONE.map(({ slot }) => itemIdOf(menu, slot))
  checkEqual('the number of distinct item ids in slots 0 to 3', PAGE_ONE.length, new Set(ids).size)
  log(`slots 0 to 3 hold four distinct items with ids ${ids.join(', ')}`)

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
  checkEqual('the item in slot 0 after the page turn', PAGE_TWO_LABEL, labelOf(turned.slots[PAGE_TWO_SLOT]))
  checkEqual('the item in slot 1 after the page turn', null, turned.slots[1])
  log(
    `the page turn reached the client: title ${JSON.stringify(titleOf(turned))}, ` +
      `slot ${PAGE_TWO_SLOT} carries ${JSON.stringify(labelOf(turned.slots[PAGE_TWO_SLOT]))}`
  )
  log('the item names above are the labels `11-client.sk` gives its icons: this client has no 26.2 item data')
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
