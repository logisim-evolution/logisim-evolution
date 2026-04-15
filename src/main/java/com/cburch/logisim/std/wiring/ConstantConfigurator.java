/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import java.math.BigInteger;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BigIntegerConfigurator;

class ConstantConfigurator extends BigIntegerConfigurator {
  public ConstantConfigurator() {
    super(Constant.ATTR_VALUE, BigInteger.ZERO, BigInteger.ZERO, 0, 16);
  }

  @Override
  public BigInteger getMaximumValue(AttributeSet attrs) {
    BitWidth width = attrs.getValue(StdAttr.WIDTH);
    return width.getBigIntegerMask();
  }

  @Override
  public BigInteger getMinimumValue(AttributeSet attrs) {
    return BigInteger.ZERO;
  }
}
