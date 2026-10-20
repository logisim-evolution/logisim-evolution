/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.opts;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.MouseMappings;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;

class OptionsActions {
  private OptionsActions() {}

  public static Action removeMapping(MouseMappings mm, Integer mods) {
    return new RemoveMapping(mm, mods);
  }

  public static Action setAttribute(AttributeSet attrs, Attribute<?> attr, Object value) {
    Object oldValue = attrs.getValue(attr);
    if (!oldValue.equals(value)) {
      return new SetAction(attrs, attr, value);
    } else {
      return null;
    }
  }

  public static Action setMapping(MouseMappings mm, Integer mods, Tool tool) {
    return new SetMapping(mm, mods, tool);
  }

  private static class RemoveMapping extends Action {
    final MouseMappings mm;
    final Integer mods;
    Tool oldtool;

    RemoveMapping(MouseMappings mm, Integer mods) {
      this.mm = mm;
      this.mods = mods;
    }

    @Override
    public void doIt(Project proj) {
      oldtool = mm.getToolFor(mods);
      mm.setToolFor(mods, null);
    }

    @Override
    public String getName() {
      return S.get("removeMouseMappingAction");
    }

    @Override
    public void undo(Project proj) {
      mm.setToolFor(mods, oldtool);
    }
  }

  private static class SetAction extends Action {
    private final AttributeSet attrs;
    private final Attribute<Object> attr;
    private final Object newval;
    private Object oldval;

    SetAction(AttributeSet attrs, Attribute<?> attr, Object value) {
      @SuppressWarnings("unchecked")
      Attribute<Object> a = (Attribute<Object>) attr;
      this.attrs = attrs;
      this.attr = a;
      this.newval = value;
    }

    @Override
    public void doIt(Project proj) {
      oldval = attrs.getValue(attr);
      attrs.setValue(attr, newval);
    }

    @Override
    public String getName() {
      return S.get("setOptionAction", attr.getDisplayName());
    }

    @Override
    public void undo(Project proj) {
      attrs.setValue(attr, oldval);
    }
  }

  private static class SetMapping extends Action {
    final MouseMappings mm;
    final Integer mods;
    Tool oldtool;
    final Tool tool;

    SetMapping(MouseMappings mm, Integer mods, Tool tool) {
      this.mm = mm;
      this.mods = mods;
      this.tool = tool;
    }

    @Override
    public void doIt(Project proj) {
      oldtool = mm.getToolFor(mods);
      mm.setToolFor(mods, tool);
    }

    @Override
    public String getName() {
      return S.get("addMouseMappingAction");
    }

    @Override
    public void undo(Project proj) {
      mm.setToolFor(mods, oldtool);
    }
  }
}
