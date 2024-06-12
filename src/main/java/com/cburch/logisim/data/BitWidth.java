/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.util.StringGetter;
import java.awt.Component;

/**
 * Represents an immutable integer bit width within
 * the {@link Value#MAX_WIDTH} limit.
 */
public class BitWidth implements Comparable<BitWidth> {
  static class Attribute extends com.cburch.logisim.data.Attribute<BitWidth> {
    private final BitWidth[] choices;
    private final int min;
    private final int max;
  
    public Attribute(String name, StringGetter disp) {
      super(name, disp);
      this.min = MINWIDTH;
      this.max = MAXWIDTH;
      int[] defaults = { 1, 2, 3, 4, 5, 6, 7, 8, 16, 24, 32, 64 };
      choices = new BitWidth[defaults.length];
      for (int i = 0; i < defaults.length; i++) {
        choices[i] = BitWidth.create(defaults[i]);
      }
    }

    public Attribute(String name, StringGetter disp, int min, int max) {
      super(name, disp);
      this.min = min;
      this.max = max;
      int length = max - min + 1;
      if (length > 12) {
        // there are too many dropdown options, so use the default editor
        choices = null;
      } else {
        choices = new BitWidth[length];
        for (int i = 0; i < length; i++) {
          choices[i] = BitWidth.create(min + i);
        }
      }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Component getCellEditor(BitWidth value) {
      // there are too many dropdown options, so use the default editor
      if (choices == null) return super.getCellEditor(value);

      final var combo = new ComboBox<>(choices);
      if (value != null) combo.setSelectedItem(value);
      combo.setEditable(true);
      return combo;
    }

    @Override
    public BitWidth parse(String value) {
      int v = (int) Long.parseLong(value);
      if (v < min) throw new NumberFormatException("integer too small");
      if (v > max) throw new NumberFormatException("integer too large");
      return BitWidth.create(v);
    }
  }

  /**
   * The main entry point for accessing bit width values.
   *
   * @param width The width of the returned object
   * @return A bit width object with the provided width
   * @throws IllegalArgumentException if <code>width</code> is negative or
   *                                  exceeds <code>Value.MAX_WIDTH</code>.
   */
  public static BitWidth create(int width) {
    ensurePrefab();
    if (width < 0) {
      throw new IllegalArgumentException("width " + width + " must be positive");
    } else if (width >= prefab.length) {
      throw new IllegalArgumentException("width " + width + " must be at most " + MAXWIDTH);
    }
    return prefab[width];
  }

  private static void ensurePrefab() {
    if (prefab == null) {
      prefab = new BitWidth[MAXWIDTH + 1];
      prefab[0] = UNKNOWN;
      prefab[1] = ONE;
      for (int i = 2; i < prefab.length; i++) {
        prefab[i] = new BitWidth(i);
      }
    }
  }

  /**
   * Parses a string into a bit width value from its decimal representation.
   * The string must have the decimal representation of the width to be returned,
   * but may optionally start with a '/' character.
   *
   * @param str The string to parse
   * @return The resulting bit width object
   */
  public static BitWidth parse(String str) {
    if (str == null || str.length() == 0) {
      throw new NumberFormatException("Width string cannot be null");
    }
    if (str.charAt(0) == '/') str = str.substring(1);
    return create(Integer.parseInt(str));
  }

  /**
   * A bit width value of width zero.
   */
  public static final BitWidth UNKNOWN = new BitWidth(0);

  /**
   * A bit width value of width one.
   */
  public static final BitWidth ONE = new BitWidth(1);

  /**
   * The maximum width for a bit-width object, an alias for {@link Value#MAX_WIDTH}.
   */
  public static final int MAXWIDTH = Value.MAX_WIDTH;

  /**
   * The minimum width for a known bit width object.
   */
  public static final int MINWIDTH = 1;

  private static BitWidth[] prefab = null;

  final int width;

  private BitWidth(int width) {
    this.width = width;
  }

  @Override
  public int compareTo(BitWidth other) {
    return this.width - other.width;
  }

  @Override
  public boolean equals(Object otherObj) {
    return (otherObj instanceof BitWidth other)
           ? this.width == other.width
           : false;
  }

  /**
   * Determines the bit mask for this bit width object.
   * The returned <code>long</code> has exactly <code>width</code> 1-bits at its least
   * significant positions.
   *
   * @return the bitmask for this <code>BitWidth</code>.
   */
  public long getMask() {
    if (width == 0) return 0;
    else if (width == MAXWIDTH) return -1L;
    else return (1L << width) - 1;
  }

  /**
   * @return the bit width represented by this object, as an integer.
   */
  public int getWidth() {
    return width;
  }

  @Override
  public int hashCode() {
    return width;
  }

  @Override
  public String toString() {
    return "" + width;
  }
}
