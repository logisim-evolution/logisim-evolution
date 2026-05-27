/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import java.math.BigInteger;

import com.cburch.logisim.data.Attribute;

public class BigIntegerConfigurator extends BigNumericConfigurator<BigInteger> {
  public BigIntegerConfigurator(Attribute<BigInteger> attr, BigInteger min, BigInteger max, int modifiersEx) {
    super(attr, min, max, modifiersEx);
  }

  public BigIntegerConfigurator(Attribute<BigInteger> attr, BigInteger min, BigInteger max, int modifiersEx, int radix) {
    super(attr, min, max, modifiersEx, radix);
  }

  @Override
  protected BigInteger createValue(BigInteger val) {
    return val;
  }
}
