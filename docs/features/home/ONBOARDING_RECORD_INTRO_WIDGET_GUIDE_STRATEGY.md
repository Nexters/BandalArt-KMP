# 온보딩 달성 기록 소개와 첫 완료 뒤 위젯 안내 전략

- 작성일: 2026-11-17
- 관련 이슈: [#500 온보딩에 달성 기록 소개 한 장 추가, 위젯은 첫 완료 뒤 안내](https://github.com/Nexters/BandalArt-KMP/issues/500)
- 참고: [완료 기록 히트맵(잔디)](COMPLETION_HEATMAP_STRATEGY.md), [연속 달성과 위젯 진행률](COMPLETION_STREAK_WIDGET_PROGRESS_STRATEGY.md)

## 목표와 범위

- 처음 쓰는 사용자가 목표를 만든 뒤 실천 기록(연속 달성, 이번 주 완료, 16주 잔디)을 어디서 보게 되는지 온보딩 한 장으로 알린다.
- 첫 할 일을 완료하고 홈으로 돌아오면 위젯에서 진행률을 볼 수 있다는 안내를 한 번만 보여 준다.
- 범위 밖: '이번 주에 할 3개'(#499), 새 알림, 설정 진행 강제, 위젯 직접 추가(pin 요청), 권한 요청.

## 1. 온보딩 한 장

- 현재 온보딩은 2장이다. 0장 "만다라트 계획표는 이제 안녕", 1장 "부담 없이 계획을 세워봐요"(목표 만들기 Lottie).
- 새 장을 1장 뒤, 마지막(2장)에 둔다. `pageCount`는 3이 되고 `시작하기` 버튼은 마지막 장으로 옮긴다. 완료 흐름(버튼 → `NavigateToHome` → 완료 플래그 저장 → 홈)은 그대로다.
- 기존 사용자(2026-11-17 결정 변경): 온보딩 전체는 다시 보여 주지 않고, 새로 추가한 이 장만 한 번 보여 준다.
  - DataStore 키 `onboarding_record_intro_shown_v1`. 위젯 안내 키와 같은 패턴이고 백업 스냅샷에는 넣지 않는다(복원 사용자가 한 번 더 보는 것은 허용).
  - Splash: 온보딩 완료 && 새 키 없음 → `OnboardingRecordIntroScreen`(새 장만 있는 화면, 버튼 '확인했어요') → 새 키 저장 → 홈.
  - 신규 사용자: 온보딩의 '시작하기'에서 완료 플래그와 새 키를 함께 저장해 두 번 보지 않는다.
  - 백업 복원 제안(`CloudBackupScreen`)의 홈 fallback 경로는 이번 범위에서 바꾸지 않는다. 그 경로로 들어온 사용자는 다음 실행 때 Splash에서 새 장을 본다.
- 예시 카드
  - 칩 두 개: '3일 연속', '이번 주 4개 완료'.
  - 제목 '16주 동안 쌓인 작은 실천', 16열 × 7행 격자, 범례(적게 □□□□□ 많이), 요약 'N일 실천', 라벨 '미리보기 · 예시 데이터'.
  - 격자 패턴은 고정 배열이다(랜덤 없음). 색은 히트맵 문서의 민트 램프(light/dark)와 `surfaceVariant` 0단계를 따른다. `CompletionHeatmapSection`은 `feature/home`의 internal이라 onboarding에서 작은 정적 격자를 따로 그린다.
  - 배경은 테마 토큰을 써서 다크 모드에서도 읽히게 한다.
- 접근성
  - 카드 전체를 하나의 노드로 묶고 `contentDescription`을 "예시 기록, 실제 기록이 아니에요. 3일 연속, 이번 주 4개 완료, 최근 16주 중 N일 실천"으로 둔다.
  - 잔디 정보는 색 외에 'N일 실천' 텍스트와 범례 텍스트로도 전달한다.
- 작은 화면에서 넘치지 않도록 새 장의 본문은 세로 스크롤을 허용하고 버튼은 아래에 고정한다.

## 2. 첫 완료 뒤 위젯 안내

### 노출 조건

- 트리거: 할 일 완료가 false → true로 바뀐 순간. 경로는 둘이다.
  - 홈에서 할 일 셀을 눌러 완료(`toggleTaskCompletion`).
  - 할 일 편집 시트에서 완료로 바꾸고 저장(`saveCell`, `initialCellData.isCompleted == false`). 시트가 닫히며 홈으로 돌아온다.
- 트리거는 `pendingWidgetGuide`(rememberRetained)를 켜고, 아래 조건이 모두 참일 때 다이얼로그를 띄운다.
  - 지원 기기다.
  - 아직 안내를 본 적 없다(`widgetGuideShown == false`, DataStore).
  - `bottomSheet == null`, `dialog == null`, `imageRequest == null`, `!isBandalartCompleted`, `rewardedAdRequestId == null`.
- 마지막 할 일로 반다라트가 완료되면 완료 축하(`ImageRequest.Complete` → `CompleteScreen`)가 먼저다. pending은 유지되고, 축하 흐름이 끝나 홈 조건을 다시 만족하면 그때 안내가 뜬다.
- 노출하는 순간 `widgetGuideShown = true`를 저장한다. 버튼을 누르지 않고 앱을 종료해도 다시 열면 반복하지 않는다.
- 업데이트 사용자: 완료 이력이 있어도 아직 안내를 본 적 없으면 다음 완료 뒤 한 번 보여 준다. #463 이전 완료 이력은 정확히 알 수 없고, 위젯 존재를 모르는 기존 사용자에게도 한 번의 안내는 유효하다고 본다.

### 지원 기기

`core/domain`에 `HomeWidgetSupport`를 두고 플랫폼이 구현한다. 반환값 `HomeWidgetPlatform`(`ANDROID`, `IOS`, `null` = 미지원)은 안내 문구 선택에도 쓴다.

| 플랫폼 | 지원 조건 | 근거 |
|---|---|---|
| Android | `PackageManager.FEATURE_APP_WIDGETS` | 위젯을 지원하지 않는 런처·기기 제외 |
| iOS | iOS 17 이상, Mac에서 실행 중이 아님, visionOS가 아님 | 앱 최소 16.6, 위젯 extension 최소 17.0. Mac·visionOS 실행 대상에는 홈 화면 위젯이 없다 |

`HomePresenter` 생성자 기본값은 미지원(`UnsupportedHomeWidgetSupport`)이다. 테스트와 미바인딩 환경에서 안내가 뜨지 않는다.

### 다이얼로그

`HomeScreen.DialogState`에 두 상태를 추가하고 `HomeDialogs.kt`에서 기존 `BandalartActionAlertDialog`를 수정 없이 쓴다. `Home.kt`는 `HomeDialogs(state.dialog)`만 호출하므로 수정하지 않는다.

1. `WidgetGuide`: 제목 '위젯에서 내 진행률 보기', 본문 '앱을 열지 않아도 홈 화면에서 목표의 변화를 확인해요.', 확인 '위젯 추가 방법 보기', 취소 '나중에'.
2. `WidgetGuideSteps(platform)`: 제목 '위젯 추가 방법', content 슬롯에 플랫폼별 번호 목록, 확인 '확인', 취소 '닫기'. 두 버튼 모두 닫는다(`BandalartActionAlertDialog`는 버튼이 항상 둘이다).

- Android: 홈 화면 빈 곳 길게 누르기 → '위젯' → 반다라트 위젯을 길게 눌러 배치.
- iOS: 홈 화면 빈 곳 길게 누르기 → 왼쪽 위 '편집' 또는 '+' → '위젯 추가' → 반다라트 검색 후 추가. iOS 17과 18 이상의 버튼 차이를 함께 적는다.

## 테스트

- DataStore: `widgetGuideShown` 기본값 false, 저장 후 재생성해도 유지.
- SettingsRepository: 노출 플래그 전달과 저장 위임.
- HomePresenter
  - 셀 탭으로 첫 완료 시 `WidgetGuide`가 뜨고 플래그가 저장된다.
  - 편집 시트에서 완료 저장 시 시트가 닫힌 뒤 뜬다.
  - 미지원 기기, 이미 본 경우, 완료 해제(true → false)에서는 뜨지 않는다.
  - 반다라트 완료 축하(`ImageRequest.Complete`) 중에는 뜨지 않는다.
  - '위젯 추가 방법 보기'는 `WidgetGuideSteps(platform)`로, '나중에'와 닫기는 `null`로 바꾼다.
- Onboarding: 예시 격자 요약 수치가 고정 패턴에서 계산된 값과 맞는지 확인한다.

## 수동 확인

- iOS 시뮬레이터(iPhone 18 Pro, iOS 27.2): 앱 삭제 후 설치 → 온보딩 3장 라이트·다크 → 반다라트 작성 후 할 일 완료 → 위젯 안내와 방법 안내 라이트·다크 → 앱 재실행 후 반복 없음.
- Android: 빌드 확인. 실기기 UI 확인은 PR에 확인 여부를 구분해 적는다.
