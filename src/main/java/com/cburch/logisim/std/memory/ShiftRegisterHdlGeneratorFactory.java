/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShiftRegisterHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NEGATE_CLOCK_STRING = "negateClock";
  private static final int NEGATE_CLOCK_ID = -1;
  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -2;

  public ShiftRegisterHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NEGATE_CLOCK_STRING, NEGATE_CLOCK_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, StdAttr.EDGE_TRIGGER, AbstractFlipFlopHdlGeneratorFactory.TRIGGER_MAP)
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var hasParallelLoad = attrs.getValue(ShiftRegister.ATTR_LOAD);
    final var clasicLogisim = attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC;
    final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    for (var idx = 0; idx < nrOfStages; idx++) {
      myWires
          .addRegister(String.format("s_stageReg%d", idx), NR_OF_BITS_ID)
          .addWire(String.format("s_stageNext%d", idx), NR_OF_BITS_ID);
    }
    myWires.addWire("s_clock", 1);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, ShiftRegister.CK)
        .add(Port.INPUT, "reset", 1, ShiftRegister.CLR)
        .add(Port.INPUT, "shiftEnable", 1, ShiftRegister.SH)
        .add(Port.INPUT, "shiftIn", NR_OF_BITS_ID, ShiftRegister.IN)
        .add(Port.OUTPUT, "shiftOut", NR_OF_BITS_ID, ShiftRegister.OUT);
    if (hasParallelLoad) {
      for (var idx = 0; idx < nrOfStages; idx++) {
        myPorts.add(Port.INPUT, String.format("D%d", idx), NR_OF_BITS_ID, 6 + 2 * idx);
        if ((idx == nrOfStages - 1 && clasicLogisim) || (idx < nrOfStages - 1)) {
          myPorts.add(Port.OUTPUT, String.format("Q%d", idx), NR_OF_BITS_ID, 6 + 2 * idx + 1);
        }
      }
      myPorts.add(Port.INPUT, "parLoad", 1, ShiftRegister.LD);
    } else {
      myPorts.add(Port.INPUT, "parLoad", 1, Hdl.zeroBit());
      for (var idx = 0; idx < nrOfStages; idx++) {
        myPorts.add(Port.INPUT, String.format("D%d", idx), NR_OF_BITS_ID, Hdl.getZeroVector(nrOfBits, true));
        if ((idx == nrOfStages - 1 && clasicLogisim) || (idx < nrOfStages - 1)) {
          myPorts.add(Port.OUTPUT, String.format("Q%d", idx), NR_OF_BITS_ID, Hdl.unconnected(true));
        }
      }
    }
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>(super.getPortMap(nets, mapInfo));
    if (mapInfo instanceof final netlistComponent comp) {
      final var attrs = comp.getComponent().getAttributeSet();
      final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
      final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
      final var clasicLogisim = attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC;
      if (Hdl.isVhdl() && nrOfBits == 1) {
        final var shiftMap = map.get("shiftIn");
        final var outMap = map.get("shiftOut");
        map.remove("shiftIn");
        map.remove("shiftOut");
        map.put("shiftIn(0)", shiftMap);
        map.put("shiftOut(0)", outMap);
        for (var idx = 0; idx < nrOfStages; idx++) {
          final var dName = String.format("D%d", idx);
          final var dMap = map.get(dName);
          map.remove(dName);
          map.put(dName + "(0)", dMap);
          if ((idx == nrOfStages - 1 && clasicLogisim) || (idx < nrOfStages - 1)) {
            final var qName = String.format("Q%d", idx);
            final var qMap = map.get(qName);
            map.remove(qName);
            map.put(qName + "(0)", qMap);
          }
        }
      }
    }
    return map;
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("clock", HdlPorts.getClockName(1))
        .pair("tick", HdlPorts.getTickName(1))
        .pair("invertClock", NEGATE_CLOCK_STRING)
        .pair("nrOfBits", NR_OF_BITS_STRING);
    final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
    final var clasicLogisim = attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC;
    if (Hdl.isVhdl()) {
      contents.empty()
          .addVhdlKeywords()
          .addRemarkBlock("Here the outputs are mapped")
          .add(String.format("shiftOut <= s_stageReg%d;", nrOfStages - 1));
      for (var idx = 0; idx < nrOfStages; idx++) {
        if ((idx == nrOfStages - 1 && clasicLogisim) || (idx < nrOfStages - 1)) {
          contents.add(String.format("Q%d <= s_stageReg%d;", idx, idx));
        }
      }
      contents
          .empty()
          .addRemarkBlock("Here the next state is defined")
          .add("""
              s_stageNext0 <= D0 {{when}} parLoad = '1' {{else}}
                              shiftIn {{when}} shiftEnable = '1' {{else}} s_stageReg0;
              """);
      for (var idx = 1; idx < nrOfStages; idx++) {
        contents.add(String.format("s_stageNext%d <= D%d {{when}} parLoad = '1' {{else}}", idx, idx));
        contents.add(String.format("                 s_stageReg%d {{when}} shiftEnable = '1' {{else}} s_stageReg%d;", idx - 1, idx));
      }
      contents
          .empty()
          .addRemarkBlock("Here the state registers are defined")
          .add("""
              s_clock <= {{clock}} {{when}} {{invertClock}} = 0 {{else}} {{not}}({{clock}});

              makeMem : {{process}} (s_clock, reset) {{is}}
                {{begin}}
                  {{if}} (reset = '1') {{then}}
              """);
      for (var idx = 0; idx < nrOfStages; idx++) {
        contents.add(String.format("      s_stageReg%d <= ({{others}} => '0');", idx));
      }
      contents
          .add("""
              {{elsif}} (rising_edge(s_clock)) {{then}}
                {{if}} ({{tick}} = '1') {{then}}
          """);
      for (var idx = 0; idx < nrOfStages; idx++) {
        contents.add(String.format("        s_stageReg%d <= s_stageNext%d;", idx, idx));
      }
      contents
          .add("""
                    {{end}} {{if}};
                  {{end}} {{if}};
                {{end}} {{process}} makeMem;
              """);
    } else {
      contents.empty()
          .addRemarkBlock("Here the outputs are mapped")
          .add(String.format("assign shiftOut = s_stageReg%d;", nrOfStages - 1));
      for (var idx = 0; idx < nrOfStages; idx++) {
        if ((idx == nrOfStages - 1 && clasicLogisim) || (idx < nrOfStages - 1)) {
          contents.add(String.format("assign Q%d = s_stageReg%d;", idx, idx));
        }
      }
      contents
          .empty()
          .addRemarkBlock("Here the next state is defined")
          .add("""
              assign s_stageNext0 = (parLoad == 1'b1) ? D0 : (shiftEnable == 1'b1) ? shiftIn : s_stageReg0;
              """);
      for (var idx = 1; idx < nrOfStages; idx++) {
        contents
          .add(String.format("assign s_stageNext%d = (parLoad == 1'b1) ? D%d : (shiftEnable == 1'b1) ? s_stageReg%d : s_stageReg%d;", idx, idx, idx - 1, idx));
      }
      contents
          .empty()
          .addRemarkBlock("Here the state registers are defined")
          .add("""
              assign s_clock = ({{invertClock}} == 0) ? {{clock}} : ~{{clock}};

              always @(posedge s_clock or posedge reset)
                begin
              """);
      for (var idx = 0; idx < nrOfStages; idx++) {
        contents.add(String.format("      s_stageReg%d <= (reset == 1'b1) ? 0 : ({{tick}} == 1'b1) ? s_stageNext%d : s_stageReg%d;", idx, idx, idx));
      }
      contents.add("  end");
    }
    return contents.empty();
  }
}
