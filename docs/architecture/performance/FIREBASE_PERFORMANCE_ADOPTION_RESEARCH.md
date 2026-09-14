# Firebase Performance Monitoring 도입 검토

- 작성일: 2026-09-14
- 기준 브랜치: `origin/main`
- 대상: BandalArt Android/iOS 앱

## 목적

Firebase Performance Monitoring을 도입했을 때 운영 환경에서 얻을 수 있는 정보와, 이 도구만으로는 측정하거나 원인을 규명할 수 없는 영역을 구분한다. 이 문서는 정밀 profiler나 별도 observability 시스템을 Firebase로 대체하는 것을 목표로 하지 않는다.

## 결론

Firebase Performance는 앱 시작, 화면 렌더링, HTTP/S 요청, 특정 사용자 흐름의 소요 시간과 같은 **운영 성능 추세와 이상 세션을 발견하는 도구**로 적합하다. 표본 foreground session에서 CPU user/system time과 heap memory timeline도 확인할 수 있어 느린 trace와 시스템 자원 변화의 시간적 상관관계를 조사할 수 있다.

반면 CPU 사용률 증가, thread 상태, 전체 memory footprint, 발열, 앱 귀속 배터리 소모율을 지속적으로 측정하거나 문제의 코드 원인을 확정하는 도구는 아니다. 이러한 영역은 Android Studio Profiler, Android vitals, Xcode Organizer, MetricKit, Instruments 등 플랫폼 도구로 보완해야 한다.

## 현재 프로젝트 상태

- Android는 Firebase BoM, Google Services, Crashlytics와 앱 초기화 코드가 있지만 Performance SDK와 `com.google.firebase.firebase-perf` plugin은 없다.
- iOS는 Swift Package Manager로 Firebase Analytics와 Crashlytics를 연결하고 `FirebaseApp.configure()`를 호출하지만 `FirebasePerformance` product는 없다.
- Firebase가 문서화한 Performance SDK는 Android와 Apple 등 플랫폼 SDK다. KMP `commonMain`에서 직접 사용하는 공식 SDK는 제공되지 않는다.
- 주요 Compose UI는 Android `MainActivity`와 iOS `ComposeUIViewController` 안에서 Circuit/Compose route로 전환한다. Android의 알림·위젯용 보조 Activity는 별도로 존재한다.
- 네트워크 계층은 Ktor Android/Darwin engine과 Supabase RPC를 사용한다.

## 도입으로 얻을 수 있는 것

| 영역 | 제공 정보 | BandalArt 활용 |
| --- | --- | --- |
| 앱 시작 | 자동 app-start trace와 duration | 릴리스 전후 시작 성능 회귀 확인 |
| 앱 lifecycle | foreground/background activity trace | 사용자 session의 성능 문맥 확인 |
| 화면 렌더링 | slow/frozen frame | Android Activity와 iOS UIViewController host의 렌더링 추세 확인 |
| HTTP/S 요청 | 응답 시간, 응답 코드, 요청·응답 payload 크기 | 백업 RPC 지연과 실패 추세 확인 |
| custom code trace | 지정한 시작·종료 구간의 duration | 홈 최초 로드, 백업 생성·복원 체감 시간 측정 |
| custom metric | trace에 포함한 정수형 수치 | 처리한 항목 수 등 duration 해석에 필요한 보조 값 기록 |
| custom attribute | trace를 나누는 저카디널리티 문자열 | 성공·실패·취소, 데이터 유무 등 제한된 조건별 비교 |
| 표본 session | trace 순서와 시간축, CPU user/system time, heap memory | 느린 trace와 CPU·heap 변화가 같은 시간대에 나타났는지 조사 |
| Console | 앱 버전, OS, 기기, 국가, 네트워크 등 조건별 비교와 percentile | 릴리스 전후 P50/P90 추세와 이상 구간 확인 |

### 권장 custom trace

초기 도입은 다음 사용자 흐름으로 제한한다.

| trace | 시작 | 종료 | 목적 |
| --- | --- | --- | --- |
| `home_initial_load` | 홈 데이터 로드 시작 | 최초 사용 가능한 UI 상태 | app-start trace와 실제 준비 시간의 차이 확인 |
| `backup_create` | 로컬 snapshot 생성 시작 | 원격 저장 성공·실패·취소 | 직렬화, 로컬 조회, 업로드를 포함한 체감 시간 확인 |
| `backup_restore` | 원격 조회 시작 | validation과 로컬 반영 성공·실패·취소 | 다운로드와 로컬 복원 병목 후보 확인 |

모든 trace는 성공, 실패, coroutine 취소 경로에서 종료해야 하며 원래 예외와 `CancellationException`을 재전파해야 한다. attribute에는 device key, 사용자·콘텐츠 ID, 제목·설명, 원문 URL/query, payload, 오류 메시지를 넣지 않는다.

## Firebase Performance만으로 할 수 없는 것

| 영역 | 한계 | 보완 도구 |
| --- | --- | --- |
| CPU | 실시간 process CPU 사용률, 코어별 사용률, hot method, call stack, 사용시간별 증가율을 자동 제공하지 않음 | Android Studio CPU Profiler, Instruments, 필요 시 플랫폼 CPU time API |
| Memory | session heap 추세만 제공하며 RSS/PSS, native/GPU memory, allocation 원인, leak을 분석하지 못함 | Android Studio Memory Profiler, Android vitals, Xcode Memory Report, Allocations/VM Tracker |
| Thread | process thread 수, thread별 실행·대기·blocking 상태를 제공하지 않음 | Android Studio Live Telemetry/CPU Profiler, Instruments Thread State/Time Profiler |
| 발열 | thermal state, thermal headroom, 온도와 throttling 상태를 기본 metric으로 제공하지 않음 | Android `PowerManager`, Apple `ProcessInfo.thermalState`, 플랫폼 profiler |
| 배터리 | 앱이 소비한 에너지와 배터리 소모율을 제공하지 않음 | Android Power Profiler/vitals, Xcode Organizer, MetricKit, Instruments Power Profiler |
| 실시간성 | 고빈도 resource sampling과 전체 session 원시 시계열 수집을 위한 도구가 아님 | 별도 telemetry collector 또는 profiler |
| 집계·export | session CPU/heap 파생지표의 자유로운 원시 데이터 export와 custom dashboard를 제공하지 않음 | 목적에 맞는 별도 telemetry/export 설계 |
| 원인 분석 | trace와 CPU·heap의 시간적 상관관계는 보여주지만 원인 코드를 확정하지 못함 | 재현 가능한 scenario와 platform profiler |

### Compose 화면 자동 trace의 한계

자동 screen rendering trace는 Android Activity/Fragment 또는 iOS UIViewController를 기준으로 한다. BandalArt의 주요 Compose UI는 하나의 `MainActivity`/`ComposeUIViewController` 안에서 Circuit/Compose route를 전환하므로, 자동 trace만으로 홈·설정·백업 같은 논리 화면을 분리하기 어렵다. 사용자 관점의 흐름은 custom trace로 보완한다.

### Ktor와 Supabase 요청의 한계

Firebase가 Ktor Android/Darwin 요청을 원하는 URL pattern으로 자동 수집한다고 전제하지 않는다. 양 플랫폼의 실기기와 Firebase Console에서 `get_device_backup`/`put_device_backup` 요청을 먼저 확인한다. 자동 trace가 누락될 때만 platform별 `HttpMetric`을 추가하고 동일 요청의 자동·수동 중복 집계를 방지한다.

## CPU와 사용시간의 해석

Firebase Console은 임의 표본 foreground session에서 앱이 소비한 CPU user/system time을 보여준다. 이는 특정 custom trace 구간의 전용 CPU 측정값이 아니라 trace가 포함된 전체 session의 timeline이므로 병목 원인을 확정하는 값으로 사용하지 않는다.

누적 CPU time은 사용시간이 길수록 자연스럽게 증가한다. Firebase Console에서 동일 session 구간의 시작·종료 CPU 수치를 추출할 수 있는 경우에 한해, 사용시간별 CPU 부하를 다음과 같이 정규화할 수 있다.

```text
평균 one-core-equivalent CPU 사용률
= ((Δuser CPU ms + Δsystem CPU ms) / foreground duration ms) × 100
```

CPU time은 여러 thread/core의 실행시간 합계이므로 결과가 100%를 넘을 수 있다. 예를 들어 foreground 10분 동안 CPU time이 60초 증가했다면 평균 one-core-equivalent 사용률은 약 10%다.

운영 집계가 필요하다면 `0~5분`, `5~15분`, `15~30분`, `30분 이상`처럼 표본이 확보되는 소수 구간으로 나누고, 앱 버전·플랫폼·기기·OS·scenario를 맞춘 뒤 정규화된 지표의 P50/P90을 비교해야 한다. 그러나 Firebase Console은 이 계산이나 bucket별 집계를 자동 제공하지 않으므로, 초기 도입에서는 표본 session 몇 건에서 계산 가능성과 한계만 확인한다. 지속적인 dashboard가 필요해지면 별도 telemetry/export 작업으로 분리한다.

## Memory의 해석

Firebase에서 확인하는 값은 session의 heap memory 추세다. 전체 physical footprint나 native/GPU allocation을 의미하지 않는다. 또한 GC 때문에 값이 오르내리므로 단순 시작·종료 차이를 memory leak으로 해석하면 안 된다.

장시간 사용 또는 반복 작업 후 GC 이후 baseline이 계속 상승하는 현상은 Android Studio Memory Profiler와 Xcode Allocations/VM Tracker에서 동일 scenario를 반복해 확인한다.

## 발열·Thread·배터리의 해석

### 발열

- Android는 API 29 이상에서 `PowerManager`의 thermal status를, API 30 이상에서 지원 기기의 thermal headroom을 조회할 수 있다.
- Apple 플랫폼은 `ProcessInfo.thermalState`와 변경 알림을 제공한다.
- 이 값들은 Firebase 기본 metric이 아니다. 필요성이 확인될 때만 `thermal_start`/`thermal_end`처럼 제한된 상태값을 custom attribute로 기록하는 후속 작업을 검토한다.
- 고빈도 온도·headroom sampling은 관측 비용과 기기별 편차가 크므로 초기 범위에서 제외한다.

### Thread

- Android에서 process의 Java thread 순간값을 구하는 방법은 있지만 profiler 자체가 추가하는 thread와 thread pool 변동 때문에 운영 지표로 해석하기 어렵다.
- iOS에는 Android와 동등한 간단한 고수준 production thread-count API가 없다.
- 초기 도입에서는 Firebase metric으로 보내지 않고 양 플랫폼 profiler에서 thread 수, 상태, blocking을 조사한다.

### 배터리

- Android `BatteryManager`의 battery level, charge counter, 순간·평균 전류는 기기 전체 값이다.
- iOS `UIDevice`의 battery level/state도 기기 전체 상태다.
- 다른 앱, 화면 밝기, 네트워크, 충전 여부, 발열의 영향을 앱별로 분리하지 못하므로 이 값의 변화량을 BandalArt 배터리 소모율로 해석하지 않는다.
- 배터리·충전 상태는 필요할 경우 실험 조건이나 분석 문맥으로만 사용한다. 앱 전력 영향은 동일 기기와 통제된 조건에서 Android Power Profiler 또는 Apple Power Profiler로 반복 측정한다.

## 도입 판단과 역할 분담

Firebase Performance의 역할은 다음으로 제한한다.

1. 운영 환경의 app-start, host screen rendering, network 성능 추세 확인
2. 핵심 사용자 흐름의 duration과 성공·실패 조건 비교
3. 릴리스 전후 성능 회귀와 느린 표본 session 발견
4. 느린 trace와 session CPU·heap 변화의 시간적 상관관계 조사

다음 영역은 Firebase 도입 작업에 포함하지 않는다.

- 모든 DAO/repository/Composable에 trace 추가
- thread 수, thermal headroom, battery level/current의 지속 sampling
- CPU·memory·battery 원인 분석을 Firebase로 대체
- 사용시간 bucket별 CPU 증가율 dashboard 또는 별도 AI observability collector 구축
- 사용자별·콘텐츠별 성능 분석
- Firebase SDK 전체 업그레이드

## 도입 시 검증 항목

- Android와 iOS에서 자동 app-start와 lifecycle trace가 수집되는지 확인한다.
- host screen의 slow/frozen frame 데이터가 수집되는지 확인하고 논리 route 미구분 한계를 기록한다.
- Ktor/Supabase 요청의 자동 network trace 수집 여부와 민감 정보 노출 여부를 양 플랫폼에서 확인한다.
- `home_initial_load`, `backup_create`, `backup_restore` trace가 성공·실패·취소 경로에서 정상 종료되는지 확인한다.
- 표본 session이 제공되면 CPU user/system time과 heap timeline을 확인하고, 제공되지 않으면 수집 상태와 재확인 시점을 기록한다.
- Android Performance Gradle plugin과 현재 AGP, R8, Baseline Profile task의 호환성을 확인한다.
- iOS Debug 실기기에서 event 전송을, Release archive에서 package linking과 product 포함 여부를 확인한다. 전송 검증은 archive에서 export·설치한 Release 앱 또는 TestFlight build를 실제 실행해 수행한다.
- Play Data Safety, App Store App Privacy, `PrivacyInfo.xcprivacy`와 실제 수집 항목의 정합성을 재검토한다.
- 내부 배포에서 SDK overhead와 신규 crash/ANR을 확인한 뒤 단계적으로 release한다.

## 공식 자료

### Firebase

- [Firebase Performance Monitoring](https://firebase.google.com/docs/perf-mon)
- [Android 시작 가이드](https://firebase.google.com/docs/perf-mon/get-started-android)
- [Apple 플랫폼 시작 가이드](https://firebase.google.com/docs/perf-mon/get-started-ios)
- [Console의 표본 session·CPU·memory](https://firebase.google.com/docs/perf-mon/console)
- [화면 렌더링 trace](https://firebase.google.com/docs/perf-mon/screen-traces)
- [custom code trace와 metric·attribute](https://firebase.google.com/docs/perf-mon/custom-code-traces)
- [custom network trace](https://firebase.google.com/docs/perf-mon/custom-network-traces)
- [sampling과 on-device rate limit](https://firebase.google.com/docs/perf-mon/troubleshooting?platform=android)
- [Performance Monitoring 개인정보와 보존 정책](https://firebase.google.com/support/privacy)

### Android

- [Android Studio Live Telemetry](https://developer.android.com/studio/profile/inspect-app-live)
- [Android Studio CPU/system trace](https://developer.android.com/studio/profile/cpu-profiler)
- [Android vitals](https://developer.android.com/topic/performance/vitals)
- [`PowerManager` thermal API](https://developer.android.com/reference/android/os/PowerManager)
- [`BatteryManager`](https://developer.android.com/reference/android/os/BatteryManager)

### Apple

- [`ProcessInfo` thermal state](https://developer.apple.com/documentation/foundation/processinfo/thermalstate-swift.property)
- [Xcode memory 사용 분석](https://developer.apple.com/documentation/xcode/gathering-information-about-memory-use)
- [앱 배터리 사용 분석](https://developer.apple.com/documentation/xcode/analyzing-your-app-s-battery-use)
- [MetricKit CPU metric](https://developer.apple.com/documentation/metrickit/mxcpumetric)
- [Instruments Power Profiler](https://developer.apple.com/documentation/xcode/measuring-your-app-s-power-use-with-power-profiler)
