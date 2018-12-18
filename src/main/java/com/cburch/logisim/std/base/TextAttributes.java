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

package com.cburch.logisim.std.base;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;

class TextAttributes extends AbstractAttributeSet {
	private static final List<Attribute<?>> ATTRIBUTES = Arrays
			.asList(new Attribute<?>[] { Text.ATTR_TEXT, Text.ATTR_FONT,
					Text.ATTR_HALIGN, Text.ATTR_VALIGN });

	private String text;
	private Font font;
	private AttributeOption halign;
	private AttributeOption valign;
	private Bounds offsetBounds;

	public TextAttributes() {
		text = "";
		font = StdAttr.DEFAULT_LABEL_FONT;
		halign = Text.ATTR_HALIGN.parse("center");
		valign = Text.ATTR_VALIGN.parse("base");
		offsetBounds = null;
	}

	@Override
	protected void copyInto(AbstractAttributeSet destObj) {
		; // nothing to do
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return ATTRIBUTES;
	}

	Font getFont() {
		return font;
	}

	int getHorizontalAlign() {
		return ((Integer) halign.getValue()).intValue();
	}

	Bounds getOffsetBounds() {
		return offsetBounds;
	}

	String getText() {
		return text;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		if (attr == Text.ATTR_TEXT)
			return (V) text;
		if (attr == Text.ATTR_FONT)
			return (V) font;
		if (attr == Text.ATTR_HALIGN)
			return (V) halign;
		if (attr == Text.ATTR_VALIGN)
			return (V) valign;
		return null;
	}

	int getVerticalAlign() {
		return ((Integer) valign.getValue()).intValue();
	}

	boolean setOffsetBounds(Bounds value) {
		Bounds old = offsetBounds;
		boolean same = old == null ? value == null : old.equals(value);
		if (!same) {
			offsetBounds = value;
		}
		return !same;
	}

	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == Text.ATTR_TEXT) {
			text = (String) value;
		} else if (attr == Text.ATTR_FONT) {
			font = (Font) value;
		} else if (attr == Text.ATTR_HALIGN) {
			halign = (AttributeOption) value;
		} else if (attr == Text.ATTR_VALIGN) {
			valign = (AttributeOption) value;
		} else {
			throw new IllegalArgumentException("unknown attribute");
		}
		offsetBounds = null;
		fireAttributeValueChanged(attr, value,null);
	}

}
