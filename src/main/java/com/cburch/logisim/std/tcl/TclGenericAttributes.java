/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.tcl;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.hdl.HdlContent;
import com.cburch.logisim.std.hdl.HdlContentEditor;
import com.cburch.logisim.std.hdl.VhdlContentComponent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

/**
 * This attribute set is the same as the one for the TclComponent but it adds an attribute to
 * specify the interface VHDL entity definition. It calls the parent class as often as possible to
 * avoid code duplication.
 *
 * @author christian.mueller@heig-vd.ch
 */
public class TclGenericAttributes extends TclComponentAttributes {

  public static HdlContentEditor getContentEditor(Window source, HdlContent value, Project proj) {
    synchronized (windowRegistry) {
      var ret = windowRegistry.get(value);
      if (ret == null) {
        ret = (source instanceof Frame)
                ? new HdlContentEditor((Frame) source, proj, value)
                : new HdlContentEditor((Dialog) source, proj, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }

  private static final List<Attribute<?>> attributes =
      Arrays.asList(CONTENT_FILE_ATTR, TclGeneric.CONTENT_ATTR, StdAttr.LABEL, StdAttr.LABEL_FONT);

  private static final WeakHashMap<HdlContent, HdlContentEditor> windowRegistry = new WeakHashMap<>();

  private VhdlContentComponent vhdlEntitiy;

  TclGenericAttributes() {
    super();

    /*
     * The editor is the same as for the VhdlContent, only the base template
     * changes
     */
    vhdlEntitiy = TclVhdlEntityContent.create();
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    final var attr = (TclGenericAttributes) dest;
    attr.vhdlEntitiy = vhdlEntitiy;

    super.copyInto(dest);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attributes;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == TclGeneric.CONTENT_ATTR) {
      return (V) vhdlEntitiy;
    } else {
      return super.getValue(attr);
    }
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == TclGeneric.CONTENT_ATTR) {
      final var newContent = (VhdlContentComponent) value;
      if (!vhdlEntitiy.equals(newContent)) vhdlEntitiy = newContent;
      fireAttributeValueChanged(attr, value, null);
    } else {
      super.setValue(attr, value);
    }
  }
}
