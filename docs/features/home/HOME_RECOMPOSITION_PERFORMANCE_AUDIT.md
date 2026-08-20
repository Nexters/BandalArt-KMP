# Home Recomposition Performance Audit

- Issue: [#359](https://github.com/Nexters/BandalArt-KMP/issues/359)
- Started: 2026-08-20
- Branch: `perf/359-home-recomposition`

## Scope

The first pass is measurement-only. Product UI behavior should not change before release-mode baseline numbers are recorded.

## Environment

| Item | Value |
| --- | --- |
| Target variant | `nonMinifiedRelease` via `:baselineprofile` |
| R8 / shrinker | Release configuration, measured through Macrobenchmark |
| Device | `R3CY70P3F4H` |
| Benchmark module | `:baselineprofile` |
| Local signing | Debug keystore through untracked `keystore.properties` |

## Benchmark Command

```bash
./gradlew :baselineprofile:connectedNonMinifiedReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nexters.bandalart.baselineprofile.HomeBenchmarks \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

## Results

| Scenario | Metric | P50 | P90 | P99 | Notes |
| --- | --- | ---: | ---: | ---: | --- |
| Baseline home scroll down/up | `FrameTimingMetric.frameDurationCpuMs` | 4.3ms | 6.8ms | 11.2ms | 5 iterations on `SM-S936N`, `CompilationMode.Partial(BaselineProfileMode.Require)` |
| Baseline home scroll down/up | `FrameTimingMetric.frameOverrunMs` | -2.4ms | 0.7ms | 4.8ms | Same run |
| After `HomeContent` state split | `FrameTimingMetric.frameDurationCpuMs` | 4.3ms | 6.7ms | 11.4ms | Same device and benchmark settings |
| After `HomeContent` state split | `FrameTimingMetric.frameOverrunMs` | -2.4ms | 1.0ms | 5.3ms | Same run |
| After bottom sheet/dialog state split | `FrameTimingMetric.frameDurationCpuMs` | 4.6ms | 8.7ms | 11.2ms | Same device; frame count was much higher, so this run is not directly comparable |
| After bottom sheet/dialog state split | `FrameTimingMetric.frameOverrunMs` | 0.7ms | 9.0ms | 9.6ms | Same run |

Trace outputs:

- `baselineprofile/build/outputs/connected_android_test_additional_output/nonMinifiedRelease/connected/SM-S936N - 16/com.nexters.bandalart.baselineprofile-benchmarkData.json`
- `baselineprofile/build/outputs/connected_android_test_additional_output/nonMinifiedRelease/connected/SM-S936N - 16/HomeBenchmarks_homeScrollFrameTiming_iter*.perfetto-trace`

## Diagnosis Checklist

- [x] Record home scroll Macrobenchmark output.
- [ ] Capture Layout Inspector recomposition and skip counts for `Home`, `HomeContent`, `HomeHeader`, `BandalartChart`, `HomeBottomSheets`, and `HomeDialogs`.
- [x] Record Argument Change Reasons for the state-driven hotspots touched in this pass.
- [x] Check release Compose compiler report generation.
- [x] Rank the observed state-driven hotspots before applying state splits.

## Observations

- `HomeContent`, `HomeBottomSheets`, and `HomeDialogs` no longer receive the whole `HomeScreen.State`; each receives only the values it reads.
- `HomeBottomSheets` regenerates generated list titles when `BottomSheetState.BandalartList` is shown.
- The first implementation change in this branch only adds stable measurement tags and a home `FrameTimingMetric` benchmark.
- Splitting `HomeContent` reduced its invalidation surface, but the connected benchmark result stayed effectively flat; further wins likely need hotspot-level recomposition counts.
- The follow-up bottom sheet/dialog split is structurally useful for avoiding unrelated state reads, but its benchmark run had a much larger frame count and should not be compared as a regression without a repeated run.
- `enableComposeCompilerReports=true` and `enableComposeCompilerMetrics=true` were passed to `:feature:home:compileAndroidMain` with `--rerun-tasks`, but no `compose-reports` or `compose-metrics` files were emitted in this KMP module build.
- `:androidApp:generateReleaseBaselineProfile` attempted to start managed device `pixel6Api35` and stalled after AVD setup in this local environment, so the baseline was recorded with `connectedNonMinifiedReleaseAndroidTest` on `SM-S936N`.

## Hotspot Ranking

| Rank | Scope | Argument change reason | Change |
| ---: | --- | --- | --- |
| 1 | `HomeContent` | Changed: whole `HomeScreen.State` was passed through while content only read chart/header/banner fields | Split into scalar/model parameters used by content |
| 2 | `HomeBottomSheets` | Changed: whole `HomeScreen.State` was passed through while sheet rendering only read sheet, list, emoji, settings, and reminder fields | Split into bottom-sheet-specific parameters |
| 3 | `HomeDialogs` | Changed: whole `HomeScreen.State` was passed through while dialog rendering only read dialog and current bandalart data | Split into dialog-specific parameters |
| 4 | `BandalartChart` | Uncertain: receives current `BandalartUiModel` and cell data; no direct whole-state dependency after this pass | Left unchanged |
| 5 | `HomeHeader` | Uncertain: receives current `BandalartUiModel`, cell data, dropdown state, and event sink | Left unchanged |

## Follow-ups

- Apply one targeted optimization only after the baseline table is filled.
- Re-run the same benchmark after each optimization and record the delta here.
