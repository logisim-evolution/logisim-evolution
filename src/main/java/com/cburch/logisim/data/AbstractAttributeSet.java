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

package com.cburch.logisim.data;

import java.util.ArrayList;
import java.util.List;

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

  protected abstract void copyInto(AbstractAttributeSet dest);

  protected void fireAttributeListChanged() {
    if (listeners != null) {
      final var event = new AttributeEvent(this);
      for (final var l : new ArrayList<>(listeners)) {
        l.attributeListChanged(event);
      }
    }
  }

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
  public abstract List<Attribute<?>> getAttributes();

  @Override
  public abstract <V> V getValue(Attribute<V> attr);

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

  @Override
  public abstract <V> void setValue(Attribute<V> attr, V value);

}
