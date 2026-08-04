# Circuit BackStack과 NavStack 가이드

> 기준 버전: Circuit `0.35.1`
>
> 작성 목적: Circuit `0.29.0` 이후 추가된 NavStack을 이해하고 BandalArt의 전환 여부를 판단한다.

## 결론

NavStack은 Circuit `0.33.0`에서 브라우저처럼 뒤로 이동한 뒤 다시 앞으로 이동할 수 있도록 추가된 새 내비게이션 모델이다. 단순히 BackStack을 대체하기 위한 이름 변경은 아니다. 활성 화면을 가리키는 위치와 그 앞뒤 기록을 함께 보관하는 것이 핵심이다. [Circuit 0.33.0 변경 내역](https://slackhq.github.io/circuit/changelog/#0330), [공식 NavStack 마이그레이션 가이드](https://slackhq.github.io/circuit/docs/navigation-navstack-migration/)

BandalArt는 현재 `goTo()`, `pop()`, `resetRoot()`만 사용하며 사용자가 앞으로 다시 이동해야 하는 흐름이 없다. 따라서 **이번 Circuit 버전 업데이트에서는 BackStack을 유지하는 편이 낫다.** NavStack으로 바꿔도 현재 동작은 거의 같고, 실제 이점은 `backward()`와 `forward()`를 제품 기능으로 제공할 때 생긴다.

## 언제, 왜 추가됐나

Circuit `0.33.0`(2026-02-10)은 다음을 새 내비게이션 아키텍처로 도입했다. [Circuit changelog의 0.33.0 항목](https://slackhq.github.io/circuit/changelog/#0330)

- `NavStack`: push/pop과 양방향 기록 이동을 표현하는 인터페이스
- `SaveableNavStack`: 구성 변경과 프로세스 복원까지 지원하는 기본 구현
- `NavStackList`: 특정 시점의 전체 내비게이션 상태를 나타내는 불변 스냅샷
- `Navigator.forward()`, `Navigator.backward()`, `Navigator.peekNavStack()`
- 새 `circuit-runtime-navigation` 아티팩트

도입 목적은 사용자가 뒤로 이동해도 방문 기록을 제거하지 않고, 이후 앞쪽 기록으로 돌아갈 수 있는 브라우저식 탐색을 지원하는 것이다. 기존 BackStack은 화면을 `pop()`하는 순간 해당 기록이 제거되기 때문에 이 동작을 표현할 수 없었다. [NavStack 공식 소스](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-runtime-navigation/src/commonMain/kotlin/com/slack/circuit/runtime/navigation/NavStack.kt)

## 데이터 모델 차이

예를 들어 `Home → Detail → Edit` 순서로 이동했다고 가정한다.

### BackStack

```text
top/current
    ↓
[ Edit, Detail, Home ]
```

BackStack에서는 현재 화면이 항상 가장 최신 항목인 `topRecord`다. `pop()`하면 Edit 기록이 제거되고 Detail이 현재 화면이 된다. `forward()`와 `backward()`는 기본적으로 `false`를 반환하는 미지원 동작이다. [BackStack 0.35.1 소스](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/backstack/src/commonMain/kotlin/com/slack/circuit/backstack/BackStack.kt), [BackStack API 문서](https://slackhq.github.io/circuit/api/0.x/backstack/com.slack.circuit.backstack/-back-stack/index.html)

### NavStack

Edit에서 `backward()`를 한 번 호출하면 기록은 유지되고 활성 위치만 Detail로 이동한다.

```text
forwardItems   active   backwardItems
    [ Edit ] [ Detail ]    [ Home ]
       ↑         ↑             ↑
      top      current        root
```

NavStack에서는 다음 세 값이 서로 다를 수 있다.

- `topRecord`: 가장 최근에 추가된 기록
- `currentRecord`: 현재 표시 중인 기록
- `rootRecord`: 최초 기록

`NavStackList`도 같은 상태를 `top`, `active`, `root`, `forwardItems`, `backwardItems`로 노출한다. 따라서 현재 화면을 판단할 때 `top`이나 전체 크기가 아니라 `active`를 사용해야 한다. [NavStackList 0.35.1 소스](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-runtime-navigation/src/commonMain/kotlin/com/slack/circuit/runtime/navigation/NavStackList.kt), [마이그레이션 가이드의 데이터 모델 설명](https://slackhq.github.io/circuit/docs/navigation-navstack-migration/#backstack-vs-navstack)

| 동작 | BackStack | NavStack |
|---|---|---|
| `goTo()` / `push()` | 새 화면 추가 | 새 화면 추가. 앞쪽 기록이 있으면 제거 |
| `pop()` | 현재 기록 제거 | 현재 기록과 앞쪽 기록을 제거 |
| `backward()` | 미지원 | 기록을 제거하지 않고 root 방향으로 활성 위치 이동 |
| `forward()` | 미지원 | 기존 앞쪽 기록으로 활성 위치 이동 |
| 현재 화면 | 항상 top | active이며 top과 다를 수 있음 |
| 불변 상태 조회 | 전통적인 back stack 목록 | 앞/현재/뒤가 구분된 `NavStackList` |

중요한 차이는 `pop()`과 `backward()`다. NavStack에서도 기존 `pop()`은 파괴적인 뒤로가기이고, 기록을 보존하려면 명시적으로 `backward()`를 호출해야 한다. 또한 뒤로 이동한 상태에서 `goTo()` 또는 `push()`를 호출하면 브라우저에서 새 링크를 여는 것처럼 기존 forward history가 잘린다. [SaveableNavStack 구현](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-foundation/src/commonMain/kotlin/com/slack/circuit/foundation/navstack/SaveableNavStack.kt)

## `resetRoot()`와 여러 back stack

`resetRoot()`는 NavStack만의 기능이 아니라 `Navigator`의 기능이다. `StateOptions`로 다음을 선택할 수 있다. [Navigator 0.35.1 소스](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-runtime/src/commonMain/kotlin/com/slack/circuit/runtime/Navigator.kt)

- `save`: 현재 root에 연결된 스택 상태 저장
- `restore`: 새 root에 과거 저장 상태가 있으면 복원
- `clear`: 새 root의 저장 상태 제거
- `StateOptions.SaveAndRestore`: 탭 전환처럼 root별 스택을 유지할 때 사용하는 조합

BackStack과 NavStack 모두 이 API를 사용할 수 있다. 차이는 저장할 수 있는 상태의 양이다. BackStack은 현재 push/pop 스택을 저장하고, NavStack은 활성 위치와 forward history까지 함께 저장한다. 공식 가이드도 NavStack은 위치와 forward history를 저장하고 BackStack의 복원 동작은 그대로라고 설명한다. [NavStack 마이그레이션 FAQ](https://slackhq.github.io/circuit/docs/navigation-navstack-migration/#faq)

Circuit `0.35.1`에서는 두 saveable stack 모두 `CircuitSaver`를 받으며, 복원할 수 없는 기록이 있을 때의 처리도 보강됐다. NavStack의 forward history 일부만 복원할 수 없다면 해당 forward history를 버리고, active 또는 그 뒤의 root 방향 기록을 복원할 수 없다면 초기값으로 돌아간다. [Circuit 0.35.1 변경 내역](https://slackhq.github.io/circuit/changelog/#0351), [Screen 저장 및 복원 문서](https://slackhq.github.io/circuit/docs/screen/)

## 시스템 뒤로가기와 제스처

NavStack으로 변경한다고 시스템 뒤로가기나 iOS/Android 예측 뒤로가기가 자동으로 `backward()`가 되지는 않는다.

Circuit `0.35.1`의 기본 `rememberCircuitNavigator()` 시스템 back handler는 `navigator.pop()`을 호출한다. 즉 기본 뒤로가기는 여전히 현재 화면을 제거한다. 브라우저식 기록 보존을 원한다면 제품의 뒤로가기 정책에 맞춰 `backward()`를 명시적으로 연결해야 한다. [Navigator 구현의 back handler](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-foundation/src/commonMain/kotlin/com/slack/circuit/foundation/NavigatorImpl.kt)

NavStack 도입으로 `NavigableCircuitContent`와 decoration은 전체 `NavStackList`를 볼 수 있게 됐고 forward/backward 전환 종류도 추가됐다. 커스텀 `NavDecoration`, `AnimatedNavDecorator`, `circuitx-navigation` interceptor를 구현한 앱은 관련 시그니처를 변경해야 한다. 기본 decoration만 사용하는 앱은 이 추가 작업이 없다. [공식 decoration 마이그레이션 안내](https://slackhq.github.io/circuit/docs/navigation-navstack-migration/#3-custom-navdecoration-or-animatednavdecorator)

Circuit `0.34.0`에서는 decoration이 올바른 Navigator를 직접 받아 back gesture를 처리하도록 API가 한 번 더 변경됐다. `GestureNavigationEventListener`는 제스처 관찰용이며 실제 이동은 여전히 Navigator가 수행한다. [Circuit 0.34.0 변경 내역](https://slackhq.github.io/circuit/changelog/#0340)

## 중첩 내비게이션과의 관계

NavStack은 **한 내비게이션 기록 안의 앞/현재/뒤 위치를 표현하는 모델**이지, 중첩 내비게이션 계층을 자동으로 만드는 기능은 아니다.

Circuit의 중첩 내비게이션 패턴은 `CircuitContent`의 `onNavEvent`에서 자식의 이벤트를 받고 부모 Presenter가 자신의 Navigator로 전달하는 방식이다. 이 이벤트 전달 구조는 BackStack과 NavStack 선택과 별개다. [Circuit 중첩 내비게이션 문서](https://slackhq.github.io/circuit/navigation/#nested-navigation)

하단 탭처럼 root마다 독립된 기록을 유지하는 기능도 중첩 NavStack을 자동 생성하는 것이 아니라, `resetRoot(..., StateOptions.SaveAndRestore)`로 현재 상태를 root별 저장소에 넣고 복원하는 방식이다. [Navigator의 multiple back stacks 설명](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-runtime/src/commonMain/kotlin/com/slack/circuit/runtime/Navigator.kt)

## 기존 BackStack 호환성과 deprecated 여부

Circuit `0.35.1` 기준으로 `BackStack`과 `rememberSaveableBackStack()`은 deprecated가 아니다. BackStack 자체가 `NavStack`을 상속하도록 변경돼 기존 코드가 새 Navigator와 함께 계속 동작한다. 공식 가이드도 전통적인 push/pop만 필요하면 마이그레이션하지 않아도 된다고 명시한다. [공식 무마이그레이션 선택지](https://slackhq.github.io/circuit/docs/navigation-navstack-migration/#1-no-migration-required)

다만 `Navigator.peekBackStack()`은 호환성을 위해 남아 있지만 새 코드에서는 권장되지 않는다. 전체 상태가 필요하다면 `peekNavStack()`의 `active`, `forwardItems`, `backwardItems`를 사용해야 한다. [Navigator 변경 사항](https://slackhq.github.io/circuit/docs/navigation-navstack-migration/#navigator)

정리하면 BackStack은 제거 예정 API라서 급히 옮겨야 하는 상태가 아니다. 공식적으로 확인되는 방향은 다음과 같다.

- 기존 단방향 앱: BackStack 유지 가능
- 새 양방향 탐색 기능: NavStack 사용
- 라이브러리나 공통 decoration처럼 양쪽을 받아야 하는 코드: 상위 타입인 NavStack 중심으로 작성

## 실제 마이그레이션 절차와 비용

기본 decoration을 사용하는 단일 스택 앱의 최소 변경은 작다.

```kotlin
// Before
val backStack = rememberSaveableBackStack(root = SplashScreen)
val navigator = rememberCircuitNavigator(backStack)
NavigableCircuitContent(navigator = navigator, backStack = backStack)

// After
val navStack = rememberSaveableNavStack(root = SplashScreen)
val navigator = rememberCircuitNavigator(navStack)
NavigableCircuitContent(navigator = navigator, navStack = navStack)
```

필요한 작업은 다음과 같다. [공식 마이그레이션 절차](https://slackhq.github.io/circuit/docs/navigation-navstack-migration/#2-migrate-to-navstack)

1. `rememberSaveableBackStack()`을 `rememberSaveableNavStack()`으로 변경한다.
2. 변수 및 `NavigableCircuitContent` 인자를 `navStack` 기준으로 변경한다.
3. 제품에서 기록 보존형 뒤로/앞으로 이동을 제공할 지점을 정하고 `backward()`와 `forward()`를 연결한다.
4. 현재 화면 판별 코드가 있다면 `top`이 아닌 `active`를 사용하도록 수정한다.
5. 구성 변경 및 프로세스 복원 후 active/forward/backward 기록이 의도대로 복원되는지 검증한다.
6. 커스텀 decoration 또는 CircuitX interceptor가 있다면 `NavStackList`와 통합된 `InterceptedResult` 시그니처로 수정한다.

코드 변경량보다 제품 동작 결정이 더 큰 비용이다. 시스템 back을 기존처럼 `pop()`으로 둘지, `backward()`로 바꿀지, forward UI를 어디에 제공할지, 새 `goTo()`가 forward history를 버리는 것이 맞는지를 먼저 정해야 한다.

## BandalArt 적용 판단

현재 BandalArt의 루트 구성은 다음과 같다.

- `rememberSaveableBackStack(root = SplashScreen)`
- `rememberCircuitNavigator(backStack)`을 `rememberAndroidScreenAwareNavigator`로 감쌈
- `NavigableCircuitContent(navigator, backStack)`
- Splash/Onboarding 완료 시 `resetRoot(HomeScreen)`
- Home에서 Complete로 `goTo(CompleteScreen)`
- Complete에서 `pop()`으로 Home 복귀
- 커스텀 `NavDecoration`, `AnimatedNavDecorator`, CircuitX interceptor 없음

이 흐름에서 Complete를 닫은 뒤 다시 앞으로 이동해야 할 요구사항은 없다. 오히려 완료 화면은 현재 상태를 반영해 다시 생성하는 편이 자연스럽고, 과거 Complete 화면을 forward history로 되살리면 오래된 데이터나 중복 완료 처리를 고려해야 한다.

### 현재 권고: BackStack 유지

근거는 다음과 같다.

- 현재 모든 이동이 전통적인 push/pop/reset 흐름이다.
- `backward()`/`forward()`를 노출할 사용자 경험이 없다.
- Splash와 Onboarding은 완료 후 돌아가면 안 되므로 기존 `resetRoot()`가 정확한 의미다.
- 타입만 NavStack으로 바꿔도 기본 시스템 back은 `pop()`이어서 사용자에게 보이는 이점이 없다.
- BackStack은 0.35.1에서도 지원되고 deprecated가 아니다.

### 다음 조건이 생기면 재검토

- 데스크톱·웹 또는 iOS에서 브라우저식 앞/뒤 탐색을 제공한다.
- 편집 화면을 여러 단계 오간 뒤 forward history를 복원해야 한다.
- `peekNavStack()` 스냅샷을 이용해 탐색 기록 UI를 제공한다.
- 커스텀 decoration에서 active 앞뒤 화면을 함께 렌더링한다.
- 신규 내비게이션 구조를 만들면서 기존 BackStack 호환보다 장기 API 통일이 더 중요해진다.

그 시점에는 별도 기능 PR에서 NavStack 전환과 `backward()`/`forward()` UX를 함께 설계하는 것이 좋다. 단순 타입 교체만 먼저 하는 것은 현재 BandalArt에 실질적 이득이 적다.

## 0.29.0 이후 내비게이션 관련 주요 변화

Circuit을 오랫동안 팔로업하지 않았을 때 우선 확인할 변화만 추리면 다음과 같다.

| 버전 | 주요 내용 |
|---|---|
| `0.30.0` | `AnimatedNavDecoration`이 전환 판단에 전체 back stack을 사용하도록 변경. [변경 내역](https://slackhq.github.io/circuit/changelog/#0300) |
| `0.31.0` | `resetRoot()`가 `StateOptions` 기반으로 변경됐고 기존 boolean 시그니처는 확장 함수로 호환. iOS gesture navigation이 Compose Multiplatform `PredictiveBackHandler` 기반으로 변경. [변경 내역](https://slackhq.github.io/circuit/changelog/#0310) |
| `0.33.0` | NavStack, SaveableNavStack, NavStackList와 `forward()`/`backward()` 도입. BackStack이 NavStack을 상속. [변경 내역](https://slackhq.github.io/circuit/changelog/#0330) |
| `0.34.0` | `NavDecoration` 위치와 시그니처, gesture navigation listener 관련 API 변경. [변경 내역](https://slackhq.github.io/circuit/changelog/#0340) |
| `0.35.1` | `CircuitSaver` 도입, saveable stack의 복원 실패 처리 보강. [변경 내역](https://slackhq.github.io/circuit/changelog/#0351) |

이 중 BandalArt에 즉시 영향을 주는 것은 `resetRoot()` 시그니처 호환 여부와 Circuit 버전 업데이트다. NavStack 전환은 현재로서는 필수 마이그레이션이 아니라 제품 요구에 따라 선택하는 기능 변경이다.
