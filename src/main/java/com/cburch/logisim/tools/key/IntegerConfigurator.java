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

public class IntegerConfigurator extends NumericConfigurator<Integer> {
  public IntegerConfigurator(Attribute<Integer> attr, int min, int max, int modifiersEx) {
    super(attr, min, max, modifiersEx);
  }

  public IntegerConfigurator(
      Attribute<Integer> attr, int min, int max, int modifiersEx, int radix) {
    super(attr, min, max, modifiersEx, radix);
  }

  @Override
  protected Integer createValue(long val) {
    return (int) val;
  }
}
