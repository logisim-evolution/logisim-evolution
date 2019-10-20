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

package com.cburch.logisim.soc.util;

import java.util.HashSet;

public class AssemblerToken {
  public static final int LABEL = 1;
  public static final int INSTRUCTION = 2;
  public static final int ASM_INSTRUCTION = 3;
  public static final int BRACKETED_REGISTER = 4;
  public static final int REGISTER = 5;
  public static final int DEC_NUMBER = 6;
  public static final int HEX_NUMBER = 7;
  public static final int MAYBE_LABEL = 8;
  public static final int STRING = 9;
  public static final int SEPERATOR = 10;
  public static final int BRACKET_OPEN = 11;
  public static final int BRACKET_CLOSE = 12;
  public static final int LABEL_IDENTIFIER = 13;
  public static final int MATH_SUBTRACT = 14;
  public static final int MATH_ADD = 15;
  public static final int PARAMETER_LABEL = 16;
  public static final int MATH_MUL = 17;
  public static final int MATH_DIV = 18;
  public static final int MATH_REM = 19;
  public static final int MATH_SHIFT_LEFT = 20;
  public static final int MATH_SHIFT_RIGHT = 21;
  public static final int PROGRAM_COUNTER = 22;
  public static final int MACRO = 23;
  public static final int MACRO_PARAMETER = 24;
  /* all numbers below 256 are reserved for internal usage, the numbers starting from 256 can
   * be used for custom purposes.
   */
  
  public static final HashSet<Integer> MATH_OPERATORS = new HashSet<Integer>() {
    private static final long serialVersionUID = 1L;
    {add(MATH_ADD); add (MATH_SUBTRACT); add(MATH_MUL); add(MATH_DIV); add(MATH_REM); 
     add(MATH_SHIFT_LEFT); add(MATH_SHIFT_RIGHT);} 
  };
  
  private int type;
  private String value;
  private int offset;
  private Boolean valid;
  private Boolean isLabel;
   
  public AssemblerToken(int type , String value, int offset) {
    this.type = type;
    this.value = value;
    this.offset = offset;
    valid = true;
    if (type == HEX_NUMBER) {
      String[] split = value.toUpperCase().split("X");
      if (split.length != 2) {
        valid = false;
        return;
      }
      this.value = split[1];
    }
    if (type == STRING) {
      int start = 0;
      int end = value.length();
      if (value.startsWith("\"")) start = 1;
      if (value.length()>1 && value.charAt(value.length()-1) == '"' && value.charAt(value.length()-2) != '\\') end --;
      if (start >= end) this.value ="";
      else this.value = value.substring(start, end);
    }
    isLabel = type == LABEL;
  }
  
  public boolean isValid() { return valid; }
  public boolean isNumber() { return type == DEC_NUMBER || type == HEX_NUMBER; }
  public int getoffset() { return offset; }
  public int getType() { return type; }
  
  public void setType(int type) { 
    this.type = type; 
    if (type == LABEL || type == LABEL_IDENTIFIER || type == PARAMETER_LABEL)
      isLabel = true;
  }
  
  public String getValue() { return value; }
  public void setValue(int val) { value = Integer.toString(val); type = DEC_NUMBER; }
  public void setValue(String str) { value = str; }
  public boolean isLabel() { return isLabel; }
  
  public int getNumberValue() {
    if (type == DEC_NUMBER) return Integer.parseInt(value);
    else if (type == HEX_NUMBER) {
      if (value.toUpperCase().contains("X")) {
        String[] split = value.toUpperCase().split("X");
        if (split.length != 2) {
          valid = false;
          return 0;
        }
        value = split[1];
      }
      return Integer.parseUnsignedInt(value, 16);
    } else if (type == MACRO_PARAMETER) {
      if (value.length() > 0)
        return Integer.parseUnsignedInt(value.substring(1));
      else
    	return 0;
    }
    else return 0;
  }

  public long getLongValue() {
    if (type == DEC_NUMBER) return Long.parseLong(value);
    else if (type == HEX_NUMBER) {
      if (value.toUpperCase().contains("X")) {
        String[] split = value.toUpperCase().split("X");
        if (split.length != 2) {
          valid = false;
          return 0;
        }
        value = split[1];
      }
      return Long.parseUnsignedLong(value, 16);
    }
    else return 0;
  }
}
