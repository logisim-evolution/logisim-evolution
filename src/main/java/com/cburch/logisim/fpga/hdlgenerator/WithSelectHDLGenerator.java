/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.fpga.hdlgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.cburch.logisim.util.LineBuffer;

public class WithSelectHDLGenerator {
  
  private final Map<Integer, Integer> myCases;
  private final String regName;
  private final String sourceSignal;
  private final int nrOfSourceBits;
  private final String destinationSignal;
  private final int nrOfDestinationBits;
  
  public static final int OTHERS_INDEX = -1;
  
  public WithSelectHDLGenerator(String componentName, String sourceSignal, int nrOfSourceBits, 
      String destinationSignal, int nrOfDestinationBits) {
    myCases = new HashMap<>();
    regName = LineBuffer.format("s_{{1}}_reg", componentName);
    this.sourceSignal = sourceSignal;
    this.nrOfSourceBits = nrOfSourceBits;
    this.destinationSignal = destinationSignal;
    this.nrOfDestinationBits = nrOfDestinationBits;
  }
  
  private int binairyStringToInt(String binairyValue) {
    var result = 0;
    for (var charIndex = 0; charIndex < binairyValue.length(); charIndex++) {
      final var character = binairyValue.charAt(charIndex) - '0';
      if ((character < 0) || (character > 1)) throw new NumberFormatException("Invalid binairy value in WithSelectHDLGenerator");
      result *= 2;
      result += character;
    }
    return result;
  }
  
  public WithSelectHDLGenerator add(int selectValue, int assignValue) {
    myCases.put(selectValue, assignValue);
    return this;
  }
  
  public WithSelectHDLGenerator add(int selectValue, String binairyAssignValue) {
    myCases.put(selectValue, binairyStringToInt(binairyAssignValue));
    return this;
  }
  
  public WithSelectHDLGenerator add(String binairySelectValue, String binairyAssignValue) {
    myCases.put(binairyStringToInt(binairySelectValue), binairyStringToInt(binairyAssignValue));
    return this;
  }
  
  public WithSelectHDLGenerator add(String binairySelectValue, int assignValue) {
    myCases.put(binairyStringToInt(binairySelectValue), assignValue);
    return this;
  }
  
  public ArrayList<String> getHdlCode() {
    final var contents = (new LineBuffer()).withHdlPairs()
        .pair("sourceName", sourceSignal)
        .pair("destName", destinationSignal)
        .pair("regName", regName)
        .pair("regBits", nrOfDestinationBits - 1);
    if (HDL.isVHDL()) {
      contents.add("WITH ({{sourceName}}) SELECT {{destName}} <=");
    } else {
      contents.add("""
        reg[{{regBits}}:0] {{regName}};
          always @(*)
          begin
             case ({{sourceName}})
          """);
    }
    for (final var thisCase : myCases.keySet()) {
      if (thisCase < 0) continue;
      final var value = myCases.get(thisCase);
      if (HDL.isVHDL()) {
        contents.add("   {{1}} WHEN {{2}},", HDL.getConstantBitVector(value, nrOfDestinationBits), HDL.getConstantBitVector(thisCase, nrOfSourceBits));
      } else {
        contents.add("      {{1}} : {{regName}} = {{2}};", HDL.getConstantBitVector(thisCase, nrOfSourceBits), HDL.getConstantBitVector(value, nrOfDestinationBits));
      }
    }
    if (myCases.containsKey(OTHERS_INDEX)) {
      final var value = myCases.get(OTHERS_INDEX);
      if (HDL.isVHDL()) {
        contents.add("   {{1}} WHEN OTHERS;", HDL.getConstantBitVector(value, nrOfDestinationBits));
      } else {
        contents.add("      default : {{regName}} = {{1}};", HDL.getConstantBitVector(value, nrOfDestinationBits));
      }
    }
    if (HDL.isVerilog()) 
      contents.add("""
             endcase
          end
          
          assign {{destName}} = {{regName}};
          """);
    return contents.get();
  }
}
