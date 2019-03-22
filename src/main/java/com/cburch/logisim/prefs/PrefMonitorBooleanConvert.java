/**
 * ***************************************************************************** This file is part
 * of logisim-evolution.
 *
 * <p>logisim-evolution is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>logisim-evolution is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with
 * logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Original code by Carl Burch (http://www.cburch.com), 2011. Subsequent modifications by: +
 * College of the Holy Cross http://www.holycross.edu + Haute École Spécialisée Bernoise/Berner
 * Fachhochschule http://www.bfh.ch + Haute École du paysage, d'ingénierie et d'architecture de
 * Genève http://hepia.hesge.ch/ + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 * http://www.heig-vd.ch/
 * *****************************************************************************
 */
package com.cburch.logisim.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ProbeAttributes;
import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;

public class PrefMonitorBooleanConvert extends PrefMonitorBoolean {

  private ArrayList<ConvertEventListener> MyListeners = new ArrayList<ConvertEventListener>();

  PrefMonitorBooleanConvert(String name, boolean dflt) {
    super(name, dflt);
  }

  public void addConvertListener(ConvertEventListener l) {
    if (!MyListeners.contains(l)) MyListeners.add(l);
  }

  public void removeConvertListener(ConvertEventListener l) {
    if (MyListeners.contains(l)) MyListeners.remove(l);
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent event) {
    Preferences prefs = event.getNode();
    String prop = event.getKey();
    String name = getIdentifier();
    if (prop.equals(name)) {
      boolean oldValue = value;
      boolean newValue = prefs.getBoolean(name, dflt);
      if (newValue != oldValue) {
        value = newValue;
        AppPreferences.firePropertyChange(name, oldValue, newValue);
        if (!MyListeners.isEmpty()) {
          ConvertEvent e =
              new ConvertEvent(
                  newValue ? ProbeAttributes.APPEAR_EVOLUTION_NEW : StdAttr.APPEAR_CLASSIC);
          Object[] options = {S.get("OptionYes"), S.get("OptionNo")};
          int ret =
              JOptionPane.showOptionDialog(
                  null,
                  S.fmt("OptionConvertAllPinsProbes", e.GetValue().getDisplayGetter().toString()),
                  S.get("OptionConvertAll"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  options,
                  options[0]);
          if (ret == JOptionPane.YES_OPTION) {
            fireConvertAction(e);
          }
        }
      }
    }
  }

  private void fireConvertAction(ConvertEvent e) {
    for (ConvertEventListener l : MyListeners) l.AttributeValueChanged(e);
  }
}
