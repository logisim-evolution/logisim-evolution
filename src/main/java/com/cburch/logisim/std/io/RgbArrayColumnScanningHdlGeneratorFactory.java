/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

import java.util.HashMap;

public class RgbArrayColumnScanningHdlGeneratorFactory extends LedArrayColumnScanningHdlGeneratorFactory {

  public static final String HDL_IDENTIFIER =  "RGBArrayColumnScanning";

  public RgbArrayColumnScanningHdlGeneratorFactory() {
    super();
    myWires
        .addWire("s_maxRedLedInputs", MAX_NR_LEDS_ID)
        .addWire("s_maxBlueLedInputs", MAX_NR_LEDS_ID)
        .addWire("s_maxGreenLedInputs", MAX_NR_LEDS_ID);
    myPorts.removePorts(); // remove the ports from the super class
    myPorts
        .add(Port.INPUT, TickComponentHdlGeneratorFactory.FPGA_CLOCK, 1, 0)
        .add(Port.INPUT, LedArrayGenericHdlGeneratorFactory.LedArrayRedInputs, NR_OF_LEDS_ID, 1)
        .add(Port.INPUT, LedArrayGenericHdlGeneratorFactory.LedArrayGreenInputs, NR_OF_LEDS_ID, 2)
        .add(Port.INPUT, LedArrayGenericHdlGeneratorFactory.LedArrayBlueInputs, NR_OF_LEDS_ID, 3)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayColumnAddress, NR_OF_COLUMN_ADDRESS_BITS_ID, 4)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayRowRedOutputs, NR_OF_ROWS_ID, 5)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayRowGreenOutputs, NR_OF_ROWS_ID, 6)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayRowBlueOutputs, NR_OF_ROWS_ID, 7);
  }

  public static LineBuffer getPortMap(int id) {
    final var ports = new HashMap<String, String>();
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayColumnAddress, String.format("%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayColumnAddress, id));
    ports.put(TickComponentHdlGeneratorFactory.FPGA_CLOCK, TickComponentHdlGeneratorFactory.FPGA_CLOCK);
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayRowRedOutputs, String.format("%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayRowRedOutputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayRowGreenOutputs, String.format("%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayRowGreenOutputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayRowBlueOutputs, String.format("%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayRowBlueOutputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayRedInputs, String.format("s_%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayRedInputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayGreenInputs, String.format("s_%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayGreenInputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayBlueInputs, String.format("s_%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayBlueInputs, id));
    return LedArrayGenericHdlGeneratorFactory.getGenericPortMapAlligned(ports, false);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("nrOfLeds", NR_OF_LEDS_STRING)
        .pair("nrOfRows", NR_OF_ROWS_STRING)
        .pair("activeLow", ACTIVE_LOW_STRING)
        .pair("insR", LedArrayGenericHdlGeneratorFactory.LedArrayRedInputs)
        .pair("insG", LedArrayGenericHdlGeneratorFactory.LedArrayGreenInputs)
        .pair("insB", LedArrayGenericHdlGeneratorFactory.LedArrayBlueInputs)
        .pair("outsR", LedArrayGenericHdlGeneratorFactory.LedArrayRowRedOutputs)
        .pair("outsG", LedArrayGenericHdlGeneratorFactory.LedArrayRowGreenOutputs)
        .pair("outsB", LedArrayGenericHdlGeneratorFactory.LedArrayRowBlueOutputs);

    contents.add(getColumnCounterCode());
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          makeVirtualInputs : {{process}} ( internalRedLeds, internalGreenLeds, internalBlueLeds ) {{is}}
          {{begin}}
             s_maxRedLedInputs <= ({{others}} => '0');
             s_maxGreenLedInputs <= ({{others}} => '0');
             s_maxBlueLedInputs <= ({{others}} => '0');
             {{if}} ({{activeLow}} = 1) {{then}}
                s_maxRedLedInputs({{nrOfLeds}}-1 {{downto}} 0)   <= {{not}} {{insR}};
                s_maxGreenLedInputs({{nrOfLeds}}-1 {{downto}} 0) <= {{not}} {{insG}};
                s_maxBlueLedInputs({{nrOfLeds}}-1 {{downto}} 0)  <= {{not}} {{insB}};
             {{else}}
                s_maxRedLedInputs({{nrOfLeds}}-1 {{downto}} 0)   <= {{insR}};
                s_maxGreenLedInputs({{nrOfLeds}}-1 {{downto}} 0) <= {{insG}};
                s_maxBlueLedInputs({{nrOfLeds}}-1 {{downto}} 0)  <= {{insB}};
             {{end}} {{if}};
          {{end}} {{process}} makeVirtualInputs;

          genOutputs : {{for}} n {{in}} {{nrOfRows}}-1 {{downto}} 0 {{generate}}
             {{outsR}}(n) <= s_maxRedLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);
             {{outsG}}(n) <= s_maxGreenLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);
             {{outsB}}(n) <= s_maxBlueLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);
          {{end}} {{generate}} genOutputs;
          """).empty();
    } else {
      contents.add("""
          genvar i;
          generate
             for (i = 0; i < {{nrOfRows}}; i = i + 1)
             begin:outputs
                assign {{outsR}}[i] = (activeLow == 1)
                    ? ~{{insR }}[i*nrOfColumns+s_columnCounterReg]
                    :  {{insR }}[i*nrOfColumns+s_columnCounterReg];
                assign {{outsG}}[i] = (activeLow == 1)
                    ? ~{{insG }}[i*nrOfColumns+s_columnCounterReg]
                    :  {{insG }}[i*nrOfColumns+s_columnCounterReg];
                assign {{outsB}}[i] = (activeLow == 1)
                    ? ~{{insB }}[i*nrOfColumns+s_columnCounterReg]
                    :  {{insB }}[i*nrOfColumns+s_columnCounterReg];
             end
          endgenerate
          """).empty();
    }
    return contents;
  }
}
