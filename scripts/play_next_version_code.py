# /// script
# requires-python = ">=3.10"
# dependencies = [
#   "google-api-python-client>=2.0",
#   "google-auth>=2.0",
# ]
# ///
"""Play Store 전체 트랙의 최대 versionCode와 다음 최소값을 조회한다."""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Optional

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

PACKAGE_NAME = "com.nexters.bandalart"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def main() -> int:
    project_root = Path(__file__).resolve().parent.parent
    credentials_path = project_root / "playstore" / "service-account-key.json"

    if not credentials_path.exists():
        print(f"error: {credentials_path} not found", file=sys.stderr)
        return 1

    try:
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

    edit_id: Optional[str] = None
    try:
        edit = service.edits().insert(packageName=PACKAGE_NAME, body={}).execute()
        edit_id = edit["id"]
        tracks_response = (
            service.edits()
            .tracks()
            .list(packageName=PACKAGE_NAME, editId=edit_id)
            .execute()
        )
    except HttpError as exc:
        print(f"error: Play API call failed: {exc}", file=sys.stderr)
        return 1
    finally:
        if edit_id is not None:
            try:
                service.edits().delete(packageName=PACKAGE_NAME, editId=edit_id).execute()
            except Exception:
                pass

    tracks = tracks_response.get("tracks", [])
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
        print("error: no tracks returned from Play API", file=sys.stderr)
        return 1

    current_max = max(per_track_max.values())
    next_code = current_max + 1
    tracks_summary = ",".join(f"{key}:{value}" for key, value in sorted(per_track_max.items()))

    print(f"CURRENT_MAX={current_max}")
    print(f"NEXT={next_code}")
    print(f"TRACKS={tracks_summary}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
