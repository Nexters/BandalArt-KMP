import fs from "node:fs";
import path from "node:path";

const [upstreamDirectory, treePath, spikeCatalogPath, emojiTestPath, outputPath] =
  process.argv.slice(2);

if (!upstreamDirectory || !treePath || !spikeCatalogPath || !emojiTestPath || !outputPath) {
  throw new Error(
    "Usage: node select-catalog.mjs <upstream-dir> <tree-paths> <spike-catalog> <emoji-test> <output>",
  );
}

const supportedGroups = new Set([
  "Smileys & Emotion",
  "People & Body",
  "Animals & Nature",
  "Food & Drink",
  "Travel & Places",
  "Activities",
  "Objects",
  "Symbols",
  "Flags",
]);
const treePaths = fs.readFileSync(treePath, "utf8").trim().split("\n");
const spikeCatalog = JSON.parse(fs.readFileSync(spikeCatalogPath, "utf8"));
const curatedAliases = new Map(
  spikeCatalog.items.map((item) => [item.unicode, item.koreanAliases]),
);
const emojiTestOrder = readEmojiTestOrder(emojiTestPath);

function readEmojiTestOrder(filePath) {
  const order = new Map();
  for (const line of fs.readFileSync(filePath, "utf8").split("\n")) {
    const match = line.match(/^([0-9A-F ]+?)\s*; (fully-qualified|minimally-qualified|unqualified|component)\b/);
    if (match) {
      const key = withoutVariationSelector(match[1].toLowerCase());
      if (!order.has(key)) order.set(key, order.size);
    }
  }
  return order;
}

function withoutVariationSelector(unicode) {
  return unicode
    .split(" ")
    .filter((codePoint) => codePoint !== "fe0f")
    .join(" ");
}

function emojiTestRank(item) {
  const rank = emojiTestOrder.get(withoutVariationSelector(item.unicode));
  if (rank === undefined) {
    throw new Error(`${item.unicode} (${item.cldrName}) is missing from emoji-test.txt.`);
  }
  return rank;
}

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
      supportedGroups.has(item.group) &&
      item.unicode &&
      item.colorSourcePath &&
      !isGenderVariant(item),
  )
  .filter((item) => {
    if (seenUnicode.has(item.unicode)) return false;
    seenUnicode.add(item.unicode);
    return true;
  });

for (const unicode of curatedAliases.keys()) {
  if (!seenUnicode.has(unicode)) {
    throw new Error(`Spike emoji ${unicode} is not a selectable Fluent candidate.`);
  }
}

const selected = [...candidates].sort(
  (left, right) => emojiTestRank(left) - emojiTestRank(right),
);

const output = {
  sourceCommit: "62ecdc0d7ca5c6df32148c169556bc8d3782fca4",
  selectionMethod:
    "every Fluent candidate in the supported metadata groups, ordered by Unicode emoji-test; gender-specific man/woman sequences excluded",
  candidateCount: candidates.length,
  items: selected.map((item, index) => ({
    rank: index + 1,
    ...item,
    resourceKey: `fluent_${item.unicode.replaceAll(" ", "_")}`,
  })),
};

fs.writeFileSync(outputPath, `${JSON.stringify(output, null, 2)}\n`);
