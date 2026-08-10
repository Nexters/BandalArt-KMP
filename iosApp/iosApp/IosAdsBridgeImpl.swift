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

private enum AdUnitID {
#if DEBUG
    static let banner = "ca-app-pub-3940256099942544/2435281174"
    static let rewarded = "ca-app-pub-3940256099942544/1712485313"
#else
    static let banner = "ca-app-pub-5570932833347277/4693940934"
    static let rewarded = "ca-app-pub-5570932833347277/2754173309"
#endif
}

@MainActor
final class IosAdsBridgeImpl: NSObject, @preconcurrency IosAdsBridge, FullScreenContentDelegate {
    private let bannerViews = NSHashTable<IosBannerAdView>.weakObjects()

    private var isInitialized = false
    private var isRewardedLoading = false
    private var rewardedAd: RewardedAd?
    private var activeRewardedAd: RewardedAd?
    private var pendingRequestID: Int64?
    private var pendingCompletion: ((CommonRewardedAdResult) -> Void)?
    private var didEarnReward = false
    private var isRewardedDismissed = false

    func start() {
        let requestConfiguration = MobileAds.shared.requestConfiguration
        requestConfiguration.publisherPrivacyPersonalizationState = .disabled
        requestConfiguration.setPublisherFirstPartyIDEnabled(false)

        MobileAds.shared.start { [weak self] _ in
            Task { @MainActor in
                guard let self else { return }
                self.isInitialized = true
                self.bannerViews.allObjects.forEach { $0.loadAdIfNeeded() }
                self.loadRewardedIfNeeded()
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
        completion: @escaping (CommonRewardedAdResult) -> Void
    ) {
        guard pendingRequestID == nil, activeRewardedAd == nil else {
            completion(.failed)
            return
        }

        pendingRequestID = requestId
        pendingCompletion = completion
        presentRewardedIfReady()
    }

    func consumeRewarded(requestId: Int64) {
        guard pendingRequestID == requestId else { return }
        pendingCompletion = nil
        if activeRewardedAd == nil {
            pendingRequestID = nil
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
        guard let rewardedAd else {
            loadRewardedIfNeeded()
            return
        }

        self.rewardedAd = nil
        activeRewardedAd = rewardedAd
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

    private func loadRewardedIfNeeded() {
        guard isInitialized, rewardedAd == nil, activeRewardedAd == nil, !isRewardedLoading else {
            return
        }

        isRewardedLoading = true
        Task { @MainActor [weak self] in
            guard let self else { return }
            do {
                let ad = try await RewardedAd.load(
                    with: AdUnitID.rewarded,
                    request: Request()
                )
                ad.fullScreenContentDelegate = self
                self.rewardedAd = ad
                self.isRewardedLoading = false
                if self.pendingCompletion != nil {
                    self.presentRewardedIfReady()
                }
            } catch {
                self.isRewardedLoading = false
                NSLog("Rewarded ad failed to load: %@", error.localizedDescription)
                if self.pendingCompletion != nil {
                    self.finishRewarded(with: .failed)
                }
            }
        }
    }

    private func finishRewarded(with result: CommonRewardedAdResult) {
        let completion = pendingCompletion
        pendingRequestID = nil
        pendingCompletion = nil
        activeRewardedAd = nil
        didEarnReward = false
        isRewardedDismissed = false
        completion?(result)
        loadRewardedIfNeeded()
    }
}

@MainActor
private final class IosBannerAdView: UIView, BannerViewDelegate {
    private let bannerView = BannerView(adSize: AdSizeBanner)
    private var hasRequestedAd = false

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
        bannerView.load(Request())
    }

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        bannerView.isHidden = false
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        bannerView.isHidden = true
        NSLog("Banner ad failed to load: %@", error.localizedDescription)
    }
}
