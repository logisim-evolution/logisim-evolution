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

  private final Map<Long, Long> myCases;
  private final String regName;
  private final String sourceSignal;
  private final int nrOfSourceBits;
  private final String destinationSignal;
  private final int nrOfDestinationBits;
  private Long defaultValue = 0L;

  public WithSelectHDLGenerator(String componentName, String sourceSignal, int nrOfSourceBits,
      String destinationSignal, int nrOfDestinationBits) {
    myCases = new HashMap<>();
    regName = LineBuffer.format("s_{{1}}_reg", componentName);
    this.sourceSignal = sourceSignal;
    this.nrOfSourceBits = nrOfSourceBits;
    this.destinationSignal = destinationSignal;
    this.nrOfDestinationBits = nrOfDestinationBits;
  }

  private Long binairyStringToInt(String binairyValue) {
    var result = 0L;
    for (var charIndex = 0; charIndex < binairyValue.length(); charIndex++) {
      final var character = binairyValue.charAt(charIndex) - '0';
      if ((character < 0) || (character > 1)) throw new NumberFormatException("Invalid binairy value in WithSelectHDLGenerator");
      result *= 2;
      result += character;
    }
    return result;
  }

  public WithSelectHDLGenerator add(Long selectValue, Long assignValue) {
    myCases.put(selectValue, assignValue);
    return this;
  }

  public WithSelectHDLGenerator add(Long selectValue, String binairyAssignValue) {
    myCases.put(selectValue, binairyStringToInt(binairyAssignValue));
    return this;
  }

  public WithSelectHDLGenerator add(String binairySelectValue, String binairyAssignValue) {
    myCases.put(binairyStringToInt(binairySelectValue), binairyStringToInt(binairyAssignValue));
    return this;
  }

  public WithSelectHDLGenerator add(String binairySelectValue, Long assignValue) {
    myCases.put(binairyStringToInt(binairySelectValue), assignValue);
    return this;
  }

  public WithSelectHDLGenerator setDefault(Long assignValue) {
    defaultValue = assignValue;
    return this;
  }

  public WithSelectHDLGenerator setDefault(String binairyAssignValue) {
    defaultValue = binairyStringToInt(binairyAssignValue);
    return this;
  }

  public ArrayList<String> getHdlCode() {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("sourceName", sourceSignal)
        .pair("destName", destinationSignal)
        .pair("regName", regName)
        .pair("regBits", nrOfDestinationBits - 1);
    contents.add("");
    if (HDL.isVhdl()) {
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
      final var value = myCases.get(thisCase);
      if (HDL.isVhdl()) {
        contents.add("   {{1}} WHEN {{2}},", HDL.getConstantVector(value, nrOfDestinationBits), HDL.getConstantVector(thisCase, nrOfSourceBits));
      } else {
        contents.add("      {{1}} : {{regName}} = {{2}};", HDL.getConstantVector(thisCase, nrOfSourceBits), HDL.getConstantVector(value, nrOfDestinationBits));
      }
    }
    if (HDL.isVhdl()) {
      contents.add("   {{1}} WHEN OTHERS;", HDL.getConstantVector(defaultValue, nrOfDestinationBits));
    } else {
      contents.add("      default : {{regName}} = {{1}};", HDL.getConstantVector(defaultValue, nrOfDestinationBits));
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
