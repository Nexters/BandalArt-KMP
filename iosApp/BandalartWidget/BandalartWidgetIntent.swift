import AppIntents
import Foundation

struct BandalartWidgetConfigurationIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Bandalart"
    static var description = IntentDescription("Shows the last Bandalart viewed in the app.")

    init() {}
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
