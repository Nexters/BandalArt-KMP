# AdMob 광고 ID와 운영 검증 가이드

## 문서 목적

Android와 iOS 빌드에 Google 테스트 광고 단위 ID와 BandalArt 운영 광고 단위 ID 중 무엇이 들어가는지 정의한다. 배포 채널, 스토어 테스터 계정과 AdMob 테스트 기기를 혼동하지 않고 운영 광고를 안전하게 검증하기 위한 현재 기준이다.

관련 이슈는 [#354](https://github.com/Nexters/BandalArt-KMP/issues/354), [#363](https://github.com/Nexters/BandalArt-KMP/issues/363)이다.

## 광고 ID 선택표

스토어의 테스트 트랙 여부가 광고 ID를 결정하지 않는다. Android는 build type, iOS는 build configuration과 `ios_ads_mode`가 광고 단위 ID를 결정한다.

| 플랫폼 | 빌드·배포 경로 | 광고 단위 ID | 용도와 주의사항 |
| --- | --- | --- | --- |
| Android | `debug` | Google 공식 테스트 ID | 로컬 개발과 기능 테스트용이다. 광고를 눌러야 하는 테스트는 이 빌드를 사용한다. |
| Android | 로컬 `release` | BandalArt 운영 ID | 운영과 같은 요청을 확인하는 빌드다. 실제 광고를 클릭하지 않는다. |
| Android | Play Internal Testing | BandalArt 운영 ID | Internal도 `release` AAB를 사용한다. 스토어 내부 테스터 여부는 광고 모드를 바꾸지 않는다. |
| Android | Play Closed/Open/Production | BandalArt 운영 ID | Internal과 같은 release 광고 정책을 사용한다. |
| iOS | Xcode `Debug` | Google 공식 테스트 ID | `DEBUG` 컴파일 조건으로 테스트 ID를 선택한다. Simulator는 별도로도 자동 테스트 기기다. |
| iOS | 로컬 `Release` | BandalArt 운영 ID | `BANDALART_TEST_ADS` 조건을 추가하지 않으면 운영 ID를 선택한다. |
| iOS | TestFlight, `ios_ads_mode=test` | Google 공식 테스트 ID | 광고 클릭·보상 등 기능 검증용이다. `BANDALART_TEST_ADS` 조건을 추가해 아카이브한다. |
| iOS | TestFlight, `ios_ads_mode=production` | BandalArt 운영 ID | App Store 릴리스 후보 검증용이다. 실제 광고를 클릭하지 않는다. |
| iOS | App Store | BandalArt 운영 ID | `production` 모드로 올린 TestFlight 빌드만 심사·출시 대상으로 선택한다. |

현재 Release CD의 `ios_ads_mode` 기본값은 `production`이다. 테스트 광고가 필요한 TestFlight를 만들 때만 dispatch 입력을 `test`로 명시한다. 업로드된 바이너리의 광고 모드는 App Store Connect에서 바꿀 수 없다.

## 설정의 기준 파일

문서에 광고 ID 문자열을 복제하지 않고 다음 파일을 단일 기준으로 사용한다.

| 플랫폼 | 기준 파일 | 보장하는 계약 |
| --- | --- | --- |
| Android | `androidApp/build.gradle.kts` | Debug의 Google 테스트 App·광고 단위 ID와 release의 BandalArt 운영 App·광고 단위 ID를 정의한다. |
| Android | `scripts/validate_play_aab.py` | Play에 올릴 AAB에서 프로젝트가 현재 사용하는 Google 테스트 Fixed Banner·Rewarded ID를 거부하고 운영 홈 배너·반다라트 생성 Rewarded·클라우드 백업 Rewarded ID를 모두 요구한다. |
| iOS | `iosApp/iosApp/IosAdsBridgeImpl.swift` | `DEBUG` 또는 `BANDALART_TEST_ADS` 조건에 따라 Banner와 목적별 Rewarded 광고 단위 ID를 선택한다. |
| iOS | `fastlane/lib/ios_ads_mode.rb` | `ios_ads_mode=test`일 때만 `BANDALART_TEST_ADS` 조건을 Release archive에 추가한다. |
| iOS | `.github/workflows/release-cd.yml` | 선택한 `ios_ads_mode`를 Fastlane에 전달한다. 현재 기본값은 `production`이다. |

Google 테스트 광고 단위 ID는 여러 테스트 목적에서 재사용할 수 있지만 운영 ID는 플랫폼과 광고 위치·목적별로 구분한다. Android App ID도 build type에 따라 테스트와 운영으로 나뉜다. iOS의 `GADApplicationIdentifier`는 모든 구성에서 BandalArt 운영 App ID를 사용하며, 실제 테스트·운영 creative 선택은 광고 단위 ID와 테스트 기기 설정으로 구분한다.

## 운영 ID인데 `Test Ad`가 보이는 경우

광고 단위 ID와 기기의 테스트 모드는 독립적이다.

| 광고 단위 ID | 기기 상태 | 예상 결과 |
| --- | --- | --- |
| Google 테스트 ID | 모든 기기 | 테스트 광고 |
| BandalArt 운영 ID | 일반 실기기 | 실제 광고 또는 광고 재고에 따른 no-fill |
| BandalArt 운영 ID | AdMob에 등록한 테스트 기기 | 테스트 모드 광고 |
| BandalArt 운영 ID | Android Emulator 또는 iOS Simulator | 자동 테스트 기기로 처리된 광고 |

Play Internal 테스터나 TestFlight 테스터에서 계정을 제거해도 AdMob 테스트 기기 등록은 해제되지 않는다. 운영 빌드에서 `Test Ad`가 보이면 설치 버전과 산출물의 광고 ID를 확인한 다음 AdMob 콘솔의 테스트 기기 목록을 별도로 확인한다.

## 실제 광고를 클릭하면 안 되는 이유

Google은 게시자가 자신의 실제 광고를 클릭해 만든 클릭이나 노출, 한 명 이상의 사용자가 만든 반복 클릭·노출을 무효 트래픽의 예로 든다. 개발자·테스터의 의도가 단순 검증이더라도 운영 광고 클릭은 광고주의 비용과 게시자의 수입을 인위적으로 만들 수 있다.

무효 트래픽은 다음 결과로 이어질 수 있다.

- 예상 수입에서 무효 활동 금액 공제
- 광고 게재 제한 또는 중지
- AdMob 계정 정지 또는 사용 중지

따라서 운영 광고는 절대 직접 클릭하지 않고 개발자·테스터 기기에서 반복 노출도 만들지 않는다. 광고 클릭, 보상 완료, dismiss와 callback 검증이 필요하면 Google 테스트 ID를 쓰는 Debug 또는 `ios_ads_mode=test` 빌드, 혹은 AdMob 테스트 기기를 사용한다.

## 안전한 검증 절차

### 개발·기능 검증

1. Android Debug 또는 iOS Debug/TestFlight `test` 모드를 사용한다.
2. 운영 광고 단위 ID를 함께 검증해야 하면 실기기를 AdMob 테스트 기기로 등록한다.
3. 배너 클릭, Rewarded 보상·닫기·실패 흐름은 테스트 광고에서만 실행한다.

### 스토어 운영 후보 검증

1. 버전과 build 번호를 기록하고 의도한 release/production 빌드인지 확인한다.
2. Android는 AAB validator로 현재 정의된 운영 광고 단위 ID 3개가 있고 프로젝트가 사용하는 Google 테스트 Fixed Banner·Rewarded ID가 없는지 검사한다.
3. 개발자·테스터 실기기는 AdMob 테스트 기기로 등록한 상태에서 요청과 UI만 확인한다. 운영 ID가 들어 있어도 이 기기에는 테스트 모드 광고가 표시되는 것이 정상이다.
4. 실제 운영 광고 게재와 수익은 개발자 기기에서 인위적으로 만들지 않고, 배포 뒤 자연 사용자 트래픽과 AdMob 보고서 반영으로 확인한다.

Android Play 배포는 `scripts/validate_play_aab.py`가 현재 프로젝트에서 사용하는 테스트 광고 단위 ID 유입과 운영 광고 단위 ID 누락을 업로드 전에 차단한다.

iOS는 현재 IPA 실행 파일 안의 광고 단위 ID를 직접 검사하는 validator가 없다. Release CD 로그의 `IOS_ADS_MODE`와 TestFlight 테스트 메모는 선택한 빌드 의도를 기록하지만 실제 바이너리 내용을 증명하지는 않는다. App Store에는 `production`으로 아카이브한 빌드만 선택하는 수동 운영 정책을 따르고, IPA 광고 단위 ID 검증 자동화는 별도 작업으로 보강한다.

## 공식 참고

- [Google AdMob 무효 트래픽](https://support.google.com/admob/answer/3342054?hl=ko)
- [Android 테스트 광고 사용](https://developers.google.com/admob/android/test-ads)
- [iOS 테스트 광고 사용](https://developers.google.com/admob/ios/test-ads)
