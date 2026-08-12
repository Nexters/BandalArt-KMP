# Fastlane CD 파일 가이드

이 문서는 BandalArt의 Continuous Delivery(CD)에 참여하는 파일의 책임과 수정 기준을 설명한다. 배포 설계, secret 계약, 실패 처리 원칙은 [Fastlane Android/iOS CD 복구 전략](FASTLANE_CD_RECOVERY_STRATEGY.md)을 따른다.

## 배포 흐름

GitHub Actions가 배포 환경과 자격증명을 준비하고 Fastlane lane을 실행한다. Fastlane은 소스와 배포 자산을 검증한 뒤 Android는 Gradle Play Publisher, iOS는 Xcode와 App Store Connect API를 호출한다.

```text
.github/workflows/release-cd.yml
  -> fastlane/Fastfile
     -> Android: Gradle Play Publisher + Play 검증 스크립트
     -> iOS: Xcode archive + App Store Connect 검증 helper
```

## GitHub Actions 파일

| 파일 | 역할 | 수정하는 경우 |
| --- | --- | --- |
| `.github/workflows/release-cd.yml` | `main`에서 Android Internal Testing, iOS TestFlight 또는 둘 다를 수동 실행한다. GitHub Environment의 secret을 runner 임시 파일로 복원하고 배포 후 삭제한다. | 배포 입력, runner, secret 이름, 환경 준비 또는 cleanup 절차가 바뀔 때 |
| `.github/workflows/android-ci.yml` | PR에서 workflow 문법, Ruby helper, Python 배포 스크립트, Fastlane lane, iOS 공유 scheme을 검증한다. Markdown만 바뀐 PR은 현재 경로 필터에 따라 실행하지 않는다. | CD 검증 항목이나 테스트 파일을 추가·제거할 때 |

`release-cd.yml`은 배포의 진입점이고 `android-ci.yml`은 배포 코드의 회귀를 막는 검증 지점이다. 실제 업로드 동작은 workflow shell에 중복하지 않고 `fastlane/Fastfile`과 하위 도구에 둔다.

## Fastlane 파일

| 파일 | 역할 | 수정하는 경우 |
| --- | --- | --- |
| `fastlane/Appfile` | Android package name과 iOS App Store Connect 메타데이터인 bundle ID, Apple ID, Team ID를 선언한다. | 앱 식별자나 Apple 팀이 바뀔 때 |
| `fastlane/Fastfile` | Android `internal`, iOS `preflight`와 `beta` lane을 정의한다. 소스 SHA, 버전, 광고 ID, 서명 자산, 업로드 결과를 검증하고 플랫폼별 배포를 조율한다. | 배포 순서, 사전·사후 검증, store 업로드 방식이 바뀔 때 |
| `fastlane/README.md` | 설치 방법과 현재 lane 목록을 보여 주는 Fastlane 생성 문서다. Fastlane 실행 시 다시 생성되므로 수동 운영 문서를 이 파일에 추가하지 않는다. | 직접 수정하지 않고 lane 설명을 바꾼 뒤 Fastlane으로 재생성할 때 |
| `fastlane/metadata/ios/what_to_test.txt` | TestFlight 빌드에 등록할 내부 테스트 안내다. | TestFlight에서 확인할 기능이나 주의사항이 바뀔 때 |

### Fastlane helper

| 파일 | 역할 |
| --- | --- |
| `fastlane/lib/app_store_connect_builds.rb` | Individual App Store Connect 키로 JWT를 만들고, 특정 marketing version의 build 번호를 페이지 끝까지 조회한다. 다음 build 번호 계산과 업로드 결과 확인에 사용한다. |
| `fastlane/lib/plist_file.rb` | XML과 binary plist를 `plutil`로 같은 형식으로 변환해 읽는다. 생성한 IPA의 앱·위젯 정보를 검사할 때 사용한다. |
| `fastlane/lib/provisioning_profile.rb` | provisioning profile 만료 시각을 Ruby `Time`으로 정규화한다. |

### Fastlane 테스트

| 파일 | 보호하는 계약 |
| --- | --- |
| `fastlane/test/android_update_priority_test.rb` | workflow 입력, Fastlane 전달값, Gradle 기본값이 Android 인앱 업데이트 우선순위 계약을 유지하는지 확인한다. |
| `fastlane/test/app_store_connect_builds_test.rb` | App Store Connect pagination, 최고 build 번호, 정확한 build 조회, 외부 origin 차단을 확인한다. |
| `fastlane/test/plist_file_test.rb` | binary plist를 읽을 수 있는지 확인한다. |
| `fastlane/test/provisioning_profile_test.rb` | provisioning profile 만료 시각 변환을 확인한다. |
| `fastlane/test/signing_configuration_test.rb` | 본 앱과 위젯에 별도 profile을 매핑하고 Release target에만 manual signing을 적용하는지 확인한다. App Group과 두 target의 버전 일치도 함께 보호한다. |
| `fastlane/test/testflight_test_ads_test.rb` | TestFlight archive가 테스트 광고 compile condition을 사용하고 iOS 광고 bridge가 이를 따르는지 확인한다. |

## Android 배포 파일

| 파일 | 역할 | 수정하는 경우 |
| --- | --- | --- |
| `gradle/libs.versions.toml` | `majorVersion`, `minorVersion`, `patchVersion`으로 Android `versionName`과 `versionCode`의 단일 source를 제공한다. | Android 제품 버전을 올릴 때 |
| `androidApp/build.gradle.kts` | release signing과 Gradle Play Publisher를 설정한다. Internal track, 완료 상태, 서비스 계정 경로, update priority 기본값을 관리한다. | Play 업로드 정책이나 Android release 설정이 바뀔 때 |
| `androidApp/src/main/play/release-notes/{ko-KR,en-US,ja-JP}/internal.txt` | Play Internal Testing에 등록할 언어별 변경 사항이다. | Android 버전을 배포할 때 세 파일을 함께 갱신 |
| `scripts/play_next_version_code.py` | Play의 모든 track을 조회해 현재 최대 versionCode보다 새 버전이 큰지 확인한다. 업로드 후에는 정확한 track, 상태, update priority를 재확인한다. | Play 버전 또는 업로드 결과 검증 규칙이 바뀔 때 |
| `scripts/play_next_version_code.py.lock` | 위 Python 스크립트의 uv 의존성을 고정한 생성 파일이다. | 스크립트의 Python 의존성을 바꾼 뒤 uv로 lock을 갱신할 때 |
| `scripts/validate_play_aab.py` | 업로드할 Android App Bundle(AAB)의 package, 버전, ZIP 무결성, 필수 리소스, 테스트·운영 광고 ID, release note를 검사한다. | AAB에 반드시 포함하거나 금지할 항목이 바뀔 때 |
| `scripts/tests/test_play_release_scripts.py` | Play track 해석과 AAB 검증 규칙의 단위 테스트다. | 위 두 Python 스크립트의 동작을 바꿀 때 |

## iOS 배포 파일

| 파일 | 역할 | 수정하는 경우 |
| --- | --- | --- |
| `iosApp/iosApp/Info.plist` | iOS marketing version의 단일 source인 `CFBundleShortVersionString`을 보관한다. TestFlight build 번호는 배포 시 App Store Connect 최신값 다음 번호로 runner에서만 설정한다. | iOS 제품 버전을 올릴 때 |
| `iosApp/iosApp/iosApp.entitlements` | 본 앱의 App Group 권한을 선언한다. | 본 앱 capability 또는 App Group이 바뀔 때 |
| `iosApp/BandalartWidget/BandalartWidget.entitlements` | 위젯 extension의 App Group 권한을 선언한다. | 위젯 capability 또는 App Group이 바뀔 때 |
| `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` | CI가 사용할 공유 archive scheme이다. 본 앱 archive에 위젯 extension을 포함하는 Xcode target 관계를 노출한다. | archive 대상이나 build action이 바뀔 때 |

App ID capability를 바꾸면 해당 provisioning profile을 다시 발급해야 한다. 본 앱과 위젯의 App Group 및 profile 등록 절차는 [iOS WidgetKit MVP 전략](../../features/widgets/IOS_WIDGETKIT_MVP_STRATEGY.md#10-testflight용-app-group과-서명-자산을-등록하는-방법)을 따른다.

## 도구 버전 파일

| 파일 | 역할 | 수정하는 경우 |
| --- | --- | --- |
| `.ruby-version` | 로컬과 CI에서 사용하는 Ruby major/minor 버전을 고정한다. | 지원 Ruby 버전을 바꿀 때 |
| `Gemfile` | Fastlane 직접 의존성과 버전을 선언한다. | Fastlane을 올리거나 Ruby 배포 도구를 추가할 때 |
| `Gemfile.lock` | Fastlane과 전이 Ruby gem의 정확한 버전을 고정한다. | `Gemfile`을 바꾼 뒤 Bundler로 갱신할 때 |

## 자격증명과 생성 파일

배포 자격증명은 저장소 파일이 아니라 GitHub Environments의 secret으로 관리한다. workflow는 배포 중에만 제한된 권한으로 임시 파일을 만들고 `always()` cleanup에서 삭제한다.

다음 파일은 Git에 추가하지 않는다.

- Android upload keystore, `keystore.properties`, `secrets.properties`, Play service account JSON
- Apple Distribution `.p12`, provisioning `.mobileprovision`, App Store Connect `.p8`
- Fastlane `report.xml`, `test_output/`, `screenshots/`
- 서명된 AAB와 IPA

secret 이름과 등록 환경은 [Fastlane Android/iOS CD 복구 전략의 GitHub Secrets 계약](FASTLANE_CD_RECOVERY_STRATEGY.md#github-secrets-계약)을 기준으로 확인한다. 값이나 복원된 파일 내용을 로그, 이슈, PR에 붙이지 않는다.

## #91의 레거시 파일 대응

이슈 #91에 기록된 초기 Fastlane 파일 중 일부는 현재 배포 구조에서 사용하지 않는다.

| 과거 파일 | 현재 상태와 대체 경로 |
| --- | --- |
| `fastlane/GoogleCloudPlatform.json` | 제거됐다. Play 서비스 계정 JSON은 `PLAY_SERVICE_ACCOUNT_JSON` secret에서 runner 임시 파일로만 복원한다. |
| `fastlane/Pluginfile` | 제거됐다. Firebase App Distribution 플러그인을 사용하지 않으며 Android는 Gradle Play Publisher로 Play Internal Testing에 배포한다. |
| `fastlane/release-notes.txt` | 제거됐다. Android는 Play locale별 `internal.txt`, iOS는 `fastlane/metadata/ios/what_to_test.txt`를 사용한다. |
| `fastlane/report.xml` | 실행 중 생성될 수 있는 산출물이며 `.gitignore` 대상이다. 배포 설정이나 기록의 source로 사용하지 않는다. |

## 변경 목적별 확인 위치

| 목적 | 먼저 확인할 파일 |
| --- | --- |
| Android 버전과 배포 안내 변경 | `gradle/libs.versions.toml`, 세 locale의 `internal.txt` |
| iOS 버전과 TestFlight 안내 변경 | `iosApp/iosApp/Info.plist`, `fastlane/metadata/ios/what_to_test.txt` |
| GitHub 입력, Environment, secret 전달 변경 | `.github/workflows/release-cd.yml` |
| 배포 순서와 검증 변경 | `fastlane/Fastfile`, 관련 `fastlane/test/*.rb` |
| Play 조회 또는 AAB 검증 변경 | `scripts/*.py`, `scripts/tests/test_play_release_scripts.py` |
| iOS 서명과 App Group 변경 | 두 entitlements, Xcode project, `fastlane/Fastfile`, 관련 signing 테스트 |

파일을 바꾼 뒤에는 관련 테스트를 같은 PR에서 갱신한다. 실제 배포는 PR이 `main`에 병합되고 Release CD 검증이 통과한 뒤 `workflow_dispatch`로 실행한다.
