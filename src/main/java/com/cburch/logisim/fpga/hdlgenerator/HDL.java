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
import com.cburch.logisim.util.LineBuffer;

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
    return isVHDL() ? LineBuffer.format("IF ({{1}}) THEN", condition)
                    : LineBuffer.format("if ({{1}}) begin", condition);
  }

  // TODO find good name
  /* 
  public static String else() {
    return isVHDL() ? "ELSE " : "end else begin";
  }
  */

  public static String elseIf(String condition) {
    return isVHDL() ? LineBuffer.format("ELSIF ({{1}}) THEN", condition)
                    : LineBuffer.format("end else if ({{1}}) begin", condition);
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
                ? LineBuffer.format("{{1}}signed({{2}})", signed ? "" : "un", signal)
                : (signed ? "$signed(" + signal + ")" : signal);
  }

  public static String greaterOperator(String signalOne, String signalTwo, boolean signed, boolean equal) {
    return LineBuffer.format("{{1}} >{{2}} {{3}}", typecast(signalOne, signed), equal ? "=" : "", typecast(signalTwo, signed));
  }

  public static String lessOperator(String signalOne, String signalTwo, boolean signed, boolean equal) {
    return LineBuffer.format("{{1}} <{{2}} {{3}}", typecast(signalOne, signed), equal ? "=" : "", typecast(signalTwo, signed));
  }

  public static String leqOperator(String signalOne, String signalTwo, boolean signed) {
    return lessOperator(signalOne, signalTwo, signed, true);
  }
  
  public static String geqOperator(String signalOne, String signalTwo, boolean signed) {
    return greaterOperator(signalOne, signalTwo, signed, true);
  }

  public static String risingEdge(String signal) {
    return isVHDL() ? "rising_edge(" + signal + ")"
                    : "posedge " + signal;
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

  public static String addOperator(String signalOne, String signalTwo, boolean signed) {
    return (isVHDL() ? "std_logic_vector(" : "")
            + typecast(signalOne, signed)
            + " + "
            + typecast(signalTwo, signed)
            + (isVHDL() ? ")" : "");
  }
  
  public static String subOperator(String signalOne, String signalTwo, boolean signed) {
    return (isVHDL() ? "std_logic_vector(" : "")
            + typecast(signalOne, signed)
            + " - "
            + typecast(signalTwo, signed)
            + (isVHDL() ? ")" : "");
  }

  public static String shiftlOperator(String signal, int with, int nrOfBits, boolean arithmetic) {
    if (nrOfBits == 0)
      return signal;
    return isVHDL() ? LineBuffer.format("{{1}}{{2}} & {{4}}{{3}}{{4}}", signal, splitVector(with - 1 - nrOfBits, 0), "0".repeat(nrOfBits), nrOfBits == 1 ? "'" : "\"")
                    : LineBuffer.format("{{{1}}{{2}},{{{3}}{1'b0}}}", signal, splitVector(with - 1 - nrOfBits, 0), nrOfBits);
  }
  
  public static String shiftrOperator(String signal, int with, int nrOfBits, boolean arithmetic) {
    if (nrOfBits == 0)
      return signal;
    if (arithmetic) {
      return isVHDL()
        ? LineBuffer.format("({{1}} DOWNTO 0 => {{2}}({{1}})) & {{2}}{{3}}", with - 1, signal, splitVector(with - 1, with - nrOfBits))
        : LineBuffer.format("{{{{1}}{{{2}}[{{1}}-1]}},{{2}}{{3}}}", with, signal, splitVector(with - 1, with - nrOfBits));
    } else {
      return isVHDL()
        ? LineBuffer.format("{{1}}{{2}}{{1}} & {{3}}{{4}", nrOfBits == 1 ? "'" : "\"", "0".repeat(nrOfBits), signal, splitVector(with - 1, with - nrOfBits))
        : LineBuffer.format("{{{{1}}{1'b0}},{{2}}{{3}}}", with, signal, splitVector(with - 1, with - nrOfBits));
    }
  }

  public static String sllOperator(String signal, int with, int nrOfBits) {
    return shiftlOperator(signal, with, nrOfBits, false);
  }

  public static String slaOperator(String signal, int with, int nrOfBits) {
    return shiftlOperator(signal, with, nrOfBits, true);
  }

  public static String srlOperator(String signal, int with, int nrOfBits) {
    return shiftrOperator(signal, with, nrOfBits, false);
  }
  
  public static String sraOperator(String signal, int with, int nrOfBits) {
    return shiftrOperator(signal, with, nrOfBits, true);
  }

  public static String rolOperator(String signal, int with, int nrOfBits) {
    return isVHDL() ? LineBuffer.format("{{1}}{{2}} & {{1}}{{3}}", signal, splitVector(with - 1 - nrOfBits, 0), splitVector(with - 1, with - nrOfBits))
                    : LineBuffer.format("{{{1}}{{2}},{{1}}{{3}}}", signal, splitVector(with - 1 - nrOfBits, 0), splitVector(with - 1, with - nrOfBits));
  }
  
  public static String rorOperator(String signal, int with, int nrOfBits) {
    return isVHDL() ? LineBuffer.format("{{1}}{{2}} & {{1}}{{3}}", signal, splitVector(nrOfBits, 0), splitVector(with - 1, nrOfBits))
                    : LineBuffer.format("{{{1}}{{2}},{{1}}{{3}}}", signal, splitVector(nrOfBits, 0), splitVector(with - 1, nrOfBits));
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
    if (start == end)
      return LineBuffer.format("{{1}}{{2}}{{3}} ", BracketOpen(), start, BracketClose());
    return isVHDL()
                ? LineBuffer.format("({{1}} DOWNTO {{2}}) ", start, end)
                : LineBuffer.format("[{{1}}:{{2}}] ", start, end);
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
