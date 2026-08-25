# Google Play 스크린샷 업로드 자동화 전략

## 배경

Google Play 스토어 스크린샷은 저장소에서 산출물을 관리해도 Play Console에서 다시 수동 업로드해야 했다. 이 작업은 외부 업로드 서비스나 별도 배포 도구를 추가하지 않고 [Google Play Developer Publishing API v3](https://developers.google.com/android-publisher)를 직접 호출해 스크린샷 교체를 자동화한다.

Google의 이미지 API는 app edit 안에서 locale과 image type별 이미지를 삭제·조회·업로드할 수 있다. edit은 commit 전까지 공개 listing에 반영되지 않으므로, 모든 대상 이미지를 한 edit에서 교체하고 검증한 뒤 한 번만 commit한다.

관련 이슈: [#371](https://github.com/Nexters/BandalArt-KMP/issues/371)

## 범위

- Google Play용 최종 이미지 산출물의 저장소 경로와 이름 규약
- credential 없이 실행되는 로컬 파일 검증과 dry-run
- 기존 Play 서비스 계정으로 공식 Android Publisher API를 호출하는 Python 업로더
- `main`에서만 수동 실행되는 GitHub Actions workflow
- 업로더의 파일 검증과 edit 수명주기를 보호하는 단위 테스트

다음은 포함하지 않는다.

- 스크린샷 캡처·디자인·렌더링 도구
- App Store Connect 이미지 업로드
- AAB build, Play track release 또는 listing text 변경

## 자산 계약

최종 산출물은 아래 경로에 둔다.

```text
store-assets/screenshots/publish/google-play/
  ko-KR/
    phoneScreenshots/
      01-hero.png
      02-feature.png
    sevenInchScreenshots/
    tenInchScreenshots/
    featureGraphic/
      01-feature-graphic.png
```

- locale 디렉터리는 Play listing과 같은 BCP-47 tag를 사용한다.
- 지원 image type은 `phoneScreenshots`, `sevenInchScreenshots`, `tenInchScreenshots`, `featureGraphic`으로 제한한다.
- 파일명은 `01-`, `02-`처럼 연속된 두 자리 순번으로 시작한다. API 업로드 순서가 스토어 노출 순서다.
- screenshot은 JPEG 또는 alpha가 없는 24-bit PNG, 각 변 320~3840px, 긴 변이 짧은 변의 두 배 이하여야 하며 type별 1~8장을 허용한다. 스토어 listing 전체의 최소 2장 조건은 기존 자산까지 포함한 API edit validation에서 최종 확인한다.
- feature graphic은 JPEG 또는 alpha가 없는 24-bit PNG 한 장이며 1024×500px이다.
- `source/`와 `references/`는 입력·참고 자산이므로 업로드 탐색 대상이 아니다.

규격은 [Play Console preview asset 요구사항](https://support.google.com/googleplay/android-developer/answer/9866151)과 [API image type](https://developers.google.com/android-publisher/api-ref/rest/v3/AppImageType)을 따른다.

## 실행 모델

### 로컬 검증

기본 실행은 credential과 네트워크를 사용하지 않는다.

```bash
python3 scripts/upload_play_screenshots.py
```

업로더는 대상 locale/image type, 정렬된 파일 목록, 크기와 형식을 출력하고 오류가 있으면 비정상 종료한다. 실제 Play 변경은 하지 않는다.

### 실제 업로드

실제 변경은 `--commit`을 명시한 경우에만 수행한다.

```bash
uv run --frozen scripts/upload_play_screenshots.py --commit
```

순서는 다음과 같다.

1. 모든 로컬 자산을 먼저 검증한다.
2. Play edit을 하나 생성한다.
3. 탐색된 locale/image type 조합만 `deleteall`한다.
4. 파일명 순서대로 이미지를 업로드한다.
5. edit을 validate하고 commit한다.
6. commit 전 오류가 발생하면 edit을 삭제한다.

따라서 대상에 포함되지 않은 locale과 image type은 변경하지 않으며, 중간 실패가 현재 공개 listing에 부분 반영되지 않는다.

## GitHub Actions

별도 `Play Store Screenshots` workflow를 사용한다.

- `workflow_dispatch`와 `confirm_upload=true`가 모두 필요하다.
- `main`의 최신 commit에서만 실행한다.
- `android-internal` environment의 기존 `PLAY_SERVICE_ACCOUNT_JSON`을 임시 파일로 복원한다.
- 기대하는 service account email만 검사하고 credential 내용은 출력하지 않는다.
- release workflow와 같은 `release-cd` concurrency group을 사용해 동시에 Play edit을 만들지 않는다.
- 업로드 전 dry-run 출력을 job log에 남기고, commit 결과는 job summary에 기록하며 실제 API 호출 후 credential을 항상 삭제한다.

서비스 계정에는 해당 앱의 `Manage store presence` 권한이 필요하다. track 배포 권한만 있고 이 권한이 없으면 API 호출을 우회하지 않고 Play Console 권한 설정을 blocker로 보고한다.

## 검증

- Python 단위 테스트
  - PNG/JPEG 크기와 alpha 판별
  - 파일 순번, 개수, 지원 locale/image type 검증
  - 대상별 delete/upload 순서
  - 성공 시 validate/commit, 실패 시 edit delete
- workflow 정적 검사
  - `actionlint`
  - `main`·명시적 승인 guard
  - release workflow와 concurrency group 공유
- 실제 첫 실행
  - 완성된 `ko-KR/phoneScreenshots` 산출물이 준비된 뒤 수동 dispatch
  - Play Console에서 이미지 순서와 review/publishing 상태 확인

## 안전 경계

- 저장소의 현재 `references/existing` 이미지는 288×512라 최소 규격을 충족하지 않으며 업로드하지 않는다.
- 현재 `source/android/phone/ko` PNG는 alpha channel을 포함한 캡처 원본이므로 그대로 업로드하지 않는다.
- workflow는 이미지가 없는 target을 삭제 요청으로 해석하지 않는다. 삭제만 수행하는 기능은 제공하지 않는다.
- API가 반환한 image URL이나 credential 내용을 로그에 출력하지 않는다.
