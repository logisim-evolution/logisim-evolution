/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.gui;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.bus.SocBusMenuProvider;
import com.cburch.logisim.soc.data.SocBusStateInfo;
import com.cburch.logisim.soc.data.SocBusStateInfo.SocBusState;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.tools.CircuitStateHolder;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

public class TraceWindowTableModel extends AbstractTableModel
    implements BaseMouseListenerContract,
        SocBusStateInfo.SocBusStateListener,
        ComponentListener,
        CircuitListener {

  private static final long serialVersionUID = 1L;
  private final SocBusMenuProvider.InstanceInformation parent;
  private final Map<SocBusStateInfo.SocBusState, CircuitStateHolder.HierarchyInfo> myTraceList;
  private JTable table;
  private int BoxWidth = SocBusStateInfo.BLOCK_WIDTH;
  public TraceWindowTableModel(Map<SocBusState, CircuitStateHolder.HierarchyInfo> traceList,
                               SocBusMenuProvider.InstanceInformation p) {
    myTraceList = traceList;
    parent = p;
    rebuild();
  }

  public String getColumnHeader(int column) {
    SocBusStateInfo.SocBusState info = getInfoAtColumn(column);
    if (info == null || myTraceList.get(info) == null) return "BUG";
    else {
      myTraceList.get(info).registerCircuitListener(this);
      myTraceList.get(info).registerComponentListener(this);
      return myTraceList.get(info).getName();
    }
  }

  public void setColMod(JTable v) {
    table = v;
    table.addMouseListener(this);
  }

  public void rebuild() {
    for (SocBusStateInfo.SocBusState i : myTraceList.keySet())
      if (myTraceList.get(i) != null) i.registerListener(this);
      else i.deregisterListener(this);
    fireTableStructureChanged();
    if (table != null) {
      for (int i = 0; i < getColumnCount(); i++)
        table.getColumnModel().getColumn(i).setPreferredWidth(AppPreferences.getScaled(BoxWidth));
      table.setRowHeight(
          AppPreferences.getScaled(
              2 * SocBusStateInfo.TRACE_HEIGHT + SocBusStateInfo.TRACE_HEIGHT / 2));
      table
          .getTableHeader()
          .setPreferredSize(
              new Dimension(AppPreferences.getScaled(BoxWidth), AppPreferences.getScaled(20)));
    }
  }

  public int getBoxWidth() {
    return BoxWidth;
  }

  public void setBoxWidth(int value) {
    BoxWidth = value;
    rebuild();
  }

  @Override
  public int getRowCount() {
    int max = 1;
    for (SocBusStateInfo.SocBusState i : myTraceList.keySet())
      if (myTraceList.get(i) != null) if (i.getNrOfEntires() > max) max = i.getNrOfEntires();
    return max;
  }

  @Override
  public int getColumnCount() {
    int cols = 0;
    for (SocBusStateInfo.SocBusState i : myTraceList.keySet())
      if (myTraceList.get(i) != null) cols++;
    return cols;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return false;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    SocBusStateInfo.SocBusState.SocBusStateTrace info =
        getInfoAtColumn(columnIndex).getEntry(rowIndex, this);
    if (info != null && info.getTransaction() != null) {
      SocBusTransaction trans = info.getTransaction();
      Object master = trans.getTransactionInitiator();
      if (master instanceof Component) ((Component) master).addComponentListener(this);
      if (trans.getTransactionResponder() != null)
        trans.getTransactionResponder().addComponentListener(this);
    }
    return info;
  }

  private SocBusStateInfo.SocBusState getInfoAtColumn(int column) {
    ArrayList<SocBusStateInfo.SocBusState> sortedList =
        new ArrayList<>();
    for (SocBusStateInfo.SocBusState i : myTraceList.keySet())
      if (myTraceList.get(i) != null) {
        if (sortedList.isEmpty()) sortedList.add(i);
        else {
          boolean inserted = false;
          for (int j = 0; j < sortedList.size(); j++) {
            if (myTraceList.get(i).getName().compareTo(myTraceList.get(sortedList.get(j)).getName())
                <= 0) {
              sortedList.add(j, i);
              inserted = true;
              break;
            }
          }
          if (!inserted) sortedList.add(i);
        }
      }
    if (column < 0 || column >= sortedList.size()) return null;
    return sortedList.get(column);
  }

  @Override
  public void fireCanged(SocBusState item) {
    if (myTraceList.containsKey(item) && myTraceList.get(item) != null) fireTableDataChanged();
  }

  public TableCellRenderer getCellRenderer() {
    return new CellRenderer();
  }

  public TableCellRenderer getHeaderRenderer() {
    return new HeaderRenderer();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() > 1) {
      java.awt.Point p = e.getPoint();
      int index = table.getColumnModel().getColumnIndexAtX(p.x);
      int realIndex = table.getColumnModel().getColumn(index).getModelIndex();
      SocBusStateInfo.SocBusState i = getInfoAtColumn(realIndex);
      if (i != null && myTraceList.get(i) != null) {
        myTraceList.get(i).deregisterCircuitListener(this);
        myTraceList.get(i).deregisterComponentListener(this);
        myTraceList.put(i, null);
        rebuild();
        if (getColumnCount() == 0) parent.destroyTraceWindow();
      }
    }
  }

  @Override
  public void labelChanged(ComponentEvent e) {
    rebuild();
  }

  @Override
  public void circuitChanged(CircuitEvent event) {
    if (event.getAction() == CircuitEvent.ACTION_SET_NAME) rebuild();
  }

  private class HeaderRenderer extends JLabel implements TableCellRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    public java.awt.Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JLabel l = new JLabel(getColumnHeader(column));
      l.setBorder(BorderFactory.createEtchedBorder());
      return l;
    }
  }

  private static class CellRenderer extends JPanel implements TableCellRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    public java.awt.Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      return (JPanel) value;
    }
  }
}
