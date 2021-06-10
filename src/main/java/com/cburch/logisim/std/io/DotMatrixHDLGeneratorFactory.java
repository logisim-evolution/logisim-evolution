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

public class DotMatrixHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      String CircuitName) {
    ArrayList<String> Contents = new ArrayList<>();
    String Label = ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL);
    boolean colbased = ComponentInfo.GetComponent().getAttributeSet().getValue(DotMatrix.ATTR_INPUT_TYPE) == DotMatrix.INPUT_COLUMN;
    boolean rowbased = ComponentInfo.GetComponent().getAttributeSet().getValue(DotMatrix.ATTR_INPUT_TYPE) == DotMatrix.INPUT_ROW;
    int rows = ComponentInfo.GetComponent().getAttributeSet().getValue(DotMatrix.ATTR_MATRIX_ROWS).getWidth();
    int cols = ComponentInfo.GetComponent().getAttributeSet().getValue(DotMatrix.ATTR_MATRIX_COLS).getWidth();
    Contents.add("  ");
    if (colbased) {
      for (int r = 0 ; r < rows ; r++)
        for (int c = 0 ; c < cols ; c++) {
          String Colname = (rows == 1) ? GetNetName(ComponentInfo, c, true, Nets)
                                       : GetBusName(ComponentInfo, c, Nets);  
          int idx = r*cols+c+ComponentInfo.GetLocalBubbleOutputStartId();
          if (Colname.isEmpty())
            Contents.add("   "+HDL.assignPreamble()+
                HDLGeneratorFactory.LocalOutputBubbleBusname+HDL.BracketOpen()+
                idx+HDL.BracketClose()+HDL.assignOperator()+HDL.zeroBit()+";");
          else {
            String Wire = (rows == 1) ? Colname : Colname+HDL.BracketOpen()+r+HDL.BracketClose();  
            Contents.add("   "+HDL.assignPreamble()+
                         HDLGeneratorFactory.LocalOutputBubbleBusname+HDL.BracketOpen()+
                         idx+HDL.BracketClose()+HDL.assignOperator()+
                         Wire+";");
          }
        }
    } else if (rowbased) {
      for (int r = 0 ; r < rows ; r++) {
        String Rowname = (cols == 1) ? GetNetName(ComponentInfo, r, true, Nets)
                                     : GetBusName(ComponentInfo, r, Nets);
        for (int c = 0 ; c < cols ; c++) {
          int idx = r*cols+c+ComponentInfo.GetLocalBubbleOutputStartId();
          if (Rowname.isEmpty())
            Contents.add("   "+HDL.assignPreamble()+
                HDLGeneratorFactory.LocalOutputBubbleBusname+HDL.BracketOpen()+
                idx+HDL.BracketClose()+HDL.assignOperator()+HDL.zeroBit()+";");
          else {
            String Wire = (cols == 1) ? Rowname : Rowname+HDL.BracketOpen()+c+HDL.BracketClose();  
            Contents.add("   "+HDL.assignPreamble()+
                         HDLGeneratorFactory.LocalOutputBubbleBusname+HDL.BracketOpen()+
                         idx+HDL.BracketClose()+HDL.assignOperator()+
                         Wire+";");
          }
        }
      }
    } else {
      String RowName = (rows == 1) ? GetNetName(ComponentInfo, 1, true, Nets)
                                   : GetBusName(ComponentInfo, 1, Nets);
      String ColName = (cols == 1) ? GetNetName(ComponentInfo, 0, true, Nets)
                                   : GetBusName(ComponentInfo, 0, Nets);
      boolean oneRow = (rows == 1);
      boolean oneCol = (cols == 1);
      if (HDL.isVHDL()) {
        String indent = "   ";
        if (!oneRow) {
          Contents.add(indent+Label+"_0 : FOR r IN "+(rows-1)+" DOWNTO 0 GENERATE");
          indent += "   ";
          RowName += "(r)";
        }
        if (!oneCol) {
          Contents.add(indent+Label+"_1 : FOR c IN "+(cols-1)+" DOWNTO 0 GENERATE");
          indent += "   ";
          ColName += "(c)";
        }
        
        String content = indent+HDLGeneratorFactory.LocalOutputBubbleBusname+"(";
        
        content +=  ((oneRow) ? "" : "r"+((oneCol) ? "" : "*"+cols+"+"))+
                    ((oneCol) ? "" : "c") +
                    ((oneRow && oneCol) ? "" : "+");
        
        content += ComponentInfo.GetLocalBubbleOutputStartId()+") <= "+RowName+" AND "+ColName+";";
        Contents.add(content);
        
        if (!oneCol) {
          indent = indent.substring(0,indent.length()-3);
          Contents.add(indent+"END GENERATE "+Label+"_1;");
        }
        if (!oneRow) {
          indent = indent.substring(0,indent.length()-3);
          Contents.add(indent+"END GENERATE "+Label+"_0;");
        }
    
      } else {
        String indent = "   ";
        if (!oneRow || !oneCol) {
          Contents.add(indent+"genvar "+ ((oneRow) ? "" : Label+"_r"+ ((oneCol) ? "" : ","))+
                      ((oneCol) ? "" : Label+"_c")+";");
          Contents.add(indent+"generate");
          indent += "   ";
        }
        if (!oneRow) {
          Contents.add(indent+"for ("+Label+"_r = 0 ; "+Label+"_r < "+rows+"; "+Label+"_r="+Label+"_r+1)");
          Contents.add(indent+"begin:"+Label+"_0");
          indent += "   ";
          RowName += "["+Label+"_r]";
        }
        if (!oneCol) {
          Contents.add(indent+"for ("+Label+"_c = 0 ; "+Label+"_c < "+cols+"; "+Label+"_c="+Label+"_c+1)");
          Contents.add(indent+"begin:"+Label+"_1");
          indent += "   ";
          ColName += "["+Label+"_c]";
        }
        
        String content = indent+HDL.assignPreamble()+HDLGeneratorFactory.LocalOutputBubbleBusname+"[";
        
        content +=  ((oneRow) ? "" : Label+"_r"+((oneCol) ? "" : "*"+cols+"+"))+
                    ((oneCol) ? "" : Label+"_c") +
                    ((oneRow && oneCol) ? "" : "+");
        
        content += ComponentInfo.GetLocalBubbleOutputStartId()+"] = "+RowName+" & "+ColName+";";
        Contents.add(content);
        
        if (!oneCol) {
          indent = indent.substring(0,indent.length()-3);
          Contents.add(indent+"end");
        }
        if (!oneRow) {
          indent = indent.substring(0,indent.length()-3);
          Contents.add(indent+"end");
        }
        
        if (!oneRow || !oneCol) {
          indent = indent.substring(0,indent.length()-3);
          Contents.add(indent+"endgenerate");
        }
      }
    }
    Contents.add("  ");
    return Contents;
  }
  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return attrs.getValue(DotMatrix.ATTR_PERSIST) == 0;
  }

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }
}
