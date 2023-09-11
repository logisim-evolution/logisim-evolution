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
import com.cburch.logisim.fpga.hdlgenerator.WithSelectHdlGenerator;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class Ttl7485HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl7485HdlGenerator() {
    super();
    myWires
        .addWire("oppA", 4)
        .addWire("oppB", 4)
        .addWire("gt", 1)
        .addWire("eq", 1)
        .addWire("lt", 1)
        .addWire("compIn", 3)
        .addWire("compOut", 3);
    myPorts
        .add(Port.INPUT, "A0", 1, 8)
        .add(Port.INPUT, "A1", 1, 10)
        .add(Port.INPUT, "A2", 1, 11)
        .add(Port.INPUT, "A3", 1, 13)
        .add(Port.INPUT, "B0", 1, 7)
        .add(Port.INPUT, "B1", 1, 9)
        .add(Port.INPUT, "B2", 1, 12)
        .add(Port.INPUT, "B3", 1, 0)
        .add(Port.INPUT, "AltBin", 1, 1)
        .add(Port.INPUT, "AeqBin", 1, 2)
        .add(Port.INPUT, "AgtBin", 1, 3)
        .add(Port.OUTPUT, "AltBout", 1, 6)
        .add(Port.OUTPUT, "AeqBout", 1, 5)
        .add(Port.OUTPUT, "AgtBout", 1, 4);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    final var decoder = new WithSelectHdlGenerator("dec1", "compIn", 3, "compOut", 3)
        .setDefault("001")
        .add("100", "100")
        .add("010", "010")
        .add("110", "000")
        .add("000", "110");
    contents.add(decoder.getHdlCode()).empty();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
            oppA   <= A3&A2&A1&A0;
            oppB   <= B3&B2&B1&B0;
            gt     <= '1' {{when}} unsigned(oppA) > unsigned(oppB) {{else}} '0';
            eq     <= '1' {{when}} unsigned(oppA) = unsigned(oppB) {{else}} '0';
            lt     <= '1' {{when}} unsigned(oppA) < unsigned(oppB) {{else}} '0';

            compIn <= AgtBin&AltBin&AeqBin;

            AgtBout <= '1' {{when}} gt = '1' {{else}} '0' {{when}} lt = '1' {{else}} compOut(2);
            AltBout <= '0' {{when}} gt = '1' {{else}} '1' {{when}} lt = '1' {{else}} compOut(1);
            AeqBout <= '0' {{when}} (gt = '1') {{or}} (lt = '1') {{else}} compOut(0);
            """);
    } else {
      contents.add("""
          assign oppA    = {A3, A2, A1, A0};
          assign oppB    = {B3, B2, B1, B0};
          assign gt      = oppA > oppB ? 1 : 0;
          assign eq      = oppA == oppB ? 1 : 0;
          assign lt      = oppA < oppB ? 1 : 0;
          assign compIn  = {AgtBin, AltBin, AeqBin};
          assign AgtBout = gt == 1 ? 1 : lt == 1 ? 0 : compOut[2];
          assign AltBout = gt == 1 ? 0 : lt == 1 ? 1 : compOut[1];
          assign AeqBout = gt == 1 || lt == 1 ? 0 : compOut[0];
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
