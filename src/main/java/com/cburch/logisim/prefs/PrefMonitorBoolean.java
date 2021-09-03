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
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import lombok.val;

public class PrefMonitorBoolean extends AbstractPrefMonitor<Boolean> implements ActionListener {
  protected final boolean dflt;
  protected boolean value;
  private JCheckBox box;

  PrefMonitorBoolean(String name, boolean dflt) {
    super(name);
    this.dflt = dflt;
    this.value = dflt;
    Preferences prefs = AppPreferences.getPrefs();
    set(prefs.getBoolean(name, dflt));
    prefs.addPreferenceChangeListener(this);
  }

  @Override
  public Boolean get() {
    return value;
  }

  @Override
  public boolean getBoolean() {
    return value;
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent event) {
    val prefs = event.getNode();
    val prop = event.getKey();
    val name = getIdentifier();
    if (prop.equals(name)) {
      val oldValue = value;
      val newValue = prefs.getBoolean(name, dflt);
      if (newValue != oldValue) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
      }
    }
  }

  @Override
  public void set(Boolean newValue) {
    if (value != newValue) {
      AppPreferences.getPrefs().putBoolean(getIdentifier(), newValue);
    }
  }

  public JCheckBox getCheckBox() {
    box = new JCheckBox();
    box.setSelected(value);
    box.addActionListener(this);
    return box;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == box)
      set(box.isSelected());
  }
}
