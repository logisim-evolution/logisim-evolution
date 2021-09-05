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

import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.log.Signal;
import com.cburch.logisim.gui.log.SignalInfo;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

// Left panel containing signal names
public class LeftPanel extends JTable {
  private static final long serialVersionUID = 1L;

  private class SignalTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    @Override
    public String getColumnName(int col) {
      return (col == 0 ? S.get("SignalName") : S.get("SignalValue"));
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class getColumnClass(int col) {
      return col == 0 ? SignalInfo.class : Signal.class;
    }

    @Override
    public int getRowCount() {
      return model.getSignalCount();
    }

    @Override
    public Object getValueAt(int row, int col) {
      return col == 0 ? model.getSignal(row).info : model.getSignal(row);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
      return false;
    }
  }

  private static final Border rowInsets =
      BorderFactory.createMatteBorder(ChronoPanel.GAP, 0, ChronoPanel.GAP, 0, Color.WHITE);

  private class SignalRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
      if (!(value instanceof SignalInfo)) return null;
      Component ret = super.getTableCellRendererComponent(table, value, false, false, row, col);
      if (ret instanceof JLabel && value instanceof SignalInfo) {
        JLabel label = (JLabel) ret;
        label.setBorder(rowInsets);
        SignalInfo item = (SignalInfo) value;
        label.setBackground(chronoPanel.rowColors(item, isSelected)[0]);
        label.setIcon(item.icon);
      }
      return ret;
    }
  }

  private class ValueRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
      if (!(value instanceof Signal)) return null;
      Signal s = (Signal) value;
      String txt = s.getFormattedValue(chronoPanel.getRightPanel().getCurrentTime());
      Component ret = super.getTableCellRendererComponent(table, txt, false, false, row, col);
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

  private final ChronoPanel chronoPanel;
  private Model model;
  private final SignalTableModel tableModel;

  public LeftPanel(ChronoPanel chronoPanel) {
    this.chronoPanel = chronoPanel;
    model = chronoPanel.getModel();

    setLayout(new BorderLayout());
    setBackground(Color.WHITE);

    tableModel = new SignalTableModel();
    setModel(tableModel);
    setShowGrid(false);
    setDefaultRenderer(SignalInfo.class, new SignalRenderer());
    setDefaultRenderer(Signal.class, new ValueRenderer());
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    setColumnSelectionAllowed(false);
    setRowSelectionAllowed(true);

    // highlight on mouse over
    addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseMoved(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (row >= 0 && e.getComponent() instanceof JTable) {
              chronoPanel.changeSpotlight(model.getSignal(row));
            } else {
              chronoPanel.changeSpotlight(null);
            }
          }
        });
    // popup on right click
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (!SwingUtilities.isRightMouseButton(e)) return;
            if (!(e.getComponent() instanceof JTable)) return;
            Signal.List signals = getSelectedValuesList();
            if (signals.size() == 0) {
              int row = rowAtPoint(e.getPoint());
              if (row >= 0 && row < model.getSignalCount()) signals.add(model.getSignal(row));
            }
            PopupMenu m = new PopupMenu(chronoPanel, signals);
            m.doPop(e);
          }
        });

    getSelectionModel()
        .addListSelectionListener(
            e -> {
              int a = e.getFirstIndex();
              int b = e.getLastIndex();
              chronoPanel.getRightPanel().updateSelected(a, b);
            });
    setDragEnabled(true);
    setDropMode(DropMode.INSERT_ROWS);
    TransferHandler ccp = new SignalTransferHandler();
    setTransferHandler(ccp);

    InputMap inputMap = getInputMap();
    ActionMap actionMap = getActionMap();
    actionMap.put(
        "ClearSelection",
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            clearSelection();
          }
        });
    actionMap.put(
        LogisimMenuBar.DELETE,
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            removeSelected();
          }
        });
    actionMap.put(
        LogisimMenuBar.SELECT_ALL,
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            selectAll();
          }
        });
    actionMap.put(LogisimMenuBar.CUT, TransferHandler.getCutAction());
    actionMap.put(LogisimMenuBar.COPY, TransferHandler.getCopyAction());
    actionMap.put(LogisimMenuBar.PASTE, TransferHandler.getPasteAction());
    actionMap.put(
        LogisimMenuBar.RAISE,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            raiseOrLower(-1);
          }
        });
    actionMap.put(
        LogisimMenuBar.LOWER,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            raiseOrLower(+1);
          }
        });
    actionMap.put(
        LogisimMenuBar.RAISE_TOP,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            raiseOrLower(-2);
          }
        });
    actionMap.put(
        LogisimMenuBar.LOWER_BOTTOM,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            raiseOrLower(+2);
          }
        });

    // calculate default sizes
    int nameWidth = 0;
    int valueWidth = 0;
    TableCellRenderer render = getDefaultRenderer(String.class);
    int n = model.getSignalCount();
    for (int i = -1; i < n; i++) {
      String name;
      String val;
      if (i < 0) {
        name = tableModel.getColumnName(0);
        val = tableModel.getColumnName(1);
      } else {
        Signal s = model.getSignal(i);
        name = s.getName();
        val = s.getFormattedMaxValue();
      }
      Component c;
      c = render.getTableCellRendererComponent(this, name, false, false, i, 0);
      nameWidth = Math.max(nameWidth, c.getPreferredSize().width);
      c = render.getTableCellRendererComponent(this, val, false, false, i, 1);
      valueWidth = Math.max(valueWidth, c.getPreferredSize().width);
    }

    setRowHeight(ChronoPanel.SIGNAL_HEIGHT);
    // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    TableColumn col;

    col = getColumnModel().getColumn(0);
    col.setMinWidth(20);
    col.setPreferredWidth(nameWidth + 10);

    col = getColumnModel().getColumn(1);
    col.setMinWidth(20);
    col.setPreferredWidth(valueWidth + 10);

    setFillsViewportHeight(true);
    setPreferredScrollableViewportSize(getPreferredSize());

    JTableHeader header = getTableHeader();
    Dimension d = header.getPreferredSize();
    d.height = ChronoPanel.HEADER_HEIGHT;
    header.setPreferredSize(d);
    requestFocusInWindow();
  }

  public void setModel(Model m) {
    model = m;
    updateSignals();
  }

  public void changeSpotlight(Signal oldSignal, Signal newSignal) {
    if (oldSignal != null) tableModel.fireTableRowsUpdated(oldSignal.idx, oldSignal.idx);
    if (newSignal != null) tableModel.fireTableRowsUpdated(newSignal.idx, newSignal.idx);
  }

  public void updateSignals() {
    tableModel.fireTableDataChanged();
  }

  public void updateSignalValues() {
    for (int row = 0; row < model.getSignalCount(); row++) tableModel.fireTableCellUpdated(row, 1);
  }

  Signal.List getSelectedValuesList() {
    Signal.List signals = new Signal.List();
    int[] sel = getSelectedRows();
    for (int i : sel) signals.add(model.getSignal(i));
    return signals;
  }

  void setSelectedRows(Signal.List signals) {
    clearSelection();
    for (Signal s : signals) {
      int i = model.indexOf(s.info);
      if (i >= 0) addRowSelectionInterval(i, i);
    }
  }

  void raiseOrLower(int d) {
    Signal.List sel = getSelectedValuesList();
    int first = Integer.MAX_VALUE;
    int last = -1;
    for (Signal s : sel) {
      first = Math.min(first, s.idx);
      last = Math.max(last, s.idx);
    }
    if (last == -1) return;

    switch (d) {
      case -2:
        model.addOrMoveSignals(sel, 0);
        break;
      case -1:
        model.addOrMoveSignals(sel, Math.max(0, first - 1));
        break;
      case 1:
        model.addOrMoveSignals(sel, Math.min(model.getSignalCount(), last + 2));
        break;
      default:
        model.addOrMoveSignals(sel, model.getSignalCount());
        break;
    }

    setSelectedRows(sel);
  }

  void removeSelected() {
    int idx = 0;
    Signal.List signals = getSelectedValuesList();
    SignalInfo.List items = new SignalInfo.List();
    for (Signal s : signals) {
      items.add(s.info);
      idx = Math.max(idx, s.idx);
    }
    int count = model.remove(items);
    if (count > 0 && model.getSignalCount() > 0) {
      idx = Math.min(idx + 1 - count, model.getSignalCount() - 1);
      setRowSelectionInterval(idx, idx);
    }
    repaint();
  }

  private class SignalTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;

    Signal.List removing = null;

    @Override
    public int getSourceActions(JComponent comp) {
      return MOVE;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
      removing = getSelectedValuesList();
      if (removing.size() == 0) removing = null;
      return removing;
    }

    @Override
    public void exportDone(JComponent comp, Transferable trans, int action) {
      if (removing == null) return;
      ArrayList<SignalInfo> items = new ArrayList<>();
      for (Signal s : removing) items.add(s.info);
      removing = null;
      model.remove(items);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      return support.isDataFlavorSupported(Signal.List.dataFlavor);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
      removing = null;
      try {
        Signal.List incoming;
        incoming = (Signal.List) support.getTransferable().getTransferData(Signal.List.dataFlavor);
        int newIdx = model.getSignalCount();
        if (support.isDrop()) {
          try {
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            newIdx = Math.min(newIdx, dl.getRow());
          } catch (ClassCastException ignored) {
          }
        } else {
          int[] sel = getSelectedRows();
          if (sel != null && sel.length > 0) {
            newIdx = 0;
            for (int i : sel) newIdx = Math.max(newIdx, i + 1);
          }
        }
        boolean change = model.addOrMoveSignals(incoming, newIdx);
        if (change) setSelectedRows(incoming);
        return change;
      } catch (UnsupportedFlavorException | IOException e) {
        e.printStackTrace();
        return false;
      }
    }
  }
}
