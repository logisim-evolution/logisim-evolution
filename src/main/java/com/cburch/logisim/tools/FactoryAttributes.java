/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
    for (AttributeListener l : listeners) {
      if (e == null) {
        e =
            new AttributeEvent(
                this, baseEvent.getAttribute(), baseEvent.getValue(), baseEvent.getOldValue());
      }
      l.attributeListChanged(e);
    }
  }

  public void attributeValueChanged(AttributeEvent baseEvent) {
    AttributeEvent e = null;
    for (AttributeListener l : listeners) {
      if (e == null) {
        e =
            new AttributeEvent(
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
    AttributeSet ret = baseAttrs;
    if (ret == null) {
      ComponentFactory fact = factory;
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
