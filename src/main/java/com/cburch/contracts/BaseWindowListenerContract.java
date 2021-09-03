/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.contracts;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Dummy implementation of java.awt.event.WindowListener interface. The main purpose of this interface is to provide
 * default (empty) implementation of interface methods as, unfortunately JDKs interfaces do not come
 * with default implementation even they easily could. Implementing this interface instead of the
 * parent one allows skipping the need of implementing all, even unneeded, methods. That's saves
 * some efforts and reduces overall LOC.
 */
public interface BaseWindowListenerContract extends WindowListener {

  @Override
  default void windowOpened(WindowEvent var1) {
    // no-op implementation
  }

  @Override
  default void windowClosing(WindowEvent var1) {
    // no-op implementation
  }

  @Override
  default void windowClosed(WindowEvent var1) {
    // no-op implementation
  }

  @Override
  default void windowIconified(WindowEvent var1) {
    // no-op implementation
  }

  @Override
  default void windowDeiconified(WindowEvent var1) {
    // no-op implementation
  }

  @Override
  default void windowActivated(WindowEvent var1) {
    // no-op implementation
  }

  @Override
  default void windowDeactivated(WindowEvent var1) {
    // no-op implementation
  }
}
