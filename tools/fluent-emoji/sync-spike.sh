#!/usr/bin/env bash

set -euo pipefail

readonly UPSTREAM_REPOSITORY="microsoft/fluentui-emoji"
readonly UPSTREAM_COMMIT="62ecdc0d7ca5c6df32148c169556bc8d3782fca4"
readonly IMAGE_SIZE="128"
readonly WEBP_QUALITY="92"
readonly REQUIRED_RSVG_VERSION="2.60.0"
readonly REQUIRED_CWEBP_VERSION="1.6.0"

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly MANIFEST_PATH="${SCRIPT_DIR}/spike-manifest.tsv"
readonly OUTPUT_DIR="${SCRIPT_DIR}/generated"
readonly RAW_BASE_URL="https://raw.githubusercontent.com/${UPSTREAM_REPOSITORY}/${UPSTREAM_COMMIT}"

for command_name in curl jq rsvg-convert cwebp; do
    if ! command -v "${command_name}" >/dev/null 2>&1; then
        echo "Required command is missing: ${command_name}" >&2
        exit 1
    fi
done

temporary_dir="$(mktemp -d)"
trap 'rm -rf "${temporary_dir}"' EXIT
working_output_dir="${temporary_dir}/generated"

mkdir -p "${working_output_dir}/color" "${working_output_dir}/3d"

rsvg_version="$(rsvg-convert --version | awk '{ print $3 }')"
cwebp_version="$(cwebp -version | head -n 1)"

if [[ "${rsvg_version}" != "${REQUIRED_RSVG_VERSION}" ]]; then
    echo "rsvg-convert ${REQUIRED_RSVG_VERSION} is required, but found ${rsvg_version}." >&2
    exit 1
fi

if [[ "${cwebp_version}" != "${REQUIRED_CWEBP_VERSION}" ]]; then
    echo "cwebp ${REQUIRED_CWEBP_VERSION} is required, but found ${cwebp_version}." >&2
    exit 1
fi

catalog_items_path="${temporary_dir}/catalog-items.ndjson"
: > "${catalog_items_path}"

url_encode_path() {
    printf '%s' "$1" | jq -sRr '@uri' | sed 's/%2F/\//g'
}

directory_bytes() {
    find "$1" -name '*.webp' -type f -exec cat {} + | wc -c | tr -d '[:space:]'
}

while IFS='|' read -r upstream_directory color_source_path three_d_source_path korean_aliases; do
    if [[ -z "${upstream_directory}" || "${upstream_directory}" == \#* ]]; then
        continue
    fi

    encoded_color_source_path="$(url_encode_path "${color_source_path}")"
    encoded_three_d_source_path="$(url_encode_path "${three_d_source_path}")"
    encoded_metadata_path="$(url_encode_path "assets/${upstream_directory}/metadata.json")"
    metadata_path="${temporary_dir}/metadata.json"
    color_svg_path="${temporary_dir}/color.svg"
    color_png_path="${temporary_dir}/color.png"
    three_d_png_path="${temporary_dir}/3d.png"

    curl --fail --location --silent --show-error \
        "${RAW_BASE_URL}/${encoded_metadata_path}" \
        --output "${metadata_path}"
    curl --fail --location --silent --show-error \
        "${RAW_BASE_URL}/${encoded_color_source_path}" \
        --output "${color_svg_path}"
    curl --fail --location --silent --show-error \
        "${RAW_BASE_URL}/${encoded_three_d_source_path}" \
        --output "${three_d_png_path}"

    unicode="$(jq -er '.unicode' "${metadata_path}")"
    glyph="$(jq -er '.glyph' "${metadata_path}")"
    cldr_name="$(jq -er '.cldr' "${metadata_path}")"
    group="$(jq -er '.group' "${metadata_path}")"
    resource_key="fluent_${unicode// /_}"
    color_output_path="${working_output_dir}/color/${resource_key}.webp"
    three_d_output_path="${working_output_dir}/3d/${resource_key}.webp"

    if [[ -e "${color_output_path}" || -e "${three_d_output_path}" ]]; then
        echo "Duplicate Unicode/resource key: ${unicode}" >&2
        exit 1
    fi

    rsvg-convert \
        --width "${IMAGE_SIZE}" \
        --height "${IMAGE_SIZE}" \
        --keep-aspect-ratio \
        --output "${color_png_path}" \
        "${color_svg_path}"
    cwebp \
        -quiet \
        -q "${WEBP_QUALITY}" \
        -alpha_q 100 \
        "${color_png_path}" \
        -o "${color_output_path}"
    cwebp \
        -quiet \
        -q "${WEBP_QUALITY}" \
        -alpha_q 100 \
        -resize "${IMAGE_SIZE}" "${IMAGE_SIZE}" \
        "${three_d_png_path}" \
        -o "${three_d_output_path}"

    jq -cn \
        --arg unicode "${unicode}" \
        --arg glyph "${glyph}" \
        --arg resourceKey "${resource_key}" \
        --arg group "${group}" \
        --arg cldrName "${cldr_name}" \
        --arg colorSourcePath "${color_source_path}" \
        --arg threeDSourcePath "${three_d_source_path}" \
        --arg koreanAliases "${korean_aliases}" \
        --slurpfile metadata "${metadata_path}" \
        '{
            unicode: $unicode,
            glyph: $glyph,
            resourceKey: $resourceKey,
            group: $group,
            cldrName: $cldrName,
            keywords: $metadata[0].keywords,
            koreanAliases: ($koreanAliases | split(",")),
            sourcePaths: {
                color: $colorSourcePath,
                "3d": $threeDSourcePath
            }
        }' >> "${catalog_items_path}"
done < "${MANIFEST_PATH}"

jq -s \
    --arg repository "https://github.com/${UPSTREAM_REPOSITORY}" \
    --arg commit "${UPSTREAM_COMMIT}" \
    --argjson imageSize "${IMAGE_SIZE}" \
    '{
        sourceRepository: $repository,
        sourceCommit: $commit,
        styles: ["Color", "3D"],
        imageSizePx: $imageSize,
        items: sort_by(.unicode)
    }' \
    "${catalog_items_path}" > "${working_output_dir}/catalog.json"

item_count="$(jq '.items | length' "${working_output_dir}/catalog.json")"
unique_count="$(jq '[.items[].unicode] | unique | length' "${working_output_dir}/catalog.json")"
color_asset_bytes="$(directory_bytes "${working_output_dir}/color")"
three_d_asset_bytes="$(directory_bytes "${working_output_dir}/3d")"

if [[ "${item_count}" -ne "${unique_count}" ]]; then
    echo "Catalog contains duplicated Unicode values." >&2
    exit 1
fi

if [[ "${item_count}" -ne 20 ]]; then
    echo "Spike catalog must contain exactly 20 items, but found ${item_count}." >&2
    exit 1
fi

jq -n \
    --arg repository "https://github.com/${UPSTREAM_REPOSITORY}" \
    --arg commit "${UPSTREAM_COMMIT}" \
    --arg rsvgVersion "${rsvg_version}" \
    --arg cwebpVersion "${cwebp_version}" \
    --argjson itemCount "${item_count}" \
    --argjson colorAssetBytes "${color_asset_bytes}" \
    --argjson threeDAssetBytes "${three_d_asset_bytes}" \
    '{
        sourceRepository: $repository,
        sourceCommit: $commit,
        imageSizePx: 128,
        webpQuality: 92,
        converterVersions: {
            rsvgConvert: $rsvgVersion,
            cwebp: $cwebpVersion
        },
        sampleItemCount: $itemCount,
        styles: {
            color: {
                sampleAssetBytes: $colorAssetBytes,
                averageAssetBytes: (($colorAssetBytes / $itemCount) | floor),
                projectedAssetBytes: {
                    "100": (($colorAssetBytes / $itemCount * 100) | floor),
                    "200": (($colorAssetBytes / $itemCount * 200) | floor),
                    "300": (($colorAssetBytes / $itemCount * 300) | floor)
                }
            },
            "3d": {
                sampleAssetBytes: $threeDAssetBytes,
                averageAssetBytes: (($threeDAssetBytes / $itemCount) | floor),
                projectedAssetBytes: {
                    "100": (($threeDAssetBytes / $itemCount * 100) | floor),
                    "200": (($threeDAssetBytes / $itemCount * 200) | floor),
                    "300": (($threeDAssetBytes / $itemCount * 300) | floor)
                }
            }
        }
    }' > "${working_output_dir}/size-report.json"

rm -rf "${OUTPUT_DIR}"
mv "${working_output_dir}" "${OUTPUT_DIR}"

echo "Generated ${item_count} Color and 3D assets in ${OUTPUT_DIR}."
echo "Color: ${color_asset_bytes} bytes, 3D: ${three_d_asset_bytes} bytes."
