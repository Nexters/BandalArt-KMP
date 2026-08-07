#!/usr/bin/env bash

set -euo pipefail
export TZ=UTC

readonly UPSTREAM_REPOSITORY="https://github.com/microsoft/fluentui-emoji.git"
readonly UPSTREAM_COMMIT="62ecdc0d7ca5c6df32148c169556bc8d3782fca4"
readonly IMAGE_SIZE="128"
readonly WEBP_QUALITY="92"
readonly REQUIRED_RSVG_VERSION="2.60.0"
readonly REQUIRED_CWEBP_VERSION="1.6.0"
readonly REQUIRED_ZIP_VERSION="3.0"

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly OUTPUT_DIR="${SCRIPT_DIR}/measurement"

for command_name in git jq node rsvg-convert cwebp zip shasum; do
    if ! command -v "${command_name}" >/dev/null 2>&1; then
        echo "Required command is missing: ${command_name}" >&2
        exit 1
    fi
done

rsvg_version="$(rsvg-convert --version | awk '{ print $3 }')"
cwebp_version="$(cwebp -version | head -n 1)"
zip_version="$(zip -v | awk 'NR == 2 { print $4 }')"
if [[ "${rsvg_version}" != "${REQUIRED_RSVG_VERSION}" ]]; then
    echo "rsvg-convert ${REQUIRED_RSVG_VERSION} is required, but found ${rsvg_version}." >&2
    exit 1
fi
if [[ "${cwebp_version}" != "${REQUIRED_CWEBP_VERSION}" ]]; then
    echo "cwebp ${REQUIRED_CWEBP_VERSION} is required, but found ${cwebp_version}." >&2
    exit 1
fi
if [[ "${zip_version}" != "${REQUIRED_ZIP_VERSION}" ]]; then
    echo "Info-ZIP ${REQUIRED_ZIP_VERSION} is required, but found ${zip_version}." >&2
    exit 1
fi

temporary_dir="$(mktemp -d)"
trap 'rm -rf "${temporary_dir}"' EXIT
upstream_dir="${temporary_dir}/upstream"
generated_dir="${temporary_dir}/generated"
working_output_dir="${temporary_dir}/measurement"
tree_path="${temporary_dir}/tree-paths.txt"
candidate_catalog_path="${working_output_dir}/catalog-candidate.json"

mkdir -p "${upstream_dir}" "${generated_dir}" "${working_output_dir}"
git -C "${upstream_dir}" init --quiet
git -C "${upstream_dir}" remote add origin "${UPSTREAM_REPOSITORY}"
git -C "${upstream_dir}" sparse-checkout init --no-cone
printf '/assets/*/metadata.json\n' | git -C "${upstream_dir}" sparse-checkout set --stdin
git -C "${upstream_dir}" fetch --quiet --depth=1 --filter=blob:none origin "${UPSTREAM_COMMIT}"
git -C "${upstream_dir}" checkout --quiet --detach FETCH_HEAD
git -C "${upstream_dir}" ls-tree -r --name-only FETCH_HEAD > "${tree_path}"

node "${SCRIPT_DIR}/select-catalog.mjs" \
    "${upstream_dir}" \
    "${tree_path}" \
    "${SCRIPT_DIR}/generated/catalog.json" \
    "${candidate_catalog_path}"

{
    printf '/assets/*/metadata.json\n'
    jq -r '.items[].colorSourcePath | "/" + .' "${candidate_catalog_path}"
} | git -C "${upstream_dir}" sparse-checkout set --stdin

while IFS=$'\t' read -r unicode resource_key color_source_path; do
    color_png_path="${temporary_dir}/${resource_key}.png"
    color_output_path="${generated_dir}/${resource_key}.webp"
    rsvg-convert \
        --width "${IMAGE_SIZE}" \
        --height "${IMAGE_SIZE}" \
        --keep-aspect-ratio \
        --output "${color_png_path}" \
        "${upstream_dir}/${color_source_path}"
    cwebp \
        -quiet \
        -q "${WEBP_QUALITY}" \
        -alpha_q 100 \
        "${color_png_path}" \
        -o "${color_output_path}"
done < <(jq -r '.items[] | [.unicode, .resourceKey, .colorSourcePath] | @tsv' "${candidate_catalog_path}")

generated_count="$(find "${generated_dir}" -name '*.webp' -type f | wc -l | tr -d '[:space:]')"
if [[ "${generated_count}" -ne 300 ]]; then
    echo "Expected 300 generated assets, but found ${generated_count}." >&2
    exit 1
fi

report_items_path="${temporary_dir}/report-items.ndjson"
: > "${report_items_path}"
for catalog_count in 100 200 300; do
    package_dir="${temporary_dir}/package-${catalog_count}"
    package_assets_dir="${package_dir}/assets"
    subset_catalog_path="${package_dir}/catalog.json"
    archive_path="${temporary_dir}/package-${catalog_count}.zip"
    mkdir -p "${package_assets_dir}"

    jq --argjson count "${catalog_count}" \
        '.catalogCount = $count | .items = .items[:$count]' \
        "${candidate_catalog_path}" > "${subset_catalog_path}"
    jq -r --argjson count "${catalog_count}" '.items[:$count][].resourceKey' \
        "${candidate_catalog_path}" | while IFS= read -r resource_key; do
        cp "${generated_dir}/${resource_key}.webp" "${package_assets_dir}/${resource_key}.webp"
    done

    asset_bytes="$(find "${package_assets_dir}" -name '*.webp' -type f -exec cat {} + | wc -c | tr -d '[:space:]')"
    catalog_bytes="$(wc -c < "${subset_catalog_path}" | tr -d '[:space:]')"
    find "${package_dir}" -type f -exec chmod 0644 {} +
    find "${package_dir}" -type f -exec touch -t 202001010000 {} +
    (
        cd "${package_dir}"
        find . -type f | LC_ALL=C sort | zip -X -q "${archive_path}" -@
    )
    zip_bytes="$(wc -c < "${archive_path}" | tr -d '[:space:]')"
    sha256="$(shasum -a 256 "${archive_path}" | awk '{ print $1 }')"

    jq -cn \
        --argjson catalogCount "${catalog_count}" \
        --argjson assetBytes "${asset_bytes}" \
        --argjson catalogBytes "${catalog_bytes}" \
        --argjson zipPayloadBytes "${zip_bytes}" \
        --arg sha256 "${sha256}" \
        '{
            catalogCount: $catalogCount,
            colorWebpBytes: $assetBytes,
            catalogJsonBytes: $catalogBytes,
            inputPayloadBytes: ($assetBytes + $catalogBytes),
            zipPayloadBytes: $zipPayloadBytes,
            zipSavingsBytes: ($assetBytes + $catalogBytes - $zipPayloadBytes),
            zipSha256: $sha256
        }' >> "${report_items_path}"
done

jq -s \
    --arg repository "${UPSTREAM_REPOSITORY}" \
    --arg commit "${UPSTREAM_COMMIT}" \
    --arg rsvgVersion "${rsvg_version}" \
    --arg cwebpVersion "${cwebp_version}" \
    --arg zipVersion "${zip_version}" \
    --argjson imageSize "${IMAGE_SIZE}" \
    --argjson webpQuality "${WEBP_QUALITY}" \
    '{
        sourceRepository: $repository,
        sourceCommit: $commit,
        imageSizePx: $imageSize,
        webpQuality: $webpQuality,
        normalizedZipTimestamp: "2020-01-01T00:00:00",
        normalizedFileMode: "0644",
        converterVersions: {
            rsvgConvert: $rsvgVersion,
            cwebp: $cwebpVersion,
            infoZip: $zipVersion
        },
        measurementType: "actual generated Color WebP files plus deterministic ZIP payload proxy",
        measurements: .
    }' "${report_items_path}" > "${working_output_dir}/measurement-report.json"

rm -rf "${OUTPUT_DIR}"
mv "${working_output_dir}" "${OUTPUT_DIR}"
echo "Measured actual 100, 200, and 300 item Color catalogs in ${OUTPUT_DIR}."
