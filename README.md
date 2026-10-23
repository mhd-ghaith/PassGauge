# 🔒 PassGauge

**Password intelligence and parallel-programming benchmarking, in one desktop app.**

![platform](https://img.shields.io/badge/platform-Windows%20%7C%20JavaFX-6366F1)
![language](https://img.shields.io/badge/language-Java-4F46E5)
![license](https://img.shields.io/badge/license-MIT-818CF8)

PassGauge is a two-module JavaFX desktop application built around one idea:
how strong a password is, and how fast you can search for one, both come
down to the same underlying problem — and the second one is a great way to
*demonstrate* the first.

<!--
  Add a screenshot or two here once you have them — drag an image into
  this README on GitHub's web editor and it'll generate the markdown for
  you automatically. For example:
  ![Strength Analyzer tab](docs/screenshot-strength.png)
  ![Benchmark tab](docs/screenshot-benchmark.png)
-->

## Modules

### 🔍 Strength Analyzer
Scores a password locally (no network calls, nothing logged or saved),
with a live 5-segment strength meter, feedback on specific weaknesses
(length, common passwords, sequential/keyboard patterns, repeats, dates),
concrete suggestions, a plain-English explanation of the score, and an
optional local password generator.

### ⚡ Parallel Benchmark
Brute-forces a password's SHA-256 hash two ways — single-threaded and
multi-threaded over a configurable thread pool — and compares them:
execution time, **speedup** (`T_sequential / T_parallel`), and **parallel
efficiency** (`speedup / threads`). Includes a dynamic chart (switch
between time/speedup/efficiency views), full run history, CSV export, and
live system info (CPU cores, Java version).

## Download

Prebuilt Windows installer available under
[**Releases**](../../releases) — no Java installation required, it bundles
its own runtime.

## Build from source

See [SETUP.md](SETUP.md) for full instructions, including:
- Setting up the project in IntelliJ (JavaFX SDK, library, run config)
- Packaging it as a Windows `.exe` installer with `jpackage`

## Tech

Plain Java + JavaFX (no external dependencies, no build tool) — `ExecutorService`-based
thread pool, atomics for lock-free synchronization, JavaFX `Task` for background
work off the UI thread, and a hand-rolled strength analyzer inspired by zxcvbn's
approach to password scoring.

## License

[MIT](LICENSE)
