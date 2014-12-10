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

package com.cburch.logisim.tools.key;

import java.awt.event.KeyEvent;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;

public abstract class NumericConfigurator<V> implements KeyConfigurator,
		Cloneable {
	private static final int MAX_TIME_KEY_LASTS = 800;

	private Attribute<V> attr;
	private int minValue;
	private int maxValue;
	private int curValue;
	private int radix;
	private int modsEx;
	private long whenTyped;

	public NumericConfigurator(Attribute<V> attr, int min, int max,
			int modifiersEx) {
		this(attr, min, max, modifiersEx, 10);
	}

	public NumericConfigurator(Attribute<V> attr, int min, int max,
			int modifiersEx, int radix) {
		this.attr = attr;
		this.minValue = min;
		this.maxValue = max;
		this.radix = radix;
		this.modsEx = modifiersEx;
		this.curValue = 0;
		this.whenTyped = 0;
	}

	@Override
	public NumericConfigurator<V> clone() {
		try {
			@SuppressWarnings("unchecked")
			NumericConfigurator<V> ret = (NumericConfigurator<V>) super.clone();
			ret.whenTyped = 0;
			ret.curValue = 0;
			return ret;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected abstract V createValue(int value);

	protected int getMaximumValue(AttributeSet attrs) {
		return maxValue;
	}

	protected int getMinimumValue(AttributeSet attrs) {
		return minValue;
	}

	public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
		if (event.getType() == KeyConfigurationEvent.KEY_TYPED) {
			KeyEvent e = event.getKeyEvent();
			int digit = Character.digit(e.getKeyChar(), radix);
			if (digit >= 0 && e.getModifiersEx() == modsEx) {
				long now = System.currentTimeMillis();
				long sinceLast = now - whenTyped;
				AttributeSet attrs = event.getAttributeSet();
				int min = getMinimumValue(attrs);
				int max = getMaximumValue(attrs);
				int val = 0;
				if (sinceLast < MAX_TIME_KEY_LASTS) {
					val = radix * curValue;
					if (val > max) {
						val = 0;
					}
				}
				val += digit;
				if (val > max) {
					val = digit;
					if (val > max) {
						return null;
					}
				}
				event.consume();
				whenTyped = now;
				curValue = val;

				if (val >= min) {
					Object valObj = createValue(val);
					return new KeyConfigurationResult(event, attr, valObj);
				}
			}
		}
		return null;
	}
}
