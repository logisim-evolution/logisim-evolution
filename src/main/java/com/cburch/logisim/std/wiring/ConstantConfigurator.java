/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.LongConfigurator;

class ConstantConfigurator extends LongConfigurator {
  public ConstantConfigurator() {
    super(Constant.ATTR_VALUE, 0, 0, 0, 16);
  }

  @Override
  public long getMaximumValue(AttributeSet attrs) {
    BitWidth width = attrs.getValue(StdAttr.WIDTH);
    long ret = width.getMask();
    if (ret >= 0) {
      return ret;
    } else {
      return Long.MAX_VALUE;
    }
  }

  @Override
  public long getMinimumValue(AttributeSet attrs) {
    BitWidth width = attrs.getValue(StdAttr.WIDTH);
    if (width.getWidth() < 64) {
      return 0;
    } else {
      return Long.MIN_VALUE;
    }
  }
}
