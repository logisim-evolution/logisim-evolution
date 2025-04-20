/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

/**
 * BlifCircuitAttributes is essentially a copy of VhdlEntityAttributes, but for BLIF.
 */
public class BlifCircuitAttributes extends AbstractAttributeSet {

  private static final List<Attribute<?>> attributes =
      Arrays.asList(
          BlifCircuitComponent.CONTENT_ATTR,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY);

  private BlifContentComponent content;
  private String label = "";
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = false;

  BlifCircuitAttributes() {
    content = BlifContentComponent.create();
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    final var attr = (BlifCircuitAttributes) dest;
    attr.labelFont = labelFont;
    attr.content = content.clone();
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attributes;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == BlifCircuitComponent.CONTENT_ATTR) {
      return (V) content;
    }
    if (attr == StdAttr.LABEL) {
      return (V) label;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) labelFont;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      return (V) labelVisible;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == BlifCircuitComponent.CONTENT_ATTR) {
      final var newContent = (BlifContentComponent) value;
      if (!content.equals(newContent)) {
        content = newContent;
      }
      fireAttributeValueChanged(attr, value, null);
    }
    if (attr == StdAttr.LABEL && value instanceof String newLabel) {
      final var oldlabel = label;
      if (label.equals(newLabel)) {
        return;
      }
      label = newLabel;
      fireAttributeValueChanged(attr, value, (V) oldlabel);
    }
    if (attr == StdAttr.LABEL_FONT && value instanceof Font newFont) {
      if (labelFont.equals(newFont)) {
        return;
      }
      labelFont = newFont;
      fireAttributeValueChanged(attr, value, null);
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      final var newVis = (Boolean) value;
      if (labelVisible.equals(newVis)) {
        return;
      }
      labelVisible = newVis;
      fireAttributeValueChanged(attr, value, null);
    }
  }
}
