/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
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

  @Override
  public String getComponentStringIdentifier() {
    return "RAM";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    int NrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    Inputs.put("Address", attrs.getValue(Mem.ADDR_ATTR).getWidth());
    Inputs.put("DataIn", NrOfBits);
    Inputs.put("WE", 1);
    Inputs.put("OE", 1);
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    boolean asynch = trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    if (!asynch) {
      Inputs.put("Clock", 1);
      Inputs.put("Tick", 1);
    }
    Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    boolean byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    if (byteEnables) {
      int NrOfByteEnables = RamAppearance.getNrBEPorts(attrs);
      for (int i = 0; i < NrOfByteEnables; i++) {
        Inputs.put("ByteEnable" + i, 1);
      }
    }
    return Inputs;
  }

  @Override
  public SortedMap<String, Integer> GetMemList(AttributeSet attrs) {
    SortedMap<String, Integer> Mems = new TreeMap<>();
    if (HDL.isVHDL()) {
      Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
      boolean byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
      int NrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
      if (byteEnables) {
        boolean truncated = (NrOfBits % 8) != 0;
        int NrOfByteEnables = RamAppearance.getNrBEPorts(attrs);
        if (truncated) {
          NrOfByteEnables--;
          Mems.put("s_trunc_mem_contents", RestArrayId);
        }
        for (int i = 0; i < NrOfByteEnables; i++) {
          Mems.put("s_byte_mem_" + i + "_contents", ByteArrayId);
        }
      } else {
        Mems.put("s_mem_contents", MemArrayId);
      }
    }
    return Mems;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    boolean byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    if (HDL.isVHDL()) {
      Contents.addAll(MakeRemarkBlock("Here the control signals are defined", 3));
      if (byteEnables) {
        for (int i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          Contents.add(
              "   s_byte_enable_"
                  + i
                  + " <= s_ByteEnableReg("
                  + i
                  + ") AND s_TickDelayLine(2) AND s_OEReg;");
          Contents.add(
              "   s_we_"
                  + i
                  + "          <= s_ByteEnableReg("
                  + i
                  + ") AND s_TickDelayLine(0) AND s_WEReg;");
        }
      } else {
        Contents.add("   s_oe <= s_TickDelayLine(2) AND s_OEReg;");
        Contents.add("   s_we <= s_TickDelayLine(0) AND s_WEReg;");
      }
      Contents.add("");
      Contents.addAll(MakeRemarkBlock("Here the input registers are defined", 3));
      Contents.add("   InputRegs : PROCESS (Clock , Tick , Address , DataIn , WE , OE )");
      Contents.add("   BEGIN");
      Contents.add("      IF (Clock'event AND (Clock = '1')) THEN");
      Contents.add("         IF (Tick = '1') THEN");
      Contents.add("             s_DataInReg        <= DataIn;");
      Contents.add("             s_Address_reg      <= Address;");
      Contents.add("             s_WEReg            <= WE;");
      Contents.add("             s_OEReg            <= OE;");
      if (byteEnables) {
        for (int i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          Contents.add(
              "             s_ByteEnableReg("
                  + i
                  + ") <= ByteEnable"
                  + i
                  + ";");
        }
      }
      Contents.add("         END IF;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS InputRegs;");
      Contents.add("");
      Contents.add("   TickPipeReg : PROCESS(Clock)");
      Contents.add("   BEGIN");
      Contents.add("      IF (Clock'event AND (Clock = '1')) THEN");
      Contents.add("          s_TickDelayLine(0)          <= Tick;");
      Contents.add("          s_TickDelayLine(2 DOWNTO 1) <= s_TickDelayLine(1 DOWNTO 0);");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS TickPipeReg;");
      Contents.add("");
      Contents.addAll(MakeRemarkBlock("Here the actual memorie(s) is(are) defined", 3));
      if (byteEnables) {
        boolean truncated = (attrs.getValue(Mem.DATA_ATTR).getWidth() % 8) != 0;
        for (int i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          Contents.add(
              "   Mem"
                  + i
                  + " : PROCESS( Clock , s_we_"
                  + i
                  + ", s_DataInReg, s_Address_reg)");
          Contents.add("   BEGIN");
          Contents.add("      IF (Clock'event AND (Clock = '1')) THEN");
          Contents.add("            IF (s_we_" + i + " = '1') THEN");
          int startIndex = i * 8;
          int endIndex =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          String Memname =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1) && truncated)
                  ? "s_trunc_mem_contents"
                  : "s_byte_mem_" + i + "_contents";
          Contents.add(
              "               "
                  + Memname
                  + "(to_integer(unsigned(s_Address_reg))) <= s_DataInReg("
                  + endIndex
                  + " DOWNTO "
                  + startIndex
                  + ");");
          Contents.add("            END IF;");
          Contents.add(
              "            s_ram_data_out("
                  + endIndex
                  + " DOWNTO "
                  + startIndex
                  + ") <= "
                  + Memname
                  + "(to_integer(unsigned(s_Address_reg)));");
          Contents.add("      END IF;");
          Contents.add("   END PROCESS Mem" + i + ";");
          Contents.add("");
        }
      } else {
        Contents.add("   Mem : PROCESS( Clock , s_we, s_DataInReg, s_Address_reg)");
        Contents.add("   BEGIN");
        Contents.add("      IF (Clock'event AND (Clock = '1')) THEN");
        Contents.add("            IF (s_we = '1') THEN");
        Contents.add(
            "               s_mem_contents(to_integer(unsigned(s_Address_reg))) <= s_DataInReg;");
        Contents.add("            END IF;");
        Contents.add(
            "            s_ram_data_out <= s_mem_contents(to_integer(unsigned(s_Address_reg)));");
        Contents.add("      END IF;");
        Contents.add("   END PROCESS Mem;");
        Contents.add("");
      }
      Contents.addAll(MakeRemarkBlock("Here the output register is defined", 3));
      if (byteEnables) {
        for (int i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          Contents.add(
              "   Res"
                  + i
                  + " : PROCESS( Clock , s_byte_enable_"
                  + i
                  + ", s_ram_data_out)");
          Contents.add("   BEGIN");
          Contents.add("      IF (Clock'event AND (Clock = '1')) THEN");
          Contents.add("         IF (s_byte_enable_" + i + " = '1') THEN");
          int startIndex = i * 8;
          int endIndex =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          Contents.add(
              "           DataOut("
                  + endIndex
                  + " DOWNTO "
                  + startIndex
                  + ") <= s_ram_data_out("
                  + endIndex
                  + " DOWNTO "
                  + startIndex
                  + ");");
          Contents.add("         END IF;");
          Contents.add("      END IF;");
          Contents.add("   END PROCESS Res" + i + ";");
          Contents.add("");
        }
      } else {
        Contents.add("   Res : PROCESS( Clock , s_oe, s_ram_data_out)");
        Contents.add("   BEGIN");
        Contents.add("      IF (Clock'event AND (Clock = '1')) THEN");
        Contents.add("         IF (s_oe = '1') THEN");
        Contents.add("           DataOut <= s_ram_data_out;");
        Contents.add("         END IF;");
        Contents.add("      END IF;");
        Contents.add("   END PROCESS Res;");
        Contents.add("");
      }
    }
    return Contents;
  }

  @Override
  public int GetNrOfTypes(Netlist TheNetlist, AttributeSet attrs) {
    Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    boolean byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    int NrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    return (byteEnables) ? ((NrOfBits % 8) == 0) ? 1 : 2 : 1;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    Outputs.put("DataOut", attrs.getValue(Mem.DATA_ATTR).getWidth());
    return Outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    boolean asynch = trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    boolean byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    PortMap.putAll(GetNetMap("Address", true, ComponentInfo, RamAppearance.getAddrIndex(0, attrs), Nets));
    int DinPin = RamAppearance.getDataInIndex(0, attrs);
    PortMap.putAll(GetNetMap("DataIn", true, ComponentInfo, DinPin, Nets));
    PortMap.putAll(GetNetMap("WE", true, ComponentInfo, RamAppearance.getWEIndex(0, attrs), Nets));
    PortMap.putAll(GetNetMap("OE", true, ComponentInfo, RamAppearance.getOEIndex(0, attrs), Nets));
    if (!asynch) {
      if (!ComponentInfo.EndIsConnected(RamAppearance.getClkIndex(0, attrs))) {
        Reporter.Report.AddError(
            "Component \"RAM\" in circuit \""
                + Nets.getCircuitName()
                + "\" has no clock connection!");
        PortMap.put("Clock", HDL.zeroBit());
        PortMap.put("Tick", HDL.zeroBit());
      } else {
        String ClockNetName = GetClockNetName(ComponentInfo, RamAppearance.getClkIndex(0, attrs), Nets);
        if (ClockNetName.isEmpty()) {
          PortMap.putAll(GetNetMap("Clock", true, ComponentInfo, RamAppearance.getClkIndex(0, attrs), Nets));
          PortMap.put("Tick", HDL.oneBit());
        } else {
          int ClockBusIndex;
          if (Nets.RequiresGlobalClockConnection()) {
            ClockBusIndex = ClockHDLGeneratorFactory.GlobalClockIndex;
          } else {
            ClockBusIndex =
                (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING)
                    ? ClockHDLGeneratorFactory.PositiveEdgeTickIndex
                    : ClockHDLGeneratorFactory.NegativeEdgeTickIndex;
          }

          PortMap.put(
              "Clock",
              ClockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.GlobalClockIndex
                  + HDL.BracketClose());
          PortMap.put("Tick", ClockNetName + HDL.BracketOpen() + ClockBusIndex + HDL.BracketClose());
        }
      }
    }
    if (byteEnables) {
      int NrOfByteEnables = RamAppearance.getNrBEPorts(ComponentInfo.GetComponent().getAttributeSet());
      int ByteEnableOffset = RamAppearance.getBEIndex(0, ComponentInfo.GetComponent().getAttributeSet());
      for (int i = 0; i < NrOfByteEnables; i++) {
        PortMap.putAll(
            GetNetMap(
                "ByteEnable" + i,
                false,
                ComponentInfo,
                ByteEnableOffset + NrOfByteEnables - i - 1,
                Nets));
      }
    }
    PortMap.putAll(GetNetMap("DataOut", true, ComponentInfo, RamAppearance.getDataOutIndex(0, attrs), Nets));
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    SortedMap<String, Integer> Regs = new TreeMap<>();
    Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    boolean byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    int NrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    int NrOfAddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    Regs.put("s_TickDelayLine", 3);
    Regs.put("s_DataInReg", NrOfBits);
    Regs.put("s_Address_reg", NrOfAddressLines);
    Regs.put("s_WEReg", 1);
    Regs.put("s_OEReg", 1);
    Regs.put("s_DataOutReg", NrOfBits);
    if (byteEnables) {
      int NrOfByteEnables = RamAppearance.getNrBEPorts(attrs);
      Regs.put("s_ByteEnableReg", NrOfByteEnables);
    }
    return Regs;
  }

  @Override
  public String GetSubDir() {
    return "memory";
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
    SortedSet<String> MyTypes = new TreeSet<>();
    if (HDL.isVHDL()) {
      Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
      boolean byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
      int NrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
      int NrOfAddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
      int RamEntries = (1 << NrOfAddressLines);
      if (byteEnables) {
        MyTypes.add(
            "TYPE "
                + ByteArrayStr
                + " IS ARRAY ("
                + (RamEntries - 1)
                + " DOWNTO 0) OF std_logic_vector(7 DOWNTO 0)");
        if ((NrOfBits % 8) != 0) {
          MyTypes.add(
              "TYPE "
                  + RestArrayStr
                  + " IS ARRAY ("
                  + (RamEntries - 1)
                  + " DOWNTO 0) OF std_logic_vector("
                  + ((NrOfBits % 8) - 1)
                  + " DOWNTO 0)");
        }
      } else {
        MyTypes.add(
            "TYPE "
                + MemArrayStr
                + " IS ARRAY ("
                + (RamEntries - 1)
                + " DOWNTO 0) OF std_logic_vector("
                + (NrOfBits - 1)
                + " DOWNTO 0)");
      }
    }
    return MyTypes;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    int NrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    boolean byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    Wires.put("s_ram_data_out", NrOfBits);
    if (byteEnables) {
      for (int i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
        Wires.put("s_byte_enable_" + i, 1);
        Wires.put("s_we_" + i, 1);
      }
    } else {
      Wires.put("s_we", 1);
      Wires.put("s_oe", 1);
    }
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    if (attrs == null) return false;
    Object busVal = attrs.getValue(RamAttributes.ATTR_DBUS);
    boolean separate = busVal != null && busVal.equals(RamAttributes.BUS_SEP);
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    boolean asynch = trigger == null || trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    boolean byteEnabled = RamAppearance.getNrLEPorts(attrs) == 0;
    boolean syncRead = !attrs.containsAttribute(Mem.ASYNC_READ) || !attrs.getValue(Mem.ASYNC_READ);
    boolean clearPin = attrs.getValue(RamAttributes.CLEAR_PIN) == null ? false : attrs.getValue(RamAttributes.CLEAR_PIN);
    boolean ReadAfterWrite = !attrs.containsAttribute(Mem.READ_ATTR) || attrs.getValue(Mem.READ_ATTR).equals(Mem.READAFTERWRITE);
    return HDL.isVHDL() && separate && !asynch && byteEnabled && syncRead && !clearPin && ReadAfterWrite;
  }
}
