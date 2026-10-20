/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.contracts;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public interface BaseComponentListenerContract extends ComponentListener {

  @Override
  default void componentResized(ComponentEvent event) {
    // no-op implementation
  }

  @Override
  default void componentMoved(ComponentEvent event) {
    // no-op implementation
  }

  @Override
  default void componentShown(ComponentEvent event) {
    // no-op implementation
  }

  @Override
  default void componentHidden(ComponentEvent event) {
    // no-op implementation
  }
}
