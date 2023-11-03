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

class PrefMonitorString extends AbstractPrefMonitor<String> {
  private static boolean isSame(String a, String b) {
    return Objects.equals(a, b);
  }

  private final String dflt;

  private String value;

  /**
   * Constructor for the PrefMonitorString.
   *
   * @param name The name or identifier for the preference.
   * @param dflt The default value for the preference.
   */
  public PrefMonitorString(String name, String dflt) {
    super(name);
    this.dflt = dflt;
    final var prefs = AppPreferences.getPrefs();
    this.value = prefs.get(name, dflt);
    prefs.addPreferenceChangeListener(this);
  }

  /**
   * Retrieves the value of the preference.
   *
   * @return The current value of the preference.
   */
  public String get() {
    return value;
  }

  /**
   * Handles the change in preference value.
   * Does nothing if newValue is the same as the current value.
   *
   * @param event The preference change event.
   */
  public void preferenceChange(PreferenceChangeEvent event) {
    final var prefs = event.getNode();
    final var prop = event.getKey();
    final var name = getIdentifier();
    if (prop.equals(name)) {
      final var oldValue = value;
      final var newValue = prefs.get(name, dflt);
      if (!isSame(oldValue, newValue)) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
      }
    }
  }

  /**
   * Updates the value of the preference.
   * Does nothing if newValue is the same as the current value.
   *
   * @param newValue The new value for the preference.
   */
  public void set(String newValue) {
    final var oldValue = value;
    if (!isSame(oldValue, newValue)) {
      value = newValue;
      AppPreferences.getPrefs().put(getIdentifier(), newValue);
    }
  }
}
