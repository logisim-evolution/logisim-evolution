/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import java.util.ArrayList;
import java.util.List;

public class FactoryAttributes implements AttributeSet, AttributeListener, Cloneable {
  private final Class<? extends Library> descBase;
  private final FactoryDescription desc;
  private ComponentFactory factory;
  private AttributeSet baseAttrs;
  private final ArrayList<AttributeListener> listeners;

  public FactoryAttributes(Class<? extends Library> descBase, FactoryDescription desc) {
    this.descBase = descBase;
    this.desc = desc;
    this.factory = null;
    this.baseAttrs = null;
    this.listeners = new ArrayList<>();
  }

  public FactoryAttributes(ComponentFactory factory) {
    this.descBase = null;
    this.desc = null;
    this.factory = factory;
    this.baseAttrs = null;
    this.listeners = new ArrayList<>();
  }

  public void addAttributeListener(AttributeListener l) {
    listeners.add(l);
  }

  public void attributeListChanged(AttributeEvent baseEvent) {
    AttributeEvent e = null;
    for (final var l : listeners) {
      if (e == null) {
        e = new AttributeEvent(
                this, baseEvent.getAttribute(), baseEvent.getValue(), baseEvent.getOldValue());
      }
      l.attributeListChanged(e);
    }
  }

  public void attributeValueChanged(AttributeEvent baseEvent) {
    AttributeEvent e = null;
    for (final var l : listeners) {
      if (e == null) {
        e = new AttributeEvent(
                this, baseEvent.getAttribute(), baseEvent.getValue(), baseEvent.getOldValue());
      }
      l.attributeValueChanged(e);
    }
  }

  @Override
  public AttributeSet clone() {
    return (AttributeSet) getBase().clone();
  }

  public boolean containsAttribute(Attribute<?> attr) {
    return getBase().containsAttribute(attr);
  }

  public Attribute<?> getAttribute(String name) {
    return getBase().getAttribute(name);
  }

  public List<Attribute<?>> getAttributes() {
    return getBase().getAttributes();
  }

  public ComponentFactory getFactory() {
    if (factory != null) return factory;
    else return desc.getFactory(descBase);
  }

  public AttributeSet getBase() {
    var ret = baseAttrs;
    if (ret == null) {
      var fact = factory;
      if (fact == null) {
        fact = desc.getFactory(descBase);
        factory = fact;
      }
      if (fact == null) {
        ret = AttributeSets.EMPTY;
      } else {
        ret = fact.createAttributeSet();
        ret.addAttributeListener(this);
      }
      baseAttrs = ret;
    }
    return ret;
  }

  public <V> V getValue(Attribute<V> attr) {
    return getBase().getValue(attr);
  }

  boolean isFactoryInstantiated() {
    return baseAttrs != null;
  }

  public boolean isReadOnly(Attribute<?> attr) {
    return getBase().isReadOnly(attr);
  }

  public boolean isToSave(Attribute<?> attr) {
    return getBase().isToSave(attr);
  }

  public void removeAttributeListener(AttributeListener l) {
    listeners.remove(l);
  }

  public void setReadOnly(Attribute<?> attr, boolean value) {
    getBase().setReadOnly(attr, value);
  }

  public <V> void setValue(Attribute<V> attr, V value) {
    getBase().setValue(attr, value);
  }
}
