project_path = ARGV.fetch(0)
project = File.read(project_path)

frameworks = {
  "composeApp" => "ComposeApp.framework",
  "iosWidgetShared" => "IosWidgetShared.framework",
}

frameworks.each do |module_name, framework_name|
  command = "./gradlew :#{module_name}:embedAndSignAppleFrameworkForXcode"
  occurrences = project.scan(command).length
  raise "Expected exactly one #{command} build phase, found #{occurrences}" unless occurrences == 1

  guard =
    [
      'if [ \"YES\" = \"$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED\" ] && ' \
        '[ -d \"$BUILT_PRODUCTS_DIR/' + framework_name + '\" ]; then',
      'echo \"Reusing cached ' + framework_name + '\"',
      "exit 0",
      "fi",
    ].join('\\n') + '\\n'
  project.sub!(command, guard + command)
end

File.write(project_path, project)
