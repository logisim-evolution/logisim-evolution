/**
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
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class RomHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private String GetBin(int value, int nr_of_bits) {
    StringBuffer Bits = new StringBuffer();
    long mask = (1L << (nr_of_bits - 1));
    int count;
    if (nr_of_bits == 1) Bits.append("'");
    else Bits.append("\"");
    for (count = 0; count < nr_of_bits; count++) {
      if ((value & mask) != 0) Bits.append("1");
      else Bits.append("0");
      mask >>= 1;
    }
    if (nr_of_bits == 1) Bits.append("'");
    else Bits.append("\"");
    return Bits.toString();
  }

  private String GetBin(long value, int nr_of_bits) {
    StringBuffer Bits = new StringBuffer();
    long mask = (1L << (nr_of_bits - 1));
    int count;
    if (nr_of_bits == 1) Bits.append("'");
    else Bits.append("\"");
    for (count = 0; count < nr_of_bits; count++) {
      if ((value & mask) != 0) Bits.append("1");
      else Bits.append("0");
      mask >>= 1;
    }
    if (nr_of_bits == 1) Bits.append("'");
    else Bits.append("\"");
    return Bits.toString();
  }

  @Override
  public String getComponentStringIdentifier() {
    return "ROM";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("Address", attrs.getValue(Mem.ADDR_ATTR).getWidth());
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    long addr;
    MemContents rom = attrs.getValue(Rom.CONTENTS_ATTR);
    if (HDLType.equals(VHDL)) {
      Contents.add("   MakeRom : PROCESS( Address )");
      Contents.add("      BEGIN");
      Contents.add("         CASE (Address) IS");
      for (addr = 0; addr < (1 << attrs.getValue(Mem.ADDR_ATTR).getWidth()); addr++) {
        if (rom.get(addr) != 0) {
          Contents.add(
              "            WHEN "
                  + GetBin(addr, attrs.getValue(Mem.ADDR_ATTR).getWidth())
                  + " => Data <= "
                  + GetBin(rom.get(addr), attrs.getValue(Mem.DATA_ATTR).getWidth())
                  + ";");
        }
      }
      if (attrs.getValue(Mem.DATA_ATTR).getWidth() == 1)
        Contents.add("            WHEN OTHERS => Data <= '0';");
      else Contents.add("            WHEN OTHERS => Data <= (OTHERS => '0');");
      Contents.add("         END CASE;");
      Contents.add("      END PROCESS MakeRom;");
    } else {
      Contents.add(
          "   reg[" + Integer.toString(attrs.getValue(Mem.DATA_ATTR).getWidth() - 1) + ":0] Data;");
      Contents.add("");
      Contents.add("   always @ (Address)");
      Contents.add("   begin");
      Contents.add("      case(Address)");
      for (addr = 0; addr < (1 << attrs.getValue(Mem.ADDR_ATTR).getWidth()); addr++) {
        if (rom.get(addr) != 0) {
          Contents.add("         " + addr + " : Data = " + rom.get(addr) + ";");
        }
      }
      Contents.add("         default : Data = 0;");
      Contents.add("      endcase");
      Contents.add("   end");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("Data", attrs.getValue(Mem.DATA_ATTR).getWidth());
    return Outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
	      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    PortMap.putAll(GetNetMap("Address", true, ComponentInfo, RamAppearance.getAddrIndex(0, attrs), Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Data", true, ComponentInfo, RamAppearance.getDataOutIndex(0, attrs), Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "memory";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return attrs.getValue(Mem.LINE_ATTR).equals(Mem.SINGLE);
  }
}
