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
            configuration: BandalartWidgetConfigurationIntent(),
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
        let snapshot = try? await BandalartWidgetBridge.recentSnapshot()
        return BandalartWidgetEntry(
            date: .now,
            configuration: configuration,
            snapshot: snapshot ?? (placeholderWhenEmpty ? .placeholder : nil)
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
            if let subGoalTitle = snapshot.subGoalTitle {
                Text(subGoalTitle)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            } else {
                Text("Choose a sub-goal")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
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
            Text("Create or open a goal in the app.")
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
