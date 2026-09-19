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
  `%-type%`. The failure mode is that the whole element disappears with a single `[Skript] Severe
  Error` line in the log while the server starts normally, which is exactly what the server test is
  for.
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
- Runtime diagnostics use `Skript.warning`/quiet no-ops, not the parse-time `Skript.error`.

## 4. Versions

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

## 5. Commits

Write the subject in the imperative mood and explain the **why** in the body when the change is not
self-evident. Keep unrelated changes in separate commits.
