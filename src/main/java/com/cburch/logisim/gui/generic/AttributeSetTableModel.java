/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.gui.HdlColorRenderer;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class AttributeSetTableModel implements AttrTableModel, AttributeListener {
  private final ArrayList<AttrTableModelListener> listeners;
  private final HashMap<Attribute<?>, AttrRow> rowMap;
  private AttributeSet attrs;
  private ArrayList<AttrRow> rows;
  private ComponentFactory compInst = null;

  public AttributeSetTableModel(AttributeSet attrs) {
    this.attrs = attrs;
    this.listeners = new ArrayList<>();
    this.rowMap = new HashMap<>();
    this.rows = new ArrayList<>();
    if (attrs != null) {
      /* put the vhdl/verilog row */
      final var rowd = new HDLrow(null);
      rows.add(rowd);
      for (final var attr : attrs.getAttributes()) {
        if (!attr.isHidden()) {
          final var row = new AttrRow(attr);
          rowMap.put(attr, row);
          rows.add(row);
        }
      }
    }
  }

  public void setInstance(ComponentFactory fact) {
    compInst = fact;
  }

  public void setIsTool() {
    /* We remove the label attribute for a tool */
    for (final var attr : attrs.getAttributes()) {
      if ("label".equals(attr.getName())) {
        final var row = rowMap.get(attr);
        rowMap.remove(attr);
        rows.remove(row);
      }
    }
  }

  @Override
  public void addAttrTableModelListener(AttrTableModelListener listener) {
    if (attrs != null && listeners.isEmpty()) {
      attrs.addAttributeListener(this);
    }
    listeners.add(listener);
  }

  //
  // AttributeListener methods
  //
  @Override
  public void attributeListChanged(AttributeEvent e) {
    // if anything has changed, don't do anything
    var index = 0;
    var match = true;
    var rowsSize = rows.size();
    for (final var attr : attrs.getAttributes()) {
      if (!attr.isHidden()) {
        if (index >= rowsSize || rows.get(index).attr != attr) {
          match = false;
          break;
        }
        index++;
      }
    }
    if (match && index == rows.size()) return;

    // compute the new list of rows, possible adding into hash map
    final var newRows = new ArrayList<AttrRow>();
    final var missing = new HashSet<Attribute<?>>(rowMap.keySet());
    /* put the vhdl/verilog row */
    final var rowd = new HDLrow(null);
    newRows.add(rowd);
    for (final var attr : attrs.getAttributes()) {
      if (!attr.isHidden()) {
        var row = rowMap.get(attr);
        if (row == null) {
          row = new AttrRow(attr);
          rowMap.put(attr, row);
        } else {
          missing.remove(attr);
        }
        newRows.add(row);
      }
    }
    rows = newRows;
    for (final var attr : missing) {
      rowMap.remove(attr);
    }
    fireStructureChanged();
  }

  @Override
  public void attributeValueChanged(AttributeEvent e) {
    final var attr = e.getAttribute();
    final var row = rowMap.get(attr);
    if (row != null) {
      final var index = rows.indexOf(row);
      if (index >= 0) {
        fireValueChanged(index);
      }
    }
  }

  protected void fireStructureChanged() {
    final var event = new AttrTableModelEvent(this);
    for (final var l : listeners) {
      l.attrStructureChanged(event);
    }
  }

  protected void fireTitleChanged() {
    final var event = new AttrTableModelEvent(this);
    for (final var l : listeners) {
      l.attrTitleChanged(event);
    }
  }

  protected void fireValueChanged(int index) {
    final var event = new AttrTableModelEvent(this, index);
    for (final var l : listeners) {
      l.attrValueChanged(event);
    }
  }

  public AttributeSet getAttributeSet() {
    return attrs;
  }

  public void setAttributeSet(AttributeSet value) {
    if (attrs != value) {
      if (!listeners.isEmpty()) {
        attrs.removeAttributeListener(this);
      }
      attrs = value;
      if (!listeners.isEmpty()) {
        attrs.addAttributeListener(this);
      }
      attributeListChanged(null);
    }
  }

  @Override
  public AttrTableModelRow getRow(int rowIndex) {
    return rows.get(rowIndex);
  }

  @Override
  public int getRowCount() {
    return rows.size();
  }

  @Override
  public void removeAttrTableModelListener(AttrTableModelListener listener) {
    listeners.remove(listener);
    if (attrs != null && listeners.isEmpty()) {
      attrs.removeAttributeListener(this);
    }
  }

  protected abstract void setValueRequested(Attribute<Object> attr, Object value) throws AttrTableSetException;

  private class AttrRow implements AttrTableModelRow {
    private final Attribute<Object> attr;

    AttrRow(Attribute<?> attr) {
      @SuppressWarnings("unchecked")
      Attribute<Object> objAttr = (Attribute<Object>) attr;
      this.attr = objAttr;
    }

    @Override
    public Component getEditor(Window parent) {
      final var value = attrs.getValue(attr);
      return attr.getCellEditor(parent, value);
    }

    @Override
    public String getLabel() {
      return attr.getDisplayName();
    }

    @Override
    public String getValue() {
      final var value = attrs.getValue(attr);
      if (value == null) {
        try {
          return attr.toDisplayString(value);
        } catch (NullPointerException e) {
          return "";
        }
      } else {
        try {
          final var str = attr.toDisplayString(value);
          if (str.isEmpty()
              && "label".equals(attr.getName())
              && compInst != null
              && compInst.requiresNonZeroLabel()) return HdlColorRenderer.REQUIRED_FIELD_STRING;
          return str;
        } catch (Exception e) {
          return "???";
        }
      }
    }

    @Override
    public boolean isValueEditable() {
      return !attrs.isReadOnly(attr);
    }

    @Override
    public boolean multiEditCompatible(AttrTableModelRow other) {
      if (!(other instanceof AttrRow o)) return false;
      if (!(((Object) attr) instanceof SplitterAttributes.BitOutAttribute)) return false;
      if (!(((Object) o.attr) instanceof SplitterAttributes.BitOutAttribute)) return false;
      final var a = (SplitterAttributes.BitOutAttribute) (Object) attr;
      final var b = (SplitterAttributes.BitOutAttribute) (Object) o.attr;
      return a.sameOptions(b);
    }

    @Override
    public void setValue(Window parent, Object value) throws AttrTableSetException {
      final var attr = this.attr;
      if (attr == null || value == null) return;

      try {
        if (value instanceof String) {
          value = attr.parse((String) value);
        }
        setValueRequested(attr, value);
      } catch (ClassCastException e) {
        final var msg = S.get("attributeChangeInvalidError") + ": " + e;
        throw new AttrTableSetException(msg);
      } catch (NumberFormatException e) {
        var msg = S.get("attributeChangeInvalidError");
        final var eMsg = e.getMessage();
        if (eMsg != null && eMsg.length() > 0) msg += ": " + eMsg;
        msg += ".";
        throw new AttrTableSetException(msg);
      }
    }
  }

  private class HDLrow extends AttrRow {

    HDLrow(Attribute<?> attr) {
      super(attr);
    }

    @Override
    public String getLabel() {
      return S.get("FPGA_Supported");
    }

    @Override
    public String getValue() {
      if (compInst == null) return HdlColorRenderer.UNKNOWN_STRING;
      return (compInst.isHDLSupportedComponent(attrs))
          ? HdlColorRenderer.SUPPORT_STRING
          : HdlColorRenderer.NO_SUPPORT_STRING;
    }

    @Override
    public boolean isValueEditable() {
      return false;
    }

    @Override
    public void setValue(Window parent, Object value) {
      // Do Nothing
    }
  }
}
