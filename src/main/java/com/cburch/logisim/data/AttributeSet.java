/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import java.util.List;

/**
 * An <code>AttributeSet</code> is an object tasked to store {@link Attribute}s and associate them with
 * their respective values.
 * <p>
 * Despite the Set nomenclature,
 * the <code>AttributeSet</code> interface more closely resembles a {@link java.util.Map} that
 * maps attributes to their respective values. In general, the attributes (keys) of an attribute set
 * are expected to be non-null
 * and not to conflict regarding their names.
 * <p>
 * Note that some operations may not be honored by all implementations. Methods that satisfy these
 * criteria are documented as such.
 *
 * <h3>
 * Implementations may not support safe multithread access.
 */
public interface AttributeSet {

  /**
   * Registers an attribute listener to this attribute set.
   * The listener is notified through {@link AttributeListener#attributeListChanged(AttributeEvent)
   * attributeListChanged}
   * when the keys of this attribute set is modified
   * and {@link AttributeListener#attributeValueChanged(AttributeEvent) attributeValueChanged} when
   * an attribute value is modified.
   * <p>
   * Multiple listeners may be added to a single attribute set, in which case all of them
   * are notified about this attribute set's events.
   * If an attribute listener is added more than once to the same attribute set, it will be
   * notified more than once, equal to the number of times it occurs in this set.
   * <p>
   * An arbitrary implementation need not honor this method.
   *
   * @param l the listener to be notified.
   */
  default void addAttributeListener(AttributeListener l) {
    // no-op implementation
  }

  Object clone();

  /**
   * Determines whether this attribute set contains the provided <code>Attribute</code> object.
   * Comparisons are performed utilizing {@link Object#equals(Object)} method of <code>attr</code>.
   *
   * @param attr The non-null attribute to check.
   * @return true if and only if this attribute set contains the provided attribute.
   */
  boolean containsAttribute(Attribute<?> attr);

  /**
   * Retrieves an attribute in this set matching the provided name.
   * @param name the name of the attribute to check.
   * @return an attribute with the provided name, or null if no such attribute was found.
   */
  default Attribute<?> getAttribute(String name) {
    return null;
  }

  /**
   * Returns a List containing this attribute set's attributes.
   *
   * @return A list containing this attribute set's Attributes.
   *         The behavior of mutable operations on the {@link java.util.List List}
   *         returned object is implementation specific.
   */
  default List<Attribute<?>> getAttributes() {
    return null;
  }

  /**
   * Retrieves the value associated with the provided attribute.
   * @param attr The attribute to lookup.
   * @return The value associated by <code>attr</code> or null
   *         if such attribute does not exist in this attribute set.
   * @param <V> The type represented by the provided attribute.
   */
  <V> V getValue(Attribute<V> attr);

  /**
   * Determines whether the provided attribute is read only.
   *
   * @param attr The attribute to check.
   * @return true if and only if the provided attribute exists in this attribute set and is
   *         considered to be read only.
   */
  boolean isReadOnly(Attribute<?> attr);

  /**
   * Determines whether an attribute attirbute should be serialized
   * @param attr the attribute to check.
   * @return true as an indication that the provided attribute should be serialized or saved
   *  upon the serialization of a component.
   */
  boolean isToSave(Attribute<?> attr);


  /**
   * Removes the provided attribute listener from this attribute set, if such listener exists.
   * If this listener occurs more than once in the set, only one of its occurences is removed.
   * <p>
   * An arbitrary implementation need not honor this method.
   *
   * @param l The listener to remove
   */
  default void removeAttributeListener(AttributeListener l) {
    // no-op implementation
  }

  /**
   * Modifies the read only state of an attribute.
   * If this operation is supported, the read only state of the provided attribute
   * will equal the provided boolean value.
   * @param attr The attribute to set
   * @param value The boolean value to set as the read only state of the attr.
   * @throws UnsupportedOperationException if this attribute set does not support this operation.
   */
  void setReadOnly(Attribute<?> attr, boolean value); // optional

  /**
   * Modifies the value associated with the given attribute.
   * @param attr the attribute to associate the value with
   * @param value the value to be assigned to <code>attr</code>.
   * @param <V> the type represented by the provided attribute.
   * @throws IllegalArgumentException if <code>attr</code> is read only.
   */
  <V> void setValue(Attribute<V> attr, V value);

  /**
   * Returns attributes that may also be changed as a side effect of changing attr to value or
   * changing attr back to its current value from value. This method does not change attr.
   *
   * @param attr The attribute whose change is being considered
   * @param value The new value for attr that is being considered
   * @return a List of attributes that may also be changed, or null if there are no such attributes.
   */
  default <V> List<Attribute<?>> attributesMayAlsoBeChanged(Attribute<V> attr, V value) {
    return null;
  }
}
