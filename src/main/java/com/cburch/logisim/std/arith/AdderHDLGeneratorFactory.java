/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class AdderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String EXTENDED_BITS_STRING = "ExtendedBits";
  private static final int EXTENDED_BITS_ID = -2;
  
  public AdderHDLGeneratorFactory() {
    super();
    myParametersList
      .add(EXTENDED_BITS_STRING, EXTENDED_BITS_ID)
      .addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var Inputs = new TreeMap<String, Integer>();
    int inputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NR_OF_BITS_ID;
    Inputs.put("DataA", inputbits);
    Inputs.put("DataB", inputbits);
    Inputs.put("CarryIn", 1);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = new LineBuffer();
    int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDL.isVHDL()) {
      Contents.add("""
          s_extended_dataA <= "0"&DataA;
          s_extended_dataB <= "0"&DataB;
          s_sum_result     <= std_logic_vector(unsigned(s_extended_dataA) +
                                               unsigned(s_extended_dataB) +
                                               (""&CarryIn));
          
          """);
      if (nrOfBits == 1) {
        Contents.add("Result <= s_sum_result(0);");
      } else {
        Contents.add("Result <= s_sum_result( ({{1}}-1) DOWNTO 0 )", NR_OF_BITS_STRING);
      }
      Contents.add("CarryOut <= s_sum_result({{1}}-1);", EXTENDED_BITS_STRING);
    } else {
      Contents.add("assign   {CarryOut,Result} = DataA + DataB + CarryIn;");
    }
    return Contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    int outputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NR_OF_BITS_ID;
    map.put("Result", outputbits);
    map.put("CarryOut", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var map = new TreeMap<String, Integer>();
    int nrOfBits = ComponentInfo.getComponent().getEnd(0).getWidth().getWidth();
    map.put(EXTENDED_BITS_STRING, nrOfBits + 1);
    if (nrOfBits > 1) map.put(NR_OF_BITS_STRING, nrOfBits);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var  portMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("DataA", true, ComponentInfo, 0, Nets));
    portMap.putAll(GetNetMap("DataB", true, ComponentInfo, 1, Nets));
    portMap.putAll(GetNetMap("Result", true, ComponentInfo, 2, Nets));
    portMap.putAll(GetNetMap("CarryIn", true, ComponentInfo, 3, Nets));
    portMap.putAll(GetNetMap("CarryOut", true, ComponentInfo, 4, Nets));
    return portMap;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    wires.put("s_extended_dataA", EXTENDED_BITS_ID);
    wires.put("s_extended_dataB", EXTENDED_BITS_ID);
    wires.put("s_sum_result", EXTENDED_BITS_ID);
    return wires;
  }
}
