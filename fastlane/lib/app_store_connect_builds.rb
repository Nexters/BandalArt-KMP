require "base64"
require "json"
require "jwt"
require "net/http"
require "openssl"
require "timeout"
require "uri"

module Bandalart
  class AppStoreConnectBuilds
    class Error < StandardError; end

    BASE_URL = "https://api.appstoreconnect.apple.com"
    MAX_PAGES = 100

    def initialize(key_id:, key_content:, app_id:, http_get: nil, token_provider: nil)
      @key_id = key_id
      @key_content = key_content
      @app_id = app_id
      @http_get = http_get || method(:perform_get)
      @token_provider = token_provider || method(:create_token)
    end

    def latest_build_number(marketing_version)
      build_numbers(marketing_version).max || 0
    end

    def build_number_exists?(marketing_version, build_number)
      build_numbers(marketing_version).include?(build_number.to_i)
    end

    def build_numbers(marketing_version)
      token = @token_provider.call
      pre_release_query = URI.encode_www_form(
        "filter[app]" => @app_id,
        "filter[platform]" => "IOS",
        "filter[version]" => marketing_version,
        "limit" => "200",
      )
      pre_release_versions = collection("/v1/preReleaseVersions?#{pre_release_query}", token)

      builds = pre_release_versions.flat_map do |pre_release_version|
        builds_query = URI.encode_www_form(
          "filter[preReleaseVersion]" => pre_release_version.fetch("id"),
          "limit" => "200",
        )
        collection("/v1/builds?#{builds_query}", token)
      end
      builds.map { |item| item.dig("attributes", "version").to_i }
    rescue KeyError, JSON::ParserError => error
      raise Error, "Invalid App Store Connect API response: #{error.class}"
    end

    private

    def create_token
      private_key = OpenSSL::PKey.read(Base64.decode64(@key_content))
      issued_at = Time.now.to_i
      JWT.encode(
        {
          aud: "appstoreconnect-v1",
          exp: issued_at + 900,
          iat: issued_at,
          sub: "user",
        },
        private_key,
        "ES256",
        { kid: @key_id, typ: "JWT" },
      )
    rescue OpenSSL::PKey::PKeyError, ArgumentError
      raise Error, "Invalid Individual App Store Connect private key"
    end

    def collection(path, token)
      items = []
      next_path = path
      pages = 0
      while next_path
        pages += 1
        raise Error, "App Store Connect pagination exceeded #{MAX_PAGES} pages" if pages > MAX_PAGES

        validate_origin!(next_path)
        response = @http_get.call(next_path, token)
        items.concat(response.fetch("data", []))
        next_path = response.dig("links", "next")
      end
      items
    end

    def validate_origin!(path)
      expected = URI(BASE_URL)
      resolved = URI.join(BASE_URL, path)
      return if resolved.scheme == expected.scheme &&
                resolved.host == expected.host &&
                resolved.port == expected.port

      raise Error, "App Store Connect pagination returned an unexpected origin"
    rescue URI::InvalidURIError
      raise Error, "App Store Connect pagination returned an invalid URL"
    end

    def perform_get(path, token)
      uri = URI.join(BASE_URL, path)
      request = Net::HTTP::Get.new(uri)
      request["Authorization"] = "Bearer #{token}"
      response = Net::HTTP.start(uri.hostname, uri.port, use_ssl: true) do |http|
        http.open_timeout = 15
        http.read_timeout = 30
        http.request(request)
      end
      raise Error, "App Store Connect API request failed with HTTP #{response.code}" unless response.is_a?(Net::HTTPSuccess)

      JSON.parse(response.body)
    rescue JSON::ParserError, SocketError, SystemCallError, Timeout::Error => error
      raise Error, "App Store Connect API request failed: #{error.class}"
    end
  end
end
