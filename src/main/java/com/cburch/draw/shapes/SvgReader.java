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
  private static final Pattern PATH_REGEX = Pattern.compile("[a-zA-Z]|[-0-9.]+");

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
    final var patt = PATH_REGEX.matcher(elt.getAttribute("d"));
    final var tokens = new ArrayList<String>();
    var type = -1; // -1 error, 0 start, 1 curve, 2 polyline
    while (patt.find()) {
      final var token = patt.group();
      tokens.add(token);
      if (Character.isLetter(token.charAt(0))) {
        switch (token.charAt(0)) {
          case 'M':
            if (type == -1) type = 0;
            else type = -1;
            break;
          case 'Q', 'q':
            if (type == 0) type = 1;
            else type = -1;
            break;
            /*
             * not supported case 'L': case 'l': case 'H': case 'h': case
             * 'V': case 'v': if (type == 0 || type == 2) type = 2; else
             * type = -1; break;
             */
          default:
            type = -1;
        }
        if (type == -1) {
          throw new NumberFormatException("Unrecognized path command '" + token.charAt(0) + "'");
        }
      }
    }

    if (type == 1) {
      if (tokens.size() == 8
          && tokens.get(0).equals("M")
          && tokens.get(3).equalsIgnoreCase("Q")) {
        final var x0 = Integer.parseInt(tokens.get(1));
        final var y0 = Integer.parseInt(tokens.get(2));
        var x1 = Integer.parseInt(tokens.get(4));
        var y1 = Integer.parseInt(tokens.get(5));
        var x2 = Integer.parseInt(tokens.get(6));
        var y2 = Integer.parseInt(tokens.get(7));
        if (tokens.get(3).equals("q")) {
          x1 += x0;
          y1 += y0;
          x2 += x0;
          y2 += y0;
        }
        final var e0 = Location.create(x0, y0);
        final var e1 = Location.create(x2, y2);
        final var ct = Location.create(x1, y1);
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
      if (stroke.equals("") || stroke.equals("none")) {
        ret.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
      } else if (fill.equals("none")) {
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
      if (color.equals("")) color = "#000000";
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
    final var size = (fontSize != null && fontSize.length() > 0) ? Integer.parseInt(fontSize) : defaultSize;

    return new Font(fontFamily, styleFlags, size);
  }

  public static Color getColor(String hue, String opacity) {
    int r;
    int g;
    int b;
    if (hue == null || hue.equals("")) {
      r = 0;
      g = 0;
      b = 0;
    } else {
      r = Integer.parseInt(hue.substring(1, 3), 16);
      g = Integer.parseInt(hue.substring(3, 5), 16);
      b = Integer.parseInt(hue.substring(5, 7), 16);
    }
    int a;
    if (opacity == null || opacity.equals("")) {
      a = 255;
    } else {
      /*
       * Patch taken from Cornell's version of Logisim:
       * http://www.cs.cornell.edu/courses/cs3410/2015sp/
       */
      double x;
      try {
        x = Double.parseDouble(opacity);
      } catch (NumberFormatException e) {
        // some localizations use commas for decimal points
        final var comma = opacity.lastIndexOf(',');
        if (comma >= 0) {
          try {
            final var repl = opacity.substring(0, comma) + "." + opacity.substring(comma + 1);
            x = Double.parseDouble(repl);
          } catch (Throwable t) {
            throw e;
          }
        } else {
          throw e;
        }
      }
      a = (int) Math.round(x * 255);
    }
    return new Color(r, g, b, a);
  }

  private static List<Location> parsePoints(String points) {
    final var patt = Pattern.compile("[ ,\n\r\t]+");
    final var toks = patt.split(points);
    final var ret = new Location[toks.length / 2];
    for (var i = 0; i < ret.length; i++) {
      final var x = Integer.parseInt(toks[2 * i]);
      final var y = Integer.parseInt(toks[2 * i + 1]);
      ret[i] = Location.create(x, y);
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
