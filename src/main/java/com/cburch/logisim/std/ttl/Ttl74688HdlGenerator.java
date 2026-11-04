/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/** VHDL and Verilog generator for the 74x688 identity comparator. */
public class Ttl74688HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose unconnected inputs are pulled high. */
  public Ttl74688HdlGenerator() {
    super();
    myWires.addWire("s_mismatch", 1);
    // A floating enable reads high and disables the comparator. Floating data bits read as one.
    myPorts
        .add(Port.INPUT, "nE", 1, 0, false)
        .add(Port.INPUT, "P0", 1, 1, false)
        .add(Port.INPUT, "Q0", 1, 2, false)
        .add(Port.INPUT, "P1", 1, 3, false)
        .add(Port.INPUT, "Q1", 1, 4, false)
        .add(Port.INPUT, "P2", 1, 5, false)
        .add(Port.INPUT, "Q2", 1, 6, false)
        .add(Port.INPUT, "P3", 1, 7, false)
        .add(Port.INPUT, "Q3", 1, 8, false)
        .add(Port.INPUT, "P4", 1, 9, false)
        .add(Port.INPUT, "Q4", 1, 10, false)
        .add(Port.INPUT, "P5", 1, 11, false)
        .add(Port.INPUT, "Q5", 1, 12, false)
        .add(Port.INPUT, "P6", 1, 13, false)
        .add(Port.INPUT, "Q6", 1, 14, false)
        .add(Port.INPUT, "P7", 1, 15, false)
        .add(Port.INPUT, "Q7", 1, 16, false)
        .add(Port.OUTPUT, "nPQ", 1, 17);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          nPQ <= nE {{or}} s_mismatch;

          s_mismatch <= (P0 {{xor}} Q0) {{or}} (P1 {{xor}} Q1) {{or}}
                        (P2 {{xor}} Q2) {{or}} (P3 {{xor}} Q3) {{or}}
                        (P4 {{xor}} Q4) {{or}} (P5 {{xor}} Q5) {{or}}
                        (P6 {{xor}} Q6) {{or}} (P7 {{xor}} Q7);
          """);
    } else {
      contents.add(
          """
          assign nPQ = nE | s_mismatch;

          assign s_mismatch = (P0 ^ Q0) | (P1 ^ Q1) |
                              (P2 ^ Q2) | (P3 ^ Q3) |
                              (P4 ^ Q4) | (P5 ^ Q5) |
                              (P6 ^ Q6) | (P7 ^ Q7);
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) {
      return false;
    }
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
