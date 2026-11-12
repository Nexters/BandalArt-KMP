# hig-adaptive 기반 Liquid Glass 적용 전략

- 작성일: 2026-11-12
- 관련 이슈: [#472 compose-hig 버전 업과 Liquid Glass 적용](https://github.com/Nexters/BandalArt-KMP/issues/472), [ienground/compose-hig#133](https://github.com/ienground/compose-hig/issues/133)
- 관련 PR: [#473 버전 업](https://github.com/Nexters/BandalArt-KMP/pull/473), [#474 홈 시범 적용](https://github.com/Nexters/BandalArt-KMP/pull/474)
- 라이브러리: `zone.ien.hig` 27.2.6 (`hig`, `hig-adaptive`)

> 표기: **[소스]** 27.2.6 소스를 읽고 확인, **[실행]** 시뮬레이터·기기에서 확인, **[미확인]** 아직 확인하지 않음.

## 결정

| 항목 | 결정 |
|---|---|
| 구현 방식 | `hig-adaptive`의 `Adaptive*` 컴포넌트로 공통 UI 코드 한 벌을 유지한다. 플랫폼별 값은 `adaptation = { cupertino { } material { } }` 블록에만 둔다 |
| 도입 조건 | 0단계(Android 의존성·크기 확인)를 A안(`minSdk` 29, `compileSdk` 37.1)으로 통과했다 |
| Android SDK | `minSdk` 28 → 29, `compileSdk` 37 → 37.1. Android 9 사용자는 Play Console 기준 표시값 없음("-") |
| Android 화면 | **변화 없음**이 합격 기준이다. 현재 색·모양·패딩을 `material { }` 블록으로 옮기고 적용 전후 스크린샷을 비교한다 |
| iOS 화면 | Liquid Glass 컨트롤로 바꾼다. 유리 효과가 드러나는 콘텐츠 위 컨트롤(상단 바, 떠 있는 버튼)부터 적용한다 |
| 테마 | `BandalartTheme` 안에서 `AdaptiveTheme`으로 `MaterialTheme`과 `CupertinoTheme`을 함께 제공한다. Preview와 테스트도 같은 테마를 받는다. 다크 여부는 `BandalartTheme`과 같은 값(설정의 `ThemeMode`를 반영한 `isDarkTheme`)을 쓴다 |
| adaptive가 없는 컴포넌트 | 공개 API `AdaptiveWidget(material, cupertino)`로 프로젝트 래퍼를 만들거나 Material을 유지한다. 화면별 expect/actual은 새로 만들지 않는다 |
| 중복 탭 방지 | 기존에 `clickableSingle`을 쓰던 곳은 adaptive 컨트롤의 `onClick`에도 같은 중복 탭 방지를 적용한다. `Adaptive*`와 `CupertinoLiquid*`는 일반 `onClick`만 받는다 |
| 버전 | `@ExperimentalAdaptiveApi`라 버전을 고정하고, 올릴 때는 iOS·Android 홈 스크린샷을 다시 비교한다 |

## 배경

- hig는 지금 `feature/home`의 `iosMain`에서만 쓴다. 사용처는 마감 알림 시간 선택(`CupertinoTimePicker`) 한 곳이다.
- #474는 iOS에서만 유리 컨트롤을 그리려고 `HomeGlassOverlay` expect/actual을 만들었다. 화면마다 이렇게 하면 CMP로 UI를 한 벌만 유지하려는 목적과 어긋난다.
- `hig-adaptive`는 호출 코드 하나로 iOS는 Cupertino, 그 외 플랫폼은 Material3 컴포넌트를 그린다.
  - **[소스]** `DefaultTheme`: iOS는 `Theme.Cupertino`, 그 외는 `Theme.Material3`.
  - **[소스]** `AdaptiveButton`은 iOS에서 `CupertinoLiquidButton`, Android에서 Material3 `Button`. `AdaptiveIconButton`은 iOS에서 `CupertinoLiquidIconButton`, Android에서 `IconButton`.
  - **[소스]** `AdaptiveTheme`은 두 테마를 함께 감싼다. `CupertinoTheme` 없이 hig를 쓰면 다크 모드에서도 라이트로 그리는 문제(compose-hig#133)가 구조적으로 사라진다.
- **[실행]** iOS 27.2 시뮬레이터에서 홈 상단 바(설정·추가)와 공유 버튼을 유리 컨트롤로 바꿔 라이트·다크 표시를 확인했다(#474). 단색 배경 위에서는 굴절보다 반투명 알약 모양이 주로 보인다.

## 대안 검토

| 안 | 내용 | 판단 |
|---|---|---|
| 화면별 expect/actual | #474 방식. iOS actual만 hig 사용 | 기각. 화면 구조를 두 벌 유지해야 하고 화면이 늘수록 차이가 커진다 |
| hig를 `commonMain`에서 직접 사용 | Android도 Cupertino 컴포넌트로 그림 | 기각. Android가 iOS 모양이 되어 기존 Material 디자인과 플랫폼 관례가 깨진다 |
| `hig-adaptive` | 공통 코드 + `adaptation` 블록 | 채택(0단계 통과 조건부) |

## 적용 범위

현재 Material3 컴포넌트 사용 위치(`feature`, `core`, `composeApp` import 기준) 기준이다.

| 분류 | 컴포넌트 | 위치 | 방침 |
|---|---|---|---|
| adaptive로 바로 교체 | `Button`, `TextButton` | 루틴 설정·목록 시트, 알림 다이얼로그 버튼, `CloudBackupScreenUi` | `AdaptiveButton`, `AdaptiveTextButton` |
| | `OutlinedButton` | `CloudBackupScreenUi` | **[소스]** adaptive outlined 변형이 없다(`AdaptiveButton`, `AdaptiveTextButton`, `AdaptiveTonalButton`만 있음). `AdaptiveWidget` 래퍼 또는 Material 유지 |
| | `IconButton`, `FilledIconButton` | `HomeTopBar`, 설정·루틴·목록 시트, `BottomSheetTopBar`, `CompleteTopBar`, `BottomSheetButton` | `AdaptiveIconButton` 계열 |
| | `Switch` | 설정·루틴 시트, `BandalartBottomSheet` | `AdaptiveSwitch` |
| | 상단 바 | `HomeTopBar`, `CompleteTopBar`, `BottomSheetTopBar` | 모두 자체 `Row`/`Box` 구현이다. 내부 버튼만 adaptive로 바꾸고, `AdaptiveTopAppBar` 전환은 디자인 검토 후 결정 |
| | `CircularProgressIndicator` | `CloudBackupScreenUi` | `AdaptiveCircularProgressIndicator` |
| 자체 클릭 컨트롤 | `BandalartButton` | 온보딩 "시작하기", 완료 화면 | `AdaptiveWidget(material = { 기존 컴포저블 그대로 }, cupertino = { Liquid 컨트롤 })`. Material `Button`으로 바꾸면 최소 높이·리플·모양이 달라져 Android 변화 없음 기준을 못 지킨다 |
| | `HomeShareButton` (`Box` + `clickableSingle`), `HomeTopBar` 추가 버튼 (`Box` + `clickable`) | 홈 | 같은 래퍼 방식 |
| | 설정 행 (`clickable`) | `SettingsBottomSheet`, `DeadlineReminderTimeRow` | 행 전체 클릭이라 그대로 둔다 |
| adaptive 없음 | `ModalBottomSheet` (5곳) | 설정, 반다라트 편집, 루틴, 목록, 이모지 시트 | 5단계에서 `AdaptiveWidget` 래퍼(`CupertinoBottomSheet`) 검토. 결정 전까지 Material 유지 |
| | `DropdownMenu` | `BandalartDropDownMenu` | `AdaptiveWidget` 래퍼(`CupertinoDropdownMenu`) 검토 |
| | `RadioButton` | 설정 시트 테마 선택 | Material 유지. iOS는 세그먼트 컨트롤(`CupertinoSegmentedControl`)이 관례라 래퍼 여부를 따로 결정 |
| | Snackbar, `Card`, `Surface` | 앱 공통 | Material 유지 |
| 별도 구조 | 알림 다이얼로그 5개 (`Dialog` 기반 자체 디자인) | 삭제·초기화·액션·네트워크·서버 오류 | 6단계에서 결정. `AdaptiveAlertDialog`로 바꾸면 iOS 디자인이 시스템 알림 모양으로 바뀐다 |
| | `TimePicker` | 마감 알림 시간 선택 | 현재 expect/actual 유지 |

## 단계

PR은 단계마다 하나로 나눈다.

### 0. Android 의존성·크기 확인 (도입 조건)

- `hig-adaptive`를 `commonMain`에 넣으면 Android 클래스패스에 hig의 전이 의존성이 들어간다. **[소스]** 27.2.6은 Kotlin stdlib `2.4.21`, CMP runtime·foundation `1.12.2`, material3 `1.12.0-alpha03`, kotlinx-serialization `1.12.0`, kotlinx-datetime `0.8.0`을 요구한다. 프로젝트는 Kotlin `2.4.10`, CMP 플러그인 `1.10.3`, serialization `1.8.1`, datetime `0.7.1`이다.
- #473의 "충돌 없음"은 iOS 컴파일·실행만 확인한 결과다. Android에는 아직 hig가 들어가지 않았다.
- 확인 항목
  - Android debug 빌드, 실행, 홈·시트·다이얼로그 화면 이상 없음
  - R8 release APK 크기 증가량. `hig-android` aar만 R8 전 약 2.1MB다
  - Compose 플러그인과 런타임 버전 차이로 인한 경고나 런타임 오류
- 실패하면 CMP·Kotlin 상향을 먼저 별도 이슈로 진행하고 이 전략은 보류한다.

#### 0단계 결과 (2026-11-12)

`feature/home` `commonMain`에 `hig` + `hig-adaptive` 27.2.6을 넣고 `:androidApp:assembleRelease`를 실행했다.

| 항목 | 결과 |
|---|---|
| 빌드 | **실패.** `checkReleaseAarMetadata`: hig 4개 모듈(`hig`, `hig-core`, `hig-native`, `hig-adaptive`)이 `minCompileSdk=37`, `minCompileMinorSdk=1`을 요구한다. 프로젝트는 `compileSdk = 37`(37.0)이고 로컬 SDK에도 `android-37.0`까지만 설치되어 있다 |
| minSdk | **충돌 예정.** `hig-android` aar 매니페스트가 `minSdkVersion="29"`다. 프로젝트 `minSdk`는 `28`이라 compileSdk 문제를 넘으면 매니페스트 병합에서 막힌다. 올리면 Android 9 사용자를 지원하지 못한다 |
| Android Compose 해석 버전 | runtime·foundation `1.11.4` → `1.12.1`, material3 `1.4.0`(안정) → `1.5.0-alpha22`(alpha) |
| APK 크기 | 빌드 실패로 미측정. 기준값(`main`, R8 release): 24,979,155바이트 |
| CMP·Kotlin | 직접 원인은 아니다. 막힌 지점은 Android SDK 요구 수준이다 |

1차 시도(요구 수준 미충족) 선택지

| 안 | 내용 | 비용·위험 |
|---|---|---|
| A. 요구 수준 맞추기 | `compileSdk` 37.1, `minSdk` 29로 올리고 Android에 hig를 넣는다 | Android 9 지원 중단. material3 alpha가 Android 운영 빌드에 들어간다 |
| B. 컴포넌트 단위 래퍼 | hig는 `iosMain`에만 두고, 버튼·아이콘 버튼·스위치 같은 공통 컴포넌트만 `core/ui`에 expect/actual로 만든다. 화면 코드는 공통 한 벌을 유지한다 | Android 영향 없음. hig-adaptive 대신 래퍼 몇 개를 직접 관리한다 |
| C. 라이브러리 측 조정 요청 | 메인테이너에게 Android 산출물의 `minSdk`·`minCompileSdk` 완화를 요청한다 | 일정이 라이브러리에 달린다 |

**결정: A안.** Play Console 통계(설치한 사용자 수, 일별, 2026-10-30~11-08)에서 Android 9는 10일 모두 표시값이 없었고("-"), Android 10은 하루 1~2명(약 0.4%)이었다. "-"가 0명인지 표시 기준 미달인지는 화면만으로 구분되지 않는다.

2차 시도(A안 적용)

| 항목 | 결과 |
|---|---|
| 설정 | `minSdk` 29, `compileSdk { version = release(37) { minorApiLevel = 1 } }`. `KmpAndroidPlugin`, `AndroidGradleDsl`, `baselineprofile`에 적용하고 카탈로그에 `compileSdkMinor = "1"` 추가 |
| 로컬 SDK | `platforms;android-37.1` 설치 필요. CI 환경도 같은 플랫폼이 있어야 한다 **[미확인: CI]** |
| 빌드 | `:androidApp:assembleRelease` 성공 |
| APK 크기 | 24,982,630바이트(+3,475바이트). Android 코드에서 hig를 아직 호출하지 않아 R8이 대부분 제거한 값이다. 실제 증가량은 2단계 이후 다시 잰다 |
| 실행 | debug 빌드를 Android 17 기기에 설치해 홈 화면 표시와 크래시 없음 확인 **[실행]**. release 빌드 실행은 미확인 |
| 남은 위험 | Android material3가 `1.5.0-alpha22`로 올라간다. 각 단계에서 Android 화면 회귀를 확인한다 |

### 1. `BandalartTheme`에 `AdaptiveTheme` 추가

- `core/designsystem`의 `BandalartTheme`이 `MaterialTheme` 안쪽에서 `AdaptiveTheme`을 감싼다. 앱 루트가 아니라 여기에 두는 이유는 화면 Preview와 테스트도 `BandalartTheme`만 감싸기 때문이다.
- `material` 스펙은 `MaterialThemeSpec.Default()`로 바로 위 `MaterialTheme`의 색·모양·타이포를 그대로 넘긴다.
- `cupertino` 스펙의 `colorScheme`은 `BandalartTheme`의 `darkTheme`(설정 `ThemeMode` 반영)으로 고른다. `isSystemInDarkTheme()`를 직접 쓰지 않는다.
- `fontScale = 1f` density 고정은 `AdaptiveTheme` 바깥 `CompositionLocalProvider`에 그대로 있다.
- **[소스]** `CupertinoTheme`은 `SystemBarAppearance(colorScheme.isDark)`를 호출한다. 앱도 `SystemBarAppearance(isDarkTheme)`를 호출하며 두 값은 같은 플래그에서 나온다.
- **[실행]** Android 17 기기에서 홈 화면이 적용 전과 같고 크래시가 없음을 확인했다(2026-11-12).

### 2. 홈 (#474 재작성)

- `HomeGlassOverlay` expect/actual을 지우고 공통 코드로 다시 구성한다.
- 상단 바 설정 버튼은 `AdaptiveIconButton`, 추가·목록 버튼과 공유 버튼은 `AdaptiveButton`.
- 유리 배경(`backdrop`)은 `cupertino { backdrop = ... }`에만 넣는다. `layerBackdrop` 기록 레이어는 공통 코드에 두고, Android에서는 효과가 없는지 확인한다 **[미확인]**.
- 높이: iOS 컨트롤은 48dp로 맞춘다(#474 결정, 추가 검토 중). **[소스]** `CupertinoLiquidIconButton`은 48dp 고정이고 `cupertino { }`에 크기 항목이 없다. `modifier`는 두 플랫폼 공통이라 플랫폼별 높이는 `adaptation` 블록의 `contentPadding`으로 맞춘다.
- 공유 이미지 캡처 영역(`homeGraphicsLayer`, `completeGraphicsLayer`)에 유리 컨트롤이 들어가지 않는지 확인한다.

### 3. 시트 안 컨트롤

- 설정·루틴·목록·편집 시트의 버튼, 아이콘 버튼, 스위치를 adaptive로 바꾼다.
- 시트 컨테이너는 Material 그대로 둔다.

### 4. 완료·백업 화면

- `CompleteTopBar`, `CloudBackupScreenUi`의 버튼과 진행 표시.

### 5. 시트·드롭다운 래퍼

- `AdaptiveWidget(material = { ModalBottomSheet(...) }, cupertino = { CupertinoBottomSheet(...) })` 형태의 프로젝트 래퍼를 `core/ui`에 만든다.
- 시트 상태(`rememberModalBottomSheetState`)와 닫기 동작이 두 구현에서 같게 동작하는지가 핵심 검증 항목이다. 맞추기 어려우면 Material 유지로 결정한다.

### 6. 다이얼로그

- 자체 디자인 유지와 `AdaptiveAlertDialog` 전환 중 하나를 디자인 검토로 정한다.

### 7. iOS 단독 배포

- 적용이 끝나면 iOS만 TestFlight로 배포한다. Android 배포와 묶지 않는다.
- 절차는 `deploy-ios-testflight` skill을 따른다(별도 작업으로 작성 중).

## 검증

| 단계 | iOS | Android |
|---|---|---|
| 공통 | 라이트·다크, 설정 테마(라이트/다크/시스템) 3가지 | 적용 전후 스크린샷 동일 |
| 0 | — | 빌드·실행, R8 release APK 크기 |
| 2 | 홈 세로·2단·휴대폰 가로, 공유 이미지 저장 | 홈 세로·2단·휴대폰 가로 |
| 3~5 | 시트 열기·닫기, 스와이프 닫기, 회전 | 동일 |

- iOS는 iPhone과 iPad 시뮬레이터, Android는 휴대폰과 폴더블 에뮬레이터로 확인한다.
- 접근성 "투명도 줄이기"에서 유리 컨트롤이 읽히는지 확인한다. **[소스]** hig는 iOS에서 이 설정을 읽어 유리 렌더링을 바꾼다.
