/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

abstract class AbstractPrefMonitor<E> implements PrefMonitor<E> {
  private final String name;

  AbstractPrefMonitor(String name) {
    this.name = name;
  }

  /**
   * Adds a PropertyChangeListener to the preference with the given identifier.
   *
   * @param listener The listener to be added.
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    AppPreferences.addPropertyChangeListener(name, listener);
  }

  /**
   * Retrieves the preference value as a boolean.
   * This assumes that the underlying type is a Boolean.
   *
   * @return The boolean representation of the preference value.
   */
  public boolean getBoolean() {
    return (Boolean) get();
  }

  /**
   * Retrieves the identifier for the preference.
   *
   * @return The identifier string.
   */
  public String getIdentifier() {
    return name;
  }

  /**
   * Checks if the given event corresponds to this preference based on its identifier.
   *
   * @param event The property change event.
   * @return True if the event's property name matches the preference's identifier, otherwise false.
   */
  public boolean isSource(PropertyChangeEvent event) {
    return name.equals(event.getPropertyName());
  }

  /**
   * Removes a PropertyChangeListener from the preference with the given identifier.
   *
   * @param listener The listener to be removed.
   */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    AppPreferences.removePropertyChangeListener(name, listener);
  }

  /**
   * Sets the preference's value as a boolean.
   * This assumes that the underlying type can be cast to a Boolean.
   *
   * @param value The boolean value to set.
   */
  public void setBoolean(boolean value) {
    @SuppressWarnings("unchecked")
    E valObj = (E) Boolean.valueOf(value);
    set(valObj);
  }
}
