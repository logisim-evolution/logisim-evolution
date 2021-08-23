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
import com.cburch.logisim.util.ContentBuilder;
import java.util.ArrayList;

public class HexDigitHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = new ContentBuilder();
    final var label = componentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL);
    final var busName = GetBusName(componentInfo, HexDigit.HEX, nets);
    final var dpName = GetNetName(componentInfo, HexDigit.DP, true, nets);
    if (HDL.isVHDL()) {
      contents.add(" ");
      if (componentInfo.EndIsConnected(HexDigit.HEX)) {
        contents
            .add("   WITH (%s) SELECT %s( %d DOWNTO %d) <= ", busName, HDLGeneratorFactory.LocalOutputBubbleBusname, (componentInfo.GetLocalBubbleOutputStartId() + 6), componentInfo.GetLocalBubbleOutputStartId())
            .add("      \"0111111\" WHEN \"0000\",")
            .add("      \"0000110\" WHEN \"0001\",")
            .add("      \"1011011\" WHEN \"0010\",")
            .add("      \"1001111\" WHEN \"0011\",")
            .add("      \"1100110\" WHEN \"0100\",")
            .add("      \"1101101\" WHEN \"0101\",")
            .add("      \"1111101\" WHEN \"0110\",")
            .add("      \"0000111\" WHEN \"0111\",")
            .add("      \"1111111\" WHEN \"1000\",")
            .add("      \"1100111\" WHEN \"1001\",")
            .add("      \"1110111\" WHEN \"1010\",")
            .add("      \"1111100\" WHEN \"1011\",")
            .add("      \"0111001\" WHEN \"1100\",")
            .add("      \"1011110\" WHEN \"1101\",")
            .add("      \"1111001\" WHEN \"1110\",")
            .add("      \"1110001\" WHEN OTHERS;");
      } else {
        contents.add("   %s(%d DOWNTO %d) <= %s;", HDLGeneratorFactory.LocalOutputBubbleBusname,
                (componentInfo.GetLocalBubbleOutputStartId() + 6), componentInfo.GetLocalBubbleOutputStartId(), busName);
      }
      if (componentInfo.GetComponent().getAttributeSet().getValue(SevenSegment.ATTR_DP)) {
        contents.add("   %s(%d) <= %s;", HDLGeneratorFactory.LocalOutputBubbleBusname, (componentInfo.GetLocalBubbleOutputStartId() + 7), dpName);
      }
    } else {
      final var sigName = HDLGeneratorFactory.LocalOutputBubbleBusname + "["
                        + (componentInfo.GetLocalBubbleOutputStartId() + 6) + ":"
                        + componentInfo.GetLocalBubbleOutputStartId() + "]";
      if (componentInfo.EndIsConnected(HexDigit.HEX)) {
        final var regName = String.format("s_%s_reg", label);
        contents
            .add(" ")
            .add("   reg[6:0] %s;", regName)
            .add("   always @(*)")
            .add("      case (%s)", busName)
            .add("         4'b0000 : %s = 7'b0111111;", regName)
            .add("         4'b0001 : %s = 7'b0000110;", regName)
            .add("         4'b0010 : %s = 7'b1011011;", regName)
            .add("         4'b0011 : %s = 7'b1001111;", regName)
            .add("         4'b0100 : %s = 7'b1100110;", regName)
            .add("         4'b0101 : %s = 7'b1101101;", regName)
            .add("         4'b0110 : %s = 7'b1111101;", regName)
            .add("         4'b0111 : %s = 7'b0000111;", regName)
            .add("         4'b1000 : %s = 7'b1111111;", regName)
            .add("         4'b1001 : %s = 7'b1100111;", regName)
            .add("         4'b1010 : %s = 7'b1110111;", regName)
            .add("         4'b1011 : %s = 7'b1111100;", regName)
            .add("         4'b1100 : %s = 7'b0111001;", regName)
            .add("         4'b1101 : %s = 7'b1011110;", regName)
            .add("         4'b1110 : %s = 7'b1111001;", regName)
            .add("         default : %s = 7'b1110001;", regName)
            .add("      endcase")
            .add(" ")
            .add("   assign %s = %s;", sigName, regName);
      } else {
        contents.add("   assign %s = %s;", sigName, busName);
      }
      if (componentInfo.GetComponent().getAttributeSet().getValue(SevenSegment.ATTR_DP)) {
        contents.add("   assign %s[%d] = %s;", HDLGeneratorFactory.LocalOutputBubbleBusname, (componentInfo.GetLocalBubbleOutputStartId() + 7), dpName);
      }
    }
    return contents.get();
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
