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


class IosWidgetProjectTest(unittest.TestCase):
    def test_widget_target_is_embedded_with_expected_runtime_contract(self):
        project = PROJECT.read_text()

        self.assertIn('BandalartWidget.appex in Embed App Extensions', project)
        self.assertIn('productType = "com.apple.product-type.app-extension";', project)
        self.assertIn('PRODUCT_BUNDLE_IDENTIFIER = com.nexters.bandalart.iosApp.widget;', project)
        self.assertIn('IPHONEOS_DEPLOYMENT_TARGET = 17.0;', project)
        self.assertIn('APPLICATION_EXTENSION_API_ONLY = YES;', project)
        self.assertIn(':iosWidgetShared:embedAndSignAppleFrameworkForXcode', project)

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


if __name__ == "__main__":
    unittest.main()
