repo_root = File.expand_path("../..", __dir__)
ci_workflow = File.read(File.join(repo_root, ".github/workflows/android-ci.yml"))
seed_workflow = File.read(File.join(repo_root, ".github/workflows/ios-cache-seed.yml"))
release_workflow = File.read(File.join(repo_root, ".github/workflows/release-cd.yml"))
fastfile = File.read(File.join(repo_root, "fastlane/Fastfile"))
gradle_properties = File.read(File.join(repo_root, "gradle.properties"))
kmp_cache_script = File.read(File.join(repo_root, "fastlane/scripts/prepare_cached_kmp_frameworks.rb"))

[ci_workflow, release_workflow].each do |workflow|
  unless workflow.include?("IOS_DERIVED_DATA_PATH") &&
         workflow.include?("actions/cache/restore@") &&
         workflow.include?("actions/cache/save@") &&
         workflow.include?("~/.konan") &&
         workflow.include?("/SourcePackages") &&
         workflow.include?("/Build")
    raise "iOS workflows must restore and save Kotlin/Native, SwiftPM, and Xcode build caches"
  end
end

unless ci_workflow.include?("Detect iOS build inputs") &&
       ci_workflow.include?(".github/workflows/ios-cache-seed.yml") &&
       ci_workflow.include?("needs.changes.outputs.ios == 'true'") &&
       ci_workflow.match?(/success\|skipped/)
  raise "PR CI must skip the iOS build only when iOS inputs are unchanged"
end

unless ci_workflow.include?("-showBuildTimingSummary") &&
       ci_workflow.include?("Report iOS build performance") &&
       release_workflow.include?("Report iOS release performance")
  raise "iOS CI and CD must report cache status and build timing"
end

unless fastfile.include?("clean: false") &&
       fastfile.include?("derived_data_path: derived_data_path") &&
       fastfile.include?("-showBuildTimingSummary") &&
       fastfile.include?('Archive: `#{archive_duration_seconds}s`') &&
       fastfile.include?('Upload and processing: `#{upload_duration_seconds}s`')
  raise "Fastlane must preserve DerivedData and report archive and upload timing"
end

unless gradle_properties.include?("org.gradle.caching=true")
  raise "Gradle build cache must be enabled for Kotlin/Native task reuse"
end

unless ci_workflow.include?("cache-read-only: false")
  raise "PR iOS CI must persist Gradle caches for Kotlin/Native task reuse"
end

unless ci_workflow.include?("Reuse cached Kotlin frameworks") &&
       ci_workflow.include?("Restore KMP framework cache") &&
       ci_workflow.include?("ios-kmp-frameworks-v2-") &&
       ci_workflow.include?("composeApp/build/xcode-frameworks/Release") &&
       ci_workflow.include?("iosWidgetShared/build/xcode-frameworks/Release") &&
       ci_workflow.include?("steps.kmp-framework-cache.outputs.cache-hit") &&
       ci_workflow.include?("OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED") &&
       kmp_cache_script.include?("ComposeApp.framework") &&
       kmp_cache_script.include?("IosWidgetShared.framework")
  raise "Exact iOS build cache hits must skip duplicate Kotlin framework builds"
end

unless seed_workflow.include?("iOS Cache Seed") &&
       seed_workflow.include?("branches:") &&
       seed_workflow.include?("- main") &&
       seed_workflow.include?("workflow_dispatch:") &&
       seed_workflow.include?("Restore KMP framework cache") &&
       seed_workflow.include?("Save KMP framework cache") &&
       seed_workflow.include?("ios-kmp-frameworks-v2-") &&
       seed_workflow.include?("composeApp/build/xcode-frameworks/Release") &&
       seed_workflow.include?("iosWidgetShared/build/xcode-frameworks/Release") &&
       seed_workflow.include?("OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED")
  raise "Trusted main builds must seed the shared iOS and KMP caches"
end

unless release_workflow.include?("Reuse cached Kotlin frameworks") &&
       release_workflow.include?("ios-device-build-v2-") &&
       release_workflow.include?("composeApp/build/xcode-frameworks/Release") &&
       release_workflow.include?("iosWidgetShared/build/xcode-frameworks/Release") &&
       release_workflow.include?("IOS_REUSE_CACHED_KMP_FRAMEWORKS") &&
       fastfile.include?('ENV["IOS_REUSE_CACHED_KMP_FRAMEWORKS"] == "true"') &&
       fastfile.include?("OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES")
  raise "Exact TestFlight build cache hits must reuse cached Kotlin frameworks"
end

puts "iOS CI cache configuration test passed"
