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

package com.cburch.draw.model;

import com.cburch.logisim.data.Attribute;

public class AttributeMapKey {
	private Attribute<?> attr;
	private CanvasObject object;

	public AttributeMapKey(Attribute<?> attr, CanvasObject object) {
		this.attr = attr;
		this.object = object;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof AttributeMapKey))
			return false;
		AttributeMapKey o = (AttributeMapKey) other;
		return (attr == null ? o.attr == null : attr.equals(o.attr))
				&& (object == null ? o.object == null : object.equals(o.object));
	}

	public Attribute<?> getAttribute() {
		return attr;
	}

	public CanvasObject getObject() {
		return object;
	}

	@Override
	public int hashCode() {
		int a = attr == null ? 0 : attr.hashCode();
		int b = object == null ? 0 : object.hashCode();
		return a ^ b;
	}
}
