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
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.val;
import org.w3c.dom.Element;

public class SvgReader {
  private static final Pattern PATH_REGEX = Pattern.compile("[a-zA-Z]|[-0-9.]+");

  private SvgReader() {
    // dummy
  }

  private static AbstractCanvasObject createLine(Element elt) {
    val x0 = Integer.parseInt(elt.getAttribute("x1"));
    val y0 = Integer.parseInt(elt.getAttribute("y1"));
    val x1 = Integer.parseInt(elt.getAttribute("x2"));
    val y1 = Integer.parseInt(elt.getAttribute("y2"));
    return new Line(x0, y0, x1, y1);
  }

  private static AbstractCanvasObject createOval(Element elt) {
    val cx = Double.parseDouble(elt.getAttribute("cx"));
    val cy = Double.parseDouble(elt.getAttribute("cy"));
    val rx = Double.parseDouble(elt.getAttribute("rx"));
    val ry = Double.parseDouble(elt.getAttribute("ry"));
    val x = (int) Math.round(cx - rx);
    val y = (int) Math.round(cy - ry);
    val w = (int) Math.round(rx * 2);
    val h = (int) Math.round(ry * 2);
    return new Oval(x, y, w, h);
  }

  private static AbstractCanvasObject createPath(Element elt) {
    val TYPE_ERROR = -1;
    val TYPE_START = 0;
    val TYPE_CURVE = 1;
    val TYPE_POLYLINE = 2;

    Matcher patt = PATH_REGEX.matcher(elt.getAttribute("d"));
    List<String> tokens = new ArrayList<>();
    var type = TYPE_ERROR;
    while (patt.find()) {
      String token = patt.group();
      tokens.add(token);
      if (Character.isLetter(token.charAt(0))) {
        switch (token.charAt(0)) {
          case 'M':
            type = (type == TYPE_ERROR) ? TYPE_START : TYPE_ERROR;
            break;
          case 'Q':
          case 'q':
            type = (type == TYPE_START) ? TYPE_CURVE : TYPE_ERROR;
            break;
            // not supported case 'L': case 'l': case 'H': case 'h': case
            //'V': case 'v': if (type == 0 || type == 2) type = 2; else
            // type = -1; break;
          default:
            type = TYPE_ERROR;
        }
        if (type == TYPE_ERROR) {
          throw new NumberFormatException("Unrecognized path command '" + token.charAt(0) + "'");
        }
      }
    }

    if (type != TYPE_CURVE) {
      throw new NumberFormatException("Unrecognized path: " + type);
    }
    if (tokens.size() == 8
        && tokens.get(0).equals("M")
        && tokens.get(3).equalsIgnoreCase("Q")) {
      int x0 = Integer.parseInt(tokens.get(1));
      int y0 = Integer.parseInt(tokens.get(2));
      int x1 = Integer.parseInt(tokens.get(4));
      int y1 = Integer.parseInt(tokens.get(5));
      int x2 = Integer.parseInt(tokens.get(6));
      int y2 = Integer.parseInt(tokens.get(7));
      if (tokens.get(3).equals("q")) {
        x1 += x0;
        y1 += y0;
        x2 += x0;
        y2 += y0;
      }
      Location e0 = Location.create(x0, y0);
      Location e1 = Location.create(x2, y2);
      Location ct = Location.create(x1, y1);
      return new Curve(e0, e1, ct);
    } else {
      throw new NumberFormatException("Unexpected format for curve");
    }
  }

  private static AbstractCanvasObject createPolygon(Element elt) {
    return new Poly(true, parsePoints(elt.getAttribute("points")));
  }

  private static AbstractCanvasObject createPolyline(Element elt) {
    return new Poly(false, parsePoints(elt.getAttribute("points")));
  }

  private static AbstractCanvasObject createRectangle(Element elt) {
    val x = Integer.parseInt(elt.getAttribute("x"));
    val y = Integer.parseInt(elt.getAttribute("y"));
    val w = Integer.parseInt(elt.getAttribute("width"));
    val h = Integer.parseInt(elt.getAttribute("height"));
    if (elt.hasAttribute("rx")) {
      val ret = new RoundRectangle(x, y, w, h);
      val rx = Integer.parseInt(elt.getAttribute("rx"));
      ret.setValue(DrawAttr.CORNER_RADIUS, rx);
      return ret;
    }
    return new Rectangle(x, y, w, h);
  }

  public static AbstractCanvasObject createShape(Element elt) {
    val name = elt.getTagName();
    val ret = createShapeObject(elt, name);
    if (ret == null) {
      return null;
    }
    var attrs = ret.getAttributes();
    if (attrs.contains(DrawAttr.PAINT_TYPE)) {
      val stroke = elt.getAttribute("stroke");
      val fill = elt.getAttribute("fill");
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
      val width = Integer.valueOf(elt.getAttribute("stroke-width"));
      ret.setValue(DrawAttr.STROKE_WIDTH, width);
    }
    if (attrs.contains(DrawAttr.STROKE_COLOR)) {
      val color = elt.getAttribute("stroke");
      val opacity = elt.getAttribute("stroke-opacity");
      if (!color.equals("none")) {
        ret.setValue(DrawAttr.STROKE_COLOR, getColor(color, opacity));
      }
    }
    if (attrs.contains(DrawAttr.FILL_COLOR)) {
      var color = elt.getAttribute("fill");
      if (color.equals("")) color = "#000000";
      val opacity = elt.getAttribute("fill-opacity");
      if (!color.equals("none")) {
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
    val x = Integer.parseInt(elt.getAttribute("x"));
    val y = Integer.parseInt(elt.getAttribute("y"));
    val text = elt.getTextContent();
    val ret = new Text(x, y, text);

    val fontFamily = elt.getAttribute("font-family");
    val fontStyle = elt.getAttribute("font-style");
    val fontWeight = elt.getAttribute("font-weight");
    val fontSize = elt.getAttribute("font-size");
    var styleFlags = Font.PLAIN;
    if (fontStyle.equals("italic")) styleFlags |= Font.ITALIC;
    if (fontWeight.equals("bold")) styleFlags |= Font.BOLD;
    val size = Integer.parseInt(fontSize);
    ret.setValue(DrawAttr.FONT, new Font(fontFamily, styleFlags, size));

    val hAlignStr = elt.getAttribute("text-anchor");
    AttributeOption horizAlign;
    if (hAlignStr.equals("start")) {
      horizAlign = DrawAttr.HALIGN_LEFT;
    } else if (hAlignStr.equals("end")) {
      horizAlign = DrawAttr.HALIGN_RIGHT;
    } else {
      horizAlign = DrawAttr.HALIGN_CENTER;
    }
    ret.setValue(DrawAttr.HALIGNMENT, horizAlign);

    val vertAlignStr = elt.getAttribute("dominant-baseline");
    val vertAlign = getAlignment(vertAlignStr);
    ret.setValue(DrawAttr.VALIGNMENT, vertAlign);

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
    if (fontFamily == null || fontFamily.length() == 0) fontFamily = defaultFamily;
    var fontStyle = elt.getAttribute(prefix + "font-style");
    if (fontStyle == null || fontStyle.length() == 0) fontStyle = "plain";
    var fontWeight = elt.getAttribute(prefix + "font-weight");
    if (fontWeight == null || fontWeight.length() == 0) fontWeight = "plain";
    val fontSize = elt.getAttribute(prefix + "font-size");
    var styleFlags = Font.PLAIN;
    if (fontStyle.equals("italic")) styleFlags |= Font.ITALIC;
    if (fontWeight.equals("bold")) styleFlags |= Font.BOLD;
    val size = (fontSize != null && fontSize.length() > 0 ? Integer.parseInt(fontSize) : defaultSize);
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
        int comma = opacity.lastIndexOf(',');
        if (comma >= 0) {
          try {
            String repl = opacity.substring(0, comma) + "." + opacity.substring(comma + 1);
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
    val patt = Pattern.compile("[ ,\n\r\t]+");
    val tokens = patt.split(points);
    val ret = new Location[tokens.length / 2];
    for (var i = 0; i < ret.length; i++) {
      val x = Integer.parseInt(tokens[2 * i]);
      val y = Integer.parseInt(tokens[2 * i + 1]);
      ret[i] = Location.create(x, y);
    }
    return UnmodifiableList.create(ret);
  }
}
