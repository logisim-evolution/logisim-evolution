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

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import java.util.List;
import java.util.Objects;

class CounterAttributes extends AbstractAttributeSet {

  private AttributeSet base;

  public CounterAttributes() {
    base =
        AttributeSets.fixedSet(
            new Attribute<?>[] {
              StdAttr.WIDTH,
              Counter.ATTR_MAX,
              Counter.ATTR_ON_GOAL,
              StdAttr.EDGE_TRIGGER,
              StdAttr.LABEL,
              StdAttr.LABEL_FONT,
              StdAttr.LABEL_LOC,
              StdAttr.APPEARANCE
            },
            new Object[] {
              BitWidth.create(8),
                0xFFL,
              Counter.ON_GOAL_WRAP,
              StdAttr.TRIG_RISING,
              "",
              StdAttr.DEFAULT_LABEL_FONT,
              Direction.NORTH,
              AppPreferences.getDefaultAppearance()
            });
  }

  @Override
  public boolean containsAttribute(Attribute<?> attr) {
    return base.containsAttribute(attr);
  }

  @Override
  public void copyInto(AbstractAttributeSet dest) {
    ((CounterAttributes) dest).base = (AttributeSet) this.base.clone();
  }

  @Override
  public Attribute<?> getAttribute(String name) {
    return base.getAttribute(name);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return base.getAttributes();
  }

  @Override
  public <V> V getValue(Attribute<V> attr) {
    return base.getValue(attr);
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return base.isReadOnly(attr);
  }

  @Override
  public void setReadOnly(Attribute<?> attr, boolean value) {
    base.setReadOnly(attr, value);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    Object oldValue = base.getValue(attr);
    if (Objects.equals(oldValue, value)) return;

    Long newMax = null;
    if (attr == StdAttr.WIDTH) {
      final var oldWidth = base.getValue(StdAttr.WIDTH);
      final var newWidth = (BitWidth) value;
      final var oldW = oldWidth.getWidth();
      final var newW = newWidth.getWidth();
      final var oldValObj = base.getValue(Counter.ATTR_MAX);
      long oldVal = oldValObj;
      base.setValue(StdAttr.WIDTH, newWidth);
      if (newW > oldW) {
        newMax = newWidth.getMask();
      } else {
        final long v = oldVal & newWidth.getMask();
        if (v != oldVal) {
          Long newValObj = v;
          base.setValue(Counter.ATTR_MAX, newValObj);
          fireAttributeValueChanged(Counter.ATTR_MAX, newValObj, null);
        }
      }
      fireAttributeValueChanged(StdAttr.WIDTH, newWidth, null);
    } else if (attr == Counter.ATTR_MAX) {
      final long oldVal = base.getValue(Counter.ATTR_MAX);
      final var width = base.getValue(StdAttr.WIDTH);
      final long newVal = (Long) value & width.getMask();
      if (newVal != oldVal) {
        value = (V) Long.valueOf(newVal);
      }
    }
    base.setValue(attr, value);
    fireAttributeValueChanged(attr, value, (V) oldValue);
    if (newMax != null) {
      base.setValue(Counter.ATTR_MAX, newMax);
      fireAttributeValueChanged(Counter.ATTR_MAX, newMax, null);
    }
  }
}
