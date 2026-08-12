import plistlib
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "iosApp/iosApp.xcodeproj/project.pbxproj"
APP_ENTITLEMENTS = ROOT / "iosApp/iosApp/iosApp.entitlements"
WIDGET_ENTITLEMENTS = ROOT / "iosApp/BandalartWidget/BandalartWidget.entitlements"
APP_INFO = ROOT / "iosApp/iosApp/Info.plist"
WIDGET_INFO = ROOT / "iosApp/BandalartWidget/Info.plist"
RELEASE_WORKFLOW = ROOT / ".github/workflows/release-cd.yml"
FASTFILE = ROOT / "fastlane/Fastfile"
WIDGET_SOURCE = ROOT / "iosApp/BandalartWidget/BandalartWidget.swift"
WIDGET_LOCALIZATIONS = ROOT / "iosApp/BandalartWidget"

REQUIRED_WIDGET_STRINGS = {
    "Choose a sub-goal",
    "Add tasks in the app",
    "Choose a Bandalart",
    "Edit this widget after creating a goal in the app.",
    "Bandalart",
    "See your goal progress and complete tasks from the Home Screen.",
    "Bandalart Goal",
    "Selects a Bandalart and sub-goal to display.",
    "Bandalart / Sub-goal",
    "Update a Bandalart task",
    "Marks a Bandalart task as complete or incomplete.",
    "Bandalart ID",
    "Sub-goal ID",
    "Task ID",
    "Completed",
    "2026 Goal",
    "Build healthy routines",
    "Exercise three times",
    "Sleep before midnight",
    "Drink enough water",
}


class IosWidgetProjectTest(unittest.TestCase):
    def test_widget_target_is_embedded_with_expected_runtime_contract(self):
        project = PROJECT.read_text()

        self.assertIn('BandalartWidget.appex in Embed App Extensions', project)
        self.assertIn('productType = "com.apple.product-type.app-extension";', project)
        self.assertIn('PRODUCT_BUNDLE_IDENTIFIER = com.nexters.bandalart.iosApp.widget;', project)
        self.assertIn('IPHONEOS_DEPLOYMENT_TARGET = 17.0;', project)
        self.assertIn('APPLICATION_EXTENSION_API_ONLY = YES;', project)
        self.assertIn(':iosWidgetShared:embedAndSignAppleFrameworkForXcode', project)

    def test_crashlytics_upload_runs_after_widget_embedding(self):
        project = PROJECT.read_text()
        app_target_phases = project.split(
            '226F38552D5A407D00A1512E /* iosApp */ = {', 1
        )[1].split('buildRules = (', 1)[0]

        self.assertLess(
            app_target_phases.index('/* Embed App Extensions */'),
            app_target_phases.index('/* Upload Crashlytics Symbols */'),
        )

    def test_app_and_widget_share_only_the_expected_app_group(self):
        expected = ["group.com.nexters.bandalart"]

        for path in (APP_ENTITLEMENTS, WIDGET_ENTITLEMENTS):
            with self.subTest(path=path):
                with path.open("rb") as file:
                    entitlements = plistlib.load(file)
                self.assertEqual(
                    entitlements["com.apple.security.application-groups"],
                    expected,
                )

    def test_widget_extension_and_deep_link_plists_are_wired(self):
        with WIDGET_INFO.open("rb") as file:
            widget_info = plistlib.load(file)
        self.assertEqual(
            widget_info["NSExtension"]["NSExtensionPointIdentifier"],
            "com.apple.widgetkit-extension",
        )

        with APP_INFO.open("rb") as file:
            app_info = plistlib.load(file)
        schemes = {
            scheme
            for url_type in app_info["CFBundleURLTypes"]
            for scheme in url_type["CFBundleURLSchemes"]
        }
        self.assertIn("bandalart", schemes)

    def test_testflight_pipeline_requires_and_maps_widget_signing(self):
        workflow = RELEASE_WORKFLOW.read_text()
        fastfile = FASTFILE.read_text()

        self.assertIn("IOS_WIDGET_PROVISIONING_PROFILE_BASE64", workflow)
        self.assertIn("IOS_WIDGET_PROVISIONING_PROFILE_PATH", workflow)
        self.assertIn('targets: ["BandalartWidget"]', fastfile)
        self.assertIn("IOS_WIDGET_BUNDLE_ID => widget_profile_name", fastfile)
        self.assertIn("CURRENT_PROJECT_VERSION=#{build_number}", fastfile)

    def test_widget_provider_does_not_replace_an_empty_selection(self):
        source = WIDGET_SOURCE.read_text()

        self.assertIn("let selection = configuration.selection", source)
        self.assertNotIn("BandalartSelectionQuery().suggestedEntities().first", source)

    def test_widget_resources_cover_Korean_English_and_Japanese(self):
        translations = {}
        for language in ("ko", "en", "ja"):
            path = WIDGET_LOCALIZATIONS / f"{language}.lproj/Localizable.strings"
            contents = path.read_text()
            translations[language] = {
                line.split(" = ", 1)[0].strip().strip('"')
                for line in contents.splitlines()
                if " = " in line
            }
            self.assertTrue(
                REQUIRED_WIDGET_STRINGS.issubset(translations[language]),
                f"{language} is missing widget translations",
            )


if __name__ == "__main__":
    unittest.main()
