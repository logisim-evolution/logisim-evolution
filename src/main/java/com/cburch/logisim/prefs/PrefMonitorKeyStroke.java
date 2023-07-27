/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.prefs.PreferenceChangeEvent;

class PrefMonitorKeyStroke extends AbstractPrefMonitor<KeyStroke> {
  private final KeyStroke dflt;
  private KeyStroke value;

  private byte[] keystrokeToByteArray(KeyStroke k){
    byte[] res=new byte[8];
    int code=k.getKeyCode();
    int mod=k.getModifiers();
  }

  public PrefMonitorKeyStroke(String name, int keycode, int modifier) {
    super(name);
    this.dflt = KeyStroke.getKeyStroke(keycode, modifier);
    this.value = KeyStroke.getKeyStroke(keycode, modifier);
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getByteArray(name, dflt));
    prefs.addPreferenceChangeListener(this);
  }

  public KeyStroke get() {
    return value;
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    final var prefs = event.getNode();
    final var prop = event.getKey();
    final var name = getIdentifier();
    if (prop.equals(name)) {
      final var oldValue = value;
      final var newValue = prefs.getInt(name, dflt);
      if (newValue != oldValue) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
      }
    }
  }

  public void set(Integer newValue) {
    final var newVal = newValue;
    if (value != newVal) {
      AppPreferences.getPrefs().putInt(getIdentifier(), newVal);
    }
  }
}
