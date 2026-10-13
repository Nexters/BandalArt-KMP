# iPhone Duo 시뮬레이터 테스트 가이드

- 작성일: 2026-10-13
- 관련 이슈: [#424 iPhone Duo 대응](https://github.com/Nexters/BandalArt-KMP/issues/424), [#434 넓은 화면 표 레이아웃](https://github.com/Nexters/BandalArt-KMP/issues/434)

iPhone Duo 시뮬레이터로 화면을 확인하거나 스크린샷을 찍을 때 지키는 규칙이다.

## 규칙

| 규칙 | 이유 |
|---|---|
| Device Hub는 **Xcode 27.1 번들 안의 것**을 연다: `open /Applications/Xcode-27.1-beta.app/Contents/Applications/DeviceHub.app` | Xcode 27.0의 Device Hub(Spotlight·`Xcode.app` 기본)는 Duo 화면은 보여주지만 접기·펼치기 컨트롤이 없다. 27.1 Device Hub는 하단 툴바에 자세 버튼(접힘·반 펼침·펼침)이 있고 Option을 누르면 힌지 각도 슬라이더가 나온다 |
| 펼치기·접기는 Device Hub 하단 툴바의 자세 버튼으로만 바꾼다 | `simctl`에는 자세(힌지) 명령이 없다 |
| Duo 레이아웃 확인용 빌드는 **27.1 SDK**로 한다: `DEVELOPER_DIR=/Applications/Xcode-27.1-beta.app/Contents/Developer xcodebuild ...` | 27.0 SDK로 빌드하면 시스템이 오른쪽 상태 레일을 검은 바탕으로 차지해 앱 창이 좁아진다. 앱 `Info.plist`의 `DTSDKName`으로 확인한다 |
| 기본 `xcode-select`는 바꾸지 않는다 | 다른 프로젝트·세션이 같은 Xcode를 쓴다. 27.1은 `DEVELOPER_DIR`로 해당 명령에만 적용한다 |
| 시뮬레이터를 삭제·초기화하기 전에 설치된 앱을 먼저 확인하고, 반다라트용이 아니면 건드리지 않는다 | 같은 이름의 Duo 시뮬레이터를 다른 프로젝트가 쓰고 있을 수 있다. 2026-10-13 다른 프로젝트 앱이 설치된 Duo를 확인 없이 삭제한 적이 있다 |
| 화면 캡처는 `scripts/ios_sim_screenshot_data.sh shot <inner\|outer>`를 쓴다 | 안쪽·바깥쪽 디스플레이를 면적으로 구분해 고른다. 접힌 상태에서 안쪽 화면은 검게 찍힌다 |

## 확인 순서

1. Duo 시뮬레이터를 부팅한다: `xcrun simctl boot <udid>`
2. 27.1 Device Hub를 열고 Duo를 선택한다.
3. 27.1 SDK로 빌드해 설치·실행한다. Debug 빌드는 첫 화면까지 1~2분 걸릴 수 있다.
4. 하단 툴바 자세 버튼으로 접힘·펼침을 바꿔가며 `shot outer`, `shot inner`로 찍는다.
5. 빌드 SDK(`DTSDKName`)와 자세를 스크린샷 설명에 함께 남긴다.

## 참고

- [Apple Developer Forums: iPhone Duo Simulator - Fold](https://developer.apple.com/forums/thread/847556)
- [Test every pose in Xcode's Device Hub](https://iphoneduosupport.com/checklist/device-hub-pose-testing/)
- [Xcode 27.1 Beta: Your App in the iPhone Duo Simulator](https://blakecrosley.com/blog/xcode-27-1-beta-iphone-duo-simulator)
- SDK별 비교 스크린샷: [#424 코멘트](https://github.com/Nexters/BandalArt-KMP/issues/424#issuecomment-6135936092)
