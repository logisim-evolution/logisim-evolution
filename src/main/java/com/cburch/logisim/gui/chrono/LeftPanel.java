/*
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

package com.cburch.logisim.gui.chrono;

import static com.cburch.logisim.gui.Strings.S;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.cburch.logisim.gui.log.ComponentIcon;
import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.log.Signal;
import com.cburch.logisim.gui.log.SignalInfo;

// Left panel containing signal names
public class LeftPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private class SignalTableModel extends AbstractTableModel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Override
    public String getColumnName(int col) {
      return (col == 0 ? S.get("SignalName") : S.get("SignalValue"));
    }
    @Override
    public int getColumnCount() { return 2; }
    @SuppressWarnings("unchecked")
    @Override
    public Class getColumnClass(int col) {
      return col == 0 ? SignalInfo.class : Signal.class; }
    @Override
    public int getRowCount() { return model.getSignalCount(); }
    @Override
    public Object getValueAt(int row, int col) {
      return col  == 0 ? model.getSignal(row).info : model.getSignal(row);
    }
    @Override
    public boolean isCellEditable(int row, int col) {
      return col == 1;
    }
  }

  private static final Border rowInsets =
      BorderFactory.createMatteBorder(ChronoPanel.GAP, 0, ChronoPanel.GAP, 0, Color.WHITE);

  private class SignalRenderer extends DefaultTableCellRenderer {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int col) {
      if (!(value instanceof SignalInfo))
        return null;
      Component ret = super.getTableCellRendererComponent(table,
          value, false, false, row, col);
      if (ret instanceof JLabel && value instanceof SignalInfo) {
        JLabel label = (JLabel)ret;
        label.setBorder(rowInsets);
        SignalInfo item = (SignalInfo)value;
        label.setBackground(chronoPanel.rowColors(item, isSelected)[0]);
        label.setIcon(new ComponentIcon(item.getComponent()));
      }
      return ret;
    }
  }

  private class ValueRenderer extends DefaultTableCellRenderer {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int col) {
      if (!(value instanceof Signal))
        return null;
      Signal s = (Signal)value;
      String txt = s.getFormattedValue(chronoPanel.getRightPanel().getCurrentTick());
      Component ret = super.getTableCellRendererComponent(table,
          txt, false, false, row, col);
      if (ret instanceof JLabel) {
        JLabel label = (JLabel) ret;
        label.setBorder(rowInsets);
        label.setIcon(null);
        label.setBackground(chronoPanel.rowColors(s.info, isSelected)[0]);
        label.setHorizontalAlignment(JLabel.CENTER);
      }
      return ret;
    }
  }

  private ChronoPanel chronoPanel;
  private Model model;
  private JTable table;
  private SignalTableModel tableModel;

 public LeftPanel(ChronoPanel chronoPanel) {
  this.chronoPanel = chronoPanel;
  model = chronoPanel.getModel();

  setLayout(new BorderLayout());
  setBackground(Color.WHITE);

  tableModel = new SignalTableModel();
  table = new JTable(tableModel);
  table.setShowGrid(false);
  table.setDefaultRenderer(SignalInfo.class, new SignalRenderer());
  table.setDefaultRenderer(Signal.class, new ValueRenderer());
  table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

  table.setColumnSelectionAllowed(false);
  table.setRowSelectionAllowed(true);

  table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "tick");
  table.getActionMap().put("tick", new AbstractAction() {

  private static final long serialVersionUID = 1L;

  public void actionPerformed(ActionEvent e) {
    // todo
   }
  });
  table.addKeyListener(chronoPanel);
    // highlight on mouse over
  table.addMouseMotionListener(new MouseMotionAdapter() {
   @Override
   public void mouseMoved(MouseEvent e) {
    int row = table.rowAtPoint(e.getPoint());
    if (row >= 0 && e.getComponent() instanceof JTable) {
     chronoPanel.changeSpotlight(model.getSignal(row));
    } else {
     chronoPanel.changeSpotlight(null);
        }
   }
  });
    // popup on right click
  table.addMouseListener(new MouseAdapter() {
   @Override
   public void mouseClicked(MouseEvent e) {
     if (!SwingUtilities.isRightMouseButton(e))
       return;
     if (!(e.getComponent() instanceof JTable))
       return;
     Signal.Collection signals = getSelectedValuesList();
     if (signals.size() == 0) {
       int row = table.rowAtPoint(e.getPoint());
       if (row < 0 || row >= model.getSignalCount())
         return;
       signals.add(model.getSignal(row));
     }
     PopupMenu m = new PopupMenu(chronoPanel, signals);
     m.doPop(e);
   }
  });

  table.getSelectionModel().addListSelectionListener(
    new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        int a = e.getFirstIndex();
        int b = e.getLastIndex();
        chronoPanel.getRightPanel().updateSelected(a, b);
      }
    });
  table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
  table.setDragEnabled(true);
  table.setDropMode(DropMode.INSERT_ROWS);
  table.setTransferHandler(new SignalTransferHandler());

  InputMap inputMap = table.getInputMap();
  inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
  ActionMap actionMap = table.getActionMap();
  actionMap.put("Delete", new AbstractAction() {
    private static final long serialVersionUID = 1L;
    public void actionPerformed(ActionEvent e) {
      removeSelected();
    }
  });

  // calculate default sizes
  int nameWidth = 0, valueWidth = 0;
  TableCellRenderer render = table.getDefaultRenderer(String.class);
  int n = model.getSignalCount();
  for (int i = -1; i < n; i++) {
    String name, val;
    if (i < 0) {
      name = tableModel.getColumnName(0);
      val = tableModel.getColumnName(1);
    } else {
      Signal s = model.getSignal(i);
      name = s.getName();
      val = s.getFormattedMaxValue();
    }
    Component c;
    c = render.getTableCellRendererComponent(table, name, false, false, i, 0);
    nameWidth = Math.max(nameWidth, c.getPreferredSize().width);
    c = render.getTableCellRendererComponent(table, val, false, false, i, 1);
    valueWidth = Math.max(valueWidth, c.getPreferredSize().width);
  }

  table.setFillsViewportHeight(true);
  table.setRowHeight(ChronoPanel.SIGNAL_HEIGHT);
  // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
  TableColumn col;

  col = table.getColumnModel().getColumn(0);
  col.setMinWidth(20);
  col.setPreferredWidth(nameWidth + 10);

  col = table.getColumnModel().getColumn(1);
  col.setMinWidth(20);
  col.setPreferredWidth(valueWidth + 10);

  JTableHeader header = table.getTableHeader();
  Dimension d = header.getPreferredSize();
  d.height = ChronoPanel.HEADER_HEIGHT;
  header.setPreferredSize(d);

  add(header, BorderLayout.NORTH);
  add(table, BorderLayout.CENTER);
}

 public void changeSpotlight(Signal oldSignal, Signal newSignal) {
    if (oldSignal != null)
      tableModel.fireTableRowsUpdated(oldSignal.idx, oldSignal.idx);
    if (newSignal != null)
      tableModel.fireTableRowsUpdated(newSignal.idx, newSignal.idx);
 }

 public void updateSignals() {
    tableModel.fireTableDataChanged();
 }

   public void updateSignalValues() {
     for (int row = 0; row < model.getSignalCount(); row++)
       tableModel.fireTableCellUpdated(row, 1);
   }

  Signal.Collection getSelectedValuesList() {
    Signal.Collection signals = new Signal.Collection();
    int[] sel = table.getSelectedRows();
    for (int i : sel)
      signals.add(model.getSignal(i));
    return signals;
  }

  void removeSelected() {
    int idx = 0;
    Signal.Collection signals = getSelectedValuesList();
    SignalInfo.List items = new SignalInfo.List();
    for (Signal s : signals) {
      items.add(s.info);
      idx = Math.max(idx, s.idx);
    }
    int count = model.remove(items);
    if (count > 0 && model.getSignalCount() > 0) {
      idx = Math.min(idx+1-count, model.getSignalCount() - 1);
      table.setRowSelectionInterval(idx, idx);
    }
    repaint();
  }

  ListSelectionModel getSelectionModel() {
    return table.getSelectionModel();
  }

  private class SignalTransferHandler extends TransferHandler {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    Signal.Collection removing = null;

    @Override
    public int getSourceActions(JComponent comp) {
      return MOVE;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
      removing = getSelectedValuesList();
      if (removing.size() == 0)
        removing = null;
      return removing;
    }

    @Override
    public void exportDone(JComponent comp, Transferable trans, int action) {
      if (removing == null)
        return;
      ArrayList<SignalInfo> items = new ArrayList<>();
      for (Signal s : removing)
        items.add(s.info);
      removing = null;
      model.remove(items);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      return support.isDataFlavorSupported(Signal.Collection.dataFlavor);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
      if (removing == null) {
        return false;
      }
      Signal.Collection signals = removing;
      removing = null;
      try {
        Signal.Collection s2 = (Signal.Collection)support.getTransferable().getTransferData(Signal.Collection.dataFlavor);
        int newIdx = model.getSignalCount();
        if (support.isDrop()) {
          try {
            JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
            newIdx = Math.min(newIdx, dl.getRow());
          } catch (ClassCastException e) {
          }
        }
        if (s2 != signals) {
          return false;
        }
        int[] idx = new int[signals.size()];
        int i = 0;
        for (Signal s : signals)
          idx[i++] = s.idx;
        model.move(idx, newIdx);
        return true;
      } catch (UnsupportedFlavorException | IOException e) {
        e.printStackTrace();
        return false;
      }
    }
  }
 }