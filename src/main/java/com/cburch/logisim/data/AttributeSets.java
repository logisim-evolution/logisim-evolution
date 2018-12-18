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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AttributeSets {
	private static class FixedSet extends AbstractAttributeSet {
		private List<Attribute<?>> attrs;
		private Object[] values;
		private int readOnly = 0;

		FixedSet(Attribute<?>[] attrs, Object[] initValues) {
			if (attrs.length != initValues.length) {
				throw new IllegalArgumentException(
						"attribute and value arrays must have same length");
			}
			if (attrs.length > 32) {
				throw new IllegalArgumentException(
						"cannot handle more than 32 attributes");
			}
			this.attrs = Arrays.asList(attrs);
			this.values = initValues.clone();
		}

		@Override
		protected void copyInto(AbstractAttributeSet destSet) {
			FixedSet dest = (FixedSet) destSet;
			dest.attrs = this.attrs;
			dest.values = this.values.clone();
			dest.readOnly = this.readOnly;
		}

		@Override
		public List<Attribute<?>> getAttributes() {
			return attrs;
		}

		@Override
		public <V> V getValue(Attribute<V> attr) {
			int index = attrs.indexOf(attr);
			if (index < 0) {
				return null;
			} else {
				@SuppressWarnings("unchecked")
				V ret = (V) values[index];
				return ret;
			}
		}

		@Override
		public boolean isReadOnly(Attribute<?> attr) {
			int index = attrs.indexOf(attr);
			if (index < 0)
				return true;
			return isReadOnly(index);
		}

		private boolean isReadOnly(int index) {
			return ((readOnly >> index) & 1) == 1;
		}

		@Override
		public void setReadOnly(Attribute<?> attr, boolean value) {
			int index = attrs.indexOf(attr);
			if (index < 0)
				throw new IllegalArgumentException("attribute "
						+ attr.getName() + " absent");

			if (value)
				readOnly |= (1 << index);
			else
				readOnly &= ~(1 << index);
		}

		@Override
		public <V> void setValue(Attribute<V> attr, V value) {
			int index = attrs.indexOf(attr);
			if (index < 0)
				throw new IllegalArgumentException("attribute "
						+ attr.getName() + " absent");
			if (isReadOnly(index))
				throw new IllegalArgumentException("read only");
			    @SuppressWarnings("unchecked")
				V oldvalue = (V) values[index];
				values[index] = value;
			fireAttributeValueChanged(attr, value, oldvalue);
		}
	}

	private static class SingletonSet extends AbstractAttributeSet {
		private List<Attribute<?>> attrs;
		private Object value;
		private boolean readOnly = false;

		SingletonSet(Attribute<?> attr, Object initValue) {
			this.attrs = new ArrayList<Attribute<?>>(1);
			this.attrs.add(attr);
			this.value = initValue;
		}

		@Override
		protected void copyInto(AbstractAttributeSet destSet) {
			SingletonSet dest = (SingletonSet) destSet;
			dest.attrs = this.attrs;
			dest.value = this.value;
			dest.readOnly = this.readOnly;
		}

		@Override
		public List<Attribute<?>> getAttributes() {
			return attrs;
		}

		@Override
		public <V> V getValue(Attribute<V> attr) {
			int index = attrs.indexOf(attr);
			@SuppressWarnings("unchecked")
			V ret = (V) (index >= 0 ? value : null);
			return ret;
		}

		@Override
		public boolean isReadOnly(Attribute<?> attr) {
			return readOnly;
		}

		@Override
		public void setReadOnly(Attribute<?> attr, boolean value) {
			int index = attrs.indexOf(attr);
			if (index < 0)
				throw new IllegalArgumentException("attribute "
						+ attr.getName() + " absent");
			readOnly = value;
		}

		@Override
		public <V> void setValue(Attribute<V> attr, V value) {
			int index = attrs.indexOf(attr);
			if (index < 0)
				throw new IllegalArgumentException("attribute "
						+ attr.getName() + " absent");
			if (readOnly)
				throw new IllegalArgumentException("read only");
			    @SuppressWarnings("unchecked")
				V oldvalue = (V) this.value;
				this.value = value;
			fireAttributeValueChanged(attr, value, oldvalue);
		}
	}

	public static void copy(AttributeSet src, AttributeSet dst) {
		if (src == null || src.getAttributes() == null)
			return;
		for (Attribute<?> attr : src.getAttributes()) {
			@SuppressWarnings("unchecked")
			Attribute<Object> attrObj = (Attribute<Object>) attr;
			Object value = src.getValue(attr);
			dst.setValue(attrObj, value);
		}
	}

	public static AttributeSet fixedSet(Attribute<?>[] attrs,
			Object[] initValues) {
		if (attrs.length > 1) {
			return new FixedSet(attrs, initValues);
		} else if (attrs.length == 1) {
			return new SingletonSet(attrs[0], initValues[0]);
		} else {
			return EMPTY;
		}
	}

	public static <V> AttributeSet fixedSet(Attribute<V> attr, V initValue) {
		return new SingletonSet(attr, initValue);
	}

	public static final AttributeSet EMPTY = new AttributeSet() {
		public void addAttributeListener(AttributeListener l) {
		}

		@Override
		public Object clone() {
			return this;
		}

		public boolean containsAttribute(Attribute<?> attr) {
			return false;
		}

		public Attribute<?> getAttribute(String name) {
			return null;
		}

		public List<Attribute<?>> getAttributes() {
			return Collections.emptyList();
		}

		public <V> V getValue(Attribute<V> attr) {
			return null;
		}

		public boolean isReadOnly(Attribute<?> attr) {
			return true;
		}

		public boolean isToSave(Attribute<?> attr) {
			return true;
		}

		public void removeAttributeListener(AttributeListener l) {
		}

		public void setReadOnly(Attribute<?> attr, boolean value) {
			throw new UnsupportedOperationException();
		}

		public <V> void setValue(Attribute<V> attr, V value) {
		}
	};

	private AttributeSets() {
	}
}
