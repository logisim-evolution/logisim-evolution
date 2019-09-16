/**
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

package com.cburch.logisim.gui.main;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.util.PropertyChangeWeakSupport;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;

class Clipboard {
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
    Clipboard old = current;
    current = value;
    propertySupport.firePropertyChange(contentsProperty, old, current);
  }

  public static void set(Selection value, AttributeSet oldAttrs) {
    set(new Clipboard(value, oldAttrs));
  }

  public static final String contentsProperty = "contents";
  private static Clipboard current = null;
  private static PropertyChangeWeakSupport propertySupport =
      new PropertyChangeWeakSupport(Clipboard.class);

  //
  // instance variables and methods
  //
  private HashSet<Component> components;
  private AttributeSet oldAttrs;
  private AttributeSet newAttrs;
  /*
   * This function is in charge of copy paste.
   * Now the tunnels' labels are not cleared except if it is requested to.
   */
  private Clipboard(Selection sel, AttributeSet viewAttrs) {
    components = new HashSet<Component>();
    oldAttrs = null;
    newAttrs = null;
    for (Component base : sel.getComponents()) {
      AttributeSet baseAttrs = base.getAttributeSet();
      AttributeSet copyAttrs = (AttributeSet) baseAttrs.clone();

      Component copy = base.getFactory().createComponent(base.getLocation(), copyAttrs);
      components.add(copy);
      if (baseAttrs == viewAttrs) {
        oldAttrs = baseAttrs;
        newAttrs = copyAttrs;
      }
    }
  }

  public Collection<Component> getComponents() {
    return components;
  }

  public AttributeSet getNewAttributeSet() {
    return newAttrs;
  }

  public AttributeSet getOldAttributeSet() {
    return oldAttrs;
  }

  void setOldAttributeSet(AttributeSet value) {
    oldAttrs = value;
  }
}
