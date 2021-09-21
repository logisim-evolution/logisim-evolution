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

public class SubtractorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String ExtendedBitsStr = "ExtendedBits";
  private static final int ExtendedBitsId = -2;

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    int inputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
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
          s_inverted_dataB <= NOT(DataB);
          s_extended_dataA <= "0"&DataA;
          s_extended_dataB <= "0"&s_inverted_dataB;
          s_carry          <= NOT(BorrowIn);
          s_sum_result     <= std_logic_vector(unsigned(s_extended_dataA)+
                              unsigned(s_extended_dataB)+
                              (""&s_carry));

          """);
      Contents.add(
          (nrOfBits == 1)
              ? "Result <= s_sum_result(0);"
              : "Result <= s_sum_result( (" + NrOfBitsStr + "-1) DOWNTO 0 );");
      Contents.add("BorrowOut <= NOT(s_sum_result(" + ExtendedBitsStr + "-1));");
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
    int outputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    outputs.put("Result", outputbits);
    outputs.put("BorrowOut", 1);
    return outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var params = new TreeMap<Integer, String>();
    int outputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (outputbits > 1) params.put(NrOfBitsId, NrOfBitsStr);
    params.put(ExtendedBitsId, ExtendedBitsStr);
    return params;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    final var parameterMap = new TreeMap<String, Integer>();
    int nrOfBits = componentInfo.getComponent().getEnd(0).getWidth().getWidth();
    parameterMap.put(ExtendedBitsStr, nrOfBits + 1);
    if (nrOfBits > 1) parameterMap.put(NrOfBitsStr, nrOfBits);
    return parameterMap;
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

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    int outputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    wires.put("s_extended_dataA", ExtendedBitsId);
    wires.put("s_extended_dataB", ExtendedBitsId);
    wires.put("s_inverted_dataB", (outputbits > 1) ? NrOfBitsId : 1);
    wires.put("s_sum_result", ExtendedBitsId);
    wires.put("s_carry", 1);
    return wires;
  }
}
