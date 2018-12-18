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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.StringGetter;

public abstract class RadixOption extends AttributeOption {
	private static class Radix10Signed extends RadixOption {
		private Radix10Signed() {
			super("10signed", Strings.getter("radix10Signed"));
		}

		@Override
		public int getMaxLength(BitWidth width) {
			switch (width.getWidth()) {
			case 2:
			case 3:
			case 4:
				return 2; // 2..8
			case 5:
			case 6:
			case 7:
				return 3; // 16..64
			case 8:
			case 9:
			case 10:
				return 4; // 128..512
			case 11:
			case 12:
			case 13:
			case 14:
				return 5; // 1K..8K
			case 15:
			case 16:
			case 17:
				return 6; // 16K..64K
			case 18:
			case 19:
			case 20:
				return 7; // 128K..256K
			case 21:
			case 22:
			case 23:
			case 24:
				return 8; // 1M..8M
			case 25:
			case 26:
			case 27:
				return 9; // 16M..64M
			case 28:
			case 29:
			case 30:
				return 10; // 128M..512M
			case 31:
			case 32:
				return 11; // 1G..2G
			default:
				return 1;
			}
		}

		@Override
		public String toString(Value value) {
			return value.toDecimalString(true);
		}

		@Override
		public String GetIndexChar() {
			return "s";
		}
	}

	private static class Radix10Unsigned extends RadixOption {
		private Radix10Unsigned() {
			super("10unsigned", Strings.getter("radix10Unsigned"));
		}

		@Override
		public int getMaxLength(BitWidth width) {
			switch (width.getWidth()) {
			case 4:
			case 5:
			case 6:
				return 2;
			case 7:
			case 8:
			case 9:
				return 3;
			case 10:
			case 11:
			case 12:
			case 13:
				return 4;
			case 14:
			case 15:
			case 16:
				return 5;
			case 17:
			case 18:
			case 19:
				return 6;
			case 20:
			case 21:
			case 22:
			case 23:
				return 7;
			case 24:
			case 25:
			case 26:
				return 8;
			case 27:
			case 28:
			case 29:
				return 9;
			case 30:
			case 31:
			case 32:
				return 10;
			default:
				return 1;
			}
		}

		@Override
		public String toString(Value value) {
			return value.toDecimalString(false);
		}

		@Override
		public String GetIndexChar() {
			return "u";
		}
	}

	private static class Radix16 extends RadixOption {
		private Radix16() {
			super("16", Strings.getter("radix16"));
		}

		@Override
		public int getMaxLength(BitWidth width) {
			return Math.max(1, (width.getWidth() + 3) / 4);
		}

		@Override
		public String toString(Value value) {
			return value.toDisplayString(16);
		}

		@Override
		public String GetIndexChar() {
			return "h";
		}
	}

	private static class Radix2 extends RadixOption {
		private Radix2() {
			super("2", Strings.getter("radix2"));
		}

		@Override
		public int getMaxLength(BitWidth width) {
			int bits = width.getWidth();
			if (bits <= 1)
				return 1;
			return bits + ((bits - 1) / 4);
		}

		@Override
		public int getMaxLength(Value value) {
			return value.toDisplayString(2).length();
		}

		@Override
		public String toString(Value value) {
			return value.toDisplayString(2);
		}

		@Override
		public String GetIndexChar() {
			return "b";
		}
	}

	private static class Radix8 extends RadixOption {
		private Radix8() {
			super("8", Strings.getter("radix8"));
		}

		@Override
		public int getMaxLength(BitWidth width) {
			return Math.max(1, (width.getWidth() + 2) / 3);
		}

		@Override
		public int getMaxLength(Value value) {
			return value.toDisplayString(8).length();
		}

		@Override
		public String toString(Value value) {
			return value.toDisplayString(8);
		}

		@Override
		public String GetIndexChar() {
			return "o";
		}
	}

	public static RadixOption decode(String value) {
		for (RadixOption opt : OPTIONS) {
			if (value.equals(opt.saveName)) {
				return opt;
			}
		}
		return RADIX_2;
	}

	public static final RadixOption RADIX_2 = new Radix2();

	public static final RadixOption RADIX_8 = new Radix8();

	public static final RadixOption RADIX_10_UNSIGNED = new Radix10Unsigned();
	public static final RadixOption RADIX_10_SIGNED = new Radix10Signed();

	public static final RadixOption RADIX_16 = new Radix16();

	public static final RadixOption[] OPTIONS = { RADIX_2, RADIX_8,
			RADIX_10_SIGNED, RADIX_10_UNSIGNED, RADIX_16 };

	public static final Attribute<RadixOption> ATTRIBUTE = Attributes
			.forOption("radix", Strings.getter("radixAttr"), OPTIONS);

	private String saveName;

	private StringGetter displayGetter;

	private RadixOption(String saveName, StringGetter displayGetter) {
		super(saveName, displayGetter);
		this.saveName = saveName;
		this.displayGetter = displayGetter;
	}

	public StringGetter getDisplayGetter() {
		return displayGetter;
	}

	public abstract int getMaxLength(BitWidth width);

	public int getMaxLength(Value value) {
		return getMaxLength(value.getBitWidth());
	}

	public String getSaveString() {
		return saveName;
	}

	@Override
	public String toDisplayString() {
		return displayGetter.toString();
	}

	@Override
	public String toString() {
		return saveName;
	}
	
	public String GetIndexChar() {
		return "";
	}

	public abstract String toString(Value value);
}
