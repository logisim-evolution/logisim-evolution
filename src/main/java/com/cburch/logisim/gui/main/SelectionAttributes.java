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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.UnmodifiableList;

class SelectionAttributes extends AbstractAttributeSet {

	private class Listener implements Selection.Listener, AttributeListener {

		@Override
		public void attributeListChanged(AttributeEvent e) {
			if (listening) {
				updateList(false);
			}
		}

		@Override
		public void attributeValueChanged(AttributeEvent e) {
			if (listening) {
				updateList(false);
			}
		}

		@Override
		public void selectionChanged(Selection.Event e) {
			updateList(true);
		}
	}

	private static LinkedHashMap<Attribute<Object>, Object> computeAttributes(
			Collection<Component> newSel) {
		LinkedHashMap<Attribute<Object>, Object> attrMap;
		attrMap = new LinkedHashMap<Attribute<Object>, Object>();
		Iterator<Component> sit = newSel.iterator();
		if (sit.hasNext()) {
			AttributeSet first = sit.next().getAttributeSet();
			for (Attribute<?> attr : first.getAttributes()) {
				@SuppressWarnings("unchecked")
				Attribute<Object> attrObj = (Attribute<Object>) attr;
				attrMap.put(attrObj, first.getValue(attr));
			}
			while (sit.hasNext()) {
				AttributeSet next = sit.next().getAttributeSet();
				Iterator<Attribute<Object>> ait = attrMap.keySet().iterator();
				while (ait.hasNext()) {
					Attribute<Object> attr = ait.next();
					if (next.containsAttribute(attr)) {
						Object v = attrMap.get(attr);
						if (v != null && !v.equals(next.getValue(attr))) {
							attrMap.put(attr, null);
						}
					} else {
						ait.remove();
					}
				}
			}
		}
		return attrMap;
	}

	private static boolean computeReadOnly(Collection<Component> sel,
			Attribute<?> attr) {
		for (Component comp : sel) {
			AttributeSet attrs = comp.getAttributeSet();
			if (attrs.isReadOnly(attr)) {
				return true;
			}
		}
		return false;
	}

	private static Set<Component> createSet(Collection<Component> comps) {
		boolean includeWires = true;
		for (Component comp : comps) {
			if (!(comp instanceof Wire)) {
				includeWires = false;
				break;
			}
		}

		if (includeWires) {
			return new HashSet<Component>(comps);
		} else {
			HashSet<Component> ret = new HashSet<Component>();
			for (Component comp : comps) {
				if (!(comp instanceof Wire)) {
					ret.add(comp);
				}
			}
			return ret;
		}
	}

	private static boolean haveSameElements(Collection<Component> a,
			Collection<Component> b) {
		if (a == null) {
			return b == null ? true : b.isEmpty();
		} else if (b == null) {
			return a.isEmpty();
		} else if (a.size() != b.size()) {
			return false;
		} else {
			for (Component item : a) {
				if (!b.contains(item)) {
					return false;
				}
			}
			return true;
		}
	}

	private static boolean isSame(
			LinkedHashMap<Attribute<Object>, Object> attrMap,
			Attribute<?>[] oldAttrs, Object[] oldValues) {
		if (oldAttrs.length != attrMap.size()) {
			return false;
		} else {
			int j = -1;
			for (Map.Entry<Attribute<Object>, Object> entry : attrMap
					.entrySet()) {
				j++;

				Attribute<Object> a = entry.getKey();
				if (!oldAttrs[j].equals(a) || j >= oldValues.length) {
					return false;
				}
				Object ov = oldValues[j];
				Object nv = entry.getValue();
				if (ov == null ? nv != null : !ov.equals(nv)) {
					return false;
				}
			}
			return true;
		}
	}

	private static final Attribute<?>[] EMPTY_ATTRIBUTES = new Attribute<?>[0];
	private static final Object[] EMPTY_VALUES = new Object[0];
	private Canvas canvas;
	private Selection selection;
	private Listener listener;
	private boolean listening;

	private Set<Component> selected;

	private Attribute<?>[] attrs;

	private boolean[] readOnly;

	private Object[] values;

	private List<Attribute<?>> attrsView;

	public SelectionAttributes(Canvas canvas, Selection selection) {
		this.canvas = canvas;
		this.selection = selection;
		this.listener = new Listener();
		this.listening = true;
		this.selected = Collections.emptySet();
		this.attrs = EMPTY_ATTRIBUTES;
		this.values = EMPTY_VALUES;
		this.attrsView = Collections.emptyList();

		selection.addListener(listener);
		updateList(true);
		setListening(true);
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		throw new UnsupportedOperationException("SelectionAttributes.copyInto");
	}

	private int findIndex(Attribute<?> attr) {
		if (attr == null) {
			return -1;
		}
		Attribute<?>[] as = attrs;
		for (int i = 0; i < as.length; i++) {
			if (attr.equals(as[i])) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		Circuit circ = canvas.getCircuit();
		if (selected.isEmpty() && circ != null) {
			return circ.getStaticAttributes().getAttributes();
		} else {
			return attrsView;
		}
	}

	public Selection getSelection() {
		return selection;
	}

	@Override
	public <V> V getValue(Attribute<V> attr) {
		Circuit circ = canvas.getCircuit();
		if (selected.isEmpty() && circ != null) {
			return circ.getStaticAttributes().getValue(attr);
		} else {
			int i = findIndex(attr);
			Object[] vs = values;
			@SuppressWarnings("unchecked")
			V ret = (V) (i >= 0 && i < vs.length ? vs[i] : null);
			return ret;
		}
	}

	@Override
	public boolean isReadOnly(Attribute<?> attr) {
		Project proj = canvas.getProject();
		Circuit circ = canvas.getCircuit();
		if (!proj.getLogisimFile().contains(circ)) {
			return true;
		} else if (selected.isEmpty() && circ != null) {
			return circ.getStaticAttributes().isReadOnly(attr);
		} else {
			int i = findIndex(attr);
			boolean[] ro = readOnly;
			return i >= 0 && i < ro.length ? ro[i] : true;
		}
	}

	@Override
	public boolean isToSave(Attribute<?> attr) {
		return false;
	}

	private void setListening(boolean value) {
		if (listening != value) {
			listening = value;
			if (value) {
				updateList(false);
			}
		}
	}

	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		Circuit circ = canvas.getCircuit();
		if (selected.isEmpty() && circ != null) {
			circ.getStaticAttributes().setValue(attr, value);
		} else {
			int i = findIndex(attr);
			Object[] vs = values;
			if (i >= 0 && i < vs.length) {
				vs[i] = value;
				for (Component comp : selected) {
					comp.getAttributeSet().setValue(attr, value);
				}
			}
		}
	}

	private void updateList(boolean ignoreIfSelectionSame) {
		Selection sel = selection;
		Set<Component> oldSel = selected;
		Set<Component> newSel;
		if (sel == null) {
			newSel = Collections.emptySet();
		} else {
			newSel = createSet(sel.getComponents());
		}
		if (haveSameElements(newSel, oldSel)) {
			if (ignoreIfSelectionSame) {
				return;
			}
			newSel = oldSel;
		} else {
			for (Component o : oldSel) {
				if (!newSel.contains(o)) {
					o.getAttributeSet().removeAttributeListener(listener);
				}
			}
			for (Component o : newSel) {
				if (!oldSel.contains(o)) {
					o.getAttributeSet().addAttributeListener(listener);
				}
			}
		}

		LinkedHashMap<Attribute<Object>, Object> attrMap = computeAttributes(newSel);
		boolean same = isSame(attrMap, this.attrs, this.values);

		if (same) {
			if (newSel != oldSel) {
				this.selected = newSel;
			}
		} else {
			Attribute<?>[] oldAttrs = this.attrs;
			Object[] oldValues = this.values;
			Attribute<?>[] newAttrs = new Attribute[attrMap.size()];
			Object[] newValues = new Object[newAttrs.length];
			boolean[] newReadOnly = new boolean[newAttrs.length];
			int i = -1;
			for (Map.Entry<Attribute<Object>, Object> entry : attrMap
					.entrySet()) {
				i++;
				newAttrs[i] = entry.getKey();
				newValues[i] = entry.getValue();
				newReadOnly[i] = computeReadOnly(newSel, newAttrs[i]);
			}
			if (newSel != oldSel) {
				this.selected = newSel;
			}
			this.attrs = newAttrs;
			this.attrsView = new UnmodifiableList<Attribute<?>>(newAttrs);
			this.values = newValues;
			this.readOnly = newReadOnly;

			boolean listSame = oldAttrs != null
					&& oldAttrs.length == newAttrs.length;
			if (listSame) {
				for (i = 0; i < oldAttrs.length; i++) {
					if (!oldAttrs[i].equals(newAttrs[i])) {
						listSame = false;
						break;
					}
				}
			}
			if (listSame) {
				for (i = 0; i < oldValues.length; i++) {
					Object oldVal = oldValues[i];
					Object newVal = newValues[i];
					boolean sameVals = oldVal == null ? newVal == null : oldVal
							.equals(newVal);
					if (!sameVals) {
						@SuppressWarnings("unchecked")
						Attribute<Object> attr = (Attribute<Object>) oldAttrs[i];
						fireAttributeValueChanged(attr, newVal,oldVal);
					}
				}
			} else {
				fireAttributeListChanged();
			}
		}
	}
}
