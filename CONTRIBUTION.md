# Contributing

## 1. Build and verify

```bash
./gradlew build          # compile + unit tests + the shaded jar (all projects)
./gradlew test           # unit tests only
./gradlew serverTest     # boots a real Paper server with Skript and checks the addon there
./gradlew ktlintCheck    # style; also runs as part of check
./gradlew ktlintFormat   # fixes everything mechanically fixable
```

Artifacts land in `build/dist`.

`serverTest` downloads Paper, Skript and PacketEvents on its first run and takes about 80 seconds; it is
not part of `check`, so a normal build stays fast.

### On a network that Maven Central refuses

A refused request (HTTP 403) stops the resolution of that repository — Gradle does not fall through to the
next one — and some build-tool artifacts are only on Central (ktlint's rule-set jars, for example), so one
refusal can fail the whole sync in an IDE. Enable the mirror with `-PcnMirror`, or once for every build by
adding this to `<GRADLE_USER_HOME>/gradle.properties` — on Windows, `echo $env:GRADLE_USER_HOME` prints that
directory, and it is not always `%USERPROFILE%\.gradle`:

```properties
cnMirror=true
```

It is off by default, so CI and everyone else keeps talking to Central directly.

## 2. Code style

Style is defined once, in the root `.editorconfig`, which both IntelliJ IDEA and ktlint read. Do
not duplicate the rules in the build script.

In short:

- 4 spaces, LF, UTF-8, no trailing whitespace, one final newline.
- Imports are ordered `*`, `java.**`, `javax.**`, `kotlin.**`. Wildcards are rejected, except for
  the single package `.editorconfig` allows: `ch.njol.skript.doc.*`. Every other import is written
  out; `java.util` in particular, because ktlint cannot reliably parse more than the first entry of
  that list.
- No trailing commas in multi-line argument lists.
- Colons in supertype lists and type-parameter bounds are written `Foo : Bar`.
- **Code, KDoc and comments are written in English.** Chinese is fine in documentation and in
  `build.gradle.kts`. One language in the source keeps it readable for outside contributors.

KDoc is expected on public API and on any non-obvious decision. When a design choice would
otherwise look like a mistake — why a value is cloned before it is handed out, why a render flag is
ignored on one path, why a task re-checks that the player is still looking at the same window —
write the reason down next to it.

Keep the code simple, friendly and reasonable, and do not abstract ahead of the problem:

- No interface for a single implementation, no configuration object for a single setting, no
  framework for two similar lines. Add the second case when it arrives.
- No unused code kept "for later". Delete it; git remembers.
- A page number is an `Int` with a documented origin, not a value class. A clone is a `.clone()`,
  not a wrapper type. Prefer the direct expression of the rule.
- Fixing a bug means making the smallest change that removes the cause, not restating the
  surrounding code in a new shape.

## 3. Skript elements

- One element per file, in `skript/elements/<area>/<kind>/`.
- Register through `skript/utils/SkriptSyntax.kt`, never through `Skript.registerEffect`,
  `Skript.registerSection`, `Skript.registerExpression`, `Skript.registerCondition`, `Skript.registerEvent`,
  the `PropertyExpression.register` shortcut, `EventValues` or `Skript.registerAddon(JavaPlugin)`: all of
  those are deprecated for removal, and the replacement is the addon's own syntax registry, which also
  attributes the syntax to this addon. An element's `companion object` holds
  `fun register(addon: SkriptAddon)`, and `skript/ElementRegistrations.kt` lists every element and calls
  it while the plugin enables — an element written but not listed there does not exist, and the server
  test says so. The price of the new API is that skUnity's automatic jar import only understands the
  deprecated static calls, so the syntax list we publish is the wiki rather than an import.
- Event values go through `SkriptSyntax.eventValue(addon, …)`, which registers on
  `SkriptAddon.registry(EventValueRegistry::class.java)`. The registry has to come from the *addon*:
  `Skript.instance().registry(EventValueRegistry::class.java)` is an unmodifiable view and throws
  `Cannot register event values with an unmodifiable event value registry` when an element is registered
  through it. Only the syntax registry behaves the same from either side, which is what makes this an easy
  mistake to make once and then remember.
- Every element carries `@Name`, `@Description`, `@Examples` and `@Since`. Skript's own docs
  generator silently drops an element that is missing any of them, so a missing annotation means
  the syntax does not exist as far as SkriptHub and skUnity are concerned. `@Since` takes the real
  release version (`1.0.4`), never a release line (`1.0`) and never `-SNAPSHOT`.
- `@Example` must parse. Examples are documentation that users copy; an example that Skript cannot
  read is a bug report waiting to happen. The server test parses them.
- A required expression (`%type%`) must never sit inside an optional group (`[...]`); it has to be
  `%-type%`. Leaving the group out does not hand the element a null: Skript fills that slot with the
  class's *default expression* (typically the current event's value, so the syntax quietly reads
  something nobody wrote), and a class with no default expression fails the whole element with
  `SkriptAPIException: The class 'object' does not provide a default expression. Either allow null
  (with %-object%) or make it mandatory`. `object` is the common case -- a title slot typed `%-object%`
  is what `titles.md` describes -- and the symptom is one `[Skript] Severe Error` line while the server
  starts normally, which is exactly what the server test is for. `object` is not the only type that can
  do this, so the rule is about the shape, not about which class it is.
- A `type` that only another addon registers (SkBee's `textcomponent`) must not appear in a pattern
  unless the element also registers a fallback pattern without it: pattern compilation is per
  pattern string, so one unknown type fails the entire syntax, including the alternatives next to
  it.
- Pages are **1-based** everywhere: `Menu.pages`, `MenuSession.page`, `MenuProperties.defaultPage`,
  `turn to page`, `in page %number%`. There is no page 0.
- The layout a menu is created with **always** becomes page 1, and `with page N` / `default page` only
  decides which page `open menu` shows. Never make page creation conditional on it: a menu with no pages
  cannot be opened, which is exactly what that used to produce.
- One creator per page: `insert page` adds pages, and the layout given to `create menu` is shorthand for
  inserting the first one. Do not add a third way to describe a page.
- A single-page menu is the normal case. Nothing in the API should force a user to think about pages
  before they want more than one.
- A layout's `slotRange` is the set of slots the **client** draws for the window that layout opens, so it
  has to equal the size of the layout's `InventoryType`, with the player's 27 main slots and 9 hotbar
  slots right after it — the lectern is the one client menu with no player inventory, so its layout carries
  no player half. A range that is off by one does not raise anything: it moves everything in the
  player's inventory and every click in the window by a slot. `ViewLayoutTest` holds each layout to
  Bukkit's own size for its type, and a container type Minecraft has no menu for cannot become a layout.
- Runtime diagnostics use `Skript.warning`/quiet no-ops, not the parse-time `Skript.error`.
- **Do not name a property what Skript already names one.** Skript 2.16 registers properties of its own
  (`viewer[s]`, `id`, `name`, …) over *any* object, and registers them before this addon, so a pattern like
  `[the] viewers of %menu%` is answered by Skript and returns nothing — silently, and only for the scripts
  that pass an untyped variable or a value Skript's property accepts. `ExprMenuViewers` is called
  `menu viewers` for that reason, and `Menu.kt`'s viewer comment says the same thing from the other side.
- A `%type%` slot preceded by the literal word of that type (`menu %menu%`) parses its argument as a lookup
  by id, so a variable put there finds nothing. Write the slot first: `menu viewers of %menu%`, not
  `viewers of menu %menu%`.
- One interaction is **one** event, however many slots it touched. A drag over four slots must not run a
  handler four times: a handler that pays out or gives an item would do it four times. Per-slot callbacks
  are the exception, and only because each belongs to one slot and is handed an event whose slot and icon
  are its own; `Page.loadInPage` is where that is written, and `docs/guide/dragging.zh-CN.md` is what a
  script author reads about it.
- User-facing words come from the game, not from this addon's model. What a player has open is a **window**
  (Bukkit's `InventoryView`), not a "session"; what is behind it is an inventory, not a "receptacle". The
  type name `menusession` and the word `session` in `the menu session of player` still parse and mean the
  same thing, and the guide teaches `window`. When a concept can only be said with our own word, that is the
  signal it needed one: `the dragged slots` earned its name that way, while `phantom` and `static` are the two
  things a script must choose between, so they stay.
- **A keyword has one meaning, and it does not become another one because of state.** If a keyword can only
  work while some other switch is set, then that switch is part of it and the keyword sets it: `player
  layout` names the rows below the container, and those rows are the menu's only while the player's
  inventory is hidden, so giving one hides it. `with hide player inventory` stays as the way to ask for
  "hidden but empty". The same rule has a second half: **a state the model rejects must be unreachable, not
  merely warned about.** The one syntax that could put a page's icons and the player's items in one window --
  `show player inventory of %menu%` on a menu whose page lays those rows out -- is refused with a line that
  says what to do instead, and the window layer clears those rows before sending them, so the mixing cannot
  happen even if some later path sets that flag. The reason to write this down: the first version of that
  keyword described a display copy of the player's rows *while they were shown*, with the player's own items
  drawn over the layout's icons. That half was then neither the player's nor the menu's, and the trap was
  invisible from the script -- which is why a keyword that quietly does nothing, or something conditional,
  is treated as a bug in the keyword rather than as a documented caveat.

## 4. Tests

Four layers, each catching what the one below it cannot:

- **Unit tests** (`src/test`) cover what needs no server at all: the page model, click types, colour
  codes. They run with `./gradlew build`.
- **The server test** (`./gradlew serverTest`) boots a real Paper 26.2 server with Skript, PacketEvents
  and SkBee, runs the scripts in `server-test/skript/`, and checks the log. It is the only layer that can
  see registration and parse failures: MockBukkit cannot host Skript at all (Skript's main class is
  final and MockBukkit loads a plugin by subclassing it), and Skript reports those failures in prose while
  the server keeps running.
- **The example gate** is part of that run: `prepareServerTest` writes `07-examples.sk` from every
  `@Example`/`@Examples` annotation and lets Skript parse it, so a documented example that does not parse
  fails the build. A `@Examples` annotation is one example; one that starts with `on ...:` or
  `command /...:` is written out as it stands, everything else is wrapped in a command trigger.
- **The client test** (`./gradlew clientTest`) connects a real client to the same prepared server, so what
  only a client can see — the window it is shown, the items in its slots, the title after a page turn —
  is asserted rather than assumed. It starts from a **fresh player**: the run directory is shared with the
  server test, Paper saves the bot's inventory under `world/players`, and the bot logs in as the same
  offline account every run, so the task clears that data before it boots (a check that reads a cell of the
  player's own half would otherwise be reading whatever the run before left there).
- **The manual pass** (`docs/manual-acceptance.zh-CN.md`) covers what no layer here can: a real 26.2
  client rather than the 26.1 one the client test connects through Via, dragging, ghost items, two clients
  side by side, and whether a window looks right at all. It is deliberately short, and anything on it that
  can be expressed as an assertion should be moved into one of the layers above.

Assertions live next to the log they read (`build.gradle.kts`), not in the scripts: the scripts report
`XIAOJIE_SELFTEST detail: <name> -> <message>` and the task requires a message for every name it expects,
and rejects a name it does not. A script whose element stopped working therefore fails instead of quietly
shrinking what is covered.

## 5. Versions

One source per version. The plugin version lives in `gradle.properties` and is expanded into
`plugin.yml` by `processResources`; the jar name, the tag and the release notes all follow it. The
Minecraft version appears in `plugin.yml` (`api-version`) and in the paperweight dev bundle, and those
two must agree.

Releasing is a workflow someone presses, not steps someone remembers:
`.github/workflows/release.yml` runs from the default branch and refuses a version that is missing, a
snapshot, a tag that already exists, or a CHANGELOG section that was never written. It then runs the same
layers CI runs, on the commit it is about to tag, writes a `.sha256` and publishes the jar together with
that CHANGELOG section. Correcting the notes of a release that is already out is the same extraction by
hand: `./gradlew releaseNotes`, then
`gh release edit v<version> --notes-file build/release-notes.md`.

The release publishes the jar; the syntax on SkriptHub follows it on its own. A `Release` run that
completed successfully starts `.github/workflows/skripthub.yml`, which generates the same JSON and sends
it to SkriptHub's API one element at a time: `scripts/publish-skripthub.mjs` is a diff, so it updates what
changed, creates what is missing, and only reports what it will not touch (deleting an element, its
examples and its supporting plugins stay the dashboard's job). An element is recognised by the
documentation tool's own id as well as by its title, and that is what makes a **rename** an update rather
than a create: a title-only diff asks for a second row under the new name, and the API refuses the whole
list with `Json id already exists` because the old row holds that id. It needs an environment named
`skripthub` holding `SKRIPTHUB_TOKEN` from the SkriptHub API documentation page, and it finds the addon by
this repository's URL, so the name SkriptHub shows it under can change without touching anything here.
Dispatching that workflow by hand with `dry-run` reports the same thing without writing.

## 6. Documentation

Two halves, for two readers:

- The **README** is the front page: what the plugin is, how to install it, and one example that runs.
  Both languages, and the example is also a script in `server-test/skript/` so it cannot go stale.
- The **wiki** is the syntax reference, because the addon API's registration is invisible to skUnity's
  automatic jar import: there is no import to keep correct, so the pages are the list. They are written in
  `docs/guide/*.zh-CN.md`, mapped to page names by `scripts/wiki-pages.tsv`, listed by
  `scripts/wiki-sidebar.md`, and published by `scripts/publish-wiki.ps1`, which
  `.github/workflows/wiki.yml` runs on every push to `master`.
  Every page is a bilingual pair: `docs/guide/<name>.zh-CN.md` is the Chinese half and keeps the plain
  page name, `docs/guide/<name>.md` is the English half and takes the `-English` suffix, and each half's
  first line switches to the other, so a syntax change has to be made in both files.
- `./gradlew gendocs` is the SkriptHub import. SkriptHubDocsTool only writes documentation on a *running*
  server, so the task boots a disposable one under `build/gendocs-server/`, has it run `/gendocs`, and
  copies the JSON to `build/skripthub/xiaojie-gui.json` for the SkriptHub dashboard's JSON import. Unlike
  skUnity's jar import, the tool reads Skript's registry, so syntax registered through the addon API does
  reach the file — but only conditions, effects, expressions and sections: the events are not in the
  export, and the wiki stays the list for those. The version in the JSON comes from `gradle.properties`,
  so it cannot disagree with the jar; a 2.0.0 run reports 58 elements, which is the 63 this addon
  registers minus its five events.
- The **forum pages** are written down here instead of typed into an editor each time:
  `docs/skunity-resource.bbcode.txt` (English) and `docs/minebbs-resource.bbcode.txt` (Chinese) are the
  resource descriptions, and `docs/update-message.bbcode.txt` is the short bilingual note that goes out
  with a version: a title, two sentences and one link, under 200 characters per half. The changelog is
  where a change is explained, so this note only says that there is a new version and what it is for.
  BB-code, because neither forum renders Markdown -- and the Chinese page carries no
  syntax list on purpose: MINEBBS's firewall refuses a post that long, so it points at the wiki and
  SkriptHub instead. A syntax or feature change belongs in all three.

A page may only describe syntax that exists: read the element's own patterns and prefer its `@Examples`
verbatim, since those are the ones the example gate parses. A claim about a trap belongs in the page only
if the code says so — `override slot` following the menu's default page, for example, is in
`EffSecOverrideSlot` and nowhere else.

## 7. Commits

Write the subject in the imperative mood and explain the **why** in the body when the change is not
self-evident. Keep unrelated changes in separate commits.
