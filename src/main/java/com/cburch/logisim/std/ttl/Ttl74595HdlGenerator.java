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
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/** VHDL and Verilog for the shift register, storage register and three-state outputs of a 74595. */
public class Ttl74595HdlGenerator extends AbstractHdlGeneratorFactory {
  private static final String[] STORAGE_NAMES = {"QA", "QB", "QC", "QD", "QE", "QF", "QG", "QH"};

  /** Creates an HDL generator whose unconnected clear and output enable are pulled high. */
  public Ttl74595HdlGenerator() {
    super();
    myWires
        .addRegister("shiftReg", 8)
        .addRegister("storeReg", 8)
        .addWire("nextShift", 8)
        .addWire("nextStore", 8);
    // Active-low pins pull high so an open clear does not empty the shift register and an open
    // output enable leaves the parallel outputs released.
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74595.pinNrToPortNr(Ttl74595.SRCLK))
        .add(Port.CLOCK, HdlPorts.getClockName(2), 1, Ttl74595.pinNrToPortNr(Ttl74595.RCLK))
        .add(Port.INPUT, "nSRCLR", 1, Ttl74595.pinNrToPortNr(Ttl74595.SRCLR), false)
        .add(Port.INPUT, "nOE", 1, Ttl74595.pinNrToPortNr(Ttl74595.OE), false)
        .add(Port.INPUT, "SER", 1, Ttl74595.pinNrToPortNr(Ttl74595.SER))
        .add(Port.OUTPUT, "QA", 1, Ttl74595.pinNrToPortNr(Ttl74595.QA))
        .add(Port.OUTPUT, "QB", 1, Ttl74595.pinNrToPortNr(Ttl74595.QB))
        .add(Port.OUTPUT, "QC", 1, Ttl74595.pinNrToPortNr(Ttl74595.QC))
        .add(Port.OUTPUT, "QD", 1, Ttl74595.pinNrToPortNr(Ttl74595.QD))
        .add(Port.OUTPUT, "QE", 1, Ttl74595.pinNrToPortNr(Ttl74595.QE))
        .add(Port.OUTPUT, "QF", 1, Ttl74595.pinNrToPortNr(Ttl74595.QF))
        .add(Port.OUTPUT, "QG", 1, Ttl74595.pinNrToPortNr(Ttl74595.QG))
        .add(Port.OUTPUT, "QH", 1, Ttl74595.pinNrToPortNr(Ttl74595.QH))
        .add(Port.OUTPUT, "QHp", 1, Ttl74595.pinNrToPortNr(Ttl74595.QHP));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLK1", HdlPorts.getClockName(1))
            .pair("CLK2", HdlPorts.getClockName(2))
            .pair("tick1", HdlPorts.getTickName(1))
            .pair("tick2", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      for (var stage = 0; stage < STORAGE_NAMES.length; stage++) {
        contents.add(
            "{{1}} <= storeReg({{2}}) {{when}} nOE = '0' {{else}} 'Z';",
            STORAGE_NAMES[stage],
            stage);
      }
      contents.add("""
          QHp <= shiftReg(7);

          nextShift <= shiftReg(6 {{downto}} 0) & SER {{when}} {{tick1}} = '1' {{else}} shiftReg;
          nextStore <= ({{others}} => '0') {{when}} nSRCLR = '0' {{and}} {{tick2}} = '1' {{else}}
                        shiftReg {{when}} {{tick2}} = '1' {{else}}
                        storeReg;

          shiftProc : {{process}}({{CLK1}}, nSRCLR) {{is}}
          {{begin}}
             {{if}} (nSRCLR = '0') {{then}} shiftReg <= ({{others}} => '0');
             {{elsif}} (rising_edge({{CLK1}})) {{then}}
                {{if}} ({{tick1}} = '1') {{then}} shiftReg <= nextShift; {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} shiftProc;

          storeProc : {{process}}({{CLK2}}) {{is}}
          {{begin}}
             {{if}} (rising_edge({{CLK2}})) {{then}}
                {{if}} ({{tick2}} = '1') {{then}} storeReg <= nextStore; {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} storeProc;
          """);
    } else {
      for (var stage = 0; stage < STORAGE_NAMES.length; stage++) {
        contents.add(
            "assign {{1}} = (nOE == 0) ? storeReg[{{2}}] : 1'bZ;",
            STORAGE_NAMES[stage],
            stage);
      }
      contents.add("""
          assign QHp = shiftReg[7];
          assign nextShift = ({{tick1}} == 1) ? {shiftReg[6:0], SER} : shiftReg;
          assign nextStore = ({{tick2}} == 1) ? ((nSRCLR == 0) ? 8'b0 : shiftReg) : storeReg;

          always @(posedge {{CLK1}} or negedge nSRCLR)
          begin
             if (nSRCLR == 0) shiftReg <= 8'b0;
             else if ({{tick1}} == 1) shiftReg <= nextShift;
          end

          always @(posedge {{CLK2}})
          begin
             if ({{tick2}} == 1) storeReg <= nextStore;
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
