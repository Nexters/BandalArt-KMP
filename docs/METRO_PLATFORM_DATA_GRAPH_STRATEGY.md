# Metro Platform/Room/DataStore graph 전략

## 목적

이 문서는 이슈 #182의 4-A 작업 범위를 고정한다. 기존 KMP Room, DataStore, AppVersion, ImageHandler 구현과 저장 형식은 유지하면서 객체 생성과 app singleton 수명 책임만 Koin에서 Metro로 이전한다.

`develop` 브랜치에는 Metro를 적용하지 않는다. 이 작업은 `main` 기반 통합 브랜치에서만 수행하며, 이후 `develop`의 Circuit Presenter를 직접 연결할 공통 데이터 graph를 준비한다.

## 기준점

- 기준 브랜치: `main`
- 기준 리비전: `0791dcfc6f19d5f916e7dbda30212742bfb85bd6`
- 작업 브랜치: `refactor/metro-platform-data-graph`
- Metro: 1.1.1
- Kotlin: 2.3.21
- Gradle 실행 JDK: 21
- 앱 Java/Kotlin target: 17
- 기존 DI: Koin과 Metro 부트스트랩 graph 공존

## 현재 생성 책임

| 객체 | 현재 생성 위치 | 현재 소유자 |
| --- | --- | --- |
| `BandalartDatabaseFactory` | Android/iOS `platformModule` | Koin |
| `BandalartDataStoreFactory` | Android/iOS `platformModule` | Koin |
| `AppVersionProvider` | Android/iOS `platformModule` | Koin |
| `ImageHandlerProvider` | Android/iOS `platformModule` | Koin |
| `BandalartDatabase` | `databaseModule` | Koin |
| `BandalartDao` | `databaseModule` | Koin |
| `BandalartDataStore` | `dataStoreModule` | Koin |
| `InAppUpdateDataStore` | `dataStoreModule` | Koin |

현재 `dataStoreModule`은 generic type이 같은 `DataStore<Preferences>` 두 개를 qualifier 없이 등록한다. 4-A에서는 raw DataStore를 graph interface에 노출하지 않고 각 wrapper를 별도 provider에서 바로 생성해 이 모호성을 제거한다.

## 목표 생성 책임

```text
Android adapter                           iOS adapter
  Application 기반 factory/provider         Foundation/UIKit 기반 factory/provider
            │                                      │
            └──────── PlatformBindings ────────────┘
                              │ @Includes
                              ▼
                    Metro AppGraph(AppScope)
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
  Room DB + DAO       DataStore wrappers    platform providers
        │                     │                     │
        └─────────────────────┴─────────────────────┘
                              │
                       Koin accessor bridge
                              │
                  기존 Repository/ViewModel/화면
```

Metro가 생성한 객체만 실제 singleton이다. Koin bridge는 `AppGraph` accessor가 반환한 동일 인스턴스를 기존 호출자에게 노출할 뿐 객체를 생성하지 않는다.

## module과 seam

### `PlatformBindings`

`PlatformBindings`는 Android와 iOS라는 두 adapter가 존재하는 실제 platform seam이다. 다음 accessor만 제공한다.

- `BandalartDatabaseFactory`
- `BandalartDataStoreFactory`
- `AppVersionProvider`
- `ImageHandlerProvider`

플랫폼 adapter는 Context, Application, Foundation/UIKit 초기화만 감춘다. Room driver 설정, DB build, DataStore wrapper 조립은 공통 Metro binding module 안에 둔다.

### Metro binding module

공통 `BindingContainer` 또는 `AppGraph` provider가 다음 객체를 `AppScope` singleton으로 제공한다.

- `BandalartDatabase`
- `BandalartDao`
- `BandalartDataStore`
- `InAppUpdateDataStore`

`AppVersionProvider`와 `ImageHandlerProvider`는 `PlatformBindings` accessor를 통해 graph에 포함되며 같은 플랫폼 adapter 인스턴스를 사용한다.

### Koin bridge

기존 `databaseModule`, `dataStoreModule`, 플랫폼 `platformModule`의 생성 provider를 제거한다. 대신 `AppGraph`를 입력받는 공통 bridge module을 만들고 다음 accessor 결과를 Koin singleton으로 노출한다.

- DB와 DAO
- 두 DataStore wrapper
- AppVersion과 ImageHandler provider

허용 방향은 `Koin → AppGraph accessor`뿐이다. Metro graph에서 Koin을 조회하거나 Koin 객체를 Metro factory input으로 전달하지 않는다.

## 수명과 소유권

- Android: `BandalartApplication`이 `AppGraph`를 프로세스 수명 동안 한 번 생성한다.
- iOS: `MainViewController` 생성 시 `AppGraph`를 한 번 생성하고 controller의 root composition이 참조한다.
- Room DB와 DataStore wrapper는 `AppScope`에서 한 번 생성한다.
- Koin을 재시작하더라도 동일 `AppGraph`를 사용하는 동안 DB와 DataStore 인스턴스는 바뀌지 않는다.
- DB close 책임은 앱 수명 종료에 두며 런타임 중 Koin stop과 연결하지 않는다.

## 보존해야 할 데이터 계약

- Room DB 파일명: `bandalart.db`
- Room schema와 migration 설정
- SQLite driver: `BundledSQLiteDriver`
- Bandalart DataStore 파일명: `bandalart.preferences_pb`
- 인앱 업데이트 DataStore 파일명: `in_app_update.preferences_pb`
- 기존 Preferences key와 직렬화 형식
- Android/iOS 각 플랫폼의 파일 경로 계산 방식

4-A에서는 DB entity, DAO query, DataStore key, repository 동작을 수정하지 않는다.

## 구현 순서

1. `PlatformBindings`를 factory/provider accessor interface로 확장한다.
2. Android/iOS adapter가 기존 actual factory/provider를 생성하도록 변경한다.
3. 공통 Metro binding module에서 Room DB, DAO, 두 DataStore wrapper를 제공한다.
4. `AppGraph`에 필요한 accessor를 추가하고 bootstrap probe를 실제 graph 검증으로 대체한다.
5. `initKoin`이 `AppGraph`를 받아 공통 accessor bridge를 설치하도록 변경한다.
6. 기존 Koin database/DataStore/platform 생성 provider를 제거한다.
7. Android/iOS 진입점에서 같은 `AppGraph`를 Koin과 root composable에 전달한다.
8. graph scope와 Koin bridge identity를 테스트한다.

## 테스트 전략

### 자동 테스트

- Android Robolectric app context로 실제 Android `PlatformBindings` 생성
- `AppGraph` accessor를 반복 조회했을 때 동일 DB, DAO, DataStore wrapper 인스턴스인지 확인
- Koin bridge가 `AppGraph`와 동일 인스턴스를 반환하는지 확인
- 기존 DAO 테스트와 DataStore 테스트 전체 통과
- 기존 repository 테스트 전체 통과
- Android Lint, debug APK, signed release AAB 통과
- iOS Simulator framework link 통과

### 정적 확인

- Koin module에 DB/DataStore/factory/provider 생성식이 남아 있지 않음
- Metro graph에서 `getKoin`, `KoinComponent`, 전역 service locator 사용 없음
- 같은 DB/DataStore wrapper를 Koin과 Metro가 중복 생성하지 않음
- DB schema와 DataStore 파일명/key diff 없음

### 수동 회귀 체크

- 기존 설치 데이터로 앱 시작
- 최근 반다라트와 완료 목록 유지
- 온보딩 완료 상태 유지
- 인앱 업데이트 거절 버전 유지
- 앱 재시작 후 같은 데이터 재조회

수동 회귀는 내부 테스트 빌드에서 확인하며, 자동 검증이 통과해도 저장 파일 호환성 확인 전에는 4-A를 완료로 간주하지 않는다.

## 구현 및 검증 결과 (2026-08-04)

### 구현 결과

- Android/iOS `PlatformBindings`가 기존 actual factory와 provider를 생성하고 `AppGraph` factory input으로 전달한다.
- Metro `AppScope`가 Room DB, DAO, 두 DataStore wrapper와 플랫폼 provider의 singleton 수명을 소유한다.
- Koin은 `metroKoinBridgeModule`을 통해 Metro accessor의 동일 인스턴스만 노출한다.
- 기존 Koin platform/database/DataStore 생성 module과 core module의 Koin 의존성을 제거했다.
- DB schema, entity, DAO query, DB/DataStore 파일명과 Preferences key는 변경하지 않았다.

### 자동 검증

다음 통합 검증을 실행해 성공했다.

```shell
./gradlew allTests \
  :androidApp:testDebugUnitTest \
  :androidApp:lintDebug \
  :androidApp:assembleDebug \
  :composeApp:linkDebugFrameworkIosSimulatorArm64 \
  :androidApp:bundleRelease \
  -x :androidApp:uploadCrashlyticsMappingFileRelease \
  --no-daemon --stacktrace
```

- 전체 KMP 테스트와 Metro/Koin identity 테스트 통과
- Android Lint와 debug APK 통과
- iOS Simulator framework link 통과
- release R8와 서명된 AAB 통과
- 847 tasks, `BUILD SUCCESSFUL`

Spotless/Detekt 전체 실행에는 이 작업과 무관한 기존 Compose/Swift entry naming 및 legacy formatting 위반이 남아 있다. 새 파일에서 발견된 formatting과 test naming 위반은 수정했으며, CI 필수 검사인 Android Lint와 전체 빌드는 통과했다.

### 남은 수동 검증

기존 설치 데이터가 있는 내부 테스트 빌드에서 DB 목록, 온보딩 완료 상태, 인앱 업데이트 거절 버전과 재시작 후 재조회를 확인해야 한다. 이 항목이 끝날 때까지 이슈 #182의 4-A 데이터 호환성 체크는 완료 처리하지 않는다.

## 제외 범위

- Repository와 UseCase의 Metro binding 전환: 4-B
- Circuit Screen/Presenter/UI factory 이식: 5단계 이후
- Koin runtime과 composition context 완전 제거: 7단계
- DB schema, DAO query, DataStore key 변경
- `develop` 브랜치의 Hilt → Metro 전환
- 신규 기능과 디자인 변경

## 완료 조건

- Android/iOS `PlatformBindings`가 동일한 공통 `AppGraph` interface를 만족한다.
- Metro가 Room DB, DAO, 두 DataStore wrapper와 platform provider의 유일한 app singleton 소유자다.
- 기존 Koin 호출자는 bridge를 통해 Metro와 동일한 인스턴스를 사용한다.
- 자동 검증과 데이터 계약 정적 확인이 통과한다.
- 다음 4-B에서 Repository constructor를 Metro graph에 연결할 수 있다.

## 롤백

문제가 생기면 Koin accessor bridge를 제거하고 기존 `platformModule`, `databaseModule`, `dataStoreModule` provider를 복원한다. DB schema와 파일 경로는 변경하지 않으므로 저장 데이터 롤백 작업은 필요하지 않아야 한다.

## 공식 참고 자료

- [Metro Dependency Graph 입력과 `@Includes`](https://zacsweers.github.io/metro/1.1.1/dependency-graphs/#inputs)
- [Metro Binding Container](https://zacsweers.github.io/metro/1.1.1/dependency-graphs/#binding-containers)
- [Metro Scope](https://zacsweers.github.io/metro/1.1.1/scopes/)
- [Metro Adoption Strategies](https://zacsweers.github.io/metro/1.1.1/adoption/)
- [JetBrains kotlinconf-app](https://github.com/JetBrains/kotlinconf-app/tree/0b1616cba68e)

KotlinConf의 공통 `AppGraph`, 플랫폼별 root graph와 app lifetime graph 소유 방식은 참고한다. KotlinConf는 단일 shared 모듈의 `@ContributesTo` aggregation을 주로 사용하지만, BandalArt는 멀티모듈 iOS Native 제약을 우선해 4-A의 명시적 `PlatformBindings` + `@Includes` 경계를 유지한다.
