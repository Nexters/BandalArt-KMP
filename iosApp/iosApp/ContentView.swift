//
//  ContentView.swift
//  iosApp
//
//  Created by 이지훈 on 2/10/25.
//

import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let notificationLaunchBridge: DeadlineNotificationLaunchBridge
    let deadlineReminderLifecycleBridge: DeadlineReminderLifecycleBridge
    let adsBridge: IosAdsBridge

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            notificationLaunchBridge: notificationLaunchBridge,
            deadlineReminderLifecycleBridge: deadlineReminderLifecycleBridge,
            adsBridge: adsBridge
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let notificationLaunchBridge: DeadlineNotificationLaunchBridge
    let deadlineReminderLifecycleBridge: DeadlineReminderLifecycleBridge
    let adsBridge: IosAdsBridge

    var body: some View {
        ComposeView(
            notificationLaunchBridge: notificationLaunchBridge,
            deadlineReminderLifecycleBridge: deadlineReminderLifecycleBridge,
            adsBridge: adsBridge
        )
                .ignoresSafeArea(edges: .all)
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
