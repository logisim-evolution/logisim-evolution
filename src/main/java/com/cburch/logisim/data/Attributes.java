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

import static com.cburch.logisim.data.Strings.S;

import com.bric.colorpicker.ColorPicker;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.util.FontUtil;
import com.cburch.logisim.util.JInputComponent;
import com.cburch.logisim.util.StringGetter;
import org.drjekyll.fontchooser.FontChooser;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class Attributes {
  private static class BooleanAttribute extends OptionAttribute<Boolean> {
    private static Boolean[] vals = {Boolean.TRUE, Boolean.FALSE};

    private BooleanAttribute(String name, StringGetter disp) {
      super(name, disp, vals);
    }

    @Override
    public Boolean parse(String value) {
      Boolean b = Boolean.valueOf(value);
      return vals[b.booleanValue() ? 0 : 1];
    }

    @Override
    public String toDisplayString(Boolean value) {
      if (value.booleanValue()) return S.get("booleanTrueOption");
      else return S.get("booleanFalseOption");
    }
  }
  
  private static class IOMapAttribute extends Attribute<ComponentMapInformationContainer> {

	@Override
	public ComponentMapInformationContainer parse(String value) {
		return null;
	}
	
	@Override
	public boolean isHidden() {return true;}
	  
  }

  private static class ColorAttribute extends Attribute<Color> {
    public ColorAttribute(String name, StringGetter desc) {
      super(name, desc);
    }

    @Override
    public java.awt.Component getCellEditor(Color value) {
      Color init = value == null ? Color.WHITE : value;
      return new ColorChooser(init);
    }

    private String hex(int value) {
      if (value >= 16) return Integer.toHexString(value);
      else return "0" + Integer.toHexString(value);
    }

    @Override
    public Color parse(String value) {
      if (value.length() == 9) {
        int r = Integer.parseInt(value.substring(1, 3), 16);
        int g = Integer.parseInt(value.substring(3, 5), 16);
        int b = Integer.parseInt(value.substring(5, 7), 16);
        int a = Integer.parseInt(value.substring(7, 9), 16);
        return new Color(r, g, b, a);
      } else {
        return Color.decode(value);
      }
    }

    @Override
    public String toDisplayString(Color value) {
      return toStandardString(value);
    }

    @Override
    public String toStandardString(Color c) {
      String ret = "#" + hex(c.getRed()) + hex(c.getGreen()) + hex(c.getBlue());
      return c.getAlpha() == 255 ? ret : ret + hex(c.getAlpha());
    }
  }

  private static class ColorChooser extends ColorPicker implements JInputComponent {
    /** */
    private static final long serialVersionUID = 1L;

    ColorChooser(Color initial) {
      if (initial != null) setColor(initial);
      setOpacityVisible(true);
    }

    public Object getValue() {
      return getColor();
    }

    public void setValue(Object value) {
      setColor((Color) value);
    }
  }

  private static class ConstantGetter implements StringGetter {
    private String str;

    public ConstantGetter(String str) {
      this.str = str;
    }

    /*
     * public String get() { return str; }
     *
     * @Override public String toString() { return get(); }
     */
    public String toString() {
      return str;
    }
  }

  private static class DirectionAttribute extends OptionAttribute<Direction> {
    private static Direction[] vals = {
      Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
    };

    public DirectionAttribute(String name, StringGetter disp) {
      super(name, disp, vals);
    }

    @Override
    public Direction parse(String value) {
      return Direction.parse(value);
    }

    @Override
    public String toDisplayString(Direction value) {
      return value == null ? "???" : value.toDisplayString();
    }
  }

  private static class DoubleAttribute extends Attribute<Double> {
    private DoubleAttribute(String name, StringGetter disp) {
      super(name, disp);
    }

    @Override
    public Double parse(String value) {
      return Double.valueOf(value);
    }
  }

  private static class FontAttribute extends Attribute<Font> {
    private FontAttribute(String name, StringGetter disp) {
      super(name, disp);
    }

    @Override
    public java.awt.Component getCellEditor(Font value) {
      return new LFontChooser(value);
    }

    @Override
    public Font parse(String value) {
      return Font.decode(value);
    }

    @Override
    public String toDisplayString(Font f) {
      if (f == null) return "???";
      return f.getFamily() + " " + FontUtil.toStyleDisplayString(f.getStyle()) + " " + f.getSize();
    }

    @Override
    public String toStandardString(Font f) {
      return f.getFamily() + " " + FontUtil.toStyleStandardString(f.getStyle()) + " " + f.getSize();
    }
  }

  private static class LFontChooser extends FontChooser implements JInputComponent {
    /** */
    private static final long serialVersionUID = 1L;

    LFontChooser(Font initial) {
      super(initial);
    }

    public Object getValue() {
      return getSelectedFont();
    }

    public void setValue(Object value) {
      setSelectedFont((Font) value);
    }
  }

  private static class HexIntegerAttribute extends Attribute<Integer> {
    private HexIntegerAttribute(String name, StringGetter disp) {
      super(name, disp);
    }

    @Override
    public Integer parse(String value) {
      value = value.toLowerCase();
      if (value.startsWith("-")) {
        value = value.substring(1);
        if (value.startsWith("0x")) {
          value = value.substring(2);
          return Integer.valueOf(Integer.parseInt("-" + value, 16));
        } else if (value.startsWith("0b")) {
          value = value.substring(2);
          return Integer.valueOf(Integer.parseInt("-" + value, 2));
        } else if (value.startsWith("0") && value.length() > 1) {
          value = value.substring(1);
          return Integer.valueOf(Integer.parseInt("-" + value, 8));
        } else {
          return Integer.valueOf(Integer.parseInt("-" + value, 10));
        }
      } else {
        if (value.startsWith("0x")) {
          value = value.substring(2);
          return Integer.valueOf(Integer.parseUnsignedInt(value, 16));
        } else if (value.startsWith("0b")) {
          value = value.substring(2);
          return Integer.valueOf(Integer.parseUnsignedInt(value, 2));
        } else if (value.startsWith("0") && value.length() > 1) {
          value = value.substring(1);
          return Integer.valueOf(Integer.parseUnsignedInt(value, 8));
        } else {
          return Integer.valueOf(Integer.parseUnsignedInt(value, 10));
        }
      }
    }

    @Override
    public String toDisplayString(Integer value) {
      int val = value.intValue();
      return "0x" + Integer.toHexString(val);
    }

    @Override
    public String toStandardString(Integer value) {
      return toDisplayString(value);
    }
  }

  private static class HexLongAttribute extends Attribute<Long> {
    private HexLongAttribute(String name, StringGetter disp) {
      super(name, disp);
    }

    @Override
    public Long parse(String value) {
      value = value.toLowerCase();
      if (value.startsWith("-")) {
        value = value.substring(1);
        if (value.startsWith("0x")) {
          value = value.substring(2);
          return Long.valueOf(Long.parseLong("-" + value, 16));
        } else if (value.startsWith("0b")) {
          value = value.substring(2);
          return Long.valueOf(Long.parseLong("-" + value, 2));
        } else if (value.startsWith("0") && value.length() > 1) {
          value = value.substring(1);
          return Long.valueOf(Long.parseLong("-" + value, 8));
        } else {
          return Long.valueOf(Long.parseLong("-" + value, 10));
        }
      } else {
        if (value.startsWith("0x")) {
          value = value.substring(2);
          return Long.valueOf(Long.parseUnsignedLong(value, 16));
        } else if (value.startsWith("0b")) {
          value = value.substring(2);
          return Long.valueOf(Long.parseUnsignedLong(value, 2));
        } else if (value.startsWith("0") && value.length() > 1) {
          value = value.substring(1);
          return Long.valueOf(Long.parseUnsignedLong(value, 8));
        } else {
          return Long.valueOf(Long.parseUnsignedLong(value, 10));
        }
      }
    }

    @Override
    public String toDisplayString(Long value) {
      long val = value.longValue();
      return "0x" + Long.toHexString(val);
    }

    @Override
    public String toStandardString(Long value) {
      return toDisplayString(value);
    }
  }

  private static class IntegerAttribute extends Attribute<Integer> {
    private IntegerAttribute(String name, StringGetter disp) {
      super(name, disp);
    }

    @Override
    public Integer parse(String value) {
      return Integer.valueOf(value);
    }
  }

  private static class IntegerRangeAttribute extends Attribute<Integer> {
    Integer[] options = null;
    int start;
    int end;

    private IntegerRangeAttribute(String name, StringGetter disp, int start, int end) {
      super(name, disp);
      this.start = start;
      this.end = end;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public java.awt.Component getCellEditor(Integer value) {
      if (end - start > 31) {
        return super.getCellEditor(value);
      } else {
        if (options == null) {
          options = new Integer[end - start + 1];
          for (int i = start; i <= end; i++) {
            options[i - start] = Integer.valueOf(i);
          }
        }
        ComboBox combo = new ComboBox<>(options);
        if (value == null) combo.setSelectedIndex(-1);
        else combo.setSelectedItem(value);
        return combo;
      }
    }

    @Override
    public Integer parse(String value) {
      int v = (int) Long.parseLong(value);
      if (v < start) throw new NumberFormatException("integer too small");
      if (v > end) throw new NumberFormatException("integer too large");
      return Integer.valueOf(v);
    }
  }

  private static class LocationAttribute extends Attribute<Location> {
    public LocationAttribute(String name, StringGetter desc) {
      super(name, desc);
    }

    @Override
    public Location parse(String value) {
      return Location.parse(value);
    }
  }

  private static class OptionAttribute<V> extends Attribute<V> {
    private V[] vals;

    private OptionAttribute(String name, StringGetter disp, V[] vals) {
      super(name, disp);
      this.vals = vals;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public java.awt.Component getCellEditor(Object value) {
      ComboBox combo = new ComboBox<>(vals);
      combo.setRenderer(new OptionComboRenderer<V>(this));
      if (value == null) combo.setSelectedIndex(-1);
      else combo.setSelectedItem(value);
      return combo;
    }

    @Override
    public V parse(String value) {
      for (int i = 0; i < vals.length; i++) {
        if (value.equals(vals[i].toString())) {
          return vals[i];
        }
      }
      throw new NumberFormatException("value not among choices");
    }

    @Override
    public String toDisplayString(V value) {
      if (value instanceof AttributeOptionInterface) {
        return ((AttributeOptionInterface) value).toDisplayString();
      } else {
        return value.toString();
      }
    }
  }

  private static class OptionComboRenderer<V> extends BasicComboBoxRenderer {
    private static final long serialVersionUID = 1L;
    Attribute<V> attr;

    OptionComboRenderer(Attribute<V> attr) {
      this.attr = attr;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Component getListCellRendererComponent(
        JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Component ret =
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (ret instanceof JLabel) {
        @SuppressWarnings("unchecked")
        V val = (V) value;
        ((JLabel) ret).setText(value == null ? "" : attr.toDisplayString(val));
      }
      return ret;
    }
  }

  private static class StringAttribute extends Attribute<String> {
    private StringAttribute(String name, StringGetter disp) {
      super(name, disp);
    }

    @Override
    public String parse(String value) {
      return value;
    }
  }

  private static class HiddenAttribute extends Attribute<String> {
    private HiddenAttribute() {
      super();
    }

    @Override
    public String parse(String value) {
      return value;
    }
  }

  public static Attribute<String> forHidden() {
    return new HiddenAttribute();
  }

  public static Attribute<BitWidth> forBitWidth(String name) {
    return forBitWidth(name, getter(name));
  }

  public static Attribute<BitWidth> forBitWidth(String name, int min, int max) {
    return forBitWidth(name, getter(name), min, max);
  }

  public static Attribute<BitWidth> forBitWidth(String name, StringGetter disp) {
    return new BitWidth.Attribute(name, disp);
  }

  public static Attribute<BitWidth> forBitWidth(String name, StringGetter disp, int min, int max) {
    return new BitWidth.Attribute(name, disp, min, max);
  }

  public static Attribute<Boolean> forBoolean(String name) {
    return forBoolean(name, getter(name));
  }

  public static Attribute<Boolean> forBoolean(String name, StringGetter disp) {
    return new BooleanAttribute(name, disp);
  }

  public static Attribute<Color> forColor(String name) {
    return forColor(name, getter(name));
  }

  public static Attribute<Color> forColor(String name, StringGetter disp) {
    return new ColorAttribute(name, disp);
  }
  
  public static Attribute<ComponentMapInformationContainer> forMap() {
    return new IOMapAttribute();
  }

  public static Attribute<Direction> forDirection(String name) {
    return forDirection(name, getter(name));
  }

  public static Attribute<Direction> forDirection(String name, StringGetter disp) {
    return new DirectionAttribute(name, disp);
  }

  public static Attribute<Double> forDouble(String name) {
    return forDouble(name, getter(name));
  }

  public static Attribute<Double> forDouble(String name, StringGetter disp) {
    return new DoubleAttribute(name, disp);
  }

  public static Attribute<Font> forFont(String name) {
    return forFont(name, getter(name));
  }

  public static Attribute<Font> forFont(String name, StringGetter disp) {
    return new FontAttribute(name, disp);
  }

  public static Attribute<Integer> forHexInteger(String name) {
    return forHexInteger(name, getter(name));
  }

  public static Attribute<Long> forHexLong(String name) {
    return forHexLong(name, getter(name));
  }

  public static Attribute<Integer> forHexInteger(String name, StringGetter disp) {
    return new HexIntegerAttribute(name, disp);
  }

  public static Attribute<Long> forHexLong(String name, StringGetter disp) {
    return new HexLongAttribute(name, disp);
  }

  public static Attribute<Integer> forInteger(String name) {
    return forInteger(name, getter(name));
  }

  public static Attribute<Integer> forInteger(String name, StringGetter disp) {
    return new IntegerAttribute(name, disp);
  }

  public static Attribute<Integer> forIntegerRange(String name, int start, int end) {
    return forIntegerRange(name, getter(name), start, end);
  }

  public static Attribute<Integer> forIntegerRange(
      String name, StringGetter disp, int start, int end) {
    return new IntegerRangeAttribute(name, disp, start, end);
  }

  public static Attribute<Location> forLocation(String name) {
    return forLocation(name, getter(name));
  }

  public static Attribute<Location> forLocation(String name, StringGetter disp) {
    return new LocationAttribute(name, disp);
  }

  public static Attribute<?> forOption(String name, Object[] vals) {
    return forOption(name, getter(name), vals);
  }

  public static <V> Attribute<V> forOption(String name, StringGetter disp, V[] vals) {
    return new OptionAttribute<V>(name, disp, vals);
  }

  //
  // methods with display name == standard name
  //
  public static Attribute<String> forString(String name) {
    return forString(name, getter(name));
  }

  //
  // methods with internationalization support
  //
  public static Attribute<String> forString(String name, StringGetter disp) {
    return new StringAttribute(name, disp);
  }

  private static StringGetter getter(String s) {
    return new ConstantGetter(s);
  }

  private Attributes() {}
}
