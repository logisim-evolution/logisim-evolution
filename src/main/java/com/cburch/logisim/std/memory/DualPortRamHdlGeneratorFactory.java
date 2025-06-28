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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class DualPortRamHdlGeneratorFactory extends AbstractHdlGeneratorFactory {
  //TODO: fix HDL generation
  private static final String ByteArrayStr = "byteArray";
  private static final int ByteArrayId = -1;
  private static final String RestArrayStr = "restArray";
  private static final int RestArrayId = -2;
  private static final String MemArrayStr = "memoryArray";
  private static final int MemArrayId = -3;

  public DualPortRamHdlGeneratorFactory() {
    super();
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var be = attrs.getValue(DualPortRamAttributes.ATTR_ByteEnables);
    final var byteEnables = be != null && be.equals(DualPortRamAttributes.BUS_WITH_BYTEENABLES);
    final var byteEnableOffset = DualPortRamAppearance.getBEIndex(0, attrs);
    final var nrBePorts = DualPortRamAppearance.getNrBEPorts(attrs);
    final var nrOfaddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    final var trigger = attrs.getValue(StdAttr.TRIGGER);
    final var async = StdAttr.TRIG_HIGH.equals(trigger) || StdAttr.TRIG_LOW.equals(trigger);
    final var ramEntries = (1 << nrOfaddressLines);
    final var truncated = (nrOfBits % 8) != 0;
    //TODO: add wires or registers depending on whether it's verilog or VHDL?
    myWires
        .addWire("s_ramdataOut", nrOfBits)
        .addRegister("s_tickDelayLine", 3)
        .addRegister("s_dataInReg", nrOfBits)
        .addRegister("s_address1Reg", nrOfaddressLines)
        .addRegister("s_address2Reg", nrOfaddressLines)
        .addRegister("s_addressWriteReg", nrOfaddressLines)
        .addRegister("s_weReg", 1)
        .addRegister("s_oe1Reg", 1)
        .addRegister("s_oe2Reg", 1)
        .addRegister("s_dataOut1Reg", nrOfBits)
        .addRegister("s_dataOut2Reg", nrOfBits);
    if (byteEnables) {
      myWires
          .addRegister("s_byteEnableReg", nrBePorts);
      for (var idx = 0; idx < nrBePorts; idx++) {
        myWires
            .addWire(String.format("s_byteEnable%d", idx), 1);
        myPorts
            .add(Port.INPUT, String.format("byteEnable%d", idx), 1, byteEnableOffset + nrBePorts - idx - 1);
      }
      myWires.addWire("s_we", 1);
      myPorts.add(Port.INPUT, "oe1", 1, DualPortRamAppearance.getOEIndex(0, attrs));
      myPorts.add(Port.INPUT, "oe2", 1, DualPortRamAppearance.getOEIndex(1, attrs));
      var nrOfMems = nrBePorts;
      if (truncated) {
        myTypedWires
            .addArray(RestArrayId, RestArrayStr, nrOfBits % 8, ramEntries)
            .addWire("s_truncMemContents", RestArrayId);
        nrOfMems--;
      }
      myTypedWires
          .addArray(ByteArrayId, ByteArrayStr, 8, ramEntries);
      for (var mem = 0; mem < nrOfMems; mem++)
        myTypedWires
            .addWire(String.format("s_byteMem%dContents", mem), ByteArrayId);
    } else {
      myPorts
          .add(Port.INPUT, "oe1", 1, Hdl.oneBit())
          .add(Port.INPUT, "oe2", 1, Hdl.oneBit());
      myTypedWires
          .addArray(MemArrayId, MemArrayStr, nrOfBits, ramEntries)
          .addWire("s_memContents", MemArrayId);
      myWires
          .addWire("s_we", 1)
          .addWire("s_oe1", 1)
          .addWire("s_oe2", 1);
    }
    myPorts
        .add(Port.INPUT, "address1", nrOfaddressLines, DualPortRamAppearance.getAddrIndex(0, attrs))
        .add(Port.INPUT, "address2", nrOfaddressLines, DualPortRamAppearance.getAddrIndex(1, attrs))
        .add(Port.INPUT, "addressWrite", nrOfaddressLines, DualPortRamAppearance.getAddrIndex(2, attrs))
        .add(Port.INPUT, "dataIn", nrOfBits, DualPortRamAppearance.getDataInIndex(0, attrs))
        .add(Port.INPUT, "we", 1, DualPortRamAppearance.getWEIndex(0, attrs))
        .add(Port.OUTPUT, "dataOut1", nrOfBits, DualPortRamAppearance.getDataOutIndex(0, attrs))
        .add(Port.OUTPUT, "dataOut2", nrOfBits, DualPortRamAppearance.getDataOutIndex(1, attrs));
    if (!async) myPorts.add(Port.CLOCK, HdlPorts.getClockName(1), 1, DualPortRamAppearance.getClkIndex(0, attrs));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("clock", HdlPorts.getClockName(1))
        .pair("tick", HdlPorts.getTickName(1));
    final var be = attrs.getValue(DualPortRamAttributes.ATTR_ByteEnables);
    final var byteEnables = be != null && be.equals(DualPortRamAttributes.BUS_WITH_BYTEENABLES);
    if (Hdl.isVhdl()) {
      //VHDL specific generated lines
      //TODO: Work on VHDL generation
      contents.empty().addVhdlKeywords().addRemarkBlock("The control signals are defined here");
      if (byteEnables) {
        for (var i = 0; i < DualPortRamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("s_byteEnable{{1}} <= s_byteEnableReg({{1}}) {{and}} s_tickDelayLine(2) {{and}} s_oeReg;", i)
              .add("s_we{{1}}         <= s_byteEnableReg({{1}}) {{and}} s_tickDelayLine(0) {{and}} s_weReg;", i);
        }
      } else {
        contents.add("""
            s_oe <= s_tickDelayLine(2) {{and}} s_oeReg;
            s_we <= s_tickDelayLine(0) {{and}} s_weReg;
            """);
      }
      contents
          .empty()
          .addRemarkBlock("The input registers are defined here")
          .add("""
              inputRegs : {{process}}({{clock}}, {{tick}}, address, dataIn, we, oe) {{is}}
              {{begin}}
                 {{if}} (rising_edge({{clock}})) {{then}}
                    {{if}} ({{tick}} = '1') {{then}}
                        s_dataInReg  <= dataIn;
                        s_addressReg <= address;
                        s_weReg      <= we;
                        s_oeReg      <= oe;
              """);
      if (byteEnables) {
        for (var i = 0; i < DualPortRamAppearance.getNrBEPorts(attrs); i++)
          contents.add("         s_byteEnableReg({{1}}) <= byteEnable{{1}}};", i);
      }
      contents
          .add("""
                    {{end}} {{if}};
                 {{end}} {{if}};
              {{end}} {{process}} inputRegs;
              """)
          .empty()
          .add("""
              tickPipeReg : {{process}}({{clock}}) {{is}}
              {{begin}}
                 {{if}} (rising_edge({{clock}})) {{then}}
                     s_tickDelayLine(0)          <= {{tick}};
                     s_tickDelayLine(2 {{downto}} 1) <= s_tickDelayLine(1 {{downto}} 0);
                 {{end}} {{if}};
              {{end}} {{process}} tickPipeReg;
              """)
          .empty()
          .addRemarkBlock("The actual memorie(s) is(are) defined here");
      if (byteEnables) {
        final var truncated = (attrs.getValue(Mem.DATA_ATTR).getWidth() % 8) != 0;
        for (var i = 0; i < DualPortRamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("mem{{1}} : {{process}}({{clock}}, s_we{{1}}, s_dataInReg, s_addressReg) {{is}}", i)
              .add("{{begin}}")
              .add("   {{if}} (rising_edge({{clock}})) {{then}}")
              .add("      {{if}} (s_we{{1}} = '1') {{then}}", i);
          final var startIndex = i * 8;
          final var endIndex =
              (i == (DualPortRamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          final var memName =
              (i == (DualPortRamAppearance.getNrBEPorts(attrs) - 1) && truncated)
                  ? "s_truncMemContents"
                  : String.format("s_byteMem%dContents", i);
          contents
              .add("         {{1}}(to_integer(unsigned(s_addressReg))) <= s_dataInReg({{2}} {{downto}} {{3}});", memName, endIndex, startIndex)
              .add("      {{end}} {{if}};")
              .add("      s_ramdataOut({{1}} {{downto}} {{2}}) <= {{3}}(to_integer(unsigned(s_addressReg)));", endIndex, startIndex, memName)
              .add("   {{end}} {{if}};")
              .add("{{end}} {{process}} mem{{1}};", i)
              .add("");
        }
      } else {
        contents
            .add("""
                mem : {{process}}({{clock}} , s_we, s_dataInReg, s_addressReg) {{is}}
                {{begin}}
                   {{if}} (rising_edge({{clock}})) {{then}}
                      {{if}} (s_we = '1') {{then}}
                         s_memContents(to_integer(unsigned(s_addressReg))) <= s_dataInReg;
                      {{end}} {{if}};
                      s_ramdataOut <= s_memContents(to_integer(unsigned(s_addressReg)));
                   {{end}} {{if}};
                {{end}} {{process}} mem;
                """);
      }
      contents.empty().addRemarkBlock("The output register is defined here");
      if (byteEnables) {
        for (var i = 0; i < DualPortRamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("res{{1}} : {{process}}({{clock}}, s_byteEnable{{1}}, s_ramdataOut) {{is}}", i)
              .add("{{begin}}")
              .add("   {{if}} (rising_edge({{clock}}) {{then}}")
              .add("      {{if}} (s_byteEnable{{1}} = '1') {{then}}", i);
          final var startIndex = i * 8;
          final var endIndex =
              (i == (DualPortRamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          contents
              .add("         dataOut({{1}} {{downto}} {{2}}) <= s_ramdataOut({{1}} {{downto}} {{2}});", endIndex, startIndex)
              .add("      {{end}} {{if}};")
              .add("   {{end}} {{if}};")
              .add("{{end}} {{process}} res{{1}};", i);
        }
      } else {
        contents
            .add("""
                res : {{process}}({{clock}}, s_oe, s_ramdataOut) {{is}}
                {{begin}}
                   {{if}} (rising_edge({{clock}})) {{then}}
                      {{if}} (s_oe = '1') {{then}}
                        dataOut <= s_ramdataOut;
                      {{end}} {{if}};
                   {{end}} {{if}};
                {{end}} {{process}} res;
                """);
      }
    } else {
      //Verilog specific generated lines
      contents
            .add("always @(posedge clock) begin")
            .empty()
            .add("  s_oe1Reg <= oe1;")
            .add("  s_oe2Reg <= oe2;")
            .empty()
            .add("  s_weReg <= we;")
            .empty()
            .add("  s_address1Reg <= address1;")
            .add("  s_address2Reg <= address2;")
            .add("  s_addressWriteReg <= addressWrite;")
            .empty()
            .add("  s_dataInReg <= dataIn;")
            .add("  if (s_weReg == 1'b1) begin")
            .add("    s_memContents[s_addressWriteReg] <= s_dataInReg;")
            .add("  end")
            .empty()
            .add("  if (s_oe1Reg == 1'b1) begin")
            .add("    s_dataOut1 <= s_memContents[s_address1Reg];")
            .add("  end")
            .empty()
            .add("  if (s_oe2Reg == 1'b1) begin")
            .add("    s_dataOut2 <= s_memContents[s_address2Reg];")
            .add("  end")
            .empty()
            .add("end");
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    final var asynch = trigger == null || trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    final var byteEnabled = DualPortRamAppearance.getNrLEPorts(attrs) == 0;
    final var syncRead = !attrs.containsAttribute(Mem.ASYNC_READ) || !attrs.getValue(Mem.ASYNC_READ);
    final var clearPin = attrs.getValue(DualPortRamAttributes.CLEAR_PIN) == null ? false : attrs.getValue(DualPortRamAttributes.CLEAR_PIN);
    final var readAfterWrite = !attrs.containsAttribute(Mem.READ_ATTR) || attrs.getValue(Mem.READ_ATTR).equals(Mem.READAFTERWRITE);
    return !asynch && byteEnabled && syncRead && !clearPin && readAfterWrite;
  }
}
