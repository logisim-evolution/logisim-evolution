/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.analyze.model;

import static com.cburch.logisim.analyze.Strings.S;

import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.StringGetter;

public class Entry implements Comparable<Entry>,PreferenceChangeListener {
  public static Entry parse(String description) {
    if (AppPreferences.FALSE_CHAR.get().charAt(0)==description.charAt(0)) return ZERO;
    if (AppPreferences.TRUE_CHAR.get().charAt(0)==description.charAt(0)) return ONE;
    if (AppPreferences.DONTCARE_CHAR.get().charAt(0)==description.charAt(0)) return DONT_CARE;
    if (AppPreferences.ERROR_CHAR.get().charAt(0)==description.charAt(0)) return BUS_ERROR;
    return null;
  }
  
  public interface EntryChangedListener {
    abstract void EntryDesriptionChanged();
  }

  public static final Entry OSCILLATE_ERROR = new Entry(-2, S.getter("oscillateError"));
  public static final Entry BUS_ERROR = new Entry(-1, S.getter("busError"));
  public static final Entry ZERO = new Entry(0, null);
  public static final Entry DONT_CARE = new Entry(1, null);
  public static final Entry ONE = new Entry(2, null);

  private int sortOrder;
  private StringGetter errorMessage;
  private ArrayList<EntryChangedListener> listeners; 

  private Entry(int sortOrder, StringGetter errorMessage) {
    this.sortOrder = sortOrder;
    this.errorMessage = errorMessage;
    listeners = new ArrayList<EntryChangedListener>();
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
    if (listeners.contains(l)) listeners.remove(l);
  }
  
  private void fireChange() {
    for (EntryChangedListener l : listeners) l.EntryDesriptionChanged();
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
    if (this == DONT_CARE || this == ZERO || this == ONE) return getDescription();
    else return "?";
  }

  @Override
  public int compareTo(Entry other) {
    return (this.sortOrder - other.sortOrder);
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent evt) {
    if ( (evt.getKey().equals(AppPreferences.ERROR_CHAR.getIdentifier()) && 
          this == BUS_ERROR) ||
         (evt.getKey().equals(AppPreferences.FALSE_CHAR.getIdentifier()) &&
          this == ZERO) ||
         (evt.getKey().equals(AppPreferences.DONTCARE_CHAR.getIdentifier()) &&
          this == DONT_CARE) ||
         (evt.getKey().equals(AppPreferences.TRUE_CHAR.getIdentifier()) &&
          this == ONE)) {
      fireChange();
    }
  }

}
