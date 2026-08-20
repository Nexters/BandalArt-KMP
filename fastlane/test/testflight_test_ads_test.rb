require_relative "../lib/ios_ads_mode"

repo_root = File.expand_path("../..", __dir__)
fastfile = File.read(File.join(repo_root, "fastlane/Fastfile"))
workflow = File.read(File.join(repo_root, ".github/workflows/release-cd.yml"))
ads_bridge = File.read(
  File.join(repo_root, "iosApp/iosApp/IosAdsBridgeImpl.swift"),
)

test_xcargs = Bandalart::IosAdsMode.compilation_condition(
  mode: "test",
)
unless test_xcargs.include?("BANDALART_TEST_ADS")
  raise "TestFlight test mode must enable the test ads compilation condition"
end

production_xcargs = Bandalart::IosAdsMode.compilation_condition(
  mode: "production",
)
unless production_xcargs.nil?
  raise "App Store production candidate must not enable test ads"
end

begin
  Bandalart::IosAdsMode.compilation_condition(mode: "invalid")
  raise "Invalid iOS ad mode must fail before archive"
rescue ArgumentError => error
  raise unless error.message == "IOS_ADS_MODE must be test or production"
end

unless ads_bridge.include?("#if DEBUG || BANDALART_TEST_ADS")
  raise "iOS ads bridge must use test ad units for TestFlight builds"
end

beta_lane = fastfile.match(/lane :beta do(?<body>.*?)^  ensure$/m)&.named_captures&.fetch("body", nil)
unless beta_lane&.include?('ads_mode = required_env!("IOS_ADS_MODE")')
  raise "Fastlane must require an explicit iOS ad mode"
end

unless beta_lane.include?('what_to_test_#{ads_mode}_ads.txt')
  raise "Fastlane must select TestFlight ad notes for the active iOS ad mode"
end

test_notes = File.read(File.join(repo_root, "fastlane/metadata/ios/what_to_test_test_ads.txt"))
production_notes = File.read(File.join(repo_root, "fastlane/metadata/ios/what_to_test_production_ads.txt"))
unless test_notes.include?("테스트 배너") && test_notes.include?("운영 광고를 사용하지 않습니다")
  raise "Test mode notes must identify Google test ads"
end
unless production_notes.include?("운영 광고 릴리스 후보") && production_notes.include?("광고를 클릭")
  raise "Production mode notes must warn TestFlight testers about live ads"
end

unless workflow.include?("IOS_ADS_MODE: ${{ inputs.ios_ads_mode }}")
  raise "Release CD must pass the selected iOS ad mode to Fastlane"
end

unless workflow.match?(/ios_ads_mode:.*?default: production.*?- test.*?- production/m)
  raise "Release CD must default to production ads and offer an explicit test mode"
end

puts "iOS TestFlight ad mode configuration test passed"
