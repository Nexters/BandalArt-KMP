# Fluent UI Emoji resource spike

This directory contains the reproducible Color/3D resource spike foundation for issue #212.

```bash
./tools/fluent-emoji/sync-spike.sh
```

Requirements:

- `curl`
- `jq`
- `rsvg-convert` 2.60.0
- `cwebp` 1.6.0
- Info-ZIP 3.0
- `shasum`

The script downloads only the entries listed in `spike-manifest.tsv` from the pinned upstream commit. It does not clone or bundle the complete Fluent UI Emoji repository. Converter versions are checked before generation so the committed WebP bytes and size report remain reproducible.

Generated WebP files are comparison artifacts, not runtime application resources. The UI-size and payload measurements selected Color. The runtime catalog now includes every supported Fluent candidate; release AAB/iOS artifact growth is recorded in `docs/features/emoji/FLUENT_EMOJI_RESOURCE_SPIKE.md`.

UI-size comparison and picker wireframe:

```bash
node ./tools/fluent-emoji/render-ui-spike.mjs
```

Actual 300/600/1000/all item Color resource and deterministic ZIP payload measurement:

```bash
./tools/fluent-emoji/measure-catalog.sh
```

Sync every selected Color asset and the runtime catalog JSON into the KMP modules. The measurement JSON remains under `tools/fluent-emoji/measurement` and is not copied into the runtime bundle:

```bash
./tools/fluent-emoji/measure-catalog.sh --sync-resources
```

The measurement script uses a blob-filtered temporary Git checkout at the pinned commit, includes every Fluent candidate in the nine supported metadata groups, orders them by the pinned Unicode `emoji-test.txt` (`EMOJI_TEST_VERSION` and `EMOJI_TEST_SHA256` in `measure-catalog.sh`), excludes gender-specific man/woman sequences, and commits only the selected catalog and measurement report. ZIP input timestamps and file modes are normalized, and the Info-ZIP version is checked. The temporary WebP files and ZIP payloads are not added to the repository.

The runtime catalog JSON (`core/designsystem/src/commonMain/composeResources/files/fluent_emoji_catalog.json`) contains each Unicode value plus normalized category, CLDR name, English keywords, curated Korean aliases, and Korean/Japanese names and keywords used by the common picker UI. The sync command downloads the pinned `ko` and `ja` emoji annotations from unicode-org/cldr-json (`CLDR_JSON_VERSION` in `measure-catalog.sh`) and fails if any catalog item has no annotation. Curated Korean aliases take precedence over the CLDR Korean name for display. Color WebP files are synced to `core/designsystem/src/commonMain/composeResources/files/fluent_emoji/` (not `drawable/`, which would generate an accessor per file). The app derives each file name from the code points (`fluent_` plus lowercase hex), so the JSON does not store it, and the generator fails if a resource key breaks that rule. Do not edit the generated catalog manually; regenerate it through the sync command above.

Fluent UI Emoji is distributed under the Microsoft MIT license preserved in `LICENSE`. CLDR annotations and the Unicode `emoji-test.txt` ordering data are distributed under the Unicode License v3 preserved in `licenses/Unicode-3.0.txt`.
