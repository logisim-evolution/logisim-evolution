[![Logisim-evolution](logisim-evolution-logo.svg)](https://github.com/logisim-evolution/logisim-evolution)

---

# Logisim-evolution project logo #

Designed by Marcin Orlowski <http://MarcinOrlowski.com>

## SVG files ##

* `logisim-evolution-logo-src.svg` - main logo project file
* `logisim-evolution-logo.svg` - derived from main project file with all texts converted to paths.

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

## Fonts ##

The following fonts were used:

* [Electronic Circuit](https://textfonts.net/electronic-circuit-font.html),
* [A Dripping Marker](https://www.1001freefonts.com/a-dripping-marker.font) by Wick van den Belt.
