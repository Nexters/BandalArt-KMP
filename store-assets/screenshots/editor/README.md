# 반다라트 스토어 스크린샷 편집기

App Store와 Google Play 등록 이미지를 같은 구성으로 편집하고 규격별 PNG 묶음으로 내보내는 도구입니다.

## Quick start

```bash
pnpm install
pnpm dev
```

## 현재 구성

- 언어: 한국어
- 테마: 기존 등록 이미지의 짙은 네이비, 흰색, 민트 포인트를 반영한 `Bandalart Navy`
- Android: 템플릿 선택 화면과 홈·목록 원본을 연결한 초안 4장
- iOS: 동일한 문구 구조의 3장 초안. iOS 원본 캡처를 연결하기 전까지 기기 화면은 비워둡니다.
- 신규 기능 문구: 템플릿으로 빠른 시작, 마지막으로 본 반다라트 위젯, 마감일 알림

위젯과 마감일 알림은 실제 홈 화면·알림 원본이 아직 없어 빈 드롭 영역으로 남겨뒀습니다. 원본을 캡처한 다음 Inspector의 Pick 또는 드래그 앤 드롭으로 채웁니다. 기존 완료 화면과 기존 등록 이미지에서 이미 소개한 입력·수정 흐름은 신규 초안에서 반복하지 않습니다.

원본과 참고 이미지는 편집기 바깥의 다음 경로에 보관합니다.

- `../source/android/phone/ko/`: Android 실기기 원본
- `../references/existing/`: 기존 스토어 등록 이미지 5장

## What's inside

- **Connected canvas editor** (`src/components/editor/`) — every screen sits on one horizontal canvas, so phones, captions, and other elements can be dragged across screen boundaries and exported as split crops when Connected mode is enabled.
- **Screen controls** — drag-to-reorder screens, click-to-edit text, screenshot drop targets, per-screen layout switcher, dark/light toggle.
- **Device frames** (`src/components/editor/device-frames.tsx`) — iPhone (PNG mockup), iPad, Android phone, Android tablet (portrait + landscape), feature graphic.
- **Auto-save (git-trackable)** — every change is persisted within ~600ms to **`app-store-screenshots.json`** at the project root (via `/api/project`) **and** mirrored to `localStorage` as an instant-paint cache. Commit `app-store-screenshots.json` and you can `git clone` to another machine and resume exactly where you left off.
- **Multi-device decks** — iOS and Android slide decks live side by side; switching the platform tab preserves both.
- **One-click export** — bulk PNG export at any required App Store / Play Store resolution using `html-to-image`; each PNG is rendered from the current connected or isolated deck mode.
- **Project migration** — older `app-store-screenshots.json` files are migrated on load. Existing per-slide transforms remain valid, and connected crops become available without rewriting the deck by hand.
- **Legacy-safe mode** — pre-v2 projects opened directly in the editor start in isolated-screen mode first, then can opt into connected crops with the toolbar's Connected/Isolated control. Skill-run in-place migrations keep legacy decks isolated unless the project had already explicitly opted into connected canvas.

## Adding screenshots

Two ways:

1. **Drop a file in the inspector** — drag-and-drop or click Pick. The file is sent to `/api/upload`, hashed, and written to `public/screenshots/uploaded/<hash>.png`. The slide stores the resulting `/screenshots/uploaded/...` path, so commit those files alongside `app-store-screenshots.json` and the screenshots survive a `git clone`.
2. **Reference a static file** — put PNGs under `public/screenshots/{platform}/{device}/{locale}/` and reference them by path. Default sample slides expect:
   - `public/screenshots/apple/iphone/en/...`
   - `public/screenshots/android/phone/en/...`
   - `public/screenshots/apple/ipad/en/...`

Update the matching `screenshot` fields in `app-store-screenshots.json` to point at whatever filenames you choose.

## Exporting

The toolbar dropdown lists every Apple/Google-required size for the current device. Click **Export bundle** to download a zip. In Connected mode, each PNG is clipped from the connected canvas, so an element that straddles two screens appears split exactly where you placed it. In Isolated mode, each screen clips its own elements and legacy offscreen content cannot leak into neighboring exports.

## Customizing

| Where | What |
|-------|------|
| `src/lib/constants.ts` | Canvas dimensions, export sizes, frame ratios, themes, locales |
| `app-store-screenshots.json` | Canonical starter project: app name, current device, connected-canvas mode, slide copy, screenshots, and transforms |
| `src/lib/defaults.ts` | Fallback/reset state used when no project file or local cache exists |
| `src/components/editor/slide-canvas.tsx` | Add new layouts and connected-canvas element rendering |
| `src/components/editor/device-frames.tsx` | Tweak device chrome (bezel radii, camera dots) |
| `src/app/layout.tsx` | Swap the font (`next/font/google`) |

## Notes

- `mockup.png` is the iPhone bezel overlay; replacing it requires re-measuring the `PHONE_SCREEN` constants.
- Image preloading converts every static path to a base64 data URI before exports run, and export retries paths that were previously missing — this prevents the html-to-image race where some slide screenshots come out black.
- Reset via the toolbar's circular arrow icon clears in-memory state and reloads the default screens. To wipe disk state too, delete `app-store-screenshots.json`.
- **Persistence model** — the canonical state lives in `app-store-screenshots.json` (git-tracked). On load, the editor reads localStorage first for instant paint, then overwrites with the file contents if present; if the file endpoint is unavailable, autosave is blocked so stale cache cannot overwrite disk. On save, both are written. If you ever see a conflict, the file always wins.
- **Migration model** — schema v1 projects do not need a manual conversion. On first load, the editor upgrades localized text and transform records, writes `schemaVersion: 2`, preserves all existing screens, and keeps `connectedCanvas: false` so old offscreen/clipped elements export exactly as isolated screens. Turn on **Connected** in the toolbar when you want elements to cross screen edges. Explicit skill migrations preserve an existing `connectedCanvas` choice, otherwise they keep legacy decks isolated too.
- **Custom themes** — if a project file references a theme id that is not present in `src/lib/constants.ts`, the editor falls back to `clean-light` and shows a warning. Merge custom `THEMES` entries during in-place upgrades.
