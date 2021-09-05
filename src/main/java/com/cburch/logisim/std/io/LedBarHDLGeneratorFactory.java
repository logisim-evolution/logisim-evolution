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

import java.util.ArrayList;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;

public class LedBarHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  protected Attribute<BitWidth> getAttributeColumns() {
    return LedBar.ATTR_MATRIX_COLS;
  }

  @Override
  public ArrayList<String> GetInlinedCode(Netlist netlist, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = new ArrayList<String>();
    final var isSingleBus = componentInfo.getComponent().getAttributeSet().getValue(LedBar.ATTR_INPUT_TYPE).equals(LedBar.INPUT_ONE_WIRE);
    final var nrOfSegments = componentInfo.getComponent().getAttributeSet().getValue(getAttributeColumns()).getWidth();
    for (var pin = 0; pin < nrOfSegments; pin++) {
      final var destPin = HDLGeneratorFactory.LocalOutputBubbleBusname
          + HDL.BracketOpen()
          + (componentInfo.getLocalBubbleOutputStartId() + pin)
          + HDL.BracketClose();
      final var sourcePin = isSingleBus ? GetBusEntryName(componentInfo, 0, true, pin, netlist)
          : GetNetName(componentInfo, pin, true, netlist);
      contents.add("   " + HDL.assignPreamble() + destPin + HDL.assignOperator() + sourcePin + ";");
    }
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
