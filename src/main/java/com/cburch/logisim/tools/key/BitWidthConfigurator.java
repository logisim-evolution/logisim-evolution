/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import java.awt.event.InputEvent;

public class BitWidthConfigurator extends NumericConfigurator<BitWidth> {
  public BitWidthConfigurator(Attribute<BitWidth> attr) {
    super(attr, 1, Value.MAX_WIDTH, InputEvent.ALT_DOWN_MASK);
  }

  public BitWidthConfigurator(Attribute<BitWidth> attr, int min, int max) {
    super(attr, min, max, InputEvent.ALT_DOWN_MASK);
  }

  public BitWidthConfigurator(Attribute<BitWidth> attr, int min, int max, int modifiersEx) {
    super(attr, min, max, modifiersEx);
  }

  @Override
  protected BitWidth createValue(long val) {
    return BitWidth.create((int) val);
  }
}
