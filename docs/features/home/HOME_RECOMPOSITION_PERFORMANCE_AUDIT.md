# Home Recomposition Performance Audit

- Issue: [#359](https://github.com/Nexters/BandalArt-KMP/issues/359)
- Started: 2026-08-20
- Branch: `perf/359-home-recomposition`

## Scope

Measure the Home scroll path in a release-like target, apply only state-read scope reductions, regenerate the Home baseline profile path, and verify the representative Home interactions do not regress.

## Environment

| Item | Value |
| --- | --- |
| Target variant | `nonMinifiedRelease` via `:baselineprofile` |
| R8 / shrinker | Release configuration, measured through Macrobenchmark |
| Device | `R3CY70P3F4H` |
| Benchmark module | `:baselineprofile` |
| Local signing | Debug keystore through untracked `keystore.properties` |
| Final benchmark context | `SM-S936N`, Android 16 / SDK 36, 5 iterations, `StartupMode.WARM`, `FrameTimingMetric` |

## Benchmark Command

```bash
./gradlew :baselineprofile:connectedNonMinifiedReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nexters.bandalart.baselineprofile.HomeBenchmarks \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

## How to Read This Document

- 최종 판단은 `Final after baseline profile regeneration` 행을 기준으로 한다. 이 행이 state split과 baseline profile 재생성을 모두 반영한 마지막 실기기 측정값이다.
- `frameDurationCpuMs`는 한 프레임을 그리는 동안 앱이 CPU에서 사용한 시간이다. 낮을수록 좋고, 홈 스크롤 작업 자체가 가벼운지 볼 때 사용한다.
- `frameOverrunMs`는 프레임 마감 시간을 얼마나 넘겼는지 나타낸다. 0 이하이면 프레임 예산 안에 들어온 것이고, 양수가 커질수록 jank 가능성이 커진다.
- `P50`, `P90`, `P99`는 각각 전체 프레임 중 50%, 90%, 99% 지점의 값이다. 체감 성능은 평균보다 `P90`과 `P99`를 우선해서 본다.
- 이번 최종 결과는 `frameDurationCpuMs P90 6.96ms`, `frameOverrunMs P90 0.67ms`로 홈 스크롤 대부분의 프레임이 안정권에 들어온 것으로 해석한다.
- `frameOverrunMs P99 8.63ms`는 드문 tail frame이 아직 튈 수 있음을 의미한다. 추가 최적화가 필요하면 새 이슈에서 P99/tail 개선을 별도로 측정한다.
- 같은 기기, 같은 variant, 같은 benchmark journey, 비슷한 frame count끼리만 직접 비교한다. `After bottom sheet/dialog state split` run은 frame count가 달라 직접적인 회귀 판단 근거로 쓰지 않는다.

## Results

| Scenario | Metric | P50 | P90 | P99 | Notes |
| --- | --- | ---: | ---: | ---: | --- |
| Baseline home scroll down/up | `FrameTimingMetric.frameDurationCpuMs` | 4.3ms | 6.8ms | 11.2ms | 5 iterations on `SM-S936N`, `CompilationMode.Partial(BaselineProfileMode.Require)` |
| Baseline home scroll down/up | `FrameTimingMetric.frameOverrunMs` | -2.4ms | 0.7ms | 4.8ms | Same run |
| After `HomeContent` state split | `FrameTimingMetric.frameDurationCpuMs` | 4.3ms | 6.7ms | 11.4ms | Same device and benchmark settings |
| After `HomeContent` state split | `FrameTimingMetric.frameOverrunMs` | -2.4ms | 1.0ms | 5.3ms | Same run |
| After bottom sheet/dialog state split | `FrameTimingMetric.frameDurationCpuMs` | 4.6ms | 8.7ms | 11.2ms | Same device; frame count was much higher, so this run is not directly comparable |
| After bottom sheet/dialog state split | `FrameTimingMetric.frameOverrunMs` | 0.7ms | 9.0ms | 9.6ms | Same run |
| Final after baseline profile regeneration | `FrameTimingMetric.frameDurationCpuMs` | 4.17ms | 6.96ms | 9.61ms | Same device; frame count min/median/max 110/111/192 |
| Final after baseline profile regeneration | `FrameTimingMetric.frameOverrunMs` | -2.93ms | 0.67ms | 8.63ms | Same run |

Trace outputs:

- `baselineprofile/build/outputs/connected_android_test_additional_output/nonMinifiedRelease/connected/SM-S936N - 16/com.nexters.bandalart.baselineprofile-benchmarkData.json`
- `baselineprofile/build/outputs/connected_android_test_additional_output/nonMinifiedRelease/connected/SM-S936N - 16/HomeBenchmarks_homeScrollFrameTiming_iter*.perfetto-trace`

## Diagnosis Checklist

- [x] Record home scroll Macrobenchmark output.
- [x] Use release Macrobenchmark and source-level Argument Change Reasons as the final evidence. Debug Layout Inspector counts were not kept as ground truth because the optimized path was validated on the release-like benchmark target.
- [x] Record Argument Change Reasons for the state-driven hotspots touched in this pass.
- [x] Check release Compose compiler report generation.
- [x] Rank the observed state-driven hotspots before applying state splits.
- [x] Regenerate and commit the Home path baseline profile.
- [x] Verify representative Home interactions.

## Observations

- `HomeContent`, `HomeBottomSheets`, and `HomeDialogs` no longer receive the whole `HomeScreen.State`; each receives only the values it reads.
- `HomeBottomSheets` regenerates generated list titles when `BottomSheetState.BandalartList` is shown.
- The first implementation change in this branch only adds stable measurement tags and a home `FrameTimingMetric` benchmark.
- Splitting `HomeContent` reduced its invalidation surface, but the connected benchmark result stayed effectively flat; further wins likely need hotspot-level recomposition counts.
- The follow-up bottom sheet/dialog split is structurally useful for avoiding unrelated state reads, but its benchmark run had a much larger frame count and should not be compared as a regression without a repeated run.
- `enableComposeCompilerReports=true` and `enableComposeCompilerMetrics=true` were passed to `:feature:home:compileAndroidMain` with `--rerun-tasks`, but no `compose-reports` or `compose-metrics` files were emitted in this KMP module build.
- `:androidApp:generateReleaseBaselineProfile` attempted to start managed device `pixel6Api35` and stalled after AVD setup in this local environment, so the baseline was recorded with `connectedNonMinifiedReleaseAndroidTest` on `SM-S936N`.
- `BaselineProfileGenerator` now launches the target explicitly through `startTargetActivity()`, matching the benchmark path and avoiding the `startActivityAndWait()` framestats launch confirmation failure seen on the connected device.
- The connected Baseline Profile rule produced `/data/misc/profman/com.nexters.bandalart-primary.prof.txt`; the AndroidX parser rejected the device stdout because it included extra `Waiting for app processes to flush profiles...` lines before `Profile saved to ...`. The generated profile was pulled from the device and merged with the existing checked-in profile so previously covered startup/app paths were not removed.
- `composeApp/src/androidRelease/generated/baselineProfiles/baseline-prof.txt` and `startup-prof.txt` are both updated to the merged 50,567-line profile, preserving the existing identical-file convention in this repo.

## Regression Checks

| Flow | Evidence | Result |
| --- | --- | --- |
| Representative icon change | Real device dev app: opened the representative emoji sheet from the Home header, selected the first emoji candidate, and verified the Home header returned with 🔥 rendered | Passed |
| Task completion | `:feature:home:testAndroidHostTest --tests HomePresenterQuickCompletionTest --tests HomePresenterEditTest --tests HomePresenterTest` | Passed |
| Selection transition | Real device dev app: selecting the main Home cell opened the `메인목표 입력` bottom sheet; presenter selection/edit host tests also passed | Passed |

## Hotspot Ranking

| Rank | Scope | Argument change reason | Change |
| ---: | --- | --- | --- |
| 1 | `HomeContent` | Changed: whole `HomeScreen.State` was passed through while content only read chart/header/banner fields | Split into scalar/model parameters used by content |
| 2 | `HomeBottomSheets` | Changed: whole `HomeScreen.State` was passed through while sheet rendering only read sheet, list, emoji, settings, and reminder fields | Split into bottom-sheet-specific parameters |
| 3 | `HomeDialogs` | Changed: whole `HomeScreen.State` was passed through while dialog rendering only read dialog and current bandalart data | Split into dialog-specific parameters |
| 4 | `BandalartChart` | Uncertain: receives current `BandalartUiModel` and cell data; no direct whole-state dependency after this pass | Left unchanged |
| 5 | `HomeHeader` | Uncertain: receives current `BandalartUiModel`, cell data, dropdown state, and event sink | Left unchanged |

## Follow-ups

- No required follow-up remains for issue #359. Further optimization should start from a new issue with a fresh before/after benchmark table.
