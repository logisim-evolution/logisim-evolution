/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.vhdl.base.VhdlSimConstants;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

public class VhdlEntityAttributes extends AbstractAttributeSet {

  public static HdlContentEditor getContentEditor(Window source, HdlContent value, Project proj) {
    synchronized (windowRegistry) {
      HdlContentEditor ret = windowRegistry.get(value);
      if (ret == null) {
        if (source instanceof Frame) ret = new HdlContentEditor((Frame) source, proj, value);
        else ret = new HdlContentEditor((Dialog) source, proj, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }

  private static final List<Attribute<?>> attributes =
      Arrays.asList(
          VhdlEntityComponent.CONTENT_ATTR,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          VhdlSimConstants.SIM_NAME_ATTR);

  private static final WeakHashMap<HdlContent, HdlContentEditor> windowRegistry =
      new WeakHashMap<>();

  private VhdlContentComponent content;
  private String label = "";
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = false;
  private String simName = "";

  VhdlEntityAttributes() {
    content = VhdlContentComponent.create();
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    final var attr = (VhdlEntityAttributes) dest;
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
    if (attr == VhdlEntityComponent.CONTENT_ATTR) {
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
    if (attr == VhdlSimConstants.SIM_NAME_ATTR) {
      return (V) simName;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == VhdlEntityComponent.CONTENT_ATTR) {
      final var newContent = (VhdlContentComponent) value;
      if (!content.equals(newContent)) content = newContent;
      fireAttributeValueChanged(attr, value, null);
    }
    if (attr == StdAttr.LABEL && value instanceof String) {
      final var newLabel = (String) value;
      final var oldlabel = label;
      if (label.equals(newLabel)) return;
      label = newLabel;
      fireAttributeValueChanged(attr, value, (V) oldlabel);
    }
    if (attr == StdAttr.LABEL_FONT && value instanceof Font) {
      final var newFont = (Font) value;
      if (labelFont.equals(newFont)) return;
      labelFont = newFont;
      fireAttributeValueChanged(attr, value, null);
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      final var newvis = (Boolean) value;
      if (labelVisible.equals(newvis)) return;
      labelVisible = newvis;
      fireAttributeValueChanged(attr, value, null);
    }
    if (attr == VhdlSimConstants.SIM_NAME_ATTR) {
      final var Name = (String) value;
      if (value.equals(simName)) return;
      simName = Name;
      fireAttributeValueChanged(attr, value, null);
    }
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave() && attr != VhdlSimConstants.SIM_NAME_ATTR;
  }
}
