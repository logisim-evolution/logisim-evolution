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

class PrefMonitorDouble extends AbstractPrefMonitor<Double> {
  private final double dflt;
  private double value;

  public PrefMonitorDouble(String name, double dflt) {
    super(name);
    this.dflt = dflt;
    this.value = dflt;
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getDouble(name, dflt));
    prefs.addPreferenceChangeListener(this);
  }

  public Double get() {
    return value;
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    final var prefs = event.getNode();
    final var prop = event.getKey();
    final var name = getIdentifier();
    if (prop.equals(name)) {
      double oldValue = value;
      double newValue = prefs.getDouble(name, dflt);
      if (newValue != oldValue) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
      }
    }
  }

  public void set(Double newValue) {
    double newVal = newValue;
    if (value != newVal) {
      AppPreferences.getPrefs().putDouble(getIdentifier(), newVal);
    }
  }
}
