/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractFlipFlopHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String INVERT_CLOCK_STRING = "InvertClockEnable";
  private static final int INVERT_CLOCK_ID = -1;
  private final int nrOfInputs;

  public static final Map<AttributeOption, Integer> TRIGGER_MAP = new HashMap<>() {{
        put(StdAttr.TRIG_HIGH, 0);
        put(StdAttr.TRIG_LOW, 1);
        put(StdAttr.TRIG_FALLING, 1);
        put(StdAttr.TRIG_RISING, 0);
      }};

  public AbstractFlipFlopHDLGeneratorFactory(int numInputs, Attribute<AttributeOption> triggerAttr) {
    super();
    nrOfInputs = numInputs;
    myParametersList
        .add(INVERT_CLOCK_STRING, INVERT_CLOCK_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, triggerAttr, TRIGGER_MAP);
    myWires
        .addWire("s_clock", 1)
        .addWire("s_next_state", 1)
        .addRegister("s_current_state_reg", 1);
    myPorts
        .add(Port.INPUT, "Reset", 1, nrOfInputs + 3)
        .add(Port.INPUT, "Preset", 1, nrOfInputs + 4)
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, nrOfInputs)
        .add(Port.OUTPUT, "Q", 1, nrOfInputs + 1)
        .add(Port.OUTPUT, "Q_bar", 1, nrOfInputs + 2);
  }

  @Override
  public ArrayList<String> getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    contents
        .pair("invertClock", INVERT_CLOCK_STRING)
        .pair("Clock", HdlPorts.CLOCK)
        .pair("Tick", HdlPorts.TICK)
        .addRemarkBlock("Here the output signals are defined")
        .add("""
                 {{assign}}Q       {{=}}s_current_state_reg;
                 {{assign}}Q_bar   {{=}}{{not}}(s_current_state_reg);
             """)
        .add(Hdl.isVhdl()
            ? "   s_clock {{=}} {{Clock}} WHEN {{invertClock}} = 0 ELSE NOT({{Clock}});"
            : "   assign s_clock {{=}} ({{invertClock}} == 0) ? {{Clock}} : ~{{Clock}};")
        .addRemarkBlock("Here the update logic is defined")
        .add(getUpdateLogic())
        .add("");
    if (Hdl.isVerilog()) {
      contents
          .addRemarkBlock("Here the initial register value is defined; for simulation only")
          .add("""
                   initial
                   begin
                      s_current_state_reg = 0;
                   end

                """);
    }

    contents.addRemarkBlock("Here the actual state register is defined");
    if (Hdl.isVhdl()) {
      contents.add("""
          make_memory : PROCESS( s_clock , Reset , Preset , {{Tick}} , s_next_state )
          BEGIN
             IF (Reset = '1') THEN s_current_state_reg <= '0';
             ELSIF (Preset = '1') THEN s_current_state_reg <= '1';
          """);
      if (Netlist.isFlipFlop(attrs)) {
        contents.add("   ELSIF (rising_edge(s_clock)) THEN");
      } else {
        contents.add("   ELSIF (s_clock = '1') THEN");
      }
      contents.add("""
                 IF ({{Tick}} = '1') THEN
                   s_current_state_reg <= s_next_state;
                END IF;
             END IF;
          END PROCESS make_memory;
          """);
    } else {
      if (Netlist.isFlipFlop(attrs)) {
        contents.add("""
            always @(posedge Reset or posedge Preset or posedge s_clock)
            begin
               if (Reset) s_current_state_reg <= 1'b0;
               else if (Preset) s_current_state_reg <= 1'b1;
               else if ({{Tick}}) s_current_state_reg <= s_next_state;
            end
            """);
      } else {
        contents
            .add("""
                always @(*)
                begin
                   if (Reset) s_current_state_reg <= 1'b0;
                   else if (Preset) s_current_state_reg <= 1'b1;
                   else if ({{Tick}} & (s_clock == 1'b1)) s_current_state_reg <= s_next_state;
                end
                """);
      }
    }
    contents.empty();
    return contents.getWithIndent();
  }

  public List<String> getUpdateLogic() {
    return new ArrayList<>();
  }
}
