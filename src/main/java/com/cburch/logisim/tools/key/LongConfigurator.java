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

public class LongConfigurator extends NumericConfigurator<Long> {
  public LongConfigurator(Attribute<Long> attr, long min, long max, int modifiersEx) {
    super(attr, min, max, modifiersEx);
  }

  public LongConfigurator(Attribute<Long> attr, long min, long max, int modifiersEx, int radix) {
    super(attr, min, max, modifiersEx, radix);
  }

  @Override
  protected Long createValue(long val) {
    return val;
  }
}
