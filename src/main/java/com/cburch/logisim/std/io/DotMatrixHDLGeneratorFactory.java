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
    final var contents = new ArrayList<String>();
    final var colBased = componentInfo.GetComponent().getAttributeSet().getValue(DotMatrixBase.ATTR_INPUT_TYPE) == DotMatrixBase.INPUT_COLUMN;
    final var rowBased = componentInfo.GetComponent().getAttributeSet().getValue(DotMatrixBase.ATTR_INPUT_TYPE) == DotMatrixBase.INPUT_ROW;
    final var rows = componentInfo.GetComponent().getAttributeSet().getValue(getAttributeRows()).getWidth();
    final var cols = componentInfo.GetComponent().getAttributeSet().getValue(getAttributeColumns()).getWidth();

    contents.add("  ");
    if (colBased) {
      for (var r = 0; r < rows; r++)
        for (var c = 0; c < cols; c++) {
          final var wire = (rows == 1) ? GetNetName(componentInfo, c, true, netlist) 
              : GetBusEntryName(componentInfo, c, true, r, netlist);
          final var idx = r * cols + c + componentInfo.GetLocalBubbleOutputStartId();
          contents.add("   " + HDL.assignPreamble() + HDLGeneratorFactory.LocalOutputBubbleBusname
              + HDL.BracketOpen() + idx + HDL.BracketClose() + HDL.assignOperator() + wire + ";");
        }
    } else if (rowBased) {
      for (var r = 0; r < rows; r++) {
        for (var c = 0; c < cols; c++) {
          final var wire = (cols == 1) ? GetNetName(componentInfo, r, true, netlist)
              : GetBusEntryName(componentInfo, r, true, c, netlist);
          final var idx = r * cols + c + componentInfo.GetLocalBubbleOutputStartId();
          contents.add("   " + HDL.assignPreamble() + HDLGeneratorFactory.LocalOutputBubbleBusname
                + HDL.BracketOpen() + idx + HDL.BracketClose() + HDL.assignOperator() + wire + ";");
        }
      }
    } else {
      for (var r = 0; r < rows; r++) {
        for (var c = 0; c < cols; c++) {
          final var rowwire = (rows == 1) ? GetNetName(componentInfo, 1, true, netlist)
              : GetBusEntryName(componentInfo, 1, true, r, netlist);
          final var colwire = (rows == 1) ? GetNetName(componentInfo, 0, true, netlist)
              : GetBusEntryName(componentInfo, 0, true, c, netlist);
          final var idx = r * cols + c + componentInfo.GetLocalBubbleOutputStartId();
          contents.add("   " + HDL.assignPreamble() + HDLGeneratorFactory.LocalOutputBubbleBusname
                + HDL.BracketOpen() + idx + HDL.BracketClose() + HDL.assignOperator() 
                + rowwire + HDL.andOperator() + colwire + ";");
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
