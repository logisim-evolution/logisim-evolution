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

/**
 * Class responsible for monitoring and managing keystroke preferences.
 */
public class PrefMonitorKeyStroke extends AbstractPrefMonitor<KeyStroke> {
  private final byte[] defaultData;
  private byte[] value;
  private String prefName;
  private boolean canModify = true;
  private boolean metaRequired = false;

  /**
   * Constructor initializes a preference monitor with a given keystroke.
   *
   * @param name     The preference's name.
   * @param keycode  Key code of the keystroke.
   * @param modifier Modifiers applied to the keystroke.
   */
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

  /**
   * Constructor initializes a preference monitor with a given keystroke,
   * meta-key requirement and modifiability status.
   *
   * @param name         The preference's name.
   * @param keycode      Key code of the keystroke.
   * @param modifier     Modifiers applied to the keystroke.
   * @param metaRequired Determines whether a meta key is required.
   * @param canModify    Determines whether the keystroke can be modified.
   */
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

  /**
   * Constructor initializes a preference monitor with multiple keystrokes.
   *
   * @param name          The preference's name.
   * @param multipleValues Array of KeyStrokes.
   */
  public PrefMonitorKeyStroke(String name, KeyStroke[] multipleValues) {
    super(name);
    prefName = name;
    this.defaultData = keystrokeToByteArray(multipleValues);
    this.value = keystrokeToByteArray(multipleValues);
    final var prefs = AppPreferences.getPrefs();
    set(prefs.getByteArray(name, defaultData));
    prefs.addPreferenceChangeListener(this);
  }

  /**
   * Checks if the keystroke can be modified.
   *
   * @return True if modifiable, else false.
   */
  public Boolean canModify() {
    return canModify;
  }

  /**
   * Checks if a meta-key is required.
   *
   * @return True if meta-key is required, else false.
   */
  public Boolean needMetaKey() {
    return metaRequired;
  }

  /**
   * Validates if the given modifier passes the meta check.
   *
   * @param modifier Modifiers applied.
   *
   * @return True if check is passed, else false.
   */
  public boolean metaCheckPass(int modifier) {
    if (metaRequired) {
      return (modifier & AppPreferences.hotkeyMenuMask) == AppPreferences.hotkeyMenuMask;
    } else {
      return true;
    }
  }

  /**
   * Gets the preference's name.
   */
  public String getName() {
    return prefName;
  }

  /**
   * Converts an array of KeyStroke objects to a byte array representation.
   * The conversion uses a Little Endian format.
   *
   * @param keyStrokes An array of KeyStroke objects to be converted.
   *
   * @return A byte array representing the provided KeyStrokes.
   */
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

  /**
   * Converts a byte array representation of KeyStrokes to a list of KeyStroke objects.
   * The conversion assumes a Little Endian format.
   *
   * @param b A byte array representing the KeyStrokes.
   *
   * @return A list of KeyStroke objects derived from the byte array.
   */
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

  /**
   * Gets the current keystroke value.
   *
   * @return Current KeyStroke value.
   */
  @Override
  public KeyStroke get() {
    return byteArrayToKeyStroke(value).get(0);
  }

  /**
   * Retrieves the list of KeyStrokes.
   *
   * @return List of KeyStrokes.
   */
  public List<KeyStroke> getList() {
    return byteArrayToKeyStroke(value);
  }

  /**
   * Retrieves the KeyStroke with the provided mask.
   *
   * @param mask Modifier mask.
   *
   * @return KeyStroke with given mask.
   */
  public KeyStroke getWithMask(int mask) {
    final var tmp = byteArrayToKeyStroke(value).get(0);
    return KeyStroke.getKeyStroke(tmp.getKeyCode(), tmp.getModifiers() | mask);
  }


  /**
   * Compares the provided key code and modifier with the stored keystrokes.
   *
   * @param keyCode   Key code to compare.
   * @param modifier  Modifier to compare.
   *
   * @return True if a match is found, else false.
   */
  public boolean compare(int keyCode, int modifier) {
    final var userString = InputEvent.getModifiersExText(modifier)
        + "+" + KeyEvent.getKeyText(keyCode);
    for (KeyStroke tmp : getList()) {
      final var compareString = InputEvent.getModifiersExText(tmp.getModifiers())
          + "+" + KeyEvent.getKeyText(tmp.getKeyCode());
      if (compareString.equals(userString)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Provides a display-friendly string representation of the stored keystrokes.
   *
   * @return Display string.
   */
  public String getDisplayString() {
    final var res = new StringBuilder();
    var list = getList();
    int cnt = 0;
    for (KeyStroke tmp : list) {
      final var modifierString = InputEvent.getModifiersExText(tmp.getModifiers());
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

  /**
   * Handles the preference change events.
   * Does nothing if newValue is the same as the current value.
   *
   *
   * @param event PreferenceChangeEvent object.
   */
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

  /**
   * Sets the value of the preference using a byte array.
   * Does nothing if newValue is the same as the current value.
   *
   * @param newValue New value in byte array format.
   */
  public void set(byte[] newValue) {
    if (value != newValue) {
      AppPreferences.getPrefs().putByteArray(getIdentifier(), newValue);
    }
  }

  /**
   * Sets the value of the preference using a single KeyStroke.
   * Does nothing if newValue is the same as the current value.
   *
   * @param newValue New KeyStroke value.
   */
  @Override
  public void set(KeyStroke newValue) {
    final byte[] newVal = keystrokeToByteArray(new KeyStroke[] {newValue});
    if (value != newVal) {
      AppPreferences.getPrefs().putByteArray(getIdentifier(), newVal);
    }
  }

  /**
   * Sets the value of the preference using an array of KeyStrokes.
   * Does nothing if newValue is the same as the current value.
   *
   * @param newValue Array of new KeyStroke values.
   */
  public void set(KeyStroke[] newValue) {
    final byte[] newVal = keystrokeToByteArray(newValue);
    if (value != newVal) {
      AppPreferences.getPrefs().putByteArray(getIdentifier(), newVal);
    }
  }
}
