# Google Play publish assets

이 디렉터리에는 Play Console에 실제 업로드할 최종 이미지 산출물만 둔다. 캡처 원본은 `store-assets/screenshots/source/`, 과거 참고 이미지는 `store-assets/screenshots/references/`에 유지한다.

```text
google-play/
  ko-KR/
    phoneScreenshots/
      01-hero.png
      02-feature.png
    sevenInchScreenshots/
      01-overview.png
    tenInchScreenshots/
      01-overview.png
    featureGraphic/
      01-feature-graphic.png
```

- locale은 Play listing과 같은 BCP-47 tag를 사용한다.
- 스크린샷은 type별 최대 8장이며 파일명의 `01`, `02` 순서로 업로드된다.
- 이미지는 JPEG 또는 alpha가 없는 24-bit PNG여야 한다.
- 스크린샷 각 변은 320~3840px이고 긴 변은 짧은 변의 두 배 이하여야 한다.
- feature graphic은 1024×500px 한 장이다.

Credential 없이 로컬 자산을 검증한다.

```bash
python3 scripts/upload_play_screenshots.py
```

실제 업로드는 `main`의 `Play Store Screenshots` GitHub Actions workflow에서 실행한다.
