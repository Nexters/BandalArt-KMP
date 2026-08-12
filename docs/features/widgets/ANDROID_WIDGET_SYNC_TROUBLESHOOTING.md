# Android 위젯 동기화 트러블슈팅

Android `2.3.2 (20302)`라는 버전만으로는 위젯 갱신 수정 #326의 포함 여부를 확인할 수 없다. 배포 기준 커밋을 먼저 확인하고, 수정이 포함된 빌드에서도 이전 값이 계속 남으면 플랫폼 지연이 아닌 코드 결함으로 추적한다.

## 문서 목적

이 문서는 앱에서 반다라트, 세부 목표 또는 달성률을 바꾼 뒤 Android 홈 화면 위젯이 이전 내용을 계속 표시하는 문제를 진단하기 위한 기준이다. 구현 규칙은 [Android 위젯 기능 가이드](ANDROID_WIDGET_FEATURE_GUIDE.md), 표시 대상 우선순위는 [Android 위젯 표시 대상 선택 가이드](ANDROID_WIDGET_SELECTION_GUIDE.md)를 함께 따른다.

플랫폼 구조와 공식 자료의 상세 근거는 [Android와 iOS 위젯 동기화 모델 비교](ANDROID_IOS_WIDGET_SYNC_RESEARCH.md)에 정리한다.

## 2.3.2에서 확인할 배포 기준

Android `2.3.2 (20302)`에서 위젯이 늦게 바뀌거나 이전 달성률을 계속 표시하는 현상은 Android 위젯의 불가피한 한계로 확정할 수 없다. `2.3.2` 버전 준비 커밋은 #326보다 먼저 병합됐지만, 실제 AAB(Android App Bundle)를 #326 병합 후 최신 `main`에서 만들었다면 수정은 포함된다.

| 구분 | 커밋/PR | 시각과 포함 관계 |
| --- | --- | --- |
| 2.3.2 버전 준비 | `a8bae441`, PR #324 | 2026-08-12 08:12 UTC에 `main` 병합 |
| 최신 snapshot 직렬화 수정 | `c888d15b`, PR #326 | 2026-08-12 11:44 UTC에 `main` 병합 |
| 현재 `main` 포함 여부 | merge `1b5f3b72` | #326 포함 |

`git merge-base --is-ancestor c888d15b a8bae441`의 종료 코드는 `1`이다. 이 결과는 버전 준비 커밋에 #326이 없다는 뜻이며, 나중에 최신 `main`에서 만든 AAB의 포함 관계까지 증명하지는 않는다.

현재 설치 앱은 `versionName`과 `versionCode`만으로 배포 기준 Git SHA를 보여주지 않는다. 따라서 2.3.2의 실제 포함 관계는 배포 실행의 `headSha`나 당시 생성한 source manifest로 확인해야 한다. 새 진단 로그나 수정이 필요하면 같은 versionCode를 Play에 다시 올릴 수 없으므로 `2.3.3 (20303)` 이상의 새 Internal 빌드를 만든다.

## 현재 갱신 파이프라인

앱 프로세스가 살아 있는 동안의 로컬 변경은 다음 경로로 처리한다.

1. 앱이 Room의 반다라트·세부 목표·할 일 또는 DataStore의 최근 선택을 변경한다.
2. `BandalartApplication`이 반다라트 목록, 최근 반다라트, 최근 세부 목표 Flow를 관찰한다.
3. 데이터 변경 또는 프로세스 `ON_STOP`에서 `BandalartWidgetRefreshRunner.refresh()`를 요청한다.
4. refresh runner가 여러 요청을 `Mutex`로 직렬화한다.
5. `BandalartGlanceWidget.updateAll()`이 설치된 모든 위젯의 갱신을 요청한다.
6. `provideGlance()`가 최신 Room/DataStore 값을 다시 읽어 `RemoteViews`를 만들고 launcher host에 전달한다.

정기 polling은 사용하지 않는다. 앱에서 방금 변경한 로컬 데이터는 위 경로로 갱신하지만, 앱 프로세스가 죽은 동안 서버에서만 바뀐 데이터는 별도의 broadcast나 WorkManager 동기화 계기가 없으면 자동으로 반영되지 않는다.

## #326에서 해결한 경쟁 조건

데이터 변경 갱신과 앱 백그라운드 갱신이 동시에 실행되면 다음 순서가 가능했다.

1. 백그라운드 갱신 A가 이전 달성률을 읽는다.
2. 앱 저장이 완료되고 갱신 B가 최신 달성률을 읽는다.
3. B가 먼저 launcher에 최신 화면을 전달한다.
4. 늦게 끝난 A가 이전 화면을 다시 전달해 최신 화면을 덮어쓴다.

#326은 갱신 전체를 직렬화해 이전 요청과 최신 요청이 서로 추월하지 못하게 했다. 다음 회귀 테스트가 이 순서 역전을 고정한다.

```shell
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
  ./gradlew :androidApp:testDebugUnitTest \
  --tests 'com.nexters.bandalart.widget.BandalartWidgetRefreshRunnerTest'
```

검증 시 refresh runner의 `Mutex`를 제거하면 `latest progress wins when background and database refresh overlap`가 실패하고, 현재 `main` 구현에서는 통과한다.

## Android/Glance의 실제 한계

`updateAll()`은 launcher 화면을 앱의 Compose UI처럼 같은 프레임에 다시 그리는 API가 아니다. Android 공식 문서에 따르면 앱 위젯은 다른 프로세스에서 host되며, Glance가 콘텐츠를 `RemoteViews`로 다시 만들고 host에 전송한다. 따라서 호출 직후 한두 프레임 안에 표시된다는 실시간 보장은 없고 기기와 launcher에 따라 짧은 비동기 지연은 생길 수 있다.

하지만 아래 시나리오는 정기 polling이 없어도 앱이 명시적으로 `updateAll()`을 요청하므로 지원해야 하는 동작이다.

- 앱에서 A 반다라트를 보다가 B로 이동한 뒤 시스템 홈으로 이동하면 B가 표시된다.
- 앱에서 할 일 완료 상태나 달성률을 바꾼 뒤 홈으로 이동하면 최신 값이 표시된다.
- 앱에서 세부 목표를 바꾼 뒤 홈으로 이동하면 해당 반다라트의 마지막 세부 목표가 표시된다.

따라서 잠깐 늦게 바뀌는 것은 플랫폼 특성일 수 있지만, 홈으로 이동한 뒤에도 계속 이전 값이 남거나 앱을 다시 열고 닫아야만 바뀌는 현상은 정상 한계로 분류하지 않는다.

앱 프로세스가 깨어 있지 않을 때의 주기 갱신은 별도 문제다. Android 공식 문서는 `updatePeriodMillis`가 30분보다 자주 실행되지 않으며, WorkManager 주기 작업은 15분 이상의 갱신에 사용하도록 안내한다. 이 방식은 배터리 비용이 있고, 앱에서 방금 수행한 로컬 변경을 즉시 반영하는 현재 문제의 대체 수단으로 사용하지 않는다.

## 현재 Android가 iOS보다 문제 지점이 많은 이유

두 플랫폼 모두 위젯 host가 앱 UI와 분리되어 있으므로 즉시 갱신을 보장하지 않는다. 현재 반다라트 구현에서는 Android의 갱신 입력과 상태 분기가 더 많아서 동기화 결함이 드러날 지점도 많다.

| 비교 지점 | Android 현재 구현 | iOS 현재 구현 | Android에서 늘어나는 위험 |
| --- | --- | --- | --- |
| 화면 host | 제조사 또는 사용자가 선택한 launcher가 `RemoteViews`를 표시 | Apple의 WidgetKit과 시스템 host가 timeline을 표시 | launcher별 캐시·배치·크기 처리 차이를 함께 검증해야 함 |
| 갱신 입력 | Room Flow, 최근 반다라트 Flow, 최근 세부 목표 Flow, 프로세스 `ON_STOP`, 위젯 action | 반다라트 Flow, 원자적인 최근 선택 Flow, 앱 활성화, 위젯 intent | 여러 Android 요청이 겹치거나 순서가 바뀔 가능성이 큼 |
| 실행 중 갱신 | 실행 중인 `provideGlance()`를 새 `updateAll()`이 재시작하지 않음 | WidgetKit이 reload 요청을 받고 새 timeline 요청 시점을 관리 | Android session이 최신 데이터를 관찰하지 않으면 후속 요청이 와도 이전 snapshot을 유지할 수 있음 |
| 갱신 시점의 읽기 | `updateAll()` 처리 중 앱 프로세스가 snapshot을 읽고 각 인스턴스를 갱신 | 앱은 선택을 App Group에 먼저 쓰고 timeline reload를 요청하며, extension이 timeline 생성 시 snapshot을 읽음 | Android는 먼저 읽은 snapshot의 갱신이 늦게 끝나면 이전 화면을 덮을 수 있었음 |
| 최근 선택 | 반다라트 ID와 해당 세부 목표 ID를 `provideGlance()`에서 따로 읽음 | 한 Preferences snapshot에서 만든 `recentBandalartSelection`을 먼저 기록 | Android는 두 읽기 사이에 선택이 바뀌면 조합이 일시적으로 어긋날 여지가 남음 |
| 인스턴스 상태 | 각 위젯의 Glance 설정값과 앱의 최근 선택 fallback을 함께 해석 | 현재 intent는 매개변수가 없고 모든 위젯이 같은 최근 선택을 사용 | 여러 Android 인스턴스에서 설정 상태와 최근 상태의 조합 수가 늘어남 |
| 백그라운드 정책 | 앱 프로세스와 launcher가 제조사별 절전 정책의 영향을 받을 수 있음 | extension 실행과 timeline 갱신을 WidgetKit이 통제 | Android 실기기 QA에 제조사·launcher 조합이 추가됨 |

Android의 `Mutex`는 앱이 시작한 갱신 요청끼리 순서를 보장한다. 실행 중인 Glance session을 다시 시작하거나 launcher가 화면에 반영하는 시점까지 통제하지는 못한다. 따라서 회귀 테스트 통과는 앱의 동시 refresh 순서를 고정했다는 뜻이며, 실행 중 session이 최신 값을 다시 읽거나 홈 화면에 즉시 보인다는 보장은 아니다.

iOS도 자동으로 안전한 것은 아니다. App Group 데이터 기록과 extension의 데이터베이스 읽기 사이에 순서 문제가 생길 수 있고, WidgetKit도 요청마다 즉시 새 timeline을 표시한다고 보장하지 않는다. Apple은 WidgetKit이 여러 위젯의 reload를 합치거나 사용 빈도에 따라 reload 예산을 조정할 수 있다고 설명한다.

현재 iOS 구현은 `최근 선택을 한 번에 읽기 → App Group에 쓰기 → timeline reload` 순서를 고정하고 위젯별 선택 분기를 제거해 애플리케이션 수준의 경쟁 조건을 줄였다. 즉 iOS 플랫폼이 항상 더 빠른 것이 아니라, 현재 iOS 구현이 상태 조합과 갱신 요청 경로를 더 좁게 유지한다.

Android에서 남은 위험을 줄일 때는 다음 순서로 검토한다.

1. 실행 중인 `provideGlance()`가 Room/DataStore 변경을 관찰하게 하거나, session 종료 후 최신 generation을 다시 갱신하는 구조를 검토한다.
2. `provideGlance()`가 반다라트와 세부 목표를 따로 읽지 않고 `recentBandalartSelection` 하나를 읽게 한다.
3. refresh 요청에 증가하는 generation과 원인을 기록해 마지막 generation이 실제 snapshot 생성까지 도달했는지 확인한다.
4. `updateAll()` 완료와 launcher 표시 완료를 같은 의미로 기록하지 않는다.
5. Pixel 계열 launcher와 Samsung One UI launcher에서 단일·복수 위젯을 각각 검증한다.
6. 배포 기록에 source commit을 남겨 같은 버전 이름의 서로 다른 소스를 구분한다.

## 실기기 재현 및 판정 절차

### 1. 설치 빌드 확인

```shell
adb shell dumpsys package com.nexters.bandalart \
  | rg 'versionName|versionCode'
```

- `2.3.2 / 20302`: 버전만으로 #326 포함 여부를 판단하지 말고 배포 기준 커밋을 확인한다.
- `2.3.3 / 20303` 이상: 새 진단 로그나 후속 수정이 포함된 경우 해당 기준 커밋을 기록한다.

### 2. 마지막 반다라트 전환

1. 위젯에서 A가 표시되는지 확인한다.
2. 위젯을 눌러 앱을 연다.
3. 앱에서 B 반다라트로 이동한다.
4. 시스템 홈 버튼으로 launcher에 돌아간다.
5. 위젯이 B로 바뀌는지 확인한다.

### 3. 달성률 갱신

1. 앱에서 현재 반다라트의 할 일 하나를 완료하거나 해제한다.
2. 앱 안의 달성률이 바뀐 것을 확인한다.
3. 시스템 홈으로 이동한다.
4. 위젯의 체크 상태와 달성률이 모두 최신인지 확인한다.

### 4. 세부 목표 선택

1. A의 세부 목표 A-1을 연다.
2. B로 이동해 B-1을 연다.
3. A로 돌아온 뒤 시스템 홈으로 이동한다.
4. 위젯이 A와 A-1을 표시하는지 확인한다.

### 판정

| 결과 | 판정 |
| --- | --- |
| launcher 진입 직후 잠깐 이전 값이었다가 곧 최신 값으로 바뀜 | 비동기 host 전달 지연 가능 |
| 최신 Internal 빌드에서도 이전 값이 계속 유지됨 | 갱신 요청 누락 또는 launcher 전달 실패로 새 이슈 등록 |
| 앱을 다시 열고 닫아야만 갱신됨 | 데이터 변경 관찰 경로 누락 가능 |
| 위젯을 삭제 후 다시 추가해야만 갱신됨 | Glance 인스턴스 상태 또는 provider/launcher 문제 가능 |
| 앱 프로세스가 죽은 동안 외부 데이터만 변경됨 | 현재 비목표인 백그라운드 동기화 계기 필요 |

## 추가 진단 시 수집할 정보

- 앱 `versionName`, `versionCode`, 설치 시각
- 배포 workflow URL과 `headSha` 또는 로컬 배포 source commit
- 기기 제조사, Android 버전, launcher 이름과 버전
- A → B 이동, 할 일 변경, 홈 이동을 포함한 화면 녹화
- 홈 이동 후 실제로 기다린 시간과 앱을 다시 열었을 때의 변화
- 위젯 크기와 설치된 위젯 개수
- `adb shell dumpsys appwidget`의 해당 provider 상태

release 빌드는 일반 debug 로그를 항상 출력하지 않으므로, 증상이 최신 빌드에서도 남으면 `데이터 저장 완료 → refresh 요청 시작/종료 → provideGlance가 읽은 선택 ID와 달성률` 세 경계에 식별 가능한 임시 진단 로그를 추가한다. 전체 로그를 무작정 늘리지 않는다.

## 재발 방지용 릴리스 확인

수정 PR이 실제 배포 커밋에 포함됐는지 릴리스 전에 확인한다.

```shell
git merge-base --is-ancestor fix_commit release_commit
```

종료 코드가 `0`이어야 포함된 것이다. 버전 준비 커밋과 실제 배포 커밋을 혼동하지 말고 Release CD 실행의 `headSha`를 배포 기록에 남긴다. 앱의 진단 화면이나 `BuildConfig`에 source commit을 노출하는 방안도 후속으로 검토한다.

## 관련 자료

- [Android 위젯 기능 가이드](ANDROID_WIDGET_FEATURE_GUIDE.md)
- [Android 위젯 표시 대상 선택 가이드](ANDROID_WIDGET_SELECTION_GUIDE.md)
- [Android와 iOS 위젯 동기화 모델 비교](ANDROID_IOS_WIDGET_SYNC_RESEARCH.md)
- [PR #320: 앱 백그라운드 전환 시 위젯 갱신](https://github.com/Nexters/BandalArt-KMP/pull/320)
- [PR #326: 표시 대상과 동기화 안정화](https://github.com/Nexters/BandalArt-KMP/pull/326)
- [Android 공식 GlanceAppWidget 갱신 문서](https://developer.android.com/develop/ui/compose/glance/glance-app-widget#update-glanceappwidget)
