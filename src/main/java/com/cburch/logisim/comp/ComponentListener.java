/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

public interface ComponentListener {
  default void componentInvalidated(ComponentEvent e) {
    // no-op implementation
  }

  default void endChanged(ComponentEvent e) {
    // no-op implementation
  }

  default void labelChanged(ComponentEvent e) {
    // no-op implementation
  }
}
