/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import java.util.ArrayList;

/**
 * A utility base implementation for <code>AttributeSet</code>.
 * This class provides default implementations for all methods but
 * <code>copyInto</code>, <code>getAttributes</code>, <code>getValue</code> and
 * <code>setValue</code> which must be implemented by subclasses.
 * The provided implementations for all other methods may use
 * those four to implement their features.
 * <p>
 * Unless overridden by subclasses, all attributes are considered not to be read only,
 * and <code>setReadOnly</code> always raises an <code>UnsupportedOperationException</code>.
 */
public abstract class AbstractAttributeSet implements Cloneable, AttributeSet {
  private ArrayList<AttributeListener> listeners = null;

  @Override
  public void addAttributeListener(AttributeListener l) {
    if (listeners == null) listeners = new ArrayList<>();
    listeners.add(l);
  }

  public boolean amIListening(AttributeListener l) {
    return listeners.contains(l);
  }

  @Override
  public Object clone() {
    try {
      AbstractAttributeSet ret = (AbstractAttributeSet) super.clone();
      ret.listeners = new ArrayList<>();
      this.copyInto(ret);
      return ret;
    } catch (CloneNotSupportedException ex) {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public boolean containsAttribute(Attribute<?> attr) {
    return getAttributes().contains(attr);
  }

  /**
   * Copies all attributes and their respective values from this attribute set
   * to the provided <code>AbstractAttributeSet</code> through {@link AttributeSet#setValue}.
   *
   * @param dest The attribute set to copy to, this object is assumed to be an instance
   *             of the implementor's class.
   */
  protected abstract void copyInto(AbstractAttributeSet dest);

  /**
   * Sends an {@link AttributeListener#attributeListChanged} event to all
   * attribute listeners associated with this attribute set.
   */
  protected void fireAttributeListChanged() {
    if (listeners != null) {
      final var event = new AttributeEvent(this);
      for (final var l : new ArrayList<>(listeners)) {
        l.attributeListChanged(event);
      }
    }
  }

  /**
   * Sends an {@link AttributeListener#attributeValueChanged} event to all
   * attribute listeners associated with this attribute set.
   *
   * @param attr The attribute of the event to be sent.
   * @param value The current/new value of the event.
   * @param oldvalue The previous value of event.
   * @param <V> The type represented by the given attribute.
   */
  protected <V> void fireAttributeValueChanged(Attribute<? super V> attr, V value, V oldvalue) {
    if (listeners != null) {
      final var event = new AttributeEvent(this, attr, value, oldvalue);
      final var ls = new ArrayList<>(listeners);
      for (final var l : ls) {
        l.attributeValueChanged(event);
      }
    }
  }

  @Override
  public Attribute<?> getAttribute(String name) {
    for (Attribute<?> attr : getAttributes()) {
      if (attr.getName().equals(name)) {
        return attr;
      }
    }
    return null;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return false;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave();
  }

  @Override
  public void removeAttributeListener(AttributeListener l) {
    listeners.remove(l);
    if (listeners.isEmpty()) listeners = null;
  }

  @Override
  public void setReadOnly(Attribute<?> attr, boolean value) {
    throw new UnsupportedOperationException();
  }
}
