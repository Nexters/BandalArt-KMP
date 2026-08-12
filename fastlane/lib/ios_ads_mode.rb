module Bandalart
  module IosAdsMode
    TEST = "test"
    PRODUCTION = "production"
    VALID_MODES = [TEST, PRODUCTION].freeze

    def self.compilation_condition(mode:)
      unless VALID_MODES.include?(mode)
        raise ArgumentError, "IOS_ADS_MODE must be test or production"
      end

      return nil if mode == PRODUCTION

      "SWIFT_ACTIVE_COMPILATION_CONDITIONS='$(inherited) BANDALART_TEST_ADS'"
    end
  end
end
