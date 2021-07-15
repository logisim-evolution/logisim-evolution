package com.cburch.contracts;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Dummy implementation of java.awt.event.KeyListener interface. The main purpose of this interface
 * is to provide default (empty) implementation of interface methods as, unfortunately JDKs
 * interfaces do not come with default implementation even they easily could. Implementing this
 * interface instead of the parent one allows skipping the need of implementing all, even unneeded,
 * methods. That's saves some efforts and reduces overall LOC.
 */
public interface BaseKeyListenerContract extends KeyListener {
  @Override
  default void keyTyped(KeyEvent keyEvent) {
    // no-op implementation
  }

  @Override
  default void keyPressed(KeyEvent keyEvent) {
    // no-op implementation
  }

  @Override
  default void keyReleased(KeyEvent keyEvent) {
    // no-op implementation
  }
}
