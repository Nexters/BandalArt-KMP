require_relative "../lib/app_store_connect_builds"

responses = {
  "/v1/preReleaseVersions?filter%5Bapp%5D=app-id&filter%5Bplatform%5D=IOS&filter%5Bversion%5D=1.0.1&limit=200" => {
    "data" => [{ "id" => "train-a" }],
    "links" => { "next" => "https://api.appstoreconnect.apple.com/pre-release-page-2" },
  },
  "https://api.appstoreconnect.apple.com/pre-release-page-2" => {
    "data" => [{ "id" => "train-b" }],
    "links" => { "next" => nil },
  },
  "/v1/builds?filter%5BpreReleaseVersion%5D=train-a&limit=200" => {
    "data" => [{ "attributes" => { "version" => "2", "expired" => true } }],
    "links" => { "next" => "https://api.appstoreconnect.apple.com/build-page-2" },
  },
  "https://api.appstoreconnect.apple.com/build-page-2" => {
    "data" => [{ "attributes" => { "version" => "9", "expired" => true } }],
    "links" => { "next" => nil },
  },
  "/v1/builds?filter%5BpreReleaseVersion%5D=train-b&limit=200" => {
    "data" => [{ "attributes" => { "version" => "4", "expired" => false } }],
    "links" => { "next" => nil },
  },
}
requests = []
client = Bandalart::AppStoreConnectBuilds.new(
  key_id: "key-id",
  key_content: "unused",
  app_id: "app-id",
  token_provider: -> { "token" },
  http_get: lambda do |path, token|
    requests << [path, token]
    responses.fetch(path)
  end,
)

raise "did not include the highest expired build" unless client.latest_build_number("1.0.1") == 9
raise "did not follow all pagination links" unless requests.length == 5
raise "did not reuse the token" unless requests.all? { |_path, token| token == "token" }

requests.clear
raise "did not find the exact build" unless client.build_number_exists?("1.0.1", 4)
raise "accepted a missing build" if client.build_number_exists?("1.0.1", 8)

cross_origin_client = Bandalart::AppStoreConnectBuilds.new(
  key_id: "key-id",
  key_content: "unused",
  app_id: "app-id",
  token_provider: -> { "token" },
  http_get: lambda do |_path, _token|
    {
      "data" => [],
      "links" => { "next" => "https://example.com/stolen-token" },
    }
  end,
)
begin
  cross_origin_client.latest_build_number("1.0.1")
  raise "accepted a cross-origin pagination URL"
rescue Bandalart::AppStoreConnectBuilds::Error => error
  raise unless error.message.include?("unexpected origin")
end

puts "AppStoreConnectBuilds pagination test passed"
