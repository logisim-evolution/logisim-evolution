/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
    final var colBased = componentInfo.getComponent().getAttributeSet().getValue(DotMatrixBase.ATTR_INPUT_TYPE) == DotMatrixBase.INPUT_COLUMN;
    final var rowBased = componentInfo.getComponent().getAttributeSet().getValue(DotMatrixBase.ATTR_INPUT_TYPE) == DotMatrixBase.INPUT_ROW;
    final var rows = componentInfo.getComponent().getAttributeSet().getValue(getAttributeRows()).getWidth();
    final var cols = componentInfo.getComponent().getAttributeSet().getValue(getAttributeColumns()).getWidth();

    contents.add("  ");
    if (colBased) {
      /* The simulator uses here following addressing scheme (2x2):
       *  r1,c0 r1,c1
       *  r0,c0 r0,c1
       *
       *  hence the rows are inverted to the definition of the LED-Matrix that uses:
       *  r0,c0 r0,c1
       *  r1,c0 r1,c1
       */
      for (var dotMatrixRow = 0; dotMatrixRow < rows; dotMatrixRow++) {
        final var ledMatrixRow = rows - dotMatrixRow - 1;
        for (var ledMatrixCol = 0; ledMatrixCol < cols; ledMatrixCol++) {
          final var wire = (rows == 1) ? GetNetName(componentInfo, ledMatrixCol, true, netlist)
              : GetBusEntryName(componentInfo, ledMatrixCol, true, dotMatrixRow, netlist);
          final var idx = (ledMatrixRow * cols) + ledMatrixCol + componentInfo.getLocalBubbleOutputStartId();
          contents.add("   " + HDL.assignPreamble() + HDLGeneratorFactory.LocalOutputBubbleBusname
              + HDL.BracketOpen() + idx + HDL.BracketClose() + HDL.assignOperator() + wire + ";");
        }
      }
    } else if (rowBased) {
      /* The simulator uses here following addressing scheme (2x2):
       *  r1,c1 r1,c0
       *  r0,c1 r0,c0
       *
       *  hence the cols are inverted to the definition of the LED-Matrix that uses:
       *  r0,c0 r0,c1
       *  r1,c0 r1,c1
       */
      for (var ledMatrixRow = 0; ledMatrixRow < rows; ledMatrixRow++) {
        for (var dotMatrixCol = 0; dotMatrixCol < cols; dotMatrixCol++) {
          final var ledMatrixCol = cols - dotMatrixCol - 1;
          final var wire = (cols == 1) ? GetNetName(componentInfo, ledMatrixRow, true, netlist)
              : GetBusEntryName(componentInfo, ledMatrixRow, true, ledMatrixCol, netlist);
          final var idx = (ledMatrixRow * cols) + dotMatrixCol + componentInfo.getLocalBubbleOutputStartId();
          contents.add("   " + HDL.assignPreamble() + HDLGeneratorFactory.LocalOutputBubbleBusname
                + HDL.BracketOpen() + idx + HDL.BracketClose() + HDL.assignOperator() + wire + ";");
        }
      }
    } else {
      /* The simulator uses here following addressing scheme (2x2):
       *  r1,c0 r1,c1
       *  r0,c0 r0,c1
       *
       *  hence the rows are inverted to the definition of the LED-Matrix that uses:
       *  r0,c0 r0,c1
       *  r1,c0 r1,c1
       */
      for (var dotMatrixRow = 0; dotMatrixRow < rows; dotMatrixRow++) {
        final var ledMatrixRow = rows - dotMatrixRow - 1;
        for (var ledMatrixCol = 0; ledMatrixCol < cols; ledMatrixCol++) {
          final var rowWire = (rows == 1) ? GetNetName(componentInfo, 1, true, netlist)
              : GetBusEntryName(componentInfo, 1, true, dotMatrixRow, netlist);
          final var colWire = (cols == 1) ? GetNetName(componentInfo, 0, true, netlist)
              : GetBusEntryName(componentInfo, 0, true, ledMatrixCol, netlist);
          final var idx = (ledMatrixRow * cols) + ledMatrixCol + componentInfo.getLocalBubbleOutputStartId();
          contents.add("   " + HDL.assignPreamble() + HDLGeneratorFactory.LocalOutputBubbleBusname
                + HDL.BracketOpen() + idx + HDL.BracketClose() + HDL.assignOperator()
                + rowWire + HDL.andOperator() + colWire + ";");
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
