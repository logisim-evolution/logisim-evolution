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

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
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
    SortedMap<String, Integer> MyInputs = new TreeMap<>();
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
    SortedMap<String, Integer> MyOutputs = new TreeMap<>();
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
    SortedMap<String, Integer> wires = new TreeMap<>();
    wires.put("segments", 7);
    wires.put("bcd", 4);
    return wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   Sega  <= segments(0);");
    contents.add("   Segb  <= segments(1);");
    contents.add("   Segc  <= segments(2);");
    contents.add("   Segd  <= segments(3);");
    contents.add("   Sege  <= segments(4);");
    contents.add("   Segf  <= segments(5);");
    contents.add("   Segg  <= segments(6);");
    contents.add("\n");
    contents.add("   bcd   <= BCD3&BCD2&BCD1&BCD0;");
    contents.add("\n");
    contents.add("   Decode : PROCESS ( bcd , LT , BI , RBI ) IS");
    contents.add("      BEGIN");
    contents.add("         CASE bcd IS");
    contents.add("            WHEN \"0000\" => segments <= \"0111111\";");
    contents.add("            WHEN \"0001\" => segments <= \"0000110\";");
    contents.add("            WHEN \"0010\" => segments <= \"1011011\";");
    contents.add("            WHEN \"0011\" => segments <= \"1001111\";");
    contents.add("            WHEN \"0100\" => segments <= \"1100110\";");
    contents.add("            WHEN \"0101\" => segments <= \"1101101\";");
    contents.add("            WHEN \"0110\" => segments <= \"1111101\";");
    contents.add("            WHEN \"0111\" => segments <= \"0000111\";");
    contents.add("            WHEN \"1000\" => segments <= \"1111111\";");
    contents.add("            WHEN \"1001\" => segments <= \"1100111\";");
    contents.add("            WHEN \"1010\" => segments <= \"1110111\";");
    contents.add("            WHEN \"1011\" => segments <= \"1111100\";");
    contents.add("            WHEN \"1100\" => segments <= \"0111001\";");
    contents.add("            WHEN \"1101\" => segments <= \"1011110\";");
    contents.add("            WHEN \"1110\" => segments <= \"1111001\";");
    contents.add("            WHEN OTHERS => segments <= \"1110001\";");
    contents.add("         END CASE;");
    contents.add("         IF (BI = '0') THEN segments <= \"0000000\";");
    contents.add("         ELSIF (LT = '0') THEN segments <= \"1111111\";");
    contents.add("         ELSIF ((RBI='0') AND (bcd=\"0000\")) THEN segments <= \"0000000\";");
    contents.add("         END IF;");
    contents.add("      END PROCESS Decode;");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> portMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    final var ComponentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("BCD0", true, ComponentInfo, 6, Nets));
    portMap.putAll(GetNetMap("BCD1", true, ComponentInfo, 0, Nets));
    portMap.putAll(GetNetMap("BCD2", true, ComponentInfo, 1, Nets));
    portMap.putAll(GetNetMap("BCD3", true, ComponentInfo, 5, Nets));
    portMap.putAll(GetNetMap("LT", false, ComponentInfo, 2, Nets));
    portMap.putAll(GetNetMap("BI", false, ComponentInfo, 3, Nets));
    portMap.putAll(GetNetMap("RBI", false, ComponentInfo, 4, Nets));
    portMap.putAll(GetNetMap("Sega", true, ComponentInfo, 11, Nets));
    portMap.putAll(GetNetMap("Segb", true, ComponentInfo, 10, Nets));
    portMap.putAll(GetNetMap("Segc", true, ComponentInfo, 9, Nets));
    portMap.putAll(GetNetMap("Segd", true, ComponentInfo, 8, Nets));
    portMap.putAll(GetNetMap("Sege", true, ComponentInfo, 7, Nets));
    portMap.putAll(GetNetMap("Segf", true, ComponentInfo, 13, Nets));
    portMap.putAll(GetNetMap("Segg", true, ComponentInfo, 12, Nets));
    return portMap;
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
  public boolean HDLTargetSupported(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TTL.VCC_GND) && HDL.isVHDL());
  }
}
