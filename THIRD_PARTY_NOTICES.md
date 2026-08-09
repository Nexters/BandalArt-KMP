# Third-party notices

## Microsoft Fluent UI Emoji

- Source: https://github.com/microsoft/fluentui-emoji
- Pinned source commit: `62ecdc0d7ca5c6df32148c169556bc8d3782fca4`
- License: MIT (`tools/fluent-emoji/LICENSE`)
- Copyright: Microsoft Corporation

The generated WebP comparison assets under `tools/fluent-emoji/generated/color/` and runtime assets under `core/designsystem/src/commonMain/composeResources/drawable/fluent_*.webp` are derived from Fluent UI Emoji Color SVG assets. Files under `tools/fluent-emoji/generated/3d/` are derived from Fluent UI Emoji 3D PNG assets. The application does not download Fluent UI Emoji resources at runtime.

## Google Material Symbols

- Source: `https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/{icon}/default/24px.svg`
- Retrieved: 2026-08-09
- Icons: `apps`, `history`, `sentiment_satisfied`, `groups`, `pets`, `restaurant`, `flight`, `sports_soccer`, `lightbulb`, `tag`, `outlined_flag`
- License: Apache License 2.0 ([local license text](licenses/Apache-2.0.txt))
- Licensing reference: [Google Fonts — Material Icons licensing](https://developers.google.com/fonts/docs/material_icons#licensing)
- Copyright: Google LLC

The category navigation vectors under `core/designsystem/src/commonMain/composeResources/drawable/emoji_category_nav_*.xml` are adapted from the listed 24px Material Symbols Outlined SVG paths. The SVG view box origin is represented with an equivalent vector group translation; the icon geometry is otherwise unchanged. The application does not download Material Symbols resources at runtime.
