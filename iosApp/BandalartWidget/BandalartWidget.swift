import AppIntents
import SwiftUI
import WidgetKit

private let bandalartWidgetKind = "BandalartWidget"

struct BandalartWidgetEntry: TimelineEntry {
    let date: Date
    let configuration: BandalartWidgetConfigurationIntent
    let snapshot: BandalartWidgetSnapshotModel?
}

struct BandalartWidgetProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> BandalartWidgetEntry {
        BandalartWidgetEntry(
            date: .now,
            configuration: BandalartWidgetConfigurationIntent(selection: nil),
            snapshot: .placeholder
        )
    }

    func snapshot(
        for configuration: BandalartWidgetConfigurationIntent,
        in context: Context
    ) async -> BandalartWidgetEntry {
        await entry(for: configuration, placeholderWhenEmpty: context.isPreview)
    }

    func timeline(
        for configuration: BandalartWidgetConfigurationIntent,
        in context: Context
    ) async -> Timeline<BandalartWidgetEntry> {
        Timeline(entries: [await entry(for: configuration)], policy: .never)
    }

    private func entry(
        for configuration: BandalartWidgetConfigurationIntent,
        placeholderWhenEmpty: Bool = false
    ) async -> BandalartWidgetEntry {
        let selection: BandalartSelectionEntity?
        if let configuredSelection = configuration.selection {
            selection = configuredSelection
        } else {
            selection = try? await BandalartSelectionQuery().suggestedEntities().first
        }
        let snapshot: BandalartWidgetSnapshotModel?
        if let selection {
            snapshot = try? await BandalartWidgetBridge.snapshot(
                bandalartId: selection.bandalartId,
                subGoalId: selection.subGoalId
            )
        } else {
            snapshot = placeholderWhenEmpty ? .placeholder : nil
        }
        return BandalartWidgetEntry(
            date: .now,
            configuration: configuration,
            snapshot: snapshot
        )
    }
}

struct BandalartWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: BandalartWidgetEntry

    var body: some View {
        Group {
            if let snapshot = entry.snapshot {
                switch family {
                case .systemSmall:
                    SmallBandalartWidgetView(snapshot: snapshot)
                case .systemMedium:
                    DetailBandalartWidgetView(snapshot: snapshot, taskLimit: 3)
                default:
                    DetailBandalartWidgetView(snapshot: snapshot, taskLimit: 5)
                }
            } else {
                EmptyBandalartWidgetView()
            }
        }
        .containerBackground(Color(.systemBackground), for: .widget)
        .widgetURL(entry.snapshot.flatMap(Self.deepLinkURL))
    }

    private static func deepLinkURL(snapshot: BandalartWidgetSnapshotModel) -> URL? {
        URL(string: "bandalart://widget?bandalartId=\(snapshot.bandalartId)")
    }
}

private struct SmallBandalartWidgetView: View {
    let snapshot: BandalartWidgetSnapshotModel

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(snapshot.profileEmoji ?? "🎯")
                .font(.system(size: 28))
            Text(snapshot.title)
                .font(.headline)
                .lineLimit(2)
            Spacer(minLength: 0)
            Text("\(snapshot.completionRatio)%")
                .font(.system(size: 24, weight: .bold, design: .rounded))
                .foregroundStyle(.primary)
            ProgressView(value: Double(snapshot.completionRatio), total: 100)
                .tint(.mint)
        }
    }
}

private struct DetailBandalartWidgetView: View {
    let snapshot: BandalartWidgetSnapshotModel
    let taskLimit: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(snapshot.profileEmoji ?? "🎯")
                Text(snapshot.title)
                    .font(.headline)
                    .lineLimit(1)
                Spacer(minLength: 4)
                Text("\(snapshot.completionRatio)%")
                    .font(.subheadline.weight(.semibold))
            }
            Text(snapshot.subGoalTitle ?? "Choose a sub-goal")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
                .lineLimit(1)
            VStack(alignment: .leading, spacing: 5) {
                ForEach(Array(snapshot.tasks.prefix(taskLimit))) { task in
                    Button(
                        intent: SetBandalartTaskCompletedIntent(
                            bandalartId: snapshot.bandalartId,
                            subGoalId: snapshot.subGoalId ?? 0,
                            taskId: task.id,
                            completed: !task.isCompleted
                        )
                    ) {
                        HStack(spacing: 7) {
                            Image(systemName: task.isCompleted ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(task.isCompleted ? .mint : .secondary)
                            Text(task.title)
                                .font(.caption)
                                .foregroundStyle(task.isCompleted ? .secondary : .primary)
                                .strikethrough(task.isCompleted)
                                .lineLimit(1)
                            Spacer(minLength: 0)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .disabled(snapshot.subGoalId == nil)
                }
                if snapshot.tasks.isEmpty {
                    Text("Add tasks in the app")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 0)
        }
    }
}

private struct EmptyBandalartWidgetView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("🎯")
                .font(.title)
            Text("Choose a Bandalart")
                .font(.headline)
            Text("Edit this widget after creating a goal in the app.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}

struct BandalartWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: bandalartWidgetKind,
            intent: BandalartWidgetConfigurationIntent.self,
            provider: BandalartWidgetProvider()
        ) { entry in
            BandalartWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Bandalart")
        .description("See your goal progress and complete tasks from the Home Screen.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
