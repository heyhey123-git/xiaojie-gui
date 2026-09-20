#!/usr/bin/env node
// Publishes the generated SkriptHub documentation to skripthub.net.
//
//   ./gradlew gendocs
//   SKRIPTHUB_TOKEN=<token> node scripts/publish-skripthub.mjs [--dry-run]
//
// The dashboard imports the whole JSON by hand, and that stays the way to publish examples; this script
// is for the syntax itself, so a release does not have to wait for someone to paste a file. The API has
// no bulk import — it has one endpoint per element — so this is a diff: it reads what SkriptHub has,
// updates the elements whose pattern, description or since version changed, creates the ones that are
// not there yet, and reports the ones it will not touch.
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
    throw new Error(method + ' ' + path + ' answered ' + response.status + ': ' + detail.slice(0, 400))
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

/** The body of a write: the fields the generated file owns, plus what the existing row must keep. */
const bodyFor = (entry, row) => {
  const body = {
    title: entry.title,
    description: entry.description,
    syntax_pattern: entry.pattern,
    syntax_type: entry.syntaxType,
    // The one field SkriptHub cannot infer: a new element has none, so it starts empty and is filled in
    // by hand where an element needs another plugin loaded.
    required_plugins: (row?.required_plugins ?? []).map((plugin) => (typeof plugin === 'string' ? plugin : plugin.name))
  }
  if (entry.since) body.compatible_addon_version = entry.since
  if (row?.addon) body.addon = row.addon
  if (row?.compatible_minecraft_version != null) body.compatible_minecraft_version = row.compatible_minecraft_version
  if (row?.type_usage != null) body.type_usage = row.type_usage
  if (row?.return_type != null) body.return_type = row.return_type
  if (row?.event_values != null) body.event_values = row.event_values
  if (row?.event_cancellable != null) body.event_cancellable = row.event_cancellable
  if (row?.keywords != null) body.keywords = row.keywords
  return body
}

const same = (left, right) => (left ?? '').trim() === (right ?? '').trim()

/** The examples of one element as SkriptHub holds them, joined the way it joins them. */
const examplesOf = async (id) => {
  const payload = await request('GET', '/syntaxexample/?syntax=' + id)
  return list(payload).map((example) => withoutBlankEdges(example.example_code ?? ''))
}

const compare = (document, rows) => {
  const updates = []
  const creates = []
  for (const entry of document.entries.values()) {
    const row = rows.find((candidate) => candidate.title === entry.title)
    if (!row) {
      creates.push(entry)
      continue
    }
    const changed = !same(row.syntax_pattern, entry.pattern) ||
      !same(row.description, entry.description) ||
      (entry.since !== undefined && !same(row.compatible_addon_version, entry.since))
    if (changed) updates.push({ entry, row })
  }
  const removed = rows.filter((row) => !document.entries.has(row.title))
  return { updates, creates, removed }
}

const differencesIn = (before, after) => {
  const changed = []
  for (const [field, left, right] of [
    ['pattern', before.syntax_pattern, after.syntax_pattern],
    ['description', before.description, after.description],
    ['since', before.compatible_addon_version, after.compatible_addon_version]
  ]) {
    if (!same(left, right)) changed.push(field)
  }
  return changed
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
  say('| To update | ' + plan.updates.length + ' |')
  say('| To create | ' + plan.creates.length + ' |')
  say('| Unchanged | ' + (document.entries.size - plan.updates.length - plan.creates.length) + ' |')
  say('| On SkriptHub only, left alone | ' + plan.removed.length + ' |')
  say('| Mode | ' + (dryRun ? 'dry run, nothing was written' : '**published**') + ' |')
  say()

  for (const { entry, row } of plan.updates) {
    say('- `' + entry.title + '` (id ' + row.id + '): ' + differencesIn(row, {
      syntax_pattern: entry.pattern,
      description: entry.description,
      compatible_addon_version: entry.since ?? row.compatible_addon_version
    }).join(', '))
  }
  for (const entry of plan.creates) say('- `' + entry.title + '`: new, will be created')
  for (const row of plan.removed) say('- `' + row.title + '` (id ' + row.id + '): not in the generated file, left as it is')

  if (!dryRun) {
    for (const { entry, row } of plan.updates) {
      try {
        await request('PUT', '/syntax/' + row.id + '/', bodyFor(entry, row))
      } catch (error) {
        failures.push(entry.title + ': ' + error.message)
      }
    }

    if (plan.creates.length) {
      // One call, because the endpoint takes a list; every element of it is created or none is.
      const body = plan.creates.map((entry) => bodyFor(entry, null))
      try {
        await request('POST', '/syntax/', body)
      } catch (error) {
        failures.push('creating ' + plan.creates.length + ' element(s): ' + error.message)
      }
    }

    // Read back rather than trust the status codes: what was written is only published once the entry
    // says so, and a field the API silently ignored would otherwise be found by a reader.
    const after = list(await request('GET', '/syntax/?addon=' + encodeURIComponent(addon)))
    const remaining = compare(document, after)
    for (const { entry, row } of remaining.updates) {
      failures.push(entry.title + ' still differs after the write: ' + differencesIn(row, {
        syntax_pattern: entry.pattern,
        description: entry.description,
        compatible_addon_version: entry.since ?? row.compatible_addon_version
      }).join(', '))
    }
  }

  // Examples are compared last and loosely: SkriptHub joins several examples of one element with a single
  // newline where the tool leaves a blank line, so only a real difference is worth reporting.
  const join = (examples) => examples.map((example) => example.trim()).join('\n').replace(/\n\s*\n/g, '\n')
  for (const entry of document.entries.values()) {
    const row = rows.find((candidate) => candidate.title === entry.title)
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
