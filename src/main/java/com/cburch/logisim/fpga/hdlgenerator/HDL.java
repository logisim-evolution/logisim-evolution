/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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

  public static String startIf(String condition) {
    return isVHDL() ? "IF (" + condition + ") THEN" : "if (" + condition + ") begin";
  }

  // TODO find good name
  /* 
  public static String else() {
    return isVHDL() ? "ELSE " : "end else begin";
  }
  */

  public static String elseIf(String condition) {
    return isVHDL() ? "ELSIF (" + condition + ") THEN"
                    : "end else if (" + condition + ") begin";
  }

  public static String endIf() {
    return isVHDL() ? "END IF;" : "end";
  }

  public static String assignPreamble() {
    return isVHDL() ? "" : "assign ";
  }

  public static String assignOperator() {
    return isVHDL() ? " <= " : " = ";
  }

  public static String equalOperator() {
    return isVHDL() ? " = " : "==";
  }

  public static String notEqualOperator() {
    return isVHDL() ? " \\= " : "!=";
  }

  private static String typecast(String signal, boolean signed) {
    return isVHDL()
                ? (signed ? "SIGNED" : "UNSIGNED") + "(" + signal + ")"
                : (signed ? "$signed(" + signal + ")" : signal);
  }

  public static String greaterOperator(String signalOne, String signalTwo, boolean signed, boolean equal) {
    return (isVHDL() ? "STD_LOGIC_VECTOR(" : "")
            + typecast(signalOne, signed)
            + (equal ? ">=" : ">")
            + typecast(signalTwo, signed)
            + (isVHDL()? ")" : "");
  }

  public static String lessOperator(String signalOne, String signalTwo, boolean signed, boolean equal) {
    return (isVHDL() ? "STD_LOGIC_VECTOR(" : "")
            + typecast(signalOne, signed)
            + (equal ? "<=" : "<")
            + typecast(signalTwo, signed)
            + (isVHDL()? ")" : "");
  }

  public static String leqOperator(String signalOne, String signalTwo, boolean signed) {
    return lessOperator(signalOne, signalTwo, signed, true);
  }
  
  public static String geqOperator(String signalOne, String signalTwo, boolean signed) {
    return greaterOperator(signalOne, signalTwo, signed, true);
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

  public static String modOperator(String signalOne, String signalTwo, boolean signed) {
    return (isVHDL() ? "STD_LOGIC_VECTOR(" : "")
            + typecast(signalOne, signed)
            + (isVHDL() ? " REM " : "%")
            + typecast(signalTwo, signed)
            + (isVHDL()? ")" : "");
  }

  public static String shiftlOperator(String signal, String nrOfBits, boolean arithmetic) {
    return isVHDL() ? "STD_LOGIC_VECTOR(SHIFT_LEFT(" + typecast(signal, arithmetic) + ", " + nrOfBits + "))"
                    : "(" + signal + (arithmetic ? " <<< " : " << ") + nrOfBits + ")";
  }
  
  public static String shiftrOperator(String signal, String nrOfBits, boolean arithmetic) {
    return isVHDL() ? "STD_LOGIC_VECTOR(SHIFT_RIGHT(" + typecast(signal, arithmetic) + ", " + nrOfBits + "))"
                    : "(" + signal + (arithmetic ? " >>> " : " >> ") + nrOfBits + ")";
  }

  public static String sllOperator(String signal, String nrOfBits) {
    return shiftlOperator(signal, nrOfBits, false);
  }

  public static String slaOperator(String signal, String nrOfBits) {
    return shiftlOperator(signal, nrOfBits, true);
  }

  public static String srlOperator(String signal, String nrOfBits) {
    return shiftlOperator(signal, nrOfBits, false);
  }
  
  public static String sraOperator(String signal, String nrOfBits) {
    return shiftlOperator(signal, nrOfBits, true);
  }

  public static String rolOperator(String signal, String nrOfBits) {
    return isVHDL() ? "ROTATE_LEFT(" + signal + ", " + nrOfBits + ")"
                    : "((" + signal + " << " + nrOfBits + ") | (" + signal + ">> -" + nrOfBits + "))";
  }
  
  public static String rorOperator(String signal, String nrOfBits) {
    return isVHDL() ? "ROTATE_RIGHT(" + signal + ", " + nrOfBits + ")"
                    : "((" + signal + " >> " + nrOfBits + ") | (" + signal + "<< -" + nrOfBits + "))";
  }

  public static String risingEdge(String signal) {
    return isVHDL() ? "RISING_EDGE(" + signal + ")"
                    : "posedge " + signal;
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

  public static String splitVector(int start, int end) {
    return isVHDL()
                ? "(" + start + " DOWNTO " + end + ")"
                : "[" + start + ":" + end + "]";
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
