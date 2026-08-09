module Bandalart
  class ProvisioningProfile
    def self.expiration_time(profile)
      value = profile["ExpirationDate"]
      return value if value.is_a?(Time)
      return value.to_time if value.respond_to?(:to_time)

      nil
    end
  end
end
