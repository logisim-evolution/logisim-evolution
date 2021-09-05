/*
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

package com.cburch.logisim.prefs;

import java.util.Objects;
import java.util.prefs.PreferenceChangeEvent;

class PrefMonitorStringOpts extends AbstractPrefMonitor<String> {
  private static boolean isSame(String a, String b) {
    return Objects.equals(a, b);
  }

  private final String[] opts;
  private String value;

  private final String dflt;

  PrefMonitorStringOpts(String name, String[] opts, String dflt) {
    super(name);
    this.opts = opts;
    this.value = opts[0];
    this.dflt = dflt;
    final var prefs = AppPreferences.getPrefs();
    set(prefs.get(name, dflt));
    prefs.addPreferenceChangeListener(this);
  }

  public String get() {
    return value;
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    final var prefs = event.getNode();
    final var prop = event.getKey();
    final var name = getIdentifier();
    if (prop.equals(name)) {
      final var oldValue = value;
      final var newValue = prefs.get(name, dflt);
      if (!isSame(oldValue, newValue)) {
        String chosen = null;
        for (final var s : opts) {
          if (isSame(s, newValue)) {
            chosen = s;
            break;
          }
        }
        if (chosen == null) chosen = dflt;
        value = chosen;
        AppPreferences.firePropertyChange(name, oldValue, chosen);
      }
    }
  }

  public void set(String newValue) {
    final var oldValue = value;
    if (!isSame(oldValue, newValue)) {
      AppPreferences.getPrefs().put(getIdentifier(), newValue);
    }
  }
}
