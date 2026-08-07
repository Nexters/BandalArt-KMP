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

Generated WebP files are comparison artifacts, not runtime application resources. The UI-size and payload measurements selected Color and a 300-item v1 catalog; those assets move into Compose Multiplatform resources in the renderer PR, where final AAB/iOS artifact growth is measured.

UI-size comparison and picker wireframe:

```bash
node ./tools/fluent-emoji/render-ui-spike.mjs
```

Actual 100/200/300 item Color resource and deterministic ZIP payload measurement:

```bash
./tools/fluent-emoji/measure-catalog.sh
```

Sync the finalized 300 Color assets, runtime catalog JSON, and generated Kotlin mapping into the KMP modules:

```bash
./tools/fluent-emoji/measure-catalog.sh --sync-resources
```

The measurement script uses a blob-filtered temporary Git checkout at the pinned commit, balances Fluent metadata groups, prioritizes goal-related terms, excludes gender-specific man/woman sequences, and commits only the selected catalog and measurement report. ZIP input timestamps and file modes are normalized, and the Info-ZIP version is checked. The temporary 300 WebP files and ZIP payloads are not added to the repository.

Fluent UI Emoji is distributed under the Microsoft MIT license preserved in `LICENSE`.
