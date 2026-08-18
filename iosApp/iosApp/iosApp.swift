//
//  iosAppApp.swift
//  iosApp
//
//  Created by 이지훈 on 2/10/25.
//

import SwiftUI
import UIKit
import ComposeApp
import Firebase
import UserNotifications
import WidgetKit

private let deadlineReminderIdentifierPrefix = "deadline.v1."
private let deadlineBandalartIdKey = "deadline_bandalart_id"
private let widgetAppGroupIdentifier = "group.com.nexters.bandalart"
private let widgetRecentBandalartIdKey = "ios_widget_recent_bandalart_id"
private let widgetRecentSubGoalIdKey = "ios_widget_recent_sub_goal_id"

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    let notificationLaunchBridge = DeadlineNotificationLaunchBridge()
    let deadlineReminderLifecycleBridge = DeadlineReminderLifecycleBridge()
    let adsBridge = IosAdsBridgeImpl()
    let deviceBackupKeyBridge = IosDeviceBackupKeyBridgeImpl()
    let widgetLaunchBridge = IosWidgetLaunchBridge()
    let widgetRuntimeBridge = IosWidgetRuntimeBridge(
        selectionWriter: IosWidgetSelectionWriterImpl(),
        timelineReloader: IosWidgetTimelineReloaderImpl()
    )
    private var timeZoneObserver: NSObjectProtocol?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        adsBridge.start()
        UNUserNotificationCenter.current().delegate = self
        timeZoneObserver = NotificationCenter.default.addObserver(
            forName: NSNotification.Name.NSSystemTimeZoneDidChange,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.deadlineReminderLifecycleBridge.record()
        }
        deadlineReminderLifecycleBridge.record()
        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        deadlineReminderLifecycleBridge.record()
    }

    func applicationSignificantTimeChange(_ application: UIApplication) {
        deadlineReminderLifecycleBridge.record()
    }

    deinit {
        if let timeZoneObserver {
            NotificationCenter.default.removeObserver(timeZoneObserver)
        }
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        guard notification.request.identifier.hasPrefix(deadlineReminderIdentifierPrefix) else {
            completionHandler([])
            return
        }
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        defer { completionHandler() }

        let request = response.notification.request
        guard response.actionIdentifier == UNNotificationDefaultActionIdentifier,
              request.identifier.hasPrefix(deadlineReminderIdentifierPrefix) else {
            return
        }

        let value = request.content.userInfo[deadlineBandalartIdKey]
        let bandalartId: Int64?
        if let stringValue = value as? String {
            bandalartId = Int64(stringValue)
        } else if let numberValue = value as? NSNumber {
            bandalartId = numberValue.int64Value
        } else {
            bandalartId = nil
        }
        if let bandalartId, bandalartId > 0 {
            DispatchQueue.main.async { [notificationLaunchBridge] in
                notificationLaunchBridge.record(bandalartId: bandalartId)
            }
        }
    }
}

private final class IosWidgetSelectionWriterImpl: IosWidgetSelectionWriter {
    private let defaults = UserDefaults(suiteName: widgetAppGroupIdentifier)

    func writeSelection(bandalartId: Int64, subGoalId: Int64) {
        guard let defaults else { return }
        if bandalartId > 0 {
            defaults.set(bandalartId, forKey: widgetRecentBandalartIdKey)
        } else {
            defaults.removeObject(forKey: widgetRecentBandalartIdKey)
        }
        if subGoalId > 0 {
            defaults.set(subGoalId, forKey: widgetRecentSubGoalIdKey)
        } else {
            defaults.removeObject(forKey: widgetRecentSubGoalIdKey)
        }
    }
}

private final class IosWidgetTimelineReloaderImpl: IosWidgetTimelineReloader {
    func reloadTimelines() {
        WidgetCenter.shared.reloadTimelines(ofKind: "BandalartWidget")
    }
}

@main
struct iosApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView(
                notificationLaunchBridge: appDelegate.notificationLaunchBridge,
                deadlineReminderLifecycleBridge: appDelegate.deadlineReminderLifecycleBridge,
                adsBridge: appDelegate.adsBridge,
                deviceBackupKeyBridge: appDelegate.deviceBackupKeyBridge,
                widgetLaunchBridge: appDelegate.widgetLaunchBridge,
                widgetRuntimeBridge: appDelegate.widgetRuntimeBridge
            )
            .onOpenURL { url in
                guard url.scheme == "bandalart",
                      url.host == "widget",
                      let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                      let value = components.queryItems?.first(where: { $0.name == "bandalartId" })?.value,
                      let bandalartId = Int64(value),
                      bandalartId > 0 else {
                    return
                }
                appDelegate.widgetLaunchBridge.record(bandalartId: bandalartId)
            }
            .onChange(of: scenePhase) { phase in
                if phase == .active {
                    appDelegate.widgetRuntimeBridge.applicationDidBecomeActive()
                }
            }
        }
    }
}
