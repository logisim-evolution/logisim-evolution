/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.contracts;

import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputListener;

/**
 * Dummy implementation of javax.swing.event.MouseInputListener interface. The main purpose of this
 * interface is to provide default (empty) implementation of interface methods as, unfortunately
 * JDKs interfaces do not come with default implementation even they easily could. Implementing this
 * interface instead of the parent one allows skipping the need of implementing all, even unneeded,
 * methods. That's saves some efforts and reduces overall LOC.
 */
public interface BaseMouseInputListenerContract extends MouseInputListener {
  @Override
  // No default implementation provided intentionally.
  void mouseClicked(MouseEvent mouseEvent);

  @Override
  default void mousePressed(MouseEvent mouseEvent) {
    // no-op implementation
  }

  @Override
  default void mouseReleased(MouseEvent mouseEvent) {
    // no-op implementation
  }

  @Override
  default void mouseEntered(MouseEvent mouseEvent) {
    // no-op implementation
  }

  @Override
  default void mouseExited(MouseEvent mouseEvent) {
    // no-op implementation
  }

  @Override
  default void mouseDragged(MouseEvent mouseEvent) {
    // no-op implementation
  }

  @Override
  default void mouseMoved(MouseEvent mouseEvent) {
    // no-op implementation
  }
}
