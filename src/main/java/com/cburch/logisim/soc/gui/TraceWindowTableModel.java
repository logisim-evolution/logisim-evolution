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
  private int boxWidth = SocBusStateInfo.BLOCK_WIDTH;

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
      if (myTraceList.get(i) != null) {
        i.registerListener(this);
      } else {
        i.deregisterListener(this);
      }
    fireTableStructureChanged();
    if (table != null) {
      for (var i = 0; i < getColumnCount(); i++) {
        table.getColumnModel().getColumn(i).setPreferredWidth(AppPreferences.getScaled(boxWidth));
      }
      final var height = 2 * SocBusStateInfo.TRACE_HEIGHT + SocBusStateInfo.TRACE_HEIGHT / 2;
      table.setRowHeight(AppPreferences.getScaled(height));
      table
          .getTableHeader()
          .setPreferredSize(new Dimension(AppPreferences.getScaled(boxWidth), AppPreferences.getScaled(20)));
    }
  }

  public int getBoxWidth() {
    return boxWidth;
  }

  public void setBoxWidth(int value) {
    boxWidth = value;
    rebuild();
  }

  @Override
  public int getRowCount() {
    var max = 1;
    for (final var i : myTraceList.keySet()) {
      if (myTraceList.get(i) != null) {
        if (i.getNrOfEntires() > max) {
          max = i.getNrOfEntires();
        }
      }
    }
    return max;
  }

  @Override
  public int getColumnCount() {
    var cols = 0;
    for (final var i : myTraceList.keySet()) {
      if (myTraceList.get(i) != null) cols++;
    }
    return cols;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return false;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    final var info = getInfoAtColumn(columnIndex);
    if (info == null) return null;

    final var stateInfo = info.getEntry(rowIndex, this);
    if (stateInfo != null && stateInfo.getTransaction() != null) {
      final var trans = stateInfo.getTransaction();
      final var master = trans.getTransactionInitiator();
      if (master instanceof Component masterComp) {
        masterComp.addComponentListener(this);
      }
      if (trans.getTransactionResponder() != null) {
        trans.getTransactionResponder().addComponentListener(this);
      }
    }
    return stateInfo;
  }

  private SocBusStateInfo.SocBusState getInfoAtColumn(int column) {
    final var sortedList = new ArrayList<SocBusStateInfo.SocBusState>();
    for (final var info : myTraceList.keySet())
      if (myTraceList.get(info) != null) {
        if (sortedList.isEmpty()) {
          sortedList.add(info);
        } else {
          var inserted = false;
          for (var j = 0; j < sortedList.size(); j++) {
            final var sortedKey = myTraceList.get(sortedList.get(j)).getName();
            if (myTraceList.get(info).getName().compareTo(sortedKey) <= 0) {
              sortedList.add(j, info);
              inserted = true;
              break;
            }
          }
          if (!inserted) sortedList.add(info);
        }
      }
    return (column < 0 || column >= sortedList.size()) ? null : sortedList.get(column);
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
      final var point = e.getPoint();
      final var index = table.getColumnModel().getColumnIndexAtX(point.x);
      final var realIndex = table.getColumnModel().getColumn(index).getModelIndex();
      final var info = getInfoAtColumn(realIndex);
      if (info != null && myTraceList.get(info) != null) {
        myTraceList.get(info).deregisterCircuitListener(this);
        myTraceList.get(info).deregisterComponentListener(this);
        myTraceList.put(info, null);
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
      final var label = new JLabel(getColumnHeader(column));
      label.setBorder(BorderFactory.createEtchedBorder());
      return label;
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
