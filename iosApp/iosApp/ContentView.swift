//
//  ContentView.swift
//  iosApp
//
//  Created by 이지훈 on 2/10/25.
//

import UIKit
import SwiftUI
import ComposeApp
import FirebaseCrashlytics

struct ComposeView: UIViewControllerRepresentable {
    let notificationLaunchBridge: DeadlineNotificationLaunchBridge
    let deadlineReminderLifecycleBridge: DeadlineReminderLifecycleBridge
    let adsBridge: IosAdsBridge
    let deviceBackupKeyBridge: IosDeviceBackupKeyBridge
    let widgetLaunchBridge: IosWidgetLaunchBridge
    let widgetRuntimeBridge: IosWidgetRuntimeBridge

    func makeUIViewController(context: Context) -> UIViewController {
        let crashlytics = Crashlytics.crashlytics()
        crashlytics.setCustomValue("main_view_controller", forKey: "startup_phase")
        crashlytics.log("Creating Compose main view controller")
        let viewController = MainViewControllerKt.MainViewController(
            notificationLaunchBridge: notificationLaunchBridge,
            deadlineReminderLifecycleBridge: deadlineReminderLifecycleBridge,
            adsBridge: adsBridge,
            deviceBackupKeyBridge: deviceBackupKeyBridge,
            widgetLaunchBridge: widgetLaunchBridge,
            widgetRuntimeBridge: widgetRuntimeBridge
        )
        crashlytics.setCustomValue("ready", forKey: "startup_phase")
        crashlytics.log("Compose main view controller created")
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let notificationLaunchBridge: DeadlineNotificationLaunchBridge
    let deadlineReminderLifecycleBridge: DeadlineReminderLifecycleBridge
    let adsBridge: IosAdsBridge
    let deviceBackupKeyBridge: IosDeviceBackupKeyBridge
    let widgetLaunchBridge: IosWidgetLaunchBridge
    let widgetRuntimeBridge: IosWidgetRuntimeBridge

    var body: some View {
        ComposeView(
            notificationLaunchBridge: notificationLaunchBridge,
            deadlineReminderLifecycleBridge: deadlineReminderLifecycleBridge,
            adsBridge: adsBridge,
            deviceBackupKeyBridge: deviceBackupKeyBridge,
            widgetLaunchBridge: widgetLaunchBridge,
            widgetRuntimeBridge: widgetRuntimeBridge
        )
                .ignoresSafeArea(edges: .all)
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
