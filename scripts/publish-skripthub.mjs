#!/usr/bin/env node
// Publishes the generated SkriptHub documentation to skripthub.net.
//
//   ./gradlew gendocs
//   SKRIPTHUB_TOKEN=<token> node scripts/publish-skripthub.mjs [--dry-run]
//
// The dashboard imports the whole JSON by hand, and that is what published this page once; from here on the
// script keeps it in step, so a release does not have to wait for someone to paste a file. The API has no
// call that replaces a whole document -- it updates one element per request, takes a list only when the
// elements are new, and keeps examples in an endpoint of their own -- so this is a diff: it reads what
// SkriptHub has, updates the elements whose title, pattern, description or since version changed, creates the
// ones that are not there yet, gives every element the examples its annotations carry, and reports the rows it
// will not touch.
//
// An element is recognised by the documentation tool's own id (`json_id` on SkriptHub) as well as by its
// title, because a renamed element is the case that goes wrong otherwise: the annotations carry the new
// name, the site still has the old one, and a title-only diff reads it as new and asks for a second row
// with an id the site has already given away (`Json id already exists`, HTTP 400).
//
// Every call and field below is the v1 API as its own documentation defines it, at
// https://skripthub.net/api/docs/ (a Swagger document): GET /api/v1/addon/, GET /api/v1/syntax/?addon=,
// PUT /api/v1/syntax/<id>/, POST /api/v1/syntax/ (a list), GET/POST /api/v1/syntaxexample/,
// DELETE /api/v1/syntaxexample/<id>/ and, for the documentation tool's id of each row, the public
// GET /api/v1/addonsyntaxlist/. The element writes carry the four fields POST /syntax/ requires -- title,
// syntax_pattern, required_plugins and addon -- plus the optional ones the generated file owns.
//
// What it deliberately does not do:
//
//   - Delete an element. A row that is on SkriptHub and not in the generated file is reported, not removed:
//     the annotations cannot say whether it was renamed, dropped, or belongs to an entry somebody made by
//     hand, and SkriptHub has `mark as removed` for the case where it was really dropped.
//   - Touch an example a reader submitted, or mark one official. Examples are written, one per element and
//     replaced whole when the text differs -- which is what keeps a `page 0` from an older release out of the
//     page. What may be replaced is an example the site marks official or one this account wrote; somebody
//     else's example is theirs, and is neither compared nor deleted. The **official mark** itself is the
//     dashboard's: that endpoint authenticates a browser session rather than an API token (with a bogus token
//     it answers exactly what it answers with no token at all, while the endpoints that do take the token
//     answer `Invalid token`), so the run reports how many examples are unmarked and leaves them as they are.
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

/**
 * Every route this script writes to, asked which methods it takes, before anything is written.
 *
 * The paths and bodies here were read off the site, and this is what keeps that honest: a route that does
 * not exist answers 404, while one that exists and wants a token answers 401 or 403 with an `Allow` header
 * that names the methods. That is how the mark's missing slash was found -- `/syntaxexample/officialexample/`
 * answers 404 where the form without the slash answers 403 and `POST, OPTIONS`, which is also what the
 * site's own "Mark as a official example" button posts to.
 *
 * A route that is wrong or that does not take the method the script needs is reported as a failure. The run
 * carries on, because the phases are independent and the messages say which one cannot work.
 */
const checkRoutes = async () => {
  const wanted = [
    ['GET', '/addon/'],
    ['GET', '/syntax/?addon=xiaojie-gui'],
    ['POST', '/syntax/'],
    ['PUT', '/syntax/1/'],
    ['GET', '/syntaxexample/?syntax=1'],
    ['POST', '/syntaxexample/'],
    ['DELETE', '/syntaxexample/1/'],
    ['GET', '/addonsyntaxlist/']
  ]
  const problems = []
  for (const [method, path] of wanted) {
    let response
    try {
      response = await fetch(API + path, { method: 'OPTIONS', headers: headers() })
    } catch (error) {
      problems.push(method + ' ' + path + ' could not be asked: ' + error.message)
      continue
    }
    if (response.status === 404) {
      problems.push('no route answers ' + path)
      continue
    }
    const allowed = response.headers.get('allow')
    if (allowed && !allowed.toUpperCase().split(/[,\s]+/).includes(method)) {
      problems.push(method + ' is not allowed on ' + path + ' (it takes ' + allowed + ')')
    }
  }
  return { problems, checked: wanted.length }
}

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
 * The rows SkriptHub has for one addon, each with the documentation tool's id on it.
 *
 * `GET /syntax/?addon=` is the listing the writes are planned from: it is the token's own view and it carries
 * every field `bodyFor` copies back to the site. It does **not** carry `json_id`, and that is measured rather
 * than assumed: a run with that listing planned three elements as new that the site already had -- `POST`
 * refused them with `Json id already exists` -- while no row of it had an id to match on.
 *
 * The public element list does carry `json_id`, per row id, and needs no token, so the ids are read from
 * there and merged in. A listing that arrives with ids of its own is left alone, and one that cannot be read
 * falls back to title matching with a line in the summary: that cannot see a rename, so a create refused
 * with `Json id already exists` is the symptom to read if it ever happens again.
 */
const rowsFor = async (addon) => {
  const rows = list(await request('GET', '/syntax/?addon=' + encodeURIComponent(addon)))
  if (rows.length > 0 && rows.every((row) => row.json_id)) return { rows, idsFrom: 'the listing' }

  const ids = new Map()
  try {
    for (const row of list(await request('GET', '/addonsyntaxlist/'))) {
      if (row.json_id) ids.set(row.id, row.json_id)
    }
  } catch (error) {
    say('> could not read the public element list for `json_id` (' + error.message + '), matching by title')
    return { rows, idsFrom: null }
  }
  return {
    rows: rows.map((row) => ({ ...row, json_id: ids.get(row.id) ?? row.json_id ?? null })),
    idsFrom: 'the public element list'
  }
}

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

/** The examples of one element as SkriptHub holds them. */
const examplesOf = async (id) => list(await request('GET', '/syntaxexample/?syntax=' + id))

/**
 * The route the official mark would need, and why the script does not use it.
 *
 * `/api/v1/syntaxexample/officialexample` is what the dashboard's "Mark as a official example" button
 * posts to -- the site's own bundle builds it as `ea + "syntaxexample/officialexample"`, without the
 * trailing slash that the collection and one example take. It cannot be used from here: it authenticates a
 * **browser session**, not an API token, which the API says plainly.
 *
 *   POST /api/v1/syntaxexample/officialexample  with `Authorization: Token bogus`
 *     -> 403 {"detail":"Authentication credentials were not provided."}   (the same as with no header)
 *   POST /api/v1/syntax/                        with `Authorization: Token bogus`
 *     -> 401 {"detail":"Invalid token."}                                  (the endpoint reads the header)
 *
 * The same is true of the dashboard's JSON import (`/api/v1/jsonimport/`), of the vote endpoint and of
 * `/api/v1/me/`, so a run can write examples and their text but cannot mark one official. The page shows an
 * example either way -- the mark only sorts it above the others -- so the run reports how many are unmarked
 * and leaves that to the dashboard.
 */
const OFFICIAL_MARK_IS_THE_DASHBOARDS = 'the API token cannot mark an example official: that endpoint takes a browser session'

/** The name the import gives the example it writes, which is what a replaced one is written as. */
const OFFICIAL_EXAMPLE_NAME = 'Official Example'

const isOfficial = (example) => example.official_example === true

/**
 * One element's examples as the text SkriptHub stores: the lines of the one example, in order.
 *
 * The same `@Examples` entry can be a line or a whole block, and the indentation inside a block is part of
 * the code, so the only thing normalized is blank lines at either end -- which the tool writes without. The
 * comparison is otherwise exact, and it is exact because the site's copy of an example it was given is the
 * text it was given: a difference here is the older syntax of a release that has moved on.
 */
const exampleText = (code) => withoutBlankEdges(String(code ?? ''))
const ourExampleText = (entry) => exampleText(entry.examples.join('\n'))

if (!TOKEN) {
  console.error('SKRIPTHUB_TOKEN is not set. The token is on the SkriptHub API documentation page.')
  process.exit(1)
}

// Everything below reports through `failures` rather than throwing out of the script, so that a request
// that fails still leaves a summary saying what was and was not published.
try {
  const document = readDocument()
  const addon = await resolveAddon()
  const { problems: routeProblems, checked: routesChecked } = await checkRoutes()
  for (const problem of routeProblems) failures.push('route check: ' + problem)
  const { rows, idsFrom } = await rowsFor(addon)
  const plan = compare(document, rows)

  // The example phase is planned before anything is written, because the summary counts it. One element's
  // examples are the text the file gives it: an element whose text is missing gets one written, and one
  // whose text is there but old gets it replaced. An element whose text is right but whose example is not
  // *official* is not written at all -- the mark is a call this token cannot make (see `officialExampleRoute`
  // below), so it is counted and left to the dashboard, because the page shows the example either way.
  //
  // Which examples may be replaced is the part worth being careful about. An example is this script's when
  // the site marks it official, or when the account this script writes as is its author -- and the account
  // is learned from an example whose text is ours, which is one this script wrote on an earlier run. An
  // example somebody else submitted is neither, so it is never compared, never replaced and never deleted.
  let ourAuthor = null
  const owned = (example) => isOfficial(example) || (ourAuthor !== null && example.example_author === ourAuthor)
  const examplesOfEntry = (examples, entry) => ({
    ours: examples.filter((example) => exampleText(example.example_code) === ourExampleText(entry)),
    stale: examples.filter((example) => owned(example) && exampleText(example.example_code) !== ourExampleText(entry))
  })

  const examplePlan = []
  const unmarked = []
  for (const entry of document.entries.values()) {
    if (entry.examples.length === 0) continue
    const row = plan.matched.get(entry.title)
    if (!row) {
      examplePlan.push({ entry, row: null, action: 'written' })
      continue
    }
    try {
      const { ours, stale } = examplesOfEntry(await examplesOf(row.id), entry)
      if (ourAuthor === null) ourAuthor = ours.find((example) => !isOfficial(example))?.example_author ?? null
      if (stale.length > 0) examplePlan.push({ entry, row, action: 'replaced' })
      else if (ours.length === 0) examplePlan.push({ entry, row, action: 'written' })
      else if (ours.some((example) => !isOfficial(example))) unmarked.push(entry.title)
    } catch (error) {
      failures.push('reading the examples of ' + entry.title + ': ' + error.message)
    }
  }

  say('### SkriptHub documentation ' + document.version)
  say()
  say('| | |')
  say('| --- | --- |')
  say('| Addon | `' + addon + '` |')
  say('| Elements in the generated file | ' + document.entries.size + ' |')
  say('| Matched by | ' + (idsFrom ? 'json_id (' + idsFrom + ')' : 'title only') + ' |')
  say('| Routes | ' + (routeProblems.length === 0 ? 'checked, ' + routesChecked + ' of them' : '**' + routeProblems.length + ' wrong**') + ' |')
  say('| To update | ' + plan.updates.length + ' |')
  say('| To rename | ' + plan.updates.filter(({ changed }) => changed.includes('title')).length + ' |')
  say('| To create | ' + plan.creates.length + ' |')
  say('| Unchanged | ' + (document.entries.size - plan.updates.length - plan.creates.length) + ' |')
  say('| On SkriptHub only, left alone | ' + plan.leftAlone.length + ' |')
  say('| Examples to write | ' + examplePlan.length + ' |')
  say('| Examples not marked official | ' + unmarked.length + ' |')
  say('| Mode | ' + (dryRun ? 'dry run, nothing was written' : '**published**') + ' |')
  say()
  if (unmarked.length > 0) {
    say('- ' + unmarked.length + ' example(s) are the text of the file and are not marked official: ' +
      OFFICIAL_MARK_IS_THE_DASHBOARDS + '. The page shows them either way -- the mark only sorts them first.')
    say()
  }

  for (const { entry, row, changed } of plan.updates) {
    say('- `' + entry.title + '` (id ' + row.id + '): ' + changed.join(', '))
  }
  for (const entry of plan.creates) say('- `' + entry.title + '`: new, will be created')
  for (const row of plan.leftAlone) say('- `' + row.title + '` (id ' + row.id + '): not in the generated file, left as it is')
  for (const { entry, row, action } of examplePlan) {
    say('- `' + entry.title + '`' + (row ? ' (id ' + row.id + ')' : '') + ': its example is ' + action)
  }

  let remaining = null
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
    const after = (await rowsFor(addon)).rows
    remaining = compare(document, after)
    for (const { entry, changed } of remaining.updates) {
      failures.push(entry.title + ' still differs after the write: ' + changed.join(', '))
    }
    if (remaining.creates.length) {
      failures.push(
        'still not on SkriptHub after the write: ' +
          remaining.creates.map((entry) => entry.title).join(', ')
      )
    }

    // The examples, after the elements they belong to. Each one is its own request, and a refusal is about
    // the body rather than about that element, so the phase stops at the first one and says how far it got
    // instead of repeating the same message for fifty elements.
    let written = 0
    for (const { entry } of examplePlan) {
      const row = remaining.matched.get(entry.title)
      if (!row) {
        failures.push('no row to write the examples of ' + entry.title + ' to')
        continue
      }
      try {
        const before = examplesOfEntry(await examplesOf(row.id), entry)
        // What is written is the text, and the old copies are removed after it: a refusal then leaves the
        // page with the example it had rather than with none. The official mark is not asked for -- this
        // token cannot make it (see `OFFICIAL_MARK_IS_THE_DASHBOARDS`).
        const created = await request('POST', '/syntaxexample/', {
          syntax_element: row.id,
          example_name: OFFICIAL_EXAMPLE_NAME,
          example_code: entry.examples.join('\n')
        })
        if (ourAuthor === null && created && typeof created === 'object') ourAuthor = created.example_author ?? null
        for (const example of [...before.stale, ...before.ours]) {
          await request('DELETE', '/syntaxexample/' + example.id + '/')
        }

        const after = examplesOfEntry(await examplesOf(row.id), entry)
        if (after.ours.length === 0) {
          failures.push('the example of ' + entry.title + ' is not on the site after the write')
        }
        if (after.stale.length > 0) {
          failures.push(entry.title + ' still has an example that is not the text of the file')
        }
        written++
      } catch (error) {
        failures.push('writing the examples of ' + entry.title + ': ' + error.message)
        failures.push('stopped after ' + written + ' of ' + examplePlan.length + ' example(s)')
        break
      }
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
