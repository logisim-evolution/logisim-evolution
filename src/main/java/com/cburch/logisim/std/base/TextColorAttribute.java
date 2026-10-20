/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.base;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.gui.generic.TextColorChooser;
import com.cburch.logisim.util.ColorUtil;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;
import java.awt.Component;

class TextColorAttribute extends Attribute<Color> {
  private final Attribute<Color> storedColor;

  TextColorAttribute(String name, StringGetter displayName) {
    super(name, displayName);
    storedColor = Attributes.forColor(name, displayName);
  }

  @Override
  public Component getCellEditor(Color value) {
    return new TextColorChooser(value, true);
  }

  @Override
  public String toDisplayString(Color value) {
    return value == null ? "" : storedColor.toStandardString(ColorUtil.getThemeTextColor(value));
  }

  // File parsing and serialization always use the light-theme value.
  @Override
  public Color parse(String value) {
    return storedColor.parse(value);
  }

  @Override
  public String toStandardString(Color value) {
    return storedColor.toStandardString(value);
  }
}
