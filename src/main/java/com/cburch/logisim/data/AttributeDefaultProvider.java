/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import com.cburch.logisim.LogisimVersion;

public interface AttributeDefaultProvider {
  Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver);

  boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver);
}
