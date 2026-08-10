import Foundation
import IosWidgetShared

struct BandalartWidgetTaskModel: Identifiable, Hashable, Sendable {
    let id: Int64
    let title: String
    let isCompleted: Bool
}

struct BandalartWidgetSnapshotModel: Hashable, Sendable {
    let bandalartId: Int64
    let subGoalId: Int64?
    let title: String
    let profileEmoji: String?
    let completionRatio: Int
    let subGoalTitle: String?
    let tasks: [BandalartWidgetTaskModel]

    init(snapshot: IosWidgetSnapshot) {
        bandalartId = snapshot.bandalartId
        subGoalId = snapshot.subGoalId?.int64Value
        title = snapshot.title
        profileEmoji = snapshot.profileEmoji
        completionRatio = Int(snapshot.completionRatio)
        subGoalTitle = snapshot.subGoalTitle
        tasks = snapshot.tasks.map { task in
            BandalartWidgetTaskModel(
                id: task.id,
                title: task.title,
                isCompleted: task.isCompleted
            )
        }
    }

    static let placeholder = BandalartWidgetSnapshotModel(
        bandalartId: 1,
        subGoalId: 2,
        title: "2026 Goal",
        profileEmoji: "🎯",
        completionRatio: 45,
        subGoalTitle: "Build healthy routines",
        tasks: [
            BandalartWidgetTaskModel(id: 3, title: "Exercise three times", isCompleted: true),
            BandalartWidgetTaskModel(id: 4, title: "Sleep before midnight", isCompleted: false),
            BandalartWidgetTaskModel(id: 5, title: "Drink enough water", isCompleted: false),
        ]
    )

    private init(
        bandalartId: Int64,
        subGoalId: Int64?,
        title: String,
        profileEmoji: String?,
        completionRatio: Int,
        subGoalTitle: String?,
        tasks: [BandalartWidgetTaskModel]
    ) {
        self.bandalartId = bandalartId
        self.subGoalId = subGoalId
        self.title = title
        self.profileEmoji = profileEmoji
        self.completionRatio = completionRatio
        self.subGoalTitle = subGoalTitle
        self.tasks = tasks
    }
}

@MainActor
enum BandalartWidgetBridge {
    private static let bridge = IosWidgetDataBridge()

    static func bandalarts() async throws -> [IosWidgetBandalartOption] {
        return try await withCheckedThrowingContinuation { continuation in
            bridge.getBandalarts { values, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: values ?? [])
                }
            }
        }
    }

    static func subGoals(bandalartId: Int64) async throws -> [IosWidgetSubGoalOption] {
        return try await withCheckedThrowingContinuation { continuation in
            bridge.getSubGoals(bandalartId: bandalartId) { values, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: values ?? [])
                }
            }
        }
    }

    static func snapshot(
        bandalartId: Int64,
        subGoalId: Int64?
    ) async throws -> BandalartWidgetSnapshotModel? {
        return try await withCheckedThrowingContinuation { continuation in
            bridge.getSnapshot(
                bandalartId: bandalartId,
                subGoalId: subGoalId.map(KotlinLong.init(longLong:))
            ) { snapshot, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: snapshot.map(BandalartWidgetSnapshotModel.init))
                }
            }
        }
    }

    static func setTaskCompleted(
        bandalartId: Int64,
        subGoalId: Int64,
        taskId: Int64,
        completed: Bool
    ) async throws -> BandalartWidgetSnapshotModel? {
        return try await withCheckedThrowingContinuation { continuation in
            bridge.setTaskCompleted(
                bandalartId: bandalartId,
                subGoalId: subGoalId,
                taskId: taskId,
                completed: completed
            ) { snapshot, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: snapshot.map(BandalartWidgetSnapshotModel.init))
                }
            }
        }
    }
}
