#!/usr/bin/env python3
"""iPhone Duo 원본 캡처를 App Store 마케팅 스크린샷으로 렌더링한다.

원본(store-assets/screenshots/source/ios/duo/{inner,outer}/*.png)을 기존 스토어 스크린샷과 같은
스타일(짙은 남색 배경, 흰색 2줄 헤드라인, 어두운 기기 테두리)의 HTML에 넣고 headless Chrome으로 찍는다.
출력 크기는 App Store Connect의 iPhone Duo 규격과 같다.
  outer(바깥 화면, 세로): 1398 x 2034
  inner(안쪽 화면, 가로): 2853 x 2007

usage:
  python3 scripts/render_duo_store_screenshots.py            # 전체 렌더링
  python3 scripts/render_duo_store_screenshots.py --only inner
"""
import argparse
import base64
import pathlib
import shutil
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "store-assets/screenshots/source/ios/duo"
OUTPUT = ROOT / "store-assets/screenshots/publish/app-store/ko-KR/iphone-duo"
FONT = ROOT / "core/designsystem/src/commonMain/composeResources/font/pretendard_bold.otf"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

SIZES = {"outer": (1398, 2034), "inner": (2853, 2007)}

# (display, 원본 파일명, 출력 파일명, 헤드라인 2줄)
SHOTS = [
    ("inner", "home-filled.png", "01-home.png", ("꿈을 향한 여정,", "25칸에 담아보세요")),
    ("inner", "task-edit-sheet.png", "02-edit.png", ("이모지와 색상으로", "반다라트를 더 특별하게")),
    ("inner", "board-list-sheet.png", "03-list.png", ("여러 개의 반다라트를", "손쉽게 관리해보세요")),
    ("inner", "settings-reminder-time.png", "04-reminder.png", ("원하는 시간에", "마감일 알림을 받아보세요")),
    ("outer", "home-filled.png", "01-home.png", ("꿈을 향한 여정,", "25칸에 담아보세요")),
    ("outer", "task-edit-sheet.png", "02-edit.png", ("이모지와 색상으로", "반다라트를 더 특별하게")),
    ("outer", "board-list-sheet.png", "03-list.png", ("여러 개의 반다라트를", "손쉽게 관리해보세요")),
]

BACKGROUND = "#222A36"
BEZEL = "#0E1116"


def data_uri(path: pathlib.Path, mime: str) -> str:
    return f"data:{mime};base64,{base64.b64encode(path.read_bytes()).decode()}"


def page_html(display: str, image: pathlib.Path, headline: tuple[str, str]) -> str:
    width, height = SIZES[display]
    if display == "outer":
        font_size, top, device_width = 92, 170, 1060
        device_style = f"width:{device_width}px; left:{(width - device_width) // 2}px; top:520px;"
    else:
        font_size, top, device_width = 104, 150, 1980
        device_style = f"width:{device_width}px; left:{(width - device_width) // 2}px; top:520px;"
    radius = int(device_width * 0.07)
    bezel = int(device_width * 0.022)
    return f"""<!doctype html>
<html><head><meta charset="utf-8"><style>
@font-face {{ font-family: Pretendard; src: url('{data_uri(FONT, "font/otf")}'); font-weight: 700; }}
html, body {{ margin:0; width:{width}px; height:{height}px; overflow:hidden; background:{BACKGROUND}; }}
.headline {{ position:absolute; top:{top}px; width:100%; text-align:center; color:#fff;
  font-family:Pretendard; font-weight:700; font-size:{font_size}px; line-height:1.4; letter-spacing:-0.02em; }}
.device {{ position:absolute; {device_style} padding:{bezel}px; background:{BEZEL};
  border-radius:{radius + bezel}px; box-sizing:border-box; }}
.device img {{ display:block; width:100%; border-radius:{radius}px; }}
</style></head><body>
<div class="headline">{headline[0]}<br>{headline[1]}</div>
<div class="device"><img src="{data_uri(image, "image/png")}"></div>
</body></html>"""


def render(html: str, size: tuple[int, int], out: pathlib.Path, workdir: pathlib.Path) -> None:
    out.unlink(missing_ok=True)
    page = workdir / f"{out.parent.name}-{out.stem}.html"
    page.write_text(html, encoding="utf-8")
    out.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
            # 사용 중인 Chrome 프로필과 충돌하지 않도록 임시 프로필을 쓴다.
            f"--user-data-dir={workdir / 'chrome-profile'}", "--no-first-run",
            "--force-device-scale-factor=1", f"--window-size={size[0]},{size[1]}",
            f"--screenshot={out}", page.as_uri(),
        ],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    # headless Chrome은 스크린샷을 저장한 뒤에도 종료 코드가 0이 아닐 때가 있어 파일로 성공을 판단한다.
    if not out.exists():
        raise RuntimeError(f"렌더링 실패: {out}")
    # App Store는 alpha 없는 이미지를 요구하므로 JPEG를 거쳐 PNG로 다시 저장한다.
    tmp = out.with_suffix(".jpg")
    subprocess.run(["sips", "-s", "format", "jpeg", "-s", "formatOptions", "best", str(out), "--out", str(tmp)],
                   check=True, stdout=subprocess.DEVNULL)
    subprocess.run(["sips", "-s", "format", "png", str(tmp), "--out", str(out)], check=True, stdout=subprocess.DEVNULL)
    tmp.unlink()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--only", choices=sorted(SIZES))
    args = parser.parse_args()

    if not pathlib.Path(CHROME).exists() or not shutil.which("sips"):
        print("Google Chrome과 sips가 필요합니다.", file=sys.stderr)
        return 1

    missing = []
    with tempfile.TemporaryDirectory() as tmp:
        workdir = pathlib.Path(tmp)
        for display, source_name, out_name, headline in SHOTS:
            if args.only and display != args.only:
                continue
            source = SOURCE / display / source_name
            if not source.exists():
                missing.append(str(source.relative_to(ROOT)))
                continue
            out = OUTPUT / display / out_name
            render(page_html(display, source, headline), SIZES[display], out, workdir)
            print(out.relative_to(ROOT))
    for path in missing:
        print(f"원본 없음: {path}", file=sys.stderr)
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
