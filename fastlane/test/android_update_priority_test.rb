repo_root = File.expand_path("../..", __dir__)
fastfile = File.read(File.join(repo_root, "fastlane/Fastfile"))
workflow = File.read(File.join(repo_root, ".github/workflows/release-cd.yml"))
android_build = File.read(File.join(repo_root, "androidApp/build.gradle.kts"))

unless workflow.include?("android_update_priority:") &&
    workflow.include?("ANDROID_UPDATE_PRIORITY: ${{ inputs.android_update_priority }}")
  raise "Release CD must expose and forward the Android update priority"
end

unless android_build.include?("updatePriority.set(0)")
  raise "Gradle Play Publisher must default normal releases to priority 0"
end

unless fastfile.include?('required_env!("ANDROID_UPDATE_PRIORITY")') &&
    fastfile.include?("--update-priority") &&
    fastfile.include?("--expect-update-priority")
  raise "Fastlane must publish and verify the selected update priority"
end

puts "Android update priority release contract test passed"
