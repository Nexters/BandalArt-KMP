# Android 종료 다이얼로그 광고 구조 결정

- 문서 유형: `STRATEGY`
- 대상 독자: Android 광고 기능을 수정하거나 검증하는 개발자
- 결정일: 2026-09-08
- 상태: 현재 구조 유지, 후속 수명주기 수정 필요
- 검토 기준: BandalArt `origin/main` `5303c721c0ea221302aedcdc47a37272ea914ee1`, YeoBee PR #522 `6e9daada6fa49ef0c54748af48de25e618e6b146`

이 문서는 Android 종료 다이얼로그 네이티브 광고에 YeoBee PR #522의 구조를 적용할지 결정한다. BandalArt는 백버튼 시점의 광고를 고정하는 단발 로드 구조를 유지하고, YeoBee의 다중 지면 갱신 상태 머신은 도입하지 않는다.

## 목표와 범위

이번 검토는 광고 노출 안정성과 구현 비용을 함께 평가한다. Android 종료 다이얼로그만 다루며 iOS 동작은 변경하지 않는다.

검토 대상은 다음과 같다:

- [BandalArt 이슈 #380](https://github.com/Nexters/BandalArt-KMP/issues/380): 종료 다이얼로그 네이티브 광고 도입
- [BandalArt 이슈 #384](https://github.com/Nexters/BandalArt-KMP/issues/384): 광고 지연 삽입과 다이얼로그 크기 변경 방지
- [BandalArt 이슈 #387](https://github.com/Nexters/BandalArt-KMP/issues/387): `MediaView` 등록 순서 수정
- [YeoBee PR #522](https://github.com/YeoBee-official/YeoBee-Android/pull/522): 지면별 preload, 갱신, 재시도, 수명주기 관리

## 결정

BandalArt는 현재의 `ExitDialogAdSession`과 `NativeAdPreloader`를 유지한다. 다이얼로그가 열리는 순간 캐시된 광고를 세션에 복사하고, 열린 세션에는 나중에 로드된 광고를 삽입하지 않는다.

다음 구조는 도입하지 않는다:

- 다이얼로그에 `NativeAdState.ad`를 직접 전달하는 live-state 방식
- 노출 후 주기적으로 광고를 교체하는 `PausableAdTimer`
- 지수 백오프 실패 재시도와 cooldown 상태 머신
- Remote Config 기반 지면별 갱신 주기
- 여러 광고 지면의 가시성을 조정하는 CompositionLocal

YeoBee PR #522는 홈과 종료 다이얼로그 두 지면을 함께 관리한다. 검토한 버전은 28개 파일에 1,429줄을 추가한다. BandalArt의 단일 종료 지면에는 같은 범위의 추상화가 필요하지 않다.

## 지연 렌더링 판단

YeoBee는 `MainScreen`에서 `exitDialogAdState.ad`를 열린 다이얼로그에 직접 전달한다. 광고가 다이얼로그보다 늦게 로드되면 콘텐츠가 `null`에서 광고로 바뀌며 다이얼로그 높이가 변할 수 있다.

`NativeAdViewContainer`의 `key(nativeAd)`는 광고 교체 시 Android View를 새로 만든다. 이 처리는 광고가 늦게 추가될 때 발생하는 레이아웃 변경을 막지 않는다.

BandalArt의 snapshot 방식은 다음 계약을 만족한다:

- 광고 없이 열린 다이얼로그는 닫힐 때까지 광고 없이 유지한다
- 로드가 끝난 광고는 다음 다이얼로그에서 사용한다
- 실제 광고를 표시한 세션만 다음 광고 준비 대상으로 처리한다
- 로딩과 실패 상태에는 광고 공간을 남기지 않는다

고정 placeholder를 항상 확보하면 live-state도 크기 변경을 막을 수 있다. 그러나 광고가 없을 때 빈 공간을 남기지 않는 이슈 #380의 조건과 충돌하므로 채택하지 않는다.

## 광고 등록과 View 정리 판단

BandalArt는 `NativeAdView`와 `MediaView`를 Compose 상태로 관찰한다. 두 View가 모두 만들어진 뒤 `registerNativeAd`를 호출하므로 이슈 #387의 회색 미디어 회귀를 방지한다.

YeoBee의 `key(nativeAd)`는 한 컨테이너 안에서 광고 객체가 교체될 때 유효하다. BandalArt는 열린 세션의 광고 객체를 고정하고 다이얼로그를 닫을 때 컨테이너를 제거하므로 현재는 필요하지 않다.

YeoBee의 `AndroidView.onRelease`에서 자식 `ComposeView`를 명시적으로 해제하는 코드는 적용 가능한 소규모 보강이다. 현재 문제의 원인은 아니므로 후속 변경이 있을 때 함께 검토한다.

## 갱신 주기 판단

YeoBee의 검토 버전은 홈 14s와 종료 다이얼로그 10s를 기본 갱신 주기로 사용한다. Google은 광고를 60s 이상 유지하고, 짧은 화면 재진입에서도 60s보다 빠르게 새 요청을 보내지 않도록 권장한다. 잦은 갱신은 fill rate를 낮출 수 있다.

근거는 [Google AdMob 구현 가이드](https://support.google.com/admob/answer/2936217)다. 네이티브 광고 캐시는 1시간 뒤 교체하고 사용이 끝난 광고는 `destroy()`해야 한다는 기준은 [Android 네이티브 광고 로드 가이드](https://developers.google.com/admob/android/next-gen/native)의 수명주기와 일치한다.

BandalArt도 광고를 표시한 뒤 취소하면 `recycle()`에서 다음 광고를 즉시 요청한다. 빠른 반복 진입에서는 요청 간격이 60s보다 짧아질 수 있다. 자동 갱신 타이머를 추가하지 않고 최소 요청 간격만 보장할지 후속 이슈에서 결정한다.

## 수명주기 리뷰 결과

현재 구현은 중복 요청, 만료 광고, 늦은 callback, 교체와 화면 파기 경로를 처리한다. `generation`이 오래된 callback을 무효화하며, 저장하지 않는 광고도 `destroy()`한다.

다음 결함은 별도 수정이 필요하다:

- `androidApp/src/main/kotlin/com/nexters/bandalart/ads/AndroidExitDialogHost.kt:70`에서 `enabled=false`가 되면 `showDialog`만 내리고 `ExitDialogAdSession`을 닫지 않는다
- 위젯 또는 외부 navigation이 Home을 교체하면 이미 노출된 광고가 preloader에 남을 수 있다
- Home 복귀 뒤 같은 광고가 다시 표시될 수 있다

취소와 화면 이탈은 같은 세션 종료 함수를 사용해야 한다. 화면 이탈에서는 현재 광고를 파기하되, Home 밖에서 다음 광고를 즉시 요청하지 않는 `discard` 경로가 필요하다.

## 최종 비교

| 항목 | BandalArt 현재 구조 | YeoBee PR #522 | 결정 |
| --- | --- | --- | --- |
| 홈 진입 후 preload | 지원 | 지원 | 유지 |
| 열린 다이얼로그의 광고 | 백버튼 시점 snapshot | live state | snapshot 유지 |
| 지연 로드 시 레이아웃 | 다음 세션까지 보류 | 열린 세션에 삽입 가능 | BandalArt 방식 유지 |
| 광고 갱신 | 노출 뒤 단발 교체 | 노출 뒤 주기 갱신 | 주기 갱신 제외 |
| 실패 재시도 | 다음 필요 시 재요청 | 지수 백오프와 cooldown | 현재 방식 유지 |
| 갱신 설정 | 없음 | Remote Config | 도입하지 않음 |
| 화면 파기 | `NativeAd.destroy()` | `NativeAd.destroy()` | 유지 |
| View 교체와 해제 | 세션 제거에 의존 | `key`와 `onRelease` | `onRelease`만 후속 검토 |

## 검증 기준

단위 테스트는 상태 전이를 검증한다. 광고 creative와 `MediaView`의 실제 렌더링은 Android 실기기에서 확인한다.

1. 콜드 스타트 직후 백버튼을 눌러 광고가 없어도 다이얼로그 크기가 바뀌지 않는지 확인한다.
2. 광고 로드 뒤 다이얼로그를 다시 열어 이미지 또는 동영상 미디어가 표시되는지 확인한다.
3. 광고가 없는 세션을 닫아도 로드된 광고가 폐기되지 않는지 확인한다.
4. 광고가 있는 세션을 닫고 다시 열어 다음 광고 준비 동작을 확인한다.
5. 다이얼로그가 열린 상태에서 위젯 진입으로 Home을 이탈한 뒤 같은 광고가 재사용되지 않는지 확인한다.

현재 단위 테스트는 snapshot, 표시 여부에 따른 recycle, 중복 preload, 만료, 늦은 callback 해제와 `MediaView` 등록 준비 조건을 다룬다. 이번 읽기 전용 리뷰에서는 빌드와 테스트를 실행하지 않았다. 실기기 검증 결과를 이 문서 또는 후속 이슈에 기록한다.

## 후속 작업과 미결정 사항

후속 작업은 현재 문제를 닫은 뒤 각각 독립적으로 진행한다:

1. `enabled=false` 경로에서 광고 세션을 종료하고 off-screen 재요청을 막는다.
2. 광고 요청 사이에 60s 최소 간격을 둘지 광고 요청 로그와 반복 진입 동작으로 결정한다.
3. 네이티브 광고 컨테이너를 다시 수정할 때 `AndroidView.onRelease` 명시적 정리를 검토한다.
