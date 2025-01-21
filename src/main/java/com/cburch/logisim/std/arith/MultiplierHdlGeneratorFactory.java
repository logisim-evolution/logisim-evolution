/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import java.util.HashMap;
import java.util.Map;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class MultiplierHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String CALC_BITS_STRING = "calcBits";
  private static final int CALC_BITS_ID = -2;
  private static final String UNSIGNED_STRING = "unsignedMultiplier";
  private static final int UNSIGNED_ID = -3;

  public static final Map<AttributeOption, Integer> SIGNED_MAP = new HashMap<>() {{
      put(Multiplier.UNSIGNED_OPTION, 0);
      put(Multiplier.SIGNED_UNSIGNED_OPTION, 1);
      put(Multiplier.SIGNED_OPTION, 2);
    }};


  public MultiplierHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(CALC_BITS_STRING, CALC_BITS_ID, HdlParameters.MAP_MULTIPLY, 2)
        .add(UNSIGNED_STRING, UNSIGNED_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, Multiplier.MODE_ATTR, SIGNED_MAP);
    myWires
        .addWire("s_multResult", CALC_BITS_ID)
        .addWire("s_extendedcarryIn", CALC_BITS_ID)
        .addWire("s_newResult", CALC_BITS_ID);
    myPorts
        .add(Port.INPUT, "inputA", NR_OF_BITS_ID, Multiplier.IN0)
        .add(Port.INPUT, "inputB", NR_OF_BITS_ID, Multiplier.IN1)
        .add(Port.INPUT, "carryIn", NR_OF_BITS_ID, Multiplier.C_IN)
        .add(Port.OUTPUT, "multLow", NR_OF_BITS_ID, Multiplier.OUT)
        .add(Port.OUTPUT, "multHigh", NR_OF_BITS_ID, Multiplier.C_OUT);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("nrOfBits", NR_OF_BITS_STRING)
        .pair("unsigned", UNSIGNED_STRING)
        .pair("calcBits", CALC_BITS_STRING);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          s_multResult <= std_logic_vector(unsigned(inputA)*unsigned(inputB))
                              {{when}} {{unsigned}}= 1 {{else}}
                           std_logic_vector(signed(inputA)*unsigned(inputB));
                              {{when}} {{unsigned}}= 2 {{else}}
                           std_logic_vector(signed(inputA)*signed(inputB));
          s_extendedcarryIn({{calcBits}}-1 {{downto}} {{nrOfBits}}) <= ({{others}} => '0') {{when}} {{unsigned}} = 1 or {{unsigned}} = 2 {{else}} ({{others}} => carryIn({{nrOfBits}}-1));
          s_extendedcarryIn({{nrOfBits}}-1 {{downto}} 0) <= carryIn;
          s_newResult  <= std_logic_vector(unsigned(s_multResult) + unsigned(s_extendedcarryIn))
                              {{when}} {{unsigned}}= 1 {{else}}
                           std_logic_vector(signed(s_multResult) + unsigned(s_extendedcarryIn));
                              {{when}} {{unsigned}}= 2 {{else}}
                           std_logic_vector(signed(s_multResult) + signed(s_extendedcarryIn));
          multHigh     <= s_newResult({{calcBits}}-1 {{downto}} {{nrOfBits}});
          multLow      <= s_newResult({{nrOfBits}}-1 {{downto}} 0);
          """);
    } else {
      contents
          .add("""
              reg[{{calcBits}}-1:0] s_carryIn;
              reg[{{calcBits}}-1:0] s_multUnsigned;
              reg[{{calcBits}}-1:0] s_intermediateResult;
              reg signed[{{calcBits}}-1:0] s_multSigned;
              
              always @(*)
              begin
                 s_carryIn[{{nrOfBits}}-1:0] = carryIn;
                 if ({{unsigned}}== 1)
                    begin
                       s_carryIn[{{calcBits}}-1:{{nrOfBits}}] = 0;
                       s_multUnsigned = $unsigned(inputA) * $unsigned(inputB);
                       s_intermediateResult = $unsigned(s_multUnsigned) + $unsigned(s_carryIn);
                     end
                  else if ({{unsigned}}== 2)
                     begin
                       s_carryIn[{{calcBits}}-1:{{nrOfBits}}] = 0;
                       s_multUnsigned = $signed(inputA) * $unsigned(inputB);
                       s_intermediateResult = $signed(s_multUnsigned) + $signed(s_carryIn);
                     end
                  else
                    begin
                       if (carryIn[{{nrOfBits}}-1] == 1)
                          s_carryIn[{{calcBits}}-1:{{nrOfBits}}] = -1;
                       else
                          s_carryIn[{{calcBits}}-1:{{nrOfBits}}] = 0;
                       s_multSigned = $signed(inputA) * $signed(inputB);
                       s_intermediateResult = $signed(s_multSigned) + $signed(s_carryIn);
                     end
              end

              assign multHigh = s_intermediateResult[{{calcBits}}-1:{{nrOfBits}}];
              assign multLow  = s_intermediateResult[{{nrOfBits}}-1:0];
              """);
    }
    return contents.empty();
  }
}
