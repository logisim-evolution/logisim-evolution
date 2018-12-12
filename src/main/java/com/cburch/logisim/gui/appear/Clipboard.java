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

package com.cburch.logisim.gui.appear;

import java.beans.PropertyChangeListener;

import com.cburch.logisim.util.PropertyChangeWeakSupport;

class Clipboard {
	//
	// PropertyChangeSource methods
	//
	public static void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}

	public static void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(propertyName, listener);
	}

	public static ClipboardContents get() {
		return current;
	}

	public static boolean isEmpty() {
		return current == null || current.getElements().isEmpty();
	}

	public static void removePropertyChangeListener(
			PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}

	public static void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(propertyName, listener);
	}

	public static void set(ClipboardContents value) {
		ClipboardContents old = current;
		current = value;
		propertySupport.firePropertyChange(contentsProperty, old, current);
	}

	public static final String contentsProperty = "appearance";
	private static ClipboardContents current = ClipboardContents.EMPTY;
	private static PropertyChangeWeakSupport propertySupport = new PropertyChangeWeakSupport(
			Clipboard.class);

	private Clipboard() {
	}
}
