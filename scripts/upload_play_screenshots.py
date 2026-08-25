# /// script
# requires-python = ">=3.10"
# dependencies = [
#   "google-api-python-client==2.198.0",
#   "google-auth==2.56.3",
# ]
# ///
"""Validate and upload Google Play listing screenshots through the official API."""

from __future__ import annotations

import argparse
import os
import re
import struct
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable, Optional

PACKAGE_NAME = "com.nexters.bandalart"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]
SUPPORTED_IMAGE_TYPES = {
    "phoneScreenshots",
    "sevenInchScreenshots",
    "tenInchScreenshots",
    "featureGraphic",
}
SCREENSHOT_IMAGE_TYPES = SUPPORTED_IMAGE_TYPES - {"featureGraphic"}
LOCALE_PATTERN = re.compile(r"^[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*$")
FILE_PATTERN = re.compile(r"^(\d{2})(?:[-_.][^/]*)?\.(png|jpe?g)$", re.IGNORECASE)
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
JPEG_START_OF_FRAME_MARKERS = {
    0xC0,
    0xC1,
    0xC2,
    0xC3,
    0xC5,
    0xC6,
    0xC7,
    0xC9,
    0xCA,
    0xCB,
    0xCD,
    0xCE,
    0xCF,
}


class AssetValidationError(ValueError):
    """Raised when local Play listing assets do not meet the repository contract."""


@dataclass(frozen=True)
class ImageInfo:
    width: int
    height: int
    mime_type: str


@dataclass(frozen=True)
class UploadImage:
    path: Path
    info: ImageInfo


@dataclass(frozen=True)
class UploadTarget:
    locale: str
    image_type: str
    images: tuple[UploadImage, ...]


def parse_args() -> argparse.Namespace:
    root = Path(__file__).resolve().parent.parent
    default_credentials = os.environ.get(
        "PLAY_SERVICE_ACCOUNT_PATH",
        str(root / "playstore" / "service-account-key.json"),
    )
    parser = argparse.ArgumentParser(
        description="Validate Google Play screenshots, and upload only with --commit.",
    )
    parser.add_argument(
        "--assets-dir",
        type=Path,
        default=root / "store-assets" / "screenshots" / "publish" / "google-play",
    )
    parser.add_argument("--locale", action="append", dest="locales")
    parser.add_argument(
        "--image-type",
        action="append",
        choices=sorted(SUPPORTED_IMAGE_TYPES),
        dest="image_types",
    )
    parser.add_argument("--credentials", type=Path, default=Path(default_credentials))
    parser.add_argument(
        "--commit",
        action="store_true",
        help="Create, validate, and commit a Google Play edit.",
    )
    return parser.parse_args()


def read_png_info(path: Path) -> ImageInfo:
    with path.open("rb") as image:
        signature = image.read(8)
        length_raw = image.read(4)
        chunk_type = image.read(4)
        if signature != PNG_SIGNATURE or len(length_raw) != 4 or chunk_type != b"IHDR":
            raise AssetValidationError(f"{path}: invalid PNG header")
        length = struct.unpack(">I", length_raw)[0]
        header = image.read(length)

    if length != 13 or len(header) != 13:
        raise AssetValidationError(f"{path}: invalid PNG IHDR")
    width, height, bit_depth, color_type, _, _, _ = struct.unpack(">IIBBBBB", header)
    if bit_depth != 8 or color_type != 2:
        raise AssetValidationError(
            f"{path}: PNG must be 24-bit RGB without alpha (bitDepth=8, colorType=2)",
        )
    return ImageInfo(width=width, height=height, mime_type="image/png")


def read_jpeg_info(path: Path) -> ImageInfo:
    data = path.read_bytes()
    if len(data) < 4 or data[:2] != b"\xff\xd8":
        raise AssetValidationError(f"{path}: invalid JPEG header")

    offset = 2
    while offset < len(data):
        while offset < len(data) and data[offset] == 0xFF:
            offset += 1
        if offset >= len(data):
            break
        marker = data[offset]
        offset += 1
        if marker in {0x01, 0xD8, 0xD9}:
            continue
        if offset + 2 > len(data):
            break
        segment_length = struct.unpack(">H", data[offset : offset + 2])[0]
        if segment_length < 2 or offset + segment_length > len(data):
            break
        if marker in JPEG_START_OF_FRAME_MARKERS:
            if segment_length < 7:
                break
            height, width = struct.unpack(">HH", data[offset + 3 : offset + 7])
            return ImageInfo(width=width, height=height, mime_type="image/jpeg")
        if marker == 0xDA:
            break
        offset += segment_length

    raise AssetValidationError(f"{path}: JPEG dimensions were not found")


def read_image_info(path: Path) -> ImageInfo:
    suffix = path.suffix.lower()
    if suffix == ".png":
        return read_png_info(path)
    if suffix in {".jpg", ".jpeg"}:
        return read_jpeg_info(path)
    raise AssetValidationError(f"{path}: only PNG and JPEG images are supported")


def validate_dimensions(path: Path, image_type: str, info: ImageInfo) -> None:
    if image_type == "featureGraphic":
        if (info.width, info.height) != (1024, 500):
            raise AssetValidationError(
                f"{path}: featureGraphic must be 1024x500, got {info.width}x{info.height}",
            )
        return

    shortest = min(info.width, info.height)
    longest = max(info.width, info.height)
    if shortest < 320 or longest > 3840:
        raise AssetValidationError(
            f"{path}: screenshot dimensions must be within 320..3840, "
            f"got {info.width}x{info.height}",
        )
    if longest > shortest * 2:
        raise AssetValidationError(
            f"{path}: the longest side cannot exceed twice the shortest side",
        )


def numbered_image_paths(directory: Path, image_type: str) -> tuple[Path, ...]:
    entries = sorted(
        (entry for entry in directory.iterdir() if not entry.name.startswith(".")),
        key=lambda entry: entry.name.casefold(),
    )
    if any(not entry.is_file() for entry in entries):
        raise AssetValidationError(f"{directory}: nested directories are not supported")

    numbered: list[tuple[int, Path]] = []
    for entry in entries:
        match = FILE_PATTERN.fullmatch(entry.name)
        if match is None:
            raise AssetValidationError(
                f"{entry}: filename must start with a two-digit order and be PNG or JPEG",
            )
        numbered.append((int(match.group(1)), entry))

    expected_count = 1 if image_type == "featureGraphic" else None
    if expected_count is not None and len(numbered) != expected_count:
        raise AssetValidationError(f"{directory}: featureGraphic requires exactly one image")
    if image_type in SCREENSHOT_IMAGE_TYPES and not 1 <= len(numbered) <= 8:
        raise AssetValidationError(f"{directory}: screenshots require 1..8 images")
    if [number for number, _ in numbered] != list(range(1, len(numbered) + 1)):
        raise AssetValidationError(f"{directory}: image order must be consecutive from 01")
    return tuple(path for _, path in numbered)


def discover_targets(
    assets_dir: Path,
    locales: Optional[Iterable[str]] = None,
    image_types: Optional[Iterable[str]] = None,
) -> tuple[UploadTarget, ...]:
    if not assets_dir.is_dir():
        raise AssetValidationError(f"assets directory not found: {assets_dir}")

    selected_locales = set(locales or [])
    selected_image_types = set(image_types or [])
    for locale in selected_locales:
        if LOCALE_PATTERN.fullmatch(locale) is None:
            raise AssetValidationError(f"invalid locale: {locale}")

    targets: list[UploadTarget] = []
    available_locales: set[str] = set()
    for locale_dir in sorted(assets_dir.iterdir(), key=lambda path: path.name.casefold()):
        if locale_dir.name.startswith(".") or locale_dir.name == "README.md":
            continue
        if not locale_dir.is_dir() or LOCALE_PATTERN.fullmatch(locale_dir.name) is None:
            raise AssetValidationError(f"unsupported locale entry: {locale_dir}")
        available_locales.add(locale_dir.name)
        if selected_locales and locale_dir.name not in selected_locales:
            continue

        for type_dir in sorted(locale_dir.iterdir(), key=lambda path: path.name.casefold()):
            if type_dir.name.startswith("."):
                continue
            if not type_dir.is_dir() or type_dir.name not in SUPPORTED_IMAGE_TYPES:
                raise AssetValidationError(f"unsupported image type entry: {type_dir}")
            if selected_image_types and type_dir.name not in selected_image_types:
                continue

            images = tuple(
                UploadImage(path=path, info=read_image_info(path))
                for path in numbered_image_paths(type_dir, type_dir.name)
            )
            for image in images:
                validate_dimensions(image.path, type_dir.name, image.info)
            targets.append(
                UploadTarget(
                    locale=locale_dir.name,
                    image_type=type_dir.name,
                    images=images,
                ),
            )

    missing_locales = selected_locales - available_locales
    if missing_locales:
        raise AssetValidationError(
            f"selected locale directories not found: {', '.join(sorted(missing_locales))}",
        )
    if not targets:
        raise AssetValidationError("no upload targets found")
    return tuple(targets)


def print_plan(assets_dir: Path, package_name: str, targets: Iterable[UploadTarget]) -> None:
    print(f"PACKAGE={package_name}")
    for target in targets:
        print(f"TARGET={target.locale}/{target.image_type} COUNT={len(target.images)}")
        for image in target.images:
            relative_path = image.path.relative_to(assets_dir)
            print(
                f"  {relative_path} {image.info.width}x{image.info.height} "
                f"{image.info.mime_type}",
            )


def replace_images(
    service: object,
    media_factory: Callable[..., object],
    package_name: str,
    targets: Iterable[UploadTarget],
) -> None:
    edit_id: Optional[str] = None
    committed = False
    try:
        edit = service.edits().insert(packageName=package_name, body={}).execute()
        edit_id = edit["id"]
        images_api = service.edits().images()
        for target in targets:
            images_api.deleteall(
                packageName=package_name,
                editId=edit_id,
                language=target.locale,
                imageType=target.image_type,
            ).execute()
            for image in target.images:
                media = media_factory(
                    str(image.path),
                    mimetype=image.info.mime_type,
                    resumable=False,
                )
                images_api.upload(
                    packageName=package_name,
                    editId=edit_id,
                    language=target.locale,
                    imageType=target.image_type,
                    media_body=media,
                ).execute()

        service.edits().validate(packageName=package_name, editId=edit_id).execute()
        service.edits().commit(packageName=package_name, editId=edit_id).execute()
        committed = True
    finally:
        if edit_id is not None and not committed:
            try:
                service.edits().delete(packageName=package_name, editId=edit_id).execute()
            except Exception as cleanup_error:
                print(
                    f"warning: failed to delete uncommitted Play edit: {cleanup_error}",
                    file=sys.stderr,
                )


def build_service(credentials_path: Path) -> tuple[object, Callable[..., object]]:
    if not credentials_path.is_file():
        raise RuntimeError(f"credentials file not found: {credentials_path}")

    from google.oauth2 import service_account
    from googleapiclient.discovery import build
    from googleapiclient.http import MediaFileUpload

    credentials = service_account.Credentials.from_service_account_file(
        str(credentials_path),
        scopes=SCOPES,
    )
    service = build("androidpublisher", "v3", credentials=credentials, cache_discovery=False)
    return service, MediaFileUpload


def main() -> int:
    args = parse_args()
    try:
        targets = discover_targets(args.assets_dir, args.locales, args.image_types)
        print_plan(args.assets_dir, PACKAGE_NAME, targets)
        if not args.commit:
            print("MODE=dry-run")
            return 0

        service, media_factory = build_service(args.credentials)
        replace_images(service, media_factory, PACKAGE_NAME, targets)
        print("MODE=commit")
        print("RESULT=validated-and-committed")
        return 0
    except Exception as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
