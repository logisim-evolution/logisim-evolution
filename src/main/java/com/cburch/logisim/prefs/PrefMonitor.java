/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public interface PrefMonitor<E> extends PreferenceChangeListener {
  void addPropertyChangeListener(PropertyChangeListener listener);

  E get();

  boolean getBoolean();

  String getIdentifier();

  boolean isSource(PropertyChangeEvent event);

  @Override
  void preferenceChange(PreferenceChangeEvent e);

  void removePropertyChangeListener(PropertyChangeListener listener);

  void set(E value);

  void setBoolean(boolean value);
}
