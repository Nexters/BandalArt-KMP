# KMP 통합 검증 및 main 일원화 전략

## 1. 목적

Circuit 화면과 Metro 의존성 그래프가 Android/iOS composition root까지 연결된 현재 `main`을 양 플랫폼의 단일 기준선으로 확정한다.

이 단계는 새로운 기능을 추가하는 작업이 아니다. 자동 검증 범위를 완성하고 CI 병목을 줄인 뒤, 기존 로컬 데이터와 실제 앱 흐름을 양 플랫폼에서 확인하고 내부 테스트 배포를 거쳐 `develop`의 역할을 종료할 수 있는 근거를 만드는 작업이다.

## 2. 현재 기준선

- PR #199까지 `main`에 반영됐다.
- Android와 iOS는 플랫폼별 Metro `AppGraph`를 한 번 생성해 공통 `BandalartApp`에 명시적으로 전달한다.
- 직접 및 전이 Koin runtime 의존성은 Android debug runtime graph에서 제거됐다.
- Circuit 0.35.1, Metro 1.1.1, compileSdk 37, targetSdk 36을 유지한다.
- 현재 CI는 unit test, Android Lint, Android/iOS build를 단일 job에서 순차 실행한다.
- PR #199의 최신 CI는 약 19분 55초가 걸렸다.
- Spotless/Detekt는 KMP 모듈에서 사용할 수 있지만 적용 범위와 기존 baseline 정비가 끝나지 않아 CI 필수 gate에 포함되지 않았다.
- `feature:splash`에는 이미 `bandalart.lint`가 적용돼 있었다. 실제 누락은 `androidApp`과 `baselineprofile`이며, `composeApp`의 Spotless task는 형제 모듈인 `androidApp` 소스를 검사하지 않는다.

## 3. 이번 브랜치의 범위

### 3.1 자동 통합 검증

- 전체 Android/iOS unit test와 대상 Circuit Presenter test를 실행한다.
- Android debug APK와 signed release AAB를 생성한다.
- iOS Simulator Arm64 framework를 link한다.
- Android Lint를 실행한다.
- 최종 Android runtime dependency graph에 Koin artifact가 없는지 확인한다.
- Metro graph test로 동일 DB, DataStore, repository가 앱 graph 안에서 중복 생성되지 않는지 확인한다.

### 3.2 정적 분석 범위 정비

- `androidApp`, `feature:splash`, `baselineprofile`을 포함한 모듈별 적용 정책을 먼저 확정한다.
- 앱과 프로덕션 KMP 소스에는 Spotless/Detekt를 적용한다.
- 생성 코드와 benchmark/profile 전용 소스처럼 도구 적용 이득이 낮거나 오탐이 큰 범위는 이유를 문서화하고 명시적으로 제외한다.
- 기존 위반은 일괄 포맷 변경으로 섞지 않는다. 변경 파일 수정, 의도적인 baseline, 별도 부채 작업 중 하나로 분류한다.
- Detekt convention이 KMP compilation별 source를 덮어쓰지 않도록 task 범위를 검토한다.
- 정비가 끝난 뒤 `spotlessCheck`와 `detekt`를 CI의 빠른 선행 gate로 추가한다.

### 3.3 CI 병목 해소

- 현재 workflow의 단계별 시간을 기록해 기준선을 남긴다.
- 동일 runner에서 여러 Gradle invocation이 반복하는 configuration/compile 비용을 확인한다.
- 빠른 정적 분석, unit test, Android 검증, iOS 검증을 독립 job으로 나눠 병렬 실행하는 구성을 우선 검토한다.
- 동일 브랜치의 새 push가 이전 run을 취소하는 현재 concurrency 동작은 유지한다.
- 문서만 변경된 PR의 경량 검증은 required check가 영구 pending되지 않는 구조에서만 적용한다.
- 브랜치 보호가 참조하는 최종 required check 이름은 안정적으로 유지한다.
- 검증 항목을 삭제하기보다 중복 실행과 순차 대기를 줄인다.

## 4. 후속 수동 gate

자동 검증 PR이 merge된 뒤 다음을 실제 설치 앱에서 확인한다.

### Android

- 기존 스토어 또는 내부 테스트 버전 위에 업그레이드 설치한다.
- 기존 Room 데이터와 DataStore 설정이 유지되는지 확인한다.
- cold start, process 재생성, background 복귀와 재시작을 확인한다.
- 온보딩, Home 조회/편집, 완료 이미지 생성·공유 흐름을 확인한다.
- 선택 업데이트와 강제 업데이트 정책이 기존 동작을 유지하는지 확인한다.
- Internal track 배포와 기존 설치 앱의 업데이트를 확인한다.

### iOS

- 기존 앱 데이터가 있는 설치 상태에서 새 framework/app을 실행한다.
- cold start, background 복귀, Home 조회/편집, 완료 이미지 생성·공유 흐름을 확인한다.
- Android 전용 업데이트 effect가 iOS graph와 UI 계약을 오염시키지 않는지 확인한다.

수동 결과는 체크리스트와 기기/OS 정보를 PR 또는 후속 이슈에 기록한다.

## 5. main 일원화 기준

다음 조건을 모두 충족한 뒤 `main`을 Android/iOS 공통 개발 기준선으로 선언한다.

- 자동 통합 검증과 CI gate가 통과한다.
- 양 플랫폼의 기존 로컬 데이터 upgrade/restart가 통과한다.
- Android 내부 테스트 배포와 업데이트 설치가 통과한다.
- `develop`에만 남은 미병합 커밋, workflow, secret 참조가 없는지 감사한다.
- 기본 브랜치, PR base, 배포 workflow와 작업 문서가 `main`을 가리킨다.
- `develop` 역할 종료와 보호 규칙 변경은 감사 결과를 제시한 뒤 수행한다.

`develop`은 즉시 삭제하지 않는다. 먼저 freeze/read-only 기준을 적용하고, 필요한 이력이 없음을 확인한 뒤 보관 또는 삭제를 결정한다.

## 6. 제외 범위

- 설정 화면과 다크 모드 같은 신규 기능
- BackStack에서 NavStack으로의 전환
- Circuit, Metro, Kotlin, AGP의 추가 버전 업그레이드
- DB schema 또는 DataStore key 변경
- 앱 버전 및 versionCode 변경
- Play production 배포와 App Store 재출시

## 7. 실행 순서

1. 현재 CI workflow와 정적 분석 convention의 실제 task 범위를 확인한다.
2. 모듈별 Spotless/Detekt 적용·제외 정책을 확정한다.
3. 기존 baseline을 최소 변경으로 정리하고 로컬 정적 분석을 통과시킨다.
4. CI를 빠른 선행 gate와 Android/iOS 병렬 검증으로 재구성한다.
5. 전체 자동 검증을 로컬과 PR CI에서 통과시킨다.
6. 양 플랫폼 수동 회귀와 기존 데이터 upgrade/restart를 수행한다.
7. Android internal track에 배포해 기존 설치 앱 업데이트를 확인한다.
8. `develop` 차이를 감사하고 `main` 일원화 변경을 별도 승인 가능한 단위로 적용한다.

## 8. 성공 기준

- 자동 검증 항목을 줄이지 않고 PR CI wall time이 기존 약 20분보다 유의미하게 감소한다.
- 정적 분석 대상과 제외 대상이 문서와 Gradle 설정에서 일치한다.
- `androidApp`이 Spotless/Detekt 대상인지 여부가 암묵적 누락이 아니라 명시적 정책으로 결정된다.
- Android/iOS build, test, static check가 모두 통과한다.
- 기존 Room schema와 DataStore key가 바뀌지 않는다.
- 양 플랫폼 실제 앱 흐름과 기존 데이터가 유지된다.
- 내부 배포 및 branch 운영 변경 전후에 되돌릴 기준점이 남는다.

## 9. 롤백 원칙

- CI 재구성은 기존 검증 명령을 기준으로 결과 동등성을 먼저 확인한다. 누락이 발견되면 병렬 workflow만 되돌리고 검증 명령은 유지한다.
- 정적 분석 적용이 대규모 무관 포맷 diff를 요구하면 baseline 또는 별도 부채 작업으로 분리한다.
- 수동 회귀에서 데이터 또는 기능 문제가 발견되면 internal 배포를 중단하고 `main` 일원화와 `develop` 역할 변경을 진행하지 않는다.
- 브랜치 보호와 기본 브랜치 변경은 코드 merge와 분리해 복구 가능한 순서로 수행한다.

## 10. 구현 결과

- `androidApp`에 `bandalart.lint`를 적용해 application 진입점도 Spotless/Detekt 대상에 포함했다.
- `baselineprofile`은 생성·계측 전용 Android test 모듈이므로 필수 정적 분석에서 명시적으로 제외했다.
- Spotless는 Kotlin과 Gradle Kotlin DSL에 ktlint 1.5.0을 적용한다. 기존 파일의 저작권 연도를 갱신하지 못하고 non-convergent 결과를 만들던 license header 및 XML step은 제거했다.
- CI는 `origin/main` ratchet을 전달해 현재 PR에서 변경된 파일만 Spotless gate로 검사한다.
- Detekt 일반 task만 모듈 전체 소스를 검사하며, target/compilation별 task의 source와 classpath는 더 이상 덮어쓰지 않는다.
- 기존 Detekt 위반 11건은 단순 포맷, 미사용 import/parameter, 정확한 예외 처리와 test naming suppression으로 정리했다.
- CI는 code quality, unit test, Android lint/build, iOS framework build를 병렬 실행하고 마지막 `ci-build`가 결과를 집계한다.
- Markdown-only PR은 무거운 KMP build workflow를 실행하지 않는다.
- PR #199 기준 약 19분 55초였던 단일 순차 job과 새 병렬 CI의 wall time은 이번 PR 결과로 비교한다.
