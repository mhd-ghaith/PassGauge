# Setup & Build Guide

Full build-from-source instructions. For a project overview, see the
main [README](README.md).

A two-module JavaFX desktop app:

1. **Strength Analyzer** — scores a password, explains why, and offers
   suggestions, entirely on-device.
2. **Benchmark** — the original parallel-programming assignment tool:
   brute-forces a password's SHA-256 hash single-threaded vs multi-threaded,
   with speedup, parallel efficiency, dynamic charts, run history, CSV
   export, and system info.

## Do you need to install any libraries?

**No new libraries are required.** Everything in this build — including
the strength analyzer — is written with plain Java + JavaFX, which you
already have set up from before. You only need what you already added:

- The JavaFX SDK as a library (unchanged from before)
- The module-path VM options (unchanged from before)

### Why not the actual zxcvbn library?

You mentioned using a Java port of zxcvbn (e.g. `zxcvbn4j`). I didn't wire
that in, for a practical reason specific to this project: it has no build
tool (no Maven/Gradle), so pulling in a third-party library means manually
downloading its jar *and* its own dependencies (zxcvbn4j pulls in Guava)
and adding each one in Project Structure > Libraries by hand — fragile,
and easy to get wrong without a dependency manager.

Instead, `strength/StrengthAnalyzer.java` is a small, self-contained
analyzer modeled on zxcvbn's ideas — length, character variety, a
common-password list, sequential/keyboard/repeated-pattern detection, and
simple l33t-speak normalization — with zero external dependencies. It's a
simplified approximation, not a full port (zxcvbn's real strength comes
from large frequency-ranked dictionaries and more elaborate pattern
matching), but it needs nothing extra installed and keeps the project
exactly as "plain" as it was.

**If you specifically need the real zxcvbn4j** (e.g. for a grading
rubric), the cleanest path is to convert this project to Maven first —
Maven can then fetch zxcvbn4j and its transitive dependencies
automatically instead of you sourcing jars by hand. Let me know if you
want that conversion; it's a quick change at that point.

## Setup (unchanged from before)

1. Download the JavaFX SDK: https://gluonhq.com/products/javafx/
2. Copy this folder's `src/` and `resources/` into your IntelliJ project.
3. Mark `src` as **Sources Root**, `resources` as **Resources Root**.
4. `File > Project Structure > Libraries` — add the JavaFX SDK's `lib`
   folder as a library.
5. Run configuration: main class `com.passgauge.Main`, VM options:
   ```
   --module-path "/path/to/javafx-sdk-21/lib" --add-modules javafx.controls,javafx.fxml
   ```

## Module 1 — Strength Analyzer

- Type a password (or click **Generate strong password** for a random
  one, built locally with `SecureRandom` — nothing is fetched online).
- The 5-segment meter and label update live as you type.
- **Feedback** explains what's weak about it (too short, common password,
  sequential/keyboard/repeated patterns, looks like a date).
- **Suggestions** lists concrete fixes.
- The paragraph underneath explains the score in plain English.
- **Privacy**: the analyzer takes a `String` in and returns a result —
  it never writes the password to a file, a log, or any in-memory history.
  Nothing from this tab is added to Module 2's history or CSV export.

## Module 2 — Benchmark

Same brute-force benchmark as before, with additions:

- **Efficiency** tile = `(speedup / thread count) × 100%` — shows how well
  the extra threads are actually being used (100% = perfect scaling).
- **Dynamic chart**: the "Time / Speedup / Efficiency" chips above the
  chart switch what the bars show, rebuilt from your run history (last 6
  runs plotted at a time; full history stays in the table below).
- **Run History** table now also has an Efficiency column.
- **Export CSV**: saves every run in the table to a `.csv` file you
  choose, as `Threads,MaxLength,SequentialSeconds,ParallelSeconds,Speedup,
  EfficiencyPercent,HashesPerSecond` — paste straight into your report.
- **System info** line under the chart shows CPU core count
  (`Runtime.getRuntime().availableProcessors()`), the running JVM's Java
  version, and which thread count your last run used.

History is kept for the current app session (in the table); it isn't
written to disk automatically — use Export CSV when you want to keep it.

## Other fixes in this round

- **Tab focus rectangle removed** — JavaFX draws a focus-ring box around a
  Tab when you click it by default; `.modern-tabs .tab .focus-indicator`
  is now set to transparent in both themes so it no longer shows.
- **Show/hide password** — both password fields (Strength Analyzer and
  Benchmark) now have an eye button next to them. It's a small custom
  vector eye icon (no icon-font dependency): a diagonal slash appears
  across it while the password is hidden (the default) and disappears
  when you click it to reveal the password; click again to hide it. Under
  the hood it's a masked `PasswordField` and a plain `TextField` sharing
  the same bound text, with only one visible at a time.
- **App icon** — replaced with your purple gauge/lock image, background
  removed (it was a flat JPEG with the checkerboard baked into the actual
  pixels rather than real transparency, so it was color-keyed out and the
  icon cropped to just the purple badge), exported at 16/32/48/64/128/256px.
- **Header simplified** — now shows just "PassGauge" next to the icon
  instead of the full tagline; the OS window title was simplified to match.

## Max Length field

- Fixed contrast: this field now sits on a plain card background
  (instead of the gradient header) with solid theme-colored text and a
  visible border, in both light and dark mode — including the increment/
  decrement arrows, which are now solid accent-colored glyphs on a
  bordered button instead of faint icons on a translucent background.
- Only digits can be typed into it — a `TextFormatter` silently rejects
  any non-digit keystroke (no error message, the character just doesn't
  appear), and the value is clamped to 1–7 when you leave the field.

## Packaging as a Windows installer

Prerequisites (one-time):
- **WiX Toolset v3** — from https://github.com/wixtoolset/wix3/releases
  (get the `wixNNN.exe` asset, not v4+ — look for `candle.exe`/`light.exe`
  in its `bin` folder after installing)
- **JavaFX jmods** (not the SDK) — from https://gluonhq.com/products/javafx/,
  matching your JDK's major version

Steps:
1. In IntelliJ: `File > Project Structure > Artifacts > + > JAR > From
   modules with dependencies...`, set Main Class to `com.passgauge.Main`,
   OK through both dialogs.
2. `Build > Build Artifacts... > (your artifact) > Build`. The jar lands
   under `out/artifacts/...`.
3. Copy just that jar into its own empty folder, e.g. `C:\PassGauge-dist`.
4. Run (adjust the three paths for your machine):
   ```
   jpackage --type exe --name PassGauge --app-version 1.0 --vendor "Your Name" ^
     --input C:\PassGauge-dist --main-jar PassGauge.jar --main-class com.passgauge.Main ^
     --module-path "C:\javafx-jmods-21" --add-modules javafx.controls,javafx.fxml ^
     --icon packaging\PassGauge-icon.ico ^
     --win-dir-chooser --win-shortcut --win-menu ^
     --dest C:\PassGauge-release
   ```
   If `jpackage` isn't recognized, call it by its full path instead, e.g.
   `& "C:\Program Files\Java\jdk-21\bin\jpackage.exe" ...`.
5. The installer appears at `C:\PassGauge-release\PassGauge-1.0.exe`.

The resulting installer is self-contained — it bundles its own Java +
JavaFX runtime, so whoever installs it doesn't need Java at all.

## Project structure

```
src/com/passgauge/
  Main.java                        - JavaFX entry point
  controller/AppController.java    - wires both modules' UI logic
  controller/RunRecord.java        - one benchmark run (table row / CSV row / chart point)
  strength/StrengthAnalyzer.java   - Module 1: scoring logic (no dependencies)
  strength/StrengthResult.java     - Module 1: analysis result value object
  strength/PasswordGenerator.java  - Module 1: local random password generator
  crack/Cracker.java, SequentialCracker.java, ParallelCracker.java
                                    - Module 2: single/multi-threaded brute force
  util/HashUtil.java                - SHA-256 + keyspace index decoding

resources/com/passgauge/
  view/main.fxml                   - header + two-tab layout
  css/light.css, css/dark.css       - themes (toggled by the dark-mode button)
  images/icon-*.png                 - app/window icon

packaging/
  PassGauge-icon.ico                - multi-resolution icon for the Windows installer
```
