/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
      final var l = data.listener.get();
      if (l == null) {
        it.remove();
      } else if (data.property.equals(ALL_PROPERTIES) || data.property.equals(property)) {
        if (e == null) {
          e = new PropertyChangeEvent(source, property, oldValue, newValue);
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
