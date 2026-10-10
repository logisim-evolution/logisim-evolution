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

/**
 * Represents a preference monitor for integer values. This class listens
 * to preference changes and appropriately updates its internal value.
 */
class PrefMonitorInt extends AbstractPrefMonitor<Integer> {
  /** Default integer value for this preference monitor. */
  protected final int dflt;

  /** Current integer value of this preference monitor. */
  protected int value;

  /**
   * Constructs a new preference monitor for integer values.
   *
   * @param name The name of the preference.
   * @param dflt The default integer value.
   */
  public PrefMonitorInt(String name, int dflt) {
    super(name);
    this.dflt = dflt;
    this.value = dflt;
    final var prefs = AppPreferences.getPrefs();
    value = prefs.getInt(name, dflt);
    prefs.addPreferenceChangeListener(this);
  }

  /** Returns the key used to persist this preference. */
  protected String getPreferenceKey() {
    return getIdentifier();
  }

  /** Returns the fallback value used when the preference is not persisted. */
  protected int getDefaultValue() {
    return dflt;
  }

  /** Reloads the value from the current preference key. */
  protected void reload() {
    setValue(AppPreferences.getPrefs().getInt(getPreferenceKey(), getDefaultValue()));
  }

  /** Updates the in-memory value and notifies listeners. */
  protected void setValue(int newValue) {
    if (value != newValue) {
      final var oldValue = value;
      value = newValue;
      AppPreferences.firePropertyChange(getIdentifier(), oldValue, newValue);
    }
  }

  /**
   * Retrieves the current value of the preference.
   *
   * @return The current integer value.
   */
  public Integer get() {
    return value;
  }

  /**
   * Handles preference changes and updates the internal value if needed.
   * Does nothing if newValue is the same as the current value.
   *
   * @param event The event indicating a preference change.
   */
  public void preferenceChange(PreferenceChangeEvent event) {
    final var prefs = event.getNode();
    final var prop = event.getKey();
    final var preferenceKey = getPreferenceKey();
    if (prop.equals(preferenceKey)) {
      setValue(prefs.getInt(preferenceKey, getDefaultValue()));
    }
  }

  /**
   * Sets the preference value.
   * Does nothing if newValue is the same as the current value.
   *
   * @param newValue The new integer value to set.
   */
  public void set(Integer newValue) {
    final var newVal = newValue;
    if (value != newVal) {
      final var oldValue = value;
      value = newVal;
      AppPreferences.getPrefs().putInt(getPreferenceKey(), newVal);
      AppPreferences.firePropertyChange(getIdentifier(), oldValue, newVal);
    }
  }
}
