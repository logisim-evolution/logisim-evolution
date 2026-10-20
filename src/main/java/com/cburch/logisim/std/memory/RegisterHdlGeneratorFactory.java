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

public class RegisterHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_Id = -1;
  private static final String INVERT_CLOCK_STRING = "invertClock";
  private static final int INVERT_CLOCK_Id = -2;

  public RegisterHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_Id)
        .add(INVERT_CLOCK_STRING, INVERT_CLOCK_Id, HdlParameters.MAP_ATTRIBUTE_OPTION, StdAttr.TRIGGER, AbstractFlipFlopHdlGeneratorFactory.TRIGGER_MAP);
    myWires
        .addWire("s_clock", 1)
        .addRegister("s_currentState", NR_OF_BITS_Id);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Register.CK)
        .add(Port.INPUT, "reset", 1, Register.CLR)
        .add(Port.INPUT, "clockEnable", 1, Register.EN, false)
        .add(Port.INPUT, "d", NR_OF_BITS_Id, Register.IN)
        .add(Port.OUTPUT, "q", NR_OF_BITS_Id, Register.OUT);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>(super.getPortMap(Nets, MapInfo));
    if (MapInfo instanceof final netlistComponent comp && Hdl.isVhdl()) {
      final var nrOfBits = comp.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
      if (nrOfBits == 1) {
        final var inMap = map.get("d");
        final var outMap = map.get("q");
        map.remove("d");
        map.remove("q");
        map.put("d(0)", inMap);
        map.put("q(0)", outMap);
      }
    }
    return map;
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer()
            .pair("invertClock", INVERT_CLOCK_STRING)
            .pair("clock", HdlPorts.getClockName(1))
            .pair("Tick", HdlPorts.getTickName(1));
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          q       <= s_currentState;
          s_clock <= {{clock}} {{when}} {{invertClock}} = 0 {{else}} {{not}}({{clock}});

          makeMemory : {{process}}(s_clock, reset, clockEnable, {{Tick}}, d) {{is}}
          {{begin}}
             {{if}} (reset = '1') {{then}} s_currentState <= ({{others}} => '0');
          """);
      if (Netlist.isFlipFlop(attrs)) {
        contents.add("""
               {{elsif}} (rising_Edge(s_clock)) {{then}}
                  {{if}} (clockEnable = '1' {{and}} {{Tick}} = '1') {{then}}
                     s_currentState <= d;
                  {{end}} {{if}};
               """);
      } else {
        contents.add("""
              {{elsif}} (s_clock = '1') {{then}}
                 {{if}} (clockEnable = '1' {{and}} {{Tick}} = '1') {{then}}
                    s_currentState <= d;
                 {{end}} {{if}};
              """);
      }
      contents.add("""
                 {{end}} {{if}};
              {{end}} {{process}} makeMemory;
              """);
    } else {
      contents.empty().add("""
            assign q = s_currentState;
            assign s_clock = {{invertClock}} == 0 ? {{clock}} : ~{{clock}};
            """)
          .empty();
      if (Netlist.isFlipFlop(attrs)) {
        contents.add("""
            always @(posedge s_clock or posedge reset)
            begin
               if (reset) s_currentState <= 0;
               else if (clockEnable&{{Tick}}) s_currentState <= d;
            end
            """);
      } else {
        contents.add("""
            always @(*)
            begin
               if (reset) s_currentState <= 0;
               else if (s_Clock&clockEnable&{{Tick}}) s_currentState <= d;
            end
            """);
      }
    }
    return contents.empty();
  }
}
