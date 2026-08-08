# /// script
# requires-python = ">=3.10"
# dependencies = [
#   "google-api-python-client==2.198.0",
#   "google-auth==2.56.3",
# ]
# ///
"""Play Store 전체 트랙의 최대 versionCode와 다음 최소값을 조회한다."""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path
from typing import Optional

PACKAGE_NAME = "com.nexters.bandalart"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--expect-version-code", type=int)
    parser.add_argument("--verify-track")
    parser.add_argument("--verify-status", default="completed")
    parser.add_argument("--retries", type=int, default=1)
    return parser.parse_args()


def read_tracks(service: object) -> list[dict]:
    edit_id: Optional[str] = None
    try:
        edit = service.edits().insert(packageName=PACKAGE_NAME, body={}).execute()
        edit_id = edit["id"]
        response = (
            service.edits()
            .tracks()
            .list(packageName=PACKAGE_NAME, editId=edit_id)
            .execute()
        )
        return response.get("tracks", [])
    finally:
        if edit_id is not None:
            try:
                service.edits().delete(packageName=PACKAGE_NAME, editId=edit_id).execute()
            except Exception:
                pass


def summarize_tracks(tracks: list[dict]) -> tuple[dict[str, int], int]:
    per_track_max: dict[str, int] = {}
    for track in tracks:
        name = track.get("track", "unknown")
        version_codes: list[int] = []
        for release in track.get("releases", []):
            for code in release.get("versionCodes", []) or []:
                try:
                    version_codes.append(int(code))
                except (TypeError, ValueError):
                    continue
        per_track_max[name] = max(version_codes) if version_codes else 0

    if not per_track_max:
        raise RuntimeError("no tracks returned from Play API")
    return per_track_max, max(per_track_max.values())


def expected_release_exists(
    tracks: list[dict],
    track_name: str,
    version_code: int,
    status: str,
) -> bool:
    for track in tracks:
        if track.get("track") != track_name:
            continue
        for release in track.get("releases", []):
            codes = {int(code) for code in release.get("versionCodes", []) or []}
            if version_code in codes and release.get("status") == status:
                return True
    return False


def main() -> int:
    args = parse_args()
    if args.verify_track and args.expect_version_code is None:
        print("error: --verify-track requires --expect-version-code", file=sys.stderr)
        return 2
    if args.retries < 1:
        print("error: --retries must be at least 1", file=sys.stderr)
        return 2

    project_root = Path(__file__).resolve().parent.parent
    credentials_path = project_root / "playstore" / "service-account-key.json"

    if not credentials_path.exists():
        print(f"error: {credentials_path} not found", file=sys.stderr)
        return 1

    try:
        from google.oauth2 import service_account
        from googleapiclient.discovery import build
        from googleapiclient.errors import HttpError

        credentials = service_account.Credentials.from_service_account_file(
            str(credentials_path),
            scopes=SCOPES,
        )
    except Exception as exc:
        print(f"error: failed to load service account credentials: {exc}", file=sys.stderr)
        return 1

    try:
        service = build("androidpublisher", "v3", credentials=credentials, cache_discovery=False)
    except Exception as exc:
        print(f"error: failed to build Play API client: {exc}", file=sys.stderr)
        return 1

    tracks: list[dict] = []
    per_track_max: dict[str, int] = {}
    current_max = 0
    for attempt in range(args.retries):
        try:
            tracks = read_tracks(service)
            per_track_max, current_max = summarize_tracks(tracks)
        except (HttpError, RuntimeError) as exc:
            print(f"error: Play API call failed: {exc}", file=sys.stderr)
            return 1

        if not args.verify_track or expected_release_exists(
            tracks,
            args.verify_track,
            args.expect_version_code,
            args.verify_status,
        ):
            break
        if attempt + 1 < args.retries:
            time.sleep(min(5 * (attempt + 1), 20))
    else:
        print(
            "error: expected release was not found on "
            f"{args.verify_track}: versionCode={args.expect_version_code}, "
            f"status={args.verify_status}",
            file=sys.stderr,
        )
        return 1

    next_code = current_max + 1
    tracks_summary = ",".join(f"{key}:{value}" for key, value in sorted(per_track_max.items()))

    print(f"CURRENT_MAX={current_max}")
    print(f"NEXT={next_code}")
    print(f"TRACKS={tracks_summary}")
    if args.expect_version_code is not None and not args.verify_track:
        if args.expect_version_code <= current_max:
            print(
                f"error: versionCode {args.expect_version_code} is already used; "
                f"it must be greater than {current_max}",
                file=sys.stderr,
            )
            return 1
        print(f"EXPECTED_VERSION_CODE={args.expect_version_code}")
    if args.verify_track:
        print(
            f"VERIFIED={args.verify_track}:{args.expect_version_code}:{args.verify_status}"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
