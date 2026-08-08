# 설정 화면 및 테마 지원 전략

## 1. 관련 이슈와 목표

- GitHub Issue: #183 `feat: 설정 화면 및 다크 모드 지원`
- UX 참고: [Dari Settings UI](https://github.com/easyhooon/dari/blob/801b012042ab0347ef2b1ec67c7c99c629d69bf0/dari/src/main/kotlin/com/easyhooon/dari/ui/components/SettingsBottomSheet.kt)

Home 상단바에서 설정 bottom sheet를 열고 앱 버전과 `시스템 설정 / 라이트 / 다크` 테마 선택을 제공한다. 선택은 Android/iOS에서 즉시 적용되고 재시작 후에도 유지돼야 한다.

Dari의 정보 구조와 3단 선택 UX만 참고한다. 색상, 타이포그래피, 간격, sheet 형태는 BandalArt 디자인 시스템을 사용하며 Dari 자체를 의존성으로 추가하지 않는다.

## 2. 현재 구조와 해결할 문제

- Home은 Circuit `HomeScreen`과 `HomePresenter`가 화면·modal 상태를 소유한다.
- Home bottom sheet는 `HomeScreen.BottomSheetState` 하나로 상호 배타적으로 관리된다.
- 앱 타이틀 클릭이 `ShowAppVersion` effect를 발생시켜 버전 toast를 띄우는 숨은 동작으로 사용된다.
- `AppVersionProvider`는 Android/iOS actual이 이미 있고 Metro `AppGraph`에서 app scope로 제공된다.
- `BandalartTheme`은 system dark를 기본 지원하지만 dark color scheme에 background만 정의돼 있다.
- Home과 주요 화면에는 `White`, `Gray50`, `Gray900` 등 직접 색상 지정이 남아 있다.
- `MainActivity`와 공통 `BandalartApp`이 각각 `BandalartTheme`을 적용해 Android에서 theme가 이중 중첩된다.
- Android/iOS는 같은 `BandalartDataStoreFactory`와 `bandalart.preferences_pb`를 사용한다.

## 3. 작업 단위

기능을 지나치게 잘게 나누지 않는다. `feat/settings` 단일 브랜치와 #183을 닫는 하나의 PR에서 아래 세 내부 단계를 순서대로 완성한다.

1. 설정 진입점, bottom sheet, 앱 버전
2. ThemeMode 영속화와 앱 루트 연결
3. 주요 화면 semantic color와 system bar 다크 모드 대응

각 단계는 별도 커밋과 테스트 경계로 구분할 수 있지만 미완성 상태로 `main`에 merge하지 않는다.

## 4. 상태와 데이터 설계

### 4.1 ThemeMode

공통 domain에 플랫폼 중립 모델을 둔다.

```kotlin
enum class ThemeMode {
    System,
    Light,
    Dark,
}
```

- DataStore에는 enum ordinal이 아니라 안정적인 문자열을 저장한다.
- 값이 없거나 알 수 없는 문자열이면 `System`을 사용한다.
- 사용자 선택값 `ThemeMode`와 실제 적용값 `isDarkTheme`을 구분한다.
- 실제 적용값은 `ThemeMode`와 현재 `isSystemInDarkTheme()`의 조합으로 계산하는 순수 함수로 둔다.

### 4.2 저장소

- 기존 `bandalart.preferences_pb`에 theme mode key를 추가한다.
- `SettingsRepository`는 `Flow<ThemeMode>` 조회와 `setThemeMode(mode)`만 제공한다.
- datastore 구현은 `core:datastore`, repository interface는 `core:domain`, 구현과 Metro binding은 `core:data`에 둔다.
- DB schema와 기존 DataStore key는 변경하거나 이름을 재사용하지 않는다.
- Metro `AppGraph`가 app-scoped `SettingsRepository`를 제공한다.

별도 settings DataStore 파일은 만들지 않는다. 한 개의 enum preference를 위해 factory와 파일 수명주기를 늘릴 필요가 없다.

## 5. 앱 루트 테마 소유권

`BandalartApp`이 `appGraph.settingsRepository`를 관찰하고 공통 루트의 유일한 `BandalartTheme`에 적용한다.

```text
DataStore → SettingsRepository → BandalartApp
                                ├─ BandalartTheme(isDarkTheme)
                                └─ Android system bar effect
```

- 초기값은 `System`으로 두고 DataStore 첫 emission에서 저장값을 반영한다.
- System mode에서는 OS theme 변경을 Compose가 다시 관찰한다.
- Android `MainActivity`의 바깥 `BandalartTheme` 래퍼는 제거해 공통 루트와 충돌하지 않게 한다.
- iOS도 같은 공통 repository와 theme 계산을 사용한다.
- Android status/navigation bar icon 명암과 투명 배경은 Android actual effect에서 실제 `isDarkTheme`에 맞춘다.
- iOS actual은 별도 system bar 조작 없이 no-op으로 둔다.

## 6. Circuit 및 설정 UI 소유권

설정은 별도 navigation destination이 아니라 Home modal로 제공한다.

### Home State

- `HomeScreen.State`에 현재 `themeMode`, `BottomSheetState.Settings` modal 상태 추가
- `OpenSettings`, `SelectThemeMode`, `DismissBottomSheet` event 추가
- Home Presenter가 `SettingsRepository`의 mode를 관찰한다.
- Home UI factory는 기존 `AppVersionProvider`에서 versionName을 한 번 읽어 settings sheet에 제공한다.
- theme 변경 event는 repository에 저장하고 root collector가 즉시 반영한다.

### 제거할 legacy 동작

- 앱 타이틀의 version toast 클릭 동작 제거
- `ShowAppVersion` Event/Effect 제거
- version toast effect에서 `AppVersionProvider` 의존성 제거
- app version 관련 toast 문자열은 더 이상 사용처가 없을 때만 제거

설정값은 app scope repository가 소유하고 Home은 표시·입력 상태만 소유한다. bottom sheet가 닫혀도 theme 선택은 유지된다.

## 7. UI 설계

### 7.1 HomeTopBar

- 기존 62dp 높이와 타이틀 위치 유지
- 우측에 최소 48dp 터치 영역의 설정 아이콘 버튼 추가
- 기존 목록/추가 액션과 hit area가 겹치지 않게 간격 확보
- `material-icons-extended`를 추가하지 않고 공통 vector resource `ic_settings` 사용
- 접근성 content description 제공

### 7.2 SettingsBottomSheet

- 기존 `ModalBottomSheet`, rounded top, drag handle, padding 패턴 재사용
- 섹션: `화면 설정`, `정보`
- `시스템 설정 / 라이트 / 다크` 3개 옵션을 단일 선택 목록으로 제공
- 행 전체 클릭, 선택 배경과 radio semantics를 함께 제공
- 앱 버전은 `앱 버전` label과 실제 versionName을 좌우 정렬
- 뒤로가기, scrim click, swipe down과 명시적 dismiss event를 동일하게 처리

## 8. 다크 모드 디자인 범위

Material color scheme과 BandalArt semantic color를 보강한다. dark palette는 기존 Gray 단계값을 직접 재사용하지 않고 `DarkBackground`, `DarkSurface`, `DarkOnSurface`, `DarkOutline`처럼 의미가 드러나는 전용 토큰으로 정의한다.

- app background
- header/sheet/dialog surface
- primary/secondary text
- icon tint
- divider/border
- snackbar/loading/error surface

Home, Splash, Onboarding, Complete의 앱 chrome과 공통 dialog/bottom sheet를 점검한다. 사용자별 반다라트 chart 색상, emoji와 공유 이미지 결과는 테마에 따라 바꾸지 않는다.

직접 색상을 전부 기계적으로 치환하지 않는다. 실제 dark mode에서 의미가 바뀌는 surface/text/icon만 semantic token으로 이전한다. 디자인 시스템 전면 개편은 하지 않는다.

## 9. 테스트 전략

### 단위 테스트

- 저장값 없음/알 수 없는 값은 System으로 복원
- System/Light/Dark 저장과 Flow emission
- ThemeMode와 system dark 조합의 실제 dark/light 계산
- SettingsRepository graph singleton

### Circuit Presenter 테스트

- OpenSettings 시 현재 theme 노출
- SelectThemeMode 시 repository 저장
- 저장값 emission 후 settings state 갱신
- dismiss 후 app preference 유지
- 기존 Home 편집·업데이트 event 회귀 없음

### UI 및 수동 검증

- 설정 아이콘 content description과 48dp hit area
- segmented selector의 선택 semantics
- versionName 표시
- Android/iOS Light/Dark/System preview 또는 스크린샷 비교
- 앱 재시작 후 mode 복원
- System mode에서 OS theme 변경 추적
- Android edge-to-edge와 status/navigation bar icon 명암
- 기존 데이터, 반다라트 편집·완료·공유 회귀 확인

## 10. 자동 검증

```bash
./gradlew detekt
./gradlew :core:domain:testAndroidHostTest \
  :core:datastore:testAndroidHostTest \
  :core:data:testAndroidHostTest \
  :feature:home:testAndroidHostTest \
  :androidApp:compileDebugKotlin \
  :composeApp:compileKotlinIosSimulatorArm64
```

PR에서는 병렬 CI의 code quality, unit test, Android Lint/build, iOS framework build와 최종 `ci-build`를 모두 통과해야 한다.

## 11. 완료 조건

- Home 상단바에서 설정 sheet를 열고 정상 dismiss할 수 있다.
- 실제 앱 versionName이 표시된다.
- System/Light/Dark 선택이 즉시 적용되고 Android/iOS 재시작 후 유지된다.
- 주요 화면과 system bar가 두 theme에서 읽기 가능하다.
- 사용자 chart 색상과 공유 이미지 결과가 유지된다.
- Metro graph에서 SettingsRepository/DataStore가 중복 생성되지 않는다.
- 기존 Home 기능과 로컬 데이터에 회귀가 없다.
- #183의 체크리스트가 모두 충족된다.

## 12. 비범위

- 계정, 로그인, 알림, 언어 설정
- 데이터 전체 삭제
- Dari 의존성 추가
- BackStack에서 NavStack으로 전환
- Circuit/Metro/toolchain 버전 변경
- 앱 버전/versionCode 변경과 스토어 배포
- 디자인 시스템 전면 개편
