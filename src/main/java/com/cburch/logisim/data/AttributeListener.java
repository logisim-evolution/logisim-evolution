/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

/**
 * An object that represents a callback for {@link AttributeSet} events
 * such as the modification of an attribute value and the modification of the attribute keys of
 * a set.
 */
public interface AttributeListener {

  /**
   * Notifies this listener that the list of keys of an attribute set has been modified.
   * @param e The event object that represents the notified event.
   *          Because this event doesn't target a particular attribute, the
   *          <code>attribute</code>, <code>value</code> and  <code>oldValue</code>
   *          of the event may be set to null.
   */
  default void attributeListChanged(AttributeEvent e) {
    // no-op implementation
  }

  /**
   * Notifies this listener that the value associated with a particular attribute has changed.
   * @param e The event object that represents the notified event.
   *          The <code>attribute</code> field of the event must be the attribute whose value has
   *          changed, and <code>value</code> and <code>oldValue</code> must refer
   *          to the new and old values respectively.
   */
  default void attributeValueChanged(AttributeEvent e) {
    // no-op implementation
  }
}
