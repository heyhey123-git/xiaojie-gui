# CI downloads and cold-run budget

## Why these caches exist

The supplied evidence for run `35496619504` was a build taking 18m55s, a server phase taking
8m7s, and a client phase cancelled at the old 30-minute job limit. The setup-gradle post
step (`!cancelled()`) was skipped and the cache API was empty. That does **not** mean Gradle
caching was unconfigured: both workflows already used `gradle/actions/setup-gradle@v5`, and
`org.gradle.caching=true` was already set.

`setup-gradle` remains the only owner of the Gradle user-home cache. There is no overlapping
`actions/cache` of `~/.gradle`. The additional caches contain only project-local downloads.

## What the full log establishes

The subsequently supplied full log confirms `Gradle User Home cache not found`. However,
cache misses do not explain every delay: Paperweight's reported setup took 3.457s and the
Paper test JAR download took 0.313s. The server phase spent about 7m34s before `runServer`,
then about 33s booting, testing and stopping Paper.

During that second Gradle invocation, `compileKotlin` appeared at 07:42:11 and was reported
`UP-TO-DATE` at 07:45:13; `compileTestKotlin` appeared at 07:45:43 and was reported
`UP-TO-DATE` at 07:47:27. These are console-output intervals, not measured compiler CPU
execution. Dependency resolution and task input checking are investigation candidates,
not proven causes. The client step was cancelled while processing prerequisite
`compileKotlin`, before the actual `clientTest` task or bot produced output.

Build, server and client invocations now use `--info --profile --console=plain` alongside
`--stacktrace`. Inspect dependency requests/retries in the Actions output and daemon logs,
and configuration, dependency resolution and task timings in `build/reports/profile/`.
Profiles are already included in the existing `build/reports/` diagnostic artifact.
A forcibly terminated Gradle process may not finish writing its HTML profile; the daemon
log is a complementary source, not a promise of a completed profile. Do not interpret an
`UP-TO-DATE` task's long interval as proof of recompilation or change repositories blindly.

## Shared build/release cache contract

Both workflows use the same exact keys and allowlists (no broad fallback restore keys):

| Cache | Contents | Key inputs after schema prefix |
| --- | --- | --- |
| `test-server-v1` | `server-test/run/{versions,libraries,cache}` and the three explicitly named Skript 2.16.2, PacketEvents 2.13.0, SkBee 3.25.4 jars in `plugins/` | runner OS/architecture; hash of `gradle/libs.versions.toml` and `build.gradle.kts` |
| `test-client-v1` | explicitly named ViaVersion/ViaBackwards 5.12.0 jars in `build/client-test-plugins/`; `build/client-test/npm-cache/_cacache` | runner OS/architecture; hash of `build.gradle.kts`, bot `package.json`, and `package-lock.json` |

No cache includes worlds, player data, scripts, plugin configuration, the addon jar under test,
logs, reports, `node_modules`, or `.part` plugin downloads. npm's log directory is excluded:
only its content-addressed `_cacache` is restored. Plugin version changes must update the
explicit paths in **both** workflows as well as the pins in the build script. Bump the `v1`
prefix in both workflows if the allowlist/layout changes incompatibly. GitHub's branch and
PR cache access rules still apply; a common key is not a cross-branch access bypass.

Server downloads are explicitly saved immediately after a successful `serverTest`, before
starting the client phase. Client downloads are saved after a successful `clientTest`.
Exact cache hits need no save (GitHub caches are immutable). Failed phases do not seed new
caches, and cache service failures/timeouts are nonfatal optimizations, not test results.
The client restore happens **after** release's `clean build` and server phase, so `clean`
cannot erase the restored Via/npm downloads. A client failure cannot undo an already saved
server cache. Gradle's own cache still relies on setup-gradle's post step.

## Deterministic downloads and diagnostics

The client task always runs `npm ci --prefer-offline --no-audit --no-fund`. The committed
lockfile, not package or `node_modules` modification time, controls installation; npm fails
if the manifest and lock disagree. Installation is bounded to five minutes and its output
is saved to `build/client-test/npm.log`.

Server-test plugin and Via downloads write a sibling `.part`, with 30-second connect and
60-second read timeouts, then check nonempty content, HTTP content length when supplied,
and a readable JAR directory before moving to the final name. A failed transfer removes
its partial file. This is temp-then-move integrity screening, **not** a cryptographic checksum
pin or a total transfer deadline; the workflow step provides the outer time budget.

Both jobs allow 60 minutes. Build, server, and client phases have 23-, 10-, and 12-minute
limits. Cache operations have two-minute limits; diagnostics have two minutes. Release
version checks, notes generation and publishing each have a one-minute limit. These limits
bound the expensive phases and leave room for ordinary setup/upload/post processing rather
than spending the whole job limit on a stuck test. They are not a guarantee that every
step can consume its maximum simultaneously and still leave generous post time.

Both workflows request diagnostic artifacts with `if: always()`: Gradle reports/test results
(including HTML profiles), client/server/npm logs, Paper logs and crash reports, retained seven days.
A separate two-minute upload includes only `${GRADLE_USER_HOME}/daemon/*/daemon-*.out.log`,
with hidden-file inclusion enabled for the `.gradle` parent. It does not upload the whole
Gradle home, credentials, properties or caches. Keep credentials out of build logging;
`--info`, not `--debug`, is used. Missing files warn
rather than inventing diagnostics. Hard job cancellation or runner loss can still prevent
an always step or post action from completing. Build path filters also cover
`gradle.properties`, `gradlew`, `gradlew.bat`, and both verification workflows.

## Native Skript assertions

The `.sk` behavioral tests use Skript 2.16.2's own
[`EffAssert`](https://github.com/SkriptLang/Skript/blob/2.16.2/src/main/java/ch/njol/skript/test/runner/EffAssert.java),
not an addon imitation.
The test JVM enables `skript.testing.enabled` and `skript.testing.devMode`, and supplies
`skript.testing.dir` and `skript.testing.results`. In this version, development mode
registers test syntax but does not run Skript's full automated suite or automatically
export its results; normal plugin scripts still load.

A separate test-only bridge observes `TestTracker` and reports assertion failures at
shutdown. It is not part of the production addon JAR. Both Gradle test tasks must reject
assertion failures and missing readiness/final summaries. The client test must wait for
server shutdown before checking the final summary. Existing scenario/completion markers
and parser-error checks remain necessary: zero recorded failures alone does not prove
that the intended scripts parsed or that all test paths ran. Skript may stop only the
current trigger on assertion failure, so the independent shutdown script remains in place.

These testing internals are pinned to Skript 2.16.2 and should be rechecked on upgrades.
Do not copy the assertion scripts wholesale to an ordinary production server: `assert`
is not registered there. A single tracked suite detects any failure, but Skript's tracker
may retain only the last failure for that suite; it is not an assertion-count report.
The client scripts' assertions describe the fixed bot scenarios, not arbitrary manual
interactions; when extending the bot, update the corresponding script expectations too.

## Skript's own messages in the server log

`serverTest` starts Skript with `-Dskript.testing.enabled=true`, and test mode makes `Skript.debug()`
true whatever `verbosity` says. Some lines that look like failures are therefore artefacts of the
test server, and one category was ours to fix:

- `Missing entry 'types.x' in the default/english language file` is printed only under
  `Skript.debug()`. A stock server with `verbosity: normal` prints none of them — the log left by
  the non-test-mode docs server has zero such lines. It appeared for `types.menu` and
  `types.menusession` because this addon registers two Skript classes and shipped no language file,
  which also made Skript name them `types.menu` in its own error messages. Fixed by
  `src/main/resources/lang/default.lang` plus `localizer().setSourceDirectories("lang", …)` in
  `onEnable`: an addon that never scopes a language directory has its `lang/` folder ignored, and the
  entries must be in place before the classes are registered. The entries still in the log belong to
  others: `types.geyser`/`types.geyserbase` (SkBee's own gap) and `types.testgui`/`types.exemplus`/
  `types.aardwolf`/`types.hoof` (Skript's test runner).
- `List is missing 'and' or 'or', defaulting to 'and'` is a parse-time warning aimed at the script
  author: a comma-separated list with no conjunction (`%strings%` parameters). `"A" and "B"` means the
  same thing and does not warn, so the examples, the guides and the test scripts join layout rows with
  `and`. There is no per-syntax way to silence it; the alternatives are `suppress missing conjunction
  warnings` before the line or `disable variable missing and/or warnings: true` in Skript's config.
- `Empty configuration section!` is a colon with nothing indented under it. One generated `@Example`
  and one test script had one; both are gone.
- The refusal messages (`Player cannot be null.`, `Menu session cannot be null …`, `At least one item
  must be provided …`, `"show player inventory" cannot undo …`) come from `04-pages-keys.sk`,
  `05-titles.sk`, `14-browser-list.sk` and `16-player-layout.sk` doing the refused thing on purpose.
  A production script sees them only by making the same mistake. Every one of them is reported through
  the `error(…)`/`warning(…)` the syntax element inherits from `RuntimeErrorProducer` (effects,
  conditions and sections inherit it directly; `SimpleExpression` already implements it, so the
  expressions do too, and the one helper that reports, `ComponentHelper.resolveTitleComponentOrNull`,
  takes the calling element as its reporter). Each message therefore names the script, the syntax and
  the type, echoes the statement's own line, and Skript's runtime-error limits apply:
  `The script '05-titles.sk' encountered an error while executing the 'Page Title' expression:` plus
  `Line 64: set the title of page 9 in {_menu} to "Nowhere"`.
  `Skript.error` is left where it belongs — `init` and `parseNode`, the parse-time calls — and the one
  bare `[Skript] Line N:` block the run still prints is that parse-time warning. Two gate checks keep
  it that way: a bare log frame naming a runtime method (`execute`/`walk`/`change`/`get` or any helper
  that is not `init`/`preInit`/`parseNode`) fails the run, and the log must contain the framed
  `'Page Title' expression` message, so the channel cannot silently fall out of use. `05-titles.sk`
  produces that message by writing to a page that does not exist and then asserting the menu was left
  alone.

## Validation boundary

Local validation: both workflows parsed with `js-yaml`; an automated comparison checked
matching build/release cache keys and restore/save allowlists. After the diagnostic update,
checks also confirmed all six verification commands carry the diagnostic flags, reports
remain in an `always()` upload, and both daemon uploads use the narrow log glob with hidden
file inclusion. A separate read-only review found no workflow ordering or permission blocker.
Gradle `help` and `ktlintKotlinScriptCheck` passed offline with the project-local Gradle user home.

A local full-run investigation found a false green: the log contained `loop-index is not a
number` and a scalar assignment that `can only be set to one class java.lang.Object, not more`,
but neither diagnostic was checked. Both the server and client
verifiers now reject those specific numeric/type-cardinality error shapes. Source-line
`Line:` echoes are excluded; warnings and intentional runtime guards are not blanket failures.
This is targeted, evidenced detection, **not** an exhaustive Skript parser-error detector.
Replaying the recorded erroneous log with `serverTest -x runServer --offline` failed with
exactly those two newly detected problems, without booting or overwriting the log. Regex
controls also checked that source echoes, list warnings and `Title cannot be null.` do not
match these new patterns. The corrected full server/client run is validated separately.

Native assertion negative verification: a temporary script waited two seconds and ran
`assert 1 is 2 with "NATIVE_ASSERT_NEGATIVE_PROBE_AFTER_SCENARIOS"`. Paper shut down
normally, the bridge reported the exact file/line and `failures=1`, and `serverTest` failed
on the bridge failure and nonzero summary, not on missing scenario markers. This tests
runtime assertion propagation rather than merely rejecting a parser error. The temporary
probe was removed before the final positive run.

The final local `build serverTest clientTest --info --profile --console=plain --no-watch-fs`
passed in 1m 41s. It re-ran the Paper server and bot: 49 server completion records and 13
client scenario records passed, including zero bridge failures. The unit-test task was
up-to-date; its existing XML results contain 114 tests across 18 suites, with no failures
or errors. The scripts contain 103 native `assert` statements, not a measured execution
count. The assertion-log self-check is attached to `check` and rejected nine invalid log
shapes. An offline replay of a successful server log with only its final assertion summary
removed failed with exactly one problem: `Expected exactly one final assertion summary,
found 0.` The production JAR was separately checked to exclude the bridge and Skript testing
classes. This warm local duration is not a GitHub Actions performance estimate.

The runtime-error channel was validated in the same style: the full `build serverTest clientTest`
passed with 50 server completion records — the newest being the refused out-of-bounds page title —
and 13 client records; the log carries exactly one addon `(from …)` frame, the deliberate parse-time
warning. The new gate check was proved offline with `verifyServerTestLog`: the unmodified log passes,
a copy whose framed `'Page Title'` block is rewritten into the old bare shape fails on the new problem,
and a copy that adds one bare frame next to the intact framed block fails on that check alone
(`build/bare-runtime-frame-verification.log`).

No remote cold/warm run, cache hit, upload completion, or speedup is claimed. Confirm these
on the next GitHub run, including a subsequent warm run and whether the post-step time
reserve is sufficient.
