/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.StringGetter;

public abstract class RadixOption extends AttributeOption {
  private static class Radix10Signed extends RadixOption {
    private Radix10Signed() {
      super("10signed", S.getter("radix10Signed"));
    }

    @Override
    public int getMaxLength(BitWidth width) {
      switch (width.getWidth()) {
        case 0:
          return 1;
        case 1:
        case 2:
        case 3:
        case 4:
          return 2; // 1..8
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
          return 7; // 128K..512K
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
        case 33:
        case 34:
          return 11; // 1G..8G
        case 35:
        case 36:
        case 37:
          return 12; // 16G..64G
        case 38:
        case 39:
        case 40:
          return 13; // 128G..512G
        case 41:
        case 42:
        case 43:
        case 44:
          return 14; // 1T..8T
        case 45:
        case 46:
        case 47:
          return 15; // 16T..64T
        case 48:
        case 49:
        case 50:
          return 16; // 128..512T
        case 51:
        case 52:
        case 53:
        case 54:
          return 17; // 1P..8P
        case 55:
        case 56:
        case 57:
          return 18; // 16P..64P
        case 58:
        case 59:
        case 60:
          return 19; // 128P..512P
        case 61:
        case 62:
        case 63:
        case 64:
          return 20; // 1E..4E
        default:
          throw new AssertionError("unexpected bit width: "+ width);
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
      super("10unsigned", S.getter("radix10Unsigned"));
    }

    @Override
    public int getMaxLength(BitWidth width) {
      switch (width.getWidth()) {
        case 0:
        case 1:
        case 2:
        case 3:
          return 1; // 0..7
        case 4:
        case 5:
        case 6:
          return 2; // 8..63
        case 7:
        case 8:
        case 9:
          return 3; // 64..511
        case 10:
        case 11:
        case 12:
        case 13:
          return 4; // 512..8K-1
        case 14:
        case 15:
        case 16:
          return 5; // 8K..64K-1
        case 17:
        case 18:
        case 19:
          return 6; // 64K..512K-1
        case 20:
        case 21:
        case 22:
        case 23:
          return 7; // 512K..8M-1
        case 24:
        case 25:
        case 26:
          return 8; // 8M..64M-1
        case 27:
        case 28:
        case 29:
          return 9; // 64M..512M-1
        case 30:
        case 31:
        case 32:
        case 33:
          return 10; // 512M..8G-1
        case 34:
        case 35:
        case 36:
          return 11; // 8G..64G-1
        case 37:
        case 38:
        case 39:
          return 12; // 64G..512G-1
        case 40:
        case 41:
        case 42:
        case 43:
          return 13; // 512G..8T-1
        case 44:
        case 45:
        case 46:
          return 14; // 8T..64T-1
        case 47:
        case 48:
        case 49:
          return 15; // 64T..512T-1
        case 50:
        case 51:
        case 52:
        case 53:
          return 16; // 512T..8P-1
        case 54:
        case 55:
        case 56:
          return 17; // 8P..64P-1
        case 57:
        case 58:
        case 59:
          return 18; // 64P..512P-1
        case 60:
        case 61:
        case 62:
        case 63:
          return 19; // 512P..8E-1
        case 64:
          return 20; // 8E..16E-1
        default:
          throw new AssertionError("unexpected bit width: "+ width);
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
      super("16", S.getter("radix16"));
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
      super("2", S.getter("radix2"));
    }

    @Override
    public int getMaxLength(BitWidth width) {
      int bits = width.getWidth();
      if (bits <= 1) return 1;
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
      super("8", S.getter("radix8"));
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

  public static final RadixOption[] OPTIONS = {
    RADIX_2, RADIX_8, RADIX_10_SIGNED, RADIX_10_UNSIGNED, RADIX_16
  };

  public static final Attribute<RadixOption> ATTRIBUTE =
      Attributes.forOption("radix", S.getter("radixAttr"), OPTIONS);

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
