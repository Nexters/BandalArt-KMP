# iOS를 Xcode 화면 없이 빌드·아카이브하는 방법

## 결론

Xcode 앱 화면을 열지 않아도 iOS 앱을 빌드하고 Archive와 IPA를 만들 수 있다. Fastlane이 Xcode를 대체해서가 아니라, Xcode에 포함된 명령줄 도구인 `xcodebuild`를 대신 호출하고 반복 작업을 자동화하기 때문이다.

```text
사람 또는 GitHub Actions
  -> Fastlane
     -> xcodebuild
        -> KMP Gradle build phase
        -> Xcode Archive (.xcarchive)
        -> App Store용 IPA
     -> App Store Connect API
        -> TestFlight 업로드와 빌드 확인
```

Xcode GUI는 필요하지 않지만 Xcode 자체와 Command Line Tools는 반드시 설치돼 있어야 한다. iOS SDK, Swift compiler, codesign, archive·export 기능은 모두 Xcode가 제공한다.

## 각 도구의 역할

| 도구 | 역할 |
| --- | --- |
| Xcode 앱 | 프로젝트 설정, 화면 기반 빌드·디버깅, Organizer에서 Archive 관리 |
| `xcodebuild` | Xcode의 build, test, archive, export 기능을 터미널에서 실행 |
| Fastlane | `xcodebuild` 명령 구성, 서명 설정, IPA 검증, App Store Connect 업로드를 한 흐름으로 자동화 |
| GitHub Actions macOS runner | Xcode와 빌드 도구가 설치된 일회성 Mac 환경을 제공하고 저장된 secret을 빌드 중에만 복원 |

Android에서 Android Studio가 Gradle을 실행하는 화면 도구이고 `./gradlew`만으로도 빌드할 수 있는 것과 비슷하다. iOS에서는 Xcode 앱이 화면 도구이고 `xcodebuild`가 실제 명령줄 빌드 도구다. 차이는 Apple 플랫폼 빌드에 Xcode와 macOS가 필수라는 점이다.

## BandalArt의 현재 TestFlight 흐름

1. `.github/workflows/release-cd.yml`이 GitHub의 macOS runner를 준비한다.
2. runner에서 Xcode 버전, JDK, Android SDK, Gradle과 Ruby를 준비한다.
3. Apple Distribution 인증서와 앱·위젯 provisioning profile을 GitHub Environment secret에서 임시 파일로 복원한다.
4. `bundle exec fastlane ios beta`를 실행한다.
5. Fastlane은 App Store Connect에서 현재 iOS 버전의 최신 build number를 읽고 다음 번호를 선택한다.
6. Fastlane `build_app`이 내부적으로 `xcodebuild archive`와 export를 실행한다.
7. Xcode build phase가 다음 Gradle 작업을 호출해 KMP framework를 앱과 위젯에 포함한다.
   - `:composeApp:embedAndSignAppleFrameworkForXcode`
   - `:iosWidgetShared:embedAndSignAppleFrameworkForXcode`
8. 생성된 IPA에서 앱·위젯의 bundle ID, 버전, build number, provisioning profile과 code signature를 검사한다.
9. 검증된 IPA만 TestFlight에 업로드하고 App Store Connect에서 정확한 build가 조회되는지 다시 확인한다.
10. 성공·실패와 관계없이 runner의 인증서, profile과 임시 credential을 삭제한다. runner 자체도 작업 종료 후 폐기된다.

따라서 이 과정은 로컬 Mac의 Xcode 창을 백그라운드로 조작하지 않는다. 빌드와 Archive는 GitHub의 별도 Mac에서 명령줄로 수행된다.

## Xcode GUI에서 하는 일과의 대응 관계

| Xcode GUI | 자동화 흐름 |
| --- | --- |
| Scheme과 Release configuration 선택 | Fastlane `build_app` 옵션 |
| Product > Archive | `xcodebuild archive` |
| Organizer > Validate App | IPA 구조·서명 검증과 App Store Connect 처리 |
| Distribute App > App Store Connect > Upload | Fastlane `upload_to_testflight` |
| TestFlight에서 build 확인 | App Store Connect API exact build 조회 |

자동화해도 Xcode GUI로 만든 Archive와 본질적으로 다른 형식이 생기는 것은 아니다. 같은 Xcode build system이 같은 project와 scheme을 읽는다.

## 로컬 Mac에서 가능한 범위

로컬에서도 Xcode 앱을 열지 않고 기본 빌드를 확인할 수 있다.

```bash
xcode-select -p
xcodebuild -version
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  build
```

Release Archive와 App Store용 IPA export도 명령줄로 가능하지만 다음 조건이 더 필요하다.

- 유효한 Apple Distribution 인증서와 private key
- 앱과 위젯 각각의 App Store provisioning profile
- Team ID, bundle ID와 App Group capability 일치
- export options와 code signing 설정
- KMP build phase를 위한 JDK, Android SDK와 Gradle 환경
- 업로드하려면 App Store Connect 인증 정보

BandalArt의 `fastlane ios beta` lane은 실제 업로드를 바꾸는 작업이므로 안전장치상 GitHub Actions의 최신 `main`에서만 실행된다. 로컬에서 `CI=true` 같은 값을 흉내 내 우회하지 않는다. 로컬은 Debug 또는 서명 없는 검증에 사용하고, TestFlight 배포는 Release CD를 사용한다.

## 언제 Xcode 앱을 직접 여는가

다음 작업은 Xcode GUI가 더 적합하다.

- 시뮬레이터나 실기기에서 화면을 보며 실행·디버깅할 때
- breakpoint, memory graph, Instruments처럼 대화형 도구가 필요할 때
- target, capability, build setting과 scheme을 처음 구성하거나 눈으로 확인할 때
- Archive와 App Store validation 오류를 Organizer에서 자세히 조사할 때

반복 가능한 CI 빌드, Archive, 서명 검증과 TestFlight 업로드는 명령줄 자동화가 적합하다. 개발 중 대화형 확인은 Xcode GUI, 재현 가능한 배포는 Fastlane과 `xcodebuild`로 역할을 나누면 된다.

## 관련 파일

- `.github/workflows/release-cd.yml`
- `fastlane/Fastfile`
- `iosApp/iosApp.xcodeproj/project.pbxproj`
- `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`
- [Fastlane CD 파일 가이드](../automation/FASTLANE_CD_FILE_GUIDE.md)
- [AdMob 광고 ID와 운영 검증 가이드](../../features/ads/ADMOB_AD_ID_POLICY_GUIDE.md)
