/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
