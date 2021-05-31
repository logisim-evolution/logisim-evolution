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

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;

import java.util.ArrayList;

public class BitExtenderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      String CircuitName) {
    ArrayList<String> Contents = new ArrayList<>();
    int NrOfPins = ComponentInfo.NrOfEnds();
    for (int i = 1; i < NrOfPins; i++) {
      if (!ComponentInfo.EndIsConnected(i)) {
        Reporter.Report.AddError(
            "Bit Extender component has floating input connection in circuit \""
                + CircuitName
                + "\"!");
        return Contents;
      }
    }
    if (ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth() == 1) {
      /* Special case: Single bit output */
      Contents.add(
          "   "
              + HDL.assignPreamble()
              + GetNetName(ComponentInfo, 0, true, Nets)
              + HDL.assignOperator()
              + GetNetName(ComponentInfo, 1, true, Nets));
      Contents.add("");
    } else {
      /*
       * We make ourselves life easy, we just enumerate through all the
       * bits
       */
      StringBuilder Replacement = new StringBuilder();
      String type =
          (String)
              ComponentInfo.GetComponent()
                  .getAttributeSet()
                  .getValue(BitExtender.ATTR_TYPE)
                  .getValue();
      if (type.equals("zero")) Replacement.append(HDL.zeroBit());
      if (type.equals("one")) Replacement.append(HDL.oneBit());
      if (type.equals("sign")) {
        if (ComponentInfo.getEnd(1).NrOfBits() > 1) {
          Replacement.append(
              GetBusEntryName(
                  ComponentInfo,
                  1,
                  true,
                  ComponentInfo.GetComponent().getEnd(1).getWidth().getWidth() - 1,
                  Nets));
        } else {
          Replacement.append(GetNetName(ComponentInfo, 1, true, Nets));
        }
      }
      if (type.equals("input"))
        Replacement.append(GetNetName(ComponentInfo, 2, true, Nets));
      for (int bit = 0; bit < ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth(); bit++) {
        if (bit < ComponentInfo.GetComponent().getEnd(1).getWidth().getWidth()) {
          if (ComponentInfo.getEnd(1).NrOfBits() > 1) {
            Contents.add(
                "   "
                    + HDL.assignPreamble()
                    + GetBusEntryName(ComponentInfo, 0, true, bit, Nets)
                    + HDL.assignOperator()
                    + GetBusEntryName(ComponentInfo, 1, true, bit, Nets)
                    + ";");
          } else {
            Contents.add(
                "   "
                    + HDL.assignPreamble()
                    + GetBusEntryName(ComponentInfo, 0, true, bit, Nets)
                    + HDL.assignOperator()
                    + GetNetName(ComponentInfo, 1, true, Nets)
                    + ";");
          }
        } else {
          Contents.add(
              "   "
                  + HDL.assignPreamble()
                  + GetBusEntryName(ComponentInfo, 0, true, bit, Nets)
                  + HDL.assignOperator()
                  + Replacement
                  + ";");
        }
      }
      Contents.add("");
    }
    return Contents;
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
