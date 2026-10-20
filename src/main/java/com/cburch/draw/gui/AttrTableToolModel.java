/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.gui;

import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;

class AttrTableToolModel extends AttributeSetTableModel {
  private final DrawingAttributeSet defaults;
  private AbstractTool currentTool;

  public AttrTableToolModel(DrawingAttributeSet defaults, AbstractTool tool) {
    super(defaults.createSubset(tool));
    this.defaults = defaults;
    this.currentTool = tool;
  }

  @Override
  public String getTitle() {
    return currentTool.getDescription();
  }

  public void setTool(AbstractTool value) {
    currentTool = value;
    setAttributeSet(defaults.createSubset(value));
    fireTitleChanged();
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) {
    defaults.setValue(attr, value);
  }
}
