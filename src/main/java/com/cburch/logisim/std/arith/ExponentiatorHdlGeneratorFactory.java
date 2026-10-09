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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class ExponentiatorHdlGeneratorFactory extends AbstractHdlGeneratorFactory {
  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String CALC_BITS_STRING = "calcBits";
  private static final int CALC_BITS_ID = -2;
  private static final String SIGNED_STRING = "signedExponentiator";
  private static final int SIGNED_ID = -3;

  public ExponentiatorHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(CALC_BITS_STRING, CALC_BITS_ID, HdlParameters.MAP_MULTIPLY, 2)
        .add(SIGNED_STRING, SIGNED_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, Comparator.MODE_ATTR,
            ComparatorHdlGeneratorFactory.SIGNED_MAP);
    myPorts
        .add(Port.INPUT, "inputBase", NR_OF_BITS_ID, Exponentiator.BASE)
        .add(Port.INPUT, "inputExponent", NR_OF_BITS_ID, Exponentiator.EXP)
        .add(Port.OUTPUT, "powLow", NR_OF_BITS_ID, Exponentiator.LOW_OUT)
        .add(Port.OUTPUT, "powHigh", NR_OF_BITS_ID, Exponentiator.UPP_OUT);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("nrOfBits", NR_OF_BITS_STRING)
        .pair("calcBits", CALC_BITS_STRING)
        .pair("signedMode", SIGNED_STRING);

    // Square-and-multiply over the exponent bits, kept modulo 2^calcBits. The low calcBits bits of
    // a product are identical for signed and unsigned operands, so only the base extension and the
    // negative-exponent handling depend on the mode.
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          calcPower : {{process}}(inputBase, inputExponent)
             {{variable}} v_result : unsigned({{calcBits}}-1 {{downto}} 0);
             {{variable}} v_power  : unsigned({{calcBits}}-1 {{downto}} 0);
          {{begin}}
             {{if}} {{signedMode}} = 1 {{then}}
                v_power := unsigned(resize(signed(inputBase), {{calcBits}}));
             {{else}}
                v_power := resize(unsigned(inputBase), {{calcBits}});
             {{end}} {{if}};
             v_result := to_unsigned(1, {{calcBits}});
             {{for}} i {{in}} 0 {{to}} {{nrOfBits}}-1 {{loop}}
                {{if}} inputExponent(i) = '1' {{then}}
                   v_result := resize(v_result * v_power, {{calcBits}});
                {{end}} {{if}};
                v_power := resize(v_power * v_power, {{calcBits}});
             {{end}} {{loop}};
             {{if}} {{signedMode}} = 1 {{and}} inputExponent({{nrOfBits}}-1) = '1' {{and}} unsigned(inputBase) /= 1 {{then}}
                v_result := ({{others}} => '0');
             {{end}} {{if}};
             powLow  <= std_logic_vector(v_result({{nrOfBits}}-1 {{downto}} 0));
             powHigh <= std_logic_vector(v_result({{calcBits}}-1 {{downto}} {{nrOfBits}}));
          {{end}} {{process}} calcPower;
          """);
    } else {
      contents.add("""
          reg [{{calcBits}}-1:0] s_result;
          reg [{{calcBits}}-1:0] s_power;
          integer                s_index;

          always @(*)
          begin
             if ({{signedMode}} == 1)
                s_power = $signed(inputBase);
             else
                s_power = inputBase;
             s_result = 1;
             for (s_index = 0; s_index < {{nrOfBits}}; s_index = s_index + 1)
             begin
                if (inputExponent[s_index])
                   s_result = s_result * s_power;
                s_power = s_power * s_power;
             end
             if ({{signedMode}} == 1 && inputExponent[{{nrOfBits}}-1] && inputBase != 1)
                s_result = 0;
          end

          assign powLow  = s_result[{{nrOfBits}}-1:0];
          assign powHigh = s_result[{{calcBits}}-1:{{nrOfBits}}];
          """);
    }
    return contents.empty();
  }
}
