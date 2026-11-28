import fs from "node:fs";
import path from "node:path";

const [catalogPath, outputPath, cldrDirectory] = process.argv.slice(2);

if (!catalogPath || !outputPath || !cldrDirectory) {
  throw new Error(
    "Usage: node generate-runtime-catalog.mjs <catalog-candidate.json> <output.json> <cldr-annotations-dir>",
  );
}

function readCldrAnnotations(language) {
  const readJson = (relativePath) =>
    JSON.parse(fs.readFileSync(path.join(cldrDirectory, relativePath), "utf8"));
  const derived = readJson(`annotationsDerived/${language}/annotations.json`);
  const base = readJson(`annotations/${language}/annotations.json`);
  return {
    ...derived.annotationsDerived.annotations,
    ...base.annotations.annotations,
  };
}

const cldrAnnotationsByLanguage = new Map(
  ["ko", "ja"].map((language) => [language, readCldrAnnotations(language)]),
);

function localizedAnnotation(language, glyph) {
  const annotations = cldrAnnotationsByLanguage.get(language);
  // CLDR keys omit the emoji presentation selector (U+FE0F).
  const annotation = annotations[glyph] ?? annotations[glyph.replaceAll("\uFE0F", "")];
  const name = annotation?.tts?.[0];
  if (!name) {
    throw new Error(`Missing CLDR ${language} annotation for ${glyph}.`);
  }
  return { name, keywords: annotation.default ?? [] };
}

const catalog = JSON.parse(fs.readFileSync(catalogPath, "utf8"));
if (catalog.items.length === 0 || catalog.items.length !== catalog.candidateCount) {
  throw new Error(
    `Runtime catalog must contain every candidate (${catalog.candidateCount}), found ${catalog.items.length}.`,
  );
}
const categoryByGroup = new Map([
  ["Smileys & Emotion", "SMILEYS_AND_EMOTION"],
  ["People & Body", "PEOPLE_AND_BODY"],
  ["Animals & Nature", "ANIMALS_AND_NATURE"],
  ["Food & Drink", "FOOD_AND_DRINK"],
  ["Travel & Places", "TRAVEL_AND_PLACES"],
  ["Activities", "ACTIVITIES"],
  ["Objects", "OBJECTS"],
  ["Symbols", "SYMBOLS"],
  ["Flags", "FLAGS"],
]);

// The app derives the image file name from the code points, so the JSON does not store it.
function resourceKeyFromGlyph(glyph) {
  return `fluent_${[...glyph].map((char) => char.codePointAt(0).toString(16).padStart(4, "0")).join("_")}`;
}

const runtimeItems = catalog.items.map((item) => {
  const category = categoryByGroup.get(item.group);
  if (!category) {
    throw new Error(`Unsupported Fluent Emoji group: ${item.group}`);
  }
  if (resourceKeyFromGlyph(item.glyph) !== item.resourceKey) {
    throw new Error(`Resource key ${item.resourceKey} does not follow the code point rule for ${item.glyph}.`);
  }
  const korean = localizedAnnotation("ko", item.glyph);
  const japanese = localizedAnnotation("ja", item.glyph);
  const runtimeItem = {
    u: item.glyph,
    c: category,
    n: item.cldrName,
    k: item.keywords,
  };
  if (item.koreanAliases.length > 0) {
    runtimeItem.a = item.koreanAliases;
  }
  return {
    ...runtimeItem,
    kn: korean.name,
    kk: korean.keywords,
    jn: japanese.name,
    jk: japanese.keywords,
  };
});

fs.writeFileSync(outputPath, `${JSON.stringify(runtimeItems)}\n`);
