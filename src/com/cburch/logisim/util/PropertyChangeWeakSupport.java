/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PropertyChangeWeakSupport {
	private static class ListenerData {
		String property;
		WeakReference<PropertyChangeListener> listener;

		ListenerData(String property, PropertyChangeListener listener) {
			this.property = property;
			this.listener = new WeakReference<PropertyChangeListener>(listener);
		}
	}

	private static final String ALL_PROPERTIES = "ALL PROPERTIES";

	private Object source;
	private ConcurrentLinkedQueue<ListenerData> listeners;

	public PropertyChangeWeakSupport(Object source) {
		this.source = source;
		this.listeners = new ConcurrentLinkedQueue<ListenerData>();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		addPropertyChangeListener(ALL_PROPERTIES, listener);
	}

	public void addPropertyChangeListener(String property,
			PropertyChangeListener listener) {
		listeners.add(new ListenerData(property, listener));
	}

	public void firePropertyChange(String property, boolean oldValue,
			boolean newValue) {
		PropertyChangeEvent e = null;
		for (Iterator<ListenerData> it = listeners.iterator(); it.hasNext();) {
			ListenerData data = it.next();
			PropertyChangeListener l = data.listener.get();
			if (l == null) {
				it.remove();
			} else if (data.property == ALL_PROPERTIES
					|| data.property.equals(property)) {
				if (e == null) {
					e = new PropertyChangeEvent(source, property,
							Boolean.valueOf(oldValue),
							Boolean.valueOf(newValue));
				}
				l.propertyChange(e);
			}
		}
	}

	public void firePropertyChange(String property, int oldValue, int newValue) {
		PropertyChangeEvent e = null;
		for (Iterator<ListenerData> it = listeners.iterator(); it.hasNext();) {
			ListenerData data = it.next();
			PropertyChangeListener l = data.listener.get();
			if (l == null) {
				it.remove();
			} else if (data.property == ALL_PROPERTIES
					|| data.property.equals(property)) {
				if (e == null) {
					e = new PropertyChangeEvent(source, property,
							Integer.valueOf(oldValue),
							Integer.valueOf(newValue));
				}
				l.propertyChange(e);
			}
		}
	}

	public void firePropertyChange(String property, Object oldValue,
			Object newValue) {
		PropertyChangeEvent e = null;
		for (Iterator<ListenerData> it = listeners.iterator(); it.hasNext();) {
			ListenerData data = it.next();
			PropertyChangeListener l = data.listener.get();
			if (l == null) {
				it.remove();
			} else if (data.property == ALL_PROPERTIES
					|| data.property.equals(property)) {
				if (e == null) {
					e = new PropertyChangeEvent(source, property, oldValue,
							newValue);
				}
				l.propertyChange(e);
			}
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		removePropertyChangeListener(ALL_PROPERTIES, listener);
	}

	public void removePropertyChangeListener(String property,
			PropertyChangeListener listener) {
		for (Iterator<ListenerData> it = listeners.iterator(); it.hasNext();) {
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
