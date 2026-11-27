# Fluent Emoji 카탈로그 전체 포함 전략

## 문제

번들 이모지 300개에 🐶(dog face), 🐕(dog), 😀, 😂, 👍, ⚽ 같은 대표 이모지가 없다. 반면 🪳(cockroach), 🅰️·🆎·🅱️(혈액형 버튼), 🏴‍☠️(pirate flag)는 들어 있다.

## 원인

`tools/fluent-emoji/select-catalog.mjs`는 그룹별 quota 안에서 spike 큐레이션(+10,000점)과 `goalTerms` 부분 문자열 매칭(용어당 10점)으로 정렬하고, 동점은 영어 CLDR 이름 알파벳 순으로 채웠다.

| 그룹 | 0점(알파벳 순) 채움 | 결과 |
| --- | --- | --- |
| Animals & Nature | 27 / 35 | badger…cow에서 끝나 dog 이후가 모두 빠짐 |
| Activities | 23 / 45 | ⚽ soccer ball, 🎾 tennis 등 빠짐 |
| Symbols | 6 / 25 | 🅰️·🆎·🅱️·🔙·🆑·♈ 포함 |
| Flags | 4 / 5 | 🏴‍☠️·🏳️‍🌈 포함, 🚩 빠짐 |
| People & Body | 2 / 30 | 🤶 포함, 👍·👏·🙏 빠짐 |

Smileys & Emotion은 `heart` 등 부분 문자열 매칭 때문에 30개가 대부분 하트 계열이었다.

필수 목록과 Unicode 순서 동점 정렬로 300개 안에서 95개를 교체하는 안을 먼저 검토했으나, 어떤 300개를 고르든 빠지는 대표 이모지가 생긴다. 사용자는 상한을 없애고 후보 전부를 넣은 뒤 크기를 실측해 판단하기로 결정했다.

## 결정

- 지원 그룹 9개의 Fluent 후보를 모두 카탈로그에 넣는다. 현재 1,428개다.
- 기존 후보 조건은 유지한다. 성별 특정 man/woman 시퀀스 제외, 컬러 SVG 필수, unicode 중복 제거.
- 표시 순서는 Unicode `emoji-test.txt` 순서다. 파일은 Emoji 16.0 버전으로 고정해 내려받고 sha256을 검증한다. 후보가 이 파일에 없으면 실패한다.
- 그룹 quota, `goalTerms` 점수, 필수 목록은 전체 포함에서 의미가 없으므로 제거한다. spike 20개는 한국어 별칭(`koreanAliases`) 출처로만 남긴다.
- 개수 검사(생성 스크립트, 동기화 스크립트, 테스트)는 300 고정값 대신 선정 결과 개수를 기준으로 한다.
- 카탈로그 데이터는 Kotlin 코드가 아니라 JSON 리소스로 둔다(아래 "카탈로그 데이터를 JSON 리소스로 옮기기").

## 크기 예산

- release AAB, iOS Release `.app` 디렉터리, iOS `.app` zip 세 값의 증가량을 잰다.
- 5MB 예산은 셋 중 가장 큰 증가량에 적용한다. 넘으면 멈추고 600~1,000개 축소안을 제안한다.
- 기준선은 미커밋 300개(95개 교체) 상태다. HEAD의 300개와 WebP 합계가 약 33KB 다르다.

실측 결과는 `FLUENT_EMOJI_RESOURCE_SPIKE.md`에 기록한다.

## 카탈로그 데이터를 JSON 리소스로 옮기기

Kotlin 코드로 생성한 1,428개 카탈로그(200개씩 파일, 25개씩 함수로 분할)는 iOS 실행 바이너리를 4,053,736B 늘렸고, iOS 설치 크기 증가(파일 바이트 합 +7,468,580B)가 5MB 예산을 넘었다. Kotlin/Native Release 링크도 느려졌다(측정은 `FLUENT_EMOJI_RESOURCE_SPIKE.md`). 사용자는 카탈로그 데이터를 JSON 리소스로 옮기기로 결정했다.

### resource key는 규칙으로 계산

1,428개 모두 `resourceKey`가 `fluent_` + 코드포인트 4자리 이상 소문자 16진수를 `_`로 이은 값과 같다. 그래서 `fluentEmojiResourceKey(unicode)`로 계산하고 JSON에 저장하지 않는다. 이미지 표시는 아래 "WebP를 drawable에서 files로 옮기기"를 따른다.

### JSON으로 옮기는 것: 피커 데이터

- 위치: `core/designsystem/src/commonMain/composeResources/files/fluent_emoji_catalog.json`. 기존 `Res`의 `files/`를 쓰고 `Res.readBytes`로 읽는다.
- 형식: 항목 배열, 키는 짧게(`u` unicode, `c` 카테고리 enum 이름, `n` CLDR 영어 이름, `k` 영어 키워드, `a` 한국어 별칭(없으면 생략), `kn`·`kk` 한국어 이름·키워드, `jn`·`jk` 일본어 이름·키워드). resource key는 규칙으로 계산하므로 저장하지 않는다. 크기는 415,812B다. 이름과 같은 키워드를 빼면 약 50KB 줄지만 일부 항목의 키워드 목록이 비어 데이터 의미가 바뀌므로 빼지 않는다.
- 생성: `generate-runtime-catalog.mjs`가 Kotlin 대신 이 JSON을 쓴다. `measure-catalog.sh --sync-resources`가 JSON을 동기화한다. 이전 `FluentEmojiCatalog.generated.kt`는 삭제한다.
- 파싱: `core:ui`에 `bandalart.kotlin.serialization`을 적용해 kotlinx.serialization으로 읽는다. 파서는 JSON 문자열을 받는 순수 함수로 둔다.
- 로딩: `FluentEmojiCatalog.items()`는 suspend 함수이고, 읽기와 파싱은 `Dispatchers.Default`에서 하며 처음 성공한 결과를 메모리에 캐시한다.
- 미리 로드: `BandalartApp`이 첫 화면 구성 뒤 `LaunchedEffect`에서 `preloadFluentEmojiCatalog()`를 부른다. 실패는 무시하고 캐시하지 않으므로 피커를 열 때 다시 읽는다(사용자 결정).
- 피커: 이미 읽은 카탈로그가 있으면 첫 프레임부터 그 값으로 그린다. 아직 읽는 중이면 카탈로그 값은 `null`이라 빈 검색 결과와 구분되고, 그리드는 높이가 고정된 채 비어 있다(사용자 결정). 이때 최근 사용 탭은 카탈로그를 읽은 뒤 나타난다.
- 검색·필터: `filterFluentEmojiItems`와 `fluentEmojiPickerItems`는 항목 목록을 인자로 받는다. 동작은 바꾸지 않는다.
- 테스트: androidHostTest는 JSON 파일을 직접 읽어 파서로 만든 목록으로 기존 검증을 그대로 돌린다. 리소스 키 규칙도 검증한다.

## WebP를 drawable에서 files로 옮기기

JSON 전환 뒤에도 iOS 설치 크기 증가가 +5,734,096B로 5MB 예산을 넘었다. 남은 실행 바이너리 증가 +1,903,496B는 Compose resource가 `drawable/` 파일마다 만드는 `Res.drawable.fluent_*` 접근자 코드다. 사용자는 이미지를 모두 앱에 넣은 채 WebP를 `files/`로 옮겨 접근자 코드를 없애기로 결정했다.

- 위치: `core/designsystem/src/commonMain/composeResources/files/fluent_emoji/fluent_<code points>.webp`. 파일 이름과 resource key 규칙은 그대로다. `files/`는 접근자 코드를 만들지 않는다.
- 렌더링: `BandalartEmoji`는 `core:ui`에 이미 있는 Coil의 `AsyncImage`로 `Res.getUri("files/fluent_emoji/<key>.webp")`를 그린다. 디코딩은 메인 스레드 밖에서 하고, Coil 메모리 캐시가 같은 이모지를 다시 읽지 않게 한다. 이미지는 앱 안에 있으므로 목표 아이콘은 오프라인에서도 보인다.
- 대체 표시: 미리 로드한 카탈로그가 있으면 카탈로그 밖 값은 이미지를 시도하지 않고 바로 `Text`로 그린다. 카탈로그를 읽기 전에는 이미지 URI를 먼저 구한다. Android의 `Res.getUri`는 파일이 없으면 예외를 던지므로 이 호출을 감싸고, 실패하거나 Coil 로드가 실패하면 `Text`로 바꾼다.
- 바뀌는 동작: 런타임 화면 확인은 Android 17 에뮬레이터에서 adb 입력이 앱에 전달되지 않아 하지 못했다. APK의 `assets/composeResources/.../files/fluent_emoji/`에 WebP 1,428개가 들어간 것은 확인했다. drawable은 동기로 그렸지만 이제 처음 그릴 때 이미지가 비동기로 나타난다. 같은 크기의 빈 영역이 잠깐 보인 뒤 채워진다(사용자가 받아들인 trade-off).
- 동기 API `FluentEmojiCatalog.resourceKeyFor`는 `Res.allDrawableResources`에 의존했으므로 없앤다. 존재 여부는 카탈로그와 이미지 로드 결과로 판단한다.
- 동기화: `measure-catalog.sh --sync-resources`는 WebP를 `files/fluent_emoji/`로 복사한다.
- 테스트: 카탈로그 항목마다 `files/fluent_emoji/`에 WebP가 있고, 그 폴더의 WebP 수가 카탈로그 수와 같은지 파일 시스템으로 확인한다.

## 영향

- 기존 300개는 모두 그대로 포함되므로 저장된 목표 아이콘과 최근 사용 이모지가 사라지지 않는다.
- 카탈로그에 없는 값(성별 특정 시퀀스 등)은 `BandalartEmoji`가 시스템 이모지 `Text`로 표시하고, 최근 사용 탭에서는 보이지 않는다.
- 전체 보기 첫 화면이 Unicode 순서(😀부터)가 되어, 기존에 앞쪽에 있던 목표 관련 spike 이모지(🎯🏆 등)는 뒤로 간다.

## 산출물 재생성

`./tools/fluent-emoji/measure-catalog.sh --sync-resources`로 `measurement/catalog-candidate.json`, `measurement-report.json`, WebP 리소스, `files/fluent_emoji_catalog.json`을 다시 만든다. 측정 구간은 300·600·1,000·전체이며, Unicode 순서 앞부분이라 축소안의 크기가 아니라 payload 증가 추이를 보는 값이다.

HEAD에 이미 있는 WebP는 로컬 변환기 차이로 몇 바이트씩 달라질 수 있다. 이미지 변경이 아니므로 HEAD 파일로 되돌리고 새 항목만 추가한다.

## 검증 기준

- 🐶가 카탈로그에 있고 "강아지", "犬", "dog" 검색으로 걸린다.
- 리소스·카탈로그 개수 정합 테스트를 포함해 `:core:ui:testAndroidHostTest`가 통과한다.
- `:core:ui:compileAndroidMain`, `:core:ui:compileKotlinIosSimulatorArm64`, `detekt`, `spotlessCheck`가 통과한다.
- release AAB와 iOS Release 앱이 빌드되고 증가량이 5MB 이하다.
