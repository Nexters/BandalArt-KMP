fastfile = File.read(File.expand_path("../Fastfile", __dir__))

if fastfile.include?("PROVISIONING_PROFILE_SPECIFIER=")
  raise "provisioning profile must not be passed globally to Swift Package targets"
end

unless fastfile.include?("provisioningProfiles: { IOS_BUNDLE_ID => profile_name }")
  raise "app bundle provisioning profile mapping is missing"
end

puts "iOS signing profile scope test passed"
