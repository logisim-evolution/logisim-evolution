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
      return (int) Math.floor((width.getWidth() - 1) * Math.log10(2)) + 2;
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
      return (int) Math.floor(width.getWidth() * Math.log10(2)) + 1;
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

  public static final RadixOption[] OPTIONS = {
    RADIX_2, RADIX_8, RADIX_10_SIGNED, RADIX_10_UNSIGNED, RADIX_16, RADIX_FLOAT
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
