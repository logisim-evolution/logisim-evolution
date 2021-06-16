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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;

public class DotMatrixHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  protected Attribute<BitWidth> getAttributeRows() {
    return DotMatrix.ATTR_MATRIX_ROWS;
  }
  protected Attribute<BitWidth> getAttributeColumns() {
    return DotMatrix.ATTR_MATRIX_COLS;
  }

  @Override
  public ArrayList<String> GetInlinedCode(Netlist netlist, Long componentId, NetlistComponent componentInfo, String circuitName) {
    var contents = new ArrayList<String>();
    var label = componentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL);
    var colBased = componentInfo.GetComponent().getAttributeSet().getValue(DotMatrixBase.ATTR_INPUT_TYPE) == DotMatrixBase.INPUT_COLUMN;
    var rowBased = componentInfo.GetComponent().getAttributeSet().getValue(DotMatrixBase.ATTR_INPUT_TYPE) == DotMatrixBase.INPUT_ROW;
    int rows = componentInfo.GetComponent().getAttributeSet().getValue(getAttributeRows()).getWidth();
    int cols = componentInfo.GetComponent().getAttributeSet().getValue(getAttributeColumns()).getWidth();

    contents.add("  ");
    if (colBased) {
      for (int r = 0 ; r < rows ; r++)
        for (int c = 0 ; c < cols ; c++) {
          String colName = (rows == 1) ? GetNetName(componentInfo, c, true, netlist)
                                       : GetBusName(componentInfo, c, netlist);
          int idx = r * cols + c + componentInfo.GetLocalBubbleOutputStartId();
          if (colName.isEmpty())
            contents.add("   "+HDL.assignPreamble()+
                HDLGeneratorFactory.LocalOutputBubbleBusname+HDL.BracketOpen()+
                idx+HDL.BracketClose()+HDL.assignOperator()+HDL.zeroBit()+";");
          else {
            String Wire = (rows == 1) ? colName : colName+HDL.BracketOpen()+r+HDL.BracketClose();
            contents.add("   "+HDL.assignPreamble()+
                         HDLGeneratorFactory.LocalOutputBubbleBusname+HDL.BracketOpen()+
                         idx+HDL.BracketClose()+HDL.assignOperator()+
                         Wire+";");
          }
        }
    } else if (rowBased) {
      for (int r = 0 ; r < rows ; r++) {
        String rowName = (cols == 1) ? GetNetName(componentInfo, r, true, netlist)
                                     : GetBusName(componentInfo, r, netlist);
        for (int c = 0 ; c < cols ; c++) {
          int idx = r * cols + c + componentInfo.GetLocalBubbleOutputStartId();
          if (rowName.isEmpty())
            contents.add("   "+HDL.assignPreamble()+
                HDLGeneratorFactory.LocalOutputBubbleBusname+HDL.BracketOpen()+
                idx+HDL.BracketClose()+HDL.assignOperator()+HDL.zeroBit()+";");
          else {
            String Wire = (cols == 1) ? rowName : rowName+HDL.BracketOpen()+c+HDL.BracketClose();
            contents.add("   "+HDL.assignPreamble()+
                         HDLGeneratorFactory.LocalOutputBubbleBusname+HDL.BracketOpen()+
                         idx+HDL.BracketClose()+HDL.assignOperator()+
                         Wire+";");
          }
        }
      }
    } else {
      String rowName = (rows == 1) ? GetNetName(componentInfo, 1, true, netlist)
                                   : GetBusName(componentInfo, 1, netlist);
      String colName = (cols == 1) ? GetNetName(componentInfo, 0, true, netlist)
                                   : GetBusName(componentInfo, 0, netlist);
      boolean oneRow = (rows == 1);
      boolean oneCol = (cols == 1);
      if (HDL.isVHDL()) {
        String indent = "   ";
        if (!oneRow) {
          contents.add(indent+label+"_0 : FOR r IN "+(rows-1)+" DOWNTO 0 GENERATE");
          indent += "   ";
          rowName += "(r)";
        }
        if (!oneCol) {
          contents.add(indent+label+"_1 : FOR c IN "+(cols-1)+" DOWNTO 0 GENERATE");
          indent += "   ";
          colName += "(c)";
        }

        String content = indent+HDLGeneratorFactory.LocalOutputBubbleBusname+"(";

        content +=  ((oneRow) ? "" : "r"+((oneCol) ? "" : "*"+cols+"+"))+
                    ((oneCol) ? "" : "c") +
                    ((oneRow && oneCol) ? "" : "+");

        content += componentInfo.GetLocalBubbleOutputStartId()+") <= "+rowName+" AND "+colName+";";
        contents.add(content);

        if (!oneCol) {
          indent = indent.substring(0, indent.length() - 3);
          contents.add(indent + "END GENERATE " + label + "_1;");
        }
        if (!oneRow) {
          indent = indent.substring(0, indent.length() - 3);
          contents.add(indent + "END GENERATE " + label + "_0;");
        }

      } else {
        String indent = "   ";
        if (!oneRow || !oneCol) {
          contents.add(indent + "genvar " + ((oneRow) ? "" : label + "_r" + ((oneCol) ? "" : ",")) +
                  ((oneCol) ? "" : label + "_c") + ";");
          contents.add(indent + "generate");
          indent += "   ";
        }
        if (!oneRow) {
          contents.add(indent + "for (" + label + "_r = 0 ; " + label + "_r < " + rows + "; " + label + "_r=" + label + "_r+1)");
        contents.add(indent + "begin:" + label + "_0");
          indent += "   ";
          rowName += "["+label+"_r]";
        }
        if (!oneCol) {
          contents.add(indent + "for (" + label + "_c = 0 ; " + label + "_c < " + cols + "; " + label + "_c=" + label + "_c+1)");
          contents.add(indent + "begin:" + label + "_1");
          indent += "   ";
          colName += "[" + label + "_c]";
        }

        var content = indent + HDL.assignPreamble() + HDLGeneratorFactory.LocalOutputBubbleBusname + "[";

        content +=  ((oneRow) ? "" : label+"_r"+((oneCol) ? "" : "*"+cols+"+"))+
                    ((oneCol) ? "" : label+"_c") +
                    ((oneRow && oneCol) ? "" : "+");

        content += componentInfo.GetLocalBubbleOutputStartId()+"] = "+rowName+" & "+colName+";";
        contents.add(content);

        if (!oneCol) {
          indent = indent.substring(0,indent.length()-3);
          contents.add(indent+"end");
        }
        if (!oneRow) {
          indent = indent.substring(0,indent.length()-3);
          contents.add(indent+"end");
        }

        if (!oneRow || !oneCol) {
          indent = indent.substring(0,indent.length()-3);
          contents.add(indent+"endgenerate");
        }
      }
    }
    contents.add("  ");

    return contents;
  }
  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return attrs.getValue(DotMatrixBase.ATTR_PERSIST) == 0;
  }

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }
}
