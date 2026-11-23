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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class SquareRootHdlGeneratorFactory extends AbstractHdlGeneratorFactory {
  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public SquareRootHdlGeneratorFactory() {
    super();
    myParametersList.add(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    myPorts
        .add(Port.INPUT, "inputA", NR_OF_BITS_ID, SquareRoot.IN)
        .add(Port.INPUT, "upper", NR_OF_BITS_ID, SquareRoot.UPPER)
        .add(Port.OUTPUT, "squareRoot", NR_OF_BITS_ID, SquareRoot.OUT)
        .add(Port.OUTPUT, "remainder", NR_OF_BITS_ID, SquareRoot.REM);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer().pair("nrOfBits", NR_OF_BITS_STRING);

    // Restoring digit-by-digit method: two radicand bits are shifted into the partial remainder
    // per iteration and the trial value is (partialRoot << 2) | 1.
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          calcRoot : {{process}}(inputA, upper)
             {{variable}} v_radicand  : unsigned(2*{{nrOfBits}}-1 {{downto}} 0);
             {{variable}} v_remainder : unsigned({{nrOfBits}}+1 {{downto}} 0);
             {{variable}} v_trial     : unsigned({{nrOfBits}}+1 {{downto}} 0);
             {{variable}} v_root      : unsigned({{nrOfBits}}-1 {{downto}} 0);
          {{begin}}
             v_radicand  := unsigned(upper) & unsigned(inputA);
             v_remainder := ({{others}} => '0');
             v_root      := ({{others}} => '0');
             {{for}} i {{in}} {{nrOfBits}}-1 {{downto}} 0 {{loop}}
                v_remainder := v_remainder({{nrOfBits}}-1 {{downto}} 0) & v_radicand(2*i+1 {{downto}} 2*i);
                v_trial     := v_root & "01";
                v_root      := shift_left(v_root, 1);
                {{if}} v_remainder >= v_trial {{then}}
                   v_remainder := v_remainder - v_trial;
                   v_root(0)   := '1';
                {{end}} {{if}};
             {{end}} {{loop}};
             squareRoot <= std_logic_vector(v_root);
             remainder  <= std_logic_vector(v_remainder({{nrOfBits}}-1 {{downto}} 0));
          {{end}} {{process}} calcRoot;
          """);
    } else {
      contents.add("""
          reg [2*{{nrOfBits}}-1:0] s_radicand;
          reg [{{nrOfBits}}+1:0]   s_remainder;
          reg [{{nrOfBits}}+1:0]   s_trial;
          reg [{{nrOfBits}}-1:0]   s_root;
          integer                  s_index;

          always @(*)
          begin
             s_radicand  = {upper, inputA};
             s_remainder = 0;
             s_root      = 0;
             for (s_index = {{nrOfBits}}-1; s_index >= 0; s_index = s_index - 1)
             begin
                s_remainder = {s_remainder[{{nrOfBits}}-1:0], s_radicand[2*s_index +: 2]};
                s_trial     = {s_root, 2'b01};
                s_root      = s_root << 1;
                if (s_remainder >= s_trial)
                begin
                   s_remainder = s_remainder - s_trial;
                   s_root[0]   = 1'b1;
                end
             end
          end

          assign squareRoot = s_root;
          assign remainder  = s_remainder[{{nrOfBits}}-1:0];
          """);
    }
    return contents.empty();
  }
}
