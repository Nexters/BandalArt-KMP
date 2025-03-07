//
//  iosAppApp.swift
//  iosApp
//
//  Created by 이지훈 on 2/10/25.
//

import SwiftUI
import ComposeApp
import Firebase

@main
struct iosApp: App {
    init() {
        FirebaseApp.configure()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
