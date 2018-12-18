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

package com.cburch.logisim.util;

import java.awt.FontMetrics;

public class StringUtil {
	public static StringGetter constantGetter(final String value) {
		return new StringGetter() {
			public String toString() {
				return value;
			}
		};
	}

	public static String format(String fmt, String... args) {
		return String.format(fmt, (Object[]) args);
	}

	public static StringGetter formatter(final StringGetter base,
			final String arg) {
		return new StringGetter() {
			public String toString() {
				return format(base.toString(), arg);
			}
		};
	}

	public static StringGetter formatter(final StringGetter base,
			final StringGetter arg) {
		return new StringGetter() {
			public String toString() {
				return format(base.toString(), arg.toString());
			}
		};
	}

	public static String resizeString(String value, FontMetrics metrics,
			int maxWidth) {
		int width = metrics.stringWidth(value);

		if (width < maxWidth)
			return value;
		if (value.length() < 4)
			return value;
		return resizeString(
				new StringBuilder(value.substring(0, value.length() - 3) + ".."),
				metrics, maxWidth);
	}

	private static String resizeString(StringBuilder value,
			FontMetrics metrics, int maxWidth) {
		int width = metrics.stringWidth(value.toString());

		if (width < maxWidth)
			return value.toString();
		if (value.length() < 4)
			return value.toString();
		return resizeString(
				value.delete(value.length() - 3, value.length() - 2), metrics,
				maxWidth);
	}

	public static String toHexString(int bits, int value) {
		if (bits < 32)
			value &= (1 << bits) - 1;
		String ret = Integer.toHexString(value);
		int len = (bits + 3) / 4;
		while (ret.length() < len)
			ret = "0" + ret;
		if (ret.length() > len)
			ret = ret.substring(ret.length() - len);
		return ret;
	}

}
