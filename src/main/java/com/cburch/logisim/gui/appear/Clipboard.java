/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.logisim.util.PropertyChangeWeakSupport;
import java.beans.PropertyChangeListener;

final class Clipboard {
  public static final String CONTENTS_PROPERTY = "appearance";
  private static final PropertyChangeWeakSupport propertySupport = new PropertyChangeWeakSupport(Clipboard.class);
  private static ClipboardContents current = ClipboardContents.EMPTY;

  private Clipboard() {}

  //
  // PropertyChangeSource methods
  //
  public static void addPropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(listener);
  }

  public static void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(propertyName, listener);
  }

  public static ClipboardContents get() {
    return current;
  }

  public static boolean isEmpty() {
    return current == null || current.getElements().isEmpty();
  }

  public static void removePropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(listener);
  }

  public static void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(propertyName, listener);
  }

  public static void set(ClipboardContents value) {
    final var old = current;
    current = value;
    propertySupport.firePropertyChange(CONTENTS_PROPERTY, old, current);
  }
}
