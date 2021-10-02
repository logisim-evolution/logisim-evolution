/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.util.LineBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WithSelectHdlGenerator {

  private final Map<Long, Long> myCases;
  private final String regName;
  private final String sourceSignal;
  private final int nrOfSourceBits;
  private final String destinationSignal;
  private final int nrOfDestinationBits;
  private Long defaultValue = 0L;

  public WithSelectHdlGenerator(String componentName, String sourceSignal, int nrOfSourceBits,
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

  public WithSelectHdlGenerator add(Long selectValue, Long assignValue) {
    myCases.put(selectValue, assignValue);
    return this;
  }

  public WithSelectHdlGenerator add(Long selectValue, String binairyAssignValue) {
    myCases.put(selectValue, binairyStringToInt(binairyAssignValue));
    return this;
  }

  public WithSelectHdlGenerator add(String binairySelectValue, String binairyAssignValue) {
    myCases.put(binairyStringToInt(binairySelectValue), binairyStringToInt(binairyAssignValue));
    return this;
  }

  public WithSelectHdlGenerator add(String binairySelectValue, Long assignValue) {
    myCases.put(binairyStringToInt(binairySelectValue), assignValue);
    return this;
  }

  public WithSelectHdlGenerator setDefault(Long assignValue) {
    defaultValue = assignValue;
    return this;
  }

  public WithSelectHdlGenerator setDefault(String binairyAssignValue) {
    defaultValue = binairyStringToInt(binairyAssignValue);
    return this;
  }

  public List<String> getHdlCode() {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("sourceName", sourceSignal)
        .pair("destName", destinationSignal)
        .pair("regName", regName)
        .pair("regBits", nrOfDestinationBits - 1);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("{{with}} ({{sourceName}}) {{select}} {{destName}} <=");
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
      if (Hdl.isVhdl()) {
        contents.add("   {{1}} {{when}} {{2}},", Hdl.getConstantVector(value, nrOfDestinationBits), Hdl.getConstantVector(thisCase, nrOfSourceBits));
      } else {
        contents.add("      {{1}} : {{regName}} = {{2}};", Hdl.getConstantVector(thisCase, nrOfSourceBits), Hdl.getConstantVector(value, nrOfDestinationBits));
      }
    }
    if (Hdl.isVhdl()) {
      contents.add("   {{1}} {{when}} {{others}};", Hdl.getConstantVector(defaultValue, nrOfDestinationBits));
    } else {
      contents.add("      default : {{regName}} = {{1}};", Hdl.getConstantVector(defaultValue, nrOfDestinationBits));
    }
    if (Hdl.isVerilog())
      contents.add("""
             endcase
          end

          assign {{destName}} = {{regName}};
          """);
    return contents.get();
  }
}
