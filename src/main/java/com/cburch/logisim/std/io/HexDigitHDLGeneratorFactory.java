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
import com.cburch.logisim.fpga.gui.FPGAReport;
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
    if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
      Contents.add(" ");
      if (ComponentInfo.EndIsConnected(HexDigit.HEX)) {
        Contents.add("   WITH ("+BusName+") SELECT "+HDLGeneratorFactory.LocalOutputBubbleBusname+"( "+
        		(ComponentInfo.GetLocalBubbleOutputStartId() + 6)+" DOWNTO "+
        		ComponentInfo.GetLocalBubbleOutputStartId()+") <= ");
        Contents.add("      \"0111111\" WHEN \"0000\",");
        Contents.add("      \"0000110\" WHEN \"0001\",");
        Contents.add("      \"1011011\" WHEN \"0010\",");
        Contents.add("      \"1001111\" WHEN \"0011\",");
        Contents.add("      \"1100110\" WHEN \"0100\",");
        Contents.add("      \"1101101\" WHEN \"0101\",");
        Contents.add("      \"1111101\" WHEN \"0110\",");
        Contents.add("      \"0000111\" WHEN \"0111\",");
        Contents.add("      \"1111111\" WHEN \"1000\",");
        Contents.add("      \"1100111\" WHEN \"1001\",");
        Contents.add("      \"1110111\" WHEN \"1010\",");
        Contents.add("      \"1111100\" WHEN \"1011\",");
        Contents.add("      \"0111001\" WHEN \"1100\",");
        Contents.add("      \"1011110\" WHEN \"1101\",");
        Contents.add("      \"1111001\" WHEN \"1110\",");
        Contents.add("      \"1110001\" WHEN OTHERS;");
      } else {
        Contents.add("   "+HDLGeneratorFactory.LocalOutputBubbleBusname+"( "+
        		(ComponentInfo.GetLocalBubbleOutputStartId() + 6)+" DOWNTO "+
        		ComponentInfo.GetLocalBubbleOutputStartId()+") <= "+BusName+";");
      }
      if (ComponentInfo.GetComponent().getAttributeSet().getValue(SevenSegment.ATTR_DP)) {
        Contents.add( "   "
                + HDLGeneratorFactory.LocalOutputBubbleBusname
                + "("
                + Integer.toString(ComponentInfo.GetLocalBubbleOutputStartId() + 7)
                + ") <= "
                + DPName
                + ";");
      }
    } else {
      String Signame = HDLGeneratorFactory.LocalOutputBubbleBusname+"["+
                       (ComponentInfo.GetLocalBubbleOutputStartId() + 6)+":"+
                       ComponentInfo.GetLocalBubbleOutputStartId()+"]";
      if (ComponentInfo.EndIsConnected(HexDigit.HEX)) {
        String RegName = "s_"+Label+"_reg";
        Contents.add(" ");
        Contents.add("   reg[6:0] "+RegName+";");
        Contents.add("   always @(*)");
        Contents.add("      case ("+BusName+")");
        Contents.add("        4'b0000 : "+RegName+" = 7'b0111111;");
        Contents.add("        4'b0001 : "+RegName+" = 7'b0000110;");
        Contents.add("        4'b0010 : "+RegName+" = 7'b1011011;");
        Contents.add("        4'b0011 : "+RegName+" = 7'b1001111;");
        Contents.add("        4'b0100 : "+RegName+" = 7'b1100110;");
        Contents.add("        4'b0101 : "+RegName+" = 7'b1101101;");
        Contents.add("        4'b0110 : "+RegName+" = 7'b1111101;");
        Contents.add("        4'b0111 : "+RegName+" = 7'b0000111;");
        Contents.add("        4'b1000 : "+RegName+" = 7'b1111111;");
        Contents.add("        4'b1001 : "+RegName+" = 7'b1100111;");
        Contents.add("        4'b1010 : "+RegName+" = 7'b1110111;");
        Contents.add("        4'b1011 : "+RegName+" = 7'b1111100;");
        Contents.add("        4'b1100 : "+RegName+" = 7'b0111001;");
        Contents.add("        4'b1101 : "+RegName+" = 7'b1011110;");
        Contents.add("        4'b1110 : "+RegName+" = 7'b1111001;");
        Contents.add("        default : "+RegName+" = 7'b1110001;");
        Contents.add("      endcase");
        Contents.add(" ");
        Contents.add("   assign "+Signame+" = "+RegName+";");
      } else {
        Contents.add("   assign "+Signame+" = "+BusName+";");
      }
      if (ComponentInfo.GetComponent().getAttributeSet().getValue(SevenSegment.ATTR_DP)) {
        Contents.add("   assign "+HDLGeneratorFactory.LocalOutputBubbleBusname+"["+
                     Integer.toString(ComponentInfo.GetLocalBubbleOutputStartId() + 7)+
                     "] = "+DPName+";");
      }
    }
    return Contents;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }

  @Override
  public boolean IsOnlyInlined(String HDLType) {
    return true;
  }
}
