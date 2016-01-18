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

package com.cburch.logisim.data;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAttributeSet implements Cloneable, AttributeSet {
	private ArrayList<AttributeListener> listeners = null;

	public AbstractAttributeSet() {
	}

	public void addAttributeListener(AttributeListener l) {
		if (listeners == null)
			listeners = new ArrayList<AttributeListener>();
		listeners.add(l);
	}

	@Override
	public Object clone() {
		AbstractAttributeSet ret;
		try {
			ret = (AbstractAttributeSet) super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new UnsupportedOperationException();
		}
		ret.listeners = new ArrayList<AttributeListener>();
		this.copyInto(ret);
		return ret;
	}

	public boolean containsAttribute(Attribute<?> attr) {
		return getAttributes().contains(attr);
	}

	protected abstract void copyInto(AbstractAttributeSet dest);

	protected void fireAttributeListChanged() {
		if (listeners != null) {
			AttributeEvent event = new AttributeEvent(this);
			List<AttributeListener> ls = new ArrayList<AttributeListener>(
					listeners);
			for (AttributeListener l : ls) {
				l.attributeListChanged(event);
			}
		}
	}

	protected <V> void fireAttributeValueChanged(Attribute<? super V> attr,
			V value , V oldvalue) {
		if (listeners != null) {
			AttributeEvent event = new AttributeEvent(this, attr, value, oldvalue);
			List<AttributeListener> ls = new ArrayList<AttributeListener>(
					listeners);
			for (AttributeListener l : ls) {
				l.attributeValueChanged(event);
			}
		}
	}

	public Attribute<?> getAttribute(String name) {
		for (Attribute<?> attr : getAttributes()) {
			if (attr.getName().equals(name)) {
				return attr;
			}
		}
		return null;
	}

	public abstract List<Attribute<?>> getAttributes();

	public abstract <V> V getValue(Attribute<V> attr);

	public boolean isReadOnly(Attribute<?> attr) {
		return false;
	}

	public boolean isToSave(Attribute<?> attr) {
		return true;
	}

	public void removeAttributeListener(AttributeListener l) {
		listeners.remove(l);
		if (listeners.isEmpty())
			listeners = null;
	}

	public void setReadOnly(Attribute<?> attr, boolean value) {
		throw new UnsupportedOperationException();
	}

	public abstract <V> void setValue(Attribute<V> attr, V value);

}
