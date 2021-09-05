[![Logisim-evolution](logisim-evolution-logo.svg)](https://github.com/logisim-evolution/logisim-evolution)

---

# Logisim-evolution project artwork #

Designed by Marcin Orlowski <http://MarcinOrlowski.com>

## SVG files ##

* `logisim-evolution-logo-src.svg` - main logo project file (with texts as editable strings),
* `logisim-evolution-logo.svg` - derived from main project file with all texts converted to paths,
* `logisim-evolution-icon.svg` - project icon.

"NOTE: To properly render the `logisim-evolution-logo-src.svg` (be it in-app, on the web page, or elsewhere)
one must have all the used fonts installed on the build machine. Otherwise, your renderer/browser will substitute
the fonts using available ones, which yields an incorrect image. Alternatively, you can either render
your image to a bitmap (PNG image format is recommended, due to its lossless compression and transparency support)
or convert all texts to paths (in [Inkscape](https://inkscape.org/): "Object" menu -> "Object to Path").
However, this will make the SVG file bigger (see logisim-evolution-logo.svg, which is exactly such
a font-less version)."

## In-app logo ##

The [in-app PNG rendition of the logo](../src/main/resources/resources/logisim/img/logisim-evolution-logo.png)
must be rendered as image of 200 px height and proportional width (but less than 600 px) or
[About.java](../src/main/java/com/cburch/logisim/gui/start/About.java) constants needs to be adjusted to match.

When updating icons, the following locations currently hold a PNG copy of the icon:

* in-app (LFrame) icon:
  * src/main/resources/resources/logisim/img/logisim-icon-*.png
* jpackage:
  * support/jpackage/linux/*.png
  * support/jpackage/linux/*.icns
  * support/jpackage/linux/*.ico

## Tools ##

Use `update_assets.sh` shell script to regenerated all PNG icons and logos using SVG source files.
NOTE: please see script header for list of required external utilties you need to install first!

## Fonts ##

The following fonts were used:

* [Electronic Circuit](https://textfonts.net/electronic-circuit-font.html),
* [A Dripping Marker](https://www.1001freefonts.com/a-dripping-marker.font) by Wick van den Belt.
