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

The script downloads only the entries listed in `spike-manifest.tsv` from the pinned upstream commit. It does not clone or bundle the complete Fluent UI Emoji repository. Converter versions are checked before generation so the committed WebP bytes and size report remain reproducible.

Generated WebP files are comparison artifacts, not runtime application resources. The catalog count and style will be finalized after the remaining UI-size and package measurements before selected files move into Compose Multiplatform resources in the renderer PR.

Fluent UI Emoji is distributed under the Microsoft MIT license preserved in `LICENSE`.
