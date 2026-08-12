import AppIntents
import Foundation

struct BandalartSelectionEntity: AppEntity, Hashable {
    static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Bandalart Goal")
    static var defaultQuery = BandalartSelectionQuery()

    let id: String
    let bandalartId: Int64
    let subGoalId: Int64?
    let bandalartTitle: String
    let subGoalTitle: String
    let profileEmoji: String?

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(subGoalTitle.isEmpty ? bandalartTitle : subGoalTitle)",
            subtitle: "\(profileEmoji ?? "🎯") \(bandalartTitle)"
        )
    }
}

struct BandalartSelectionQuery: EntityQuery {
    func entities(for identifiers: [BandalartSelectionEntity.ID]) async throws -> [BandalartSelectionEntity] {
        let identifierSet = Set(identifiers)
        return try await selections().filter { identifierSet.contains($0.id) }
    }

    func suggestedEntities() async throws -> [BandalartSelectionEntity] {
        try await selections()
    }

    func defaultResult() async -> BandalartSelectionEntity? {
        try? await suggestedEntities().first
    }

    private func selections() async throws -> [BandalartSelectionEntity] {
        var result: [BandalartSelectionEntity] = []
        for bandalart in try await BandalartWidgetBridge.bandalarts() {
            result.append(
                BandalartSelectionEntity(
                    id: "\(bandalart.id)",
                    bandalartId: bandalart.id,
                    subGoalId: nil,
                    bandalartTitle: bandalart.title,
                    subGoalTitle: "",
                    profileEmoji: bandalart.profileEmoji
                )
            )
            for subGoal in try await BandalartWidgetBridge.subGoals(bandalartId: bandalart.id) {
                result.append(
                    BandalartSelectionEntity(
                        id: "\(bandalart.id):\(subGoal.id)",
                        bandalartId: bandalart.id,
                        subGoalId: subGoal.id,
                        bandalartTitle: bandalart.title,
                        subGoalTitle: subGoal.title,
                        profileEmoji: bandalart.profileEmoji
                    )
                )
            }
        }
        return result
    }
}

struct BandalartWidgetConfigurationIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Choose a Bandalart"
    static var description = IntentDescription("Selects a Bandalart and sub-goal to display.")

    @Parameter(title: "Bandalart / Sub-goal")
    var selection: BandalartSelectionEntity?

    init() {}

    init(selection: BandalartSelectionEntity?) {
        self.selection = selection
    }
}

struct SetBandalartTaskCompletedIntent: AppIntent {
    static var title: LocalizedStringResource = "Update a Bandalart task"
    static var description = IntentDescription("Marks a Bandalart task as complete or incomplete.")

    @Parameter(title: "Bandalart ID")
    var bandalartId: Int

    @Parameter(title: "Sub-goal ID")
    var subGoalId: Int

    @Parameter(title: "Task ID")
    var taskId: Int

    @Parameter(title: "Completed")
    var completed: Bool

    init() {}

    init(
        bandalartId: Int64,
        subGoalId: Int64,
        taskId: Int64,
        completed: Bool
    ) {
        self.bandalartId = Int(bandalartId)
        self.subGoalId = Int(subGoalId)
        self.taskId = Int(taskId)
        self.completed = completed
    }

    func perform() async throws -> some IntentResult {
        _ = try await BandalartWidgetBridge.setTaskCompleted(
            bandalartId: Int64(bandalartId),
            subGoalId: Int64(subGoalId),
            taskId: Int64(taskId),
            completed: completed
        )
        return .result()
    }
}
