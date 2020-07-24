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

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl7447HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL7447";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
    MyInputs.put("BCD0", 1);
    MyInputs.put("BCD1", 1);
    MyInputs.put("BCD2", 1);
    MyInputs.put("BCD3", 1);
    MyInputs.put("LT", 1);
    MyInputs.put("BI", 1);
    MyInputs.put("RBI", 1);
    return MyInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
    MyOutputs.put("Sega", 1);
    MyOutputs.put("Segb", 1);
    MyOutputs.put("Segc", 1);
    MyOutputs.put("Segd", 1);
    MyOutputs.put("Sege", 1);
    MyOutputs.put("Segf", 1);
    MyOutputs.put("Segg", 1);
    return MyOutputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    Wires.put("segments", 7);
    Wires.put("bcd", 4);
    return Wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.add("   Sega  <= segments(0);");
    Contents.add("   Segb  <= segments(1);");
    Contents.add("   Segc  <= segments(2);");
    Contents.add("   Segd  <= segments(3);");
    Contents.add("   Sege  <= segments(4);");
    Contents.add("   Segf  <= segments(5);");
    Contents.add("   Segg  <= segments(6);");
    Contents.add("\n");
    Contents.add("   bcd   <= BCD3&BCD2&BCD1&BCD0;");
    Contents.add("\n");
    Contents.add("   Decode : PROCESS ( bcd , LT , BI , RBI ) IS");
    Contents.add("      BEGIN");
    Contents.add("         CASE bcd IS");
    Contents.add("            WHEN \"0000\" => segments <= \"0111111\";");
    Contents.add("            WHEN \"0001\" => segments <= \"0000110\";");
    Contents.add("            WHEN \"0010\" => segments <= \"1011011\";");
    Contents.add("            WHEN \"0011\" => segments <= \"1001111\";");
    Contents.add("            WHEN \"0100\" => segments <= \"1100110\";");
    Contents.add("            WHEN \"0101\" => segments <= \"1101101\";");
    Contents.add("            WHEN \"0110\" => segments <= \"1111101\";");
    Contents.add("            WHEN \"0111\" => segments <= \"0000111\";");
    Contents.add("            WHEN \"1000\" => segments <= \"1111111\";");
    Contents.add("            WHEN \"1001\" => segments <= \"1100111\";");
    Contents.add("            WHEN \"1010\" => segments <= \"1110111\";");
    Contents.add("            WHEN \"1011\" => segments <= \"1111100\";");
    Contents.add("            WHEN \"1100\" => segments <= \"0111001\";");
    Contents.add("            WHEN \"1101\" => segments <= \"1011110\";");
    Contents.add("            WHEN \"1110\" => segments <= \"1111001\";");
    Contents.add("            WHEN OTHERS => segments <= \"1110001\";");
    Contents.add("         END CASE;");
    Contents.add("         IF (BI = '0') THEN segments <= \"0000000\";");
    Contents.add("         ELSIF (LT = '0') THEN segments <= \"1111111\";");
    Contents.add("         ELSIF ((RBI='0') AND (bcd=\"0000\")) THEN segments <= \"0000000\";");
    Contents.add("         END IF;");
    Contents.add("      END PROCESS Decode;");
    return Contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("BCD0", true, ComponentInfo, 6, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("BCD1", true, ComponentInfo, 0, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("BCD2", true, ComponentInfo, 1, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("BCD3", true, ComponentInfo, 5, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("LT", false, ComponentInfo, 2, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("BI", false, ComponentInfo, 3, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("RBI", false, ComponentInfo, 4, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Sega", true, ComponentInfo, 11, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Segb", true, ComponentInfo, 10, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Segc", true, ComponentInfo, 9, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Segd", true, ComponentInfo, 8, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Sege", true, ComponentInfo, 7, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Segf", true, ComponentInfo, 13, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Segg", true, ComponentInfo, 12, Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    /*
     * this method returns the module directory where the HDL code needs to
     * be placed
     */
    return "ttl";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TTL.VCC_GND) && (HDLType.equals(HDLGeneratorFactory.VHDL)));
  }
}
