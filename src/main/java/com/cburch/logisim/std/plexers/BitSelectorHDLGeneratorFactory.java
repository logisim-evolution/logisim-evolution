/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.plexers;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class BitSelectorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String INPUT_BITS_STR = "NrOfInputBits";
  private static final int InputBitsId = -1;
  private static final String OUTPUTS_BITS_STR = "NrOfOutputBits";
  private static final int OutputsBitsId = -2;
  private static final String SelectBitsStr = "NrOfSelBits";
  private static final int SelectBitsId = -3;
  private static final String EXTENDED_BITS_STR = "NrOfExtendedBits";
  private static final int ExtendedBitsId = -4;

  @Override
  public String getComponentStringIdentifier() {
    return "BITSELECTOR";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist theNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("DataIn", InputBitsId);
    map.put("Sel", SelectBitsId);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        (new LineBuffer())
            .pair("extBits", EXTENDED_BITS_STR)
            .pair("inBits", INPUT_BITS_STR)
            .pair("outBits", OUTPUTS_BITS_STR);
    final var outputBits = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    if (HDL.isVHDL()) {
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

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist theNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    int outputBits = (attrs.getValue(BitSelector.GROUP_ATTR).getWidth() == 1) ? 1 : OutputsBitsId;
    map.put("DataOut", outputBits);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    int outputBits = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    map.put(InputBitsId, INPUT_BITS_STR);
    if (outputBits > 1) map.put(OutputsBitsId, OUTPUTS_BITS_STR);
    map.put(SelectBitsId, SelectBitsStr);
    map.put(ExtendedBitsId, EXTENDED_BITS_STR);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    final var map = new TreeMap<String, Integer>();
    int selBits = componentInfo.getComponent().getEnd(2).getWidth().getWidth();
    int inputBits = componentInfo.getComponent().getEnd(1).getWidth().getWidth();
    int outputBits = componentInfo.getComponent().getEnd(0).getWidth().getWidth();
    map.put(INPUT_BITS_STR, inputBits);
    map.put(SelectBitsStr, selBits);
    if (outputBits > 1) map.put(OUTPUTS_BITS_STR, outputBits);
    var nrOfSlices = 1;
    for (var i = 0; i < selBits; i++) {
      nrOfSlices <<= 1;
    }
    map.put(EXTENDED_BITS_STR, nrOfSlices * outputBits + 1);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("DataIn", true, comp, 1, nets));
    map.putAll(GetNetMap("Sel", true, comp, 2, nets));
    map.putAll(GetNetMap("DataOut", true, comp, 0, nets));
    return map;
  }

  @Override
  public String GetSubDir() {
    return "plexers";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_extended_vector", ExtendedBitsId);
    return map;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
