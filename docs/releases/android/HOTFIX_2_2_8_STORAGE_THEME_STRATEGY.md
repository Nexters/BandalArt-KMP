# 2.2.8 저장 호환성 및 다크 테마 회귀 핫픽스 전략

## 1. 배경

GitHub Issue #203은 2.2.6 Play Internal 설치본을 2.2.7로 인앱 업데이트한 실제 Android 기기에서 발견한 출시 차단 회귀를 다룬다.

- 온보딩이 다시 노출되고 기존 반다라트가 보이지 않는다.
- 다크 모드가 system bar와 일부 app chrome에만 적용되고 Home의 바람개비형 차트, bottom sheet와 입력 UI는 밝은색으로 남는다.
- Home top bar의 설정 아이콘과 `+추가`/목록 액션이 높이 기준으로 중앙 정렬되지 않는다.

이번 핫픽스는 새 아키텍처를 추가하지 않는다. KMP 전환 과정에서 바뀐 Android 저장 경로와 semantic color 적용 누락을 복구하고 2.2.8 Internal Testing에서 동일 업데이트 경로를 다시 검증한다.

## 2. 확인된 원인

### Android 저장 경로

2.2.6과 2.2.7은 key와 Room entity schema는 동일하지만 실제 파일 경로가 다르다.

| 저장소 | 2.2.6 | 2.2.7 |
|---|---|---|
| Bandalart DataStore | `files/datastore/bandalart_datastore.preferences_pb` | `files/bandalart.preferences_pb` |
| In-app update DataStore | `files/datastore/in_app_update_datastore.preferences_pb` | `files/in_app_update.preferences_pb` |
| Room | `databases/bandalart_database` | `databases/bandalart.db` |

기존 파일은 삭제되지 않았다. 2.2.7 Android factory가 새 빈 파일을 열어 onboarding status의 기본값 `false`와 빈 DB를 반환한다.

### 다크 테마

`BandalartTheme`에는 dark semantic palette가 있으나 Home 내부 컴포넌트가 `White`, `Gray100`, `Gray300`, `Gray900`을 직접 사용한다. 따라서 테마 전환이 화면 표면, 글자, 아이콘, border와 gradient에 전달되지 않는다.

### Top bar 정렬

설정 액션은 48dp `IconButton`, `+추가`/목록은 높이가 없는 `Box`다. 상위 `Row`에도 `verticalAlignment = Alignment.CenterVertically`가 없어 서로 다른 높이 기준으로 배치된다.

## 3. 목표

- Android에서 2.2.6의 실제 Room/DataStore 경로를 다시 사용한다.
- 2.2.6 → 2.2.8 업데이트 후 onboarding, 최근 반다라트 ID, 목록과 Room 데이터가 그대로 보이게 한다.
- Home의 실제 화면 표면 전체가 `MaterialTheme.colorScheme`을 사용하게 한다.
- 사용자가 선택한 main/sub chart 색상과 emoji는 테마와 무관하게 유지한다.
- 설정, `+추가`/목록 액션을 동일한 최소 48dp hit area와 수직 중앙 기준으로 맞춘다.
- 앱 버전을 2.2.8(20208)로 올려 Internal Testing에서 재검증한다.

## 4. 저장 호환성 구현

### Android

- DataStore factory는 기존 `preferencesDataStore(name = ...)`와 동일한 `files/datastore/` 경로를 직접 사용한다.
- Bandalart: `bandalart_datastore.preferences_pb`
- In-app update: `in_app_update_datastore.preferences_pb`
- Room factory는 legacy database name `bandalart_database`를 사용한다.
- Android 전용 상수와 경로 선택에 회귀 테스트를 추가한다.

새 파일을 옛 파일 위에 복사하거나 DB를 삭제하지 않는다. 2.2.7은 Internal Testing에만 배포됐고 production의 사용자 데이터는 legacy 경로에 있으므로 Android의 source of truth를 legacy 경로로 복구하는 것이 가장 안전하다.

### iOS

iOS의 기존 `bandalart.preferences_pb`, `bandalart.db` 경로는 변경하지 않는다. Android 호환성 복구를 common filename 변경으로 구현하지 않는다.

## 5. 다크 테마 구현 범위

다음 의미를 Material color scheme에 연결한다.

- page background → `background`
- top bar, bottom sheet, dialog, menu, card → `surface`
- 입력/선택/비활성 container → `surfaceVariant`
- 기본 text/icon → `onSurface`
- 보조 text/icon → `onSurfaceVariant`
- divider/border/chart gap → `outline` 또는 `outlineVariant`
- primary action → `primary` / `onPrimary`

대상은 Home 차트와 cell, header, progress, skeleton, list/edit bottom sheet, emoji/color/date picker, dropdown, delete dialog, scroll gradient와 공통 bottom-sheet 구성요소다.

다음은 직접 색상을 유지한다.

- 사용자 선택 main/sub 색상
- completion/error처럼 제품 의미가 있는 색상
- 공유·저장 캡처 레이어의 `Gray50` 캔버스
- emoji/vector 자체 색상과 `Color.Unspecified` tint

캡처 전용 `Gray50` 배경과 실제 화면 배경은 draw 단계에서 분리한다. 저장되는 이미지는 기존 밝은 캔버스를 유지하고, 화면에는 캡처 배경을 다시 그리지 않아 상위 theme background가 보이게 한다.

## 6. Top bar 정렬

- 62dp top bar 높이는 유지한다.
- 상위 `Row`를 top bar 높이에 맞추고 `verticalAlignment = Alignment.CenterVertically`를 적용한다.
- 설정과 `+추가`/목록 모두 최소 48dp hit area를 갖는다.
- 아이콘과 text row를 중앙 정렬하고 기존 좌우 padding과 시각적 간격은 유지한다.

## 7. 회귀 테스트

### 자동

- Android legacy DataStore file name과 `datastore/` 상대 경로
- Android legacy Room database name
- 기존 DataStore key 읽기/쓰기 테스트 유지
- 기존 Room DAO schema/CRUD 테스트 유지
- ThemeMode와 Home Presenter 테스트 유지
- Android/iOS 컴파일과 정적 분석 통과

Compose screenshot test 기반이 현재 없으므로 다크 테마 전체 색상과 top bar 시각 정렬은 코드 수준 semantic color 감사와 Internal Testing 실기기 비교를 완료 조건으로 둔다. 얕은 source-string 테스트는 잘못된 안정감을 주므로 추가하지 않는다.

### Internal Testing

1. 기존 2.2.6 설치본과 데이터가 있는 상태를 준비한다.
2. 2.2.8 선택 업데이트를 수행한다.
3. 온보딩이 생략되고 기존 Home 데이터가 표시되는지 확인한다.
4. 생성·편집·삭제, 목록, 완료, 공유·저장을 확인한다.
5. 시스템/라이트/다크 전환 후 Home 차트, modal과 입력 UI를 확인한다.
6. 앱 재시작 후 데이터와 테마 설정을 확인한다.

## 8. 검증 명령

```bash
./gradlew :core:datastore:testAndroidHostTest \
  :core:database:testAndroidHostTest \
  :feature:home:testAndroidHostTest \
  :androidApp:compileDebugKotlin \
  :composeApp:compileKotlinIosSimulatorArm64

./gradlew spotlessCheck detekt -PspotlessRatchetFrom=origin/main
```

전체 APK/AAB와 실제 Play 업로드는 PR CI 통과·merge 후 `deploy-android-playstore` 절차에서 실행한다.

## 9. 완료 조건

- Android가 legacy Room/DataStore 파일을 열고 기존 데이터가 유지된다.
- iOS 저장 경로와 사용자 선택 차트/공유 이미지 색상에 영향이 없다.
- Home의 바람개비형 차트와 modal까지 다크 테마가 일관되게 적용된다.
- top bar 액션이 동일 높이 기준으로 중앙 정렬된다.
- 2.2.8(20208) 자동 검증과 Internal Testing 업데이트 검증이 통과한다.
- 결과를 #203과 umbrella #182에 기록한다.

## 10. 비범위

- 새로운 테마 팔레트 또는 디자인 시스템 전면 개편
- Room schema/version 변경
- 2.2.7 새 빈 저장소와 legacy 저장소의 양방향 merge
- iOS 배포
- production 배포
- foreground 강제 업데이트 정책 #186

## 11. 구현 후 자동 검증 결과

- Android legacy DataStore/Room 경로 회귀 테스트: 통과
- 기존 Home Presenter Android host test: 통과
- `core:common`, `core:database`, `core:datastore`, `feature:home` detekt: 통과
- Android 앱 debug Kotlin compile: 통과
- iOS Simulator Arm64 KMP compile: 통과

실제 2.2.6 → 2.2.8 데이터 복구와 다크 테마 시각 확인은 PR merge 후 Internal Testing에서 수행한다.
