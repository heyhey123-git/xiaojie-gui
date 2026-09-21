#!/usr/bin/env node
// Audits what SkriptHub shows for this addon against what `gendocs` generates, field by field.
//
//   ./gradlew gendocs
//   node scripts/audit-skripthub.mjs
//
// Nothing here needs a token: the element list and the examples of an element are both public. The site's
// rows are matched to the generated file by the documentation tool's own id (`json_id` on SkriptHub),
// because that is what survives a rename -- a title alone cannot tell a renamed element from a new one,
// which is the mistake `publish-skripthub.mjs` used to make and now does not.
//
// Three kinds of difference are worth telling apart, and the exit code does:
//
//   - an element the generated file has and the site does not, or a row whose title, pattern, type or
//     description differs: the site is not what this version documents. `publish-skripthub.mjs` fixes the
//     rows it can reach (including a rename) and creates the missing ones, so run this before or after a
//     release to see whether that happened.
//   - a row no element of the generated file is. A syntax that was dropped, an event entry somebody wrote
//     by hand (the documentation tool does not export events, so those four are not in the file at all),
//     or a second row for an element the site already has -- only a person can say which, so the report
//     separates them and the tool does not touch them. `mark as removed` in the dashboard is the answer
//     for a syntax that is really gone.
//   - examples that differ. The publisher deliberately does not write examples: they are a set on the
//     site and the dashboard's JSON import owns them. So they are reported and do not fail the run --
//     SkriptHub joins several examples of one element into one string and keeps its indentation, which is
//     a false alarm, while the rest are the older syntax of a release that has moved on.
import { readFileSync } from 'node:fs'

const API = 'https://skripthub.net/api/v1'
const DOCUMENT = process.env.SKRIPTHUB_DOCS ?? 'build/skripthub/xiaojie-gui.json'
const REPO = process.env.SKRIPTHUB_REPOSITORY ?? 'xiaojie-gui'
const KINDS = ['events', 'conditions', 'effects', 'expressions', 'types', 'functions', 'sections', 'structures']

const text = (value) => String(value ?? '').trim().replace(/\r\n/g, '\n')
const same = (left, right) => text(left) === text(right)
/** How SkriptHub joins the examples of one element, which is how the publisher compares them too. */
const join = (examples) => examples.map((example) => String(example).trim()).join('\n').replace(/\n\s*\n/g, '\n')
/** The same code with the line breaks and indentation taken out, to tell a real change from a join. */
const code = (examples) =>
  examples
    .flatMap((example) => String(example).split('\n'))
    .map((line) => line.trim())
    .filter((line) => line !== '')
    .join('\n')

/** Everything the generated file says, keyed by the id the documentation tool wrote for the element. */
const readDocument = () => {
  const document = JSON.parse(readFileSync(DOCUMENT, 'utf8'))
  const entries = new Map()
  for (const kind of KINDS) {
    for (const entry of document[kind] ?? []) {
      entries.set(entry.id, {
        kind: kind.replace(/s$/, ''),
        title: entry.name,
        pattern: (entry.patterns ?? []).join('\n'),
        description: (entry.description ?? []).join('\n'),
        examples: (entry.examples ?? []).map(String)
      })
    }
  }
  return { version: document.metadata?.version ?? 'unknown', entries }
}

const get = async (path) => {
  const response = await fetch(API + path)
  if (!response.ok) throw new Error('GET ' + path + ' answered ' + response.status)
  return response.json()
}

const list = (payload) => (Array.isArray(payload) ? payload : (payload.results ?? []))

/** The examples of one element as the site holds them. */
const examplesOf = async (id) => list(await get('/syntaxexample/?syntax=' + id)).map((example) => example.example_code ?? '')

const say = (line = '') => console.log(line)
const section = (label, items, format) => {
  say('== ' + label + ': ' + items.length)
  for (const item of items) say('   ' + format(item))
  say()
}

try {
  const document = readDocument()
  const all = await list(await get('/addonsyntaxlist/'))
  const rows = all.filter((row) => (row.addon?.link_to_addon ?? '').toLowerCase().includes(REPO))
  say('generated: ' + document.version + ', ' + document.entries.size + ' element(s); rows on the site: ' + rows.length)
  say()

  const byJsonId = new Map()
  for (const row of rows) {
    if (row.json_id) byJsonId.set(row.json_id, [...(byJsonId.get(row.json_id) ?? []), row])
  }

  const claimed = new Set()
  const inStep = []
  const drifted = []
  const missing = []
  for (const [jsonId, entry] of document.entries) {
    // A row another element already claimed is not a candidate: the site can hold the same element twice,
    // and the extra row is reported below rather than written over here.
    const candidates = (byJsonId.get(jsonId) ?? []).filter((row) => !claimed.has(row.id))
    if (candidates.length === 0) {
      missing.push({ jsonId, entry })
      continue
    }
    // The row that already carries this title, so a rename is reported against the row it replaces.
    const row = candidates.find((candidate) => candidate.title === entry.title) ?? candidates[0]
    claimed.add(row.id)
    const differs = []
    if (row.title !== entry.title) differs.push('title: the site says `' + row.title + '`')
    if (!same(row.syntax_pattern, entry.pattern)) differs.push('pattern')
    if (row.syntax_type !== entry.kind) differs.push('type: the site says ' + row.syntax_type)
    if (!same(row.description, entry.description)) differs.push('description')
    const siteExamples = await examplesOf(row.id)
    if (join(siteExamples) !== join(entry.examples)) {
      differs.push(code(siteExamples) === code(entry.examples) ? 'examples, joined differently' : 'examples')
    }
    const line = { jsonId, entry, row, differs, examples: { site: siteExamples, ours: entry.examples }, extraRows: candidates.length - 1 }
    if (differs.length === 0) inStep.push(line)
    else drifted.push(line)
  }

  const unclaimed = rows.filter((row) => !claimed.has(row.id))
  const events = unclaimed.filter((row) => row.syntax_type === 'event')
  const dropped = unclaimed.filter((row) => row.syntax_type !== 'event')

  section('in step, examples included', inStep, ({ entry, row }) => entry.kind + ' `' + entry.title + '` (id ' + row.id + ')')
  section('drifted', drifted, ({ entry, row, differs, extraRows }) =>
    '`' + entry.title + '` (id ' + row.id + '): ' + differs.join(', ') +
    (extraRows > 0 ? ' [' + extraRows + ' more row(s) carry this id]' : ''))
  section('missing from the site', missing, ({ jsonId, entry }) => entry.kind + ' `' + entry.title + '` (json_id ' + jsonId + ')')
  section('no element of the generated file is this', dropped, (row) =>
    '`' + row.title + '` (id ' + row.id + ', ' + row.syntax_type + ', json_id ' + row.json_id + ')')
  section('events, which the export never carries', events, (row) =>
    '`' + row.title + '` (id ' + row.id + '): pattern ' + JSON.stringify(row.syntax_pattern) + ', description ' + JSON.stringify(row.description))

  const realExamples = drifted.filter(({ differs }) => differs.includes('examples'))
  section('examples that are not the same code, only stored differently', realExamples, ({ entry, row }) => '`' + entry.title + '` (id ' + row.id + ')')
  for (const { entry, row, examples } of realExamples) {
    say('--- `' + entry.title + '` (id ' + row.id + ')')
    say('    the site: ' + JSON.stringify(examples.site))
    say('    the file: ' + JSON.stringify(examples.ours))
  }
  if (realExamples.length) say()

  // The example differences are the site's to keep, so they do not fail the run; everything else means the
  // site is not what this version documents, or holds something this version does not.
  const problems = missing.length + drifted.filter(({ differs }) => differs.some((d) => d !== 'examples' && d !== 'examples, joined differently')).length + dropped.length
  say(problems === 0 ? 'the site is what this version documents, examples aside' : problems + ' difference(s) the publish workflow or the dashboard has to settle')
  process.exitCode = problems === 0 ? 0 : 1
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error))
  process.exitCode = 1
}
