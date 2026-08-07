import fs from "node:fs";

const [catalogPath, outputPath] = process.argv.slice(2);

if (!catalogPath || !outputPath) {
  throw new Error(
    "Usage: node generate-runtime-catalog.mjs <catalog-candidate.json> <output.kt>",
  );
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
    return `            FluentEmojiItem(
                unicode = ${JSON.stringify(item.glyph)},
                resourceKey = ${JSON.stringify(item.resourceKey)},
                category = FluentEmojiCategory.${category},
                cldrName = ${JSON.stringify(item.cldrName)},
                keywords = listOf(${keywords}),
                koreanAliases = listOf(${koreanAliases}),
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
