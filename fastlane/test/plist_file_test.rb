require "tmpdir"

require_relative "../lib/plist_file"

Dir.mktmpdir("bandalart-plist-test-") do |directory|
  path = File.join(directory, "Info.plist")
  File.write(
    path,
    <<~PLIST,
      <?xml version="1.0" encoding="UTF-8"?>
      <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
      <plist version="1.0">
      <dict>
        <key>CFBundleIdentifier</key>
        <string>com.nexters.bandalart.iosApp</string>
      </dict>
      </plist>
    PLIST
  )
  raise "could not create binary plist fixture" unless system("/usr/bin/plutil", "-convert", "binary1", path)

  parsed = Bandalart::PlistFile.read(path)
  raise "binary plist was not parsed" unless parsed["CFBundleIdentifier"] == "com.nexters.bandalart.iosApp"
end

puts "PlistFile binary fixture test passed"
