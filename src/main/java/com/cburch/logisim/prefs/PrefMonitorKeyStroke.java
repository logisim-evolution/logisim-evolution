/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;


import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import javax.swing.KeyStroke;

public class PrefMonitorKeyStroke extends AbstractPrefMonitor<KeyStroke> {
  private final byte[] defaultData;
  private byte[] value;
  private String prefName;
  private boolean canModify = true;
  private boolean metaRequired = false;

  public PrefMonitorKeyStroke(String name, int keycode, int modifier) {
    super(name);
    prefName = name;
    this.defaultData = keystrokeToByteArray(
        new KeyStroke[] {KeyStroke.getKeyStroke(keycode, modifier)});
    this.value = keystrokeToByteArray(new KeyStroke[] {KeyStroke.getKeyStroke(keycode, modifier)});
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getByteArray(name, defaultData));
    prefs.addPreferenceChangeListener(this);
  }

  public PrefMonitorKeyStroke(String name, int keycode, int modifier,
                              boolean metaRequired, boolean canModify) {
    super(name);
    prefName = name;
    this.defaultData = keystrokeToByteArray(new KeyStroke[] {
        KeyStroke.getKeyStroke(keycode, modifier)});
    this.value = keystrokeToByteArray(new KeyStroke[] {KeyStroke.getKeyStroke(keycode, modifier)});
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getByteArray(name, defaultData));
    prefs.addPreferenceChangeListener(this);
    this.metaRequired = metaRequired;
    this.canModify = canModify;
  }

  public PrefMonitorKeyStroke(String name, KeyStroke[] multipleValues) {
    super(name);
    prefName = name;
    this.defaultData = keystrokeToByteArray(multipleValues);
    this.value = keystrokeToByteArray(multipleValues);
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getByteArray(name, defaultData));
    prefs.addPreferenceChangeListener(this);
  }

  public Boolean canModify() {
    return canModify;
  }

  public Boolean needMetaKey() {
    return metaRequired;
  }

  public boolean metaCheckPass(int modifier) {
    if (metaRequired) {
      return (modifier & AppPreferences.hotkeyMenuMask) == AppPreferences.hotkeyMenuMask;
    } else {
      return true;
    }
  }

  public String getName() {
    return prefName;
  }

  private byte[] keystrokeToByteArray(KeyStroke[] keyStrokes) {
    /* Little Endian */
    byte[] res = new byte[keyStrokes.length * 8];
    int cnt = 0;
    for (var k : keyStrokes) {
      int code = k.getKeyCode();
      res[cnt + 0] = (byte) (code & 0xff);
      res[cnt + 1] = (byte) ((code >> 8) & 0xff);
      res[cnt + 2] = (byte) ((code >> 16) & 0xff);
      res[cnt + 3] = (byte) ((code >> 24) & 0xff);
      int mod = k.getModifiers();
      res[cnt + 4] = (byte) (mod & 0xff);
      res[cnt + 5] = (byte) ((mod >> 8) & 0xff);
      res[cnt + 6] = (byte) ((mod >> 16) & 0xff);
      res[cnt + 7] = (byte) ((mod >> 24) & 0xff);
      cnt += 8;
    }
    return res;
  }

  private List<KeyStroke> byteArrayToKeyStroke(byte[] b) {
    /* Little Endian */
    List<KeyStroke> list = new ArrayList<>();
    for (int i = 0; i < b.length; i += 8) {
      int code = 0;
      code |= b[i + 0] & 0x00ff;
      code |= (b[i + 1] << 8) & 0xff00;
      code |= (b[i + 2] << 16) & 0xff0000;
      code |= (b[i + 3] << 24) & 0xff000000;

      int mod = 0;
      mod |= b[i + 4] & 0x00ff;
      mod |= (b[i + 5] << 8) & 0xff00;
      mod |= (b[i + 6] << 16) & 0xff0000;
      mod |= (b[i + 7] << 24) & 0xff000000;
      list.add(KeyStroke.getKeyStroke(code, mod));
    }
    return list;
  }

  @Override
  public KeyStroke get() {
    return byteArrayToKeyStroke(value).get(0);
  }

  public List<KeyStroke> getList() {
    return byteArrayToKeyStroke(value);
  }

  public KeyStroke getWithMask(int mask) {
    KeyStroke tmp = byteArrayToKeyStroke(value).get(0);
    return KeyStroke.getKeyStroke(tmp.getKeyCode(), tmp.getModifiers() | mask);
  }

  public boolean compare(int keyCode, int modifier) {
    String userString = InputEvent.getModifiersExText(modifier)
        + "+" + KeyEvent.getKeyText(keyCode);
    for (KeyStroke tmp : getList()) {
      String compareString = InputEvent.getModifiersExText(tmp.getModifiers())
          + "+" + KeyEvent.getKeyText(tmp.getKeyCode());
      if (compareString.equals(userString)) {
        return true;
      }
    }
    return false;
  }

  public String getDisplayString() {
    StringBuilder res = new StringBuilder();
    var list = getList();
    int cnt = 0;
    for (KeyStroke tmp : list) {
      String modifierString = InputEvent.getModifiersExText(tmp.getModifiers());
      if (modifierString.isEmpty()) {
        res.append(KeyEvent.getKeyText(tmp.getKeyCode()));
      } else {
        res.append(modifierString).append("+").append(KeyEvent.getKeyText(tmp.getKeyCode()));
      }
      cnt++;
      if (cnt < list.size()) {
        res.append(" / ");
      }
    }
    return res.toString();
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    final var prefs = event.getNode();
    final var prop = event.getKey();
    final var name = getIdentifier();
    if (prop.equals(name)) {
      final var oldValue = value;
      final var newValue = prefs.getByteArray(name, defaultData);
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

  @Override
  public void set(KeyStroke newValue) {
    final byte[] newVal = keystrokeToByteArray(new KeyStroke[] {newValue});
    if (value != newVal) {
      AppPreferences.getPrefs().putByteArray(getIdentifier(), newVal);
    }
  }

  public void set(KeyStroke[] newValue) {
    final byte[] newVal = keystrokeToByteArray(newValue);
    if (value != newVal) {
      AppPreferences.getPrefs().putByteArray(getIdentifier(), newVal);
    }
  }
}
