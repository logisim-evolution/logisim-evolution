[![Logisim-evolution](logisim-evolution-logo.png)](https://github.com/logisim-evolution/logisim-evolution)

---

# Logisim-evolution project artwork #

Designed by Marcin Orlowski <http://MarcinOrlowski.com>

## SVG files ##

* `logisim-evolution-logo.svg` - main logo project file (with texts as editable strings),
* `logisim-evolution-logo.png` - bitmap rendition of the the project logo to be used mainly in documentation files,
* `logisim-evolution-icon.svg` - project icon.

"NOTE: To properly render the `logisim-evolution-logo.svg` (be it in-app, on the web page, or elsewhere)
one must have all the used fonts installed on the build machine. Otherwise, your renderer/browser will substitute
the fonts using available ones, which yields an incorrect image. Alternatively, you can either render
your image to a bitmap (PNG image format is recommended, due to its lossless compression and transparency support)
or convert all texts to paths (in [Inkscape](https://inkscape.org/): "Object" menu -> "Object to Path").

## In-app logo ##

The [in-app PNG rendition of the logo](../src/main/resources/resources/logisim/img/logisim-evolution-logo.png)
must be rendered as image of 200 px height and proportional width (but less than 600 px) or
[About.java](../src/main/java/com/cburch/logisim/gui/start/About.java) constants needs to be adjusted to match.

When updating icons, the following locations currently hold a PNG copy of the icon:

* in-app (LFrame) icon:
  * `src/main/resources/resources/logisim/img/logisim-icon-*.png`
* jpackage:
  * `support/jpackage/linux/*.png`
  * `support/jpackage/linux/*.icns`
  * `support/jpackage/linux/*.ico`

Also update configuration of Github repo and organization:

* repo settings -> Options -> Social preview (upload 1280x640 rendition of main logo)
* organization profile icon (use one of hi-res PNGs images from set of [in-app icons](..src/main/resources/resources/logisim/img/)).

## Tools ##

Use `update_assets.sh` shell script to regenerated all PNG icons and logos using SVG source files.

IMPORTANT NOTES:

* You must have Inkscape, icoutils and icnsutils installed first as script needs these tools.
* you **MUST** install logo fonts before running this script or output (mainly logo) will be broken.

## Fonts ##

The following fonts were used:

* [Electronic Circuit](https://textfonts.net/electronic-circuit-font.html),
* [A Dripping Marker](https://www.1001freefonts.com/a-dripping-marker.font) by Wick van den Belt.
