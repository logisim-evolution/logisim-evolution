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

import java.util.ArrayList;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;

public class DotMatrixHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

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
    boolean colbased = ComponentInfo.GetComponent().getAttributeSet().getValue(DotMatrix.ATTR_INPUT_TYPE) == DotMatrix.INPUT_COLUMN;
    boolean rowbased = ComponentInfo.GetComponent().getAttributeSet().getValue(DotMatrix.ATTR_INPUT_TYPE) == DotMatrix.INPUT_ROW;
    int rows = ComponentInfo.GetComponent().getAttributeSet().getValue(DotMatrix.ATTR_MATRIX_ROWS).getWidth();
    int cols = ComponentInfo.GetComponent().getAttributeSet().getValue(DotMatrix.ATTR_MATRIX_COLS).getWidth();
    String Preamble = (HDLType.equals(VHDL)) ? "" : "assign ";
    String AssignOperator = (HDLType.equals(VHDL)) ? " <= " : " = ";
    String OpenBracket = (HDLType.equals(VHDL)) ? "(" : "[";
    String CloseBracket = (HDLType.equals(VHDL)) ? ")" : "]";
    Contents.add("  ");
    if (colbased) {
      for (int r = 0 ; r < rows ; r++)
        for (int c = 0 ; c < cols ; c++) {
          String Colname = GetBusName(ComponentInfo, c, HDLType, Nets);
          int idx = r*cols+c+ComponentInfo.GetLocalBubbleOutputStartId();
          Contents.add("   "+Preamble+
                       HDLGeneratorFactory.LocalOutputBubbleBusname+OpenBracket+
                       idx+CloseBracket+AssignOperator+
                       Colname+OpenBracket+r+CloseBracket+";");
        }
    } else if (rowbased) {
      for (int r = 0 ; r < rows ; r++) {
        String Rowname = GetBusName(ComponentInfo, r, HDLType, Nets);
        for (int c = 0 ; c < cols ; c++) {
          int idx = r*cols+c+ComponentInfo.GetLocalBubbleOutputStartId();
          Contents.add("   "+Preamble+
                       HDLGeneratorFactory.LocalOutputBubbleBusname+OpenBracket+
                       idx+CloseBracket+AssignOperator+
                       Rowname+OpenBracket+c+CloseBracket+";");
        }
      }
    } else {
      String RowName = GetBusName(ComponentInfo, 1, HDLType, Nets);
      String ColName = GetBusName(ComponentInfo, 0, HDLType, Nets);
      if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
        Contents.add("   "+Label+"_1 : FOR r IN "+(rows-1)+" DOWNTO 0 GENERATE");
        Contents.add("      "+Label+"_2 : FOR c IN "+(cols-1)+" DOWNTO 0 GENERATE");
        Contents.add("         "+HDLGeneratorFactory.LocalOutputBubbleBusname+"(r*"+cols+
                     "+c+"+ComponentInfo.GetLocalBubbleOutputStartId()+") <= "+
                     RowName+"(r) AND "+ColName+"(c);");
        Contents.add("      END GENERATE "+Label+"_2;");
        Contents.add("    END GENERATE "+Label+"_1;");
      } else {
    	Contents.add("   genvar "+Label+"_r,"+Label+"_c;");
        Contents.add("   generate");
        Contents.add("      for ("+Label+"_r = 0 ; "+Label+"_r < "+rows+"; "+Label+"_r="+Label+"_r+1)");
        Contents.add("      begin:"+Label+"_1");
        Contents.add("         for ("+Label+"_c = 0 ; "+Label+"_c <"+cols+"; "+Label+"_c="+Label+"_c+1)");
        Contents.add("         begin:"+Label+"_2");
        Contents.add("            "+Preamble+HDLGeneratorFactory.LocalOutputBubbleBusname+"["+Label+"_r*"+cols+
                     "+"+Label+"_c+"+ComponentInfo.GetLocalBubbleOutputStartId()+"] = "+
                     RowName+"["+Label+"_r] & "+ColName+"["+Label+"_c];");
        Contents.add("         end");
        Contents.add("      end");
        Contents.add("   endgenerate");
      }
    }
    Contents.add("  ");
    return Contents;
  }
  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    boolean noPersist = attrs.getValue(DotMatrix.ATTR_PERSIST) == 0;
    return noPersist;
  }

  @Override
  public boolean IsOnlyInlined(String HDLType) {
    return true;
  }
}
