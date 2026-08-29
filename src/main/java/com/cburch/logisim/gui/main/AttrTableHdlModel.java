/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.vhdl.base.HdlModel;
import com.cburch.logisim.vhdl.base.HdlModelListener;
import com.cburch.logisim.vhdl.base.VhdlContent;

class AttrTableHdlModel extends AttributeSetTableModel implements HdlModelListener {
  private final Project proj;
  private final VhdlContent hdl;

  AttrTableHdlModel(Project proj, VhdlContent hdl) {
    super(hdl.getStaticAttributes());
    this.proj = proj;
    this.hdl = hdl;
    hdl.addHdlModelListener(this);
  }

  @Override
  public void contentSet(HdlModel source) {
    setAttributeSet(hdl.getStaticAttributes());
    fireTitleChanged();
  }

  @Override
  public void displayChanged(HdlModel source) {
    fireTitleChanged();
  }

  @Override
  public String getTitle() {
    return S.get("hdlAttrTitle", hdl.getName());
  }

  @Override
  public void setValueRequested(final Attribute<Object> attr, Object value)
      throws AttrTableSetException {
    proj.doAction(new HdlAttributeAction(hdl, attr, value));
  }

  private static class HdlAttributeAction extends Action {
    private final VhdlContent hdl;
    private final Attribute<Object> attr;
    private final Object newValue;
    private Object oldValue;

    HdlAttributeAction(VhdlContent hdl, Attribute<Object> attr, Object newValue) {
      this.hdl = hdl;
      this.attr = attr;
      this.newValue = newValue;
    }

    @Override
    public void doIt(Project proj) {
      oldValue = hdl.getStaticAttributes().getValue(attr);
      hdl.getStaticAttributes().setValue(attr, newValue);
    }

    @Override
    public String getName() {
      return "VHDL attributes";
    }

    @Override
    public void undo(Project proj) {
      hdl.getStaticAttributes().setValue(attr, oldValue);
    }
  }
}
