/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import com.cburch.logisim.util.StringGetter;
import java.awt.Window;
import javax.swing.JTextField;
import lombok.Getter;
import lombok.Setter;

public abstract class Attribute<V> {
  @Getter private final String name;
  private final StringGetter disp;
  @Getter @Setter private boolean hidden;

  public Attribute() {
    this("dummy", null, true);
  }

  public Attribute(String name, StringGetter disp) {
    this(name, disp, false);
  }

  public Attribute(String name, StringGetter disp, boolean hidden) {
    this.name = name;
    this.disp = disp;
    this.hidden = hidden;
  }

  protected java.awt.Component getCellEditor(V value) {
    return new JTextField(toDisplayString(value));
  }

  public java.awt.Component getCellEditor(Window source, V value) {
    return getCellEditor(value);
  }

  public String getDisplayName() {
    return (disp != null) ? disp.toString() : name;
  }

  public V parse(Window source, String value) {
    return parse(value);
  }

  public abstract V parse(String value);

  public String toDisplayString(V value) {
    return value == null ? "" : value.toString();
  }

  public String toStandardString(V value) {
    return value.toString().replaceAll("[\u0000-\u001f]", "").replaceAll("&#.*?;", "");
  }

  public boolean isToSave() {
    return true;
  }

  @Override
  public String toString() {
    return name;
  }
}
