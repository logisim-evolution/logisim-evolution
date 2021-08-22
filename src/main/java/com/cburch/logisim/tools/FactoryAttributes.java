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
import lombok.val;

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

  @Override
  public void addAttributeListener(AttributeListener l) {
    listeners.add(l);
  }

  @Override
  public void attributeListChanged(AttributeEvent baseEvent) {
    AttributeEvent e = null;
    for (val l : listeners) {
      if (e == null) {
        e = new AttributeEvent(
                this, baseEvent.getAttribute(), baseEvent.getValue(), baseEvent.getOldValue());
      }
      l.attributeListChanged(e);
    }
  }

  @Override
  public void attributeValueChanged(AttributeEvent baseEvent) {
    AttributeEvent e = null;
    for (val l : listeners) {
      if (e == null) {
        e = new AttributeEvent(this, baseEvent.getAttribute(), baseEvent.getValue(), baseEvent.getOldValue());
      }
      l.attributeValueChanged(e);
    }
  }

  @Override
  public AttributeSet clone() {
    return (AttributeSet) getBase().clone();
  }

  @Override
  public boolean containsAttribute(Attribute<?> attr) {
    return getBase().containsAttribute(attr);
  }

  @Override
  public Attribute<?> getAttribute(String name) {
    return getBase().getAttribute(name);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return getBase().getAttributes();
  }

  public ComponentFactory getFactory() {
    return (factory != null) ? factory : desc.getFactory(descBase);
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

  @Override
  public <V> V getValue(Attribute<V> attr) {
    return getBase().getValue(attr);
  }

  boolean isFactoryInstantiated() {
    return baseAttrs != null;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return getBase().isReadOnly(attr);
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return getBase().isToSave(attr);
  }

  @Override
  public void removeAttributeListener(AttributeListener l) {
    listeners.remove(l);
  }

  @Override
  public void setReadOnly(Attribute<?> attr, boolean value) {
    getBase().setReadOnly(attr, value);
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    getBase().setValue(attr, value);
  }
}
