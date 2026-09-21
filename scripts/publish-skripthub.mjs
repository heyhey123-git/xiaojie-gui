#!/usr/bin/env node
// Publishes the generated SkriptHub documentation to skripthub.net.
//
//   ./gradlew gendocs
//   SKRIPTHUB_TOKEN=<token> node scripts/publish-skripthub.mjs [--dry-run]
//
// The dashboard imports the whole JSON by hand, and that stays the way to publish examples; this script
// is for the syntax itself, so a release does not have to wait for someone to paste a file. The API has no
// call that replaces a whole document -- it updates one element per request, and takes a list only when the
// elements are new -- so this is a diff: it reads what SkriptHub has, updates the elements whose title,
// pattern, description or since version changed, creates the ones that are not there yet, and reports the
// ones it will not touch.
//
// An element is recognised by the documentation tool's own id (`json_id` on SkriptHub) as well as by its
// title, because a renamed element is the case that goes wrong otherwise: the annotations carry the new
// name, the site still has the old one, and a title-only diff reads it as new and asks for a second row
// with an id the site has already given away (`Json id already exists`, HTTP 400).
//
// Every call and field below is the v1 API as its own documentation defines it, at
// https://skripthub.net/api/docs/ (a Swagger document): GET /api/v1/addon/, GET /api/v1/syntax/?addon=,
// PUT /api/v1/syntax/<id>/, POST /api/v1/syntax/ (a list) and GET /api/v1/syntaxexample/?syntax=. The
// writes carry the four fields POST /syntax/ requires -- title, syntax_pattern, required_plugins and
// addon -- plus the optional ones the generated file owns.
//
// What it deliberately does not do:
//
//   - Delete. An element that is on SkriptHub and not in the generated file is reported, not removed:
//     the annotations cannot say whether it was renamed, dropped, or belongs to an entry somebody made by
//     hand, and SkriptHub has `mark as removed` for the case where it was really dropped.
//   - Write examples. They live in their own endpoint, SkriptHub joins the examples of one element with a
//     single newline where SkriptHubDocsTool leaves a blank line, and the dashboard import handles them
//     as a set. A difference is reported so that it is a decision instead of a surprise.
//   - Set supporting plugins. The generated file does not carry them for the elements it lists here;
//     SkriptHub's own import cannot either, and its documentation says they are set by hand.
//
// The token is read from SKRIPTHUB_TOKEN and never written or printed. `SKRIPTHUB_ADDON` overrides which
// SkriptHub addon to publish to; without it the addon is found by the repository url, because the name on
// SkriptHub is not the plugin's name and renaming it there must not break this.

import { appendFileSync, readFileSync } from 'node:fs'

const API = 'https://skripthub.net/api/v1'
const DOCUMENT = process.env.SKRIPTHUB_DOCS ?? 'build/skripthub/xiaojie-gui.json'
const REPOSITORY = process.env.GITHUB_REPOSITORY ?? 'heyhey123-git/xiaojie-gui'
const TOKEN = process.env.SKRIPTHUB_TOKEN
const dryRun = process.argv.includes('--dry-run')

/** The kinds the documentation tool writes, and the singular name the API gives the same kind. */
const KINDS = ['events', 'conditions', 'effects', 'expressions', 'types', 'functions', 'sections', 'structures']

const failures = []
const lines = []

const say = (line = '') => {
  console.log(line)
  lines.push(line)
}

const headers = () => ({
  Authorization: 'Token ' + TOKEN,
  Accept: '*/*',
  'Content-Type': 'application/json'
})

/**
 * Reads a response as JSON.
 *
 * The site sometimes appends an HTML comment to what it returns, which is not JSON, and a body that is
 * not JSON at all is handed back as text so the caller can report it as the server sent it.
 */
const readJson = async (response) => {
  const text = await response.text()
  for (const candidate of [text, text.replace(/<!--[\s\S]*?-->/g, '')]) {
    try {
      return JSON.parse(candidate)
    } catch {
      // The next form, or the text itself below.
    }
  }
  return text
}

const request = async (method, path, body) => {
  const response = await fetch(API + path, {
    method,
    headers: headers(),
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const payload = await readJson(response)
  if (!response.ok) {
    const detail = typeof payload === 'object' ? JSON.stringify(payload) : String(payload)
    const error = new Error(method + ' ' + path + ' answered ' + response.status + ': ' + detail.slice(0, 400))
    // The caller can do more with the body than the message does: `POST /syntax/` answers one error object
    // per element of the list it was given, which is what lets a refused element be named.
    error.status = response.status
    error.payload = payload
    throw error
  }
  return payload
}

const list = (payload) => (Array.isArray(payload) ? payload : (payload.results ?? []))

/** Everything the generated file says, keyed by the title SkriptHub knows the element by. */
const readDocument = () => {
  const document = JSON.parse(readFileSync(DOCUMENT, 'utf8'))
  const entries = new Map()
  for (const kind of KINDS) {
    for (const entry of document[kind] ?? []) {
      entries.set(entry.name, {
        title: entry.name,
        // The id the documentation tool wrote for this element, which SkriptHub keeps in `json_id`.
        jsonId: entry.id,
        syntaxType: kind.replace(/s$/, ''),
        description: (entry.description ?? []).join('\n'),
        pattern: (entry.patterns ?? []).join('\n'),
        since: (entry.since ?? [])[0],
        examples: (entry.examples ?? []).map(withoutBlankEdges)
      })
    }
  }
  return { version: document.metadata?.version ?? 'unknown', entries }
}

/** An example without blank lines at either end, the way the gendocs task leaves it. */
function withoutBlankEdges(example) {
  const kept = String(example).replace(/\r\n/g, '\n').split('\n')
  while (kept.length > 1 && kept[0].trim() === '') kept.shift()
  while (kept.length > 1 && kept[kept.length - 1].trim() === '') kept.pop()
  return kept.join('\n')
}

/**
 * The addon to publish to.
 *
 * Found by url rather than named here: SkriptHub calls an addon by a name the dashboard owns, which is
 * not the plugin's name and can be changed there without this repository hearing about it, so a constant
 * would fail on the day it did. The repository url is what both sides keep.
 */
const resolveAddon = async () => {
  if (process.env.SKRIPTHUB_ADDON) return process.env.SKRIPTHUB_ADDON

  const addons = await request('GET', '/addon/')
  const slug = REPOSITORY.split('/').pop().toLowerCase()
  const match = addons.find((addon) => (addon.url ?? '').toLowerCase().includes(slug))
  if (!match) {
    throw new Error(
      'no SkriptHub addon links to ' + REPOSITORY + '. Register the addon in the dashboard, or set ' +
      'SKRIPTHUB_ADDON to the name SkriptHub knows it by.'
    )
  }
  return match.name
}

/**
 * The body of a write: the fields the generated file owns, plus what the existing row must keep.
 *
 * `addon` is one of the four the API requires of a write, and the token does not say which addon a syntax
 * belongs to, so it is sent every time. The value an existing row carries is what SkriptHub itself wrote
 * and is preferred to the name this script resolved; only an addon with nothing on SkriptHub yet has no
 * row to copy from.
 */
const bodyFor = (entry, row, addon) => {
  const body = {
    title: entry.title,
    description: entry.description,
    syntax_pattern: entry.pattern,
    syntax_type: entry.syntaxType,
    addon: row?.addon ?? addon,
    // The one field SkriptHub cannot infer: a new element has none, so it starts empty and is filled in
    // by hand where an element needs another plugin loaded.
    required_plugins: (row?.required_plugins ?? []).map((plugin) => (typeof plugin === 'string' ? plugin : plugin.name))
  }
  if (entry.since) body.compatible_addon_version = entry.since
  // SkriptHub stores the documentation tool's own element id in `json_id`, and rows that came from the
  // dashboard's JSON import carry it (`CondHasGUI`, `ExprVersion`). Sending it is what lets an element this
  // script creates be recognised by a later JSON import instead of being added a second time beside it.
  if (entry.jsonId) body.json_id = entry.jsonId
  if (row?.compatible_minecraft_version != null) body.compatible_minecraft_version = row.compatible_minecraft_version
  if (row?.type_usage != null) body.type_usage = row.type_usage
  if (row?.return_type != null) body.return_type = row.return_type
  if (row?.event_values != null) body.event_values = row.event_values
  if (row?.event_cancellable != null) body.event_cancellable = row.event_cancellable
  if (row?.keywords != null) body.keywords = row.keywords
  return body
}

const same = (left, right) => (left ?? '').trim() === (right ?? '').trim()

/**
 * The SkriptHub row one element of the generated file is, or null while the site does not have it.
 *
 * Two things identify an element and both are needed. The title is what a reader sees and what this script
 * reads back; `json_id` is the id the documentation tool wrote, which SkriptHub kept when the row was made
 * -- by the dashboard import or by an earlier run of this script. A renamed element has one of each: the
 * annotations carry `Player's Menu Window` while the site still says `GUI of Player`, so a title-only match
 * reads it as new and `POST /syntax/` refuses the whole list with `Json id already exists`. The id is what
 * survives the rename, and the title travels with the rest of the row in the update that follows.
 *
 * A row matching on both wins over one matching either: the site can hold the same element twice (two rows
 * titled `Slot Key from Menu` are on it today), and the row that already carries this title is the one to
 * keep, so the other is reported as left alone instead of being written over.
 */
const rowFor = (entry, rows) => {
  const byJsonId = rows.filter((row) => entry.jsonId != null && row.json_id === entry.jsonId)
  const byTitle = rows.filter((row) => row.title === entry.title)
  return byTitle.find((row) => byJsonId.includes(row)) ?? byJsonId[0] ?? byTitle[0] ?? null
}

/**
 * What the rows are matched by, as one word for the summary.
 *
 * `json_id` is what the API is asked for -- it is a field of a row on the public element list, so this is
 * expected rather than hoped for. A listing that does not carry it leaves title matching, which cannot see a
 * rename; the summary then says so, and a create refused with `Json id already exists` is the symptom.
 */
const identityBasis = (rows) => (rows.length > 0 && !rows.some((row) => 'json_id' in row) ? 'title only' : 'json_id')

const plural = (count, one) => count + ' ' + one + (count === 1 ? '' : 's')

/**
 * The fields of a row this script owns, in the order a difference is reported.
 *
 * The title is one of them: a rename is written the same way any other change is, by sending the whole row.
 */
const differencesIn = (row, entry) => {
  const changed = []
  for (const [field, left, right] of [
    ['title', row.title, entry.title],
    ['pattern', row.syntax_pattern, entry.pattern],
    ['description', row.description, entry.description],
    ['since', row.compatible_addon_version, entry.since ?? row.compatible_addon_version]
  ]) {
    if (!same(left, right)) changed.push(field)
  }
  return changed
}

const compare = (document, rows) => {
  const updates = []
  const creates = []
  /** The row each element of the generated file is, whether or not it has to be written. */
  const matched = new Map()
  /** The rows some element claimed, so that `left alone` means "no element of the file is this". */
  const claimed = new Set()
  for (const entry of document.entries.values()) {
    // A row another element of the file already claimed is not a candidate: the site can hold one element
    // twice, and writing the same row twice would lose one of the two names instead of reporting it.
    const row = rowFor(entry, rows.filter((candidate) => !claimed.has(candidate.id)))
    if (!row) {
      creates.push(entry)
      continue
    }
    matched.set(entry.title, row)
    claimed.add(row.id)
    const changed = differencesIn(row, entry)
    if (changed.length) updates.push({ entry, row, changed })
  }
  const leftAlone = rows.filter((row) => !claimed.has(row.id))
  return { updates, creates, leftAlone, matched }
}

/**
 * The elements a refused `POST /syntax/` would not create, by title.
 *
 * The endpoint takes the list of new elements and answers one error object per element of it, in the same
 * order, with an empty object where the element was accepted. Its own message for the call says only that it
 * answered 400, and what it says per element -- `Json id already exists` -- is the thing to act on, so that
 * is what the summary gets.
 */
const refusedCreates = (entries, error) => {
  const perItem = Array.isArray(error.payload) ? error.payload : []
  const named = entries
    .map((entry, index) => ({ entry, errors: perItem[index] }))
    .filter(({ errors }) => errors !== null && typeof errors === 'object' && Object.keys(errors).length > 0)
    .map(({ entry, errors }) => 'creating `' + entry.title + '`: ' + JSON.stringify(errors))
  return named.length ? named : ['creating ' + plural(entries.length, 'element') + ': ' + error.message]
}

/** The examples of one element as SkriptHub holds them, joined the way it joins them. */
const examplesOf = async (id) => {
  const payload = await request('GET', '/syntaxexample/?syntax=' + id)
  return list(payload).map((example) => withoutBlankEdges(example.example_code ?? ''))
}

if (!TOKEN) {
  console.error('SKRIPTHUB_TOKEN is not set. The token is on the SkriptHub API documentation page.')
  process.exit(1)
}

// Everything below reports through `failures` rather than throwing out of the script, so that a request
// that fails still leaves a summary saying what was and was not published.
try {
  const document = readDocument()
  const addon = await resolveAddon()
  const rows = list(await request('GET', '/syntax/?addon=' + encodeURIComponent(addon)))
  const plan = compare(document, rows)

  say('### SkriptHub documentation ' + document.version)
  say()
  say('| | |')
  say('| --- | --- |')
  say('| Addon | `' + addon + '` |')
  say('| Elements in the generated file | ' + document.entries.size + ' |')
  say('| Matched by | ' + identityBasis(rows) + ' |')
  say('| To update | ' + plan.updates.length + ' |')
  say('| To rename | ' + plan.updates.filter(({ changed }) => changed.includes('title')).length + ' |')
  say('| To create | ' + plan.creates.length + ' |')
  say('| Unchanged | ' + (document.entries.size - plan.updates.length - plan.creates.length) + ' |')
  say('| On SkriptHub only, left alone | ' + plan.leftAlone.length + ' |')
  say('| Mode | ' + (dryRun ? 'dry run, nothing was written' : '**published**') + ' |')
  say()

  for (const { entry, row, changed } of plan.updates) {
    say('- `' + entry.title + '` (id ' + row.id + '): ' + changed.join(', '))
  }
  for (const entry of plan.creates) say('- `' + entry.title + '`: new, will be created')
  for (const row of plan.leftAlone) say('- `' + row.title + '` (id ' + row.id + '): not in the generated file, left as it is')

  if (!dryRun) {
    for (const { entry, row } of plan.updates) {
      try {
        await request('PUT', '/syntax/' + row.id + '/', bodyFor(entry, row, addon))
      } catch (error) {
        failures.push(entry.title + ': ' + error.message)
      }
    }

    if (plan.creates.length) {
      // One call, because the endpoint takes a list; every element of it is created or none is.
      const body = plan.creates.map((entry) => bodyFor(entry, null, addon))
      try {
        await request('POST', '/syntax/', body)
      } catch (error) {
        failures.push(...refusedCreates(plan.creates, error))
      }
    }

    // Read back rather than trust the status codes: what was written is only published once the entry
    // says so, and a field the API silently ignored would otherwise be found by a reader.
    const after = list(await request('GET', '/syntax/?addon=' + encodeURIComponent(addon)))
    const remaining = compare(document, after)
    for (const { entry, changed } of remaining.updates) {
      failures.push(entry.title + ' still differs after the write: ' + changed.join(', '))
    }
    if (remaining.creates.length) {
      failures.push(
        'still not on SkriptHub after the write: ' +
          remaining.creates.map((entry) => entry.title).join(', ')
      )
    }
  }

  // Examples are compared last and loosely: SkriptHub joins several examples of one element with a single
  // newline where the tool leaves a blank line, so only a real difference is worth reporting.
  const join = (examples) => examples.map((example) => example.trim()).join('\n').replace(/\n\s*\n/g, '\n')
  for (const entry of document.entries.values()) {
    const row = plan.matched.get(entry.title)
    if (!row) continue
    try {
      if (join(await examplesOf(row.id)) !== join(entry.examples)) {
        say('- `' + entry.title + '` (id ' + row.id + '): its examples differ, which this script does not write — use the dashboard JSON import')
      }
    } catch (error) {
      failures.push('reading the examples of ' + entry.title + ': ' + error.message)
    }
  }
} catch (error) {
  failures.push(error instanceof Error ? error.message : String(error))
}

if (failures.length) {
  say()
  say('**' + failures.length + ' failure(s)**')
  for (const failure of failures) say('- ' + failure)
}

// The workflow appends this to the run summary, so it has to survive being read on its own.
if (process.env.GITHUB_STEP_SUMMARY) {
  appendFileSync(process.env.GITHUB_STEP_SUMMARY, lines.join('\n') + '\n')
}

// The exit code is set rather than exited with: `process.exit()` cuts a connection off while it is still
// closing, which Node turns into an assertion failure on Windows. Whatever is left of the connection is
// let go of on its own, and the code still reaches the shell and CI.
process.exitCode = failures.length ? 1 : 0
