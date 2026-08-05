---
name: deploy-android-firebase
description: BandalArt Android APK를 Firebase App Distribution에 배포한다. deploy-android-firebase 또는 Android Firebase 테스트 배포 요청에 사용하며 iOS 배포는 다루지 않는다.
---

# Deploy Android Firebase

BandalArt Android APK를 Firebase App Distribution에 배포한다. 이 skill의 범위는 Android debug/release APK뿐이며 iOS, TestFlight, App Store와 공통 KMP release는 포함하지 않는다.

## 입력

- `--debug` 또는 `--release` 중 하나를 필수로 받는다.
- `--groups <group1,group2>`는 선택 사항이다. 없으면 저장소에 설정된 기본 tester group을 사용한다.
- `--notes-file <path>`는 선택 사항이다. 없으면 저장소에 설정된 Android Firebase release notes 경로를 사용한다.

## 절차

1. 현재 branch와 `origin`의 동기화 상태를 확인한다. dirty/unpushed 변경이 있으면 배포 대상을 특정할 수 없으므로 중단한다.
2. Android Firebase App Distribution 설정을 확인한다.
   - Android application module과 package name을 찾는다.
   - Firebase App Distribution Gradle plugin 또는 저장소가 채택한 동등한 Android 업로드 명령을 찾는다.
   - build type에 대응하는 Firebase app id, tester group, release notes 경로와 인증 방식을 확인한다.
   - 실제 업로드 task/명령이 없으면 필요한 설정을 목록으로 보고하고 중단한다. Firebase CLI나 임의 API 호출로 우회하지 않는다.
3. credential은 존재 여부와 Git ignore 상태만 확인한다. 파일 내용, token, service account JSON과 webhook 값을 출력하지 않는다.
4. release notes가 없거나 비어 있으면 최근 Android 변경을 바탕으로 초안을 제안하고 사용자 확인 전에는 쓰거나 배포하지 않는다.
5. 아래 정보를 보여주고 실제 업로드 직전에 명시적 확인을 받는다.
   - Android build type
   - package name과 versionName/versionCode
   - tester group
   - release notes
   - 실행할 Gradle task 또는 저장소의 Android 업로드 명령
6. 확인 후 해당 APK build와 Firebase App Distribution 업로드를 실행한다. 배포 산출물 생성이 목적이므로 이 workflow에서는 Android build를 실행할 수 있다.
7. build나 upload가 실패하면 즉시 중단하고 secret을 가린 실패 단계와 원인을 보고한다.
8. 성공하면 Android APK, 앱 버전, tester group과 Firebase 배포 결과를 보고한다.

## 제약

- Android APK 외 산출물을 배포하지 않는다.
- Crashlytics 설정만 존재하는 것을 App Distribution 설정으로 간주하지 않는다.
- 실패했던 Fastlane 설정을 읽거나 fallback으로 사용하지 않는다.
- 환경별 app id나 credential 경로를 추측하지 않는다.
- Discord 등 외부 알림은 저장소에 검증된 전용 script와 credential이 있고 사용자가 요청한 경우에만 실행한다.
