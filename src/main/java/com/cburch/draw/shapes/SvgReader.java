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
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;

public class SvgReader {
  private static final Pattern PATH_REGEX = Pattern.compile("[a-zA-Z]|[-0-9.]+");

  private SvgReader() {
    // dummy
  }

  private static AbstractCanvasObject createLine(Element elt) {
    int x0 = Integer.parseInt(elt.getAttribute("x1"));
    int y0 = Integer.parseInt(elt.getAttribute("y1"));
    int x1 = Integer.parseInt(elt.getAttribute("x2"));
    int y1 = Integer.parseInt(elt.getAttribute("y2"));
    return new Line(x0, y0, x1, y1);
  }

  private static AbstractCanvasObject createOval(Element elt) {
    double cx = Double.parseDouble(elt.getAttribute("cx"));
    double cy = Double.parseDouble(elt.getAttribute("cy"));
    double rx = Double.parseDouble(elt.getAttribute("rx"));
    double ry = Double.parseDouble(elt.getAttribute("ry"));
    int x = (int) Math.round(cx - rx);
    int y = (int) Math.round(cy - ry);
    int w = (int) Math.round(rx * 2);
    int h = (int) Math.round(ry * 2);
    return new Oval(x, y, w, h);
  }

  private static AbstractCanvasObject createPath(Element elt) {
    Matcher patt = PATH_REGEX.matcher(elt.getAttribute("d"));
    List<String> tokens = new ArrayList<>();
    int type = -1; // -1 error, 0 start, 1 curve, 2 polyline
    while (patt.find()) {
      String token = patt.group();
      tokens.add(token);
      if (Character.isLetter(token.charAt(0))) {
        switch (token.charAt(0)) {
          case 'M':
            if (type == -1) type = 0;
            else type = -1;
            break;
          case 'Q':
          case 'q':
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
    int x = Integer.parseInt(elt.getAttribute("x"));
    int y = Integer.parseInt(elt.getAttribute("y"));
    int w = Integer.parseInt(elt.getAttribute("width"));
    int h = Integer.parseInt(elt.getAttribute("height"));
    if (elt.hasAttribute("rx")) {
      AbstractCanvasObject ret = new RoundRectangle(x, y, w, h);
      int rx = Integer.parseInt(elt.getAttribute("rx"));
      ret.setValue(DrawAttr.CORNER_RADIUS, rx);
      return ret;
    } else {
      return new Rectangle(x, y, w, h);
    }
  }

  public static AbstractCanvasObject createShape(Element elt) {
    String name = elt.getTagName();
    AbstractCanvasObject ret = createShapeObject(elt, name);
    if (ret == null) {
      return null;
    }
    List<Attribute<?>> attrs = ret.getAttributes();
    if (attrs.contains(DrawAttr.PAINT_TYPE)) {
      String stroke = elt.getAttribute("stroke");
      String fill = elt.getAttribute("fill");
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
      Integer width = Integer.valueOf(elt.getAttribute("stroke-width"));
      ret.setValue(DrawAttr.STROKE_WIDTH, width);
    }
    if (attrs.contains(DrawAttr.STROKE_COLOR)) {
      String color = elt.getAttribute("stroke");
      String opacity = elt.getAttribute("stroke-opacity");
      if (!color.equals("none")) {
        ret.setValue(DrawAttr.STROKE_COLOR, getColor(color, opacity));
      }
    }
    if (attrs.contains(DrawAttr.FILL_COLOR)) {
      String color = elt.getAttribute("fill");
      if (color.equals("")) color = "#000000";
      String opacity = elt.getAttribute("fill-opacity");
      if (!color.equals("none")) {
        ret.setValue(DrawAttr.FILL_COLOR, getColor(color, opacity));
      }
    }
    return ret;
  }

  private static AbstractCanvasObject createShapeObject(Element elt, String name) {
    switch (name) {
      case "ellipse":
        return createOval(elt);
      case "line":
        return createLine(elt);
      case "path":
        return createPath(elt);
      case "polyline":
        return createPolyline(elt);
      case "polygon":
        return createPolygon(elt);
      case "rect":
        return createRectangle(elt);
      case "text":
        return createText(elt);
      default:
        return null;
    }
  }

  private static AbstractCanvasObject createText(Element elt) {
    int x = Integer.parseInt(elt.getAttribute("x"));
    int y = Integer.parseInt(elt.getAttribute("y"));
    String text = elt.getTextContent();
    Text ret = new Text(x, y, text);

    String fontFamily = elt.getAttribute("font-family");
    String fontStyle = elt.getAttribute("font-style");
    String fontWeight = elt.getAttribute("font-weight");
    String fontSize = elt.getAttribute("font-size");
    int styleFlags = Font.PLAIN;
    if (fontStyle.equals("italic")) styleFlags |= Font.ITALIC;
    if (fontWeight.equals("bold")) styleFlags |= Font.BOLD;
    int size = Integer.parseInt(fontSize);
    ret.setValue(DrawAttr.FONT, new Font(fontFamily, styleFlags, size));

    String halignStr = elt.getAttribute("text-anchor");
    AttributeOption halign;
    if (halignStr.equals("start")) {
      halign = DrawAttr.HALIGN_LEFT;
    } else if (halignStr.equals("end")) {
      halign = DrawAttr.HALIGN_RIGHT;
    } else {
      halign = DrawAttr.HALIGN_CENTER;
    }
    ret.setValue(DrawAttr.HALIGNMENT, halign);

    String valignStr = elt.getAttribute("dominant-baseline");
    AttributeOption valign = getAlignment(valignStr);
    ret.setValue(DrawAttr.VALIGNMENT, valign);

    // fill color is handled after we return
    return ret;
  }

  private static AttributeOption getAlignment(String valignStr) {
    switch (valignStr) {
      case "top":
        return DrawAttr.VALIGN_TOP;
      case "bottom":
        return DrawAttr.VALIGN_BOTTOM;
      case "alphabetic":
        return DrawAttr.VALIGN_BASELINE;
      default:
        return DrawAttr.VALIGN_MIDDLE;
    }
  }

  public static Font getFontAttribute(
      Element elt, String prefix, String defaultFamily, int defaultSize) {
    String fontFamily = elt.getAttribute(prefix + "font-family");
    if (fontFamily == null || fontFamily.length() == 0) fontFamily = defaultFamily;
    String fontStyle = elt.getAttribute(prefix + "font-style");
    if (fontStyle == null || fontStyle.length() == 0) fontStyle = "plain";
    String fontWeight = elt.getAttribute(prefix + "font-weight");
    if (fontWeight == null || fontWeight.length() == 0) fontWeight = "plain";
    String fontSize = elt.getAttribute(prefix + "font-size");
    int styleFlags = Font.PLAIN;
    if (fontStyle.equals("italic")) styleFlags |= Font.ITALIC;
    if (fontWeight.equals("bold")) styleFlags |= Font.BOLD;
    int size =
        (fontSize != null && fontSize.length() > 0 ? Integer.parseInt(fontSize) : defaultSize);
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
    Pattern patt = Pattern.compile("[ ,\n\r\t]+");
    String[] toks = patt.split(points);
    Location[] ret = new Location[toks.length / 2];
    for (int i = 0; i < ret.length; i++) {
      int x = Integer.parseInt(toks[2 * i]);
      int y = Integer.parseInt(toks[2 * i + 1]);
      ret[i] = Location.create(x, y);
    }
    return UnmodifiableList.create(ret);
  }
}
