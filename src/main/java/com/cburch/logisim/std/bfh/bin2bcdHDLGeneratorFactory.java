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

package com.cburch.logisim.std.bfh;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.util.ContentBuilder;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class bin2bcdHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String nrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "BIN2BCD";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put("BinValue", NrOfBitsId);
    return Inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    BitWidth nrofbits = attrs.getValue(bin2bcd.ATTR_BinBits);
    int NrOfPorts = (int) (Math.log10(1 << nrofbits.getWidth()) + 1.0);
    for (int i = 1; i <= NrOfPorts; i++) {
      Outputs.put("BCD" + (int) (Math.pow(10, i - 1)), 4);
    }
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> ParameterList = new TreeMap<>();
    ParameterList.put(NrOfBitsId, nrOfBitsStr);
    return ParameterList;
  }

  @Override
  public String GetSubDir() {
    return "bfh";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return HDL.isVHDL();
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    int BinBits = ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
    ParameterMap.put(nrOfBitsStr, BinBits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    int BinBits = ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
    int NrOfPorts = (int) (Math.log10(1 << BinBits) + 1.0);
    PortMap.putAll(GetNetMap("BinValue", true, ComponentInfo, 0, Nets));
    for (int i = 1; i <= NrOfPorts; i++)
      PortMap.putAll(GetNetMap("BCD" + (int) (Math.pow(10, i - 1)), true, ComponentInfo, i, Nets));
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    final var nrOfBits = attrs.getValue(bin2bcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    switch (nrOfPorts) {
      case 2:
        wires.put("s_level_0", 7);
        wires.put("s_level_1", 7);
        wires.put("s_level_2", 7);
        wires.put("s_level_3", 7);
        break;
      case 3:
        wires.put("s_level_0", 11);
        wires.put("s_level_1", 11);
        wires.put("s_level_2", 11);
        wires.put("s_level_3", 11);
        wires.put("s_level_4", 11);
        wires.put("s_level_5", 11);
        wires.put("s_level_6", 11);
        break;
      case 4:
        wires.put("s_level_0", 16);
        wires.put("s_level_1", 16);
        wires.put("s_level_2", 16);
        wires.put("s_level_3", 16);
        wires.put("s_level_4", 16);
        wires.put("s_level_5", 16);
        wires.put("s_level_6", 16);
        wires.put("s_level_7", 16);
        wires.put("s_level_8", 16);
        wires.put("s_level_9", 16);
        wires.put("s_level_10", 16);
        break;
    }
    return wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents = new ContentBuilder();
    final var nrOfBits = attrs.getValue(bin2bcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    if (HDL.isVHDL()) {
      switch (nrOfPorts) {
        case 2:
          contents.add("   s_level_0(6 DOWNTO %s) <= (OTHERS => '0');", nrOfBitsStr);
          contents.add("   s_level_0(%s-1 DOWNTO 0) <= BinValue;", nrOfBitsStr);
          contents.add("   s_level_1(2 DOWNTO 0) <= s_level_0(2 DOWNTO 0);");
          contents.add("   s_level_2(1 DOWNTO 0) <= s_level_1(1 DOWNTO 0);");
          contents.add("   s_level_2(6)          <= s_level_1(6);");
          contents.add("   s_level_3(6 DOWNTO 5) <= s_level_2(6 DOWNTO 5);");
          contents.add("   s_level_3(0)          <= s_level_2(0);");
          contents.add("   ");
          contents.add("   BCD1  <= s_level_3( 3 DOWNTO 0);");
          contents.add("   BCD10 <= \"0\"&s_level_3(6 DOWNTO 4);");
          contents.add(getAdd3Block("s_level_0", 6, "s_level_1", 6, "C1"));
          contents.add(getAdd3Block("s_level_1", 5, "s_level_2", 5, "C2"));
          contents.add(getAdd3Block("s_level_2", 4, "s_level_3", 4, "C3"));
          break;
        case 3:
          contents.add("   s_level_0(10 DOWNTO %s) <= (OTHERS => '0');", nrOfBitsStr);
          contents.add("   s_level_0(%s-1 DOWNTO 0) <= BinValue;", nrOfBitsStr);
          contents.add("   s_level_1(10)          <= s_level_0(10);");
          contents.add("   s_level_1( 5 DOWNTO 0) <= s_level_0( 5 DOWNTO 0);");
          contents.add("   s_level_2(10 DOWNTO 9) <= s_level_1(10 DOWNTO 9);");
          contents.add("   s_level_2( 4 DOWNTO 0) <= s_level_1( 4 DOWNTO 0);");
          contents.add("   s_level_3(10 DOWNTO 8) <= s_level_2(10 DOWNTO 8);");
          contents.add("   s_level_3( 3 DOWNTO 0) <= s_level_2( 3 DOWNTO 0);");
          contents.add("   s_level_4( 2 DOWNTO 0) <= s_level_3( 2 DOWNTO 0);");
          contents.add("   s_level_5(10)          <= s_level_4(10);");
          contents.add("   s_level_5( 1 DOWNTO 0) <= s_level_4( 1 DOWNTO 0);");
          contents.add("   s_level_6(10 DOWNTO 9) <= s_level_5(10 DOWNTO 9);");
          contents.add("   s_level_6(0)           <= s_level_5(0);");
          contents.add("   ");
          contents.add("   BCD1   <= s_level_6( 3 DOWNTO 0 );");
          contents.add("   BCD10  <= s_level_6( 7 DOWNTO 4 );");
          contents.add("   BCD100 <= \"0\"&s_level_6(10 DOWNTO 8);");
          contents.add(getAdd3Block("s_level_0", 9, "s_level_1", 9, "C0"));
          contents.add(getAdd3Block("s_level_1", 8, "s_level_2", 8, "C1"));
          contents.add(getAdd3Block("s_level_2", 7, "s_level_3", 7, "C2"));
          contents.add(getAdd3Block("s_level_3", 6, "s_level_4", 6, "C3"));
          contents.add(getAdd3Block("s_level_4", 5, "s_level_5", 5, "C4"));
          contents.add(getAdd3Block("s_level_5", 4, "s_level_6", 4, "C5"));
          contents.add(getAdd3Block("s_level_3", 10, "s_level_4", 10, "C6"));
          contents.add(getAdd3Block("s_level_4", 9, "s_level_5", 9, "C7"));
          contents.add(getAdd3Block("s_level_5", 8, "s_level_6", 8, "C8"));
          break;
        case 4:
          contents.add("   s_level_0(15 DOWNTO %s) <= (OTHERS => '0');", nrOfBitsStr);
          contents.add("   s_level_0(%s-1 DOWNTO 0) <= BinValue;", nrOfBitsStr);
          contents.add("   s_level_1(15 DOWNTO 14)  <= s_level_0(15 DOWNTO 14);");
          contents.add("   s_level_1( 9 DOWNTO  0)  <= s_level_0( 9 DOWNTO  0);");
          contents.add("   s_level_2(15 DOWNTO 13)  <= s_level_1(15 DOWNTO 13);");
          contents.add("   s_level_2( 8 DOWNTO  0)  <= s_level_1( 8 DOWNTO  0);");
          contents.add("   s_level_3(15 DOWNTO 12)  <= s_level_2(15 DOWNTO 12);");
          contents.add("   s_level_3( 7 DOWNTO  0)  <= s_level_2( 7 DOWNTO  0);");
          contents.add("   s_level_4(15)            <= s_level_3(15);");
          contents.add("   s_level_4( 6 DOWNTO  0)  <= s_level_3( 6 DOWNTO  0);");
          contents.add("   s_level_5(15 DOWNTO 14)  <= s_level_4(15 DOWNTO 14);");
          contents.add("   s_level_5( 5 DOWNTO  0)  <= s_level_4( 5 DOWNTO  0);");
          contents.add("   s_level_6(15 DOWNTO 13)  <= s_level_5(15 DOWNTO 13);");
          contents.add("   s_level_6( 4 DOWNTO  0)  <= s_level_5( 4 DOWNTO  0);");
          contents.add("   s_level_7( 3 DOWNTO  0)  <= s_level_6( 3 DOWNTO  0);");
          contents.add("   s_level_8(15)            <= s_level_7(15);");
          contents.add("   s_level_8( 2 DOWNTO  0)  <= s_level_7( 2 DOWNTO  0);");
          contents.add("   s_level_9(15 DOWNTO 14)  <= s_level_8(15 DOWNTO 14);");
          contents.add("   s_level_9( 1 DOWNTO  0)  <= s_level_8( 1 DOWNTO  0);");
          contents.add("   s_level_10(15 DOWNTO 13) <= s_level_9(15 DOWNTO 13);");
          contents.add("   s_level_10(0)            <= s_level_9(0);");
          contents.add("   ");
          contents.add("   BCD1    <= s_level_10( 3 DOWNTO  0 );");
          contents.add("   BCD10   <= s_level_10( 7 DOWNTO  4 );");
          contents.add("   BCD100  <= s_level_10(11 DOWNTO  8);");
          contents.add("   BCD1000 <= s_level_10(15 DOWNTO 12);");
          contents.add(getAdd3Block("s_level_0", 13, "s_level_1", 13, "C0"));
          contents.add(getAdd3Block("s_level_1", 12, "s_level_2", 12, "C1"));
          contents.add(getAdd3Block("s_level_2", 11, "s_level_3", 11, "C2"));
          contents.add(getAdd3Block("s_level_3", 10, "s_level_4", 10, "C3"));
          contents.add(getAdd3Block("s_level_4", 9, "s_level_5", 9, "C4"));
          contents.add(getAdd3Block("s_level_5", 8, "s_level_6", 8, "C5"));
          contents.add(getAdd3Block("s_level_6", 7, "s_level_7", 7, "C6"));
          contents.add(getAdd3Block("s_level_7", 6, "s_level_8", 6, "C7"));
          contents.add(getAdd3Block("s_level_8", 5, "s_level_9", 5, "C8"));
          contents.add(getAdd3Block("s_level_9", 4, "s_level_10", 4, "C9"));
          contents.add(getAdd3Block("s_level_3", 14, "s_level_4", 14, "C10"));
          contents.add(getAdd3Block("s_level_4", 13, "s_level_5", 13, "C11"));
          contents.add(getAdd3Block("s_level_5", 12, "s_level_6", 12, "C12"));
          contents.add(getAdd3Block("s_level_6", 11, "s_level_7", 11, "C13"));
          contents.add(getAdd3Block("s_level_7", 10, "s_level_8", 10, "C14"));
          contents.add(getAdd3Block("s_level_8", 9, "s_level_9", 9, "C15"));
          contents.add(getAdd3Block("s_level_9", 8, "s_level_10", 8, "C16"));
          contents.add(getAdd3Block("s_level_6", 15, "s_level_7", 15, "C17"));
          contents.add(getAdd3Block("s_level_7", 14, "s_level_8", 14, "C18"));
          contents.add(getAdd3Block("s_level_8", 13, "s_level_9", 13, "C19"));
          contents.add(getAdd3Block("s_level_9", 12, "s_level_10", 12, "C20"));
          break;
      }
    } else {
      Reporter.Report.AddFatalError("Strange, this should not happen as Verilog is not yet supported!\n");
    }
    return contents.get();
  }

  private ArrayList<String> getAdd3Block(String srcName, int srcStartId, String destName, int destStartId, String processName) {
    ContentBuilder contents = new ContentBuilder();
    contents.add("   ");
    contents.add("   ADD3_%s : PROCESS(%s)", processName, srcName);
    contents.add("   BEGIN");
    contents.add("      CASE (%s( %d DOWNTO %d ) ) IS", srcName, srcStartId, (srcStartId - 3));
    contents.add("         WHEN \"0000\" => %s( %d DOWNTO %d ) <= \"0000\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"0001\" => %s( %d DOWNTO %d ) <= \"0001\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"0010\" => %s( %d DOWNTO %d ) <= \"0010\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"0011\" => %s( %d DOWNTO %d ) <= \"0011\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"0100\" => %s( %d DOWNTO %d ) <= \"0100\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"0101\" => %s( %d DOWNTO %d ) <= \"1000\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"0110\" => %s( %d DOWNTO %d ) <= \"1001\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"0111\" => %s( %d DOWNTO %d ) <= \"1010\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"1000\" => %s( %d DOWNTO %d ) <= \"1011\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"1001\" => %s( %d DOWNTO %d ) <= \"1100\";", destName, destStartId, (destStartId-3));
    contents.add("         WHEN \"0000\" => %s( %d DOWNTO %d ) <= \"0000\";", destName, destStartId, (destStartId-3));
    contents.add("      END CASE;");
    contents.add("   END PROCESS ADD3_%s;", processName);
    return contents.get();
  }
}
