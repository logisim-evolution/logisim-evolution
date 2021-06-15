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
  
  public static String getRemakrChar(boolean first,boolean last) {
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
  
  public static String GetZeroVector(int NrOfBits, boolean FloatingPinTiedToGround) {
    StringBuffer Contents = new StringBuffer();
    if (isVHDL()) {
      String FillValue = (FloatingPinTiedToGround) ? "0" : "1";
      String HexFillValue = (FloatingPinTiedToGround) ? "0" : "F";
      if (NrOfBits == 1) {
        Contents.append("'" + FillValue + "'");
      } else {
        if ((NrOfBits % 4) > 0) {
          Contents.append("\"");
          for (int i = 0; i < (NrOfBits % 4); i++) {
            Contents.append(FillValue);
          }
          Contents.append("\"");
          if (NrOfBits > 3) {
            Contents.append("&");
          }
        }
        if ((NrOfBits / 4) > 0) {
          Contents.append("X\"");
          for (int i = 0; i < (NrOfBits / 4); i++) {
            Contents.append(HexFillValue);
          }
          Contents.append("\"");
        }
      }
    } else {
      Contents.append(NrOfBits + "'d");
      if (FloatingPinTiedToGround) {
        Contents.append("0");
      } else {
        Contents.append("-1");
      }
    }
    return Contents.toString();
  }

}
