/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;


import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.prefs.PreferenceChangeEvent;

public class PrefMonitorKeyStroke extends AbstractPrefMonitor<KeyStroke> {
  private final byte[] dflt;
  private byte[] value;
  private String pName;
  private boolean canModify = true;

  public PrefMonitorKeyStroke(String name, int keycode, int modifier) {
    super(name);
    pName = name;
    this.dflt = keystrokeToByteArray(KeyStroke.getKeyStroke(keycode, modifier));
    this.value = keystrokeToByteArray(KeyStroke.getKeyStroke(keycode, modifier));
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getByteArray(name, dflt));
    prefs.addPreferenceChangeListener(this);
  }

  public PrefMonitorKeyStroke(String name, int keycode, int modifier, boolean canModify) {
    super(name);
    pName = name;
    this.dflt = keystrokeToByteArray(KeyStroke.getKeyStroke(keycode, modifier));
    this.value = keystrokeToByteArray(KeyStroke.getKeyStroke(keycode, modifier));
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getByteArray(name, dflt));
    prefs.addPreferenceChangeListener(this);
    this.canModify = canModify;
  }

  public Boolean canModify() {
    return canModify;
  }

  public String getName() {
    return pName;
  }

  private byte[] keystrokeToByteArray(KeyStroke k) {
    /* Little Endian */
    byte[] res = new byte[8];
    int code = k.getKeyCode();
    int mod = k.getModifiers();
    res[0] = (byte) (code & 0xff);
    res[1] = (byte) ((code >> 8) & 0xff);
    res[2] = (byte) ((code >> 16) & 0xff);
    res[3] = (byte) ((code >> 24) & 0xff);
    res[4] = (byte) (mod & 0xff);
    res[5] = (byte) ((mod >> 8) & 0xff);
    res[6] = (byte) ((mod >> 16) & 0xff);
    res[7] = (byte) ((mod >> 24) & 0xff);
    return res;
  }

  private KeyStroke byteArrayToKeyStroke(byte[] b) {
    /* Little Endian */
    int code = 0;
    code |= b[0] & 0xff;
    code |= (b[1] << 8) & 0xff;
    code |= (b[2] << 16) & 0xff;
    code |= (b[3] << 24) & 0xff;

    int mod = 0;
    mod |= b[4] & 0xff;
    mod |= (b[5] << 8) & 0xff;
    mod |= (b[6] << 16) & 0xff;
    mod |= (b[7] << 24) & 0xff;
    return KeyStroke.getKeyStroke(code, mod);
  }

  public KeyStroke get() {
    return byteArrayToKeyStroke(value);
  }

  public KeyStroke getWithMask(int mask) {
    KeyStroke tmp = byteArrayToKeyStroke(value);
    return KeyStroke.getKeyStroke(tmp.getKeyCode(), tmp.getModifiers() | mask);
  }

  public String getCompareString() {
    KeyStroke tmp = byteArrayToKeyStroke(this.value);
    return InputEvent.getModifiersExText(tmp.getModifiers())
        + " + " + KeyEvent.getKeyText(tmp.getKeyCode());
  }

  public String getDisplayString() {
    KeyStroke tmp = byteArrayToKeyStroke(this.value);
    String modifierString = InputEvent.getModifiersExText(tmp.getModifiers());
    if (modifierString.equals("")) {
      return KeyEvent.getKeyText(tmp.getKeyCode());
    }
    return modifierString + "+" + KeyEvent.getKeyText(tmp.getKeyCode());
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
