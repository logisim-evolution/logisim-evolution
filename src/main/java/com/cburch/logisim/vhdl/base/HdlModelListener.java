/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

public interface HdlModelListener {

  /** Called when the content of the given model has been set. */
  default void contentSet(HdlModel source) {
    // no-op implementation
  }

  /** Called when the content of the given model is about to be saved. */
  default void aboutToSave(HdlModel source) {
    // no-op implementation
  }

  /** Called when the vhdl appearance has changed. */
  default void appearanceChanged(HdlModel source) {
    // no-op implementation
  }

  /** Called when the vhdl icon or name has changed. */
  default void displayChanged(HdlModel source) {
    // no-op implementation
  }
}
