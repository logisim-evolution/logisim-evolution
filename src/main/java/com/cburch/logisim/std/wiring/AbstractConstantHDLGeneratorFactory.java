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

public class AbstractConstantHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public long GetConstant(AttributeSet attrs) {
    return 0;
  }

  private String GetConvertOperator(long value, int nr_of_bits, String HDLType) {
    if (HDLType.equals(VHDL)) {
      if (nr_of_bits == 1) return "'" + Long.toString(value) + "'";
      return "std_logic_vector(to_unsigned("
          + Long.toString(value)
          + ","
          + Integer.toString(nr_of_bits)
          + "))";
    } else {
      return Integer.toString(nr_of_bits) + "'d" + Long.toString(value);
    }
  }

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
    int NrOfBits = ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
    if (ComponentInfo.EndIsConnected(0)) {
      long ConstantValue = GetConstant(ComponentInfo.GetComponent().getAttributeSet());
      if (ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth() == 1) {
        /* Single Port net */
        Contents.add(
            "   "
                + Preamble
                + GetNetName(ComponentInfo, 0, true, HDLType, Nets)
                + AssignOperator
                + GetConvertOperator(ConstantValue, 1, HDLType)
                + ";");
        Contents.add("");
      } else {
        if (Nets.IsContinuesBus(ComponentInfo, 0)) {
          /* easy case */
          Contents.add(
              "   "
                  + Preamble
                  + GetBusNameContinues(ComponentInfo, 0, HDLType, Nets)
                  + AssignOperator
                  + GetConvertOperator(ConstantValue, NrOfBits, HDLType)
                  + ";");
          Contents.add("");
        } else {
          /* we have to enumerate all bits */
          long mask = 1;
          String ConstValue = (HDLType.equals(VHDL)) ? "'0'" : "1'b0";
          for (byte bit = 0; bit < NrOfBits; bit++) {
            if ((mask & ConstantValue) != 0) ConstValue = (HDLType.equals(VHDL)) ? "'1'" : "1'b1";
            else ConstValue = (HDLType.equals(VHDL)) ? "'0'" : "1'b0";
            mask <<= 1;
            Contents.add(
                "   "
                    + Preamble
                    + GetBusEntryName(ComponentInfo, 0, true, bit, HDLType, Nets)
                    + AssignOperator
                    + ConstValue
                    + ";");
          }
          Contents.add("");
        }
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
