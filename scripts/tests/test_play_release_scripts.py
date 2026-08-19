from __future__ import annotations

import tempfile
import unittest
import zipfile
from pathlib import Path

from scripts import play_next_version_code
from scripts import validate_play_aab


class PlayVersionTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tracks = [
            {
                "track": "internal",
                "releases": [
                    {
                        "status": "completed",
                        "versionCodes": ["20217"],
                        "inAppUpdatePriority": 4,
                    },
                ],
            },
            {
                "track": "production",
                "releases": [
                    {"status": "completed", "versionCodes": ["20213"]},
                ],
            },
        ]

    def test_summarizes_all_tracks(self) -> None:
        per_track, current_max = play_next_version_code.summarize_tracks(self.tracks)

        self.assertEqual({"internal": 20217, "production": 20213}, per_track)
        self.assertEqual(20217, current_max)

    def test_finds_exact_internal_release_and_status(self) -> None:
        self.assertTrue(
            play_next_version_code.expected_release_exists(
                self.tracks,
                "internal",
                20217,
                "completed",
                4,
            )
        )
        self.assertFalse(
            play_next_version_code.expected_release_exists(
                self.tracks,
                "internal",
                20218,
                "completed",
                4,
            )
        )

    def test_rejects_exact_release_with_wrong_update_priority(self) -> None:
        self.assertFalse(
            play_next_version_code.expected_release_exists(
                self.tracks,
                "internal",
                20217,
                "completed",
                0,
            )
        )

    def test_treats_missing_update_priority_as_zero(self) -> None:
        self.assertTrue(
            play_next_version_code.expected_release_exists(
                self.tracks,
                "production",
                20213,
                "completed",
                0,
            )
        )


class ValidateAabTest(unittest.TestCase):
    def test_debug_uses_test_ads_and_release_uses_production_ads(self) -> None:
        fixed_size_banner_id = "ca-app-pub-3940256099942544/6300978111"
        anchored_adaptive_banner_id = "ca-app-pub-3940256099942544/9214589741"
        root = Path(__file__).resolve().parents[2]
        build_gradle = (root / "androidApp/build.gradle.kts").read_text(
            encoding="utf-8"
        )
        fastfile = (root / "fastlane/Fastfile").read_text(encoding="utf-8")
        debug_config, release_and_later = build_gradle.split(
            'getByName("debug")', maxsplit=1
        )[1].split('getByName("release")', maxsplit=1)

        self.assertEqual(fixed_size_banner_id.encode(), validate_play_aab.TEST_BANNER_ID)
        self.assertIn(validate_play_aab.TEST_REWARDED_ID.decode(), debug_config)
        self.assertIn(fixed_size_banner_id, debug_config)
        self.assertNotIn(validate_play_aab.PRODUCTION_REWARDED_ID.decode(), debug_config)
        self.assertNotIn(validate_play_aab.PRODUCTION_BANNER_ID.decode(), debug_config)
        self.assertIn(validate_play_aab.PRODUCTION_REWARDED_ID.decode(), release_and_later)
        self.assertIn(validate_play_aab.PRODUCTION_BANNER_ID.decode(), release_and_later)
        self.assertNotIn(validate_play_aab.TEST_REWARDED_ID.decode(), release_and_later)
        self.assertNotIn(fixed_size_banner_id, release_and_later)
        self.assertNotIn("bandalart.useTestAds", build_gradle)
        self.assertNotIn("bandalart.useTestAds", fastfile)
        self.assertNotIn(anchored_adaptive_banner_id, build_gradle)

    def test_validates_exact_production_package(self) -> None:
        validate_play_aab.verify_manifest_xml(
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
            'package="com.nexters.bandalart" android:versionName="2.2.18" '
            'android:versionCode="20218"></manifest>',
            "2.2.18",
            20218,
        )
        with self.assertRaisesRegex(ValueError, "production package"):
            validate_play_aab.verify_manifest_xml(
                '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                'package="com.nexters.bandalart.dev" android:versionName="2.2.18" '
                'android:versionCode="20218"></manifest>',
                "2.2.18",
                20218,
            )

    def test_rejects_manifest_version_mismatch(self) -> None:
        manifest = (
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
            'package="com.nexters.bandalart" android:versionName="2.2.17" '
            'android:versionCode="20217"></manifest>'
        )
        with self.assertRaisesRegex(ValueError, "versionName"):
            validate_play_aab.verify_manifest_xml(manifest, "2.2.18", 20218)

    def test_validates_required_bundle_content(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            aab = root / "app.aab"
            with zipfile.ZipFile(aab, "w") as archive:
                archive.writestr("base/manifest/AndroidManifest.xml", b"manifest")
                archive.writestr(
                    "base/dex/classes.dex",
                    validate_play_aab.PRODUCTION_REWARDED_ID
                    + b"\0"
                    + validate_play_aab.PRODUCTION_BANNER_ID
                    + b"\0"
                    + validate_play_aab.REQUIRED_NAMESPACE[0],
                )

            validate_play_aab.verify_archive(aab)

    def test_rejects_test_rewarded_id(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            aab = Path(directory) / "app.aab"
            with zipfile.ZipFile(aab, "w") as archive:
                archive.writestr("base/manifest/AndroidManifest.xml", b"manifest")
                archive.writestr(
                    "base/resources.pb",
                    validate_play_aab.TEST_REWARDED_ID
                    + validate_play_aab.PRODUCTION_REWARDED_ID
                    + validate_play_aab.PRODUCTION_BANNER_ID
                    + validate_play_aab.REQUIRED_NAMESPACE[0],
                )

            with self.assertRaisesRegex(ValueError, "test rewarded ad ID"):
                validate_play_aab.verify_archive(aab)

    def test_rejects_test_banner_id(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            aab = Path(directory) / "app.aab"
            with zipfile.ZipFile(aab, "w") as archive:
                archive.writestr("base/manifest/AndroidManifest.xml", b"manifest")
                archive.writestr(
                    "base/resources.pb",
                    validate_play_aab.PRODUCTION_REWARDED_ID
                    + validate_play_aab.TEST_BANNER_ID
                    + validate_play_aab.PRODUCTION_BANNER_ID
                    + validate_play_aab.REQUIRED_NAMESPACE[0],
                )

            with self.assertRaisesRegex(ValueError, "test banner ad ID"):
                validate_play_aab.verify_archive(aab)

    def test_requires_production_rewarded_id(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            aab = Path(directory) / "app.aab"
            with zipfile.ZipFile(aab, "w") as archive:
                archive.writestr("base/manifest/AndroidManifest.xml", b"manifest")
                archive.writestr(
                    "base/resources.pb",
                    validate_play_aab.PRODUCTION_BANNER_ID
                    + validate_play_aab.REQUIRED_NAMESPACE[0],
                )

            with self.assertRaisesRegex(ValueError, "production rewarded ad ID"):
                validate_play_aab.verify_archive(aab)

    def test_requires_production_banner_id(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            aab = Path(directory) / "app.aab"
            with zipfile.ZipFile(aab, "w") as archive:
                archive.writestr("base/manifest/AndroidManifest.xml", b"manifest")
                archive.writestr(
                    "base/resources.pb",
                    validate_play_aab.PRODUCTION_REWARDED_ID
                    + validate_play_aab.REQUIRED_NAMESPACE[0],
                )

            with self.assertRaisesRegex(ValueError, "production banner ad ID"):
                validate_play_aab.verify_archive(aab)


if __name__ == "__main__":
    unittest.main()
