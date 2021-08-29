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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import java.util.ArrayList;

public class PortHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }

  @Override
  public ArrayList<String> GetInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = new ArrayList<String>();
    final var dir = componentInfo.getComponent().getAttributeSet().getValue(PortIO.ATTR_DIR);
    var size = componentInfo.getComponent().getAttributeSet().getValue(PortIO.ATTR_SIZE).getWidth();
    final var nBus = (((size - 1) / BitWidth.MAXWIDTH) + 1);
    if (dir == PortIO.INPUT) {
      for (var i = 0; i < nBus; i++) {
        final var start = componentInfo.getLocalBubbleInputStartId() + i * BitWidth.MAXWIDTH;
        var end = start - 1;
        end += Math.min(size, BitWidth.MAXWIDTH);
        size -= BitWidth.MAXWIDTH;
        contents.add(
            "   "
                + HDL.assignPreamble()
                + GetBusName(componentInfo, i, nets)
                + HDL.assignOperator()
                + HDLGeneratorFactory.LocalInputBubbleBusname
                + HDL.BracketOpen()
                + end
                + HDL.vectorLoopId()
                + "0"
                + HDL.BracketClose()
                + ";");
      }
    } else if (dir == PortIO.OUTPUT) {
      for (var i = 0; i < nBus; i++) {
        final var start = componentInfo.getLocalBubbleOutputStartId() + i * BitWidth.MAXWIDTH;
        var end = start - 1;
        end += Math.min(size, BitWidth.MAXWIDTH);
        size -= BitWidth.MAXWIDTH;
        contents.add("   " + HDL.assignPreamble() + HDLGeneratorFactory.LocalOutputBubbleBusname
                + HDL.assignOperator() + GetBusName(componentInfo, i, nets) + HDL.BracketOpen()
                + end + HDL.vectorLoopId() + "0" + HDL.BracketClose() + ";");
      }
    } else {
      for (var i = 0; i < nBus; i++) {
        final var start = componentInfo.getLocalBubbleInOutStartId() + i * BitWidth.MAXWIDTH;
        final var nbits = Math.min(size, BitWidth.MAXWIDTH);
        final var end = start - 1 + nbits;
        size -= nbits;
        final var enableIndex = (dir == PortIO.INOUTSE) ? 0 : i * 2;
        final var inputIndex = (dir == PortIO.INOUTSE) ? i + 1 : i * 2 + 1;
        final var outputIndex = (dir == PortIO.INOUTSE) ? 1 + nBus + i : 2 * nBus + i;
        final var inputName = GetBusName(componentInfo, inputIndex, nets);
        final var outputName = GetBusName(componentInfo, outputIndex, nets);
        final var enableName = (dir == PortIO.INOUTSE)
                              ? GetNetName(componentInfo, enableIndex, true, nets)
                              : GetBusName(componentInfo, enableIndex, nets);
        contents.add(
            "   " + HDL.assignPreamble() + outputName + HDL.assignOperator()
                + HDLGeneratorFactory.LocalInOutBubbleBusname + HDL.BracketOpen()
                + end + HDL.vectorLoopId() + start + HDL.BracketClose() + ";");
        if (dir == PortIO.INOUTSE) {
          if (HDL.isVHDL()) {
            contents.add("   " + HDLGeneratorFactory.LocalInOutBubbleBusname + HDL.BracketOpen()
                    + end + HDL.vectorLoopId() + start + HDL.BracketClose() + " <= "
                    + inputName + " WHEN " + enableName + " = '1' ELSE (OTHERS => 'Z');");
          } else {
            contents.add("   " + HDL.assignPreamble() + HDLGeneratorFactory.LocalInOutBubbleBusname
                    + HDL.BracketOpen() + end + HDL.vectorLoopId() + start
                    + HDL.BracketClose() + " = (" + enableName + ") ? " + inputName + " : "
                    + nbits + "'bZ;");
          }
        } else {
          for (var bit = 0; bit < nbits; bit++) {
            if (HDL.isVHDL()) {
              contents.add("   " + HDLGeneratorFactory.LocalInOutBubbleBusname
                      + HDL.BracketOpen() + (start + bit) + HDL.BracketClose() + " <= " + inputName
                      + "(" + bit + ") WHEN " + enableName + "(" + bit + ") = '1' ELSE 'Z';");
            } else {
              contents.add("   " + HDL.assignPreamble() + HDLGeneratorFactory.LocalInOutBubbleBusname
                      + HDL.BracketOpen() + (start + bit) + HDL.BracketClose() + " = ("
                      + enableName + "[" + bit + "]) ? " + inputName + "[" + bit + "] : 1'bZ;");
            }
          }
        }
      }
    }
    return contents;
  }
}
