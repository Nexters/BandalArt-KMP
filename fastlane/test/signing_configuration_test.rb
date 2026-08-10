fastfile = File.read(File.expand_path("../Fastfile", __dir__))

if fastfile.include?("PROVISIONING_PROFILE_SPECIFIER=")
  raise "provisioning profile must not be passed globally to Swift Package targets"
end

unless fastfile.include?("IOS_BUNDLE_ID => profile_name") &&
       fastfile.include?("IOS_WIDGET_BUNDLE_ID => widget_profile_name")
  raise "app and widget provisioning profile mappings are required"
end

unless fastfile.include?('targets: ["iosApp"]') && fastfile.include?('targets: ["BandalartWidget"]')
  raise "manual signing must be scoped to the app and widget targets"
end

unless fastfile.include?('build_configurations: ["Release"]')
  raise "manual signing must be scoped to the Release configuration"
end

unless fastfile.include?("required_app_group: IOS_APP_GROUP_ID")
  raise "app and widget profiles must include the shared App Group"
end

unless fastfile.include?('CURRENT_PROJECT_VERSION=#{build_number} MARKETING_VERSION=#{version}')
  raise "host app and widget must receive the same release version"
end

puts "iOS signing profile scope test passed"
