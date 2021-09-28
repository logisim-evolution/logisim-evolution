/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.plexers;

import java.util.ArrayList;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class BitSelectorHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String INPUT_BITS_STRING = "NrOfInputBits";
  private static final int INPUT_BITS_ID = -1;
  private static final String OUTPUTS_BITS_STRING = "NrOfOutputBits";
  private static final int OUTPUT_BITS_ID = -2;
  private static final String SELECT_BITS_STRING = "NrOfSelBits";
  private static final int SELECT_BITS_ID = -3;
  private static final String EXTENDED_BITS_STRING = "NrOfExtendedBits";
  private static final int EXTENDED_BITS_ID = -4;

  public BitSelectorHDLGeneratorFactory() {
    super();
    myParametersList
        .add(SELECT_BITS_STRING, SELECT_BITS_ID, HdlParameters.MAP_INT_ATTRIBUTE, BitSelector.SELECT_ATTR)
        .add(INPUT_BITS_STRING, INPUT_BITS_ID, HdlParameters.MAP_INT_ATTRIBUTE, BitSelector.EXTENDED_ATTR)
        .add(EXTENDED_BITS_STRING, EXTENDED_BITS_ID)
        .addBusOnly(BitSelector.GROUP_ATTR, OUTPUTS_BITS_STRING, OUTPUT_BITS_ID);
    myWires
        .addWire("s_extended_vector", EXTENDED_BITS_ID);
    myPorts
        .add(Port.INPUT, "DataIn", INPUT_BITS_ID, 1)
        .add(Port.INPUT, "Sel", SELECT_BITS_ID, 2)
        .add(Port.OUTPUT, "DataOut", OUTPUT_BITS_ID, 0, BitSelector.GROUP_ATTR);
  }

  @Override
  public ArrayList<String> getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getBuffer()
            .pair("extBits", EXTENDED_BITS_STRING)
            .pair("inBits", INPUT_BITS_STRING)
            .pair("outBits", OUTPUTS_BITS_STRING);
    final var outputBits = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    if (Hdl.isVhdl()) {
      contents
          .add("""
              s_extended_vector(({{extBits}}-1) DOWNTO {{inBits}}) <= (OTHERS => '0');
              s_extended_vector(({{inBits}}-1) DOWNTO 0) <= DataIn;
              """)
          .add(
              outputBits > 1
                  ? "DataOut <= s_extended_vector( ((to_integer(unsigned(Sel))+1) * {{outBits}})-1 DOWNTO to_integer(unsigned(Sel))*{{outBits}} );"
                  : "DataOut <= s_extended_vector( to_integer(unsigned(Sel)) );");
    } else {
      contents.add("""
          assign s_extended_vector[{{extBits}}-1:{{inBits}}] = 0;
          assign s_extended_vector[{{inBits}}-1:0] = DataIn;
          """);
      if (outputBits > 1) {
        contents.add("""
            wire[513:0] s_select_vector;
            reg[{{outBits}}-1:0] s_selected_slice;
            assign s_select_vector[513:{{extBits}}] = 0;
            assign s_select_vector[{{extBits}}-1:0] = s_extended_vector;
            assign DataOut = s_selected_slice;
            
            always @(*)
            begin
               case (Sel)
            """);
        for (var i = 15; i > 0; i--) {
          contents.add("{{1}}{{2}} : s_selected_slice <= s_select_vector[({{3}}*{{outBits}})-1:{{2}}*{{outBits}}];", LineBuffer.getIndent(2), i, (i + 1));
        }
        contents.add("""
                  default : s_selected_slice <= s_select_vector[{{outBits}}-1:0];
               endcase
            end
            """);
      } else {
        contents.add("assign DataOut = s_extended_vector[Sel];");
      }
    }
    return contents.getWithIndent();
  }
}
