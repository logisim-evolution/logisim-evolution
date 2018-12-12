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

package com.cburch.logisim.circuit;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.util.GraphicsUtil;

class SplitterParameters {
	private int dxEnd0; // location of split end 0 relative to origin
	private int dyEnd0;
	private int ddxEnd; // distance from split end i to split end (i + 1)
	private int ddyEnd;
	private int dxEndSpine; // distance from split end to spine
	private int dyEndSpine;
	private int dxSpine0; // distance from origin to far end of spine
	private int dySpine0;
	private int dxSpine1; // distance from origin to near end of spine
	private int dySpine1;
	private int textAngle; // angle to rotate text
	private int halign; // justification of text
	private int valign;

	SplitterParameters(SplitterAttributes attrs) {

		Object appear = attrs.appear;
		int fanout = attrs.fanout;
		Direction facing = attrs.facing;

		int justify;
		if (appear == SplitterAttributes.APPEAR_CENTER
				|| appear == SplitterAttributes.APPEAR_LEGACY) {
			justify = 0;
		} else if (appear == SplitterAttributes.APPEAR_RIGHT) {
			justify = 1;
		} else {
			justify = -1;
		}
		int width = 20;

		int offs = 6;
		if (facing == Direction.NORTH || facing == Direction.SOUTH) { // ^ or V
			int m = facing == Direction.NORTH ? 1 : -1;
			dxEnd0 = justify == 0 ? 10 * ((fanout + 1) / 2 - 1)
					: m * justify < 0 ? -10 : 10 * fanout;
			dyEnd0 = -m * width;
			ddxEnd = -10;
			ddyEnd = 0;
			dxEndSpine = 0;
			dyEndSpine = m * (width - offs);
			dxSpine0 = m * justify * (10 * fanout - 1);
			dySpine0 = -m * offs;
			dxSpine1 = m * justify * offs;
			dySpine1 = -m * offs;
			textAngle = 90;
			halign = m > 0 ? GraphicsUtil.H_RIGHT : GraphicsUtil.H_LEFT;
			valign = m * justify <= 0 ? GraphicsUtil.V_BASELINE
					: GraphicsUtil.V_TOP;
		} else { // > or <
			int m = facing == Direction.WEST ? -1 : 1;
			dxEnd0 = m * width;
			dyEnd0 = justify == 0 ? -10 * (fanout / 2) : m * justify > 0 ? 10
					: -10 * fanout;
			ddxEnd = 0;
			ddyEnd = 10;
			dxEndSpine = -m * (width - offs);
			dyEndSpine = 0;
			dxSpine0 = m * offs;
			dySpine0 = m * justify * (10 * fanout - 1);
			dxSpine1 = m * offs;
			dySpine1 = m * justify * offs;
			textAngle = 0;
			halign = m > 0 ? GraphicsUtil.H_LEFT : GraphicsUtil.H_RIGHT;
			valign = m * justify < 0 ? GraphicsUtil.V_TOP
					: GraphicsUtil.V_BASELINE;
		}
	}

	public int getEnd0X() {
		return dxEnd0;
	}

	public int getEnd0Y() {
		return dyEnd0;
	}

	public int getEndToEndDeltaX() {
		return ddxEnd;
	}

	public int getEndToEndDeltaY() {
		return ddyEnd;
	}

	public int getEndToSpineDeltaX() {
		return dxEndSpine;
	}

	public int getEndToSpineDeltaY() {
		return dyEndSpine;
	}

	public int getSpine0X() {
		return dxSpine0;
	}

	public int getSpine0Y() {
		return dySpine0;
	}

	public int getSpine1X() {
		return dxSpine1;
	}

	public int getSpine1Y() {
		return dySpine1;
	}

	public int getTextAngle() {
		return textAngle;
	}

	public int getTextHorzAlign() {
		return halign;
	}

	public int getTextVertAlign() {
		return valign;
	}
}
