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

package com.cburch.logisim.fpga.library;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class bin2bcdHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "BIN2BCD";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("BinValue", NrOfBitsId);
    return Inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    BitWidth nrofbits = attrs.getValue(bin2bcd.ATTR_BinBits);
    int NrOfPorts = (int) (Math.log10(1 << nrofbits.getWidth()) + 1.0);
    for (int i = 1; i <= NrOfPorts; i++) {
      Outputs.put("BCD" + Integer.toString((int) (Math.pow(10, i - 1))), 4);
    }
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> ParameterList = new TreeMap<Integer, String>();
    ParameterList.put(NrOfBitsId, NrOfBitsStr);
    return ParameterList;
  }

  @Override
  public String GetSubDir() {
    return "bfh";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return HDLType.equals(VHDL);
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    int BinBits = ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
    ParameterMap.put(NrOfBitsStr, BinBits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
	      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
	if (!(MapInfo instanceof NetlistComponent)) return PortMap;
	NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    int BinBits = ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
    int NrOfPorts = (int) (Math.log10(1 << BinBits) + 1.0);
    PortMap.putAll(GetNetMap("BinValue", true, ComponentInfo, 0, Reporter, HDLType, Nets));
    for (int i = 1; i <= NrOfPorts; i++)
      PortMap.putAll(
          GetNetMap(
              "BCD" + Integer.toString((int) (Math.pow(10, i - 1))),
              true,
              ComponentInfo,
              i,
              Reporter,
              HDLType,
              Nets));
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    BitWidth nrofbits = attrs.getValue(bin2bcd.ATTR_BinBits);
    int NrOfPorts = (int) (Math.log10(1 << nrofbits.getWidth()) + 1.0);
    switch (NrOfPorts) {
      case 2:
        Wires.put("s_level_0", 7);
        Wires.put("s_level_1", 7);
        Wires.put("s_level_2", 7);
        Wires.put("s_level_3", 7);
        break;
      case 3:
        Wires.put("s_level_0", 11);
        Wires.put("s_level_1", 11);
        Wires.put("s_level_2", 11);
        Wires.put("s_level_3", 11);
        Wires.put("s_level_4", 11);
        Wires.put("s_level_5", 11);
        Wires.put("s_level_6", 11);
        break;
      case 4:
        Wires.put("s_level_0", 16);
        Wires.put("s_level_1", 16);
        Wires.put("s_level_2", 16);
        Wires.put("s_level_3", 16);
        Wires.put("s_level_4", 16);
        Wires.put("s_level_5", 16);
        Wires.put("s_level_6", 16);
        Wires.put("s_level_7", 16);
        Wires.put("s_level_8", 16);
        Wires.put("s_level_9", 16);
        Wires.put("s_level_10", 16);
        break;
    }
    return Wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    BitWidth nrofbits = attrs.getValue(bin2bcd.ATTR_BinBits);
    int NrOfPorts = (int) (Math.log10(1 << nrofbits.getWidth()) + 1.0);
    if (HDLType.equals(VHDL)) {
      switch (NrOfPorts) {
        case 2:
          Contents.add("   s_level_0(6 DOWNTO " + NrOfBitsStr + ") <= (OTHERS => '0');");
          Contents.add("   s_level_0(" + NrOfBitsStr + "-1 DOWNTO 0) <= BinValue;");
          Contents.add("   s_level_1(2 DOWNTO 0) <= s_level_0(2 DOWNTO 0);");
          Contents.add("   s_level_2(1 DOWNTO 0) <= s_level_1(1 DOWNTO 0);");
          Contents.add("   s_level_2(6)          <= s_level_1(6);");
          Contents.add("   s_level_3(6 DOWNTO 5) <= s_level_2(6 DOWNTO 5);");
          Contents.add("   s_level_3(0)          <= s_level_2(0);");
          Contents.add("   ");
          Contents.add("   BCD1  <= s_level_3( 3 DOWNTO 0);");
          Contents.add("   BCD10 <= \"0\"&s_level_3(6 DOWNTO 4);");
          Contents.addAll(GetAdd3Block("s_level_0", 6, "s_level_1", 6, "C1"));
          Contents.addAll(GetAdd3Block("s_level_1", 5, "s_level_2", 5, "C2"));
          Contents.addAll(GetAdd3Block("s_level_2", 4, "s_level_3", 4, "C3"));
          break;
        case 3:
          Contents.add("   s_level_0(10 DOWNTO " + NrOfBitsStr + ") <= (OTHERS => '0');");
          Contents.add("   s_level_0(" + NrOfBitsStr + "-1 DOWNTO 0) <= BinValue;");
          Contents.add("   s_level_1(10)          <= s_level_0(10);");
          Contents.add("   s_level_1( 5 DOWNTO 0) <= s_level_0( 5 DOWNTO 0);");
          Contents.add("   s_level_2(10 DOWNTO 9) <= s_level_1(10 DOWNTO 9);");
          Contents.add("   s_level_2( 4 DOWNTO 0) <= s_level_1( 4 DOWNTO 0);");
          Contents.add("   s_level_3(10 DOWNTO 8) <= s_level_2(10 DOWNTO 8);");
          Contents.add("   s_level_3( 3 DOWNTO 0) <= s_level_2( 3 DOWNTO 0);");
          Contents.add("   s_level_4( 2 DOWNTO 0) <= s_level_3( 2 DOWNTO 0);");
          Contents.add("   s_level_5(10)          <= s_level_4(10);");
          Contents.add("   s_level_5( 1 DOWNTO 0) <= s_level_4( 1 DOWNTO 0);");
          Contents.add("   s_level_6(10 DOWNTO 9) <= s_level_5(10 DOWNTO 9);");
          Contents.add("   s_level_6(0)           <= s_level_5(0);");
          Contents.add("   ");
          Contents.add("   BCD1   <= s_level_6( 3 DOWNTO 0 );");
          Contents.add("   BCD10  <= s_level_6( 7 DOWNTO 4 );");
          Contents.add("   BCD100 <= \"0\"&s_level_6(10 DOWNTO 8);");
          Contents.addAll(GetAdd3Block("s_level_0", 9, "s_level_1", 9, "C0"));
          Contents.addAll(GetAdd3Block("s_level_1", 8, "s_level_2", 8, "C1"));
          Contents.addAll(GetAdd3Block("s_level_2", 7, "s_level_3", 7, "C2"));
          Contents.addAll(GetAdd3Block("s_level_3", 6, "s_level_4", 6, "C3"));
          Contents.addAll(GetAdd3Block("s_level_4", 5, "s_level_5", 5, "C4"));
          Contents.addAll(GetAdd3Block("s_level_5", 4, "s_level_6", 4, "C5"));
          Contents.addAll(GetAdd3Block("s_level_3", 10, "s_level_4", 10, "C6"));
          Contents.addAll(GetAdd3Block("s_level_4", 9, "s_level_5", 9, "C7"));
          Contents.addAll(GetAdd3Block("s_level_5", 8, "s_level_6", 8, "C8"));
          break;
        case 4:
          Contents.add("   s_level_0(15 DOWNTO " + NrOfBitsStr + ") <= (OTHERS => '0');");
          Contents.add("   s_level_0(" + NrOfBitsStr + "-1 DOWNTO 0) <= BinValue;");
          Contents.add("   s_level_1(15 DOWNTO 14)  <= s_level_0(15 DOWNTO 14);");
          Contents.add("   s_level_1( 9 DOWNTO  0)  <= s_level_0( 9 DOWNTO  0);");
          Contents.add("   s_level_2(15 DOWNTO 13)  <= s_level_1(15 DOWNTO 13);");
          Contents.add("   s_level_2( 8 DOWNTO  0)  <= s_level_1( 8 DOWNTO  0);");
          Contents.add("   s_level_3(15 DOWNTO 12)  <= s_level_2(15 DOWNTO 12);");
          Contents.add("   s_level_3( 7 DOWNTO  0)  <= s_level_2( 7 DOWNTO  0);");
          Contents.add("   s_level_4(15)            <= s_level_3(15);");
          Contents.add("   s_level_4( 6 DOWNTO  0)  <= s_level_3( 6 DOWNTO  0);");
          Contents.add("   s_level_5(15 DOWNTO 14)  <= s_level_4(15 DOWNTO 14);");
          Contents.add("   s_level_5( 5 DOWNTO  0)  <= s_level_4( 5 DOWNTO  0);");
          Contents.add("   s_level_6(15 DOWNTO 13)  <= s_level_5(15 DOWNTO 13);");
          Contents.add("   s_level_6( 4 DOWNTO  0)  <= s_level_5( 4 DOWNTO  0);");
          Contents.add("   s_level_7( 3 DOWNTO  0)  <= s_level_6( 3 DOWNTO  0);");
          Contents.add("   s_level_8(15)            <= s_level_7(15);");
          Contents.add("   s_level_8( 2 DOWNTO  0)  <= s_level_7( 2 DOWNTO  0);");
          Contents.add("   s_level_9(15 DOWNTO 14)  <= s_level_8(15 DOWNTO 14);");
          Contents.add("   s_level_9( 1 DOWNTO  0)  <= s_level_8( 1 DOWNTO  0);");
          Contents.add("   s_level_10(15 DOWNTO 13) <= s_level_9(15 DOWNTO 13);");
          Contents.add("   s_level_10(0)            <= s_level_9(0);");
          Contents.add("   ");
          Contents.add("   BCD1    <= s_level_10( 3 DOWNTO  0 );");
          Contents.add("   BCD10   <= s_level_10( 7 DOWNTO  4 );");
          Contents.add("   BCD100  <= s_level_10(11 DOWNTO  8);");
          Contents.add("   BCD1000 <= s_level_10(15 DOWNTO 12);");
          Contents.addAll(GetAdd3Block("s_level_0", 13, "s_level_1", 13, "C0"));
          Contents.addAll(GetAdd3Block("s_level_1", 12, "s_level_2", 12, "C1"));
          Contents.addAll(GetAdd3Block("s_level_2", 11, "s_level_3", 11, "C2"));
          Contents.addAll(GetAdd3Block("s_level_3", 10, "s_level_4", 10, "C3"));
          Contents.addAll(GetAdd3Block("s_level_4", 9, "s_level_5", 9, "C4"));
          Contents.addAll(GetAdd3Block("s_level_5", 8, "s_level_6", 8, "C5"));
          Contents.addAll(GetAdd3Block("s_level_6", 7, "s_level_7", 7, "C6"));
          Contents.addAll(GetAdd3Block("s_level_7", 6, "s_level_8", 6, "C7"));
          Contents.addAll(GetAdd3Block("s_level_8", 5, "s_level_9", 5, "C8"));
          Contents.addAll(GetAdd3Block("s_level_9", 4, "s_level_10", 4, "C9"));
          Contents.addAll(GetAdd3Block("s_level_3", 14, "s_level_4", 14, "C10"));
          Contents.addAll(GetAdd3Block("s_level_4", 13, "s_level_5", 13, "C11"));
          Contents.addAll(GetAdd3Block("s_level_5", 12, "s_level_6", 12, "C12"));
          Contents.addAll(GetAdd3Block("s_level_6", 11, "s_level_7", 11, "C13"));
          Contents.addAll(GetAdd3Block("s_level_7", 10, "s_level_8", 10, "C14"));
          Contents.addAll(GetAdd3Block("s_level_8", 9, "s_level_9", 9, "C15"));
          Contents.addAll(GetAdd3Block("s_level_9", 8, "s_level_10", 8, "C16"));
          Contents.addAll(GetAdd3Block("s_level_6", 15, "s_level_7", 15, "C17"));
          Contents.addAll(GetAdd3Block("s_level_7", 14, "s_level_8", 14, "C18"));
          Contents.addAll(GetAdd3Block("s_level_8", 13, "s_level_9", 13, "C19"));
          Contents.addAll(GetAdd3Block("s_level_9", 12, "s_level_10", 12, "C20"));
          break;
      }
    } else {
      Reporter.AddFatalError("Strange, this should not happen as Verilog is not yet supported!\n");
    }
    return Contents;
  }

  private ArrayList<String> GetAdd3Block(
      String SourceName, int SourceStartId, String DestName, int DestStartId, String ProcessName) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.add("   ");
    Contents.add("   ADD3_" + ProcessName + " : PROCESS(" + SourceName + ")");
    Contents.add("   BEGIN");
    Contents.add(
        "      CASE ("
            + SourceName
            + "("
            + Integer.toString(SourceStartId)
            + " DOWNTO "
            + Integer.toString(SourceStartId - 3)
            + ") ) IS");
    Contents.add(
        "         WHEN \"0000\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"0000\";");
    Contents.add(
        "         WHEN \"0001\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"0001\";");
    Contents.add(
        "         WHEN \"0010\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"0010\";");
    Contents.add(
        "         WHEN \"0011\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"0011\";");
    Contents.add(
        "         WHEN \"0100\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"0100\";");
    Contents.add(
        "         WHEN \"0101\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"1000\";");
    Contents.add(
        "         WHEN \"0110\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"1001\";");
    Contents.add(
        "         WHEN \"0111\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"1010\";");
    Contents.add(
        "         WHEN \"1000\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"1011\";");
    Contents.add(
        "         WHEN \"1001\" => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"1100\";");
    Contents.add(
        "         WHEN OTHERS => "
            + DestName
            + "( "
            + Integer.toString(DestStartId)
            + " DOWNTO "
            + Integer.toString(DestStartId - 3)
            + " ) <= \"----\";");
    Contents.add("      END CASE;");
    Contents.add("   END PROCESS ADD3_" + ProcessName + ";");
    return Contents;
  }
}
