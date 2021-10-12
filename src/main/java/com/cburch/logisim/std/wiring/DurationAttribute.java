/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.util.StringGetter;
import javax.swing.JTextField;

public class DurationAttribute extends Attribute<Integer> {
  private final int min;
  private final int max;
  private final boolean TickUnits;

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
      if (ret < min) {
        throw new NumberFormatException(S.get("durationSmallMessage", "" + min));
      } else if (ret > max) {
        throw new NumberFormatException(S.get("durationLargeMessage", "" + max));
      }
      return ret;
    } catch (NumberFormatException e) {
      throw new NumberFormatException(S.get("freqInvalidMessage"));
    }
  }

  @Override
  public String toDisplayString(Integer value) {
    if (TickUnits) {
      if (value.equals(1)) {
        return S.get("clockDurationOneValue");
      } else {
        return S.get("clockDurationValue", value.toString());
      }
    } else {
      if (value.equals(1)) {
        return S.get("PORDurationOneValue");
      } else {
        return S.get("PORDurationValue", value.toString());
      }
    }
  }
}
