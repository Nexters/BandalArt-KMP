# KMP ktlint / Detekt 지원 범위

작성 기준: 2026-08-05, `refactor/metro-repository-graph`

## 결론

- **ktlint는 KMP 소스의 스타일 검사와 포맷에 사용할 수 있다.** ktlint 자체는 `.kt`/`.kts` 파일을 검사하며 KMP target을 컴파일하거나 타입을 해석하지 않는다. 이 저장소에서는 ktlint Gradle plugin이 아니라 **Spotless 7.0.1이 ktlint 1.5.0을 formatter/linter 엔진으로 호출**한다.
- **Detekt 1.23.8은 KMP Gradle plugin과 함께 target/compilation별 task를 생성한다.** 다만 실제 type resolution은 JVM/Android target에만 연결되고 Kotlin/Native(iOS)는 타입 정보 없이 분석된다.
- 따라서 “KMP에서 지원된다”는 답은 둘 다 `Yes`이지만 범위가 다르다. ktlint/Spotless는 source set과 무관한 파일 기반 스타일 검사이고, Detekt는 KMP task를 제공하되 모든 플랫폼에서 동일한 의미 분석을 제공하지 않는다.
- 현재 저장소는 12개 KMP 모듈에 도구가 적용되어 있으나 `feature:splash`, `androidApp`, `baselineprofile`에는 적용되지 않는다. 또한 Detekt convention이 KMP task별 source 입력을 모듈 전체로 덮어써 source set 분리가 무효화된다. 현재 상태를 그대로 CI 필수 gate로 승격하면 안 된다.

## 공식 지원 근거

### ktlint와 Spotless

ktlint 1.5.0 공식 문서는 인자 없이 실행할 경우 현재 디렉터리 아래의 모든 `.kt`와 `.kts`를 재귀적으로 검사하고, glob으로 검사/제외 범위를 지정한다고 설명한다. `.editorconfig`를 읽으며 가능한 위반은 자동 수정할 수 있다. KMP source set은 모두 Kotlin 파일 디렉터리이므로 syntax/style 검사 대상이 될 수 있다. 반대로 공식 인터페이스에는 KMP compilation classpath나 type resolution 입력이 없다.

- [ktlint 1.5.0 기능](https://pinterest.github.io/ktlint/1.5.0/)
- [ktlint 1.5.0 CLI 파일 선택과 glob](https://pinterest.github.io/ktlint/1.5.0/install/cli/#rule-sets)
- [ktlint Gradle plugin의 지원 Kotlin plugin 목록](https://github.com/JLLeitschuh/ktlint-gradle#supported-kotlin-plugins) — `kotlin-multiplatform`을 명시적으로 지원하지만, 이 저장소는 이 plugin을 적용하지 않는다.

Spotless는 format마다 `target`/`targetExclude`로 파일 집합을 정하고 formatter step을 적용한다. Kotlin block에서 ktlint를 사용할 수 있고, `spotlessCheck`가 각 format의 check task를 묶는다. 기본적으로 `check`에도 연결된다.

- [Spotless Gradle: target과 formatter step](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md#quickstart)
- [Spotless Gradle: Kotlin/ktlint 설정](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md#ktlint)
- [Spotless Gradle: `spotlessCheck`와 `check` 연결](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md#disabling-warnings-and-error-messages)

Spotless의 `$YEAR`는 헤더가 없는 파일에 현재 연도를 넣지만, 이미 유효한 연도가 있는 헤더는 기본 설정에서 갱신하지 않는다. 즉 2026년에 새 파일에 기존 `2025` 헤더를 복사하면 Spotless가 자동으로 `2026`으로 바꾸지 않는다. `ratchetFrom`을 사용해야 변경 파일의 연도 갱신 동작을 활성화할 수 있다.

- [Spotless license header 연도 동작](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md#license-header)

### Detekt

Detekt 1.23.8 Gradle plugin은 기본 `detekt` task와 KMP target/compilation별 task를 생성한다. `detekt`는 type resolution 없이 실행된다. 타입 정보가 없는 실행에서는 `@RequiresFullAnalysis` 규칙이 실행되지 않는다.

- [Detekt 1.23.8 Gradle plugin task](https://detekt.dev/docs/1.23.8/gettingstarted/gradle/#available-plugin-tasks)
- [Detekt 1.23.8 type resolution과 KMP](https://detekt.dev/docs/1.23.8/gettingstarted/type-resolution/#enabling-on-a-kmp-project)
- [Detekt 1.23.8 KMP task 구현](https://github.com/detekt/detekt/blob/v1.23.8/detekt-gradle-plugin/src/main/kotlin/io/gitlab/arturbosch/detekt/internal/DetektMultiplatform.kt)

1.23.8 문서 일부는 metadata/iOS task도 type resolution이 된다고 표현하지만, 같은 버전의 공식 구현은 `platformType`이 JVM 또는 Android JVM인 경우에만 classpath를 설정한다. 이 저장소에서 확인한 task 설명도 `detektAndroidMain`, `detektAndroidHostTest`에만 `with type resolution`을 표시하고 iOS/metadata task에는 표시하지 않았다. 이 문서는 실제 1.23.8 구현과 생성 task를 기준으로 판단한다.

KMP의 `commonMain`, 플랫폼별 source set, 중간 source set은 서로 다른 target/dependency 범위를 가진다. 이 차이 때문에 파일만 파싱하는 검사와 compilation classpath를 사용하는 검사를 구분해야 한다.

- [Kotlin Multiplatform source set 구조](https://kotlinlang.org/docs/multiplatform/multiplatform-discover-project.html#source-sets)

## 이 저장소의 실제 적용 방식

버전은 `gradle/libs.versions.toml` 기준 Spotless `7.0.1`, ktlint `1.5.0`, Detekt `1.23.8`이다. 카탈로그의 `org.jlleitschuh.gradle.ktlint` `12.1.2`와 ktlint library `0.50.0` 선언은 현재 어느 build script에서도 사용하지 않는다.

`bandalart.lint`는 Android Lint가 아니다. `bandalart.spotless`와 `bandalart.detekt`를 묶은 convention plugin이다.

### Spotless/ktlint

`SpotlessPlugin.kt`의 실제 범위는 다음과 같다.

| Format | 대상 | 제외 | 적용 step |
| --- | --- | --- | --- |
| Kotlin | `**/*.kt` | `**/build/**/*.kt` | license header, ktlint 1.5.0 |
| Gradle Kotlin DSL | `**/*.kts` | `**/build/**/*.kts` | license header |
| XML | `**/*.xml` | `**/build/**/*.xml` | XML용 license header |

Kotlin target이 source set별로 나뉘지 않으므로 plugin이 적용된 모듈에서는 `commonMain`, `androidMain`, `androidHostTest`, `iosMain` 등 디렉터리 이름과 관계없이 모든 `.kt`가 같은 `spotlessKotlinCheck`에서 검사된다. generated source가 `build/` 밖에 생성되면 현재 exclude로는 걸러지지 않는다.

현재 `.editorconfig`는 ktlint의 filename, import ordering, wrapping, argument list wrapping, multiline if/else, trailing comma 등의 일부 표준 규칙을 비활성화한다. 따라서 “ktlint 지원”이 “모든 ktlint 표준 규칙 활성화”를 뜻하지는 않는다.

### Detekt

`DetektPlugin.kt`는 다음을 설정한다.

- root `config/detekt/detekt.yml`과 존재하지 않는 `config/detekt/baseline.xml` 경로 사용
- `buildUponDefaultConfig = true`, `ignoreFailures = false`, `autoCorrect = false`, parallel 실행
- `detekt-formatting` ruleset 추가
- 모든 `Detekt` task에 `jvmTarget = 17`
- 모든 `Detekt` task의 source를 각 모듈의 `./` 전체로 다시 설정하고 `**/*.kt`, `**/*.kts`를 포함하며 `resources/`, `build/`를 제외

마지막 source 재설정이 핵심 한계다. Detekt plugin이 만든 `detektAndroidMain`, `detektMetadataCommonMain`, `detektIosArm64Main` 등의 원래 compilation별 입력이 모두 같은 모듈 전체 파일 트리로 바뀐다. task 이름과 classpath는 target별이어도 실제 입력은 source set별로 분리되지 않는다. 특히 Android type-resolution task가 iOS/common 파일과 모듈의 `build.gradle.kts`까지 함께 받으므로 정확한 의미 분석을 보장하기 어렵고 같은 파일이 여러 task에서 중복 검사될 수 있다.

`./gradlew :composeApp:tasks --all --offline`로 확인한 대표 task는 다음과 같다.

| Task 종류 | 예시 | Type resolution | 현재 실제 source 입력 |
| --- | --- | --- | --- |
| 일반 | `detekt` | 없음 | 모듈 전체 `.kt`/`.kts` |
| Android | `detektAndroidMain`, `detektAndroidHostTest` | 있음 | 모듈 전체 `.kt`/`.kts`로 덮어씀 |
| metadata/common/intermediate | `detektMetadataCommonMain`, `detektMetadataIosMain`, `detektMetadataAppleMain` | 없음 | 모듈 전체 `.kt`/`.kts`로 덮어씀 |
| Kotlin/Native | `detektIosArm64Main`, `detektIosSimulatorArm64Main`, `detektIosX64Main` 및 test task | 없음 | 모듈 전체 `.kt`/`.kts`로 덮어씀 |

## 모듈 및 source set별 적용 여부

| 범위 | Spotless/ktlint | Detekt | 비고 |
| --- | --- | --- | --- |
| `composeApp` | 적용 | 적용 | `commonMain`, `androidMain`, `androidHostTest`, `androidRelease`, `iosMain`의 실제 파일 포함 |
| `core:common`, `core:data`, `core:database`, `core:datastore`, `core:designsystem`, `core:domain`, `core:navigation`, `core:ui` | 적용 | 적용 | 각 모듈의 존재하는 common/Android/iOS/test 파일 포함 |
| `feature:complete`, `feature:home`, `feature:onboarding` | 적용 | 적용 | `bandalart.kmp.feature`가 `bandalart.lint`를 적용 |
| `feature:splash` | **미적용** | **미적용** | KMP 모듈이지만 `bandalart.lint`/`bandalart.detekt`가 없음 |
| `androidApp` | 미적용 | 미적용 | Android application module |
| `baselineprofile` | 미적용 | 미적용 | Android test module |

`bandalart.kmp.feature`는 `bandalart.lint`를 통해 Detekt를 이미 적용한 뒤 `bandalart.detekt`를 다시 요청한다. Gradle plugin idempotency 때문에 task가 두 벌 생성되지는 않지만 중복 선언은 불필요하다.

## 현재 검증 결과와 기존 문서 상태

기존 `docs/KMP_AGP_9_MIGRATION_STRATEGY.md`와 `docs/KMP_METRO_MIGRATION_TROUBLESHOOTING.md`에는 “KMP에서 task가 생성되지만 규칙/source 범위 정비가 필요하며 CI gate에서 제외했다”는 요약만 있다. 지원 범위, 누락 모듈, type resolution 한계, Detekt source 덮어쓰기는 기록되어 있지 않았다.

현재 PR branch에서 실제 실행한 결과는 다음과 같다.

- `composeApp` Spotless 검사는 처음에 `RepositoryBindings.kt`의 formatting 2건을 발견했고 이번 PR에서 수정했다. 이후 검사에서는 기존 `BandalartApp`, `BandalartNavHost`, `BandalartSnackbar`, `MainViewController`의 function naming 4건이 남았다.
- `composeApp`의 일반 `detekt` task는 기존 `MainViewController` naming 1건으로 실패했다.
- 현재 `.github/workflows/android-ci.yml`은 단위 테스트, `:androidApp:lintDebug`, Android/iOS build만 실행하며 Spotless/Detekt를 실행하지 않는다.

이는 도구가 KMP에서 실행되지 않는 문제가 아니라, **지원되지만 기존 baseline/규칙/적용 범위 정비가 끝나지 않은 상태**라는 뜻이다.

## CI 권장 task와 도입 순서

### 지금

현재 CI 명령은 유지한다. Spotless/Detekt는 실패 원인과 누락 범위를 정리하는 별도 작업에서 먼저 통과시킨다. `ktlintCheck`는 이 저장소에 생성되지 않는 task이므로 사용하지 않는다.

로컬 현황 확인에는 아래 selector를 사용한다.

```bash
./gradlew spotlessCheck detekt
```

현재 이 명령은 적용된 12개 모듈의 `spotlessCheck`와 type resolution 없는 일반 `detekt`를 선택한다. `feature:splash`, `androidApp`, `baselineprofile`은 포함하지 않는다.

### CI gate로 올리기 전 필수 정비

1. 정적 분석 대상 모듈 정책을 확정하고 최소한 누락된 KMP 모듈 `feature:splash`에 동일 convention을 적용한다.
2. 기존 Spotless/Detekt 위반을 수정하거나 의도적인 baseline으로 동결한다.
3. Detekt의 `tasks.withType<Detekt> { source = ... }` 전역 덮어쓰기를 제거한다. 일반 `detekt`만 별도 범위로 설정하거나 KMP plugin이 제공한 compilation별 source를 보존한다.
4. Native task에는 type resolution이 없다는 전제하에 규칙 결과를 해석한다. 플랫폼별 타입 규칙의 완전한 대체재로 사용하지 않는다.
5. report merge가 필요한 형식과 산출물 경로를 분리해 검증한다.

### 정비 후 권장 gate

```bash
./gradlew spotlessCheck detekt
```

- `spotlessCheck`: 모든 적용 모듈의 ktlint/헤더/Gradle Kotlin DSL/XML 검사
- `detekt`: KMP 전체 파일의 공통 syntax/구조 규칙 검사, type resolution 없음

Android/JVM 의미 분석 규칙도 gate로 삼으려면 compilation별 source 입력을 복구한 후 생성된 Android task를 별도 실행한다.

```bash
./gradlew detektAndroidMain
```

`androidHostTest`가 있는 모듈은 해당 `detektAndroidHostTest`도 명시적으로 추가한다. iOS task는 별도로 실행할 수 있지만 type resolution이 없으므로 `detektAndroidMain`과 동등한 의미 분석으로 간주하면 안 된다.

## 알려진 한계

- ktlint/Spotless는 KMP compiler 검증이 아니다. `expect`/`actual` 연결, source set dependency, 플랫폼 API 사용 가능 여부는 Kotlin compilation이 검증한다.
- Detekt 1.23.8의 Native/iOS 분석에는 type resolution이 없다. 타입이 필요한 규칙은 실행되지 않거나 제한된 동작만 한다.
- 현재 Detekt source 덮어쓰기로 compilation별 task의 장점이 훼손되어 있다.
- Spotless와 일반 `detekt`는 모두 파일 기반 범위를 넓게 잡으므로 build 밖의 generated Kotlin은 별도 exclude가 필요하다.
- Spotless의 `$YEAR`는 기존의 유효한 연도를 자동 갱신하지 않는다.
- Detekt 1.23.8은 공식 페이지에서도 더 이상 actively maintained 버전이 아니며 Kotlin 2.0.21로 빌드되었다. 현재 프로젝트 Kotlin/Gradle/AGP 조합에서 task 생성은 확인했지만, version upgrade는 규칙/baseline과 함께 별도 검증해야 한다.
