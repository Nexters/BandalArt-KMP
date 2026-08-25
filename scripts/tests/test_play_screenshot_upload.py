from __future__ import annotations

import struct
import tempfile
import unittest
from pathlib import Path

from scripts import upload_play_screenshots


def write_png(
    path: Path,
    width: int,
    height: int,
    *,
    color_type: int = 2,
) -> None:
    header = struct.pack(">IIBBBBB", width, height, 8, color_type, 0, 0, 0)
    path.write_bytes(
        upload_play_screenshots.PNG_SIGNATURE
        + struct.pack(">I", len(header))
        + b"IHDR"
        + header
        + b"\x00\x00\x00\x00",
    )


def write_jpeg(path: Path, width: int, height: int) -> None:
    frame = (
        b"\xff\xc0"
        + struct.pack(">H", 17)
        + b"\x08"
        + struct.pack(">HH", height, width)
        + b"\x03\x01\x11\x00\x02\x11\x00\x03\x11\x00"
    )
    path.write_bytes(b"\xff\xd8" + frame + b"\xff\xd9")


class AssetDiscoveryTest(unittest.TestCase):
    def test_discovers_ordered_png_and_jpeg_screenshots(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "ko-KR" / "phoneScreenshots"
            target.mkdir(parents=True)
            write_jpeg(target / "02-list.jpg", 1080, 1920)
            write_png(target / "01-hero.png", 1080, 1920)

            targets = upload_play_screenshots.discover_targets(root)

            self.assertEqual(1, len(targets))
            self.assertEqual("ko-KR", targets[0].locale)
            self.assertEqual("phoneScreenshots", targets[0].image_type)
            self.assertEqual(
                ["01-hero.png", "02-list.jpg"],
                [image.path.name for image in targets[0].images],
            )
            self.assertEqual(
                ["image/png", "image/jpeg"],
                [image.info.mime_type for image in targets[0].images],
            )

    def test_rejects_png_with_alpha(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "ko-KR" / "phoneScreenshots"
            target.mkdir(parents=True)
            write_png(target / "01-alpha.png", 1080, 1920, color_type=6)

            with self.assertRaisesRegex(
                upload_play_screenshots.AssetValidationError,
                "without alpha",
            ):
                upload_play_screenshots.discover_targets(root)

    def test_rejects_non_consecutive_file_order(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "en-US" / "phoneScreenshots"
            target.mkdir(parents=True)
            write_png(target / "01-hero.png", 1080, 1920)
            write_png(target / "03-list.png", 1080, 1920)

            with self.assertRaisesRegex(
                upload_play_screenshots.AssetValidationError,
                "consecutive from 01",
            ):
                upload_play_screenshots.discover_targets(root)

    def test_rejects_screenshot_aspect_ratio_over_two_to_one(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "ja-JP" / "phoneScreenshots"
            target.mkdir(parents=True)
            write_png(target / "01-too-tall.png", 1000, 2100)

            with self.assertRaisesRegex(
                upload_play_screenshots.AssetValidationError,
                "twice the shortest side",
            ):
                upload_play_screenshots.discover_targets(root)

    def test_feature_graphic_requires_exact_dimensions_and_one_file(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "ko-KR" / "featureGraphic"
            target.mkdir(parents=True)
            write_png(target / "01-feature.png", 1024, 500)

            targets = upload_play_screenshots.discover_targets(root)

            self.assertEqual(
                (1024, 500),
                (
                    targets[0].images[0].info.width,
                    targets[0].images[0].info.height,
                ),
            )

    def test_filters_selected_locale_and_image_type(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for locale in ("en-US", "ko-KR"):
                phone = root / locale / "phoneScreenshots"
                feature = root / locale / "featureGraphic"
                phone.mkdir(parents=True)
                feature.mkdir(parents=True)
                write_png(phone / "01-hero.png", 1080, 1920)
                write_png(feature / "01-feature.png", 1024, 500)

            targets = upload_play_screenshots.discover_targets(
                root,
                locales=["ko-KR"],
                image_types=["phoneScreenshots"],
            )

            self.assertEqual(
                [("ko-KR", "phoneScreenshots")],
                [(target.locale, target.image_type) for target in targets],
            )


class FakeRequest:
    def __init__(self, calls: list[tuple], name: str, result=None, fail=False) -> None:
        self.calls = calls
        self.name = name
        self.result = result
        self.fail = fail

    def execute(self):
        self.calls.append(("execute", self.name))
        if self.fail:
            raise RuntimeError(f"{self.name} failed")
        return self.result


class FakeImagesApi:
    def __init__(self, calls: list[tuple], fail_upload: bool) -> None:
        self.calls = calls
        self.fail_upload = fail_upload

    def deleteall(self, **kwargs):
        self.calls.append(("deleteall", kwargs))
        return FakeRequest(self.calls, "deleteall", result={"deleted": []})

    def upload(self, **kwargs):
        self.calls.append(("upload", kwargs))
        return FakeRequest(self.calls, "upload", result={}, fail=self.fail_upload)


class FakeEditsApi:
    def __init__(self, calls: list[tuple], fail_upload: bool) -> None:
        self.calls = calls
        self.images_api = FakeImagesApi(calls, fail_upload)

    def insert(self, **kwargs):
        self.calls.append(("insert", kwargs))
        return FakeRequest(self.calls, "insert", result={"id": "edit-1"})

    def images(self):
        return self.images_api

    def validate(self, **kwargs):
        self.calls.append(("validate", kwargs))
        return FakeRequest(self.calls, "validate", result={})

    def commit(self, **kwargs):
        self.calls.append(("commit", kwargs))
        return FakeRequest(self.calls, "commit", result={})

    def delete(self, **kwargs):
        self.calls.append(("delete", kwargs))
        return FakeRequest(self.calls, "delete", result={})


class FakeService:
    def __init__(self, fail_upload: bool = False) -> None:
        self.calls: list[tuple] = []
        self.edits_api = FakeEditsApi(self.calls, fail_upload)

    def edits(self):
        return self.edits_api


class EditLifecycleTest(unittest.TestCase):
    def setUp(self) -> None:
        self.image = upload_play_screenshots.UploadImage(
            path=Path("01-hero.png"),
            info=upload_play_screenshots.ImageInfo(1080, 1920, "image/png"),
        )
        self.target = upload_play_screenshots.UploadTarget(
            locale="ko-KR",
            image_type="phoneScreenshots",
            images=(self.image,),
        )

    def test_commits_after_delete_upload_and_validate(self) -> None:
        service = FakeService()

        upload_play_screenshots.replace_images(
            service,
            lambda path, **kwargs: (path, kwargs),
            upload_play_screenshots.PACKAGE_NAME,
            [self.target],
        )

        operations = [call[0] for call in service.calls if call[0] != "execute"]
        self.assertEqual(
            ["insert", "deleteall", "upload", "validate", "commit"],
            operations,
        )
        self.assertNotIn("delete", operations)

    def test_deletes_uncommitted_edit_after_upload_failure(self) -> None:
        service = FakeService(fail_upload=True)

        with self.assertRaisesRegex(RuntimeError, "upload failed"):
            upload_play_screenshots.replace_images(
                service,
                lambda path, **kwargs: (path, kwargs),
                upload_play_screenshots.PACKAGE_NAME,
                [self.target],
            )

        operations = [call[0] for call in service.calls if call[0] != "execute"]
        self.assertEqual(["insert", "deleteall", "upload", "delete"], operations)


class WorkflowContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.root = Path(__file__).resolve().parents[2]
        self.screenshot_workflow = (
            self.root / ".github/workflows/play-store-screenshots.yml"
        ).read_text(encoding="utf-8")
        self.release_workflow = (
            self.root / ".github/workflows/release-cd.yml"
        ).read_text(encoding="utf-8")
        self.android_ci = (
            self.root / ".github/workflows/android-ci.yml"
        ).read_text(encoding="utf-8")

    def test_requires_main_and_explicit_confirmation(self) -> None:
        self.assertIn('test "$UPLOAD_REF" = "refs/heads/main"', self.screenshot_workflow)
        self.assertIn('test "$UPLOAD_CONFIRMED" = "true"', self.screenshot_workflow)
        self.assertIn("--commit", self.screenshot_workflow)

    def test_serializes_with_release_cd_and_cleans_credentials(self) -> None:
        concurrency = "group: release-cd"
        self.assertIn(concurrency, self.screenshot_workflow)
        self.assertIn(concurrency, self.release_workflow)
        self.assertIn("if: always()", self.screenshot_workflow)
        self.assertIn("rm -f playstore/service-account-key.json", self.screenshot_workflow)

    def test_android_ci_validates_workflow_and_unit_tests(self) -> None:
        self.assertIn(".github/workflows/play-store-screenshots.yml", self.android_ci)
        self.assertIn("scripts.tests.test_play_screenshot_upload", self.android_ci)


if __name__ == "__main__":
    unittest.main()
