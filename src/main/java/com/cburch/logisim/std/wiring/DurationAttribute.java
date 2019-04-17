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

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;
import javax.swing.JTextField;

public class DurationAttribute extends Attribute<Integer> {
  private int min;
  private int max;
  private boolean TickUnits;

  public DurationAttribute(String name, StringGetter disp, int min, int max, boolean IsTicks) {
    super(name, disp);
    this.min = min;
    this.max = max;
    TickUnits = IsTicks;
  }

  @Override
  public java.awt.Component getCellEditor(Integer value) {
    JTextField field = new JTextField();
    field.setText(value.toString());
    return field;
  }

  @Override
  public Integer parse(String value) {
    try {
      Integer ret = Integer.valueOf(value);
      if (ret.intValue() < min) {
        throw new NumberFormatException(StringUtil.format(S.get("durationSmallMessage"), "" + min));
      } else if (ret.intValue() > max) {
        throw new NumberFormatException(StringUtil.format(S.get("durationLargeMessage"), "" + max));
      }
      return ret;
    } catch (NumberFormatException e) {
      throw new NumberFormatException(S.get("freqInvalidMessage"));
    }
  }

  @Override
  public String toDisplayString(Integer value) {
    if (TickUnits) {
      if (value.equals(Integer.valueOf(1))) {
        return S.get("clockDurationOneValue");
      } else {
        return StringUtil.format(S.get("clockDurationValue"), value.toString());
      }
    } else {
      if (value.equals(Integer.valueOf(1))) {
        return S.get("PORDurationOneValue");
      } else {
        return StringUtil.format(S.get("PORDurationValue"), value.toString());
      }
    }
  }
}
