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

import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class MultiplierHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String CALC_BITS_STRING = "CalcBits";
  private static final int CALC_BITS_ID = -2;
  private static final String UNSIGNED_STRING = "UnsignedMultiplier";
  private static final int UNSIGNED_ID = -3;

  public MultiplierHDLGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(CALC_BITS_STRING, CALC_BITS_ID)
        .add(UNSIGNED_STRING, UNSIGNED_ID);
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put("INP_A", NR_OF_BITS_ID);
    inputs.put("INP_B", NR_OF_BITS_ID);
    inputs.put("Cin", NR_OF_BITS_ID);
    return inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents =
        (new LineBuffer())
            .pair("nrOfBits", NR_OF_BITS_STRING)
            .pair("unsigned", UNSIGNED_STRING)
            .pair("calcBits", CALC_BITS_STRING);

    if (HDL.isVHDL()) {
      Contents.add("""
          s_mult_result <= std_logic_vector(unsigned(INP_A)*unsigned(INP_B))
                              WHEN {{unsigned}}= 1 ELSE
                           std_logic_vector(signed(INP_A)*signed(INP_B));
          s_extended_Cin({{calcBits}}-1 DOWNTO {{nrOfBits}}) <= (OTHERS => '0') WHEN {{unsigned}} = 1 ELSE (OTHERS => Cin({{nrOfBits}}-1));
          s_extended_Cin({{nrOfBits}}-1 DOWNTO 0) <= Cin;
          s_new_result  <= std_logic_vector(unsigned(s_mult_result) + unsigned(s_extended_Cin))
                              WHEN {{unsigned}}= 1 ELSE
                           std_logic_vector(signed(s_mult_result) + signed(s_extended_Cin));
          Mult_hi       <= s_new_result({{calcBits}}-1 DOWNTO {{nrOfBits}});
          Mult_lo       <= s_new_result({{nrOfBits}}-1 DOWNTO 0);
          """);
    } else {
      Contents.add("""
          reg[{{calcBits}}-1:0] s_Cin;
          reg[{{calcBits}}-1:0] s_mult_unsigned;
          reg[{{calcBits}}-1:0] s_interm_result;
          reg signed[{{calcBits}}-1:0] s_mult_signed;
          always @(*)
          begin
             s_Cin[{{nrOfBits}}-1:0] = Cin;
             if ({{unsigned}}== 1)
                begin
                   s_Cin[{{calcBits}}-1:{{nrOfBits}}] = 0;
                   s_mult_unsigned = $unsigned(INP_A) * $unsigned(INP_B);
                   s_interm_result = $unsigned(s_mult_unsigned) + $unsigned(s_Cin);
                 end
              else
                begin
                   if (Cin[{{nrOfBits}}-1] == 1)
                      s_Cin[{{calcBits}}-1:{{nrOfBits}}] = -1;
                   else
                      s_Cin[{{calcBits}}-1:{{nrOfBits}}] = 0;
                   s_mult_signed = $signed(INP_A) * $signed(INP_B);
                   s_interm_result = $signed(s_mult_signed) + $signed(s_Cin);
                 end
          end
          
          assign Mult_hi = s_interm_result[{{calcBits}}-1:{{nrOfBits}}];
          assign Mult_lo = s_interm_result[{{nrOfBits}}-1:0];
          """);
    }
    return Contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put("Mult_lo", NR_OF_BITS_ID);
    outputs.put("Mult_hi", NR_OF_BITS_ID);
    return outputs;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var parameterMap = new TreeMap<String, Integer>();
    final var nrOfBits = ComponentInfo.getComponent().getEnd(0).getWidth().getWidth();
    boolean isUnsigned =
        ComponentInfo.getComponent()
            .getAttributeSet()
            .getValue(Multiplier.MODE_ATTR)
            .equals(Multiplier.UNSIGNED_OPTION);
    parameterMap.put(NR_OF_BITS_STRING, nrOfBits);
    parameterMap.put(CALC_BITS_STRING, 2 * nrOfBits);
    parameterMap.put(UNSIGNED_STRING, isUnsigned ? 1 : 0);
    return parameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("INP_A", true, ComponentInfo, Multiplier.IN0, Nets));
    portMap.putAll(GetNetMap("INP_B", true, ComponentInfo, Multiplier.IN1, Nets));
    portMap.putAll(GetNetMap("Cin", true, ComponentInfo, Multiplier.C_IN, Nets));
    portMap.putAll(GetNetMap("Mult_lo", true, ComponentInfo, Multiplier.OUT, Nets));
    portMap.putAll(GetNetMap("Mult_hi", true, ComponentInfo, Multiplier.C_OUT, Nets));
    return portMap;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    wires.put("s_mult_result", CALC_BITS_ID);
    wires.put("s_extended_Cin", CALC_BITS_ID);
    wires.put("s_new_result", CALC_BITS_ID);
    return wires;
  }
}
