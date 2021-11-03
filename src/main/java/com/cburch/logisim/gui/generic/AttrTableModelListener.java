/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

public interface AttrTableModelListener {
  void attrStructureChanged(AttrTableModelEvent event);

  void attrTitleChanged(AttrTableModelEvent event);

  void attrValueChanged(AttrTableModelEvent event);
}
