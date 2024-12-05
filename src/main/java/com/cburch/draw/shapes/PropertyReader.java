package com.cburch.draw.shapes;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.regex.Pattern;
import org.w3c.dom.Element;

public final class PropertyReader {

    public static AttributeOption getAlignment(String valignStr) {
        return switch (valignStr) {
            case "top" -> DrawAttr.VALIGN_TOP;
            case "bottom" -> DrawAttr.VALIGN_BOTTOM;
            case "alphabetic" -> DrawAttr.VALIGN_BASELINE;
            default -> DrawAttr.VALIGN_MIDDLE;
        };
    }

    public static Font getFontAttribute(Element elt, String prefix, String defaultFamily, int defaultSize) {
        var fontFamily = elt.getAttribute(prefix + "font-family");
        var fontStyle = elt.getAttribute(prefix + "font-style");
        var fontWeight = elt.getAttribute(prefix + "font-weight");
        final var fontSize = elt.getAttribute(prefix + "font-size");

        if (StringUtil.isNullOrEmpty(fontFamily)) fontFamily = defaultFamily;
        if (StringUtil.isNullOrEmpty(fontStyle)) fontStyle = "plain";
        if (StringUtil.isNullOrEmpty(fontWeight)) fontWeight = "plain";
        var styleFlags = Font.PLAIN;
        if (isItalic(fontStyle)) styleFlags |= Font.ITALIC;
        if (isBold(fontWeight)) styleFlags |= Font.BOLD;

        var size = defaultSize;
        if (StringUtil.isNotEmpty(fontSize)) {
            try {
                size = Integer.parseInt(fontSize);
            } catch (NumberFormatException ignored) {
                // Do nothing, we are using defaultSize
            }
        }
        return new Font(fontFamily, styleFlags, size);
    }

    /**
     * Process color/opactiy string representation and returns instance of `Color`.
     *
     * @param hue Color value in HTML format, with `#` as prefix, i.e. #RRGGBB
     * @param opacity opacity, as floating point (in from 0 to 1 range).
     */
    public static Color getColor(String hue, String opacity) {
        var r = 0;
        var g = 0;
        var b = 0;
        final var colorStrLen = 7;
        if (StringUtil.isNotEmpty(hue) && hue.length() == colorStrLen) {
            try {
                r = Integer.parseInt(hue.substring(1, 3), 16);
                g = Integer.parseInt(hue.substring(3, 5), 16);
                b = Integer.parseInt(hue.substring(5, 7), 16);
            } catch (NumberFormatException ignored) {
                // Do nothing and stick to defaults.
            }
        }
        var alpha = 255;
        if (StringUtil.isNotEmpty(opacity)) {
            double tmpOpacity;
            try {
                tmpOpacity = Double.parseDouble(opacity);
            } catch (NumberFormatException exception) {
                // Some localizations use commas for decimal points, so let's try to deal with it.
                final var commaIdx = opacity.lastIndexOf(',');
                // No comma. Got no idea why it failed then, so rethrow
                // FIXME: shall we really throw here? What about falling back to defaults?
                if (commaIdx < 0) throw exception;
                try {
                    final var repl = opacity.substring(0, commaIdx) + "." + opacity.substring(commaIdx + 1);
                    tmpOpacity = Double.parseDouble(repl);
                } catch (Throwable t) {
                    // FIXME: shall we really throw here? What about falling back to defaults?
                    throw exception;
                }
            }
            alpha = (int) Math.round(tmpOpacity * 255);
        }
        return new Color(r, g, b, alpha);
    }

    public static List<Location> parsePoints(String points) {
        final var patt = Pattern.compile("[ ,\n\r\t]+");
        final var toks = patt.split(points);
        final var ret = new Location[toks.length / 2];
        for (var i = 0; i < ret.length; i++) {
            final var x = Integer.parseInt(toks[2 * i]);
            final var y = Integer.parseInt(toks[2 * i + 1]);
            ret[i] = Location.create(x, y, false);
        }
        return UnmodifiableList.create(ret);
    }

    public static boolean isBold(String fontStyle) {
        return "bold".equals(fontStyle);
    }
    public static boolean isItalic(String fontStyle) {
        return "italic".equals(fontStyle);
    }
}
