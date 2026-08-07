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

const entries = catalog.items
  .map(
    (item) =>
      `            ${JSON.stringify(item.glyph)} to ${JSON.stringify(item.resourceKey)},`,
  )
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

internal object FluentEmojiCatalog {
    private val resourceKeys =
        mapOf(
${entries}
        )

    val size: Int
        get() = resourceKeys.size

    fun resourceKeyFor(unicode: String): String? = resourceKeys[unicode]
}
`;

fs.writeFileSync(outputPath, kotlinSource);
