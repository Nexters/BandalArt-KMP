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
import CryptoKit
import Foundation
import Security

final class IosDeviceBackupKeyBridgeImpl: NSObject, IosDeviceBackupKeyBridge {
    private let service = "com.nexters.bandalart.cloud-backup"
    private let account = "device-seed.v1"

    func getDeviceKey() -> String? {
        guard let seed = loadSeed() ?? createSeed() else { return nil }
        let namespace = Bundle.main.bundleIdentifier ?? "com.nexters.bandalart.iosApp"
        let digest = SHA256.hash(data: Data("\(namespace):\(seed)".utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    private func loadSeed() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    private func createSeed() -> String? {
        let seed = UUID().uuidString.lowercased()
        let attributes: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData as String: Data(seed.utf8),
        ]
        let status = SecItemAdd(attributes as CFDictionary, nil)
        if status == errSecSuccess {
            return seed
        }
        if status == errSecDuplicateItem {
            return loadSeed()
        }
        return nil
    }
}
