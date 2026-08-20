#!/usr/bin/env python3
"""Validate the exact Android App Bundle selected for Play Internal upload."""

from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE_NAME = "com.nexters.bandalart"
TEST_REWARDED_ID = b"ca-app-pub-3940256099942544/5224354917"
PRODUCTION_REWARDED_IDS = (
    b"ca-app-pub-5570932833347277/6659503579",
    b"ca-app-pub-5570932833347277/7686378276",
)
TEST_BANNER_ID = b"ca-app-pub-3940256099942544/6300978111"
PRODUCTION_BANNER_ID = b"ca-app-pub-5570932833347277/1215605203"
ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
REQUIRED_NAMESPACE = (
    b"bandalart.core.designsystem.generated.resources",
    b"bandalart/core/designsystem/generated/resources",
)
REMOVED_NAMESPACE = (
    b"bandalart.composeapp.generated.resources",
    b"bandalart/composeapp/generated/resources",
)
RELEASE_NOTES = (
    "androidApp/src/main/play/release-notes/ko-KR/internal.txt",
    "androidApp/src/main/play/release-notes/en-US/internal.txt",
    "androidApp/src/main/play/release-notes/ja-JP/internal.txt",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--aab", type=Path, required=True)
    parser.add_argument("--bundletool", type=Path, required=True)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--version-code", type=int, required=True)
    return parser.parse_args()


def fail(message: str) -> None:
    raise ValueError(message)


def verify_release_notes(root: Path) -> None:
    for relative in RELEASE_NOTES:
        path = root / relative
        text = path.read_text(encoding="utf-8").strip()
        if not text:
            fail(f"release notes are empty: {relative}")
        if len(text) > 500:
            fail(f"release notes exceed 500 characters: {relative}")


def verify_archive(path: Path) -> None:
    with zipfile.ZipFile(path) as archive:
        bad_entry = archive.testzip()
        if bad_entry:
            fail(f"AAB ZIP integrity failed at {bad_entry}")
        names = set(archive.namelist())
        if "base/manifest/AndroidManifest.xml" not in names:
            fail("AAB base manifest is missing")

        test_rewarded_id_found = False
        production_rewarded_ids_found: set[bytes] = set()
        test_banner_id_found = False
        production_banner_id_found = False
        required_namespace_found = False
        removed_namespace_found = False
        for info in archive.infolist():
            if info.is_dir():
                continue
            content = archive.read(info)
            test_rewarded_id_found = test_rewarded_id_found or TEST_REWARDED_ID in content
            production_rewarded_ids_found.update(
                ad_unit_id for ad_unit_id in PRODUCTION_REWARDED_IDS if ad_unit_id in content
            )
            test_banner_id_found = test_banner_id_found or TEST_BANNER_ID in content
            production_banner_id_found = production_banner_id_found or PRODUCTION_BANNER_ID in content
            required_namespace_found = required_namespace_found or any(
                value in content for value in REQUIRED_NAMESPACE
            )
            removed_namespace_found = removed_namespace_found or any(
                value in content for value in REMOVED_NAMESPACE
            )

        if test_rewarded_id_found:
            fail("official Google test rewarded ad ID is present")
        if len(production_rewarded_ids_found) != len(PRODUCTION_REWARDED_IDS):
            fail("production rewarded ad ID is missing")
        if test_banner_id_found:
            fail("official Google test banner ad ID is present")
        if not production_banner_id_found:
            fail("production banner ad ID is missing")
        if not required_namespace_found:
            fail("expected Compose resource namespace is missing")
        if removed_namespace_found:
            fail("removed Compose resource namespace is present")


def verify_signature(path: Path) -> None:
    result = subprocess.run(
        ["jarsigner", "-verify", str(path)],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        check=False,
    )
    if result.returncode != 0 or "jar verified." not in result.stdout:
        fail("AAB signature verification failed")


def verify_manifest_xml(
    manifest_xml: str,
    version_name: str,
    version_code: int,
) -> None:
    try:
        root = ET.fromstring(manifest_xml)
    except ET.ParseError as exc:
        fail(f"bundletool returned an invalid manifest: {exc}")
    if root.tag != "manifest" or root.attrib.get("package") != PACKAGE_NAME:
        fail("AAB manifest package does not match the production package")
    if root.attrib.get(f"{{{ANDROID_NAMESPACE}}}versionName") != version_name:
        fail("AAB manifest versionName does not match source")
    if root.attrib.get(f"{{{ANDROID_NAMESPACE}}}versionCode") != str(version_code):
        fail("AAB manifest versionCode does not match source")


def verify_with_bundletool(
    bundletool: Path,
    aab: Path,
    version_name: str,
    version_code: int,
) -> None:
    if not bundletool.is_file():
        fail("bundletool is missing")
    validation = subprocess.run(
        ["java", "-jar", str(bundletool), "validate", f"--bundle={aab}"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,
        text=True,
        check=False,
    )
    if validation.returncode != 0:
        fail("bundletool validation failed")
    manifest = subprocess.run(
        [
            "java",
            "-jar",
            str(bundletool),
            "dump",
            "manifest",
            f"--bundle={aab}",
            "--module=base",
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        check=False,
    )
    if manifest.returncode != 0:
        fail("bundletool manifest dump failed")
    verify_manifest_xml(manifest.stdout, version_name, version_code)


def main() -> int:
    args = parse_args()
    root = Path(__file__).resolve().parent.parent
    try:
        if not args.aab.is_file() or args.aab.stat().st_size == 0:
            fail("AAB is missing or empty")
        verify_release_notes(root)
        verify_archive(args.aab)
        verify_signature(args.aab)
        verify_with_bundletool(
            args.bundletool,
            args.aab,
            args.version_name,
            args.version_code,
        )
    except (OSError, ValueError, zipfile.BadZipFile) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    checksum = hashlib.sha256(args.aab.read_bytes()).hexdigest()
    print(f"AAB_PATH={args.aab}")
    print(f"AAB_SIZE={args.aab.stat().st_size}")
    print(f"AAB_SHA256={checksum}")
    print(f"PACKAGE={PACKAGE_NAME}")
    print(f"VERSION={args.version_name} ({args.version_code})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
