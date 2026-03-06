/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
      return switch (width.getWidth()) {
        case 0 -> 1;
        case 1, 2, 3, 4 -> 2; // 1..8
        case 5, 6, 7 -> 3; // 16..64
        case 8, 9, 10 -> 4; // 128..512
        case 11, 12, 13, 14 -> 5; // 1K..8K
        case 15, 16, 17 -> 6; // 16K..64K
        case 18, 19, 20 -> 7; // 128K..512K
        case 21, 22, 23, 24 -> 8; // 1M..8M
        case 25, 26, 27 -> 9; // 16M..64M
        case 28, 29, 30 -> 10; // 128M..512M
        case 31, 32, 33, 34 -> 11; // 1G..8G
        case 35, 36, 37 -> 12; // 16G..64G
        case 38, 39, 40 -> 13; // 128G..512G
        case 41, 42, 43, 44 -> 14; // 1T..8T
        case 45, 46, 47 -> 15; // 16T..64T
        case 48, 49, 50 -> 16; // 128..512T
        case 51, 52, 53, 54 -> 17; // 1P..8P
        case 55, 56, 57 -> 18; // 16P..64P
        case 58, 59, 60 -> 19; // 128P..512P
        case 61, 62, 63, 64 -> 20; // 1E..4E
        default -> throw new AssertionError("unexpected bit width: " + width);
      };
    }

    @Override
    public String toString(Value value) {
      return value.toDecimalString(true);
    }

    @Override
    public String getIndexChar() {
      return "s";
    }
  }

  private static class Radix10Unsigned extends RadixOption {
    private Radix10Unsigned() {
      super("10unsigned", S.getter("radix10Unsigned"));
    }

    @Override
    public int getMaxLength(BitWidth width) {
      return switch (width.getWidth()) {
        case 0, 1, 2, 3 -> 1; // 0..7
        case 4, 5, 6 -> 2; // 8..63
        case 7, 8, 9 -> 3; // 64..511
        case 10, 11, 12, 13 -> 4; // 512..8K-1
        case 14, 15, 16 -> 5; // 8K..64K-1
        case 17, 18, 19 -> 6; // 64K..512K-1
        case 20, 21, 22, 23 -> 7; // 512K..8M-1
        case 24, 25, 26 -> 8; // 8M..64M-
        case 27, 28, 29 -> 9; // 64M..512M-1
        case 30, 31, 32, 33 -> 10; // 512M..8G-1
        case 34, 35, 36 -> 11; // 8G..64G-1
        case 37, 38, 39 -> 12; // 64G..512G-1
        case 40, 41, 42, 43 -> 13; // 512G..8T-1
        case 44, 45, 46 -> 14; // 8T..64T-1
        case 47, 48, 49 -> 15; // 64T..512T-1
        case 50, 51, 52, 53 -> 16; // 512T..8P-1
        case 54, 55, 56 -> 17; // 8P..64P-1
        case 57, 58, 59 -> 18; // 64P..512P-1
        case 60, 61, 62, 63 -> 19; // 512P..8E-1
        case 64 -> 20; // 8E..16E-1
        default -> throw new AssertionError("unexpected bit width: " + width);
      };
    }

    @Override
    public String toString(Value value) {
      return value.toDecimalString(false);
    }

    @Override
    public String getIndexChar() {
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
    public String getIndexChar() {
      return "h";
    }
  }

  private static class Radix2 extends RadixOption {
    private Radix2() {
      super("2", S.getter("radix2"));
    }

    @Override
    public int getMaxLength(BitWidth width) {
      final var bits = width.getWidth();
      return (bits <= 1) ? 1 : bits + ((bits - 1) / 4);
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
    public String getIndexChar() {
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
    public String getIndexChar() {
      return "o";
    }
  }

  private static class RadixFloat extends RadixOption {
    private RadixFloat() {
      super("float", S.getter("radixFloat"));
    }

    @Override
    public int getMaxLength(BitWidth width) {
      return width.getWidth() == 64 ? 24 : 12;
    }

    @Override
    public String toString(Value value) {
      return value.toStringFromFloatValue();
    }

    @Override
    public String getIndexChar() {
      return "f";
    }
  }

  private static class RadixQ1616 extends RadixOption {
    private RadixQ1616() {
      super("q16.16", S.getter("radixQ1616"));
    }

    @Override
    public int getMaxLength(BitWidth width) {
      return switch (width.getWidth()) {
        case 0 -> 1;
        case 1, 2, 3, 4 -> 2;
        case 5, 6, 7 -> 3;
        case 8, 9, 10 -> 4;
        case 11, 12, 13, 14 -> 5;
        case 15, 16, 17 -> 6;
        case 18, 19, 20 -> 7;
        case 21, 22, 23, 24 -> 8;
        case 25, 26, 27 -> 9;
        case 28, 29, 30 -> 10;
        case 31, 32, 33, 34 -> 11;
        default -> 20;
      };
    }

    @Override
    public String toString(Value value) {
      if (value == null || !value.isFullyDefined()) return "Q??";
      long bits = value.toLongValue();
      int signedBits = (int) bits;
      double fixedValue = (double) signedBits / 65536.0;
      return String.format("%.6f", fixedValue);
    }

    @Override
    public String getIndexChar() {
      return "q";
    }
  }

  public static RadixOption decode(String value) {
    for (final var opt : OPTIONS) {
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

  public static final RadixOption RADIX_FLOAT = new RadixFloat();

  public static final RadixOption RADIX_Q1616 = new RadixQ1616();

  public static final RadixOption[] OPTIONS = {
    RADIX_2, RADIX_8, RADIX_10_SIGNED, RADIX_10_UNSIGNED, RADIX_16, RADIX_FLOAT, RADIX_Q1616
  };

  public static final Attribute<RadixOption> ATTRIBUTE = Attributes.forOption("radix", S.getter("radixAttr"), OPTIONS);

  private final String saveName;

  private final StringGetter displayGetter;

  private RadixOption(String saveName, StringGetter displayGetter) {
    super(saveName, displayGetter);
    this.saveName = saveName;
    this.displayGetter = displayGetter;
  }

  @Override
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

  public String getIndexChar() {
    return "";
  }

  @Override
  public String toString() {
    return saveName;
  }

  public abstract String toString(Value value);
}
