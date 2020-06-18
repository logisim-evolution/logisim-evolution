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
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class bcd2sevensegHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "BCD2SEVENSEGMENT";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("BCDin", 4);
    return Inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("Segment_a", 1);
    Outputs.put("Segment_b", 1);
    Outputs.put("Segment_c", 1);
    Outputs.put("Segment_d", 1);
    Outputs.put("Segment_e", 1);
    Outputs.put("Segment_f", 1);
    Outputs.put("Segment_g", 1);
    return Outputs;
  }

  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    Wires.put("s_output_value", 7);
    return Wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.add("   Segment_a <= s_output_value(0);");
    Contents.add("   Segment_b <= s_output_value(1);");
    Contents.add("   Segment_c <= s_output_value(2);");
    Contents.add("   Segment_d <= s_output_value(3);");
    Contents.add("   Segment_e <= s_output_value(4);");
    Contents.add("   Segment_f <= s_output_value(5);");
    Contents.add("   Segment_g <= s_output_value(6);");
    Contents.add("   ");
    Contents.add("   MakeSegs : PROCESS( BCDin )");
    Contents.add("   BEGIN");
    Contents.add("      CASE (BCDin) IS");
    Contents.add("         WHEN \"0000\" => s_output_value <= \"0111111\";");
    Contents.add("         WHEN \"0001\" => s_output_value <= \"0000110\";");
    Contents.add("         WHEN \"0010\" => s_output_value <= \"1011011\";");
    Contents.add("         WHEN \"0011\" => s_output_value <= \"1001111\";");
    Contents.add("         WHEN \"0100\" => s_output_value <= \"1100110\";");
    Contents.add("         WHEN \"0101\" => s_output_value <= \"1101101\";");
    Contents.add("         WHEN \"0110\" => s_output_value <= \"1111101\";");
    Contents.add("         WHEN \"0111\" => s_output_value <= \"0000111\";");
    Contents.add("         WHEN \"1000\" => s_output_value <= \"1111111\";");
    Contents.add("         WHEN \"1001\" => s_output_value <= \"1101111\";");
    Contents.add("         WHEN OTHERS => s_output_value <= \"-------\";");
    Contents.add("      END CASE;");
    Contents.add("   END PROCESS MakeSegs;");
    return Contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
	if (!(MapInfo instanceof NetlistComponent)) return PortMap;
	NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(
        GetNetMap("BCDin", true, ComponentInfo, bcd2sevenseg.BCDin, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap(
            "Segment_a", true, ComponentInfo, bcd2sevenseg.Segment_A, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap(
            "Segment_b", true, ComponentInfo, bcd2sevenseg.Segment_B, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap(
            "Segment_c", true, ComponentInfo, bcd2sevenseg.Segment_C, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap(
            "Segment_d", true, ComponentInfo, bcd2sevenseg.Segment_D, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap(
            "Segment_e", true, ComponentInfo, bcd2sevenseg.Segment_E, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap(
            "Segment_f", true, ComponentInfo, bcd2sevenseg.Segment_F, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap(
            "Segment_g", true, ComponentInfo, bcd2sevenseg.Segment_G, Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "bfh";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return HDLType.equals(VHDL);
  }
}
