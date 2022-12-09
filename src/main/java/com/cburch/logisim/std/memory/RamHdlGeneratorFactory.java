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

public class RamHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String ByteArrayStr = "byteArray";
  private static final int ByteArrayId = -1;
  private static final String RestArrayStr = "restArray";
  private static final int RestArrayId = -2;
  private static final String MemArrayStr = "memoryArray";
  private static final int MemArrayId = -3;

  public RamHdlGeneratorFactory() {
    super();
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    final var byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    final var byteEnableOffset = RamAppearance.getBEIndex(0, attrs);
    final var nrBePorts = RamAppearance.getNrBEPorts(attrs);
    final var nrOfaddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    final var trigger = attrs.getValue(StdAttr.TRIGGER);
    final var async = StdAttr.TRIG_HIGH.equals(trigger) || StdAttr.TRIG_LOW.equals(trigger);
    final var ramEntries = (1 << nrOfaddressLines);
    final var truncated = (nrOfBits % 8) != 0;
    if (byteEnables) {
      for (var idx = 0; idx < nrBePorts; idx++) {
        myPorts
            .add(Port.INPUT, String.format("byteEnable%d", idx), 1, byteEnableOffset + nrBePorts - idx - 1);
      }
      myPorts.add(Port.INPUT, "oe", 1, RamAppearance.getOEIndex(0, attrs));
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
      myPorts.add(Port.INPUT, "oe", 1, RamAppearance.getOEIndex(0, attrs));
      myTypedWires
          .addArray(MemArrayId, MemArrayStr, nrOfBits, ramEntries)
          .addWire("s_memContents", MemArrayId);
    }
    myPorts
        .add(Port.INPUT, "address", nrOfaddressLines, RamAppearance.getAddrIndex(0, attrs))
        .add(Port.INPUT, "dataIn", nrOfBits, RamAppearance.getDataInIndex(0, attrs))
        .add(Port.INPUT, "we", 1, RamAppearance.getWEIndex(0, attrs))
        .add(Port.OUTPUT, "dataOut", nrOfBits, RamAppearance.getDataOutIndex(0, attrs));
    if (!async) myPorts.add(Port.CLOCK, HdlPorts.getClockName(1), 1, RamAppearance.getClkIndex(0, attrs));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("clock", HdlPorts.getClockName(1))
        .pair("tick", HdlPorts.getTickName(1));
    final var be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    final var byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().addRemarkBlock("The control signals are defined here");
      contents
          .addRemarkBlock("The actual memorie(s) is(are) defined here");
      if (byteEnables) {
        final var truncated = (attrs.getValue(Mem.DATA_ATTR).getWidth() % 8) != 0;
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("mem{{1}} : {{process}}({{clock}}, oe, we, byteEnable{{1}}, dataIn, address) {{is}}", i)
              .add("{{begin}}")
              .add("   {{if}} (rising_edge({{clock}})) {{then}}")
              .add("      {{if}} (byteEnable{{1}} = '1') {{then}}", i)
              .add("         {{if}} (we = '1') {{then}}", i);
          final var startIndex = i * 8;
          final var endIndex =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          final var memName =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1) && truncated)
                  ? "s_truncMemContents"
                  : String.format("s_byteMem%dContents", i);
          contents
              .add("            {{1}}(to_integer(unsigned(address))) <= dataIn({{2}} {{downto}} {{3}});", memName, endIndex, startIndex)
              .add("         {{end}} {{if}};")
              .add("         {{if}} (oe = '1') {{then}}", i)
              .add("            dataOut({{1}} {{downto}} {{2}}) <= {{3}}(to_integer(unsigned(address)));", endIndex, startIndex, memName)
              .add("         {{end}} {{if}};")
              .add("      {{end}} {{if}};")
              .add("   {{end}} {{if}};")
              .add("{{end}} {{process}} mem{{1}};", i)
              .add("");
        }
      } else {
        contents
            .add("""
                mem : {{process}}({{clock}} , oe, we, dataIn, address) {{is}}
                {{begin}}
                   {{if}} (rising_edge({{clock}})) {{then}}
                      {{if}} (we = '1') {{then}}
                         s_memContents(to_integer(unsigned(address))) <= dataIn;
                      {{end}} {{if}};
                      {{if}} (oe = '1') {{then}}
                        dataOut <= s_memContents(to_integer(unsigned(address)));
                      {{end}} {{if}};
                   {{end}} {{if}};
                {{end}} {{process}} mem;
                """);
      }
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    Object busVal = attrs.getValue(RamAttributes.ATTR_DBUS);
    final var separate = busVal != null && busVal.equals(RamAttributes.BUS_SEP);
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    final var asynch = trigger == null || trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    final var byteEnabled = RamAppearance.getNrLEPorts(attrs) == 0;
    final var syncRead = !attrs.containsAttribute(Mem.ASYNC_READ) || !attrs.getValue(Mem.ASYNC_READ);
    final var clearPin = attrs.getValue(RamAttributes.CLEAR_PIN) == null ? false : attrs.getValue(RamAttributes.CLEAR_PIN);
    final var readAfterWrite = !attrs.containsAttribute(Mem.READ_ATTR) || attrs.getValue(Mem.READ_ATTR).equals(Mem.READAFTERWRITE);
    return Hdl.isVhdl() && separate && !asynch && byteEnabled && syncRead && !clearPin && readAfterWrite;
  }
}
