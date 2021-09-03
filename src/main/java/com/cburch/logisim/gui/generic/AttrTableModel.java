/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

public interface AttrTableModel {
  void addAttrTableModelListener(AttrTableModelListener listener);

  AttrTableModelRow getRow(int rowIndex);

  int getRowCount();

  String getTitle();

  void removeAttrTableModelListener(AttrTableModelListener listener);
}
