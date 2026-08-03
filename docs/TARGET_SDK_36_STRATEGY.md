# Target SDK 36 대응 전략

- 작성일: 2026-08-03
- 작업 브랜치: `chore/target-sdk-36`
- 기준 브랜치: `develop`
- 대상 앱: Android (`com.nexters.bandalart.android`)

## 1. 목표

Google Play의 Android 16(API 36) 타깃 요구사항을 만족하는 업데이트를 준비한다.

- `compileSdk`와 `targetSdk`를 36으로 올린다.
- API 36을 공식 지원하는 최소 빌드 도구 조합으로 갱신한다.
- 앱 버전을 `2.2.2 (20202)` 후보로 올린다.
- Android 16에서 바뀌는 edge-to-edge, predictive back, 대화면 동작을 검증한다.
- Target SDK 대응이 끝난 최종 코드에서 인앱 업데이트를 Play 경유로 검증한다.

## 2. 범위에서 제외하는 작업

이번 작업에서 필요하지 않은 마이그레이션과 배포 자동화는 섞지 않는다.

- AGP 9 및 built-in Kotlin 마이그레이션
- Kotlin, Hilt, Compose, Circuit 등 전체 의존성 일괄 업데이트
- KMP/CMP 구조 변경
- Fastlane 복구 또는 Play/Firebase 배포 workflow 작성
- 관측되지 않은 UI 문제를 가정한 대규모 adaptive UI 리팩터링

배포 자동화는 Target SDK 36 PR 머지 후 별도 브랜치에서 진행한다.

## 3. 현재 상태

| 항목 | 현재 | 목표 |
|---|---:|---:|
| minSdk | 28 | 28 유지 |
| compileSdk | 35 | 36 |
| targetSdk | 35 | 36 |
| AGP | 8.8.2 | 8.9.1 |
| Gradle | 8.10.2 | 8.11.1 |
| JDK | 17 | 17 유지 |
| Kotlin | 2.1.21 | 우선 유지 |
| versionName | 2.2.1 | 2.2.2 |
| versionCode | 20201 | 20202 후보 |

API 36의 공식 최소 AGP는 8.9.1이며, AGP 8.9의 최소 Gradle은 8.11.1이다. Play 전체 트랙의 최대 versionCode가 20201 이하인지 확인한 뒤 20202를 최종 확정한다.

## 4. 구현 전략

### 4.1 빌드 도구와 SDK

1. Android SDK Platform 36과 최신 36.x Build Tools를 설치한다.
2. Version Catalog에서 `compileSdk`와 `targetSdk`를 36으로 변경한다.
3. AGP를 8.9.1로 변경한다.
4. Gradle Wrapper를 8.11.1로 변경한다.
5. 앱 patch version을 2로 변경해 `2.2.2 (20202)`를 생성한다.
6. 빌드 오류가 실제 발생한 경우에만 관련 플러그인 또는 의존성을 추가 조정한다.

### 4.2 Edge-to-edge와 IME

현재 `MainActivity`는 `ComponentActivity.enableEdgeToEdge()`를 호출하고 있다. API 36에서는 edge-to-edge opt-out이 비활성화되므로 이 호출을 유지하고 opt-out 속성을 추가하지 않는다.

- 루트 `Scaffold`의 `innerPadding` 전달과 하위 inset 적용이 중복되지 않는지 확인한다.
- `innerPadding`을 적용하는 컨테이너에서 inset 소비가 필요한지 검토한다.
- 텍스트 입력을 사용하는 `MainActivity`에 `android:windowSoftInputMode="adjustResize"` 적용을 검토한다.
- `BandalartBottomSheet`와 `BandalartListBottomSheet`의 `statusBarsPadding`, `navigationBarsPadding`, `imePadding` 조합은 실기기에서 중복 여백 또는 가림이 관측될 때만 수정한다.
- `BandalartTheme`의 중복된 system bar 색상·아이콘 수동 제어는 제거하고 `ComponentActivity.enableEdgeToEdge()`에 위임한다.

### 4.3 Predictive back

API 36 타깃 앱에서는 predictive back이 기본 활성화되고 기존 `onBackPressed` 및 back key 이벤트가 전달되지 않는다.

- 현재 코드에는 직접적인 `onBackPressed`, `KEYCODE_BACK`, `BackHandler` 사용이 없다.
- Circuit 내비게이션의 화면 이동, overlay, bottom sheet dismiss를 Android 16에서 확인한다.
- 실제 문제가 확인되지 않는 한 `android:enableOnBackInvokedCallback="false"` opt-out을 추가하지 않는다.

### 4.4 대화면과 방향 전환

현재 `MainActivity`는 portrait로 고정되어 있지만 API 36 타깃 앱은 `sw600dp` 이상 화면에서 방향 및 크기 제한이 무시된다.

- 휴대전화 portrait 고정은 현재 동작을 유지한다.
- Android 16 태블릿 또는 resizable emulator에서 portrait/landscape를 모두 확인한다.
- 임시 호환성 opt-out은 사용하지 않고, 실제 깨짐이 확인된 화면만 최소 수정한다.
- 화면 회전 후 Circuit back stack과 편집 중 상태가 유지되는지 확인한다.

### 4.5 기타 Android 16 변경점

- `MediaStore#getVersion()`의 반환 형식을 사용하는 코드는 없어 별도 변경하지 않는다.
- Health sensor, Bluetooth bond, local network 권한 관련 API를 사용하지 않아 이번 범위에서 제외한다.
- 공유 및 이미지 저장 흐름은 Intent 보안 강화와 MediaStore 회귀 확인 대상으로만 둔다.

## 5. 검증 전략

### 5.1 자동 검증

코드 수정이 끝난 뒤 에이전트는 다음 검증을 수행한다. `clean`은 실행하지 않는다.

```shell
./gradlew help
./gradlew ktlintCheck detekt :feature:home:testDebugUnitTest :app:lintDebug
```

저장소 지침에 따라 전체 Debug 빌드와 Release AAB 빌드는 사용자가 실행한다.

```shell
./gradlew buildDebug --stacktrace
./gradlew :app:bundleRelease
```

### 5.2 Android 16 수동 검증

- 휴대전화 제스처 내비게이션: 상·하단 시스템 바 겹침과 하단 버튼 여백
- 휴대전화 3버튼 내비게이션: 과도하거나 부족한 하단 여백
- 바텀시트: 제목·메모 입력 중 키보드가 입력란과 완료 버튼을 가리지 않는지
- 바텀시트: 상태 표시줄 영역 dim과 dismiss 동작
- Predictive back: 홈 종료, 화면 이동, overlay 및 bottom sheet dismiss
- 태블릿 `sw600dp`: portrait/landscape와 창 크기 변경 시 주요 UI 잘림 여부
- 공유/저장: 이미지 저장과 Android Sharesheet 실행

위 항목은 오래된 PR #158의 구현을 재사용하지 않고, 문제 시나리오만 최신 코드에서 다시 검증한다.

### 5.3 현재 자동 검증 결과

- `./gradlew help`: 성공
- `./gradlew ktlintCheck detekt :feature:home:testDebugUnitTest :app:lintDebug`: 성공
- 생성된 Debug manifest의 `targetSdkVersion=36`, `windowSoftInputMode=adjustResize` 확인
- 생성된 BuildConfig의 `VERSION_NAME=2.2.2`, `VERSION_CODE=20202` 확인
- 기존 Kotlin DSL 및 Gradle deprecated API 경고는 남아 있으나 이번 변경으로 발생한 실패는 없음

## 6. Play 검증 및 출시

1. Target SDK 36 PR을 `develop`에 머지한다.
2. Play 전체 트랙의 최대 versionCode를 조회해 출시 versionCode를 확정한다.
3. Internal App Sharing에 수정 코드가 포함된 versionCode `N`을 업로드하고 설치한다.
4. 같은 코드의 더 높은 versionCode `N+1`을 업로드하되 직접 설치하지 않는다.
5. `N` 앱을 실행해 flexible in-app update를 검증한다.
6. 다운로드 완료 전 재시작 안내 미노출, 완료 후 안내 노출, 재시작 후 업데이트 완료를 확인한다.
7. 검증이 끝나면 실제 출시 AAB를 Internal Testing에 배포한다.

Internal App Sharing 빌드의 versionCode override 방식은 배포 자동화 브랜치에서 별도로 결정한다.

## 7. 완료 기준

- [x] `compileSdk`와 `targetSdk`가 36이다.
- [x] AGP 8.9.1과 Gradle 8.11.1 조합으로 동기화 및 자동 검증이 통과한다.
- [ ] 앱 버전이 최종 승인된 versionName/versionCode로 변경된다.
- [ ] Android 16 휴대전화에서 edge-to-edge, IME, predictive back 검증이 통과한다.
- [ ] Android 16 `sw600dp` 화면에서 핵심 기능을 사용할 수 있다.
- [ ] 공유 및 이미지 저장 회귀 검증이 통과한다.
- [ ] Internal App Sharing에서 인앱 업데이트 실제 흐름이 통과한다.

## 8. 공식 참고 자료

- Android 16 SDK 설정: https://developer.android.com/about/versions/16/setup-sdk
- API 레벨별 최소 AGP: https://developer.android.com/build/releases/about-agp
- API 36 타깃 동작 변경: https://developer.android.com/about/versions/16/behavior-changes-16
- Android 16 전체 앱 동작 변경: https://developer.android.com/about/versions/16/behavior-changes-all
- 인앱 업데이트 테스트: https://developer.android.com/guide/playcore/in-app-updates/test
