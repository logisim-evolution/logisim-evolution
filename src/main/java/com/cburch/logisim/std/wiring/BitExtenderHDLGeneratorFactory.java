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

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import java.util.ArrayList;

public class BitExtenderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      FPGAReport Reporter,
      String CircuitName,
      String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    String Preamble = (HDLType.equals(VHDL)) ? "" : "assign ";
    String AssignOperator = (HDLType.equals(VHDL)) ? " <= " : " = ";
    String ZeroBit = (HDLType.equals(VHDL)) ? "'0'" : "1'b0";
    String SetBit = (HDLType.equals(VHDL)) ? "'1'" : "1'b1";
    int NrOfPins = ComponentInfo.NrOfEnds();
    for (int i = 1; i < NrOfPins; i++) {
      if (!ComponentInfo.EndIsConnected(i)) {
        Reporter.AddError(
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
              + Preamble
              + GetNetName(ComponentInfo, 0, true, HDLType, Nets)
              + AssignOperator
              + GetNetName(ComponentInfo, 1, true, HDLType, Nets));
      Contents.add("");
    } else {
      /*
       * We make ourselves life easy, we just enumerate through all the
       * bits
       */
      StringBuffer Replacement = new StringBuffer();
      String type =
          (String)
              ComponentInfo.GetComponent()
                  .getAttributeSet()
                  .getValue(BitExtender.ATTR_TYPE)
                  .getValue();
      if (type.equals("zero")) Replacement.append(ZeroBit);
      if (type.equals("one")) Replacement.append(SetBit);
      if (type.equals("sign")) {
        if (ComponentInfo.getEnd(1).NrOfBits() > 1) {
          Replacement.append(
              GetBusEntryName(
                  ComponentInfo,
                  1,
                  true,
                  ComponentInfo.GetComponent().getEnd(1).getWidth().getWidth() - 1,
                  HDLType,
                  Nets));
        } else {
          Replacement.append(GetNetName(ComponentInfo, 1, true, HDLType, Nets));
        }
      }
      if (type.equals("input"))
        Replacement.append(GetNetName(ComponentInfo, 2, true, HDLType, Nets));
      for (int bit = 0; bit < ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth(); bit++) {
        if (bit < ComponentInfo.GetComponent().getEnd(1).getWidth().getWidth()) {
          if (ComponentInfo.getEnd(1).NrOfBits() > 1) {
            Contents.add(
                "   "
                    + Preamble
                    + GetBusEntryName(ComponentInfo, 0, true, bit, HDLType, Nets)
                    + AssignOperator
                    + GetBusEntryName(ComponentInfo, 1, true, bit, HDLType, Nets)
                    + ";");
          } else {
            Contents.add(
                "   "
                    + Preamble
                    + GetBusEntryName(ComponentInfo, 0, true, bit, HDLType, Nets)
                    + AssignOperator
                    + GetNetName(ComponentInfo, 1, true, HDLType, Nets)
                    + ";");
          }
        } else {
          Contents.add(
              "   "
                  + Preamble
                  + GetBusEntryName(ComponentInfo, 0, true, bit, HDLType, Nets)
                  + AssignOperator
                  + Replacement.toString()
                  + ";");
        }
      }
      Contents.add("");
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
