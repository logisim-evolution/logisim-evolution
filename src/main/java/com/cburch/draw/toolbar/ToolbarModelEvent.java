/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.toolbar;

import java.util.EventObject;

public class ToolbarModelEvent extends EventObject {
  private static final long serialVersionUID = 1L;

  public ToolbarModelEvent(ToolbarModel model) {
    super(model);
  }
}
