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
import com.cburch.logisim.fpga.hdlgenerator.HDLParameters;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class SubtractorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String EXTENDED_BITS_STRING = "ExtendedBits";
  private static final int EXTENDED_BITS_ID = -2;

  public SubtractorHDLGeneratorFactory() {
    super();
    myParametersList
        .addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(EXTENDED_BITS_STRING, EXTENDED_BITS_ID, HDLParameters.MAP_OFFSET, 1);
    myWires
        .addWire("s_extended_dataA", EXTENDED_BITS_ID)
        .addWire("s_extended_dataB", EXTENDED_BITS_ID)
        .addWire("s_sum_result", EXTENDED_BITS_ID)
        .addWire("s_carry", 1);
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    int inputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NR_OF_BITS_ID;
    inputs.put("DataA", inputbits);
    inputs.put("DataB", inputbits);
    inputs.put("BorrowIn", 1);
    return inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = LineBuffer.getBuffer();
    int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDL.isVHDL()) {
      Contents.add("""
          s_extended_dataA <= "0"&DataA;
          s_extended_dataB <= "0"&(NOT(DataB));
          s_carry          <= NOT(BorrowIn);
          s_sum_result     <= std_logic_vector(unsigned(s_extended_dataA)+
                              unsigned(s_extended_dataB)+
                              (""&s_carry));

          """);
      Contents.add(
          (nrOfBits == 1)
              ? "Result <= s_sum_result(0);"
              : "Result <= s_sum_result( (" + NR_OF_BITS_STRING + "-1) DOWNTO 0 );");
      Contents.add("BorrowOut <= NOT(s_sum_result(" + EXTENDED_BITS_STRING + "-1));");
    } else {
      Contents.add("""
          assign   {s_carry,Result} = DataA + ~(DataB) + ~(BorrowIn);
          assign   BorrowOut = ~s_carry;
          """);
    }
    return Contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    int outputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NR_OF_BITS_ID;
    outputs.put("Result", outputbits);
    outputs.put("BorrowOut", 1);
    return outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    final var ComponentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("DataA", true, ComponentInfo, 0, Nets));
    portMap.putAll(GetNetMap("DataB", true, ComponentInfo, 1, Nets));
    portMap.putAll(GetNetMap("Result", true, ComponentInfo, 2, Nets));
    portMap.putAll(GetNetMap("BorrowIn", true, ComponentInfo, 3, Nets));
    portMap.putAll(GetNetMap("BorrowOut", true, ComponentInfo, 4, Nets));
    return portMap;
  }
}
