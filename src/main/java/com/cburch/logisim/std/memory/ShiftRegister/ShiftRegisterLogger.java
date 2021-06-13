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

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class ShiftRegisterLogger extends InstanceLogger {
  @Override
  public String getLogName(InstanceState state, Object option) {
    String inName = state.getAttributeValue(StdAttr.LABEL);
    if (inName == null || inName.equals("")) {
      inName = S.get("shiftRegisterComponent") + state.getInstance().getLocation();
    }
    if (option instanceof Integer) {
      return inName + "[" + option + "]";
    } else {
      return inName;
    }
  }

  @Override
  public BitWidth getBitWidth(InstanceState state, Object option) {
    return state.getAttributeValue(StdAttr.WIDTH);
  }
  
  @Override
  public Object[] getLogOptions(InstanceState state) {
    Integer stages = state.getAttributeValue(ShiftRegister.ATTR_LENGTH);
    Object[] ret = new Object[stages];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = i;
    }
    return ret;
  }

  @Override
  public Value getLogValue(InstanceState state, Object option) {
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    if (dataWidth == null) dataWidth = BitWidth.create(0);
    ShiftRegisterData data = (ShiftRegisterData) state.getData();
    if (data == null) {
      return Value.createKnown(dataWidth, 0);
    } else {
      int index = option == null ? 0 : (Integer) option;
      return data.get(index);
    }
  }
}
