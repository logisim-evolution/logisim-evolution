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

package com.cburch.logisim.data;

import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.util.StringGetter;

public class BitWidth implements Comparable<BitWidth> {
  static class Attribute extends com.cburch.logisim.data.Attribute<BitWidth> {
    private BitWidth[] choices;

    public Attribute(String name, StringGetter disp) {
      super(name, disp);
      ensurePrefab();
      choices = prefab;
    }

    public Attribute(String name, StringGetter disp, int min, int max) {
      super(name, disp);
      choices = new BitWidth[max - min + 1];
      for (int i = 0; i < choices.length; i++) {
        choices[i] = BitWidth.create(min + i);
      }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public java.awt.Component getCellEditor(BitWidth value) {
      ComboBox combo = new ComboBox<>(choices);
      if (value != null) {
        int wid = value.getWidth();
        if (wid <= 0 || wid > prefab.length) {
          combo.addItem(value);
        }
        combo.setSelectedItem(value);
      }
      return combo;
    }

    @Override
    public BitWidth parse(String value) {
      return BitWidth.parse(value);
    }
  }

  public static BitWidth create(int width) {
    ensurePrefab();
    if (width <= 0) {
      if (width == 0) {
        return UNKNOWN;
      } else {
        throw new IllegalArgumentException("width " + width + " must be positive");
      }
    } else if (width - 1 < prefab.length) {
      return prefab[width - 1];
    } else {
      return new BitWidth(width);
    }
  }

  private static void ensurePrefab() {
    if (prefab == null) {
      prefab = new BitWidth[Math.min(64, Value.MAX_WIDTH)];
      prefab[0] = ONE;
      for (int i = 1; i < prefab.length; i++) {
        prefab[i] = new BitWidth(i + 1);
      }
    }
  }

  public static BitWidth parse(String str) {
    if (str == null || str.length() == 0) {
      throw new NumberFormatException("Width string cannot be null");
    }
    if (str.charAt(0) == '/') str = str.substring(1);
    return create(Integer.parseInt(str));
  }

  public static final BitWidth UNKNOWN = new BitWidth(0);

  public static final BitWidth ONE = new BitWidth(1);
  
  public static final int MAXWIDTH = 64;
  public static final int MINWIDTH = 1;

  private static BitWidth[] prefab = null;

  final int width;

  private BitWidth(int width) {
    this.width = width;
  }

  public int compareTo(BitWidth other) {
    return this.width - other.width;
  }

  @Override
  public boolean equals(Object other_obj) {
    if (!(other_obj instanceof BitWidth)) return false;
    BitWidth other = (BitWidth) other_obj;
    return this.width == other.width;
  }

  public long getMask() {
    if (width == 0) return 0;
    else if (width == MAXWIDTH) return -1L;
    else return (1L << width) - 1;
  }

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
