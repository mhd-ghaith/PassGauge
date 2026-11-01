# 🔒 PassGauge

**Password intelligence and parallel-programming benchmarking, in one desktop app.**

![platform](https://img.shields.io/badge/platform-Windows%20%7C%20JavaFX-6366F1)
![language](https://img.shields.io/badge/language-Java-4F46E5)
![license](https://img.shields.io/badge/license-MIT-818CF8)
![docs](https://img.shields.io/badge/docs-technical%20report-818CF8)

PassGauge is a two-module JavaFX desktop application built around one idea:
how strong a password is, and how fast you can search for one, both come
down to the same underlying problem — and the second one is a great way to
*demonstrate* the first.

> 📄 **Looking for the full write-up?** See
> [`docs/PassGauge-Technical-Report.docx`](docs/PassGauge-Technical-Report.docx)
> for an in-depth explanation of the theory, architecture, multithreaded
> implementation, and deployment process behind this project.

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

For the theory behind thread management, synchronization, speedup, and
Amdahl's Law as applied in this module, see Sections 2 and 5 of the
[technical report](docs/PassGauge-Technical-Report.docx).

## Download

Prebuilt Windows installer available under
[**Releases**](../../releases) — no Java installation required, it bundles
its own runtime.

## Documentation

| File | Contents |
|---|---|
| [`docs/PassGauge-Technical-Report.docx`](docs/PassGauge-Technical-Report.docx) | Full technical report — theory, architecture, multithreading implementation, UI design, deployment process, testing, and conclusions |
| [`SETUP.md`](SETUP.md) | Build-from-source instructions: IntelliJ project setup and packaging the Windows `.exe` installer with `jpackage` |
| [`LICENSE`](LICENSE) | MIT License |

## Build from source

See [SETUP.md](SETUP.md) for full instructions, including:
- Setting up the project in IntelliJ (JavaFX SDK, library, run config)
- Packaging it as a Windows `.exe` installer with `jpackage`

The deployment process itself — what `jlink`/`jpackage`/WiX actually do
under the hood, not just the click-by-click steps — is documented in
Section 7 of the [technical report](docs/PassGauge-Technical-Report.docx).

## Tech

Plain Java + JavaFX (no external dependencies, no build tool) — `ExecutorService`-based
thread pool, atomics for lock-free synchronization, JavaFX `Task` for background
work off the UI thread, and a hand-rolled strength analyzer inspired by zxcvbn's
approach to password scoring. See the [technical report](docs/PassGauge-Technical-Report.docx)
for the full architecture breakdown.

## License

[MIT](LICENSE)
