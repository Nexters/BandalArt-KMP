# 반다라트 Android/iOS
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-blue.svg)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/gradle-8.10-green.svg)](https://gradle.org/)
[![Android Studio](https://img.shields.io/badge/Android%20Studio-2024.3.1%20%28Meerkat%29-green)](https://developer.android.com/studio)
[![minSdkVersion](https://img.shields.io/badge/minSdkVersion-28-red)](https://developer.android.com/distribute/best-practices/develop/target-sdk)
[![targetSdkVersion](https://img.shields.io/badge/targetSdkVersion-35-orange)](https://developer.android.com/distribute/best-practices/develop/target-sdk)
<br/>

부담 없는 만다라트 계획표로 당신의 목표를 더욱 선명하게, 반다라트 [PlayStore](https://play.google.com/store/apps/details?id=com.nexters.bandalart) [AppStore](https://apps.apple.com/kr/app/%EB%B0%98%EB%8B%A4%EB%9D%BC%ED%8A%B8-%EB%B6%80%EB%8B%B4-%EC%97%86%EB%8A%94-%EB%A7%8C%EB%8B%A4%EB%9D%BC%ED%8A%B8-%EA%B3%84%ED%9A%8D%ED%91%9C/id6743101965)
<br/>
![반다라트 그래픽이미지 2](https://github.com/Nexters/BandalArt-Android/assets/51016231/a357f7aa-d086-47de-bbac-d423cdaffdbe)
<br/>

<p align="center">
<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/541f9309-bb9a-4131-be46-ac7df5f74fc1" width="30%"/>
<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/3af26254-8c48-4e53-b79a-9f9764427a60" width="30%"/>
<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/c772cc49-75df-4e2a-94f7-9d6c1f3e1aa3" width="30%"/>
</p>

## Features

|온보딩|메인 목표 입력|메인목표달성|
|:-----:|:-----:|:-----:|
|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/e00aec2e-d9ca-4057-9a8e-af14b8da89bf.gif">|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/e402dfcb-b490-43fa-9dca-ee843920c187.gif">|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/d554c9bd-0067-429f-acee-10d9bf018f6a.gif">|

|반다라트 추가|반다라트 삭제|반다라트 공유|
|:-----:|:-----:|:-----:|
|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/b85bbed8-7c2e-4fa5-9a27-f5e327ae71f6.gif">|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/addaf2a8-31f8-4c1c-8cad-c49c5fd24a48.gif">|<img width="240" src="https://github.com/Nexters/BandalArt-Android/assets/51016231/cd9776e0-0be0-46e3-87ea-7ef88e215054.gif">

## Article
[[KMP] Koin과 Expect/Actual 패턴을 통해 네이티브 기능 구현하기(이미지 저장/공유)](https://velog.io/@mraz3068/KMP-Koin-Expect-Actual-Pattern-For-Native-Image-Handling)

[[KMP] 반다라트 iOS 앱 출시 완료!](https://velog.io/@mraz3068/Bandalart-iOS-App-Deployment-Complete)

[[Circuit] AAC ViewModel -> Circuit Presenter](https://velog.io/@mraz3068/AAC-ViewModel-to-Circuit-Presenter)

[In-app update 적용해보기 with Compose](https://velog.io/@mraz3068/Implementing-In-app-update-with-Compose)

[Custom Splash Screen 만드는 방법(Splash Screen API을 사용하는 경우)](https://velog.io/@mraz3068/How-to-make-Custom-Splash-Screen-with-Splash-Screen-API)

## Development

### Required

- IDE : Android Studio Ladybug
- JDK : Java 17을 실행할 수 있는 JDK
- Kotlin Language : 2.1.10

### Language

- Kotlin
### Architecture
- Modularization Based on [Now in Android](https://github.com/android/nowinandroid) with Clean Architecture

### Libraries

- AndroidX
  - Activity & Activity Compose
  - Core
  - Lifecycle & ViewModel Compose
  - Navigation
  - DataStore
  - ~StartUp~
  - Splash
  - Room
  - In-app update

- Kotlin Libraries (Coroutine, DateTime, Serialization)
- Compose
  - Material3
  - Navigation

- ~Dagger Hilt~ -> Koin
- ~Ktor~
- ~Retrofit~
- ~Timber~ -> [Napier](https://github.com/AAkira/Napier)
- ~Lottie~ -> [compottie](https://github.com/alexzhirkevich/compottie)
- Facebook Shimmer
- ~Firebase(Analytics, Crashlytics)~ -> [Kotlin Firebase(Analytics, Crashlytics)](https://github.com/GitLiveApp/firebase-kotlin-sdk?tab=readme-ov-file)
- [Compose-Stable-Marker](https://github.com/skydoves/compose-stable-marker)
- [Circuit](https://slackhq.github.io/circuit/)
- [uri-kmp](https://github.com/eygraber/uri-kmp)
- [CMPToast](https://github.com/Chaintech-Network/CMPToast)

#### Test 
- JUnit5
- mockK
- robolectric
- Kotest
  
#### Code analysis
- Spotless
- Ktlint
- Detekt

#### Gradle Dependency

- Gradle Version Catalog

check Circuit implementation [here](https://github.com/Nexters/BandalArt-KMP/tree/develop)

## Module
![image](https://github.com/user-attachments/assets/c45b3830-95b2-4b20-9280-7004fc812350)

## Package Structure
```
├── app
│   ├── MainActivity
│   └── Application
├── build-logic
├── core
│   ├── common
│   ├── data
│   ├── database
│   ├── datastore
│   ├── designsystem
│   ├── domain
│   ├── navigation
│   └── ui
├── feature
│   ├── complete
│   ├── home
│   ├── onboarding
│   └── splash
├── gradle
│   └── libs.versions.toml
└── report
    ├── compose-metrics
    └── compose-reports
```

## Testing(Instrumentation, Unit)
Beginner in testing([Studying...](https://github.com/Nexters/BandalArt-KMP/issues/172))

### Room Database(DaoTest)
![image](https://github.com/user-attachments/assets/00cfe300-12f2-4c6d-bf5c-967ba4609985)

### DataStore
![image](https://github.com/user-attachments/assets/b7d4d152-bb8c-46ce-a56a-4d2eff6b969d)
![image](https://github.com/user-attachments/assets/529ad494-735b-415d-bbc2-ef955a437628)

### Repository
![image](https://github.com/user-attachments/assets/fd4b238e-76d7-449f-b108-57603180a482)
![image](https://github.com/user-attachments/assets/27b50fd0-36b3-41e2-81c6-0ae86766ad83)
![image](https://github.com/user-attachments/assets/367a00aa-82de-4b1d-8edd-c97aa60935c7)

### ViewModel
![image](https://github.com/user-attachments/assets/683806a3-10b4-4099-aef4-4cf5bff7fd89)
![image](https://github.com/user-attachments/assets/329eedcb-2a51-4d9d-ab05-081fd1f0b84d)
![image](https://github.com/user-attachments/assets/234690f7-a02e-4eb7-9cb9-01f9e84e08c4)
![image](https://github.com/user-attachments/assets/42a63855-222d-484d-86ae-2278e19ed108)

### Util
![image](https://github.com/user-attachments/assets/c0e7cf5a-e065-40c0-a0d9-9045235fb4ce)
![image](https://github.com/user-attachments/assets/3d591c75-048f-4548-9196-e41994833ace)


## Developer

|이지훈|이석규|
|:-:|:-:|
|<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/e7b05305-b831-4c81-8635-84b478726c55" width=200>|<img src="https://github.com/Nexters/BandalArt-Android/assets/51016231/bbcf9941-5fbb-4f8a-8e8d-8f78db396808" width=200>|
|[@easyhooon](https://github.com/easyhooon)|[@likppi10](https://github.com/likppi10)|
<br/>

