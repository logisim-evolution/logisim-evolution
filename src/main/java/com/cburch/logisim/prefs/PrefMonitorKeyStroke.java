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

public class PrefMonitorKeyStroke extends AbstractPrefMonitor<KeyStroke> {
  private final byte[] dflt;
  private byte[] value;

  private String _name;

  public PrefMonitorKeyStroke(String name, int keycode, int modifier) {
    super(name);
    _name=name;
    this.dflt = keystrokeToByteArray(KeyStroke.getKeyStroke(keycode, modifier));
    this.value = keystrokeToByteArray(KeyStroke.getKeyStroke(keycode, modifier));
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getByteArray(name, dflt));
    prefs.addPreferenceChangeListener(this);
  }

  public String getName(){
    return _name;
  }
  private byte[] keystrokeToByteArray(KeyStroke k){
    /* Little Endian */
    byte[] res=new byte[8];
    int code=k.getKeyCode();
    int mod=k.getModifiers();
    res[0]=(byte)(code);
    res[1]=(byte)(code>>8);
    res[2]=(byte)(code>>16);
    res[3]=(byte)(code>>24);
    res[4]=(byte)(mod);
    res[5]=(byte)(mod>>8);
    res[6]=(byte)(mod>>16);
    res[7]=(byte)(mod>>24);
    return res;
  }

  private KeyStroke byteArrayToKeyStroke(byte[] b){
    /* Little Endian */
    int code=0,mod=0;
    code|=b[0];
    code|=(b[1]<<8);
    code|=(b[2]<<16);
    code|=(b[3]<<24);

    mod|=b[4];
    mod|=(b[5]<<8);
    mod|=(b[6]<<16);
    mod|=(b[7]<<24);
    return KeyStroke.getKeyStroke(code,mod);
  }
  public KeyStroke get() {
    return byteArrayToKeyStroke(value);
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    final var prefs = event.getNode();
    final var prop = event.getKey();
    final var name = getIdentifier();
    if (prop.equals(name)) {
      final var oldValue = value;
      final var newValue = prefs.getByteArray(name, dflt);
      if (newValue != oldValue) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
      }
    }
  }

  public void set(byte[] newValue) {
    final var newVal = newValue;
    if (value != newVal) {
      AppPreferences.getPrefs().putByteArray(getIdentifier(), newVal);
    }
  }

  public void set(KeyStroke newValue) {
    final byte[] newVal = keystrokeToByteArray(newValue);
    if (value != newVal) {
      AppPreferences.getPrefs().putByteArray(getIdentifier(), newVal);
    }
  }
}
