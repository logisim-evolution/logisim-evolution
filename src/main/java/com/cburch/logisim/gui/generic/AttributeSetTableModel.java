/**
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

package com.cburch.logisim.gui.generic;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.gui.HDLColorRenderer;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class AttributeSetTableModel implements AttrTableModel, AttributeListener {
  private class AttrRow implements AttrTableModelRow {
    private Attribute<Object> attr;

    AttrRow(Attribute<?> attr) {
      @SuppressWarnings("unchecked")
      Attribute<Object> objAttr = (Attribute<Object>) attr;
      this.attr = objAttr;
    }

    public Component getEditor(Window parent) {
      Object value = attrs.getValue(attr);
      return attr.getCellEditor(parent, value);
    }

    public String getLabel() {
      return attr.getDisplayName();
    }

    public String getValue() {
      Object value = attrs.getValue(attr);
      if (value == null) {
        try {
          return attr.toDisplayString(value);
        } catch (NullPointerException e) {
          return "";
        }
      } else {
        try {
          String Str = attr.toDisplayString(value);
          if (Str.isEmpty()
              && attr.getName().equals("label")
              && CompInst != null
              && CompInst.RequiresNonZeroLabel()) return HDLColorRenderer.RequiredFieldString;
          return Str;
        } catch (Exception e) {
          return "???";
        }
      }
    }

    public boolean isValueEditable() {
      return !attrs.isReadOnly(attr);
    }

    public boolean multiEditCompatible(AttrTableModelRow other) {
      if (other == null || !(other instanceof AttrRow)) return false;
      AttrRow o = (AttrRow) other;
      if (!(((Object) attr) instanceof SplitterAttributes.BitOutAttribute)) return false;
      if (!(((Object) o.attr) instanceof SplitterAttributes.BitOutAttribute)) return false;
      SplitterAttributes.BitOutAttribute a = (SplitterAttributes.BitOutAttribute) (Object) attr;
      SplitterAttributes.BitOutAttribute b = (SplitterAttributes.BitOutAttribute) (Object) o.attr;
      return a.sameOptions(b);
    }

    public void setValue(Window parent, Object value) throws AttrTableSetException {
      Attribute<Object> attr = this.attr;
      if (attr == null || value == null) return;

      try {
        if (value instanceof String) {
          value = attr.parse((String) value);
        }
        setValueRequested(attr, value);
      } catch (ClassCastException e) {
        String msg = S.get("attributeChangeInvalidError") + ": " + e;
        throw new AttrTableSetException(msg);
      } catch (NumberFormatException e) {
        String msg = S.get("attributeChangeInvalidError");
        String emsg = e.getMessage();
        if (emsg != null && emsg.length() > 0) msg += ": " + emsg;
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
      if (CompInst == null) return HDLColorRenderer.UnKnownString;
      if (CompInst.HDLSupportedComponent(HDLGeneratorFactory.VHDL, attrs))
        return HDLColorRenderer.VHDLSupportString;
      return HDLColorRenderer.NoSupportString;
    }

    @Override
    public String getValue() {
      if (CompInst == null) return HDLColorRenderer.UnKnownString;
      if (CompInst.HDLSupportedComponent(HDLGeneratorFactory.VERILOG, attrs))
        return HDLColorRenderer.VHDLSupportString;
      return HDLColorRenderer.NoSupportString;
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

  private ArrayList<AttrTableModelListener> listeners;
  private AttributeSet attrs;
  private HashMap<Attribute<?>, AttrRow> rowMap;
  private ArrayList<AttrRow> rows;
  private ComponentFactory CompInst = null;

  public AttributeSetTableModel(AttributeSet attrs) {
    this.attrs = attrs;
    this.listeners = new ArrayList<AttrTableModelListener>();
    this.rowMap = new HashMap<Attribute<?>, AttrRow>();
    this.rows = new ArrayList<AttrRow>();
    if (attrs != null) {
      /* put the vhdl/verilog row */
      HDLrow rowd = new HDLrow(null);
      rows.add(rowd);
      for (Attribute<?> attr : attrs.getAttributes()) {
        if (!attr.isHidden()) {
          AttrRow row = new AttrRow(attr);
          rowMap.put(attr, row);
          rows.add(row);
        }
      }
    }
  }

  public void SetInstance(ComponentFactory fact) {
    CompInst = fact;
  }

  public void SetIsTool() {
    /* We remove the label attribute for a tool */
    for (Attribute<?> attr : attrs.getAttributes()) {
      if (attr.getName().equals("label")) {
        AttrRow row = rowMap.get(attr);
        rowMap.remove(attr);
        rows.remove(row);
      }
    }
  }

  public void addAttrTableModelListener(AttrTableModelListener listener) {
    if (listeners.isEmpty() && attrs != null) {
      attrs.addAttributeListener(this);
    }
    listeners.add(listener);
  }

  //
  // AttributeListener methods
  //
  public void attributeListChanged(AttributeEvent e) {
    // if anything has changed, don't do anything
    int index = 0;
    boolean match = true;
    int rowsSize = rows.size();
    for (Attribute<?> attr : attrs.getAttributes()) {
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
    ArrayList<AttrRow> newRows = new ArrayList<AttrRow>();
    HashSet<Attribute<?>> missing = new HashSet<Attribute<?>>(rowMap.keySet());
    /* put the vhdl/verilog row */
    HDLrow rowd = new HDLrow(null);
    newRows.add(rowd);
    for (Attribute<?> attr : attrs.getAttributes()) {
      if (!attr.isHidden()) {
        AttrRow row = rowMap.get(attr);
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
    for (Attribute<?> attr : missing) {
      rowMap.remove(attr);
    }
    fireStructureChanged();
  }

  public void attributeValueChanged(AttributeEvent e) {
    Attribute<?> attr = e.getAttribute();
    AttrTableModelRow row = rowMap.get(attr);
    if (row != null) {
      int index = rows.indexOf(row);
      if (index >= 0) {
        fireValueChanged(index);
      }
    }
  }

  protected void fireStructureChanged() {
    AttrTableModelEvent event = new AttrTableModelEvent(this);
    for (AttrTableModelListener l : listeners) {
      l.attrStructureChanged(event);
    }
  }

  protected void fireTitleChanged() {
    AttrTableModelEvent event = new AttrTableModelEvent(this);
    for (AttrTableModelListener l : listeners) {
      l.attrTitleChanged(event);
    }
  }

  protected void fireValueChanged(int index) {
    AttrTableModelEvent event = new AttrTableModelEvent(this, index);
    for (AttrTableModelListener l : listeners) {
      l.attrValueChanged(event);
    }
  }

  public AttributeSet getAttributeSet() {
    return attrs;
  }

  public AttrTableModelRow getRow(int rowIndex) {
    return rows.get(rowIndex);
  }

  public int getRowCount() {
    return rows.size();
  }

  public abstract String getTitle();

  public void removeAttrTableModelListener(AttrTableModelListener listener) {
    listeners.remove(listener);
    if (listeners.isEmpty() && attrs != null) {
      attrs.removeAttributeListener(this);
    }
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

  protected abstract void setValueRequested(Attribute<Object> attr, Object value)
      throws AttrTableSetException;
}
