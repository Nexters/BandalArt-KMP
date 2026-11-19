/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ComposeApp
import GoogleMobileAds
import UIKit
import UserMessagingPlatform

private enum AdUnitID {
#if DEBUG || BANDALART_TEST_ADS
    static let banner = "ca-app-pub-3940256099942544/2435281174"
    static let rewardedBandalartCreation = "ca-app-pub-3940256099942544/1712485313"
    static let rewardedCloudBackup = "ca-app-pub-3940256099942544/1712485313"
#else
    static let banner = "ca-app-pub-5570932833347277/4693940934"
    static let rewardedBandalartCreation = "ca-app-pub-5570932833347277/2754173309"
    static let rewardedCloudBackup = "ca-app-pub-5570932833347277/5251786621"
#endif

    static let rewardedUnitIDs = Set([rewardedBandalartCreation, rewardedCloudBackup])

    static func rewarded(for purpose: CommonRewardedAdPurpose) -> String {
        switch purpose {
        case .bandalartCreation:
            rewardedBandalartCreation
        case .cloudBackup:
            rewardedCloudBackup
        default:
            fatalError("Unsupported rewarded ad purpose")
        }
    }
}

@MainActor
final class IosAdsBridgeImpl: NSObject, @preconcurrency IosAdsBridge, FullScreenContentDelegate {
    private let bannerViews = NSHashTable<IosBannerAdView>.weakObjects()

    private var isInitialized = false
    private var isConsentRequested = false
    private var isMobileAdsStartRequested = false
    private var isAdsUnavailable = false
    private var privacyOptionsRequirementListener: ((KotlinBoolean) -> Void)?
    private var loadingRewardedAdUnitIDs = Set<String>()
    private var rewardedAds: [String: RewardedAd] = [:]
    private var activeRewardedAd: RewardedAd?
    private var activeRewardedAdUnitID: String?
    private var pendingRequestID: Int64?
    private var pendingRewardedAdUnitID: String?
    private var pendingCompletion: ((CommonRewardedAdResult) -> Void)?
    private var didEarnReward = false
    private var isRewardedDismissed = false

    /// 앱이 active가 될 때마다 호출한다. 진행 중이거나 이미 확인을 마쳤으면 무시한다.
    /// 동의 양식은 표시할 view controller가 있어야 하므로 didFinishLaunching이 아니라 active 시점에 시작한다.
    func start() {
        guard !isConsentRequested else { return }
        isConsentRequested = true

        ConsentInformation.shared.requestConsentInfoUpdate(with: consentRequestParameters()) { [weak self] error in
            Task { @MainActor in
                guard let self else { return }
                if let error {
                    NSLog("Consent info update failed: %@", error.localizedDescription)
                    self.finishConsentGathering()
                    return
                }
                ConsentForm.loadAndPresentIfRequired(from: Self.topViewController()) { [weak self] formError in
                    Task { @MainActor in
                        if let formError {
                            NSLog("Consent form failed: %@", formError.localizedDescription)
                        }
                        self?.finishConsentGathering()
                    }
                }
            }
        }
        // 이전 실행에서 받은 동의가 있으면 갱신 응답을 기다리지 않고 광고를 준비한다.
        if ConsentInformation.shared.canRequestAds {
            startMobileAdsIfNeeded()
        }
    }

    func observePrivacyOptionsRequired(onChange: @escaping (KotlinBoolean) -> Void) {
        privacyOptionsRequirementListener = onChange
        onChange(KotlinBoolean(value: isPrivacyOptionsRequired))
    }

    func showPrivacyOptions() {
        ConsentForm.presentPrivacyOptionsForm(from: Self.topViewController()) { [weak self] formError in
            Task { @MainActor in
                if let formError {
                    NSLog("Privacy options form failed: %@", formError.localizedDescription)
                }
                self?.finishConsentGathering()
            }
        }
    }

    private var isPrivacyOptionsRequired: Bool {
        ConsentInformation.shared.privacyOptionsRequirementStatus == .required
    }

    private func finishConsentGathering() {
        privacyOptionsRequirementListener?(KotlinBoolean(value: isPrivacyOptionsRequired))
        if ConsentInformation.shared.canRequestAds {
            startMobileAdsIfNeeded()
        } else if !isMobileAdsStartRequested {
            // 지금은 광고를 요청할 수 없다. 대기 중인 보상형은 실패로 끝내 생성 흐름을 막지 않고,
            // 다음 active 시점에 동의 확인을 다시 시도한다.
            isAdsUnavailable = true
            isConsentRequested = false
            if pendingRequestID != nil, activeRewardedAd == nil {
                finishRewarded(with: .failed)
            }
        }
    }

    private func consentRequestParameters() -> RequestParameters {
        let parameters = RequestParameters()
        #if DEBUG
        // Xcode scheme 환경 변수 UMP_DEBUG_EEA=1이면 시뮬레이터를 EEA 사용자로 보고 동의 양식을 확인한다.
        if ProcessInfo.processInfo.environment["UMP_DEBUG_EEA"] == "1" {
            let debugSettings = DebugSettings()
            debugSettings.geography = .EEA
            parameters.debugSettings = debugSettings
        }
        #endif
        return parameters
    }

    private static func topViewController() -> UIViewController? {
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }

    private func startMobileAdsIfNeeded() {
        guard !isMobileAdsStartRequested else { return }
        isMobileAdsStartRequested = true
        isAdsUnavailable = false

        let requestConfiguration = MobileAds.shared.requestConfiguration
        requestConfiguration.setPublisherFirstPartyIDEnabled(false)

        MobileAds.shared.start { [weak self] _ in
            Task { @MainActor in
                guard let self else { return }
                self.isInitialized = true
                self.bannerViews.allObjects.forEach { $0.loadAdIfNeeded() }
                AdUnitID.rewardedUnitIDs.forEach { self.loadRewardedIfNeeded(adUnitID: $0) }
            }
        }
    }

    func makeBannerView() -> UIView {
        let bannerView = IosBannerAdView(adUnitID: AdUnitID.banner)
        bannerViews.add(bannerView)
        if isInitialized {
            bannerView.loadAdIfNeeded()
        }
        return bannerView
    }

    func showRewarded(
        requestId: Int64,
        purpose: CommonRewardedAdPurpose,
        completion: @escaping (CommonRewardedAdResult) -> Void
    ) {
        guard pendingRequestID == nil, activeRewardedAd == nil, !isAdsUnavailable else {
            completion(.failed)
            return
        }

        pendingRequestID = requestId
        pendingRewardedAdUnitID = AdUnitID.rewarded(for: purpose)
        pendingCompletion = completion
        presentRewardedIfReady()
    }

    func consumeRewarded(requestId: Int64) {
        guard pendingRequestID == requestId else { return }
        pendingCompletion = nil
        if activeRewardedAd == nil {
            pendingRequestID = nil
            pendingRewardedAdUnitID = nil
        }
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        isRewardedDismissed = true
        if didEarnReward {
            finishRewarded(with: .rewarded)
            return
        }

        let requestID = pendingRequestID
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
            guard let self, self.pendingRequestID == requestID, self.activeRewardedAd != nil else {
                return
            }
            self.finishRewarded(with: self.didEarnReward ? .rewarded : .dismissed)
        }
    }

    func ad(
        _ ad: FullScreenPresentingAd,
        didFailToPresentFullScreenContentWithError error: Error
    ) {
        NSLog("Rewarded ad failed to present: %@", error.localizedDescription)
        finishRewarded(with: .failed)
    }

    private func presentRewardedIfReady() {
        guard isInitialized else { return }
        guard let pendingRewardedAdUnitID else { return }
        guard let rewardedAd = rewardedAds.removeValue(forKey: pendingRewardedAdUnitID) else {
            loadRewardedIfNeeded(adUnitID: pendingRewardedAdUnitID)
            return
        }

        activeRewardedAd = rewardedAd
        activeRewardedAdUnitID = pendingRewardedAdUnitID
        didEarnReward = false
        isRewardedDismissed = false
        rewardedAd.present(from: nil) { [weak self] in
            guard let self else { return }
            self.didEarnReward = true
            if self.isRewardedDismissed {
                self.finishRewarded(with: .rewarded)
            }
        }
    }

    private func loadRewardedIfNeeded(adUnitID: String) {
        guard isInitialized,
              rewardedAds[adUnitID] == nil,
              activeRewardedAdUnitID != adUnitID,
              !loadingRewardedAdUnitIDs.contains(adUnitID) else {
            return
        }

        loadingRewardedAdUnitIDs.insert(adUnitID)
        Task { @MainActor [weak self] in
            guard let self else { return }
            do {
                let ad = try await RewardedAd.load(
                    with: adUnitID,
                    request: Request()
                )
                ad.fullScreenContentDelegate = self
                self.rewardedAds[adUnitID] = ad
                self.loadingRewardedAdUnitIDs.remove(adUnitID)
                if self.pendingRewardedAdUnitID == adUnitID {
                    self.presentRewardedIfReady()
                }
            } catch {
                self.loadingRewardedAdUnitIDs.remove(adUnitID)
                NSLog("Rewarded ad failed to load: %@", error.localizedDescription)
                if self.pendingRewardedAdUnitID == adUnitID {
                    self.finishRewarded(with: .failed)
                }
            }
        }
    }

    private func finishRewarded(with result: CommonRewardedAdResult) {
        let completion = pendingCompletion
        let completedAdUnitID = activeRewardedAdUnitID ?? pendingRewardedAdUnitID
        pendingRequestID = nil
        pendingRewardedAdUnitID = nil
        pendingCompletion = nil
        activeRewardedAd = nil
        activeRewardedAdUnitID = nil
        didEarnReward = false
        isRewardedDismissed = false
        completion?(result)
        if let completedAdUnitID {
            loadRewardedIfNeeded(adUnitID: completedAdUnitID)
        }
    }
}

@MainActor
private final class IosBannerAdView: UIView, BannerViewDelegate {
    private let bannerView = BannerView(adSize: AdSizeBanner)
    private var hasRequestedAd = false
    private var successfulResponseCount = 0

    init(adUnitID: String) {
        super.init(frame: .zero)

        backgroundColor = .clear
        bannerView.adUnitID = adUnitID
        bannerView.delegate = self
        bannerView.isHidden = true
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(bannerView)

        NSLayoutConstraint.activate([
            bannerView.centerXAnchor.constraint(equalTo: centerXAnchor),
            bannerView.centerYAnchor.constraint(equalTo: centerYAnchor),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func loadAdIfNeeded() {
        guard !hasRequestedAd else { return }
        hasRequestedAd = true
        logBanner("request phase=initial")
        bannerView.load(Request())
    }

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        successfulResponseCount += 1
        bannerView.isHidden = false
        let phase = successfulResponseCount == 1 ? "initial" : "refresh"
        let responseID = bannerView.responseInfo?.responseIdentifier ?? "unknown"
        logBanner("response phase=\(phase) responseId=\(responseID)")
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        let phase = successfulResponseCount == 0 ? "initial_failed" : "refresh_failed"
        if successfulResponseCount == 0 {
            bannerView.isHidden = true
        }
        NSLog("Banner ad response phase=%@ error=%@", phase, error.localizedDescription)
    }

    private func logBanner(_ message: String) {
#if DEBUG || BANDALART_TEST_ADS
        NSLog("Banner ad %@", message)
#endif
    }
}
