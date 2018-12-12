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

package com.cburch.draw.tools;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.UnmodifiableList;

public class DrawingAttributeSet implements AttributeSet, Cloneable {
	private class Restriction extends AbstractAttributeSet implements
			AttributeListener {
		private AbstractTool tool;
		private List<Attribute<?>> selectedAttrs;
		private List<Attribute<?>> selectedView;

		Restriction(AbstractTool tool) {
			this.tool = tool;
			updateAttributes();
		}

		//
		// AttributeListener methods
		//
		public void attributeListChanged(AttributeEvent e) {
			fireAttributeListChanged();
		}

		public void attributeValueChanged(AttributeEvent e) {
			if (selectedAttrs.contains(e.getAttribute())) {
				@SuppressWarnings("unchecked")
				Attribute<Object> attr = (Attribute<Object>) e.getAttribute();
				fireAttributeValueChanged(attr, e.getValue(),e.getOldValue());
			}
			updateAttributes();
		}

		@Override
		protected void copyInto(AbstractAttributeSet dest) {
			DrawingAttributeSet.this.addAttributeListener(this);
		}

		@Override
		public List<Attribute<?>> getAttributes() {
			return selectedView;
		}

		@Override
		public <V> V getValue(Attribute<V> attr) {
			return DrawingAttributeSet.this.getValue(attr);
		}

		@Override
		public <V> void setValue(Attribute<V> attr, V value) {
			DrawingAttributeSet.this.setValue(attr, value);
			updateAttributes();
		}

		private void updateAttributes() {
			List<Attribute<?>> toolAttrs;
			if (tool == null) {
				toolAttrs = Collections.emptyList();
			} else {
				toolAttrs = tool.getAttributes();
			}
			if (!toolAttrs.equals(selectedAttrs)) {
				selectedAttrs = new ArrayList<Attribute<?>>(toolAttrs);
				selectedView = Collections.unmodifiableList(selectedAttrs);
				DrawingAttributeSet.this.addAttributeListener(this);
				fireAttributeListChanged();
			}
		}
	}

	static final List<Attribute<?>> ATTRS_ALL = UnmodifiableList
			.create(new Attribute<?>[] { DrawAttr.FONT, DrawAttr.ALIGNMENT,
					DrawAttr.PAINT_TYPE, DrawAttr.STROKE_WIDTH,
					DrawAttr.STROKE_COLOR, DrawAttr.FILL_COLOR,
					DrawAttr.TEXT_DEFAULT_FILL, DrawAttr.CORNER_RADIUS });

	static final List<Object> DEFAULTS_ALL = Arrays.asList(new Object[] {
			DrawAttr.DEFAULT_FONT, DrawAttr.ALIGN_CENTER,
			DrawAttr.PAINT_STROKE, Integer.valueOf(1), Color.BLACK,
			Color.WHITE, Color.BLACK, Integer.valueOf(10) });

	private EventSourceWeakSupport<AttributeListener> listeners;
	private List<Attribute<?>> attrs;
	private List<Object> values;

	public DrawingAttributeSet() {
		listeners = new EventSourceWeakSupport<AttributeListener>();
		attrs = ATTRS_ALL;
		values = DEFAULTS_ALL;
	}

	public void addAttributeListener(AttributeListener l) {
		listeners.add(l);
	}

	public <E extends CanvasObject> E applyTo(E drawable) {
		AbstractCanvasObject d = (AbstractCanvasObject) drawable;
		// use a for(i...) loop since the attribute list may change as we go on
		for (int i = 0; i < d.getAttributes().size(); i++) {
			Attribute<?> attr = d.getAttributes().get(i);
			@SuppressWarnings("unchecked")
			Attribute<Object> a = (Attribute<Object>) attr;
			if (attr == DrawAttr.FILL_COLOR
					&& this.containsAttribute(DrawAttr.TEXT_DEFAULT_FILL)) {
				d.setValue(a, this.getValue(DrawAttr.TEXT_DEFAULT_FILL));
			} else if (this.containsAttribute(a)) {
				Object value = this.getValue(a);
				d.setValue(a, value);
			}
		}
		return drawable;
	}

	@Override
	public Object clone() {
		try {
			DrawingAttributeSet ret = (DrawingAttributeSet) super.clone();
			ret.listeners = new EventSourceWeakSupport<AttributeListener>();
			ret.values = new ArrayList<Object>(this.values);
			return ret;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public boolean containsAttribute(Attribute<?> attr) {
		return attrs.contains(attr);
	}

	public AttributeSet createSubset(AbstractTool tool) {
		return new Restriction(tool);
	}

	public Attribute<?> getAttribute(String name) {
		for (Attribute<?> attr : attrs) {
			if (attr.getName().equals(name))
				return attr;
		}
		return null;
	}

	public List<Attribute<?>> getAttributes() {
		return attrs;
	}

	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		Iterator<Attribute<?>> ait = attrs.iterator();
		Iterator<Object> vit = values.iterator();
		while (ait.hasNext()) {
			Object a = ait.next();
			Object v = vit.next();
			if (a.equals(attr)) {
				return (V) v;
			}
		}
		return null;
	}

	public boolean isReadOnly(Attribute<?> attr) {
		return false;
	}

	public boolean isToSave(Attribute<?> attr) {
		return true;
	}

	public void removeAttributeListener(AttributeListener l) {
		listeners.remove(l);
	}

	public void setReadOnly(Attribute<?> attr, boolean value) {
		throw new UnsupportedOperationException("setReadOnly");
	}

	public <V> void setValue(Attribute<V> attr, V value) {
		Iterator<Attribute<?>> ait = attrs.iterator();
		ListIterator<Object> vit = values.listIterator();
		while (ait.hasNext()) {
			Object a = ait.next();
			vit.next();
			if (a.equals(attr)) {
				vit.set(value);
				AttributeEvent e = new AttributeEvent(this, attr, value,null);
				for (AttributeListener listener : listeners) {
					listener.attributeValueChanged(e);
				}
				if (attr == DrawAttr.PAINT_TYPE) {
					e = new AttributeEvent(this);
					for (AttributeListener listener : listeners) {
						listener.attributeListChanged(e);
					}
				}
				return;
			}
		}
		throw new IllegalArgumentException(attr.toString());
	}
}
