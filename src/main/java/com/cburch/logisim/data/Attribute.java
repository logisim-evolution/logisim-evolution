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

/**
 * An abstract attribute identifier for a logisim evolution component.
 * <p>
 * An <code>Attribute</code> object represents an attribute of a component.
 * It may be interpreted as a "tag" or "identifier" for an attribute value, because it does not
 * store the information it represents.
 * The task of storage of an <code>Attribute</code>'s value is instead delegated to the
 * {@link AttributeSet} interface and its implementations.
 *
 * @see Attributes for <code>Attribute</code> instantiation utilities.
 *
 * @param <V> The type of the value represented by this <code>Attribute</code>.
 */
public abstract class Attribute<V> {
  private final String name;
  private final StringGetter displayName;
  private boolean hidden;

  /**
   * Instantiates a hidden dummy <code>Attribute</code> object.
   */
  public Attribute() {
    this("dummy", null, true);
  }

  /**
   * Instantiates an <code>Attribute</code> object.
   *
   * @param name The identifer/name of this attribute.
   * @param disp The display name of this attribute.
   */
  public Attribute(String name, StringGetter disp) {
    this(name, disp, false);
  }

  /**
   * Instantiates an <code>Attribute</code> object.
   *
   * @param name The identifer/name of this attribute.
   * @param disp The display name of this attribute.
   * @param hidden Whether this attribute should be displayed or not.
   */
  public Attribute(String name, StringGetter disp, boolean hidden) {
    this.name = name;
    this.displayName = disp;
    this.hidden = hidden;
  }

  /**
   * Constructs a java.awt component for editing a value represented by
   * this <code>Attribute</code>.
   *
   * @param value The default value for the field.
   * @return a suitable editor field for this <code>Attribute</code>.
   */
  protected java.awt.Component getCellEditor(V value) {
    return new JTextField(toDisplayString(value));
  }

  /**
   * Constructs a java.awt component for editing a value represented by
   * this <code>Attribute</code>.
   *
   * @param source The souce window for the returned component.
   * @param value The default value for the field.
   * @return a suitable editor field for this <code>Attribute</code>.
   */
  public java.awt.Component getCellEditor(Window source, V value) {
    return getCellEditor(value);
  }

  /**
   * The display name for this attribute.
   *
   * @return A string suitable for user display for this attribute, possibly derived from
   *         the <code>StringGetter</code> supplied in the
   *         {@link Attribute#Attribute(String name, StringGetter disp) constructor}.
   */
  public String getDisplayName() {
    return (displayName != null) ? displayName.toString() : name;
  }

  /**
   * The identifier/name for this attribute.
   *
   * @return A string representing the name of this attribute, expected (but not mandated) not to equal
   *         other attributes' names within a component.
   */
  public String getName() {
    return name;
  }

  /**
   * Parses an input string into a value to be represented by this attribute.
   *
   * @param source A java.awt window for which the string was derivated from.
   * @param value The string to parse.
   * @return A value taht is representable by this attribute.
   */
  public V parse(Window source, String value) {
    return parse(value);
  }

  /**
   * Parses an input string into a value to be represented by this attribute.
   *
   * @param value The string to parse.
   * @throws NumberFormatException if the string may not be converted into a value
   *                               representable by this attribute.
   *
   * @return The value parsed from the provided string.
   */
  public abstract V parse(String value);

  /**
   * Computes a string suitable for user display from the provided value.
   *
   * @param value The value to be displayed.
   * @return A display string representing the provided value.
   */
  public String toDisplayString(V value) {
    return value == null ? "" : value.toString();
  }

  /**
   * Computes a string suitable for serialization from the provided value.
   *
   * @param value The value to be displayed.
   * @return A serialization string representing the provided value.
   */
  public String toStandardString(V value) {
    return value.toString().replaceAll("[\u0000-\u001f]", "").replaceAll("&#.*?;", "");
  }

  /**
   * Attempts to set this attribute as hidden.
   * An Attribute implementation needs not honor this method.
   *
   * @see Attribute#isHidden
   *
   * @param val The value to set.
   */
  public void setHidden(boolean val) {
    this.hidden = val;
  }

  /**
   * Determines whether this attribute should be treated as hidden or not.
   * <p>
   * The output of this method may be affected by the <code>setHidden</code> method, but
   * implementations are not obligated to honor it. As such, <code>isHidden</code> is not
   * necessarily pure.
   *
   * @return true as an indication that this attribute should not be displayed or serialized.
   */
  public boolean isHidden() {
    return hidden;
  }

  /**
   * Determines whether this attribute should be saved/serialized.
   * <p>
   *
   * @return true as an indication that this attribute should be serialized or saved
   *  upon the serialization of a component.
   */
  public boolean isToSave() {
    return true;
  }

  @Override
  public String toString() {
    return name;
  }
}
