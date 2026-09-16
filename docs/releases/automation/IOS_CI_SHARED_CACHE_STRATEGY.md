# iOS CI 공유 캐시 전략

## 1. 배경

PR #398은 동일 PR 재실행에서 iOS 빌드 단계를 32분 33초에서 15분 54초로 줄였다. 하지만 `pull_request` 실행이 저장한 GitHub Actions 캐시는 `refs/pull/.../merge` 범위에 묶여 다른 PR이 사용할 수 없다. 이후 PR #404와 #405에서는 `Reuse cached Kotlin frameworks` 단계가 건너뛰어졌고 iOS job이 다시 30~40분대까지 증가했다.

GitHub는 기본 브랜치 캐시를 신뢰 가능한 `push` workflow에서 유지하고, PR workflow는 그 캐시를 복원하는 구성을 권장한다.

- [GitHub Actions dependency caching reference](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching)
- 관련 이슈: #407

## 2. 목표

- `main`의 최신 iOS simulator 산출물을 기본 브랜치 캐시로 유지한다.
- Kotlin/Native framework와 Xcode DerivedData의 무효화 기준을 분리한다.
- Swift·Xcode project만 바뀐 PR에서는 캐시된 KMP framework를 재사용한다.
- Kotlin 또는 Gradle 입력이 바뀌면 기존 Gradle build phase로 안전하게 fallback한다.

## 3. 비범위

- TestFlight device archive 캐시 구조 변경
- untrusted PR cache를 `main` 범위로 복사하거나 승격하는 처리
- 로컬 iOS 전체 빌드
- Xcode Cloud 또는 self-hosted macOS runner 도입

## 4. 캐시 구조

### 4.1 Dependency cache

`~/.konan`과 Swift Package `SourcePackages`를 보관한다. Xcode, Gradle wrapper와 dependency 선언이 바뀌면 새 키를 사용한다.

### 4.2 KMP framework cache

아래 simulator framework의 Gradle 실산출물과 DerivedData product link를 함께 별도 캐시한다. DerivedData의 framework는 `composeApp/build/xcode-frameworks/Release` 또는 `iosWidgetShared/build/xcode-frameworks/Release`를 가리키는 link이므로 link만 저장하면 새 runner에서 재사용할 수 없다.

- `ComposeApp.framework`
- `IosWidgetShared.framework`

키에는 Kotlin/Native 결과에 영향을 주는 공통 source, Gradle build logic, version catalog, Gradle 설정과 Xcode 버전을 포함한다. Swift source와 Xcode project 파일은 키에서 제외한다.

정확한 KMP 키가 일치할 때만 Xcode의 Gradle framework build phase를 건너뛴다. framework directory가 누락되면 runtime guard가 기존 Gradle task를 실행한다.

### 4.3 Xcode DerivedData cache

Xcode Build, ModuleCache와 SDKStatCaches를 보관한다. iOS·KMP 전체 입력을 키로 사용하되, 정확한 키가 없으면 같은 Xcode 버전의 최신 `main` cache를 fallback으로 복원해 증분 빌드에 사용한다.

## 5. Workflow

### main cache seed

별도 `iOS Cache Seed` workflow를 `main`의 iOS 관련 변경과 수동 실행에 연결한다. 신뢰 가능한 기본 브랜치 실행만 cache를 저장하며 simulator를 부팅하거나 앱을 실행하지 않고 generic simulator Release build로 산출물을 만든다.

동시 merge가 발생하면 이전 seed를 취소하고 최신 `main`만 남긴다.

### PR CI

PR workflow는 dependency, Xcode, KMP cache 순서로 복원한다. KMP exact hit이면 framework 재사용 guard와 Xcode build setting을 활성화한다. miss이면 지금과 동일하게 Gradle task를 실행한다.

## 6. 검증 기준

- actionlint가 PR, seed, release workflow를 모두 통과한다.
- 계약 테스트가 `main` push trigger, KMP 전용 키와 exact-hit guard 연결을 확인한다.
- cache miss에서 기존 Gradle build phase가 보존된다.
- PR 필수 CI가 모두 통과한다.
- 머지 후 `main` seed workflow가 성공하고 기본 브랜치 cache를 저장한다.
- 후속 iOS PR에서 `KMP frameworks: exact hit`와 단축된 시간을 기록한다.
