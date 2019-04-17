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

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.fpgagui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;

public class HexDigitHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      FPGAReport Reporter,
      String CircuitName,
      String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    String Label = ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL);
    String BusName = GetBusName(ComponentInfo, HexDigit.HEX, HDLType, Nets);
    String DPName = GetNetName(ComponentInfo, HexDigit.DP, true, HDLType, Nets);
    Contents.add(" ");
    Contents.add("   " + Label + " : PROCESS ( " + BusName + ", " + DPName + " ) IS");
    Contents.add("      VARIABLE v_segs : std_logic_vector( 6 DOWNTO 0 );");
    Contents.add("      BEGIN");
    Contents.add("         CASE ( " + BusName + " ) IS");
    Contents.add("            WHEN \"0000\" => v_segs := \"0111111\";");
    Contents.add("            WHEN \"0001\" => v_segs := \"0000110\";");
    Contents.add("            WHEN \"0010\" => v_segs := \"1011011\";");
    Contents.add("            WHEN \"0011\" => v_segs := \"1001111\";");
    Contents.add("            WHEN \"0100\" => v_segs := \"1100110\";");
    Contents.add("            WHEN \"0101\" => v_segs := \"1101101\";");
    Contents.add("            WHEN \"0110\" => v_segs := \"1111101\";");
    Contents.add("            WHEN \"0111\" => v_segs := \"0000111\";");
    Contents.add("            WHEN \"1000\" => v_segs := \"1111111\";");
    Contents.add("            WHEN \"1001\" => v_segs := \"1100111\";");
    Contents.add("            WHEN \"1010\" => v_segs := \"1110111\";");
    Contents.add("            WHEN \"1011\" => v_segs := \"1111100\";");
    Contents.add("            WHEN \"1100\" => v_segs := \"0111001\";");
    Contents.add("            WHEN \"1101\" => v_segs := \"1011110\";");
    Contents.add("            WHEN \"1110\" => v_segs := \"1111001\";");
    Contents.add("            WHEN OTHERS => v_segs := \"1110001\";");
    Contents.add("         END CASE;");
    for (int i = 0; i < 7; i++)
      Contents.add(
          "         "
              + HDLGeneratorFactory.LocalOutputBubbleBusname
              + "("
              + Integer.toString(ComponentInfo.GetLocalBubbleOutputStartId() + i)
              + ") <= v_segs("
              + i
              + ");");
    Contents.add(
        "         "
            + HDLGeneratorFactory.LocalOutputBubbleBusname
            + "("
            + Integer.toString(ComponentInfo.GetLocalBubbleOutputStartId() + 7)
            + ") <= "
            + DPName
            + ";");
    Contents.add("      END PROCESS " + Label + ";");
    return Contents;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return HDLType.equals(HDLGeneratorFactory.VHDL);
  }

  @Override
  public boolean IsOnlyInlined(String HDLType) {
    return true;
  }
}
