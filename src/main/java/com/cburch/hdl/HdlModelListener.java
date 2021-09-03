/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.hdl;

public interface HdlModelListener {

  /** Called when the content of the given model has been set. */
  void contentSet(HdlModel source);
}
