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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class RamHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String ByteArrayStr = "BYTE_ARRAY";
  private static final int ByteArrayId = -1;
  private static final String RestArrayStr = "REST_ARRAY";
  private static final int RestArrayId = -2;
  private static final String MemArrayStr = "MEMORY_ARRAY";
  private static final int MemArrayId = -3;

  public RamHDLGeneratorFactory() {
    super();
    getWiresPortsduringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    final var byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    final var byteEnableOffset = RamAppearance.getBEIndex(0, attrs);
    final var nrBePorts = RamAppearance.getNrBEPorts(attrs);
    final var nrOfAddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    final var trigger = attrs.getValue(StdAttr.TRIGGER);
    final var async = StdAttr.TRIG_HIGH.equals(trigger) || StdAttr.TRIG_LOW.equals(trigger);
    myWires
        .addWire("s_ram_data_out", nrOfBits)
        .addRegister("s_TickDelayLine", 3)
        .addRegister("s_DataInReg", nrOfBits)
        .addRegister("s_Address_reg", nrOfAddressLines)
        .addRegister("s_WEReg", 1)
        .addRegister("s_OEReg", 1)
        .addRegister("s_DataOutReg", nrOfBits);
    if (byteEnables) {
      myWires
          .addRegister("s_ByteEnableReg", nrBePorts);
      for (var idx = 0; idx < nrBePorts; idx++) {
        myWires
            .addWire(String.format("s_byte_enable_%d", idx), 1)
            .addWire(String.format("s_we_%d", idx), 1);
        myPorts
            .add(Port.INPUT, String.format("ByteEnable%d", idx), 1, byteEnableOffset + nrBePorts - idx - 1);
      }
    } else {
      myWires
          .addWire("s_we", 1)
          .addWire("s_oe", 1);
    }
    myPorts
        .add(Port.INPUT, "Address", nrOfAddressLines, RamAppearance.getAddrIndex(0, attrs))
        .add(Port.INPUT, "DataIn", nrOfBits, RamAppearance.getDataInIndex(0, attrs))
        .add(Port.INPUT, "WE", 1, RamAppearance.getWEIndex(0, attrs))
        .add(Port.INPUT, "OE", 1, RamAppearance.getOEIndex(0, attrs))
        .add(Port.OUTPUT, "DataOut", nrOfBits, RamAppearance.getDataOutIndex(0, attrs));
    if (!async) myPorts.add(Port.CLOCK, HDLPorts.getClockName(1), 1, ByteArrayId);
  }

  @Override
  public SortedMap<String, Integer> GetMemList(AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    if (HDL.isVHDL()) {
      Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
      final var byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
      int nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
      if (byteEnables) {
        final var truncated = (nrOfBits % 8) != 0;
        var nrOfByteEnables = RamAppearance.getNrBEPorts(attrs);
        if (truncated) {
          nrOfByteEnables--;
          map.put("s_trunc_mem_contents", RestArrayId);
        }
        for (int i = 0; i < nrOfByteEnables; i++) {
          map.put("s_byte_mem_" + i + "_contents", ByteArrayId);
        }
      } else {
        map.put("s_mem_contents", MemArrayId);
      }
    }
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = (new LineBuffer())
        .pair("clock", HDLPorts.getClockName(1))
        .pair("tick", HDLPorts.getTickName(1));
    final var be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    final var byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    if (HDL.isVHDL()) {
      contents.addRemarkBlock("Here the control signals are defined");
      if (byteEnables) {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("s_byte_enable_{{1}} <= s_ByteEnableReg({{1}}) AND s_TickDelayLine(2) AND s_OEReg;", i)
              .add("s_we_{{1}}          <= s_ByteEnableReg({{1}}) AND s_TickDelayLine(0) AND s_WEReg;", i);
        }
      } else {
        contents.add("""
            s_oe <= s_TickDelayLine(2) AND s_OEReg;
            s_we <= s_TickDelayLine(0) AND s_WEReg;
            """);
      }
      contents
          .empty()
          .addRemarkBlock("Here the input registers are defined")
          .add("""
              InputRegs : PROCESS ({{clock}}, {{tick}}, Address, DataIn, WE, OE)
              BEGIN
                 IF (rising_edge({{clock}})) THEN
                    IF ({{tick}} = '1') THEN
                        s_DataInReg        <= DataIn;
                        s_Address_reg      <= Address;
                        s_WEReg            <= WE;
                        s_OEReg            <= OE;
              """);
      if (byteEnables) {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++)
          contents.add("         s_ByteEnableReg({{1}}) <= ByteEnable{{1}}};", i);
      }
      contents
          .add("""
                    END IF;
                 END IF;
              END PROCESS InputRegs;
              """)
          .empty()
          .add("""
              TickPipeReg : PROCESS({{clock}})
              BEGIN
                 IF (rising_edge({{clock}})) THEN
                     s_TickDelayLine(0)          <= {{tick}};
                     s_TickDelayLine(2 DOWNTO 1) <= s_TickDelayLine(1 DOWNTO 0);
                 END IF;
              END PROCESS TickPipeReg;
              """)
          .empty()
          .addRemarkBlock("Here the actual memorie(s) is(are) defined");

      if (byteEnables) {
        final var truncated = (attrs.getValue(Mem.DATA_ATTR).getWidth() % 8) != 0;
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("Mem{{1}} : PROCESS({{clock}}, s_we_{{1}}, s_DataInReg, s_Address_reg)", i)
              .add("BEGIN")
              .add("   IF (rising_edge({{clock}})) THEN")
              .add("      IF (s_we_{{1}} = '1') THEN", i);
          final var startIndex = i * 8;
          final var endIndex =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          final var memName =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1) && truncated)
                  ? "s_trunc_mem_contents"
                  : String.format("s_byte_mem_%d_contents", i);
          contents
              .add("         {{1}}(to_integer(unsigned(s_Address_reg))) <= s_DataInReg({{2}} DOWNTO {{3}});", memName, endIndex, startIndex)
              .add("      END IF;")
              .add("      s_ram_data_out({{1}} DOWNTO {{2}}) <= {{3}}(to_integer(unsigned(s_Address_reg)));", endIndex, startIndex, memName)
              .add("   END IF;")
              .add("END PROCESS Mem{{1}};", i)
              .add("");
        }
      } else {
        contents
            .add("""
                Mem : PROCESS( {{clock}} , s_we, s_DataInReg, s_Address_reg)
                BEGIN
                   IF (rising_edge({{clock}})) THEN
                      IF (s_we = '1') THEN
                         s_mem_contents(to_integer(unsigned(s_Address_reg))) <= s_DataInReg;
                      END IF;
                      s_ram_data_out <= s_mem_contents(to_integer(unsigned(s_Address_reg)));
                   END IF;
                END PROCESS Mem;
                """)
            .empty();
      }
      contents.addRemarkBlock("Here the output register is defined");
      if (byteEnables) {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("Res{{1}} : PROCESS({{clock}}, s_byte_enable_{{1}}, s_ram_data_out)", i)
              .add("BEGIN")
              .add("   IF (rising_edge({{clock}}) THEN")
              .add("      IF (s_byte_enable_{{1}} = '1') THEN", i);
          final var startIndex = i * 8;
          final var endIndex =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          contents
              .add("         DataOut({{1}} DOWNTO {{2}}) <= s_ram_data_out({{1}} DOWNTO {{2}});", endIndex, startIndex)
              .add("      END IF;")
              .add("   END IF;")
              .add("END PROCESS Res{{1}};", i)
              .empty();
        }
      } else {
        contents
            .add("""
                Res : PROCESS( {{clock}} , s_oe, s_ram_data_out)
                BEGIN
                   IF (rising_edge({{clock}})) THEN
                      IF (s_oe = '1') THEN
                        DataOut <= s_ram_data_out;
                      END IF;
                   END IF;
                END PROCESS Res;
                
                """);
      }
    }
    return contents.getWithIndent();
  }

  @Override
  public int GetNrOfTypes(Netlist nets, AttributeSet attrs) {
    Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    final var byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    final var nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    return (byteEnables) ? ((nrOfBits % 8) == 0) ? 1 : 2 : 1;
  }

  @Override
  public String GetType(int TypeNr) {
    switch (TypeNr) {
      case MemArrayId:
        return MemArrayStr;
      case ByteArrayId:
        return ByteArrayStr;
      case RestArrayId:
        return RestArrayStr;
    }
    return "";
  }

  @Override
  public SortedSet<String> GetTypeDefinitions(Netlist TheNetlist, AttributeSet attrs) {
    SortedSet<String> myTypes = new TreeSet<>();
    if (HDL.isVHDL()) {
      Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
      final var byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
      final var nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
      final var nrOfAddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
      final var ramEntries = (1 << nrOfAddressLines);
      if (byteEnables) {
        myTypes.add(
            "TYPE "
                + ByteArrayStr
                + " IS ARRAY ("
                + (ramEntries - 1)
                + " DOWNTO 0) OF std_logic_vector(7 DOWNTO 0)");
        if ((nrOfBits % 8) != 0) {
          myTypes.add(
              "TYPE "
                  + RestArrayStr
                  + " IS ARRAY ("
                  + (ramEntries - 1)
                  + " DOWNTO 0) OF std_logic_vector("
                  + ((nrOfBits % 8) - 1)
                  + " DOWNTO 0)");
        }
      } else {
        myTypes.add(
            "TYPE "
                + MemArrayStr
                + " IS ARRAY ("
                + (ramEntries - 1)
                + " DOWNTO 0) OF std_logic_vector("
                + (nrOfBits - 1)
                + " DOWNTO 0)");
      }
    }
    return myTypes;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    Object busVal = attrs.getValue(RamAttributes.ATTR_DBUS);
    final var separate = busVal != null && busVal.equals(RamAttributes.BUS_SEP);
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    final var asynch = trigger == null || trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    final var byteEnabled = RamAppearance.getNrLEPorts(attrs) == 0;
    final var syncRead = !attrs.containsAttribute(Mem.ASYNC_READ) || !attrs.getValue(Mem.ASYNC_READ);
    final var clearPin = attrs.getValue(RamAttributes.CLEAR_PIN) == null ? false : attrs.getValue(RamAttributes.CLEAR_PIN);
    final var readAfterWrite = !attrs.containsAttribute(Mem.READ_ATTR) || attrs.getValue(Mem.READ_ATTR).equals(Mem.READAFTERWRITE);
    return HDL.isVHDL() && separate && !asynch && byteEnabled && syncRead && !clearPin && readAfterWrite;
  }
}
