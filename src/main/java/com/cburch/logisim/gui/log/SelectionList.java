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

package com.cburch.logisim.gui.log;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.Icons;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.EventObject;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

public class SelectionList extends JTable {
  private static final long serialVersionUID = 1L;
  private Selection selection;

  private class SelectionListModel extends AbstractTableModel implements Model.Listener {
  
    private static final long serialVersionUID = 1L;

    @Override
    public void resetEntries(Model.Event event, Value[] values) {}

    @Override
    public void entryAdded(Model.Event event, Value[] values) {}

    @Override
    public void filePropertyChanged(Model.Event event) {}

    @Override
    public void selectionChanged(Model.Event event) {
      fireTableDataChanged();
    }

    @Override
    public int getColumnCount() { return 1; };
    @Override
    public String getColumnName(int column) { return ""; }
    @Override
    public Class<?> getColumnClass(int columnIndex) { return SelectionItem.class; }
    @Override
    public int getRowCount() { return selection == null ? 0 : selection.size(); }

    @Override
    public Object getValueAt(int row, int col) {
      return selection.get(row);
    }

    @Override
    public void setValueAt(Object o, int row, int column) {
      // nothing to do
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return true;
    }
  }

  private class SelectionItemRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    public java.awt.Component getTableCellRendererComponent(
        JTable table,Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      java.awt.Component ret =
          super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (ret instanceof JLabel && value instanceof SelectionItem) {
        JLabel label = (JLabel) ret;
        SelectionItem item = (SelectionItem) value;
        label.setIcon(new ComponentIcon(item.getComponent()));
        label.setText(item.toString()+" ["+item.getRadix().toDisplayString()+"]");
      }
      return ret;
    }
  }
  
  class SelectionItemEditor extends AbstractCellEditor implements TableCellEditor {
    private static final long serialVersionUID = 1L;
    JPanel panel = new JPanel();
    JLabel label = new JLabel();
    JButton button = new JButton(Icons.getIcon("dropdown.png"));
    JPopupMenu popup = new JPopupMenu("Options");
    SelectionItem item;
    SelectionItems items;
    HashMap<RadixOption, JRadioButtonMenuItem> radixMenuItems = new HashMap<>();
    
    public SelectionItemEditor() {
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

      label.setFont(label.getFont().deriveFont(Font.PLAIN));
      button.setFont(button.getFont().deriveFont(9.0f));

      ButtonGroup g = new ButtonGroup();
      for (RadixOption r : RadixOption.OPTIONS) {
        JRadioButtonMenuItem m = new JRadioButtonMenuItem(r.toDisplayString()); 
        radixMenuItems.put(r, m);
        popup.add(m);
        g.add(m);
        m.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {              
            for (SelectionItem s : items)
              s.setRadix(r);
            if (item != null)
              label.setText(item.toString() + " [" + item.getRadix().toDisplayString() + "]");
            SelectionList.this.repaint();
          }
        });
      }

      popup.addSeparator();
      JMenuItem m = new JMenuItem("Delete");
      popup.add(m);
      m.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {              
          cancelCellEditing();
          removeSelected();
        }
      });

      button.setMargin(new Insets(0, 0, 0, 0));
      button.setHorizontalTextPosition(SwingConstants.LEFT);
      button.setText("Options");
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          popup.show(panel, button.getX(), button.getY() + button.getHeight());
        }
      });
      button.setMinimumSize(button.getPreferredSize());

      label.setHorizontalAlignment(SwingConstants.LEFT);
      label.setAlignmentX(0.0f);
      label.setAlignmentY(0.5f);
      button.setAlignmentX(1.0f);
      button.setAlignmentY(0.5f);
      panel.add(label);
      panel.add(button);
    }

    @Override
    public Object getCellEditorValue() {
      return item;
    }

    @Override
    public java.awt.Component getTableCellEditorComponent(JTable table,
        Object value, boolean isSelected, int row, int column) {
      int margin = getColumnModel().getColumnMargin();
      label.setBorder(BorderFactory.createEmptyBorder(0, margin, 0, margin));

      Dimension d = new Dimension(getColumnModel().getTotalColumnWidth(), getRowHeight());
      label.setMinimumSize(new Dimension(10, d.height));
      label.setPreferredSize(new Dimension(d.width - button.getWidth(), d.height));
      label.setMaximumSize(new Dimension(d.width - button.getWidth(), d.height));

      panel.setBackground(isSelected ? getSelectionBackground() : getBackground());
      item = (SelectionItem)value;
      items = getSelectedValuesList();
      if (!items.contains(item)) {
        items.clear();
        items.add(item);
      }
      radixMenuItems.get(item.getRadix()).setSelected(true);
      label.setIcon(new ComponentIcon(item.getComponent()));
      label.setText(item.toString() + " [" + item.getRadix().toDisplayString() + "]");
      return panel;
    }

     @Override
     public boolean stopCellEditing() {
       super.stopCellEditing();
       return true;
     }

     @Override
    public boolean isCellEditable(EventObject e) {
      return true;
    }
  } 

  public SelectionList() {
    selection = null;
    setModel(new SelectionListModel());
    setDefaultRenderer(SelectionItem.class, new SelectionItemRenderer());
    setDefaultEditor(SelectionItem.class, new SelectionItemEditor());
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    getTableHeader().setUI(null);
    setRowHeight(24);
    // setAutoResizeMode(AUTO_RESIZE_OFF);
    setShowGrid(false);
    setFillsViewportHeight(true);
    setDragEnabled(true);
    setDropMode(DropMode.INSERT_ROWS);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    setTransferHandler(new SelectionTransferHandler());

    InputMap inputMap = getInputMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
    ActionMap actionMap = getActionMap();
    actionMap.put("Delete", new AbstractAction() {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(ActionEvent e) {
        removeSelected();
      }
    });
 }

  void removeSelected() {
    int idx = 0;
    SelectionItems items = getSelectedValuesList();
    for (SelectionItem item : items) {
      idx = Math.max(idx, selection.indexOf(item));
    }
    int count = selection.remove(items);
    if (count > 0 && selection.size() > 0) {
      idx = Math.min(idx+1-count, selection.size()-1);
      setRowSelectionInterval(idx, idx);
    }
    repaint();
  }
  
  public void localeChanged() {
    repaint();
  }

  public void setSelection(Selection value) {
    if (selection != value) {
      SelectionListModel model = (SelectionListModel) getModel();
      if (selection != null) selection.removeModelListener(model);
      selection = value;
      if (selection != null) selection.addModelListener(model);
      model.selectionChanged(null);
    }
  }

  SelectionItems getSelectedValuesList() {
    SelectionItems items = new SelectionItems();
    int[] sel = getSelectedRows();
    for (int i : sel)
      items.add(selection.get(i));
    return items;
  }

  private class SelectionTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;
    boolean removing;

    @Override
    public int getSourceActions(JComponent comp) {
      return MOVE;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
      removing = true;
      SelectionItems items = new SelectionItems();
      items.addAll(getSelectedValuesList());
      return items.isEmpty() ? null : items;
    }

    @Override
    public void exportDone(JComponent comp, Transferable trans, int action) {
      if (removing)
        selection.remove(getSelectedValuesList());
      removing = false;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      return support.isDataFlavorSupported(SelectionItems.dataFlavor);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
      removing = false;
      try {
        SelectionItems items =
            (SelectionItems)support.getTransferable().getTransferData(
                SelectionItems.dataFlavor);
        int newIdx = selection.size();
        if (support.isDrop()) {
          try {
            JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
            newIdx = Math.min(dl.getRow(), selection.size());
          } catch (ClassCastException e) {
          }
        }
        addOrMove(items, newIdx);
        return true;
      } catch (UnsupportedFlavorException | IOException e) {
        e.printStackTrace();
        return false;
      }
    }
  }

  private void addOrMove(SelectionItems items, int idx) {
    selection.addOrMove(items, idx);
    clearSelection();
    for (SelectionItem item : items) {
      int i = selection.indexOf(item);
      addRowSelectionInterval(i, i);
    }
  }

  private static final Font MSG_FONT = AppPreferences.getScaledFont(new Font("Sans Serif", Font.ITALIC, 12));

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    /* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

    Font f = g.getFont();
    Color c = g.getColor();
    g.setColor(Color.GRAY);
    g.setFont(MSG_FONT);
    g.drawString("drag here to add", 10, getRowHeight() * getRowCount() + 20);
    g.setFont(f);
    g.setColor(c);
  }}
