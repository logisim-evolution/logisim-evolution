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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.util.StringUtil;

class NegateAttribute extends Attribute<Boolean> {
	private static Attribute<Boolean> BOOLEAN_ATTR = Attributes
			.forBoolean("negateDummy");

	int index;
	private Direction side;

	public NegateAttribute(int index, Direction side) {
		super("negate" + index, null);
		this.index = index;
		this.side = side;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof NegateAttribute) {
			NegateAttribute o = (NegateAttribute) other;
			return this.index == o.index && this.side == o.side;
		} else {
			return false;
		}
	}

	@Override
	public java.awt.Component getCellEditor(Boolean value) {
		return BOOLEAN_ATTR.getCellEditor(null, value);
	}

	@Override
	public String getDisplayName() {
		String ret = StringUtil.format(Strings.get("gateNegateAttr"), ""
				+ (index + 1));
		if (side != null) {
			ret += " (" + side.toVerticalDisplayString() + ")";
		}
		return ret;
	}

	@Override
	public int hashCode() {
		return index * 31 + (side == null ? 0 : side.hashCode());
	}

	@Override
	public Boolean parse(String value) {
		return BOOLEAN_ATTR.parse(value);
	}

	@Override
	public String toDisplayString(Boolean value) {
		return BOOLEAN_ATTR.toDisplayString(value);
	}

}
