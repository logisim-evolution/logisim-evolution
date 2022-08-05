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

public final class StringUtil {

  private StringUtil() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static StringGetter constantGetter(final String value) {
    return new StringGetter() {
      @Override
      public String toString() {
        return value;
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
    return (ret.length() > len) ? ret.substring(ret.length() - len) : ret;
  }

  public static Bounds estimateBounds(String text, Font font) {
    return estimateBounds(text, font, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);
  }

  public static Bounds estimateBounds(String text, Font font, int hAlign, int vAlign) {
    // TODO - you can imagine being more clever here
    if (text == null || text.length() == 0) text = "X"; // return Bounds.EMPTY_BOUNDS;
    var n = 0;
    var c = 0;
    var lines = 0;
    for (var i = 0; i < text.length(); i++) {
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

  /** Checks if given char sequence is either null or empty. */
  public static boolean isNullOrEmpty(CharSequence str) {
    return (str == null) ? true : str.isEmpty();
  }

  /** Checks if given char sequence is not null and not empty. */
  public static boolean isNotEmpty(CharSequence seq) {
    return (seq != null) ? !seq.isEmpty() : false;
  }

  /** Null safe version of `String.startsWith()` */
  public static boolean startsWith(String seq, String prefix) {
    return (seq != null) ? seq.startsWith(prefix) : false;
  }
}
