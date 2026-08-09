fastfile = File.read(File.expand_path("../Fastfile", __dir__))

if fastfile.include?("PROVISIONING_PROFILE_SPECIFIER=")
  raise "provisioning profile must not be passed globally to Swift Package targets"
end

unless fastfile.include?("provisioningProfiles: { IOS_BUNDLE_ID => profile_name }")
  raise "app bundle provisioning profile mapping is missing"
end

unless fastfile.include?('targets: ["iosApp"]')
  raise "manual signing must be scoped to the iosApp target"
end

unless fastfile.include?('build_configurations: ["Release"]')
  raise "manual signing must be scoped to the Release configuration"
end

puts "iOS signing profile scope test passed"
