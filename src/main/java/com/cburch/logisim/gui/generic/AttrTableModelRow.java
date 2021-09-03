/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import java.awt.Component;
import java.awt.Window;

public interface AttrTableModelRow {
  Component getEditor(Window parent);

  String getLabel();

  String getValue();

  boolean isValueEditable();

  boolean multiEditCompatible(AttrTableModelRow other);

  void setValue(Window parent, Object value) throws AttrTableSetException;
}
