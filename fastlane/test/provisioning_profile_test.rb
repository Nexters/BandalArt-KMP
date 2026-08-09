require "date"

require_relative "../lib/provisioning_profile"

parsed_expiration = DateTime.new(2027, 8, 10, 0, 0, 0, "+0")
profile = { "ExpirationDate" => parsed_expiration }

expiration_time = Bandalart::ProvisioningProfile.expiration_time(profile)
raise "expiration should normalize to Time" unless expiration_time.is_a?(Time)
raise "expiration instant changed during normalization" unless expiration_time.to_i == parsed_expiration.to_time.to_i
raise "Time values should remain unchanged" unless Bandalart::ProvisioningProfile.expiration_time("ExpirationDate" => expiration_time).equal?(expiration_time)
raise "missing expiration should stay nil" unless Bandalart::ProvisioningProfile.expiration_time({}).nil?

puts "ProvisioningProfile expiration normalization test passed"
