/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public class Entry implements Comparable<Entry>, PreferenceChangeListener {
  public static Entry parse(String description) {
    if (AppPreferences.FALSE_CHAR.get().charAt(0) == description.charAt(0)) return ZERO;
    if (AppPreferences.TRUE_CHAR.get().charAt(0) == description.charAt(0)) return ONE;
    if (AppPreferences.DONTCARE_CHAR.get().charAt(0) == description.charAt(0)) return DONT_CARE;
    if (AppPreferences.ERROR_CHAR.get().charAt(0) == description.charAt(0)) return BUS_ERROR;
    return null;
  }

  public interface EntryChangedListener {
    void entryDesriptionChanged();
  }

  public static final Entry OSCILLATE_ERROR = new Entry(-2, S.getter("oscillateError"));
  public static final Entry BUS_ERROR = new Entry(-1, S.getter("busError"));
  public static final Entry ZERO = new Entry(0, null);
  public static final Entry DONT_CARE = new Entry(1, null);
  public static final Entry ONE = new Entry(2, null);

  private final int sortOrder;
  private final StringGetter errorMessage;
  private final ArrayList<EntryChangedListener> listeners;

  private Entry(int sortOrder, StringGetter errorMessage) {
    this.sortOrder = sortOrder;
    this.errorMessage = errorMessage;
    listeners = new ArrayList<>();
    AppPreferences.getPrefs().addPreferenceChangeListener(this);
  }

  public String getDescription() {
    if (this == OSCILLATE_ERROR) return "@";
    if (this == BUS_ERROR) return Character.toString(AppPreferences.ERROR_CHAR.get().charAt(0));
    if (this == ZERO) return Character.toString(AppPreferences.FALSE_CHAR.get().charAt(0));
    if (this == DONT_CARE) return Character.toString(AppPreferences.DONTCARE_CHAR.get().charAt(0));
    if (this == ONE) return Character.toString(AppPreferences.TRUE_CHAR.get().charAt(0));
    return Character.toString(AppPreferences.UNKNOWN_CHAR.get().charAt(0));
  }

  public void addListener(EntryChangedListener l) {
    if (!listeners.contains(l)) listeners.add(l);
  }

  public void removeListener(EntryChangedListener l) {
    listeners.remove(l);
  }

  private void fireChange() {
    for (final var l : listeners) l.entryDesriptionChanged();
  }

  public String getErrorMessage() {
    return errorMessage == null ? null : errorMessage.toString();
  }

  public boolean isError() {
    return errorMessage != null;
  }

  @Override
  public String toString() {
    return "Entry[" + getDescription() + "]";
  }

  public String toBitString() {
    return (this == DONT_CARE || this == ZERO || this == ONE) ? getDescription() : "?";
  }

  @Override
  public int compareTo(Entry other) {
    return (this.sortOrder - other.sortOrder);
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent evt) {
    if ((evt.getKey().equals(AppPreferences.ERROR_CHAR.getIdentifier()) && this == BUS_ERROR)
        || (evt.getKey().equals(AppPreferences.FALSE_CHAR.getIdentifier()) && this == ZERO)
        || (evt.getKey().equals(AppPreferences.DONTCARE_CHAR.getIdentifier()) && this == DONT_CARE)
        || (evt.getKey().equals(AppPreferences.TRUE_CHAR.getIdentifier()) && this == ONE)) {
      fireChange();
    }
  }
}
