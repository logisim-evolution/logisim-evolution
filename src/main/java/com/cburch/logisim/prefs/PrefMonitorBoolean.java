/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.PreferenceChangeEvent;
import javax.swing.JCheckBox;

/**
 * Represents a preference monitor for boolean values with GUI integration.
 * This class extends the basic preference monitor functionality to handle
 * a GUI checkbox representing the boolean preference value.
 */
public class PrefMonitorBoolean extends AbstractPrefMonitor<Boolean> implements ActionListener {
  /**
   * The default boolean value for this preference.
   */
  protected final boolean dflt;
  /**
   * The current value of the boolean preference.
   */
  protected boolean value;
  /**
   * GUI representation of the preference using a checkbox.
   */
  private JCheckBox box;

  /**
   * Constructs a new preference monitor for boolean values with GUI capabilities.
   *
   * @param name The name of the preference.
   * @param dflt The default boolean value.
   */
  public PrefMonitorBoolean(String name, boolean dflt) {
    super(name);
    this.dflt = dflt;
    this.value = dflt;
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getBoolean(name, dflt));
    prefs.addPreferenceChangeListener(this);
  }

  /**
   * Retrieves the current value of the boolean preference.
   */
  public Boolean get() {
    return value;
  }

  /**
   * Retrieves the current boolean value directly.
   */
  @Override
  public boolean getBoolean() {
    return value;
  }

  /**
   * Handles preference changes, updates the internal value if new
   * value is different from the current then fires conversion
   * events to registered listeners. If value is the same, does nothing.
   *
   * @param event The event indicating a preference change.
   */
  public void preferenceChange(PreferenceChangeEvent event) {
    final var prefs = event.getNode();
    final var prop = event.getKey();
    final var name = getIdentifier();
    if (prop.equals(name)) {
      final var oldValue = value;
      final var newValue = prefs.getBoolean(name, dflt);
      if (newValue != oldValue) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
      }
    }
  }

  /**
   * Sets the value of the boolean preference. Does nothing if the new value
   * is the same as the current.
   *
   * @param newValue The new value to set.
   */
  public void set(Boolean newValue) {
    if (value != newValue) {
      AppPreferences.getPrefs().putBoolean(getIdentifier(), newValue);
    }
  }

  /**
   * Retrieves the GUI representation (checkbox) of the preference.
   *
   * @return A checkbox representing the preference value.
   */
  public JCheckBox getCheckBox() {
    box = new JCheckBox();
    box.setSelected(value);
    box.addActionListener(this);
    return box;
  }

  /**
   * Handles actions performed on the checkbox, updating the preference
   * value accordingly.
   *
   * @param e The action event from the checkbox.
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == box) set(box.isSelected());
  }
}
