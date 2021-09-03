/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.util.PropertyChangeWeakSupport;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

class Clipboard {
  public static final String CONTENTS_PROPERTY = "contents";
  private static final PropertyChangeWeakSupport propertySupport =
      new PropertyChangeWeakSupport(Clipboard.class);
  private static Clipboard current = null;

  @Getter private final HashSet<Component> components;
  @Getter @Setter private AttributeSet oldAttributeSet;
  @Getter private AttributeSet newAttributeSet;

  /*
   * This function is in charge of copy paste.
   * Now the tunnels' labels are not cleared except if it is requested to.
   */
  private Clipboard(Selection sel, AttributeSet viewAttrs) {
    components = new HashSet<>();
    oldAttributeSet = null;
    newAttributeSet = null;
    for (val base : sel.getComponents()) {
      val baseAttrs = base.getAttributeSet();
      val copyAttrs = (AttributeSet) baseAttrs.clone();

      val copy = base.getFactory().createComponent(base.getLocation(), copyAttrs);
      components.add(copy);
      if (baseAttrs == viewAttrs) {
        oldAttributeSet = baseAttrs;
        newAttributeSet = copyAttrs;
      }
    }
  }

  //
  // PropertyChangeSource methods
  //
  public static void addPropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(listener);
  }

  public static void addPropertyChangeListener(
      String propertyName, PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(propertyName, listener);
  }

  public static Clipboard get() {
    return current;
  }

  public static boolean isEmpty() {
    return current == null || current.components.isEmpty();
  }

  public static void removePropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(listener);
  }

  public static void removePropertyChangeListener(
      String propertyName, PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(propertyName, listener);
  }

  public static void set(Clipboard value) {
    val old = current;
    current = value;
    propertySupport.firePropertyChange(CONTENTS_PROPERTY, old, current);
  }

  public static void set(Selection value, AttributeSet oldAttrs) {
    set(new Clipboard(value, oldAttrs));
  }

}
