/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import java.awt.Font;

public final class FontUtil {

  private FontUtil() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static String toStyleDisplayString(int style) {
    return switch (style) {
      case Font.PLAIN -> S.get("fontPlainStyle");
      case Font.ITALIC -> S.get("fontItalicStyle");
      case Font.BOLD -> S.get("fontBoldStyle");
      case Font.BOLD | Font.ITALIC -> S.get("fontBoldItalicStyle");
      default -> "??";
    };
  }

  public static String toStyleStandardString(int style) {
    return switch (style) {
      case Font.PLAIN -> "plain";
      case Font.ITALIC -> "italic";
      case Font.BOLD -> "bold";
      case Font.BOLD | Font.ITALIC -> "bolditalic";
      default -> "??";
    };
  }
}
