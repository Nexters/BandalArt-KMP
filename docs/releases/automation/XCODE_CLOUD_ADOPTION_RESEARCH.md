# Xcode Cloud 도입 검토

- 작성일: 2026-08-18
- 관련 이슈: [#334](https://github.com/Nexters/BandalArt-KMP/issues/334)
- 대상: BandalArt KMP의 iOS CI, TestFlight 및 App Store 배포 경로
- 조사 원칙: 기능·요금·제약은 Apple 공식 문서만 근거로 삼고, 프로젝트 적합성은 현재 저장소 구성을 대조해 판단한다.

## 결론

**현재는 Xcode Cloud를 전체 CI/CD에 연동하거나 기존 GitHub Actions를 대체하지 않는다.**

이미 GitHub Actions가 PR마다 공통 KMP 테스트·정적 분석·Android 빌드·iOS host app 빌드를 실행하고, 수동 Release CD가 Android Internal Testing과 iOS TestFlight의 버전·광고 모드·서명·업로드 후 검증까지 담당한다. Xcode Cloud를 지금 병행하면 iOS build와 TestFlight 업로드가 중복되고, 실패 표면과 유지 대상이 두 개로 늘어난다.

다만 Xcode Cloud 자체는 Apple 플랫폼 전용 CI/CD로서 충분한 기능을 제공한다. Apple은 build, analyze, test, archive action, pull request·branch·tag·schedule 기반 실행, TestFlight 배포와 App Store 심사 제출이 가능한 binary 생성을 지원한다. 첫 설정은 Xcode에서 해야 하지만 이후 workflow와 build는 Xcode 또는 App Store Connect에서 관리할 수 있다. ([Xcode Cloud 개요](https://developer.apple.com/documentation/xcode/xcode-cloud), [workflow action](https://developer.apple.com/documentation/xcode/configuring-your-xcode-cloud-workflow-s-actions), [workflow reference](https://developer.apple.com/documentation/xcode/xcode-cloud-workflow-reference))

따라서 향후 **iOS XCTest/UI test가 생겨 Apple simulator matrix를 병렬 실행할 가치가 커지거나**, **현재 TestFlight 자격증명·runner 관리 비용을 줄이는 것이 우선순위가 될 때** 제한된 pilot부터 시작하는 것이 적절하다.

## Xcode Cloud가 지원하는 것

### CI action과 결과

Workflow에는 다음 action을 한 개 이상 배치할 수 있다.

- Build: scheme을 빌드한다.
- Analyze: 정적 분석을 수행한다.
- Test: scheme 또는 test plan을 지정하고 하나 이상의 simulator destination에서 테스트한다.
- Archive: TestFlight 또는 App Store 배포용 archive를 만든다.

각 action은 격리된 임시 환경에서 repository clone, dependency resolution, custom script 실행, action 수행, artifact 저장 순으로 처리된다. Action별 환경이 분리되므로 한 action의 artifact가 다른 action에 항상 전달되는 구조는 아니다. ([workflow action](https://developer.apple.com/documentation/xcode/configuring-your-xcode-cloud-workflow-s-actions))

빌드 로그, export된 archive·binary·framework, test result bundle과 UI test screenshot을 Xcode와 App Store Connect에서 확인할 수 있다. Build 정보와 artifact는 완료 시점부터 30일 동안 접근 가능하므로 출시 artifact를 장기 보관하려면 별도 보관이 필요하다. ([첫 workflow 구성](https://developer.apple.com/documentation/xcode/configuring-your-first-xcode-cloud-workflow))

### 자동 실행과 GitHub 연동

Branch 변경, pull request 생성·변경, Git tag, schedule 등을 시작 조건으로 사용할 수 있다. Pull request 검증 시에는 관련 branch를 임시 환경에서 merge한 뒤 build와 test를 수행하고 결과를 PR status로 게시하며, SCM에서 성공을 merge 조건으로 요구할 수도 있다. ([CI/CD 개요](https://developer.apple.com/documentation/xcode/about-continuous-integration-and-delivery-with-xcode-cloud), [workflow reference](https://developer.apple.com/documentation/xcode/xcode-cloud-workflow-reference))

GitHub와 GitHub Enterprise가 공식 지원된다. GitHub organization repository를 처음 연결하는 사람은 organization owner여야 하고, 개인 repository라면 admin 권한이 필요하다. Apple은 GitHub App을 필요한 repository에만 설치하도록 안내한다. ([Xcode Cloud와 GitHub 연결](https://developer.apple.com/documentation/xcode/connecting-xcode-cloud-to-github), [도입 요구사항](https://developer.apple.com/documentation/xcode/setting-up-your-project-to-use-xcode-cloud))

### TestFlight와 App Store

Archive action의 deployment preparation은 최소 다음 두 배포 수준을 구분한다.

- `TestFlight (Internal Testing Only)`: 내부 TestFlight용 binary
- `TestFlight and App Store`: 외부 TestFlight 및 App Store 출시 자격을 갖춘 binary

Post-action으로 내부·외부 TestFlight tester 또는 group에 배포할 수 있다. 외부 TestFlight는 beta app review 대상이며, App Store 공개는 별도로 App Review에 제출해야 한다. 즉 Xcode Cloud가 binary 생성·서명·업로드를 자동화해도 심사와 공개 정책까지 생략하는 것은 아니다. ([배포 workflow 구성](https://developer.apple.com/documentation/xcode/creating-a-workflow-that-builds-your-app-for-distribution), [TestFlight 배포](https://developer.apple.com/documentation/xcode/distributing-your-xcode-cloud-builds-through-testflight))

Xcode Cloud는 자신이 수행하는 build마다 `1`부터 자동 증가하는 정수 build number를 할당하고, 배포 시 App Store Connect도 그 번호를 사용한다. iOS는 marketing version과 build number 조합의 유일성을 요구하므로 새 marketing version에서는 이전 버전보다 낮은 build number도 허용된다. 기존 배포 자동화에서 전환한다면 같은 marketing version의 기존 번호와 충돌하지 않도록 새 version 경계에서 시작하거나 전환 번호를 먼저 검증해야 한다. ([Xcode Cloud build number](https://developer.apple.com/documentation/xcode/setting-the-next-build-number-for-xcode-cloud-builds))

언어·지역별 `What to Test` 파일도 repository의 `TestFlight` 폴더에서 제공할 수 있다. ([tester note 제공](https://developer.apple.com/documentation/xcode/including-notes-for-testers-with-a-beta-release-of-your-app))

### 환경, 의존성, secret

Xcode Cloud 환경에는 macOS·Xcode 구성 요소, Python과 Homebrew가 제공된다. 기본 이미지에 없는 도구는 `ci_scripts/ci_post_clone.sh` 등 custom build script로 설치할 수 있고, post-clone·pre-xcodebuild·post-xcodebuild 시점을 지원한다. Script는 `sudo`를 사용할 수 없다. ([dependency 준비](https://developer.apple.com/documentation/xcode/making-dependencies-available-to-xcode-cloud), [custom build script](https://developer.apple.com/documentation/xcode/writing-custom-build-scripts))

Workflow 환경변수는 secret으로 표시해 로그에서 값을 가릴 수 있다. Xcode Cloud는 build 때만 source에 접근하며 임시 build 환경은 완료 후 폐기되고, 저장 데이터는 암호화되며 접근에는 이중 인증을 사용한다. ([workflow reference](https://developer.apple.com/documentation/xcode/xcode-cloud-workflow-reference), [Xcode Cloud 소개](https://developer.apple.com/xcode-cloud/))

## 요금과 사용량

Apple Developer Program 멤버십에는 팀 전체 기준 월 25 compute hour가 포함된다. 2026-08-18 공식 표시 가격은 다음과 같다. ([Xcode Cloud 시작하기](https://developer.apple.com/xcode-cloud/get-started/))

| 월 compute hour | 요금 |
| ---: | ---: |
| 25시간 | Developer Program 멤버십에 포함 |
| 100시간 | US$49.99/월 |
| 250시간 | US$99.99/월 |
| 1,000시간 | US$399.99/월 |
| 10,000시간 | US$3,999.99/월 |

Compute hour는 build나 test 같은 특정 task가 cloud에서 실행된 시간의 합계다. 예를 들어 12분짜리 test 5개는 1 compute hour이며, 사용하지 않은 시간은 다음 달로 이월되지 않는다. 사용량은 App Store Connect와 Apple Developer 앱에서 추적할 수 있다. ([Xcode Cloud 시작하기](https://developer.apple.com/xcode-cloud/get-started/))

BandalArt처럼 KMP framework를 만드는 iOS build는 Xcode build뿐 아니라 Gradle과 dependency 준비 시간도 포함한다. 따라서 무료 25시간으로 pilot은 가능하지만, 모든 PR에서 기존 GitHub Actions와 중복 실행하는 구성은 비용 효율을 실제 측정하기 전에는 선택하지 않는다.

## BandalArt와의 기술 적합성

### 충족하는 선행 조건

Apple은 Xcode 15 이상, Apple Developer Program 가입, App Store Connect app record, 고정된 Xcode project/workspace, shared scheme, archive 가능한 scheme, automatic signing, 원격 Git repository와 접근 가능한 의존성을 요구한다. ([도입 요구사항](https://developer.apple.com/documentation/xcode/setting-up-your-project-to-use-xcode-cloud))

현재 저장소는 다음 조건을 이미 갖춘다.

- `iosApp/iosApp.xcodeproj`가 repository에 고정되어 있다.
- `iosApp` shared scheme이 추적되며 build·analyze·test·archive가 활성화되어 있다.
- app과 widget target 모두 automatic signing과 명시적 bundle identifier를 사용한다.
- Swift Package의 `Package.resolved`가 권장 위치에 추적되어 있다. Apple은 Xcode Cloud가 자동 package resolution 대신 이 lock file을 사용하므로 repository에 commit하라고 안내한다. ([dependency 준비](https://developer.apple.com/documentation/xcode/making-dependencies-available-to-xcode-cloud))

### KMP 때문에 별도 검증이 필요한 부분

Xcode Cloud가 Kotlin Multiplatform을 별도 제품 유형으로 직접 지원한다고 명시한 Apple 문서는 없다. 이 프로젝트의 가능성은 **Xcode Cloud가 Xcode project의 shell build phase를 실행하고 custom script로 제3자 도구를 준비할 수 있다는 일반 기능에 근거한 추론**이다. ([workflow action](https://developer.apple.com/documentation/xcode/configuring-your-xcode-cloud-workflow-s-actions), [custom build script](https://developer.apple.com/documentation/xcode/writing-custom-build-scripts))

현재 app과 widget Xcode target은 각각 Gradle의 `embedAndSignAppleFrameworkForXcode` task를 shell phase에서 호출한다. 현 GitHub Actions가 iOS build 전 JDK 21, Android SDK와 Gradle을 명시적으로 준비하는 이유도 이 경로 때문이다. 따라서 Xcode Cloud pilot은 최소 다음을 실제 build로 입증해야 한다.

1. `ci_post_clone.sh`에서 JDK 21과 필요한 Android SDK·Gradle 환경을 관리자 권한 없이 재현할 수 있다.
2. app과 widget의 KMP framework shell phase가 Debug simulator build와 Release archive 양쪽에서 성공한다.
3. Firebase와 Google Mobile Ads Swift Package가 committed `Package.resolved`로 재현된다.
4. App Group entitlement가 있는 app·widget 두 target의 automatic signing과 archive export가 성공한다.
5. `SERVER_BASE_URL`, 광고 모드 등 build 입력을 secret 또는 일반 환경변수로 안전하게 전달한다.

Apple은 기본 환경에 없는 제3자 도구를 Homebrew와 custom script로 설치할 수 있다고 명시하지만, 설치 시간과 KMP/Gradle cache 효율을 보장하지는 않는다. ([dependency 준비](https://developer.apple.com/documentation/xcode/making-dependencies-available-to-xcode-cloud)) 따라서 위 항목은 문서상 가능 여부가 아니라 무료 quota 안의 pilot build 시간과 성공률로 판단해야 한다.

### 현재 자동화와 겹치는 범위

| 책임 | 현재 GitHub Actions | Xcode Cloud 도입 시 |
| --- | --- | --- |
| 공통 KMP·Android unit test | `allTests`, Android unit test | 이관 이점 없음. 기존 CI 유지 |
| 정적 분석·Android build | Spotless, Detekt, lint, assemble | 이관 이점 없음. 기존 CI 유지 |
| iOS PR build | simulator용 `xcodebuild` | 거의 완전히 중복 |
| iOS XCTest/UI test | 현재 shared scheme에 명시적 test target이 없음 | test target 추가 후 simulator matrix에서 차별점 발생 |
| TestFlight | Fastlane이 수동 workflow에서 archive·upload·사후 조회 | Xcode Cloud archive와 TestFlight post-action이 중복 |
| signing | GitHub secret의 app/widget profile과 distribution certificate | automatic signing을 전제로 Apple 쪽 관리로 단순화 가능 ([도입 요구사항](https://developer.apple.com/documentation/xcode/setting-up-your-project-to-use-xcode-cloud)) |
| version/build·광고 정책 | 같은 train의 다음 build 계산, test/production 광고 모드, 최신 main guard를 repository code로 검증 | Xcode Cloud의 자동 증가 build number로 정책을 전환할 수 있지만, 광고 모드·source 제한과 migration 충돌 검증은 별도 설계 필요 ([Xcode Cloud build number](https://developer.apple.com/documentation/xcode/setting-the-next-build-number-for-xcode-cloud-builds)) |
| 결과 확인 | GitHub PR check와 Actions log | Xcode·App Store Connect 통합 결과와 TestFlight feedback 제공 ([Xcode Cloud 소개](https://developer.apple.com/xcode-cloud/)) |

Xcode Cloud는 GitHub Actions보다 상위 호환인 범용 CI가 아니라 Apple 플랫폼에 밀착된 별도 CI/CD다. 현재의 공통 KMP·Android 검증을 대체하지 못하므로 도입하더라도 GitHub Actions 전체를 없애는 구조는 적절하지 않다.

## 도입하는 편이 나아지는 조건

다음 중 하나 이상이 실제 우선순위가 되면 pilot 가치가 있다.

- iOS XCTest 또는 UI test가 추가되어 여러 simulator destination 병렬 검증이 필요하다. Xcode Cloud는 여러 destination test와 병렬 실행을 지원한다. ([Xcode Cloud 소개](https://developer.apple.com/xcode-cloud/), [workflow action](https://developer.apple.com/documentation/xcode/configuring-your-xcode-cloud-workflow-s-actions))
- TestFlight 배포 빈도가 높아져 GitHub macOS runner, 수동 distribution certificate·profile secret 갱신, Fastlane 유지비보다 Apple 통합 배포의 편익이 커진다.
- 최신 공개 Xcode뿐 아니라 beta Xcode/macOS에서도 별도 호환성 workflow를 운영할 필요가 생긴다. Workflow마다 제공되는 Xcode·macOS 환경을 선택할 수 있다. ([workflow reference](https://developer.apple.com/documentation/xcode/xcode-cloud-workflow-reference))
- App Store Connect에서 build, test, TestFlight feedback과 사용량을 한곳에서 관리하는 것이 팀 workflow에 실질적인 이점이 된다. ([Xcode Cloud 소개](https://developer.apple.com/xcode-cloud/))

## 도입하지 않는 편이 나은 조건

- iOS 검증이 현재처럼 host app compile 위주이고 XCTest/UI test matrix가 없다.
- 월 TestFlight 배포 횟수가 적고 현재 Release CD가 안정적으로 작동한다.
- Xcode Cloud에서도 JDK·Android SDK 설치와 KMP framework build가 매번 긴 시간을 사용해 무료 quota를 빠르게 소진한다.
- Xcode Cloud의 자동 build number로 전환하더라도 test/production 광고 분리, app·widget signing 검증과 업로드 후 조회를 별도로 유지해야 하며 유지보수 절감이 migration 비용보다 작다.
- 같은 commit을 두 CI에서 필수 check로 실행해 대기 시간과 실패 원인만 늘어난다.

## 권장 도입 범위

### 0단계 — 현재

- Xcode Cloud의 repository 접근을 아직 승인하지 않는다.
- 기존 GitHub Actions PR CI와 수동 Release CD를 source of truth로 유지한다.
- Xcode Cloud 탭이 보인다는 이유만으로 설정할 필요는 없다.

### 1단계 — 조건 충족 시 비필수 pilot

다음 조건이 생겼을 때 별도 구현 이슈로 진행한다.

- iOS XCTest/UI test를 최소 한 개 이상 실제 회귀 gate로 운영하거나,
- TestFlight signing secret 유지비를 줄이는 것이 명시적 목표가 된다.

Pilot은 다음 범위로 제한한다.

1. GitHub App은 BandalArt repository 하나에만 권한을 부여한다. ([GitHub 연결](https://developer.apple.com/documentation/xcode/connecting-xcode-cloud-to-github))
2. 수동 시작 또는 전용 pilot branch에만 반응하는 workflow를 만든다.
3. `Build` 한 action으로 Debug simulator build만 수행하고, 필수 PR check나 TestFlight post-action은 설정하지 않는다.
4. JDK 21·Android SDK 준비, app/widget KMP framework build, 총 compute time과 cache 효과를 3회 연속 측정한다.
5. 3회 모두 성공하고 중앙값 build 시간이 수용 가능할 때만 다음 단계로 간다.

### 2단계 — Apple 전용 test 보강

- iOS XCTest/UI test가 존재할 때에만 제한된 simulator matrix를 추가한다.
- 모든 PR이 아니라 iOS 관련 path, main 변경 또는 schedule에만 실행해 25시간 quota를 우선 활용한다.
- 안정화 전에는 GitHub `ci-build`의 필수 조건으로 지정하지 않는다.

### 3단계 — 선택적 TestFlight 이관

다음 항목이 기존 Release CD와 동등하게 검증된 경우에만 GitHub Actions의 iOS TestFlight job을 Xcode Cloud로 교체한다.

- app과 widget App Group signing
- Xcode Cloud 자동 build number와 기존 TestFlight train의 충돌 없는 migration
- test/production 광고 모드 분리
- locale별 tester note
- 최신 `main` 또는 release tag 제한
- 업로드된 정확한 version/build 확인
- 실패 시 재실행·복구 절차

이 단계에서도 Android CI와 Play 배포는 GitHub Actions에 남긴다. External TestFlight와 App Store용 archive를 만들 때에는 clean build, restricted workflow editing, `TestFlight and App Store` deployment preparation을 적용하고 App Review는 App Store Connect에서 별도로 진행한다. ([배포 workflow 구성](https://developer.apple.com/documentation/xcode/creating-a-workflow-that-builds-your-app-for-distribution))

## 최종 추천

이슈 #334의 현재 결정은 **도입 보류**다. 무료 25시간은 매력적이지만 현재 BandalArt가 당장 얻을 유일한 큰 차별점은 Apple simulator test와 signing·TestFlight 통합이고, 전자는 아직 명시적 iOS test suite가 없으며 후자는 이미 검증 로직을 갖춘 GitHub Actions/Fastlane 경로가 있다.

Xcode Cloud를 “설정해 두면 좋은 기능”으로 추가하지 말고, **iOS 자동 테스트 확대 또는 TestFlight 운영비 절감이라는 측정 가능한 목표가 생겼을 때 1단계 pilot으로 재검토**한다. Pilot 전까지는 App Store Connect의 Xcode Cloud 탭을 미설정 상태로 두어도 앱 심사·출시·리뷰 대응에는 영향이 없다. Xcode Cloud는 App Store build 업로드 방법 중 하나이며 필수 제출 경로는 아니다. ([App Store Connect build 업로드](https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds))
