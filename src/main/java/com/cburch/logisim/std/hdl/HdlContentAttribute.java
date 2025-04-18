/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.proj.Project;
import java.awt.Dialog;
import java.awt.Window;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Universal code for any textual simulated HDL format.
 */
public class HdlContentAttribute<C extends HdlContent> extends Attribute<C> {
  /**
   * Returns an HdlContentEditor to edit HdlContent.
   * This is suitable for returning from getCellEditor.
   */
  public static HdlContentEditor getContentEditor(Window source, HdlContent value, Project proj) {
    synchronized (windowRegistry) {
      HdlContentEditor ret = windowRegistry.get(value);
      if (ret == null) {
        if (source instanceof Frame frame) {
          ret = new HdlContentEditor(frame, proj, value);
        } else {
          ret = new HdlContentEditor((Dialog) source, proj, value);
        }
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }

  private static final WeakHashMap<HdlContent, HdlContentEditor> windowRegistry =
      new WeakHashMap<>();

  private final Supplier<C> contentFactory;

  public HdlContentAttribute(Supplier<C> creator) {
    super("content", S.getter("vhdlContentAttr"));
    this.contentFactory = creator;
  }

  @Override
  public java.awt.Component getCellEditor(Window source, C value) {
    final var proj = (source instanceof Frame frame) ? frame.getProject() : null;
    return getContentEditor(source, value, proj);
  }

  @Override
  public C parse(String value) {
    C content = contentFactory.get();
    if (!content.compare(value)) {
      content.setContent(value);
    }
    return content;
  }

  @Override
  public String toDisplayString(HdlContent value) {
    return S.get("vhdlContentValue");
  }

  @Override
  public String toStandardString(HdlContent value) {
    return value.getContent();
  }
}