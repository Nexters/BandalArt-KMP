# 반다라트 (BandalArt)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![Gradle](https://img.shields.io/badge/Gradle-9.5.0-02303A.svg?logo=gradle&logoColor=white)](https://gradle.org/)
[![Android](https://img.shields.io/badge/Android-minSdk%2028%20%7C%20targetSdk%2036-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![iOS](https://img.shields.io/badge/iOS-16.6%2B-000000.svg?logo=apple&logoColor=white)](https://developer.apple.com/ios/)
[![Android CI](https://github.com/Nexters/BandalArt-KMP/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/Nexters/BandalArt-KMP/actions/workflows/android-ci.yml)

반다라트는 큰 목표를 작은 실천 항목으로 나누어 관리하는 만다라트 계획 앱입니다. Kotlin Multiplatform (KMP)과 Compose Multiplatform으로 Android와 iOS 앱의 화면, 상태, 도메인 로직을 공유합니다.

[공식 웹사이트](https://bandal-art-fe.vercel.app/) · [Google Play](https://play.google.com/store/apps/details?id=com.nexters.bandalart) · [App Store](https://apps.apple.com/kr/app/%EB%B0%98%EB%8B%A4%EB%9D%BC%ED%8A%B8-%EB%B6%80%EB%8B%B4-%EC%97%86%EB%8A%94-%EB%A7%8C%EB%8B%A4%EB%9D%BC%ED%8A%B8-%EA%B3%84%ED%9A%8D%ED%91%9C/id6743101965) · [개인정보처리방침](https://bandal-art-fe.vercel.app/privacy) · [랜딩 페이지 저장소](https://github.com/easyhooon/BandalArt-FE)

![반다라트 그래픽이미지 2](https://github.com/Nexters/BandalArt-Android/assets/51016231/a357f7aa-d086-47de-bbac-d423cdaffdbe)

<p align="center">
<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/541f9309-bb9a-4131-be46-ac7df5f74fc1" width="30%"/>
<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/3af26254-8c48-4e53-b79a-9f9764427a60" width="30%"/>
<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/c772cc49-75df-4e2a-94f7-9d6c1f3e1aa3" width="30%"/>
</p>

## 주요 기능

- 목표, 하위 목표, 실천 항목으로 구성된 만다라트 계획 작성과 달성 관리
- 취업 준비, 운동 습관, 공부 계획, 재테크 습관, 여행 준비 템플릿 제공
- 여러 반다라트 생성, 전환, 삭제와 이미지 저장·공유
- 마감일 당일 오전 9시부터 전달되는 Android·iOS 로컬 알림
- 시스템 설정, 라이트 모드, 다크 모드와 목표 완료 시 진동 피드백
- 한국어, 영어, 일본어 인터페이스
- Android·iOS 하단 배너와 추가 반다라트 생성을 위한 보상형 광고

## 앱 화면

|온보딩|메인 목표 입력|메인목표달성|
|:-----:|:-----:|:-----:|
|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/e00aec2e-d9ca-4057-9a8e-af14b8da89bf.gif">|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/e402dfcb-b490-43fa-9dca-ee843920c187.gif">|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/d554c9bd-0067-429f-acee-10d9bf018f6a.gif">|

|반다라트 추가|반다라트 삭제|반다라트 공유|
|:-----:|:-----:|:-----:|
|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/b85bbed8-7c2e-4fa5-9a27-f5e327ae71f6.gif">|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/addaf2a8-31f8-4c1c-8cad-c49c5fd24a48.gif">|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/cd9776e0-0be0-46e3-87ea-7ef88e215054.gif">

## 프로젝트 구조

공통 사용자 인터페이스(UI)와 비즈니스 로직은 `commonMain`에 두고, Android와 iOS 호스트 앱은 광고, 알림, 저장·공유처럼 운영체제 API가 필요한 기능을 연결합니다. 화면 상태와 내비게이션은 Circuit Presenter가 관리하며 Metro가 공통 및 플랫폼 의존성을 구성합니다.

![image](https://github.com/user-attachments/assets/c45b3830-95b2-4b20-9280-7004fc812350)

```text
├── androidApp          # Android 애플리케이션과 플랫폼 연동
├── composeApp          # 공통 앱 조립, Android·iOS 플랫폼 구현
├── iosApp              # Swift 호스트 앱과 iOS 네이티브 브리지
├── baselineprofile     # Android Baseline Profile 생성
├── build-logic         # Gradle convention plugin
├── core
│   ├── common          # 공통 유틸리티와 플랫폼 계약
│   ├── data            # Repository 구현
│   ├── database        # Room KMP 데이터베이스
│   ├── datastore       # 설정과 앱 상태 저장
│   ├── designsystem    # 테마, Pretendard, 공통 컴포넌트와 리소스
│   ├── domain          # 도메인 모델과 Repository 계약
│   ├── navigation      # Circuit Screen과 내비게이션 계약
│   └── ui              # 공통 UI 도구
├── feature
│   ├── complete        # 목표 달성 화면
│   ├── home            # 계획 작성, 템플릿, 설정과 광고 흐름
│   ├── onboarding      # 온보딩
│   └── splash          # 초기 진입과 업데이트 정책
├── docs                # 아키텍처, 기능, 배포와 법률 문서
├── fastlane            # Android·iOS 배포 자동화
└── gradle              # Version Catalog와 Gradle Wrapper
```

자세한 설계 결정과 작업 기록은 [프로젝트 문서 인덱스](docs/README.md)에서 확인할 수 있습니다.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 언어·빌드 | Kotlin 2.4.10, Swift, Gradle 9.5.0, Android Gradle Plugin 9.3.0, Java Development Kit (JDK) 21 |
| UI | Compose Multiplatform 1.10.3, Material 3, Pretendard, Compottie, Coil 3, Landscapist |
| 상태·내비게이션 | [Circuit 0.35.1](https://slackhq.github.io/circuit/) |
| 의존성 주입 | [Metro 1.1.1](https://zacsweers.github.io/metro/) |
| 데이터 | Room KMP, SQLite, DataStore |
| 비동기·직렬화 | Kotlin Coroutines, Kotlinx Serialization, Kotlinx DateTime, Immutable Collections |
| 서비스 | Firebase Analytics·Crashlytics·Remote Config, Google AdMob |
| Android | WorkManager, In-app Updates, Baseline Profiles, R8 |
| iOS | Swift 호스트 앱, Swift Package Manager, UserNotifications |
| 로깅·도구 | Napier, Ding, uri-kmp, CMPToast, Jindong |
| 테스트 | JUnit 5, Circuit Test, Turbine, MockK, Robolectric, Kotest |
| 코드 품질 | Spotless, ktlint, Detekt |

정확한 의존성 버전은 [Gradle Version Catalog](gradle/libs.versions.toml)를 기준으로 관리합니다. 오픈소스 고지와 라이선스는 [Third-party notices](THIRD_PARTY_NOTICES.md)에서 확인할 수 있습니다.

## 개발 환경

프로젝트는 Gradle 실행에 JDK 21을 사용하고 JVM 17 바이트코드를 생성합니다. Android는 `minSdk 28`, `targetSdk 36`, `compileSdk 37`이며 iOS 배포 대상은 16.6 이상입니다.

필요한 도구는 다음과 같습니다:

- Android Gradle Plugin 9.3.0을 지원하는 Android Studio
- JDK 21과 Android SDK
- iOS 앱을 빌드할 경우 macOS와 Xcode
- Android 빌드에 필요한 로컬 `keystore.properties`와 배포용 비공개 설정

대표 검증 명령은 다음과 같습니다:

```bash
./gradlew spotlessCheck detekt
./gradlew allTests :androidApp:testDebugUnitTest
./gradlew :androidApp:lintDebug :androidApp:assembleDebug
```

iOS 호스트 앱은 Xcode의 `iosApp` scheme으로 실행합니다. 테스트 source set 선택과 Circuit Presenter 테스트 규칙은 [KMP 테스트 가이드](docs/architecture/kmp/KMP_TESTING_GUIDE.md)를 따릅니다.

## 테스트 리포트

### Room database

![image](https://github.com/user-attachments/assets/00cfe300-12f2-4c6d-bf5c-967ba4609985)

### DataStore

![image](https://github.com/user-attachments/assets/b7d4d152-bb8c-46ce-a56a-4d2eff6b969d)
![image](https://github.com/user-attachments/assets/529ad494-735b-415d-bbc2-ef955a437628)

### Repository

![image](https://github.com/user-attachments/assets/fd4b238e-76d7-449f-b108-57603180a482)
![image](https://github.com/user-attachments/assets/27b50fd0-36b3-41e2-81c6-0ae86766ad83)
![image](https://github.com/user-attachments/assets/367a00aa-82de-4b1d-8edd-c97aa60935c7)

### 상태 관리

![image](https://github.com/user-attachments/assets/683806a3-10b4-4099-aef4-4cf5bff7fd89)
![image](https://github.com/user-attachments/assets/329eedcb-2a51-4d9d-ab05-081fd1f0b84d)
![image](https://github.com/user-attachments/assets/234690f7-a02e-4eb7-9cb9-01f9e84e08c4)
![image](https://github.com/user-attachments/assets/42a63855-222d-484d-86ae-2278e19ed108)

### 공통 유틸리티

![image](https://github.com/user-attachments/assets/c0e7cf5a-e065-40c0-a0d9-9045235fb4ce)
![image](https://github.com/user-attachments/assets/3d591c75-048f-4548-9196-e41994833ace)

## 관련 글

- [[KMP] Koin과 Expect/Actual 패턴으로 네이티브 이미지 저장·공유 구현하기](https://velog.io/@mraz3068/KMP-Koin-Expect-Actual-Pattern-For-Native-Image-Handling)
- [[KMP] 반다라트 iOS 앱 출시 완료](https://velog.io/@mraz3068/Bandalart-iOS-App-Deployment-Complete)
- [[Circuit] AAC ViewModel에서 Circuit Presenter로 전환하기](https://velog.io/@mraz3068/AAC-ViewModel-to-Circuit-Presenter)
- [Compose에서 In-app Update 적용하기](https://velog.io/@mraz3068/Implementing-In-app-update-with-Compose)
- [Splash Screen API로 Custom Splash Screen 만들기](https://velog.io/@mraz3068/How-to-make-Custom-Splash-Screen-with-Splash-Screen-API)

## 개발자

|이지훈|이석규|
|:-:|:-:|
|<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/e7b05305-b831-4c81-8635-84b478726c55" width=200>|<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/bbcf9941-5fbb-4f8a-8e8d-8f78db396808" width=200>|
|[@easyhooon](https://github.com/easyhooon)|[@likppi10](https://github.com/likppi10)|
