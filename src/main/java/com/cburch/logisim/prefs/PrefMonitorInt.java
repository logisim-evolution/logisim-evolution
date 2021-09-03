/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import java.util.prefs.PreferenceChangeEvent;
import lombok.val;

class PrefMonitorInt extends AbstractPrefMonitor<Integer> {
  private final int dflt;
  private int value;

  PrefMonitorInt(String name, int dflt) {
    super(name);
    this.dflt = dflt;
    this.value = dflt;
    val prefs = AppPreferences.getPrefs();
    set(prefs.getInt(name, dflt));
    prefs.addPreferenceChangeListener(this);
  }

  @Override
  public Integer get() {
    return value;
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent event) {
    val prefs = event.getNode();
    val prop = event.getKey();
    val name = getIdentifier();
    if (prop.equals(name)) {
      val oldValue = value;
      val newValue = prefs.getInt(name, dflt);
      if (newValue != oldValue) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
      }
    }
  }

  @Override
  public void set(Integer newValue) {
    val newVal = newValue;
    if (value != newVal) {
      AppPreferences.getPrefs().putInt(getIdentifier(), newVal);
    }
  }
}
