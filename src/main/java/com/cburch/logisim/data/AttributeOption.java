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

/**
 * An attribute option is an immutable value that may be represented by an
 * <code>Attribute&lt;AttributeOption&gt;/code>.
 * When a component attribute has some named options that it may represent,
 * one may represent is as an <code>Attribute&lt;AttributeOption&gt;/code>, and define
 * its possible variants as an array of attribute options, with {@link Attributes#forOption }.
 *
 * @see AttributeOptionInterface
 */
public class AttributeOption implements AttributeOptionInterface {
  private final Object value;
  private final String name;
  private final StringGetter desc;

  /**
   * Constructs an attribute option with the provided information.
   * @param value The value associated with this attribute option.
   * @param name The name/identifier of this attribute option,
   *             This is used to distinguish distinct options within the same attribute,
   *             however it needs not be unique across all attribute option instances.
   * @param desc The textual description (getter) of this attribute option.
   */
  public AttributeOption(Object value, String name, StringGetter desc) {
    this.value = value;
    this.name = name;
    this.desc = desc;
  }

  /**
   * Constructs an attribute option with the provided information.
   * The name of the returned attribute is assigned to the string representation of
   * <code>value</code>, through {@link Object#toString()}.
   * @param value The value associated with this attribute option.
   * @param desc The textual description (getter) of this attribute option.
   */
  public AttributeOption(Object value, StringGetter desc) {
    this.value = value;
    this.name = value.toString();
    this.desc = desc;
  }

  public Object getValue() {
    return value;
  }

  public String toDisplayString() {
    return desc.toString();
  }

  /**
   * @return The {@link StringGetter} associated with the description of this attribute option.
   */
  public StringGetter getDisplayGetter() {
    return desc;
  }

  @Override
  public String toString() {
    return name;
  }
}
