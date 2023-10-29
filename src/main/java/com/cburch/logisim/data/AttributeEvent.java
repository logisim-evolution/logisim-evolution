/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

// NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
// getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`. We may change
// that in the future, but for now it looks stupid in this file only.

/**
 * A record that represents an event involving a specific attribute. Typically
 * passed as an argument to {@link AttributeListener#attributeListChanged} and
 * {@link AttributeListener#attributeValueChanged} by an {@link AttributeSet}
 * @param getSource The attribute source from which this event originated from.
 * @param getAttribute The attribute that was targeted by this event.
 *                     This may be null, such in the case of an
 *                     <code>attributeValueChanged</code> event.
 * @param getValue The current/new associated with this attribute. May be null
 *                 if <code>getAttribute</code> is null.
 * @param getOldValue The value previously associated with this attribute. May be null
 *                    if <code>getAttribute</code> is null.
 */
public record AttributeEvent(AttributeSet getSource, Attribute<?> getAttribute, Object getValue, Object getOldValue) {

  public AttributeEvent(AttributeSet source) {
    this(source, null, null, null);
  }

}
