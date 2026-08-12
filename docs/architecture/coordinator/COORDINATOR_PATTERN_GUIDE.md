# BandalArt Coordinator 패턴 가이드

## 1. Coordinator 패턴이란

Coordinator는 화면이나 SDK callback이 직접 다음 단계를 결정하지 않게 하고, 여러 객체와 비동기 단계 사이의 **진행 순서와 전환 규칙**을 한곳에서 관리하는 패턴이다. iOS에서는 보통 ViewController 생성과 화면 전환을 Coordinator가 맡는 형태로 알려져 있지만, 핵심은 UIKit 클래스의 사용 여부가 아니라 다음 책임의 분리다.

- 어떤 흐름을 시작할 수 있는지 판단한다.
- 현재 단계와 중복 요청을 추적한다.
- 여러 callback을 하나의 최종 결과로 수렴한다.
- 화면 전환이나 외부 진입을 올바른 시점에 전달한다.
- 완료·취소·실패 후 흐름을 정리한다.

Coordinator는 UI를 직접 그리거나 영속 데이터를 소유하지 않는다. Repository의 업무 규칙, Presenter의 화면 상태 계산, DI graph의 객체 생성 책임도 대신하지 않는다.

## 2. 이 프로젝트에서의 적용 방식

BandalArt는 전통적인 `AppCoordinator`/`ChildCoordinator` 계층을 프로젝트 전체에 도입하지 않았다. 대신 조정이 필요한 경계에 작은 Coordinator 또는 coordinator 역할의 객체를 두고, 화면 이동 자체는 Circuit `Navigator`와 back stack에 맡긴다.

| 적용 영역 | 조정 객체 | 맡는 책임 | 맡지 않는 책임 |
| --- | --- | --- | --- |
| 앱 화면 이동 | `BandalartApp` + Circuit `Navigator` | root back stack 생성, 외부 진입 시 목적 화면으로 reset | 화면 UI, 데이터 조회 |
| 화면 내부 이동 | 각 Circuit Presenter의 `Navigator` | 사용자 이벤트에 따른 push/pop/reset | back stack 구현, 화면 렌더링 |
| 보상형 생성 흐름 | `RewardedCreateCoordinator` | 슬롯 확인→확인창→광고→권한 반영→생성 관찰 상태 전이 | 광고 SDK 호출, DB 쓰기 |
| 광고 callback 수렴 | `RewardedAdCallbackCoordinator` | reward/dismiss/failure callback 순서 차이를 단일 결과로 수렴 | 광고 로드·표시, UI |
| 외부 진입 전달 | `BufferedDeadlineNotificationLaunchTarget`, `BufferedBandalartWidgetLaunchTarget` | 앱 준비 전 ID 버퍼링, 소비 완료 acknowledgement | 실제 화면 선택 |
| iOS platform 연결 | `DeadlineNotificationLaunchBridge`, `IosWidgetLaunchBridge` | Swift 이벤트를 KMP launch target이 준비될 때까지 연결 | Circuit navigation |
| iOS 위젯 동기화 | `IosWidgetRuntimeBridge` | 앱 활성화 시 DB invalidation, reminder reconcile, timeline reload 순서 조정 | WidgetKit UI, task mutation |

이 표의 모든 객체가 이름에 `Coordinator`를 쓰는 것은 아니다. 다만 여러 생명주기나 계층 사이에서 흐름을 이어 주는 객체는 coordinator 역할을 한다. 반대로 `AppGraph`는 객체를 생성·제공하는 composition root이므로 Coordinator로 분류하지 않는다.

## 3. 현재 적용 위치

### 3.1 Circuit navigation

- `composeApp/.../BandalartApp.kt`
  - `rememberSaveableBackStack`으로 앱의 root back stack을 소유한다.
  - `rememberBandalartNavigator`를 생성해 `NavigableCircuitContent`에 전달한다.
  - 위젯 외부 진입이 현재 Home이 아닌 화면에서 도착하면 `resetRoot`로 Home 경계를 복구한다.
- `feature/splash`, `feature/onboarding`, `feature/home`, `feature/complete` Presenter
  - 각 화면에서 발생한 이벤트를 Circuit `Navigator` 명령으로 변환한다.
  - Presenter는 목적지를 결정하지만 실제 back stack 구현은 소유하지 않는다.

이 구조는 화면 생성을 직접 수행하는 전통적인 iOS Coordinator와 다르다. Circuit이 화면 factory와 back stack 역할을 제공하고, 앱 root와 Presenter가 navigation 의사결정만 나눠 가진다.

### 3.2 보상형 광고 생성 흐름

`feature/home/.../RewardedCreateCoordinator.kt`는 명시적인 상태 머신이다.

```text
IDLE
  -> CHECKING_SLOTS
  -> CREATING
  -> AWAITING_CONFIRMATION
  -> SHOWING_AD
  -> APPLYING_GRANT
  -> AWAITING_CREATION
  -> IDLE
```

이 Coordinator가 필요한 이유는 광고 결과 callback과 반다라트 목록 Flow의 갱신 순서가 항상 같지 않기 때문이다. request ID, 기대 목록 개수, 마지막 관찰 개수를 함께 추적해 다음을 보장한다.

- 빠른 연속 탭으로 같은 요청을 두 번 시작하지 않는다.
- 이전 광고 요청의 늦은 callback을 무시한다.
- DB 쓰기 완료와 목록 Flow 반영 사이의 시간 차이를 흡수한다.
- 취소·실패 시 다시 시작 가능한 `IDLE`로 돌아간다.

`HomePresenter`는 실제 repository·광고 gateway 호출을 담당하고, Coordinator는 순서와 허용 가능한 상태 전이만 반환한다.

### 3.3 광고 SDK callback 수렴

`androidApp/.../AndroidRewardedAdGateway.kt`의 `RewardedAdCallbackCoordinator`는 SDK callback 순서 차이를 처리한다. 광고 닫힘 callback이 reward 기록보다 먼저 오더라도 짧게 기다린 뒤 최종 결과를 한 번만 완료한다.

- `onRewardRecordingStarted()`는 reward 기록 중임을 표시한다.
- `onRewardEarned()`와 `onDismissed()`의 순서가 바뀌어도 `REWARDED`를 보존한다.
- `onFailed()`는 진행 중 상태를 종료한다.
- `finishOnce()`는 consumer callback의 exactly-once를 보장한다.

이 객체가 없다면 Gateway의 SDK listener마다 동일한 flag와 중복 완료 방지 로직이 흩어진다.

### 3.4 알림·위젯 외부 진입

외부 진입은 platform lifecycle과 Compose/Circuit 준비 시점이 다르므로 두 단계로 조정한다.

```text
Android Intent / iOS URL·notification
  -> platform record 함수 또는 Bridge
  -> Buffered LaunchTarget
  -> BandalartApp이 필요한 root 화면으로 이동
  -> HomePresenter가 ID에 해당하는 반다라트를 선택
  -> acknowledge
```

`Buffered*LaunchTarget`은 단일 pending ID를 `StateFlow`로 제공한다. 아직 AppGraph가 만들어지지 않은 iOS cold launch에서는 `DeadlineNotificationLaunchBridge`와 `IosWidgetLaunchBridge`가 ID를 먼저 보관했다가 `MainViewController`에서 target이 연결되면 전달한다.

이 구분 덕분에 Swift/Android framework 코드는 Circuit 화면을 알 필요가 없고, HomePresenter는 platform Intent나 URL 타입을 알 필요가 없다.

### 3.5 iOS 위젯 runtime 조정

`IosWidgetRuntimeBridge`는 Widget Extension이 App Group DB를 변경한 뒤 본 앱이 활성화되는 경계를 조정한다.

1. Room invalidation tracker에 외부 프로세스 변경 확인을 요청한다.
2. deadline reminder를 최신 task 상태에 맞게 다시 계산한다.
3. WidgetKit timeline reload를 요청한다.

이는 화면 navigation Coordinator는 아니지만, iOS app lifecycle·Room·notification·WidgetKit 네 경계의 실행 순서를 한 객체에 모았다는 점에서 coordinator 역할을 한다.

## 4. 새 Coordinator를 만들 기준

다음 중 둘 이상이 동시에 나타나면 Coordinator 도입을 검토한다.

- 서로 다른 callback이나 Flow의 순서가 결과에 영향을 준다.
- 중복 시작, stale callback, exactly-once 완료를 막아야 한다.
- 한 흐름이 여러 화면·SDK·repository에 걸친다.
- cold/warm lifecycle에 따라 이벤트 도착 시점이 달라진다.
- Presenter나 Gateway에 상태 flag가 계속 늘어난다.

단일 repository 호출 뒤 화면 하나를 이동하는 정도라면 Presenter와 `Navigator`로 충분하다. 이름만 Coordinator인 forwarding wrapper는 추가하지 않는다.

## 5. 구현 규칙

- Coordinator의 입력은 의미가 드러나는 이벤트 메서드로 제한한다.
- 내부 상태는 private으로 두고 호출자에게 다음 action이나 결과만 반환한다.
- DB entity나 platform View 객체를 장기 보관하지 않는다.
- 외부 작업은 Repository/Gateway에 위임하고, Coordinator 테스트에서는 순수 상태 전이를 검증한다.
- request ID 또는 generation을 사용해 이전 비동기 결과가 새 흐름을 끝내지 못하게 한다.
- 완료·취소·실패 모든 경로가 재진입 가능한 상태로 돌아가는지 테스트한다.
- 앱 전체 Coordinator 계층은 실제로 화면 생명주기 소유권이 필요해질 때만 도입한다.

## 6. 테스트 기준

- 정상 순서뿐 아니라 callback 역순과 중복 callback을 검증한다.
- 이미 진행 중일 때 두 번째 시작 요청이 거부되는지 검증한다.
- 오래된 request ID가 현재 요청을 변경하지 않는지 검증한다.
- cold launch에서 먼저 기록된 외부 진입이 graph 연결 후 전달되는지 검증한다.
- 없는 대상도 acknowledge되어 무한 재시도되지 않는지 검증한다.
- navigation 조정은 `FakeNavigator`와 root destination 계산 함수로 검증한다.

현재 대표 테스트는 `RewardedCreateCoordinatorTest`, `RewardedAdCallbackCoordinatorTest`, `BandalartWidgetLaunchTargetTest`, `DeadlineNotificationLaunchTargetTest`, `WidgetLaunchNavigationTest`에 있다.
