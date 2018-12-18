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

package com.cburch.logisim.gui.main;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Tunnel;
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

	public static Clipboard get() {
		return current;
	}

	public static boolean isEmpty() {
		return current == null || current.components.isEmpty();
	}

	public static void removePropertyChangeListener(
			PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}

	public static void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(propertyName, listener);
	}

	public static void set(Clipboard value) {
		Clipboard old = current;
		current = value;
		propertySupport.firePropertyChange(contentsProperty, old, current);
	}

	public static void set(Selection value, AttributeSet oldAttrs, boolean ClearLabels) {
		set(new Clipboard(value, oldAttrs, ClearLabels));
	}

	public static final String contentsProperty = "contents";
	private static Clipboard current = null;
	private static PropertyChangeWeakSupport propertySupport = new PropertyChangeWeakSupport(
			Clipboard.class);

	//
	// instance variables and methods
	//
	private HashSet<Component> components;
	private AttributeSet oldAttrs;
	private AttributeSet newAttrs;
	/*
	 * This function is in charge of copy paste.
	 * Now the tunnels' labels are not cleared except if it is requested to.
	 */
	private Clipboard(Selection sel, AttributeSet viewAttrs , boolean ClearLabels) {
		components = new HashSet<Component>();
		oldAttrs = null;
		newAttrs = null;
		for (Component base : sel.getComponents()) {
			AttributeSet baseAttrs = base.getAttributeSet();
			AttributeSet copyAttrs = (AttributeSet) baseAttrs.clone();
			/* We clear all labels on the Clipboard */
			if (copyAttrs.containsAttribute(StdAttr.LABEL)&&ClearLabels) {
				if (!(base.getFactory() instanceof Tunnel)) {
					copyAttrs.setValue(StdAttr.LABEL, "");
				}
			}
			
			Component copy = base.getFactory().createComponent(
					base.getLocation(), copyAttrs);
			components.add(copy);
			if (baseAttrs == viewAttrs) {
				oldAttrs = baseAttrs;
				newAttrs = copyAttrs;
			}
		}
	}
	
	public void ClearLabels() {
		for (Component comp : components) {
			AttributeSet attrs = comp.getAttributeSet();
			if (comp.getFactory() instanceof Tunnel) {
				continue;
			}
			
			if (attrs.containsAttribute(StdAttr.LABEL)) {
				attrs.setValue(StdAttr.LABEL, "");
			}
		}
	}

	public Collection<Component> getComponents() {
		return components;
	}

	public AttributeSet getNewAttributeSet() {
		return newAttrs;
	}

	public AttributeSet getOldAttributeSet() {
		return oldAttrs;
	}

	void setOldAttributeSet(AttributeSet value) {
		oldAttrs = value;
	}
}
