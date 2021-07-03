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

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;

public class HexDigitHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(
      Netlist nets,
      Long componentId,
      NetlistComponent componentInfo,
      String circuitName) {
    final var contents = new ArrayList<String>();
    final var label = componentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL);
    final var busName = GetBusName(componentInfo, HexDigit.HEX, nets);
    final var dpName = GetNetName(componentInfo, HexDigit.DP, true, nets);
    if (HDL.isVHDL()) {
      contents.add(" ");
      if (componentInfo.EndIsConnected(HexDigit.HEX)) {
        contents.add("   WITH (" + busName + ") SELECT " + HDLGeneratorFactory.LocalOutputBubbleBusname
                + "( " + (componentInfo.GetLocalBubbleOutputStartId() + 6) + " DOWNTO "
                + componentInfo.GetLocalBubbleOutputStartId() + ") <= ");
        contents.add("      \"0111111\" WHEN \"0000\",");
        contents.add("      \"0000110\" WHEN \"0001\",");
        contents.add("      \"1011011\" WHEN \"0010\",");
        contents.add("      \"1001111\" WHEN \"0011\",");
        contents.add("      \"1100110\" WHEN \"0100\",");
        contents.add("      \"1101101\" WHEN \"0101\",");
        contents.add("      \"1111101\" WHEN \"0110\",");
        contents.add("      \"0000111\" WHEN \"0111\",");
        contents.add("      \"1111111\" WHEN \"1000\",");
        contents.add("      \"1100111\" WHEN \"1001\",");
        contents.add("      \"1110111\" WHEN \"1010\",");
        contents.add("      \"1111100\" WHEN \"1011\",");
        contents.add("      \"0111001\" WHEN \"1100\",");
        contents.add("      \"1011110\" WHEN \"1101\",");
        contents.add("      \"1111001\" WHEN \"1110\",");
        contents.add("      \"1110001\" WHEN OTHERS;");
      } else {
        contents.add("   " + HDLGeneratorFactory.LocalOutputBubbleBusname + "( "
                + (componentInfo.GetLocalBubbleOutputStartId() + 6) + " DOWNTO "
                + componentInfo.GetLocalBubbleOutputStartId() + ") <= " + busName + ";");
      }
      if (componentInfo.GetComponent().getAttributeSet().getValue(SevenSegment.ATTR_DP)) {
        contents.add("   " + HDLGeneratorFactory.LocalOutputBubbleBusname + "("
                + (componentInfo.GetLocalBubbleOutputStartId() + 7) + ") <= " + dpName + ";");
      }
    } else {
      String Signame = HDLGeneratorFactory.LocalOutputBubbleBusname + "["
                        + (componentInfo.GetLocalBubbleOutputStartId() + 6) + ":"
                        + componentInfo.GetLocalBubbleOutputStartId() + "]";
      if (componentInfo.EndIsConnected(HexDigit.HEX)) {
        String RegName = "s_" + label + "_reg";
        contents.add(" ");
        contents.add("   reg[6:0] " + RegName + ";");
        contents.add("   always @(*)");
        contents.add("      case (" + busName + ")");
        contents.add("         4'b0000 : " + RegName + " = 7'b0111111;");
        contents.add("         4'b0001 : " + RegName + " = 7'b0000110;");
        contents.add("         4'b0010 : " + RegName + " = 7'b1011011;");
        contents.add("         4'b0011 : " + RegName + " = 7'b1001111;");
        contents.add("         4'b0100 : " + RegName + " = 7'b1100110;");
        contents.add("         4'b0101 : " + RegName + " = 7'b1101101;");
        contents.add("         4'b0110 : " + RegName + " = 7'b1111101;");
        contents.add("         4'b0111 : " + RegName + " = 7'b0000111;");
        contents.add("         4'b1000 : " + RegName + " = 7'b1111111;");
        contents.add("         4'b1001 : " + RegName + " = 7'b1100111;");
        contents.add("         4'b1010 : " + RegName + " = 7'b1110111;");
        contents.add("         4'b1011 : " + RegName + " = 7'b1111100;");
        contents.add("         4'b1100 : " + RegName + " = 7'b0111001;");
        contents.add("         4'b1101 : " + RegName + " = 7'b1011110;");
        contents.add("         4'b1110 : " + RegName + " = 7'b1111001;");
        contents.add("         default : " + RegName + " = 7'b1110001;");
        contents.add("      endcase");
        contents.add(" ");
        contents.add("   assign " + Signame + " = " + RegName + ";");
      } else {
        contents.add("   assign " + Signame + " = " + busName + ";");
      }
      if (componentInfo.GetComponent().getAttributeSet().getValue(SevenSegment.ATTR_DP)) {
        contents.add("   assign " + HDLGeneratorFactory.LocalOutputBubbleBusname + "["
                + (componentInfo.GetLocalBubbleOutputStartId()  +  7) + "] = " + dpName + ";");
      }
    }
    return contents;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }
}
