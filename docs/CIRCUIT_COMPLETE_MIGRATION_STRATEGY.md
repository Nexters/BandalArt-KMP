# Circuit Complete 전환 전략

## 1. 목적

이 문서는 이슈 #182의 6-A 단계인 Complete 전환 범위와 검증 기준을 구현 전에 고정한다.

- 기준 브랜치: `main` (`a9fe7e36019d8c7690b2844b619c5cf84726681c`)
- 작업 브랜치: `refactor/circuit-complete`
- 대상: Android/iOS 공통 Complete UI, 상태, 완료 기록, 이미지 저장·공유
- 목표: Complete의 ViewModel + Compose Navigation + Koin 경로를 Circuit Presenter + Metro factory로 교체
- 비목표: Home Presenter 전환, Home UI 변경, NavStack 도입, Koin 완전 제거, 이미지 생성 방식 변경

## 2. 관찰된 현재 동작

### `main`

- Home이 완료된 반다라트의 ID, 제목, 프로필 이모지, 차트 이미지 URI를 Compose Navigation route로 전달한다.
- `CompleteViewModel`이 route를 읽어 화면 상태를 만들고, 진입 시 완료 ID를 DataStore에 기록한다.
- 저장과 공유 이벤트는 `CompleteRoute`가 Koin으로 받은 KMP `ImageHandlerProvider`에 위임한다.
- Complete UI와 Android/iOS 이미지 구현은 이미 KMP로 동작하므로 그대로 보존해야 한다.
- Splash·Onboarding은 root Circuit BackStack에 있지만 Home·Complete는 `LegacyHomeScreen` 내부 Compose Navigation에 남아 있다.

### `develop` 참고 구현

- Complete가 route 인자를 가진 Circuit `Screen`과 `Presenter`를 사용한다.
- Presenter가 화면 인자를 상태로 노출하고, 완료 ID 기록과 뒤로가기·저장·공유 이벤트를 처리한다.
- 저장·공유는 UI side effect로 실행한 뒤 즉시 상태에서 제거한다.
- Hilt, Android `Uri`, Android resource와 Android Context 확장은 이식 대상이 아니다.

## 3. 결정

### 3.1 Screen과 Presenter 계약

- `CompleteScreen`은 ID, 제목, 프로필 이모지, KMP 이미지 URI 문자열을 보유한 `ParcelableScreen`으로 만든다.
- Presenter는 Screen 데이터를 그대로 State에 노출한다.
- Presenter 최초 composition에서 `BandalartRepository.upsertBandalartId(id, true)`를 한 번 호출한다.
- 뒤로가기는 root Circuit `Navigator.pop()`으로 처리한다.
- 저장·공유 이벤트는 State의 일회성 side effect로 노출하고 UI가 처리한 뒤 clear 이벤트를 보낸다.
- 프로필 이모지는 기존 Home callback의 non-null 문자열을 그대로 전달한다.

### 3.2 과도기 내비게이션

Complete를 내부 Compose Navigation destination으로 남기면 Circuit Screen을 이식해도 root BackStack이 화면을 소유하지 못한다. 이번 단계에서 경계를 아래처럼 바꾼다.

```text
BandalartApp
  └─ Circuit BackStack
       ├─ SplashScreen
       ├─ OnboardingScreen
       ├─ LegacyHomeScreen
       │    └─ Compose Navigation
       │         └─ Home
       └─ CompleteScreen
```

- `LegacyHomeScreen`에 최소 State/Event 계약과 Presenter를 추가한다.
- Home의 `navigateToComplete` 콜백은 LegacyHome Presenter에 이벤트를 보내고, Presenter가 `navigator.goTo(CompleteScreen(...))`를 호출한다.
- `BandalartNavHost`에서는 Complete destination을 제거하고 Home만 유지한다.
- Complete 뒤로가기는 Circuit BackStack을 pop해 기존 `LegacyHomeScreen`으로 돌아간다.
- Home 자체의 ViewModel, Koin module, Compose Navigation start destination은 다음 6-B/6-C 단계까지 유지한다.

### 3.3 저장·공유 플랫폼 effect

- `ImageHandlerProvider`의 KMP expect/actual 구현과 이미지 URI 형식은 변경하지 않는다.
- Metro Circuit function injection을 사용해 UI factory에 `ImageHandlerProvider`를 주입한다.
- 저장 side effect는 `saveUriToGallery()` 호출 후 기존 성공 토스트를 표시한다.
- 공유 side effect는 `shareImage()`를 호출한다.
- UI가 side effect를 처리하면 `ClearSideEffect` 이벤트를 보내 재구성 시 중복 실행을 막는다.
- Android Context, iOS UIKit 타입은 공통 Screen/Presenter 상태에 노출하지 않는다.

## 4. 구현 순서

1. Complete feature에 Circuit/Metro/Parcelable 설정을 추가한다.
2. KMP `CompleteScreen` 계약과 `CompletePresenter`를 작성한다.
3. 기존 Complete UI를 State/Event에 연결하고 KMP image effect를 추가한다.
4. `LegacyHomeScreen` Presenter bridge를 추가해 Home → Complete 이동을 root Circuit에 연결한다.
5. 기존 Complete ViewModel, UI state/action/event, Koin module, Compose Navigation destination을 제거한다.
6. Presenter, navigation bridge, Metro graph 테스트를 작성한다.
7. Android/iOS compile과 정적 검사를 통과시킨 뒤 이식 맵과 트러블슈팅 문서를 갱신한다.

## 5. 테스트 및 완료 기준

### 자동 검증

- Complete Presenter
  - Screen 인자가 State에 그대로 노출됨
  - 최초 진입 시 완료 ID가 한 번 기록됨
  - 저장·공유 이벤트가 해당 side effect를 만들고 clear 이벤트로 제거됨
  - 뒤로가기 이벤트가 현재 Complete 화면을 pop함
- LegacyHome Presenter
  - Home 완료 이벤트가 동일 인자의 `CompleteScreen`을 push함
- Metro graph
  - Complete UI/Presenter와 LegacyHome UI/Presenter factory를 모두 생성함
- 기존 repository, DB, DataStore 테스트와 전체 Android host test가 통과함
- Android compile, Android Lint, iOS Simulator Arm64 framework link가 통과함

### 수동 검증

- Home에서 완료 처리 후 Complete 화면으로 이동
- Complete 뒤로가기 후 기존 Home 상태로 복귀
- Android/iOS에서 이미지 저장과 성공 토스트 확인
- Android/iOS 공유 시트와 이미지 확인
- 앱 재생성 후 Circuit BackStack의 Complete route 인자 복원

## 6. 롤백 경계

- repository, DB, DataStore, 이미지 provider 소유권과 저장 형식은 변경하지 않는다.
- Home의 기존 ViewModel/Koin/Compose Navigation 경로는 유지하므로 회귀 시 LegacyHome Presenter bridge만 되돌릴 수 있다.
- Circuit factory aggregation 문제가 생기면 Screen/Presenter 계약을 바꾸지 않고 composeApp의 명시적 factory binding으로 대체한다.
- 이미지 저장·공유 문제가 생기면 기존 KMP `ImageHandlerProvider` 호출 위치만 비교하며 플랫폼 구현을 새로 작성하지 않는다.

## 7. 구현 검증 결과

- `:feature:complete:testAndroidHostTest` 통과
- `:composeApp:testAndroidHostTest` 통과
- Complete Presenter의 상태, 완료 기록, save/share side effect, 뒤로가기 테스트 통과
- LegacyHome Presenter의 Home → Complete navigation bridge 테스트 통과
- Metro graph에서 Complete와 LegacyHome의 UI/Presenter factory 생성 확인
- `feature:complete`, `core:navigation`, `composeApp` Spotless/Detekt 통과
- Android common/host test compile에서 Metro Circuit codegen 확인
- iOS Simulator Arm64 framework link와 실제 기기 동작은 아직 검증하지 않음

## 8. 참고 자료

- [Circuit code generation](https://slackhq.github.io/circuit/docs/code-gen/)
- [Circuit navigation](https://slackhq.github.io/circuit/docs/navigation/)
- [Circuit testing](https://slackhq.github.io/circuit/docs/testing/)
- [Metro Circuit integration 1.1.1](https://zacsweers.github.io/metro/1.1.1/circuit/)
