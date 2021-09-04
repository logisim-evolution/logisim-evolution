/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import java.util.Objects;
import java.util.prefs.PreferenceChangeEvent;
import lombok.val;

class PrefMonitorString extends AbstractPrefMonitor<String> {
  private static boolean isSame(String a, String b) {
    return Objects.equals(a, b);
  }

  private final String dflt;

  private String value;

  public PrefMonitorString(String name, String dflt) {
    super(name);
    this.dflt = dflt;
    val prefs = AppPreferences.getPrefs();
    this.value = prefs.get(name, dflt);
    prefs.addPreferenceChangeListener(this);
  }

  @Override
  public String get() {
    return value;
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent event) {
    val prefs = event.getNode();
    val prop = event.getKey();
    val name = getIdentifier();
    if (prop.equals(name)) {
      val oldValue = value;
      val newValue = prefs.get(name, dflt);
      if (!isSame(oldValue, newValue)) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
      }
    }
  }

  @Override
  public void set(String newValue) {
    val oldValue = value;
    if (!isSame(oldValue, newValue)) {
      value = newValue;
      AppPreferences.getPrefs().put(getIdentifier(), newValue);
    }
  }
}
