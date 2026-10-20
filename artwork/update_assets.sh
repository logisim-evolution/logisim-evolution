#!/bin/bash

# Generates PNG assets from SVG source files.

# Dependencies:
# - apt install -y inkscape icoutils icnsutils

set -euo pipefail

# Sizes as supported by png2cns: https://icns.sourceforge.io/apidocs.html (Icon Type Constants).
declare -r SIZES=(16 32 48 128 256)

# Project root
declare -r ROOT="../"

# SVG folder
declare -r SRC_DIR="./"
# Target directory for generated PNG files.
declare -r DEST_DIR="${ROOT}/build/resources/icons/"
# local -r dest="${ROOT}build/resources/main/resources/logisim/img/"

declare -r SUPPORT_DIR="${ROOT}/support/jpackage/"
declare -r IMG_DIR="${ROOT}/src/main/resources/resources/logisim/img/"

# Generates PNG file from SVG source.
function svg_to_png {
	local -r src="${1:-}"
	local -r dest="${2:-}"
	local -r width="${3:-}"

	local -r opts=(
		"--export-overwrite"
		"--export-background-opacity=0"
		"--export-type=png"
		"--export-png-color-mode=RGBA_8"
	)

	inkscape ${opts[*]} --export-width="${width}" --export-filename="${dest}" "${src}"
}

# Generates all required size PNG icons.
function generate_png_icons {
	local -r svg="${SRC_DIR}/logisim-evolution-icon.svg"
	mkdir -p "${DEST_DIR}"
	for width in ${SIZES[*]}; do
		svg_to_png "${svg}" "${DEST_DIR}/logisim-icon-${width}.png" "${width}"
	done
}

# ##############################################################################

# Update logo PNG
declare -r logo_src="${SRC_DIR}/logisim-evolution-logo.svg"
declare -r logo_width=550
declare -r logo_dest="${IMG_DIR}/logisim-evolution-logo.png"
svg_to_png "${logo_src}" "${logo_dest}" "${logo_width}"

# Update icon PNGs
generate_png_icons


src_pngs=()
for width in ${SIZES[*]}; do src_pngs+=("${DEST_DIR}/logisim-icon-${width}.png"); done

# Copy icons into sources
for icon in ${src_pngs[*]}; do
	cp -f "${icon}" "${IMG_DIR}/$(basename "${icon}")"
done


# Copy icon for Linux installation packages.
cp -f "${DEST_DIR}/logisim-icon-128.png" "${SUPPORT_DIR}/linux/"

# build macOS icns package using:
# Linux: icnsutils package
# macOS: ???
# Windows: ???
for suffix in "" "-circ"; do
	png2icns "${SUPPORT_DIR}/macos/Logisim-evolution-${suffix}.icns" ${src_pngs[*]}
done

# build Windows ico file using:
# Linux: icoutils package
# macOS: ???
# Windows: http://www.telegraphics.com.au/sw/product/ICOBundle (???)
icotool -c -o "${SUPPORT_DIR}/windows/Logisim-evolution.ico" ${src_pngs[*]}
