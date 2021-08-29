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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class RomHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private String getBin(long value, int nrOfBits) {
    final var bits = new StringBuilder();
    var mask = (1L << (nrOfBits - 1));
    int count;
    if (nrOfBits == 1) bits.append("'");
    else bits.append("\"");
    for (count = 0; count < nrOfBits; count++) {
      if ((value & mask) != 0) bits.append("1");
      else bits.append("0");
      mask >>= 1;
    }
    if (nrOfBits == 1) bits.append("'");
    else bits.append("\"");
    return bits.toString();
  }

  @Override
  public String getComponentStringIdentifier() {
    return "ROM";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Address", attrs.getValue(Mem.ADDR_ATTR).getWidth());
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new LineBuffer();
    long addr;
    final var rom = attrs.getValue(Rom.CONTENTS_ATTR);
    if (HDL.isVHDL()) {
      contents.addLines(
              "MakeRom : PROCESS( Address )",
              "   BEGIN",
              "      CASE (Address) IS");
      for (addr = 0; addr < (1 << attrs.getValue(Mem.ADDR_ATTR).getWidth()); addr++) {
        if (rom.get(addr) != 0) {
          contents.add(
              "         WHEN {{1}} => Data <= {{2}};",
              getBin(addr, attrs.getValue(Mem.ADDR_ATTR).getWidth()),
              getBin(rom.get(addr), attrs.getValue(Mem.DATA_ATTR).getWidth()));
        }
      }
      if (attrs.getValue(Mem.DATA_ATTR).getWidth() == 1)
        contents.add("         WHEN OTHERS => Data <= '0';");
      else
        contents.add("         WHEN OTHERS => Data <= (OTHERS => '0');");
      contents.add("      END CASE;");
      contents.add("   END PROCESS MakeRom;");
    } else {
      contents
          .add("reg[{{1}}:0] Data;", attrs.getValue(Mem.DATA_ATTR).getWidth() - 1)
          .add("")
          .add("always @ (Address)")
          .add("begin")
          .add("   case(Address)");
      for (addr = 0; addr < (1 << attrs.getValue(Mem.ADDR_ATTR).getWidth()); addr++) {
        if (rom.get(addr) != 0) {
          contents.add("      {{1}} : Data = {{2}};", addr, rom.get(addr));
        }
      }
      contents.addLines(
          "      default : Data = 0;",
          "   endcase",
          "end");
    }
    return contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Data", attrs.getValue(Mem.DATA_ATTR).getWidth());
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    final var attrs = comp.getComponent().getAttributeSet();
    map.putAll(GetNetMap("Address", true, comp, RamAppearance.getAddrIndex(0, attrs), nets));
    map.putAll(GetNetMap("Data", true, comp, RamAppearance.getDataOutIndex(0, attrs), nets));
    return map;
  }

  @Override
  public String GetSubDir() {
    return "memory";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    if (attrs == null) return false;
    if (attrs.getValue(Mem.LINE_ATTR) == null) return false;
    return attrs.getValue(Mem.LINE_ATTR).equals(Mem.SINGLE);
  }
}
