/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.ParserException;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.analyze.model.VariableListEvent;
import com.cburch.logisim.analyze.model.VariableListListener;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.SyntaxChecker;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import org.jdesktop.swingx.prompt.BuddySupport;

public class VariableTab extends AnalyzerTab {
  private static final long serialVersionUID = 1L;
  private final JTable inputsTable;
  private final JTable outputsTable;
  private final JLabel error = new JLabel(" ");
  private final JLabel inputsLabel;
  private final JLabel outputsLabel;

  private JTable ioTable(VariableList data, LogisimMenuBar menubar) {
    final var ed1 = new SingleClickVarEditor(data);
    final var ed2 = new DoubleClickVarEditor(data);
    final var table = new JTable(1, 1) {
      private static final long serialVersionUID = 1L;

      @Override
      public TableCellEditor getCellEditor(int row, int column) {
        return (row == getRowCount() - 1 ? ed1 : ed2);
      }
    };
    table.getTableHeader().setUI(null);
    table.setModel(new VariableTableModel(data, table));
    table.setDefaultRenderer(Var.class, new VarRenderer());
    table.setRowHeight(30);
    table.setShowGrid(false);
    table.setDragEnabled(true);
    table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final var ccp = new VarTransferHandler(table, data);
    table.setTransferHandler(ccp);
    table.setDropMode(DropMode.INSERT_ROWS);

    final var inputMap = table.getInputMap();
    for (LogisimMenuItem item : LogisimMenuBar.EDIT_ITEMS) {
      final var accel = menubar.getAccelerator(item);
      inputMap.put(accel, item);
    }

    final var actionMap = table.getActionMap();

    actionMap.put(LogisimMenuBar.CUT, TransferHandler.getCutAction());
    actionMap.put(LogisimMenuBar.COPY, TransferHandler.getCopyAction());
    actionMap.put(LogisimMenuBar.PASTE, TransferHandler.getPasteAction());
    actionMap.put(
        LogisimMenuBar.DELETE,
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            var idx = table.getSelectedRow();
            if (idx < 0 || idx >= data.vars.size()) return;
            data.remove(data.vars.get(idx));
            if (idx >= data.vars.size()) idx = data.vars.size() - 1;
            if (idx >= 0) table.changeSelection(idx, 0, false, false);
          }
        });
    actionMap.put(
        LogisimMenuBar.RAISE,
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            final var idx = table.getSelectedRow();
            if (idx <= 0 || idx > data.vars.size() - 1) return;
            data.move(data.vars.get(idx), -1);
            table.changeSelection(idx - 1, 0, false, false);
          }
        });
    actionMap.put(
        LogisimMenuBar.LOWER,
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            final var idx = table.getSelectedRow();
            if (idx < 0 || idx >= data.vars.size() - 1) return;
            data.move(data.vars.get(idx), +1);
            table.changeSelection(idx + 1, 0, false, false);
          }
        });
    actionMap.put(
        LogisimMenuBar.RAISE_TOP,
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            final var idx = table.getSelectedRow();
            if (idx <= 0 || idx > data.vars.size() - 1) return;
            data.move(data.vars.get(idx), -idx);
            table.changeSelection(0, 0, false, false);
          }
        });
    actionMap.put(
        LogisimMenuBar.LOWER_BOTTOM,
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            final var idx = table.getSelectedRow();
            int end = data.vars.size() - 1;
            if (idx < 0 || idx >= data.vars.size() - 1) return;
            data.move(data.vars.get(idx), end - idx);
            table.changeSelection(end, 0, false, false);
          }
        });
    return table;
  }

  private JTable focus;

  private JScrollPane wrap(JTable table) {
    final var scroll = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setPreferredSize(new Dimension(AppPreferences.getScaled(60), AppPreferences.getScaled(100)));
    scroll.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent me) {
            table.changeSelection(table.getRowCount() - 1, 0, false, false);
            table.grabFocus();
          }
        });
    scroll.setTransferHandler(table.getTransferHandler());

    table.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        if (e.isTemporary()) return;
        focus = table;
        editHandler.computeEnabled();
      }

      @Override
      public void focusLost(FocusEvent e) {
        if (e.isTemporary()) return;
        table.clearSelection();
        if (focus == table) {
          focus = null;
        }
      }
    });
    return scroll;
  }

  VariableTab(VariableList inputs, VariableList outputs, LogisimMenuBar menubar) {
    inputs.addCompanion(outputs);
    outputs.addCompanion(inputs);

    inputsTable = ioTable(inputs, menubar);
    outputsTable = ioTable(outputs, menubar);
    final var inputsTablePane = wrap(inputsTable);
    final var outputsTablePane = wrap(outputsTable);

    final var gbl = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    setLayout(gbl);

    gbc.insets = new Insets(10, 10, 2, 10);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = gbc.weighty = 0.0;

    inputsLabel = new JLabel("Input Variables");

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbl.setConstraints(inputsLabel, gbc);
    add(inputsLabel);

    outputsLabel = new JLabel("Output Variables");

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbl.setConstraints(outputsLabel, gbc);
    add(outputsLabel);

    gbc.insets = new Insets(2, 10, 3, 10);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = gbc.weighty = 1.0;
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbl.setConstraints(inputsTablePane, gbc);
    add(inputsTablePane);

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbl.setConstraints(outputsTablePane, gbc);
    add(outputsTablePane);

    gbc.insets = new Insets(3, 10, 10, 10);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = gbc.weighty = 0.0;
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbl.setConstraints(error, gbc);
    add(error);
    error.setForeground(Color.RED);

    if (!outputs.vars.isEmpty()) {
      outputsTable.changeSelection(0, 0, false, false);
      focus = outputsTable;
    } else if (!inputs.vars.isEmpty()) {
      inputsTable.changeSelection(0, 0, false, false);
      focus = inputsTable;
    }

    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        if (focus != null) focus.requestFocusInWindow();
      }
    });
    editHandler.computeEnabled();
  }

  @Override
  void localeChanged() {
    inputsLabel.setText(S.get("inputVariables"));
    outputsLabel.setText(S.get("outputVariables"));
  }

  @Override
  void updateTab() {
    final var inputModel = (VariableTableModel) inputsTable.getModel();
    inputModel.update();
    final var outputModel = (VariableTableModel) outputsTable.getModel();
    outputModel.update();
  }

  public static final int OK = 0;
  public static final int EMPTY = 1;
  public static final int UNCHANGED = 2;
  public static final int RESIZED = 3;
  public static final int BAD_NAME = 4;
  public static final int DUP_NAME = 5;
  private static final int TOO_WIDE = 6;
  public static final int NO_START_PAR = -1;
  public static final int NO_VALID_MSB_INDEX = -2;
  public static final int NO_VALID_INDEX_SEP = -3;
  public static final int NO_VALID_LSB_INDEX = -4;
  public static final int LSB_BIGGER_MSB = -5;
  public static final int NO_FINAL_PAR = -6;
  public static final int INVALID_CHARS = -7;

  public static int checkindex(String index) {
    final var length = index.length();
    var pos = 0;
    if (length < 2) return 0;
    if (index.charAt(pos++) != '[') return NO_START_PAR;
    while ((pos < length) && ("0123456789".indexOf(index.charAt(pos)) >= 0)) pos++;
    if (pos == 1) return NO_VALID_MSB_INDEX;
    final var msbIndex = Integer.parseInt(index.substring(1, pos));
    if (pos >= length) return NO_FINAL_PAR;
    if (index.charAt(pos) == ']') {
      pos++;
      if (pos != length) return INVALID_CHARS;
      else return msbIndex;
    }
    if (pos >= length - 2) return NO_VALID_INDEX_SEP;
    if (!index.startsWith("..", pos)) return NO_VALID_INDEX_SEP;
    pos += 2;
    final var curpos = pos;
    while ((pos < length) && ("0123456789".indexOf(index.charAt(pos)) >= 0)) pos++;
    if (pos == curpos) return NO_VALID_LSB_INDEX;
    final var lsbIndex = Integer.parseInt(index.substring(curpos, pos));
    if (lsbIndex > msbIndex) return LSB_BIGGER_MSB;
    if (pos >= length) return NO_FINAL_PAR;
    if (index.charAt(pos++) != ']') return NO_FINAL_PAR;
    if (pos != length) return INVALID_CHARS;
    return msbIndex - lsbIndex + 1;
  }

  private int validateInput(VariableList data, Var oldVar, String text, int w) {
    var err = OK;
    if (text.length() == 0) {
      err = EMPTY;
    } else if (!Character.isJavaIdentifierStart(text.charAt(0))) {
      error.setText(S.get("variableStartError"));
      err = BAD_NAME;
    } else {
      for (int i = 1; i < text.length() && err == OK; i++) {
        final var c = text.charAt(i);
        if (!Character.isJavaIdentifierPart(c)) {
          error.setText(S.get("variablePartError", "" + c));
          err = BAD_NAME;
        }
      }
    }
    if (err == OK) {
      final var message = SyntaxChecker.getErrorMessage(text);
      if (message != null) {
        err = BAD_NAME;
        error.setText(message);
      }
    }

    if (err == OK && oldVar != null) {
      if (oldVar.name.equals(text) && oldVar.width == w) {
        err = UNCHANGED;
      } else if (oldVar.name.equals(text)) {
        err = RESIZED;
      }
    }
    if (err == OK) {
      if (data.containsDuplicate(data, oldVar, text)) {
        error.setText(S.get("variableDuplicateError"));
        err = DUP_NAME;
      }
    }
    if (err == OK || err == EMPTY) {
      if (data.bits.size() + w > data.getMaximumSize()) {
        error.setText(S.get("variableMaximumError", "" + data.getMaximumSize()));
        err = TOO_WIDE;
      } else {
        error.setText(" ");
      }
    }
    return err;
  }

  @Override
  EditHandler getEditHandler() {
    return editHandler;
  }

  final EditHandler editHandler =
      new EditHandler() {
        @Override
        public void computeEnabled() {
          final var n = (focus == null || focus.isEditing()) ? -1 : (focus.getRowCount() - 1);
          final var i = (focus == null || focus.isEditing()) ? -1 : focus.getSelectedRow();
          setEnabled(LogisimMenuBar.CUT, true);
          setEnabled(LogisimMenuBar.COPY, true);
          setEnabled(LogisimMenuBar.PASTE, true);
          setEnabled(LogisimMenuBar.DELETE, true);
          setEnabled(LogisimMenuBar.DUPLICATE, false);
          setEnabled(LogisimMenuBar.SELECT_ALL, focus != null && focus.isEditing());
          setEnabled(LogisimMenuBar.RAISE, 0 < i && i <= n - 1);
          setEnabled(LogisimMenuBar.LOWER, 0 <= i && i < n - 1);
          setEnabled(LogisimMenuBar.RAISE_TOP, 0 < i && i <= n - 1);
          setEnabled(LogisimMenuBar.LOWER_BOTTOM, 0 <= i && i < n - 1);
          setEnabled(LogisimMenuBar.ADD_CONTROL, false);
          setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
          final var action = e.getSource();
          if (focus != null) focus.getActionMap().get(action).actionPerformed(null);
        }
      };

  private static class VariableTableModel extends AbstractTableModel implements VariableListListener {
    private static final long serialVersionUID = 1L;
    private final JTable table;
    private final VariableList list;
    private Var[] listCopy;
    private final Var empty = new Var("", 1);

    public VariableTableModel(VariableList list, JTable table) {
      this.list = list;
      this.table = table;
      updateCopy();
      list.addVariableListListener(this);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return true;
    }

    @Override
    public void setValueAt(Object obj, int row, int column) {
      final var newVar = (Var) obj;
      final var oldVar = (Var) getValueAt(row, column);
      if (newVar == null || newVar.name.equals("") || newVar.equals(oldVar)) return;
      if (row == listCopy.length) {
        list.add(newVar);
        table.changeSelection(row + 1, column, false, false);
      } else {
        list.replace(oldVar, newVar);
        table.changeSelection(row, column, false, false);
      }
      table.grabFocus();
    }

    @Override
    public Object getValueAt(int row, int col) {
      if (row == listCopy.length)
        return empty;
      else if (row >= 0 && row < listCopy.length)
        return listCopy[row];
      return null;
    }

    @Override
    public int getColumnCount() {
      return 1;
    }

    @Override
    public String getColumnName(int column) {
      return "";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return Var.class;
    }

    @Override
    public int getRowCount() {
      return listCopy.length + 1;
    }

    @Override
    public void listChanged(VariableListEvent event) {
      final var oldSize = listCopy.length;
      updateCopy();
      Integer idx = event.getIndex();
      switch (event.getType()) {
        case VariableListEvent.ALL_REPLACED -> fireTableRowsUpdated(0, oldSize);
        case VariableListEvent.ADD -> fireTableRowsInserted(idx, idx);
        case VariableListEvent.REMOVE -> fireTableRowsDeleted(idx, idx);
        case VariableListEvent.MOVE -> fireTableRowsUpdated(0, listCopy.length - 1);
        case VariableListEvent.REPLACE -> fireTableRowsUpdated(idx, idx);
        default -> {
          // do nothing
        }
      }
    }

    private void update() {
      updateCopy();
      fireTableDataChanged();
    }

    private void updateCopy() {
      listCopy = list.vars.toArray(new Var[0]);
    }
  }

  public static class VarRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    final Border border = BorderFactory.createEmptyBorder(10, 10, 10, 10);
    final Font plain;
    final Font italic;

    public VarRenderer() {
      setBorder(border);
      plain = AppPreferences.getScaledFont(getFont());
      italic = AppPreferences.getScaledFont(plain.deriveFont(Font.ITALIC));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      final var empty = value.toString().equals("");
      if (empty) value = S.get("variableClickToAdd");
      final var c = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      c.setFont(empty ? italic : plain);
      return c;
    }
  }

  static class BitWidthRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 1L;

    public BitWidthRenderer() {
      // Do nothing.
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list,
        Object w, int index, boolean isSelected, boolean cellHasFocus) {
      final var s = ((Integer) w) == 1 ? ("1 bit") : (w + " bits");
      return super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
    }
  }

  public class SingleClickVarEditor extends AbstractCellEditor implements TableCellEditor {
    private static final long serialVersionUID = 1L;
    final JTextField field = new JTextField();
    final JComboBox<Integer> width;
    Var editing;
    final VariableList data;

    public SingleClickVarEditor(VariableList data) {
      field.setBorder(BorderFactory.createCompoundBorder(
            field.getBorder(),
            BorderFactory.createEmptyBorder(1, 3, 1, 3)));
      this.data = data;
      final var maxwidth = data.getMaximumSize();
      final var widths = new Integer[Math.min(maxwidth, 32)];
      for (int i = 0; i < widths.length; i++) widths[i] = i + 1;
      width = new JComboBox<>(widths);
      width.setFocusable(false);
      width.setRenderer(new BitWidthRenderer());
      width.setMaximumRowCount(widths.length);
      BuddySupport.addRight(width, field);
    }

    @Override
    public Object getCellEditorValue() {
      final var name = field.getText().trim();
      final var w = (Integer) width.getSelectedItem();
      return new Var(name, w);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      editing = (Var) value;
      field.setText(editing.name);
      width.setSelectedItem(editing.width);
      return field;
    }

    @Override
    public boolean stopCellEditing() {
      if (ok()) {
        super.stopCellEditing();
        return true;
      } else {
        return false;
      }
    }

    boolean ok() {
      final var oldVar = editing;
      editing = null;
      final var text = field.getText().trim();
      var name = "";
      var index = "";
      int w;
      if (text.contains("[")) {
        int idx = text.indexOf('[');
        name = text.substring(0, idx);
        index = text.substring(idx);
        w = checkindex(index);
        if (w <= 0) {
          String errorText = null;
          if (w == NO_START_PAR) errorText = S.get("variableRangeStartPar");
          else if (w == NO_VALID_MSB_INDEX) errorText = S.get("variableRangeMSBWrong");
          else if (w == NO_VALID_INDEX_SEP) errorText = S.get("variableRangeWrongSep");
          else if (w == NO_VALID_LSB_INDEX) errorText = S.get("variableRangeWrongSep");
          else if (w == LSB_BIGGER_MSB) errorText = S.get("variableRangeWrongLB");
          else if (w == NO_FINAL_PAR) errorText = S.get("variableRangeFinalPar");
          else if (w == INVALID_CHARS) errorText = S.get("variableRangeInvalChar");
          if (errorText != null) {
            error.setText(errorText);
          }
          w = (Integer) width.getSelectedItem();
          return false;
        } else {
          width.setSelectedIndex(w - 1);
          w -= 1;
          field.setText(name);
        }
      } else {
        name = text;
        w = (Integer) width.getSelectedItem();
      }
      if (oldVar == null || oldVar.name.equals("")) {
        // validate new name and width
        final var err = validateInput(data, null, name, w);
        if (err == EMPTY) return true; // do nothing, empty Var will be ignored in setValueAt()
        if (err == BAD_NAME || err == DUP_NAME || err == TOO_WIDE) return false; // prevent loss of focus
        return err == OK; // new Var will be added in setValueAt()
      } else {
        // validate replacement name and width
        final var err = validateInput(data, oldVar, name, w);
        if (err == EMPTY || err == BAD_NAME || err == DUP_NAME || err == TOO_WIDE) return false; // prevent loss of focus
        if (err == UNCHANGED) return true; // do nothing, unchanged Var will be ignored in setValueAt()
        return err == OK || err == RESIZED; // modified Var will be created in setValueAt()
      } // should never happen
    }
  }

  public class DoubleClickVarEditor extends SingleClickVarEditor {
    private static final long serialVersionUID = 1L;

    public DoubleClickVarEditor(VariableList data) {
      super(data);
    }

    @Override
    public boolean isCellEditable(EventObject e) {
      if (super.isCellEditable(e)) {
        if (e instanceof MouseEvent me) {
          return me.getClickCount() >= 2;
        }
        if (e instanceof KeyEvent ke) {
          return (ke.getKeyCode() == KeyEvent.VK_F2 || ke.getKeyCode() == KeyEvent.VK_ENTER);
        }
      }
      return false;
    }
  }

  Var parse(String s) {
    try {
      return Var.parse(s);
    } catch (ParserException e) {
      error.setText(e.getMessage());
      return null;
    }
  }

  private class VarTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;
    final JTable table;
    final VariableList data;
    Var pendingDelete;

    VarTransferHandler(JTable table, VariableList data) {
      this.table = table;
      this.data = data;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
      String s;
      try {
        s = (String) info.getTransferable().getTransferData(DataFlavor.stringFlavor);
      } catch (Exception e) {
        return false;
      }

      final var newVar = parse(s);
      if (newVar == null) return false;
      var newIdx = data.vars.size();
      if (info.isDrop()) {
        try {
          JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
          newIdx = Math.min(dl.getRow(), data.vars.size());
        } catch (ClassCastException ignored) {
        }
      }

      Var oldVar = null;
      int oldIdx;
      for (oldIdx = 0; oldIdx < data.vars.size(); oldIdx++) {
        final var v = data.vars.get(oldIdx);
        if (v.name.equals(newVar.name)) {
          oldVar = v;
          break;
        }
      }

      final var err = validateInput(data, oldVar, newVar.name, newVar.width);
      if (err == UNCHANGED) {
        pendingDelete = null;
        if (newIdx > oldIdx)
          newIdx--; // old item will no longer be there
        if (oldIdx != newIdx) {
          data.move(oldVar, newIdx - oldIdx);
          table.changeSelection(newIdx, 0, false, false);
          table.grabFocus();
        }
        return true;
      } else if (err == OK) {
        pendingDelete = null;
        data.add(newVar);
        if (newIdx < data.vars.size() - 1)
          data.move(newVar, newIdx - data.vars.size() + 2);
        table.changeSelection(newIdx, 0, false, false);
        table.grabFocus();
        return true;
      } else if (err == RESIZED) {
        pendingDelete = null;
        data.replace(oldVar, newVar);
        table.changeSelection(oldIdx, 0, false, false);
        table.grabFocus();
        return true;
      } else {
        return false;
      }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
      final var row = table.getSelectedRow();
      if (row < 0 || row >= data.vars.size())
        return null;
      pendingDelete = data.vars.get(row);
      return new StringSelection(pendingDelete.toString());
    }

    @Override
    public int getSourceActions(JComponent c) {
      return COPY_OR_MOVE;
    }

    @Override
    protected void exportDone(JComponent c, Transferable tdata, int action) {
      if (action == MOVE && pendingDelete != null) {
        data.remove(pendingDelete);
      }
      pendingDelete = null;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }
  }


}
