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
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.SortedMap;
import java.util.TreeMap;

public class PriorityEncoderHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_SELECT_BITS_STRING = "nrOfSelectBits";
  private static final int NR_OF_SELECT_BITS_ID = -1;
  private static final String NR_OF_INPUT_BITS_STRING = "nrOfInputBits";
  private static final int NR_OF_INPUT_BITS_ID = -2;

  public PriorityEncoderHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_INPUT_BITS_STRING, NR_OF_INPUT_BITS_ID, HdlParameters.MAP_POW2, PlexersLibrary.ATTR_SELECT)
        .add(NR_OF_SELECT_BITS_STRING, NR_OF_SELECT_BITS_ID, HdlParameters.MAP_INT_ATTRIBUTE, PlexersLibrary.ATTR_SELECT);
    myWires
        .addWire("s_inIsZero", 1)
        .addWire("s_address", 6)
        .addWire("s_selectVector0", 65)
        .addWire("s_selectVector1", 32)
        .addWire("s_selectVector2", 16)
        .addWire("s_selectVector3", 8)
        .addWire("s_selectVector4", 4);
    myPorts
        .add(Port.INPUT, "enable", 1, 0)
        .add(Port.INPUT, "inputVector", NR_OF_INPUT_BITS_ID, 0)
        .add(Port.OUTPUT, "groupSelect", 1, 0)
        .add(Port.OUTPUT, "enableOut", 1, 0)
        .add(Port.OUTPUT, "address", NR_OF_SELECT_BITS_ID, 0);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof netlistComponent)) return map;
    final var comp = (netlistComponent) mapInfo;
    final var nrOfBits = comp.nrOfEnds() - 4;
    map.putAll(Hdl.getNetMap("enable", false, comp, nrOfBits + PriorityEncoder.EN_IN, nets));
    final var vectorList = new StringBuilder();
    for (var i = nrOfBits - 1; i >= 0; i--) {
      if (Hdl.isVhdl())
        map.putAll(Hdl.getNetMap("inputVector(" + i + ")", true, comp, i, nets));
      else {
        if (vectorList.length() > 0) vectorList.append(",");
        vectorList.append(Hdl.getNetName(comp, i, true, nets));
      }
    }
    if (Hdl.isVerilog()) map.put("inputVector", vectorList.toString());
    map.putAll(Hdl.getNetMap("groupSelect", true, comp, nrOfBits + PriorityEncoder.GS, nets));
    map.putAll(Hdl.getNetMap("enableOut", true, comp, nrOfBits + PriorityEncoder.EN_OUT, nets));
    map.putAll(Hdl.getNetMap("address", true, comp, nrOfBits + PriorityEncoder.OUT, nets));
    return map;
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer()
            .pair("selBits", NR_OF_SELECT_BITS_STRING)
            .pair("inBits", NR_OF_INPUT_BITS_STRING);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          -- Output Signals
          groupSelect <= {{not}}(s_inIsZero) {{and}} enable;
          enableOut   <= s_inIsZero {{and}} enable;
          address     <= ({{others}} => '0') {{when}} enable = '0' {{else}}
                         s_address({{selBits}}-1 {{downto}} 0);

          -- Control Signals
          s_inIsZero  <= '1' {{when}} inputVector = std_logic_vector(to_unsigned(0,{{inBits}})) {{else}} '0';

          -- Processes
          makeAddr : {{process}}(inputVector, s_selectVector1, s_selectVector2, s_selectVector3, s_selectVector4) {{is}}
          {{begin}}
             s_selectVector0(64 {{downto}} {{inBits}})  <= ({{others}} => '0');
             s_selectVector0({{inBits}}-1 {{downto}} 0) <= inputVector;
             {{if}} (s_selectVector0(63 {{downto}} 32) = X"00000000") {{then}} s_address(5)      <= '0';
                                                                   s_selectVector1 <= s_selectVector0(31 {{downto}} 0);
                                                              {{else}} s_address(5)      <= '1';
                                                                   s_selectVector1 <= s_selectVector0(63 {{downto}} 32);
             {{end}} {{if}};
             {{if}} (s_selectVector1(31 {{downto}} 16) = X"0000") {{then}} s_address(4)      <= '0';
                                                               s_selectVector2 <= s_selectVector1(15 {{downto}} 0);
                                                          {{else}} s_address(4)      <= '1';
                                                               s_selectVector2 <= s_selectVector1(31 {{downto}} 16);
             {{end}} {{if}};
             {{if}} (s_selectVector2(15 {{downto}} 8) = X"00") {{then}} s_address(3)      <= '0';
                                                            s_selectVector3 <= s_selectVector2(7 {{downto}} 0);
                                                       {{else}} s_address(3)      <= '1';
                                                            s_selectVector3 <= s_selectVector2(15 {{downto}} 8);
             {{end}} {{if}};
             {{if}} (s_selectVector3(7 {{downto}} 4) = X"0") {{then}} s_address(2)      <= '0';
                                                          s_selectVector4 <= s_selectVector3(3 {{downto}} 0);
                                                     {{else}} s_address(2)      <= '1';
                                                          s_selectVector4 <= s_selectVector3(7 {{downto}} 4);
             {{end}} {{if}};
             {{if}} (s_selectVector4(3 {{downto}} 2) = "00") {{then}} s_address(1) <= '0';
                                                          s_address(0) <= s_selectVector4(1);
                                                     {{else}} s_address(1) <= '1';
                                                          s_address(0) <= s_selectVector4(3);
             {{end}} {{if}};
          {{end}} {{process}} makeAddr;
          """);
    } else {
      contents.add("""
          assign groupSelect = ~s_inIsZero&enable;
          assign enableOut = s_inIsZero&enable;
          assign address = (~enable) ? 0 : s_address[{{selBits}}-1:0];
          assign s_inIsZero = (inputVector == 0) ? 1'b1 : 1'b0;

          assign s_selectVector0[64:{{selBits}}] = 0;
          assign s_selectVector0[{{selBits}}-1:0] = inputVector;
          assign s_address[5] = (s_selectVector0[63:32] == 0) ? 1'b0 : 1'b1;
          assign s_selectVector1 = (s_selectVector0[63:32] == 0) ? s_selectVector0[31:0] : s_selectVector0[63:32];
          assign s_address[4] = (s_selectVector1[31:16] == 0) ? 1'b0 : 1'b1;
          assign s_selectVector2 = (s_selectVector1[31:16] == 0) ? s_selectVector1[15:0] : s_selectVector1[31:16];
          assign s_address[3] = (s_selectVector2[15:8] == 0) ? 1'b0 : 1'b1;
          assign s_selectVector3 = (s_selectVector2[15:8] == 0) ? s_selectVector2[7:0] : s_selectVector2[15:8];
          assign s_address[2] = (s_selectVector3[7:4] == 0) ? 1'b0 : 1'b1;
          assign s_selectVector4 = (s_selectVector3[7:4] == 0) ? s_selectVector3[3:0] : s_selectVector2[7:4];
          assign s_address[1] = (s_selectVector4[3:2] == 0) ? 1'b0 : 1'b1;
          assign s_address[0] = (s_selectVector4[3:2] == 0) ? s_selectVector4[1] : s_selectVector4[3];
          """);
    }
    return contents.empty();
  }
}
