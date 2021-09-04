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

class PrefMonitorDouble extends AbstractPrefMonitor<Double> {
  private final double dflt;
  private double value;

  public PrefMonitorDouble(String name, double dflt) {
    super(name);
    this.dflt = dflt;
    this.value = dflt;
    val prefs = AppPreferences.getPrefs();
    set(prefs.getDouble(name, dflt));
    prefs.addPreferenceChangeListener(this);
  }

  @Override
  public Double get() {
    return value;
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent event) {
    val prefs = event.getNode();
    val prop = event.getKey();
    val name = getIdentifier();
    if (prop.equals(name)) {
      val oldValue = value;
      val newValue = prefs.getDouble(name, dflt);
      if (newValue != oldValue) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
      }
    }
  }

  @Override
  public void set(Double newValue) {
    val newVal = newValue;
    if (value != newVal) {
      AppPreferences.getPrefs().putDouble(getIdentifier(), newVal);
    }
  }
}
