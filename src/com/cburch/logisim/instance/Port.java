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

package com.cburch.logisim.instance;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.StringGetter;

public class Port {
	private static String defaultExclusive(String s) {
		if (s == null)
			throw new IllegalArgumentException("Null port type");
		else if (s.equals(INPUT))
			return SHARED;
		else if (s.equals(OUTPUT))
			return EXCLUSIVE;
		else if (s.equals(INOUT))
			return SHARED;
		else
			throw new IllegalArgumentException("Not recognized port type");
	}

	private static boolean toExclusive(String s) {
		if (s == null)
			throw new IllegalArgumentException("Null exclusion type");
		else if (s.equals(EXCLUSIVE))
			return true;
		else if (s.equals(SHARED))
			return false;
		else
			throw new IllegalArgumentException("Not recognized exclusion type");
	}

	private static int toType(String s) {
		if (s == null)
			throw new IllegalArgumentException("Null port type");
		else if (s.equals(INPUT))
			return EndData.INPUT_ONLY;
		else if (s.equals(OUTPUT))
			return EndData.OUTPUT_ONLY;
		else if (s.equals(INOUT))
			return EndData.INPUT_OUTPUT;
		else
			throw new IllegalArgumentException("Not recognized port type");
	}

	public static final String INPUT = "input";
	public static final String OUTPUT = "output";

	public static final String INOUT = "inout";
	public static final String EXCLUSIVE = "exclusive";
	public static final String SHARED = "shared";
	private int dx;
	private int dy;
	private int type;
	private BitWidth widthFixed;

	private Attribute<BitWidth> widthAttr;

	private boolean exclude;

	private StringGetter toolTip;

	public Port(int dx, int dy, String type, Attribute<BitWidth> attr) {
		this(dx, dy, type, attr, defaultExclusive(type));
	}

	public Port(int dx, int dy, String type, Attribute<BitWidth> attr,
			String exclude) {
		this.dx = dx;
		this.dy = dy;
		this.type = toType(type);
		this.widthFixed = null;
		this.widthAttr = attr;
		this.exclude = toExclusive(exclude);
		this.toolTip = null;
	}

	public Port(int dx, int dy, String type, BitWidth bits) {
		this(dx, dy, type, bits, defaultExclusive(type));
	}

	public Port(int dx, int dy, String type, BitWidth bits, String exclude) {
		this.dx = dx;
		this.dy = dy;
		this.type = toType(type);
		this.widthFixed = bits;
		this.widthAttr = null;
		this.exclude = toExclusive(exclude);
		this.toolTip = null;
	}

	public Port(int dx, int dy, String type, int bits) {
		this(dx, dy, type, BitWidth.create(bits), defaultExclusive(type));
	}

	public Port(int dx, int dy, String type, int bits, String exclude) {
		this(dx, dy, type, BitWidth.create(bits), exclude);
	}

	public BitWidth getFixedBitWidth() {
		return widthFixed == null ? BitWidth.UNKNOWN : widthFixed;
	}

	public String getToolTip() {
		StringGetter getter = toolTip;
		return getter == null ? null : getter.toString();
	}

	public int getType() {
		return type;
	}

	public Attribute<BitWidth> getWidthAttribute() {
		return widthAttr;
	}

	public void setToolTip(StringGetter value) {
		toolTip = value;
	}

	public EndData toEnd(Location loc, AttributeSet attrs) {
		Location pt = loc.translate(dx, dy);
		if (widthFixed != null) {
			return new EndData(pt, widthFixed, type, exclude);
		} else {
			Object val = attrs.getValue(widthAttr);
			if (!(val instanceof BitWidth)) {
				throw new IllegalArgumentException("Width attribute not set");
			}
			return new EndData(pt, (BitWidth) val, type, exclude);
		}
	}
}
