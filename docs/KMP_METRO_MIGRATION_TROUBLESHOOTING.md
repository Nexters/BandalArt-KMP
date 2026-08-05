# KMP AGP 9 및 Metro 마이그레이션 Troubleshooting

## 문서 목적

이 문서는 BandalArt의 KMP AGP 9 전환과 Metro 도입 중 실제로 발생한 문제, 원인, 해결 방법을 기록한다. 비슷한 오류가 다시 발생했을 때 전체 마이그레이션 과정을 재조사하지 않고 증상별로 확인하기 위한 운영 문서다.

기준 작업:

- PR #189: KMP AGP 9 구조 전환
- PR #190: Metro 1.1.1 bootstrap
- PR #191: Platform/Room/DataStore graph 이전
- Kotlin 2.3.21, AGP 9.3.0, Gradle 9.5.0
- Gradle runtime JDK 21, 앱 Java/Kotlin target 17

새 문제를 추가할 때는 추측이 아니라 재현된 증상, 확인한 원인과 실제 해결을 함께 기록한다.

## 빠른 증상표

| 증상 | 먼저 확인할 항목 | 적용한 해결 |
| --- | --- | --- |
| KMP 모듈에서 Android application plugin 충돌 | `composeApp`에 KMP와 application plugin이 함께 있는지 | `androidApp` application 모듈 분리 |
| iOS framework native cache/link 실패 | Kotlin과 Navigation/CMP 구성 버전 | Navigation 2.9.2로 정렬 |
| Metro plugin 적용 직후 JVM version 오류 | Gradle daemon/CI setup JDK | Gradle runtime JDK 21 |
| Metro KLIB ABI/version 오류 | Metro와 Kotlin compiler 조합 | Kotlin 2.3.21에서는 Metro 1.1.1 사용 |
| provider 이동 후 Room/DataStore symbol 미해결 | provider가 위치한 모듈의 직접 의존성 | `composeApp`에 필요한 runtime 직접 선언 |
| bootstrap probe 제거 후 컴파일 오류 | 남은 accessor 참조 | root composition key를 `appGraph`로 변경 |
| 동일 generic DataStore binding 충돌 가능성 | raw `DataStore<Preferences>` binding 수 | wrapper를 직접 제공하고 raw type 미노출 |
| code style 전체 검사 실패 | 변경 파일인지 기존 baseline인지 | 신규 위반만 수정하고 기존 정비는 분리 |

## 1. AGP 9에서 KMP와 Android Application plugin을 한 모듈에 둘 수 없음

### 증상

기존 `composeApp`이 KMP 공통 코드와 Android application packaging을 동시에 담당해 AGP 9 구조로 전환할 수 없었다. 기존 convention plugin도 KMP 모듈에 Android library plugin을 적용한 뒤 application plugin 역할을 추가하는 구조였다.

### 원인

AGP 9의 Android-KMP 구조에서는 KMP Android library target과 Android application packaging을 같은 모듈에서 구성하지 않는다. 기존 모듈은 KMP source set, manifest, application ID, signing, build type과 앱 entry point 책임이 섞여 있었다.

### 해결

- `androidApp`: Android application plugin, manifest, application ID, version, signing, R8와 Android entry point
- `composeApp`: Android-KMP library, 공통 UI, Android/iOS actual 구현과 iOS framework

기능 코드를 재작성하지 않고 packaging 경계만 분리했다.

### 재발 방지

새 Android application 전용 설정은 `androidApp`에 둔다. KMP library 모듈에 `com.android.application` 또는 `org.jetbrains.kotlin.android`를 다시 추가하지 않는다.

## 2. AGP 9 public DSL 및 built-in Kotlin 전환

### 증상

기존 build logic에 AGP 9에서 제거되거나 바뀐 Android extension 타입과 DSL이 남아 있었다. 초기 문서의 `androidLibrary {}` 예시와 실제 AGP 9.3 DSL도 일치하지 않았다.

### 원인

AGP 9는 built-in Kotlin과 public DSL을 사용하며, 이전 AGP 내부 타입 또는 Android/Kotlin plugin 조합을 그대로 유지할 수 없다.

### 해결

- Android-KMP 설정을 `kotlin { android { ... } }`에 맞춤
- 제거된 내부 extension 대신 AGP public DSL 사용
- Kotlin Android plugin 중복 적용 제거
- AGP 9.3.0, Gradle 9.5.0, Kotlin 2.3.21, KSP 2.3.10으로 정렬

### 남은 경고

Gradle 10에서 제거될 reporting API 경고가 남아 있다. 현재 AGP 9 빌드를 막지는 않으므로 이번 기능 마이그레이션과 섞지 않고 build logic 정비 대상으로 남긴다.

## 3. Navigation 구버전의 Kotlin/Native link 실패

### 증상

Navigation 2.8.0-alpha12를 유지한 상태에서 Kotlin 2.3.21로 iOS simulator framework를 링크하면 Native cache/link 단계가 실패했다.

### 원인

기존 Navigation 바이너리와 Kotlin 2.3.21 및 Compose Multiplatform 1.10.3 조합이 맞지 않았다.

### 해결

Compose Multiplatform 1.10.3 구성과 맞는 Navigation 2.9.2로 올린 뒤 iOS simulator arm64 framework link가 통과했다.

### 확인 순서

Native link 오류가 생기면 application 코드부터 수정하지 말고 Kotlin, Compose Multiplatform, Navigation 바이너리 조합을 먼저 확인한다.

## 4. Metro와 Kotlin/Native ABI 호환성

### 증상

최신 Metro를 바로 사용하려 했을 때 현재 Kotlin compiler가 읽을 수 없는 iOS runtime KLIB/ABI 문제가 확인됐다.

### 원인

Metro 1.2.0 이상 배포물은 Kotlin 2.4 계열 ABI를 사용하고, 프로젝트는 Kotlin 2.3.21을 사용한다.

### 해결

현재 toolchain에서는 Metro 1.1.1을 고정했다. Android compilation만 성공한 상태로 버전을 확정하지 않고 iOS framework link까지 통과한 조합을 기준으로 삼았다.

### 재발 방지

Metro를 올릴 때는 다음을 한 묶음으로 검증한다.

1. Kotlin compiler 및 Compose Multiplatform 호환성
2. Android Metro codegen
3. iOS KLIB resolve와 framework link
4. Circuit integration codegen

## 5. Metro Gradle plugin은 JDK 21이 필요함

### 증상

PR #190 CI에서 Metro plugin 적용 후 build가 시작되기 전에 JVM version 오류로 실패했다. 로컬 구현은 맞았지만 GitHub Actions가 JDK 17로 Gradle을 실행하고 있었다.

### 원인

앱 bytecode target과 Gradle plugin 실행 JVM을 같은 값으로 취급했다. Metro 1.1.1 Gradle plugin은 JVM 21이 필요하지만 앱의 Java/Kotlin target은 17을 유지할 수 있다.

### 해결

- GitHub Actions setup JDK를 21로 변경
- Gradle runtime JDK는 21 사용
- 앱 Java/Kotlin target과 toolchain은 17 유지
- README와 마이그레이션 문서에서 두 값을 분리해 기록

### 확인 명령

CI 실패 시 로그의 Gradle runtime JVM을 먼저 확인한다. `jvmTarget` 또는 Android device 요구사항을 불필요하게 21로 올리지 않는다.

## 6. Provider 위치 이동 후 직접 의존성 누락

### 증상

Room DB와 DataStore provider를 core Koin module에서 `composeApp`의 Metro binding container로 옮기자 `BundledSQLiteDriver`, Room runtime과 DataStore 관련 symbol을 찾지 못해 compilation이 실패했다.

### 원인

provider 구현 코드가 사용하는 라이브러리는 해당 코드가 위치한 모듈의 compile classpath에 직접 있어야 한다. core module의 `implementation` 의존성은 `composeApp`의 provider 구현에 전이 API로 보장되지 않는다.

### 해결

`composeApp` common source set에 provider 구현이 직접 사용하는 다음 runtime 의존성을 선언했다.

- Room runtime
- Bundled SQLite driver
- DataStore core/preferences

### 재발 방지

4-B에서 repository provider 또는 binding 위치를 옮길 때도 구현 모듈의 직접 의존성을 먼저 확인한다. 단순히 하위 모듈을 dependency로 가진다는 이유로 내부 `implementation` 라이브러리를 사용할 수 있다고 가정하지 않는다.

## 7. Bootstrap probe 제거 후 남은 accessor 참조

### 증상

4-A에서 임시 `MetroBootstrapProbe`를 실제 DB/DataStore graph로 교체한 뒤 `BandalartApp`이 삭제된 `bootstrapProbe` accessor를 계속 참조해 compilation이 실패했다.

### 원인

bootstrap 단계에서 graph identity를 composition key로 쓰기 위해 probe accessor를 사용했고, 임시 타입 제거 시 호출부가 함께 정리되지 않았다.

### 해결

root composition의 key를 임시 probe가 아니라 `appGraph` 자체로 변경했다.

### 재발 방지

임시 spike/probe를 제거할 때는 타입 선언뿐 아니라 accessor, test, composition key와 entry point 참조를 함께 검색한다.

## 8. 동일 generic DataStore binding의 모호성

### 관측된 구조

기존 Koin `dataStoreModule`은 Bandalart와 인앱 업데이트용 `DataStore<Preferences>` 두 개를 qualifier 없이 같은 raw generic type으로 등록하고 wrapper 생성 시 `get()`으로 조회했다.

### 위험

등록 순서나 DI 해석에 따라 wrapper가 의도하지 않은 Preferences 파일을 받을 수 있고, Metro/Koin 공존 중 같은 파일 handle을 중복 생성할 가능성도 있다.

### 해결

- Metro graph에 raw `DataStore<Preferences>` accessor를 노출하지 않음
- `BandalartDataStoreFactory`에서 각 DataStore를 만든 즉시 대응 wrapper에 전달
- graph에는 `BandalartDataStore`, `InAppUpdateDataStore`라는 서로 다른 타입만 등록
- Koin bridge는 Metro가 만든 wrapper 인스턴스만 반환

### 재발 방지

같은 generic type의 저장소가 둘 이상이면 qualifier 없이 raw type을 graph에 공개하지 않는다. 파일별 wrapper 또는 명시적인 qualifier로 구분한다.

## 9. Koin과 Metro 과도기 중 singleton 중복 생성

### 문제 조건

동일한 DB, DataStore 또는 repository를 Koin과 Metro 양쪽에서 `single`/`@SingleIn`으로 각각 생성하면 타입 수준 테스트는 통과해도 파일 handle과 캐시 상태가 분리된다.

### 적용한 해결

허용 방향을 `Koin → Metro AppGraph accessor` 하나로 제한했다.

- Metro: 실제 객체 생성과 app scope 소유
- Koin bridge: Metro accessor 결과를 기존 호출자에게 노출
- 금지: Metro에서 Koin 조회, Koin 객체를 graph factory input으로 재주입, 양쪽의 부분 graph 혼합

Robolectric test에서 AppGraph accessor의 singleton identity와 Koin bridge 반환 identity를 함께 확인한다.

## 10. Robolectric JUnit 5 host test 설정

### 적용한 설정

실제 Android `Application`으로 Metro platform graph를 생성하기 위해 `androidHostTest`에 다음 구성을 사용했다.

- Android unit test bundle
- Robolectric
- Robolectric JUnit 5 extension
- SDK 35 명시
- JUnit Platform launcher interceptor 활성화

Robolectric SDK 35는 host test runtime 선택이며 앱의 compileSdk 37/targetSdk 36을 낮추지 않는다.

### 테스트 종료 처리

graph identity test가 생성한 Room DB는 test 종료 시 close한다. production app graph의 DB 수명은 application 수명이며 Koin stop과 연결하지 않는다.

## 11. Code style 검사와 KMP source set baseline

### 증상

전체 Spotless/Detekt 실행 시 이번 변경과 무관한 기존 Compose/Swift entry naming, trailing comma와 legacy formatting 위반이 함께 보고됐다.

### 원인

KMP source set에 task는 생성되지만 현재 저장소의 기존 코드와 Spotless/Detekt 규칙이 완전히 정렬돼 있지 않다. `bandalart.lint`는 Android Lint가 아니라 Spotless ktlint와 Detekt를 묶은 convention이다.

### 대응

- 새 provider/test 파일에서 발생한 formatting과 test naming 위반만 수정
- 기존 파일 전체 포맷이나 naming 변경은 DI 마이그레이션 PR에 포함하지 않음
- 필수 CI인 unit test, Android Lint, Android/iOS build를 별도로 유지

기존 baseline 정비는 별도 작업으로 수행한다. 코드 스타일 실패를 이유로 관련 없는 파일까지 일괄 수정하지 않는다.

## 12. 실패가 아닌 경고 구분

다음 경고는 현재 검증 산출물을 실패시키지 않았다.

- Gradle 10에서 제거 예정인 reporting API
- 기존 Koin/GitLive Firebase iOS 바이너리 ABI linker 경고
- iOS framework bundle ID 자동 추론 경고
- GitHub Actions의 Node.js 20 action deprecation 안내
- GitHub Actions Gradle cache restore 400

경고를 무시한다는 의미는 아니다. 다만 기능/DI 변경과 원인이 다른 경고는 별도 build/CI 정비로 분리해 회귀 원인을 섞지 않는다.

## 13. 후속 단계 확인 체크리스트

### Repository graph 4-B

- repository의 실제 생성자는 Metro 한쪽에만 존재하는가
- Koin module은 Metro accessor의 동일 인스턴스만 반환하는가
- `core:data` contribution의 iOS multi-module aggregation을 가정하지 않았는가
- provider 위치가 바뀌었다면 직접 compile dependency가 있는가
- 기존 repository/DAO/DataStore 테스트가 그대로 유효한가

### Circuit vertical slice

- Metro 1.1.1 Circuit codegen이 Android와 iOS 모두에서 생성되는가
- Hilt component 인자를 복사하지 않고 repository만 Metro에서 주입하는가
- `Screen`과 `Navigator`가 assisted input으로 처리되는가
- Presenter test가 Android Play API를 iOS common state에 노출하지 않는가

### Composition root 정리

- Koin 제거 전에 남은 bridge consumer가 없는가
- Android Application과 iOS entry point가 각각 하나의 AppGraph만 소유하는가
- DB/DataStore close 또는 재생성 책임이 Koin lifecycle에 남아 있지 않은가

## 참고 문서

- [KMP AGP 9 마이그레이션 전략](KMP_AGP_9_MIGRATION_STRATEGY.md)
- [Metro 부트스트랩 전략](METRO_BOOTSTRAP_STRATEGY.md)
- [Metro Platform/Room/DataStore graph 전략](METRO_PLATFORM_DATA_GRAPH_STRATEGY.md)
- [Circuit + Metro KMP 이식 맵](CIRCUIT_METRO_KMP_MIGRATION_MAP.md)
- [JetBrains kotlinconf-app](https://github.com/JetBrains/kotlinconf-app/tree/0b1616cba68e)
