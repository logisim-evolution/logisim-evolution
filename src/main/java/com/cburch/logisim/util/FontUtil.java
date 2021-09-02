/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import java.awt.Font;

public class FontUtil {
  public static String toStyleDisplayString(int style) {
    switch (style) {
      case Font.PLAIN:
        return S.get("fontPlainStyle");
      case Font.ITALIC:
        return S.get("fontItalicStyle");
      case Font.BOLD:
        return S.get("fontBoldStyle");
      case Font.BOLD | Font.ITALIC:
        return S.get("fontBoldItalicStyle");
      default:
        return "??";
    }
  }

  public static String toStyleStandardString(int style) {
    switch (style) {
      case Font.PLAIN:
        return "plain";
      case Font.ITALIC:
        return "italic";
      case Font.BOLD:
        return "bold";
      case Font.BOLD | Font.ITALIC:
        return "bolditalic";
      default:
        return "??";
    }
  }
}
