/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import com.cburch.logisim.data.AttributeOption;
import lombok.Data;

@Data
public class ConvertEvent {
  private final AttributeOption value;
}
