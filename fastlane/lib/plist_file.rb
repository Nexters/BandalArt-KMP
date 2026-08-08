require "open3"
require "plist"

module Bandalart
  class PlistFile
    class Error < StandardError; end

    def self.read(path)
      xml, stderr, status = Open3.capture3(
        "/usr/bin/plutil",
        "-convert",
        "xml1",
        "-o",
        "-",
        path,
      )
      raise Error, "Unable to decode plist: #{stderr.lines.last&.strip}" unless status.success?

      Plist.parse_xml(xml)
    rescue ArgumentError => error
      raise Error, "Unable to parse plist: #{error.class}"
    end
  end
end
