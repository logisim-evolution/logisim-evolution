/*
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

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.prefs.AppPreferences;

public abstract class HDL {

  public static boolean isVHDL() {
    return AppPreferences.HDL_Type.get().equals(HDLGeneratorFactory.VHDL);
  }

  public static boolean isVerilog() {
    return AppPreferences.HDL_Type.get().equals(HDLGeneratorFactory.VERILOG);
  }

  public static String BracketOpen() {
    return isVHDL() ? "(" : "[";
  }

  public static String BracketClose() {
    return isVHDL() ? ")" : "]";
  }

  public static int remarkOverhead() {
    return isVHDL() ? 3 : 4;
  }

  public static String getRemakrChar(boolean first, boolean last) {
    if (isVHDL()) return "-";
    if (first) return "/";
    if (last) return " ";
    return "*";
  }

  public static String getRemarkStart() {
    if (isVHDL()) return "-- ";
    return " ** ";
  }

  public static String assignPreamble() {
    return isVHDL() ? "" : "assign ";
  }

  public static String assignOperator() {
    return isVHDL() ? " <= " : " = ";
  }

  public static String notOperator() {
    return isVHDL() ? " NOT " : "~";
  }

  public static String andOperator() {
    return isVHDL() ? " AND " : "&";
  }

  public static String orOperator() {
    return isVHDL() ? " OR " : "|";
  }

  public static String xorOperator() {
    return isVHDL() ? " XOR " : "^";
  }

  public static String zeroBit() {
    return isVHDL() ? "'0'" : "1'b0";
  }

  public static String oneBit() {
    return isVHDL() ? "'1'" : "1'b1";
  }

  public static String unconnected(boolean empty) {
    return isVHDL() ? "OPEN" : empty ? "" : "'bz";
  }

  public static String vectorLoopId() {
    return isVHDL() ? " DOWNTO " : ":";
  }

  public static String GetZeroVector(int nrOfBits, boolean floatingPinTiedToGround) {
    var contents = new StringBuilder();
    if (isVHDL()) {
      var fillValue = (floatingPinTiedToGround) ? "0" : "1";
      var hexFillValue = (floatingPinTiedToGround) ? "0" : "F";
      if (nrOfBits == 1) {
        contents.append("'").append(fillValue).append("'");
      } else {
        if ((nrOfBits % 4) > 0) {
          contents.append("\"");
          contents.append(fillValue.repeat((nrOfBits % 4)));
          contents.append("\"");
          if (nrOfBits > 3) {
            contents.append("&");
          }
        }
        if ((nrOfBits / 4) > 0) {
          contents.append("X\"");
          contents.append(hexFillValue.repeat(Math.max(0, (nrOfBits / 4))));
          contents.append("\"");
        }
      }
    } else {
      contents.append(nrOfBits).append("'d");
      contents.append(floatingPinTiedToGround ? "0" : "-1");
    }
    return contents.toString();
  }

}
