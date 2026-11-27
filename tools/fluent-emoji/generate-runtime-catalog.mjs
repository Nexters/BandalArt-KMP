import fs from "node:fs";
import path from "node:path";

const [catalogPath, outputPath, cldrDirectory] = process.argv.slice(2);

if (!catalogPath || !outputPath || !cldrDirectory) {
  throw new Error(
    "Usage: node generate-runtime-catalog.mjs <catalog-candidate.json> <output.kt> <cldr-annotations-dir>",
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
if (catalog.items.length !== 300) {
  throw new Error(`Runtime catalog must contain 300 items, found ${catalog.items.length}.`);
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

const entries = catalog.items
  .map((item) => {
    const category = categoryByGroup.get(item.group);
    if (!category) {
      throw new Error(`Unsupported Fluent Emoji group: ${item.group}`);
    }
    const keywords = item.keywords.map(JSON.stringify).join(", ");
    const koreanAliases = item.koreanAliases.map(JSON.stringify).join(", ");
    const korean = localizedAnnotation("ko", item.glyph);
    const japanese = localizedAnnotation("ja", item.glyph);
    return `            FluentEmojiItem(
                unicode = ${JSON.stringify(item.glyph)},
                resourceKey = ${JSON.stringify(item.resourceKey)},
                category = FluentEmojiCategory.${category},
                cldrName = ${JSON.stringify(item.cldrName)},
                keywords = listOf(${keywords}),
                koreanAliases = listOf(${koreanAliases}),
                koreanName = ${JSON.stringify(korean.name)},
                koreanKeywords = listOf(${korean.keywords.map(JSON.stringify).join(", ")}),
                japaneseName = ${JSON.stringify(japanese.name)},
                japaneseKeywords = listOf(${japanese.keywords.map(JSON.stringify).join(", ")}),
            ),`;
  })
  .join("\n");

const kotlinSource = `/*
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

package com.nexters.bandalart.core.ui.component.emoji

@Suppress("LargeClass")
internal object FluentEmojiCatalog {
    val items =
        listOf(
${entries}
        )

    private val resourceKeys = items.associate { it.unicode to it.resourceKey }

    val size: Int
        get() = resourceKeys.size

    fun resourceKeyFor(unicode: String): String? = resourceKeys[unicode]
}
`;

fs.writeFileSync(outputPath, kotlinSource);
