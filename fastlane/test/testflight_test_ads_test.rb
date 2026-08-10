fastfile = File.read(File.expand_path("../Fastfile", __dir__))
ads_bridge = File.read(
  File.expand_path("../../iosApp/iosApp/IosAdsBridgeImpl.swift", __dir__),
)

unless fastfile.include?(
  "SWIFT_ACTIVE_COMPILATION_CONDITIONS='$(inherited) BANDALART_TEST_ADS'",
)
  raise "TestFlight build must enable the test ads compilation condition"
end

unless ads_bridge.include?("#if DEBUG || BANDALART_TEST_ADS")
  raise "iOS ads bridge must use test ad units for TestFlight builds"
end

puts "iOS TestFlight test ads configuration test passed"
