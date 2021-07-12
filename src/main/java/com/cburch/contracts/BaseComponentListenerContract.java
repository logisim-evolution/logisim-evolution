package com.cburch.contracts;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public interface BaseComponentListenerContract extends ComponentListener {

  @Override
  default void componentResized(ComponentEvent event) {
    // dummy implementation
  }

  @Override
  default void componentMoved(ComponentEvent event) {
    // dummy implementation
  }

  @Override
  default void componentShown(ComponentEvent event) {
    // dummy implementation
  }

  @Override
  default void componentHidden(ComponentEvent event) {
    // dummy implementation
  }
}
