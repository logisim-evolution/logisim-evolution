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
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class PriorityEncoderHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_SELECT_BITS_STRING = "NrOfSelectBits";
  private static final int NR_OF_SELECT_BITS_ID = -1;
  private static final String NR_OF_INPUT_BITS_STRING = "NrOfInputBits";
  private static final int NR_OF_INPUT_BITS_ID = -2;

  public PriorityEncoderHDLGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_INPUT_BITS_STRING, NR_OF_INPUT_BITS_ID, HdlParameters.MAP_POW2, PlexersLibrary.ATTR_SELECT)
        .add(NR_OF_SELECT_BITS_STRING, NR_OF_SELECT_BITS_ID, HdlParameters.MAP_INT_ATTRIBUTE, PlexersLibrary.ATTR_SELECT);
    myWires
        .addWire("s_in_is_zero", 1)
        .addWire("s_address", 5)
        .addWire("v_select_1_vector", 33)
        .addWire("v_select_2_vector", 16)
        .addWire("v_select_3_vector", 8)
        .addWire("v_select_4_vector", 4);
    myPorts
        .add(Port.INPUT, "enable", 1, 0)
        .add(Port.INPUT, "input_vector", NR_OF_INPUT_BITS_ID, 0)
        .add(Port.OUTPUT, "GroupSelect", 1, 0)
        .add(Port.OUTPUT, "EnableOut", 1, 0)
        .add(Port.OUTPUT, "Address", NR_OF_SELECT_BITS_ID, 0);
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
        map.putAll(Hdl.getNetMap("input_vector(" + i + ")", true, comp, i, nets));
      else {
        if (vectorList.length() > 0) vectorList.append(",");
        vectorList.append(Hdl.getNetName(comp, i, true, nets));
      }
    }
    if (Hdl.isVerilog()) map.put("input_vector", vectorList.toString());
    map.putAll(Hdl.getNetMap("GroupSelect", true, comp, nrOfBits + PriorityEncoder.GS, nets));
    map.putAll(Hdl.getNetMap("EnableOut", true, comp, nrOfBits + PriorityEncoder.EN_OUT, nets));
    map.putAll(Hdl.getNetMap("Address", true, comp, nrOfBits + PriorityEncoder.OUT, nets));
    return map;
  }

  @Override
  public ArrayList<String> getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer()
            .pair("selBits", NR_OF_SELECT_BITS_STRING)
            .pair("inBits", NR_OF_INPUT_BITS_STRING);
    if (Hdl.isVhdl()) {
      contents.add("""
          -- Output Signals
          GroupSelect <= NOT(s_in_is_zero) AND enable;
          EnableOut   <= s_in_is_zero AND enable;
          Address     <= (OTHERS => '0') WHEN enable = '0' ELSE
                         s_address({{selBits}}-1 DOWNTO 0);
       
          -- Control Signals 
          s_in_is_zero  <= '1' WHEN input_vector = std_logic_vector(to_unsigned(0,{{inBits}})) ELSE '0';
       
          -- Processes
          make_addr : PROCESS( input_vector , v_select_1_vector , v_select_2_vector , v_select_3_vector , v_select_4_vector )
          BEGIN
             v_select_1_vector(32 DOWNTO {{inBits}})  <= (OTHERS => '0');
             v_select_1_vector({{inBits}}-1 DOWNTO 0) <= input_vector;
             IF (v_select_1_vector(31 DOWNTO 16) = X"0000") THEN s_address(4)      <= '0';
                                                                 v_select_2_vector <= v_select_1_vector(15 DOWNTO 0);
                                                            ELSE s_address(4)      <= '1';
                                                                 v_select_2_vector <= v_select_1_vector(31 DOWNTO 16);
             END IF;
             IF (v_select_2_vector(15 DOWNTO 8) = X"00") THEN s_address(3)      <= '0';
                                                              v_select_3_vector <= v_select_2_vector(7 DOWNTO 0);
                                                         ELSE s_address(3)      <= '1';
                                                              v_select_3_vector <= v_select_2_vector(15 DOWNTO 8);
             END IF;
             IF (v_select_3_vector(7 DOWNTO 4) = X"0") THEN s_address(2)      <= '0';
                                                            v_select_4_vector <= v_select_3_vector(3 DOWNTO 0);
                                                       ELSE s_address(2)      <= '1';
                                                            v_select_4_vector <= v_select_3_vector(7 DOWNTO 4);
             END IF;
             IF (v_select_4_vector(3 DOWNTO 2) = "00") THEN s_address(1) <= '0';
                                                            s_address(0) <= v_select_4_vector(1);
                                                       ELSE s_address(1) <= '1';
                                                            s_address(0) <= v_select_4_vector(3);
             END IF;
          END PROCESS make_addr;
          """);
    } else {
      contents.add("""
          assign GroupSelect = ~s_in_is_zero&enable;
          assign EnableOut = s_in_is_zero&enable;
          assign Address = (~enable) ? 0 : s_address[{{selBits}}-1:0];
          assign s_in_is_zero = (input_vector == 0) ? 1'b1 : 1'b0;
          
          assign v_select_1_vector[32:{{selBits}}] = 0;
          assign v_select_1_vector[{{selBits}}-1:0] = input_vector;
          assign s_address[4] = (v_select_1_vector[31:16] == 0) ? 1'b0 : 1'b1;
          assign v_select_2_vector = (v_select_1_vector[31:16] == 0) ? v_select_1_vector[15:0] : v_select_1_vector[31:16];
          assign s_address[3] = (v_select_2_vector[15:8] == 0) ? 1'b0 : 1'b1;
          assign v_select_3_vector = (v_select_2_vector[15:8] == 0) ? v_select_2_vector[7:0] : v_select_2_vector[15:8];
          assign s_address[2] = (v_select_3_vector[7:4] == 0) ? 1'b0 : 1'b1;
          assign v_select_4_vector = (v_select_3_vector[7:4] == 0) ? v_select_3_vector[3:0] : v_select_2_vector[7:4];
          assign s_address[1] = (v_select_4_vector[3:2] == 0) ? 1'b0 : 1'b1;
          assign s_address[0] = (v_select_4_vector[3:2] == 0) ? v_select_4_vector[1] : v_select_4_vector[3];
          """);
    }
    return contents.getWithIndent();
  }
}
