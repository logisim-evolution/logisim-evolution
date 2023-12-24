/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.w3c.dom.Element;

public final class SvgReader {
  private static final Pattern PATH_REGEX = Pattern.compile("[a-zA-Z]|[-\\d.]+");

  private SvgReader() {
    // dummy
  }

  private static AbstractCanvasObject createLine(Element elt) {
    final var x0 = Integer.parseInt(elt.getAttribute("x1"));
    final var y0 = Integer.parseInt(elt.getAttribute("y1"));
    final var x1 = Integer.parseInt(elt.getAttribute("x2"));
    final var y1 = Integer.parseInt(elt.getAttribute("y2"));
    return new Line(x0, y0, x1, y1);
  }

  private static AbstractCanvasObject createOval(Element elt) {
    final var cx = Double.parseDouble(elt.getAttribute("cx"));
    final var cy = Double.parseDouble(elt.getAttribute("cy"));
    final var rx = Double.parseDouble(elt.getAttribute("rx"));
    final var ry = Double.parseDouble(elt.getAttribute("ry"));
    final var x = (int) Math.round(cx - rx);
    final var y = (int) Math.round(cy - ry);
    final var w = (int) Math.round(rx * 2);
    final var h = (int) Math.round(ry * 2);
    return new Oval(x, y, w, h);
  }

  private static AbstractCanvasObject createPath(Element elt) {
    final var typeError = -1;
    final var patt = PATH_REGEX.matcher(elt.getAttribute("d"));
    final var tokens = new ArrayList<String>();
    var type = -1; // -1 error, 0 start, 1 curve, 2 polyline
    while (patt.find()) {
      final var token = patt.group();
      tokens.add(token);
      if (Character.isLetter(token.charAt(0))) {
        type = switch (token.charAt(0)) {
          case 'M' -> (type == typeError) ? 0 : typeError;
          case 'Q', 'q' -> (type == 0) ? 1 : typeError;
          /*
           * not supported case 'L': case 'l': case 'H': case 'h': case
           * 'V': case 'v': if (type == 0 || type == 2) type = 2; else
           * type = -1; break;
           */
          default -> typeError;
        };
        if (type == typeError) {
          final var tokenStr = String.valueOf(token.charAt(0));
          final var msg = String.format("Unrecognized path command '%s'", tokenStr);
          throw new NumberFormatException(msg);
        }
      }
    }

    if (type == 1) {
      if (tokens.size() == 8
          && "M".equals(tokens.get(0))
          && "Q".equalsIgnoreCase(tokens.get(3))) {
        final var x0 = Integer.parseInt(tokens.get(1));
        final var y0 = Integer.parseInt(tokens.get(2));
        var x1 = Integer.parseInt(tokens.get(4));
        var y1 = Integer.parseInt(tokens.get(5));
        var x2 = Integer.parseInt(tokens.get(6));
        var y2 = Integer.parseInt(tokens.get(7));
        if ("q".equals(tokens.get(3))) {
          x1 += x0;
          y1 += y0;
          x2 += x0;
          y2 += y0;
        }
        final var e0 = Location.create(x0, y0, false);
        final var e1 = Location.create(x2, y2, false);
        final var ct = Location.create(x1, y1, false);
        return new Curve(e0, e1, ct);
      } else {
        throw new NumberFormatException("Unexpected format for curve");
      }
    } else {
      throw new NumberFormatException("Unrecognized path");
    }
  }

  private static AbstractCanvasObject createPolygon(Element elt) {
    return new Poly(true, parsePoints(elt.getAttribute("points")));
  }

  private static AbstractCanvasObject createPolyline(Element elt) {
    return new Poly(false, parsePoints(elt.getAttribute("points")));
  }

  private static AbstractCanvasObject createRectangle(Element elt) {
    final var x = Integer.parseInt(elt.getAttribute("x"));
    final var y = Integer.parseInt(elt.getAttribute("y"));
    final var w = Integer.parseInt(elt.getAttribute("width"));
    final var h = Integer.parseInt(elt.getAttribute("height"));
    if (elt.hasAttribute("rx")) {
      final var ret = new RoundRectangle(x, y, w, h);
      final var rx = Integer.parseInt(elt.getAttribute("rx"));
      ret.setValue(DrawAttr.CORNER_RADIUS, rx);
      return ret;
    } else {
      return new Rectangle(x, y, w, h);
    }
  }

  private static AbstractCanvasObject createImage(Element elt) {
    final var x = Integer.parseInt(elt.getAttribute("x"));
    final var y = Integer.parseInt(elt.getAttribute("y"));
    final var w = Integer.parseInt(elt.getAttribute("width"));
    final var h = Integer.parseInt(elt.getAttribute("height"));
    return new Image(elt.getAttribute("imageData"), x, y, w, h);
  }

  public static AbstractCanvasObject createShape(Element elt) {
    final var name = elt.getTagName();
    final var ret = createShapeObject(elt, name);
    if (ret == null) {
      return null;
    }
    var attrs = ret.getAttributes();
    if (attrs.contains(DrawAttr.PAINT_TYPE)) {
      final var stroke = elt.getAttribute("stroke");
      final var fill = elt.getAttribute("fill");
      if (stroke.isEmpty() || "none".equals(stroke)) {
        ret.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
      } else if ("none".equals(fill)) {
        ret.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_STROKE);
      } else {
        ret.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_STROKE_FILL);
      }
    }
    attrs = ret.getAttributes(); // since changing paintType could change it
    if (attrs.contains(DrawAttr.STROKE_WIDTH) && elt.hasAttribute("stroke-width")) {
      final var width = Integer.valueOf(elt.getAttribute("stroke-width"));
      ret.setValue(DrawAttr.STROKE_WIDTH, width);
    }
    if (attrs.contains(DrawAttr.STROKE_COLOR)) {
      final var color = elt.getAttribute("stroke");
      final var opacity = elt.getAttribute("stroke-opacity");
      if (!"none".equals(color)) {
        ret.setValue(DrawAttr.STROKE_COLOR, getColor(color, opacity));
      }
    }
    if (attrs.contains(DrawAttr.FILL_COLOR)) {
      var color = elt.getAttribute("fill");
      // FIXME: hardcoded color value
      if (color.isEmpty()) color = "#000000";
      final var opacity = elt.getAttribute("fill-opacity");
      if (!"none".equals(color)) {
        ret.setValue(DrawAttr.FILL_COLOR, getColor(color, opacity));
      }
    }
    return ret;
  }

  private static AbstractCanvasObject createShapeObject(Element elt, String name) {
    return switch (name) {
      case "ellipse" -> createOval(elt);
      case "line" -> createLine(elt);
      case "path" -> createPath(elt);
      case "polyline" -> createPolyline(elt);
      case "polygon" -> createPolygon(elt);
      case "rect" -> createRectangle(elt);
      case "text" -> createText(elt);
      case "image" -> createImage(elt);
      default -> null;
    };
  }

  private static AbstractCanvasObject createText(Element elt) {
    final var x = Integer.parseInt(elt.getAttribute("x"));
    final var y = Integer.parseInt(elt.getAttribute("y"));
    final var text = elt.getTextContent();
    final var ret = new Text(x, y, text);

    final var fontFamily = elt.getAttribute("font-family");
    final var fontStyle = elt.getAttribute("font-style");
    final var fontWeight = elt.getAttribute("font-weight");
    final var fontSize = elt.getAttribute("font-size");
    var styleFlags = Font.PLAIN;
    if (isItalic(fontStyle)) styleFlags |= Font.ITALIC;
    if (isBold(fontWeight)) styleFlags |= Font.BOLD;
    final var size = Integer.parseInt(fontSize);
    ret.setValue(DrawAttr.FONT, new Font(fontFamily, styleFlags, size));

    final var hAlignStr = elt.getAttribute("text-anchor");
    AttributeOption hAlign;
    if ("start".equals(hAlignStr)) {
      hAlign = DrawAttr.HALIGN_LEFT;
    } else if ("end".equals(hAlignStr)) {
      hAlign = DrawAttr.HALIGN_RIGHT;
    } else {
      hAlign = DrawAttr.HALIGN_CENTER;
    }
    ret.setValue(DrawAttr.HALIGNMENT, hAlign);

    final var vAlignStr = elt.getAttribute("dominant-baseline");
    final var vAlign = getAlignment(vAlignStr);
    ret.setValue(DrawAttr.VALIGNMENT, vAlign);

    // fill color is handled after we return
    return ret;
  }

  private static AttributeOption getAlignment(String valignStr) {
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

  private static List<Location> parsePoints(String points) {
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

  private static boolean isBold(String fontStyle) {
    return "bold".equals(fontStyle);
  }
  private static boolean isItalic(String fontStyle) {
    return "italic".equals(fontStyle);
  }
}
