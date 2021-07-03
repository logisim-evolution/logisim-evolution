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

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ProbeAttributes;
import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;

public class PrefMonitorBooleanConvert extends PrefMonitorBoolean {

  private final ArrayList<ConvertEventListener> myListeners = new ArrayList<>();

  PrefMonitorBooleanConvert(String name, boolean dflt) {
    super(name, dflt);
  }

  public void addConvertListener(ConvertEventListener l) {
    if (!myListeners.contains(l)) myListeners.add(l);
  }

  public void removeConvertListener(ConvertEventListener l) {
    myListeners.remove(l);
  }

  @Override
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
        if (!myListeners.isEmpty()) {
          final var e =
              new ConvertEvent(
                  newValue ? ProbeAttributes.APPEAR_EVOLUTION_NEW : StdAttr.APPEAR_CLASSIC);
          Object[] options = {S.get("OptionYes"), S.get("OptionNo")};
          int ret =
              OptionPane.showOptionDialog(
                  null,
                  S.get("OptionConvertAllPinsProbes", e.getValue().getDisplayGetter().toString()),
                  S.get("OptionConvertAll"),
                  OptionPane.YES_NO_OPTION,
                  OptionPane.QUESTION_MESSAGE,
                  null,
                  options,
                  options[0]);
          if (ret == OptionPane.YES_OPTION) {
            fireConvertAction(e);
          }
        }
      }
    }
  }

  private void fireConvertAction(ConvertEvent e) {
    for (ConvertEventListener l : myListeners) l.attributeValueChanged(e);
  }
}
