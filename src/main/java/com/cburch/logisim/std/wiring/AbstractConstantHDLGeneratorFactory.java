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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;

import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class AbstractConstantHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public long GetConstant(AttributeSet attrs) {
    return 0;
  }

  private String GetConvertOperator(long value, int nr_of_bits) {
    if (HDL.isVHDL()) {
      if (nr_of_bits == 1) return "'" + value + "'";
      return "std_logic_vector(to_unsigned("
          + value
          + ","
          + nr_of_bits
          + "))";
    } else {
      return nr_of_bits + "'d" + value;
    }
  }

  @Override
  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      String CircuitName) {
    final var Contents = (new LineBuffer()).addHdlPairs();
    int NrOfBits = ComponentInfo.getComponent().getEnd(0).getWidth().getWidth();
    if (ComponentInfo.isEndConnected(0)) {
      long ConstantValue = GetConstant(ComponentInfo.getComponent().getAttributeSet());
      if (ComponentInfo.getComponent().getEnd(0).getWidth().getWidth() == 1) {
        /* Single Port net */
        Contents.add("{{assign}} {{1}} {{=}} {{2}};", GetNetName(ComponentInfo, 0, true, Nets), GetConvertOperator(ConstantValue, 1))
            .add("");
      } else {
        if (Nets.isContinuesBus(ComponentInfo, 0)) {
          /* easy case */
          Contents.add("{{assign}} {{1}} {{=}} {{2}};", GetBusNameContinues(ComponentInfo, 0, Nets), GetConvertOperator(ConstantValue, NrOfBits));
          Contents.add("");
        } else {
          /* we have to enumerate all bits */
          long mask = 1;
          String ConstValue = HDL.zeroBit();
          for (byte bit = 0; bit < NrOfBits; bit++) {
            if ((mask & ConstantValue) != 0) ConstValue = HDL.oneBit();
            else ConstValue = HDL.zeroBit();
            mask <<= 1;
            Contents.add("{{assign}} {{1}} {{=}} {{2}};", GetBusEntryName(ComponentInfo, 0, true, bit, Nets), ConstValue);
          }
          Contents.add("");
        }
      }
    }
    return Contents.getWithIndent();
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
