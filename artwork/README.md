[![Logisim-evolution](logisim-evolution-logo.svg)](https://github.com/logisim-evolution/logisim-evolution)

---

# Logisim-evolution project logo #

Designed by Marcin Orlowski <http://MarcinOrlowski.com>

## SVG files ##

* `logisim-evolution-logo-src.svg` - main logo project file
* `logisim-evolution-logo.svg` - derived from main project file with all texts converted to paths.

NOTE: to properly render the `logisim-evolution-logo-src.svg` (be it in-app, on the web page or elsewhere)
one must have all the used fonts installed on target machine otherwise your renderer/browser will substitute
the fonts using available ones which resulting in incorrect final image. Alternatively you can either render
your image to a bitmap (i.e. PNG) or convert all texts to paths (in Inkscape: "Object" menu -> "Object to Path").
This will however make SVG file bigger (see `logisim-evolution-logo.svg` which is exactly such font-less version).

## In-app logo ##

The in-app PNG rendition of the logo (stored in
[src/main/resources/resources/logisim/img/logisim-evolution-logo.png](../src/main/resources/resources/logisim/img/logisim-evolution-logo.png) file)
must be rendered as image of 200px height and proportional width (but less than 600px)
or [About.java](../src/main/java/com/cburch/logisim/gui/start/About.java) consts needs to be adjusted.

## Fonts ##

The following fonts were used:

* [Electronic Circuit](https://textfonts.net/electronic-circuit-font.html),
* [A Dripping Marker](https://www.1001freefonts.com/a-dripping-marker.font) by Wick van den Belt.
