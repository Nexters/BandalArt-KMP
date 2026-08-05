# Circuit Home 런타임 전환 전략

## 1. 목적

이 문서는 이슈 #182의 6-D 단계인 Home 플랫폼 effect와 실제 Circuit 런타임 전환 범위, 제거 조건, 검증 기준을 구현 전에 고정한다.

- 기준 브랜치: `main` (`c0c9becae190d6568f3e80a60fa02efd295e6aa4`)
- 작업 브랜치: `refactor/circuit-home-runtime`
- 선행 작업: PR #196 Home 읽기, PR #197 Home 편집/modal Presenter
- 목표: `LegacyHomeScreen → Compose Navigation → HomeViewModel` 경로를 `HomeScreen → HomePresenter → Circuit UI`로 교체한다.
- 대상 플랫폼: Android, iOS
- 비목표: 전역 Koin runtime 제거, NavStack 전환, 신규 설정 화면, UI 디자인 변경, DB schema/DataStore key 변경

## 2. 현재 런타임과 문제

현재 새 `HomePresenter`는 Metro graph에 등록됐지만 실제 화면에는 연결되지 않은 shadow 구현이다.

```text
실제 runtime
  Splash/Onboarding
    → LegacyHomeScreen
    → LegacyHome Presenter/UI
    → Compose NavHost(Route.Home)
    → HomeRoute
    → HomeViewModel

shadow graph
  HomeScreen
    → HomePresenter(read + edit/modal)
    → UI factory 없음
```

두 상태 소유자를 동시에 composition하면 repository Flow 수집, 빈 목록 생성과 mutation이 중복될 수 있다. 따라서 Home UI factory를 만든 뒤 root destination을 한 번에 교체하고, 레거시 경로를 fallback으로 병행하지 않는다.

## 3. 책임 경계

### Presenter가 소유하는 상태

- repository에서 읽은 목록, 선택 상세와 셀 트리
- loading, 새 완료 상태 감지
- bottom sheet/dialog 편집 draft와 dropdown
- 사용자 Event에 따른 repository mutation
- 공유·저장·capture 요청 상태
- 선택 업데이트 후보 versionCode와 거절 기록
- capture 완료 후 `CompleteScreen` 이동
- snackbar/toast/app version 같은 one-shot effect 종류

Presenter는 `ImageBitmap`, `GraphicsLayer`, Android `Context`, Play Core API를 직접 다루지 않는다. 캡처 결과는 UI가 문자열 URI로 반환한다.

### 공통 Circuit UI가 소유하는 동작

- 기존 KMP Home UI와 두 `GraphicsLayer` 구성
- `HomeScreen.State`를 bottom sheet/dialog/top bar/chart UI에 전달
- 공유·저장 요청 시 적절한 layer를 `ImageBitmap`으로 변환
- Metro가 주입한 `ImageHandlerProvider`로 Android/iOS 이미지 처리
- Compose resource 기반 snackbar/toast/app version 문구 표시
- side effect 처리 후 명시적인 consume Event 전송

### 플랫폼 UI가 소유하는 동작

- Android flexible in-app update 확인, 실행, listener 등록/해제와 재개
- update 취소 versionCode를 Presenter에 전달
- 다운로드 완료 snackbar와 `completeUpdate()` 재시도
- iOS에서는 선택 업데이트 effect를 no-op으로 처리

Play Core 타입은 common Screen/Presenter 계약과 iOS compilation에 노출하지 않는다.

## 4. Screen 계약 확장

### State

기존 읽기·편집 State에 다음을 추가한다.

- `ImageRequest.Share`: Home 전체 layer 공유 요청
- `ImageRequest.Save`: chart 저장 요청
- `ImageRequest.Complete`: 완료 chart 캡처와 이동 metadata
- `updateVersionCode`: Android flexible update 실행 후보
- 공통 UI가 처리할 one-shot side effect

`ImageBitmap`을 State/SideEffect에 넣지 않는다. Bitmap은 composable과 `ImageHandlerProvider` 사이에서만 사용하고, Presenter에는 처리 완료 Event만 전달한다.

### Event

- 공유 버튼, dropdown 저장, 앱 버전 표시
- 공유/저장/capture 처리 완료
- Android update 후보 확인과 취소
- side effect 소비

6-C Event 이름과 기존 KMP UI action을 일대일로 변환한다. UI component의 시각적 구조와 사용자 동작은 바꾸지 않는다.

## 5. 이미지 처리와 Complete 이동

### 공유

1. 공유 버튼 Event가 `ImageRequest.Share`를 만든다.
2. UI가 Home 전체 `GraphicsLayer`를 bitmap으로 캡처한다.
3. `ImageHandlerProvider.externalShareForBitmap`을 호출한다.
4. 완료 Event가 request를 소비한다.

### 저장

1. dropdown 저장 Event가 `ImageRequest.Save`를 만든다.
2. 완료 상태가 아니면 chart `GraphicsLayer`를 bitmap으로 캡처해 gallery에 저장한다.
3. 성공 toast를 표시하고 완료 Event로 상태와 dropdown을 정리한다.

### 새 완료 이동

1. Presenter가 완료 snapshot 변화와 제목 존재 여부를 확인하고 capture를 요청한다.
2. UI가 chart layer를 bitmap으로 캡처해 `ImageHandlerProvider.bitmapToFileUri`로 URI를 만든다.
3. UI가 `CaptureFinished(uri)` Event를 보낸다.
4. Presenter가 해당 URI로 `navigator.goTo(CompleteScreen(...))`를 호출한다.
5. 이동 요청 상태를 즉시 정리해 같은 완료 변화에서 중복 이동하지 않는다.

기존 ViewModel의 500ms 고정 지연 두 번과 빈 URI를 읽을 수 있는 순서를 복사하지 않는다. 실제 capture 완료가 navigation의 선행 조건이다.

## 6. Android 선택 업데이트

`origin/develop`의 보강된 `HandleAppUpdate` 동작을 KMP 구조에 맞춰 이식한다.

- lifecycle이 `RESUMED`일 때 flexible update 상태를 확인한다.
- 다운로드 완료 상태도 재진입 시 복구해 재시작 snackbar를 다시 노출한다.
- 현재 버전보다 강제 업데이트 조건에 해당하는 versionCode는 Home flexible flow에서 제외한다.
- 동일하거나 더 낮은 거절 versionCode는 다시 띄우지 않고, 더 높은 버전은 다시 제안한다.
- update UI 취소 시 실제 제안된 versionCode를 repository에 저장한다.
- 다운로드 완료 후 사용자가 재시작을 누르면 `completeUpdate()`를 호출하고, 실패하면 재시도 가능한 문구를 유지한다.
- listener는 composition 종료 시 반드시 해제한다.

Android 구현은 `androidMain`, iOS no-op은 `iosMain`에 둔다. Presenter는 `InAppUpdateRepository`만 주입받는다.

## 7. 실제 root 전환

다음 변경을 하나의 PR에서 수행한다.

1. `HomeScreen` 공통 Circuit UI factory를 추가한다.
2. Splash와 Onboarding의 `resetRoot` 목적지를 `HomeScreen`으로 바꾼다.
3. Metro graph test에서 Home Presenter와 UI factory를 모두 검증한다.
4. `LegacyHomeScreen`, `LegacyHomePresenter`, legacy Circuit UI adapter를 제거한다.
5. Home의 Compose Navigation destination과 `Route.Home` 참조를 제거한다.
6. `HomeViewModel`, `HomeUiState/Action/Event`, `HomeModule`과 대체된 테스트를 제거한다.
7. `featureModule/appModule`이 비면 제거하되, 전역 Koin bridge/start/context는 7단계까지 유지한다.

Complete, Splash, Onboarding의 이미 전환된 Circuit 경로는 유지한다. `BandalartApp`의 root는 계속 `SplashScreen`이며 Home으로 직접 시작하지 않는다.

## 8. 삭제 조건

레거시 파일은 새 runtime이 아래 기준을 만족한 뒤 같은 변경에서 제거한다.

- Home Presenter와 UI factory가 Metro graph에서 생성된다.
- Splash/Onboarding이 `HomeScreen`으로 resetRoot한다.
- 읽기 5개와 편집/modal 7개 Presenter 회귀 테스트가 유지된다.
- HomeViewModel 테스트에만 존재하는 이미지/update/완료 이동 사례가 Presenter 또는 effect 테스트로 이전된다.
- Android/iOS compile에서 레거시 Home 참조가 없다.

전역 Koin 제거는 이 PR의 완료 조건이 아니다. Home 전용 Koin 생성 책임만 없애고 Metro bridge는 남은 runtime 정리 단계까지 유지한다.

## 9. 테스트 계획

### Presenter

- 공유·저장 요청과 완료 후 flag 정리
- 앱 버전/안내 effect 소비
- 완료 감지 후 capture 요청, URI 완료 후 정확한 `CompleteScreen` 이동
- capture 완료 전 이동하지 않음과 중복 이동 방지
- update 후보 중 강제 업데이트 version 제외
- 같은/낮은 거절 version 미노출, 더 높은 version 노출
- update 취소 시 versionCode 저장
- 기존 Home 읽기와 편집/modal 테스트 유지

### 공통 UI/effect

- Metro graph에서 Home UI factory 생성
- state에 따른 layer capture Event 연결
- save/share/capture 후 적절한 완료 Event와 effect consume
- legacy Home factory가 graph에서 제거됨

### Android update

- update 가능/불가와 이미 거절한 version 상태 전이
- 취소 Event
- 다운로드 완료 복구와 `completeUpdate()` 성공/실패
- listener 등록/해제

### 정적 검증

- `LegacyHomeScreen`, `LegacyHomePresenter`, `HomeViewModel`, `HomeRoute`, `Route.Home` 참조 0건
- Home feature에서 Koin/Compose Navigation 의존성 제거
- commonMain과 iosMain에 Play Core 타입 참조 0건

## 10. 검증 기준

- 대상 Presenter/effect/graph Android host test 통과
- 변경 Kotlin 파일 Spotless 포맷과 Home/composeApp Detekt 통과
- PR CI의 전체 unit test, Android Lint, Android assemble, iOS Simulator Arm64 framework link 통과
- Android 수동: 기존 로컬 데이터, 목록/편집/modal, 공유/저장, 완료 이동/뒤로가기, flexible update 취소·다운로드·재시작
- iOS 수동: 기존 로컬 데이터, 목록/편집/modal, 공유/저장, 완료 이동/뒤로가기

빌드와 양 플랫폼 수동 검증은 구현 완료 후 PR gate에서 수행한다.

## 11. 예상 구현 순서

1. Home runtime State/Event와 Presenter 플랫폼 상태 전이를 테스트로 고정한다.
2. 공통 Home Circuit UI와 이미지 effect를 연결한다.
3. Android/iOS update effect를 source set별로 추가한다.
4. Splash/Onboarding root 목적지를 `HomeScreen`으로 교체한다.
5. legacy Home adapter, ViewModel, Compose Navigation, Home Koin module을 제거한다.
6. graph/Presenter/effect/static check를 검증하고 migration map과 troubleshooting을 갱신한다.

## 12. 롤백 경계

- DB schema, DataStore key, repository 계약과 이미지 파일 형식은 바꾸지 않는다.
- PR 단위 revert 시 `LegacyHomeScreen` 경로 전체가 함께 복구된다.
- 새/레거시 Home runtime을 feature flag로 동시에 유지하지 않는다.

## 13. 참고 자료

- PR #196 Home 읽기 Presenter
- PR #197 Home 편집/modal Presenter
- `origin/develop`의 `HomeScreen`, `HomePresenter`, `HandleAppUpdate`, `HandleAppUpdateTest`
- Circuit Presenter testing: <https://slackhq.github.io/circuit/docs/testing/>
- Circuit navigation: <https://slackhq.github.io/circuit/navigation/>
- Play Core in-app updates: <https://developer.android.com/guide/playcore/in-app-updates/kotlin-java>

## 14. 구현 결과

- `HomeScreen.State/Event/Effect`가 읽기, 편집/modal, 이미지 처리와 선택 업데이트 상태를 모두 소유한다.
- 공통 Home UI factory가 Metro로 생성되며 Android/iOS `ImageHandlerProvider`로 공유·저장·완료 캡처를 처리한다.
- 완료 이동은 고정 지연 없이 실제 캡처 URI를 받은 뒤 수행한다.
- Android flexible update는 listener와 lifecycle 재진입을 함께 처리하고, iOS effect는 no-op이다.
- Splash와 Onboarding의 root 목적지가 `HomeScreen`으로 전환됐다.
- Home ViewModel, Compose Navigation destination, Koin feature module과 legacy Home adapter를 제거했다.
- Home 읽기 5개, 편집/modal 7개와 runtime/update 정책 테스트를 포함한 대상 Android host test가 통과했다.
- Home iOS Simulator Arm64 compile과 변경 모듈 Detekt가 통과했다.
- 변경 KTS/XML Spotless check가 통과했다. Home 모듈 전체 Kotlin Spotless check는 이번 diff와 무관한 기존 UI 15개 파일의 포맷 부채가 남아 있어 변경 파일 포맷과 Detekt로 분리 검증했다.
- Android/iOS 수동 회귀와 전체 Lint/assemble/framework link는 PR CI 및 수동 gate에서 확인한다.
