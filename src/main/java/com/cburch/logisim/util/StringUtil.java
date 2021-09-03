/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.logisim.data.Bounds;
import java.awt.Font;
import java.awt.FontMetrics;

public class StringUtil {
  public static StringGetter constantGetter(final String value) {
    return new StringGetter() {
      public String toString() {
        return value;
      }
    };
  }

  public static String format(String fmt, String... args) {
    return String.format(fmt, (Object[]) args);
  }

  public static StringGetter formatter(final StringGetter base, final String arg) {
    return new StringGetter() {
      public String toString() {
        return format(base.toString(), arg);
      }
    };
  }

  public static StringGetter formatter(final StringGetter base, final StringGetter arg) {
    return new StringGetter() {
      public String toString() {
        return format(base.toString(), arg.toString());
      }
    };
  }

  public static String resizeString(String value, FontMetrics metrics, int maxWidth) {
    final var width = metrics.stringWidth(value);

    if (width < maxWidth) return value;
    if (value.length() < 4) return value;
    return resizeString(
        new StringBuilder(value.substring(0, value.length() - 3) + ".."), metrics, maxWidth);
  }

  private static String resizeString(StringBuilder value, FontMetrics metrics, int maxWidth) {
    final var width = metrics.stringWidth(value.toString());

    if (width < maxWidth) return value.toString();
    if (value.length() < 4) return value.toString();
    return resizeString(value.delete(value.length() - 3, value.length() - 2), metrics, maxWidth);
  }

  public static String toHexString(int bits, long value) {
    if (bits < 64) value &= (1L << bits) - 1;
    final var len = (bits + 3) / 4;
    final var ret = String.format("%0" + len + "x", value);
    if (ret.length() > len) return ret.substring(ret.length() - len);
    return ret;
  }

  public static Bounds estimateBounds(String text, Font font) {
    return estimateBounds(text, font, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);
  }

  public static Bounds estimateBounds(String text, Font font, int hAlign, int vAlign) {
    // TODO - you can imagine being more clever here
    if (text == null || text.length() == 0) text = "X"; // return Bounds.EMPTY_BOUNDS;
    int n = 0;
    int c = 0;
    int lines = 0;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        n = (Math.max(c, n));
        c = 0;
        lines++;
      } else if (text.charAt(i) == '\t') {
        c += 4;
      } else {
        c++;
      }
    }
    if (text.charAt(text.length() - 1) != '\n') {
      n = (Math.max(c, n));
      lines++;
    }
    final var size = font.getSize();
    final var h = size * lines;
    final var w = size * n * 2 / 3; // assume approx monospace 12x8 aspect ratio
    int x;
    int y;
    if (hAlign == GraphicsUtil.H_LEFT) {
      x = 0;
    } else if (hAlign == GraphicsUtil.H_RIGHT) {
      x = -w;
    } else {
      x = -w / 2;
    }
    if (vAlign == GraphicsUtil.V_TOP) {
      y = 0;
    } else if (vAlign == GraphicsUtil.V_CENTER) {
      y = -h / 2;
    } else {
      y = -h;
    }
    return Bounds.create(x, y, w, h);
  }

}
