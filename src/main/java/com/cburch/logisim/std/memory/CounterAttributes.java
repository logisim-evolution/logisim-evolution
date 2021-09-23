/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
