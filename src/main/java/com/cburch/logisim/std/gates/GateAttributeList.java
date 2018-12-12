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

package com.cburch.logisim.std.gates;

import java.util.AbstractList;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.StdAttr;

class GateAttributeList extends AbstractList<Attribute<?>> {
	private static final Attribute<?>[] BASE_ATTRIBUTES = { StdAttr.FACING,
			StdAttr.WIDTH, GateAttributes.ATTR_SIZE,
			GateAttributes.ATTR_INPUTS, GateAttributes.ATTR_OUTPUT,
			StdAttr.LABEL, StdAttr.LABEL_FONT, };

	private GateAttributes attrs;

	public GateAttributeList(GateAttributes attrs) {
		this.attrs = attrs;
	}

	@Override
	public Attribute<?> get(int index) {
		int len = BASE_ATTRIBUTES.length;
		if (index < len) {
			return BASE_ATTRIBUTES[index];
		}
		index -= len;
		if (attrs.xorBehave != null) {
			index--;
			if (index < 0)
				return GateAttributes.ATTR_XOR;
		}
		Direction facing = attrs.facing;
		int inputs = attrs.inputs;
		if (index == 0) {
			if (facing == Direction.EAST || facing == Direction.WEST) {
				return new NegateAttribute(index, Direction.NORTH);
			} else {
				return new NegateAttribute(index, Direction.WEST);
			}
		} else if (index == inputs - 1) {
			if (facing == Direction.EAST || facing == Direction.WEST) {
				return new NegateAttribute(index, Direction.SOUTH);
			} else {
				return new NegateAttribute(index, Direction.EAST);
			}
		} else if (index < inputs) {
			return new NegateAttribute(index, null);
		}
		return null;
	}

	@Override
	public int size() {
		int ret = BASE_ATTRIBUTES.length;
		if (attrs.xorBehave != null)
			ret++;
		ret += attrs.inputs;
		return ret;
	}
}
