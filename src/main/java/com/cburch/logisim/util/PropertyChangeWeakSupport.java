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

package com.cburch.logisim.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PropertyChangeWeakSupport {
  private static class ListenerData {
    final String property;
    final WeakReference<PropertyChangeListener> listener;

    ListenerData(String property, PropertyChangeListener listener) {
      this.property = property;
      this.listener = new WeakReference<>(listener);
    }
  }

  private static final String ALL_PROPERTIES = "ALL PROPERTIES";

  private final Object source;
  private final ConcurrentLinkedQueue<ListenerData> listeners;

  public PropertyChangeWeakSupport(Object source) {
    this.source = source;
    this.listeners = new ConcurrentLinkedQueue<>();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    addPropertyChangeListener(ALL_PROPERTIES, listener);
  }

  public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
    listeners.add(new ListenerData(property, listener));
  }

  public <T> void firePropertyChange(String property, T oldValue, T newValue) {
    PropertyChangeEvent e = null;
    for (Iterator<ListenerData> it = listeners.iterator(); it.hasNext(); ) {
      ListenerData data = it.next();
      PropertyChangeListener l = data.listener.get();
      if (l == null) {
        it.remove();
      } else if (data.property.equals(ALL_PROPERTIES) || data.property.equals(property)) {
        if (e == null) {
          e =
              new PropertyChangeEvent(
                  source, property, oldValue, newValue);
        }
        l.propertyChange(e);
      }
    }
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    removePropertyChangeListener(ALL_PROPERTIES, listener);
  }

  public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
    for (Iterator<ListenerData> it = listeners.iterator(); it.hasNext(); ) {
      ListenerData data = it.next();
      PropertyChangeListener l = data.listener.get();
      if (l == null) {
        it.remove();
      } else if (data.property.equals(property) && l == listener) {
        it.remove();
      }
    }
  }
}
