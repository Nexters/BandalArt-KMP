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

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(notificationLaunchBridge: notificationLaunchBridge)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let notificationLaunchBridge: DeadlineNotificationLaunchBridge

    var body: some View {
        ComposeView(notificationLaunchBridge: notificationLaunchBridge)
                .ignoresSafeArea(edges: .all)
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
