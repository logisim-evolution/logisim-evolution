/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ProbeAttributes;
import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;

/**
 * Represents a preference monitor for boolean values with additional
 * functionality to handle conversion events. This class extends the
 * behavior of the basic boolean preference monitor to facilitate
 * listeners which are notified of conversion events.
 */
public class PrefMonitorBooleanConvert extends PrefMonitorBoolean {

  /** List of listeners interested in conversion events. */
  private final ArrayList<ConvertEventListener> myListeners = new ArrayList<>();

  /**
   * Constructs a new preference monitor for boolean values with conversion capabilities.
   *
   * @param name The name of the preference.
   * @param dflt The default boolean value.
   */
  public PrefMonitorBooleanConvert(String name, boolean dflt) {
    super(name, dflt);
  }

  /**
   * Adds a listener to monitor conversion events.
   *
   * @param l The listener to add.
   */
  public void addConvertListener(ConvertEventListener l) {
    if (!myListeners.contains(l)) myListeners.add(l);
  }

  /**
   * Removes a listener from monitoring conversion events.
   *
   * @param l The listener to remove.
   */
  public void removeConvertListener(ConvertEventListener l) {
    myListeners.remove(l);
  }

  /**
   * Handles preference changes, updates the internal value if needed,
   * and fires conversion events to registered listeners.
   *
   * @param event The event indicating a preference change.
   */
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

  /**
   * Notifies all registered listeners of a conversion event.
   *
   * @param e The conversion event to broadcast.
   */
  private void fireConvertAction(ConvertEvent e) {
    for (final var l : myListeners) l.attributeValueChanged(e);
  }
}
