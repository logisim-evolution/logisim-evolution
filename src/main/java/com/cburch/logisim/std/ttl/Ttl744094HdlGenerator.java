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

/** VHDL and Verilog for the shift register, storage latch and three-state outputs of a 744094. */
public class Ttl744094HdlGenerator extends AbstractHdlGeneratorFactory {
  private static final String[] PARALLEL_NAMES = {"QP0", "QP1", "QP2", "QP3", "QP4", "QP5", "QP6", "QP7"};

  /** Creates an HDL generator whose unconnected strobe and output enable are pulled low. */
  public Ttl744094HdlGenerator() {
    super();
    myWires
        .addRegister("shiftReg", 8)
        .addRegister("storeReg", 8)
        .addRegister("qs2Reg", 1);
    // An open strobe holds storage, and an open output enable leaves the parallel pins released.
    myPorts
        .add(Port.CLOCK, "CP", 1, Ttl744094.pinNrToPortNr(Ttl744094.CP))
        .add(Port.INPUT, "STR", 1, Ttl744094.pinNrToPortNr(Ttl744094.STR))
        .add(Port.INPUT, "D", 1, Ttl744094.pinNrToPortNr(Ttl744094.D))
        .add(Port.INPUT, "OE", 1, Ttl744094.pinNrToPortNr(Ttl744094.OE))
        .add(Port.OUTPUT, "QP0", 1, Ttl744094.pinNrToPortNr(Ttl744094.QP0))
        .add(Port.OUTPUT, "QP1", 1, Ttl744094.pinNrToPortNr(Ttl744094.QP1))
        .add(Port.OUTPUT, "QP2", 1, Ttl744094.pinNrToPortNr(Ttl744094.QP2))
        .add(Port.OUTPUT, "QP3", 1, Ttl744094.pinNrToPortNr(Ttl744094.QP3))
        .add(Port.OUTPUT, "QP4", 1, Ttl744094.pinNrToPortNr(Ttl744094.QP4))
        .add(Port.OUTPUT, "QP5", 1, Ttl744094.pinNrToPortNr(Ttl744094.QP5))
        .add(Port.OUTPUT, "QP6", 1, Ttl744094.pinNrToPortNr(Ttl744094.QP6))
        .add(Port.OUTPUT, "QP7", 1, Ttl744094.pinNrToPortNr(Ttl744094.QP7))
        .add(Port.OUTPUT, "QS1", 1, Ttl744094.pinNrToPortNr(Ttl744094.QS1))
        .add(Port.OUTPUT, "QS2", 1, Ttl744094.pinNrToPortNr(Ttl744094.QS2));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      for (var stage = 0; stage < PARALLEL_NAMES.length; stage++) {
        contents.add(
            "{{1}} <= storeReg({{2}}) {{when}} OE = '1' {{else}} 'Z';",
            PARALLEL_NAMES[stage],
            stage);
      }
      contents.add("""
          QS1 <= shiftReg(7);
          QS2 <= qs2Reg;

          shiftProc : {{process}}(CP) {{is}}
          {{begin}}
             {{if}} (rising_edge(CP)) {{then}}
                shiftReg <= shiftReg(6 {{downto}} 0) & D;
             {{end}} {{if}};
          {{end}} {{process}} shiftProc;

          qs2Proc : {{process}}(CP) {{is}}
          {{begin}}
             {{if}} (falling_edge(CP)) {{then}}
                qs2Reg <= shiftReg(7);
             {{end}} {{if}};
          {{end}} {{process}} qs2Proc;

          storeProc : {{process}}(STR, shiftReg) {{is}}
          {{begin}}
             {{if}} (STR = '1') {{then}} storeReg <= shiftReg;
             {{end}} {{if}};
          {{end}} {{process}} storeProc;
          """);
    } else {
      for (var stage = 0; stage < PARALLEL_NAMES.length; stage++) {
        contents.add(
            "assign {{1}} = (OE == 1) ? storeReg[{{2}}] : 1'bZ;",
            PARALLEL_NAMES[stage],
            stage);
      }
      contents.add("""
          assign QS1 = shiftReg[7];
          assign QS2 = qs2Reg;

          always @(posedge CP)
          begin
             shiftReg <= {shiftReg[6:0], D};
          end

          always @(negedge CP)
          begin
             qs2Reg <= shiftReg[7];
          end

          always @(*)
          begin
             if (STR == 1) storeReg <= shiftReg;
          end
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
