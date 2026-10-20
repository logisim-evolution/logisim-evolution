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
import java.awt.event.WindowFocusListener;

/**
 * Dummy implementation of java.awt.event.WindowFocusListener interface. The main purpose of this
 * interface is to provide default (empty) implementation of interface methods as, unfortunately
 * JDKs interfaces do not come with default implementation even they easily could. Implementing this
 * interface instead of the parent one allows skipping the need of implementing all, even unneeded,
 * methods. That's saves some efforts and reduces overall LOC.
 */
public interface BaseWindowFocusListenerContract extends WindowFocusListener {

  @Override
  default void windowGainedFocus(WindowEvent var1) {
    // dummy implementaion
  }

  @Override
  default void windowLostFocus(WindowEvent var1) {
    // dummy implementaion
  }
}
