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

private let deadlineReminderIdentifierPrefix = "deadline.v1."
private let deadlineBandalartIdKey = "deadline_bandalart_id"

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    let notificationLaunchBridge = DeadlineNotificationLaunchBridge()
    let deadlineReminderLifecycleBridge = DeadlineReminderLifecycleBridge()
    let adsBridge = IosAdsBridgeImpl()
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

@main
struct iosApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView(
                notificationLaunchBridge: appDelegate.notificationLaunchBridge,
                deadlineReminderLifecycleBridge: appDelegate.deadlineReminderLifecycleBridge,
                adsBridge: appDelegate.adsBridge
            )
        }
    }
}
