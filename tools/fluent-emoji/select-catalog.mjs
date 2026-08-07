import fs from "node:fs";
import path from "node:path";

const [upstreamDirectory, treePath, spikeCatalogPath, outputPath] = process.argv.slice(2);

if (!upstreamDirectory || !treePath || !spikeCatalogPath || !outputPath) {
  throw new Error(
    "Usage: node select-catalog.mjs <upstream-dir> <tree-paths> <spike-catalog> <output>",
  );
}

const groupQuotas = new Map([
  ["Smileys & Emotion", 30],
  ["People & Body", 30],
  ["Animals & Nature", 35],
  ["Food & Drink", 35],
  ["Travel & Places", 35],
  ["Activities", 45],
  ["Objects", 60],
  ["Symbols", 25],
  ["Flags", 5],
]);
const goalTerms = [
  "goal", "target", "check", "success", "award", "medal", "trophy", "study",
  "book", "school", "graduate", "write", "pencil", "work", "office", "briefcase",
  "computer", "chart", "money", "coin", "bank", "save", "health", "exercise",
  "sport", "run", "walk", "bicycle", "swim", "food", "vegetable", "fruit",
  "water", "sleep", "time", "calendar", "alarm", "habit", "growth", "plant",
  "seedling", "fire", "rocket", "star", "sparkle", "heart", "home", "travel",
  "music", "art", "clean", "cook", "family", "friend",
];
const treePaths = fs.readFileSync(treePath, "utf8").trim().split("\n");
const spikeCatalog = JSON.parse(fs.readFileSync(spikeCatalogPath, "utf8"));
const curatedAliases = new Map(
  spikeCatalog.items.map((item) => [item.unicode, item.koreanAliases]),
);
const curatedUnicode = new Set(curatedAliases.keys());

function collectMetadata(directory) {
  const results = [];
  const entries = fs
    .readdirSync(directory, { withFileTypes: true })
    .sort((left, right) => compareText(left.name, right.name));
  for (const entry of entries) {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      results.push(...collectMetadata(entryPath));
    } else if (entry.name === "metadata.json") {
      results.push(entryPath);
    }
  }
  return results;
}

function normalizeRepositoryPath(filePath) {
  return path.relative(upstreamDirectory, filePath).split(path.sep).join("/");
}

function colorPathFor(metadataPath) {
  const emojiDirectory = path.posix.dirname(metadataPath);
  const directPrefix = `${emojiDirectory}/Color/`;
  const defaultPrefix = `${emojiDirectory}/Default/Color/`;
  return (
    treePaths.find(
      (candidate) => candidate.startsWith(directPrefix) && candidate.endsWith("_color.svg"),
    ) ??
    treePaths.find(
      (candidate) => candidate.startsWith(defaultPrefix) && candidate.endsWith("_color_default.svg"),
    )
  );
}

function relevanceScore(item) {
  const searchable = [item.cldrName, ...item.keywords].join(" ").toLowerCase();
  const goalScore = goalTerms.reduce(
    (score, term) => score + (searchable.includes(term) ? 10 : 0),
    0,
  );
  return goalScore + (curatedUnicode.has(item.unicode) ? 10_000 : 0);
}

function isGenderVariant(item) {
  const codePoints = item.unicode.split(" ");
  const genderSpecificCodePoints = new Set(["2640", "2642", "1f468", "1f469"]);
  return codePoints.some((codePoint) => genderSpecificCodePoints.has(codePoint));
}

function compareText(left, right) {
  if (left < right) return -1;
  if (left > right) return 1;
  return 0;
}

const seenUnicode = new Set();
const candidates = collectMetadata(path.join(upstreamDirectory, "assets"))
  .map((metadataFilePath) => {
    const metadataPath = normalizeRepositoryPath(metadataFilePath);
    const metadata = JSON.parse(fs.readFileSync(metadataFilePath, "utf8"));
    const colorSourcePath = colorPathFor(metadataPath);
    return {
      unicode: metadata.unicode,
      glyph: metadata.glyph,
      group: metadata.group,
      cldrName: metadata.cldr,
      keywords: metadata.keywords ?? [],
      koreanAliases: curatedAliases.get(metadata.unicode) ?? [],
      metadataPath,
      colorSourcePath,
    };
  })
  .filter(
    (item) =>
      groupQuotas.has(item.group) &&
      item.unicode &&
      item.colorSourcePath &&
      !isGenderVariant(item),
  )
  .filter((item) => {
    if (seenUnicode.has(item.unicode)) return false;
    seenUnicode.add(item.unicode);
    return true;
  });

const rankedByGroup = new Map();
for (const [group, quota] of groupQuotas) {
  const ranked = candidates
    .filter((item) => item.group === group)
    .sort(
      (left, right) =>
        relevanceScore(right) - relevanceScore(left) ||
        compareText(left.cldrName, right.cldrName),
    );
  if (ranked.length < quota) {
    throw new Error(`${group} has ${ranked.length} candidates, below quota ${quota}.`);
  }
  rankedByGroup.set(group, ranked);
}

const selectedCounts = new Map([...groupQuotas.keys()].map((group) => [group, 0]));
const selected = [];
while (selected.length < 300) {
  const group = [...groupQuotas.keys()]
    .filter((candidateGroup) => selectedCounts.get(candidateGroup) < groupQuotas.get(candidateGroup))
    .sort((left, right) => {
      const leftRatio = selectedCounts.get(left) / groupQuotas.get(left);
      const rightRatio = selectedCounts.get(right) / groupQuotas.get(right);
      return leftRatio - rightRatio;
    })[0];
  const itemIndex = selectedCounts.get(group);
  selected.push(rankedByGroup.get(group)[itemIndex]);
  selectedCounts.set(group, itemIndex + 1);
}

const output = {
  sourceCommit: "62ecdc0d7ca5c6df32148c169556bc8d3782fca4",
  selectionMethod:
    "goal-term relevance within balanced Fluent metadata group quotas; gender-specific man/woman sequences excluded",
  groupQuotas: Object.fromEntries(groupQuotas),
  candidateCount: candidates.length,
  items: selected.map((item, index) => ({
    rank: index + 1,
    ...item,
    resourceKey: `fluent_${item.unicode.replaceAll(" ", "_")}`,
  })),
};

fs.writeFileSync(outputPath, `${JSON.stringify(output, null, 2)}\n`);
