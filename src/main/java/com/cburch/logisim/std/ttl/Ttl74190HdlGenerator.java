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

/** VHDL and Verilog for the synchronous BCD counter and asynchronous load of a 74190. */
public class Ttl74190HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74190. An open load pin stays high, so it does not load. */
  public Ttl74190HdlGenerator() {
    super();
    myWires
        .addWire("s_up", 4)
        .addWire("s_down", 4)
        .addWire("s_next", 4)
        .addWire("s_data", 4)
        .addWire("s_visible", 4)
        .addWire("s_at_max", 1)
        .addWire("s_at_min", 1)
        .addWire("s_terminal", 1)
        .addRegister("s_count", 4)
        .addRegister("s_loading", 1);
    // nCE and nPL pull high, so an open counter holds and does not load. D/U and the data pins
    // pull low, which counts up from zero when the clock runs.
    myPorts
        .add(Port.INPUT, "D1", 1, Ttl74190.pinNrToPortNr(Ttl74190.D1), true)
        .add(Port.OUTPUT, "Q1", 1, Ttl74190.pinNrToPortNr(Ttl74190.Q1))
        .add(Port.OUTPUT, "Q0", 1, Ttl74190.pinNrToPortNr(Ttl74190.Q0))
        .add(Port.INPUT, "nCE", 1, Ttl74190.pinNrToPortNr(Ttl74190.CE), false)
        .add(Port.INPUT, "DU", 1, Ttl74190.pinNrToPortNr(Ttl74190.DU), true)
        .add(Port.OUTPUT, "Q2", 1, Ttl74190.pinNrToPortNr(Ttl74190.Q2))
        .add(Port.OUTPUT, "Q3", 1, Ttl74190.pinNrToPortNr(Ttl74190.Q3))
        .add(Port.INPUT, "D3", 1, Ttl74190.pinNrToPortNr(Ttl74190.D3), true)
        .add(Port.INPUT, "D2", 1, Ttl74190.pinNrToPortNr(Ttl74190.D2), true)
        .add(Port.INPUT, "nPL", 1, Ttl74190.pinNrToPortNr(Ttl74190.PL), false)
        .add(Port.OUTPUT, "TC", 1, Ttl74190.pinNrToPortNr(Ttl74190.TC))
        .add(Port.OUTPUT, "nRC", 1, Ttl74190.pinNrToPortNr(Ttl74190.RC))
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74190.pinNrToPortNr(Ttl74190.CP), true)
        .add(Port.INPUT, "D0", 1, Ttl74190.pinNrToPortNr(Ttl74190.D0), true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      addVhdl(contents);
    } else {
      addVerilog(contents);
    }
    return contents.empty();
  }

  private static void addVhdl(LineBuffer contents) {
    addVhdlSelect(contents, "s_up", Ttl74190.COUNT_UP);
    addVhdlSelect(contents, "s_down", Ttl74190.COUNT_DOWN);
    contents.add(
        """
        s_data <= D3 & D2 & D1 & D0;
        s_visible <= s_data {{when}} nPL = '0' {{else}} s_count;
        s_next <= s_count {{when}} {{tick}} = '0' {{or}} nCE = '1' {{else}}
                  s_up {{when}} DU = '0' {{else}}
                  s_down;
        s_at_max <= '1' {{when}} DU = '0' {{and}} s_visible(0) = '1' {{and}}
                    s_visible(3) = '1' {{else}} '0';
        s_at_min <= '1' {{when}} DU = '1' {{and}} s_visible = "0000" {{else}} '0';
        s_terminal <= '1' {{when}} s_at_max = '1' {{or}} s_at_min = '1' {{else}} '0';
        TC <= s_terminal;
        nRC <= '0' {{when}} s_terminal = '1' {{and}} nCE = '0' {{and}} {{CLK}} = '0' {{else}} '1';
        Q0 <= s_visible(0);
        Q1 <= s_visible(1);
        Q2 <= s_visible(2);
        Q3 <= s_visible(3);

        count : {{process}}({{CLK}}, nPL, D0, D1, D2, D3) {{is}}
        {{begin}}
           {{if}} (nPL = '0') {{then}}
              s_count <= s_data;
              s_loading <= '1';
           {{elsif}} (s_loading = '1') {{then}}
              s_count <= s_data;
              s_loading <= '0';
           {{elsif}} (rising_edge({{CLK}})) {{then}}
              s_count <= s_next;
              s_loading <= '0';
           {{end}} {{if}};
        {{end}} {{process}} count;
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add("assign s_up = " + verilogSelect(Ttl74190.COUNT_UP) + ";");
    contents.add("assign s_down = " + verilogSelect(Ttl74190.COUNT_DOWN) + ";");
    contents.add(
        """
        assign s_data = {D3, D2, D1, D0};
        assign s_visible = (nPL == 0 || s_loading == 1) ? s_data : s_count;
        assign s_next = ({{tick}} == 0 || nCE == 1) ? s_count :
                        (DU == 0 ? s_up : s_down);
        assign s_at_max = (DU == 0) && s_visible[0] && s_visible[3];
        assign s_at_min = (DU == 1) && (s_visible == 4'b0000);
        assign s_terminal = s_at_max || s_at_min;
        assign TC = s_terminal;
        assign nRC = ((s_terminal == 1) && (nCE == 0) && ({{CLK}} == 0)) ? 1'b0 : 1'b1;
        assign Q0 = s_visible[0];
        assign Q1 = s_visible[1];
        assign Q2 = s_visible[2];
        assign Q3 = s_visible[3];

        always @(posedge {{CLK}} or nPL)
        begin
           if (nPL == 0) begin
              s_count <= s_data;
              s_loading <= 1'b1;
           end else if (s_loading == 1) begin
              s_count <= s_data;
              s_loading <= 1'b0;
           end else begin
              s_count <= s_next;
              s_loading <= 1'b0;
           end
        end
        """);
  }

  private static void addVhdlSelect(LineBuffer contents, String name, int[] table) {
    for (var code = 0; code < table.length; code++) {
      final var next = bits(table[code]);
      final var current = bits(code);
      if (code == 0) {
        contents.add(name + " <= \"" + next + "\" {{when}} s_count = \"" + current + "\" {{else}}");
      } else if (code == table.length - 1) {
        contents.add("        \"" + next + "\";");
      } else {
        contents.add("        \"" + next + "\" {{when}} s_count = \"" + current + "\" {{else}}");
      }
    }
  }

  private static String verilogSelect(int[] table) {
    final var text = new StringBuilder();
    for (var code = 0; code < table.length; code++) {
      text.append("(s_count == 4'b")
          .append(bits(code))
          .append(") ? 4'b")
          .append(bits(table[code]))
          .append(" : ");
    }
    return text.append("4'b0000").toString();
  }

  private static String bits(int code) {
    return String.format("%4s", Integer.toBinaryString(code & 0xF)).replace(' ', '0');
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
