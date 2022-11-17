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

import com.cburch.logisim.fpga.gui.HdlColorRenderer;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.JInputComponent;
import com.cburch.logisim.util.JInputDialog;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedList;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

@SuppressWarnings("serial")

/*
 * Attribute table panel
 * Shows detailed attributes of the selected element.
 */
public class AttrTable extends JPanel implements LocaleListener {

  private static final AttrTableModel NULL_ATTR_MODEL = new NullAttrModel();
  private final Window parent;
  private final JLabel title;
  private final JTable table;
  private final TableModelAdapter tableModel;
  private final CellEditor editor = new CellEditor();
  private boolean titleEnabled;

  public AttrTable(Window parent) {
    super(new BorderLayout());
    this.parent = parent;

    titleEnabled = true;
    title = new TitleLabel();
    title.setHorizontalAlignment(SwingConstants.CENTER);
    title.setVerticalAlignment(SwingConstants.CENTER);
    tableModel = new TableModelAdapter(parent, NULL_ATTR_MODEL);
    table = new JTable(tableModel);
    table.setDefaultEditor(Object.class, editor);
    table.setTableHeader(null);
    table.setRowHeight(AppPreferences.getScaled(AppPreferences.BOX_SIZE));

    final var baseFont = title.getFont();
    final var titleSize = Math.round(baseFont.getSize() * 1.2F);
    final var titleFont = baseFont.deriveFont(AppPreferences.getScaled((float) titleSize)).deriveFont(Font.BOLD);
    title.setFont(titleFont);
    table.setDefaultRenderer(String.class, new HdlColorRenderer());

    final var propPanel = new JPanel(new BorderLayout(0, 0));
    final var tableScroll = new JScrollPane(table);

    propPanel.add(title, BorderLayout.PAGE_START);
    propPanel.add(tableScroll, BorderLayout.CENTER);

    this.add(propPanel, BorderLayout.CENTER);

    LocaleManager.addLocaleListener(this);
    localeChanged();
  }

  public AttrTableModel getAttrTableModel() {
    return tableModel.attrModel;
  }

  public void setAttrTableModel(AttrTableModel value) {
    final var editor = table.getCellEditor();
    if (editor != null) table.getCellEditor().cancelCellEditing();
    tableModel.setAttrTableModel(value == null ? NULL_ATTR_MODEL : value);
    updateTitle();
  }

  public boolean isTitleEnabled() {
    return titleEnabled;
  }

  public void setTitleEnabled(boolean value) {
    titleEnabled = value;
    updateTitle();
  }

  @Override
  public void localeChanged() {
    updateTitle();
    tableModel.fireTableChanged();
  }

  private void updateTitle() {
    if (titleEnabled) {
      final var text = tableModel.attrModel.getTitle();
      if (text == null) {
        title.setVisible(false);
      } else {
        title.setText(text);
        title.setVisible(true);
      }
    } else {
      title.setVisible(false);
    }
  }

  /* ******************************************************************************************** */

  private static class MyDialog extends JDialogOk {
    JInputComponent input;
    Object value;

    public MyDialog(JInputComponent input) {
      super(S.get("attributeDialogTitle"));
      configure(input);
    }

    private void configure(JInputComponent input) {
      this.input = input;
      this.value = input.getValue();

      // Thanks to Christophe Jacquet, who contributed a fix to this
      // so that when the dialog is resized, the component within it
      // is resized as well. (Tracker #2024479)
      final var p = new JPanel(new BorderLayout());
      p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      // Hide the JFileChooser buttons, since we already have the
      // MyDialog ones
      if (input instanceof JFileChooser chooser) chooser.setControlButtonsAreShown(false);
      p.add((JComponent) input, BorderLayout.CENTER);
      getContentPane().add(p, BorderLayout.CENTER);

      pack();
    }

    public Object getValue() {
      return value;
    }

    @Override
    public void okClicked() {
      value = input.getValue();
    }
  }

  /* ******************************************************************************************** */

  private static class NullAttrModel implements AttrTableModel {

    @Override
    public void addAttrTableModelListener(AttrTableModelListener listener) {
      // Do nothing.
    }

    @Override
    public AttrTableModelRow getRow(int rowIndex) {
      return null;
    }

    @Override
    public int getRowCount() {
      return 0;
    }

    @Override
    public String getTitle() {
      return null;
    }

    @Override
    public void removeAttrTableModelListener(AttrTableModelListener listener) {
      // Do nothing.
    }
  }

  private static class TitleLabel extends JLabel {

    @Override
    public Dimension getMinimumSize() {
      Dimension ret = super.getMinimumSize();
      return new Dimension(1, ret.height);
    }
  }

  /* ******************************************************************************************** */

  private class CellEditor implements TableCellEditor, FocusListener, ActionListener {

    final LinkedList<CellEditorListener> listeners = new LinkedList<>();
    AttrTableModelRow currentRow;
    AttrTableModelRow[] currentRows;
    int[] currentRowIndexes;
    Component currentEditor;
    boolean multiEditActive = false;

    //
    // ActionListener methods
    //
    @Override
    public void actionPerformed(ActionEvent e) {
      stopCellEditing();
    }

    //
    // TableCellListener management
    //
    @Override
    public void addCellEditorListener(CellEditorListener l) {
      // Adds a listener to the list that's notified when the
      // editor stops, or cancels editing.
      listeners.add(l);
    }

    //
    // other TableCellEditor methods
    //
    @Override
    public void cancelCellEditing() {
      // Tells the editor to cancel editing and not accept any
      // partially edited value.
      fireEditingCanceled();
    }

    public void fireEditingCanceled() {
      final var col = table.getEditingColumn();
      final var e = new ChangeEvent(AttrTable.this);
      for (final var l : new ArrayList<>(listeners)) {
        l.editingCanceled(e);
      }
      if (multiEditActive) {
        final var value = getCellEditorValue();
        for (final var r : currentRowIndexes) {
          if (r == table.getEditingRow()) continue;
          table.setValueAt(value, r, col);
        }
      }
    }

    public void fireEditingStopped() {
      final var e = new ChangeEvent(AttrTable.this);
      for (final var l : new ArrayList<>(listeners)) {
        l.editingStopped(e);
      }
    }

    @Override
    public void focusGained(FocusEvent e) {
      // Do nothing
    }

    //
    // FocusListener methods
    //
    @Override
    public void focusLost(FocusEvent e) {
      final var dst = e.getOppositeComponent();
      if (dst != null) {
        var p = dst;
        while (p != null && !(p instanceof Window)) {
          if (p == AttrTable.this) {
            // switch to another place in this table,
            // no problem
            return;
          }
          p = p.getParent();
        }
        // focus transferred outside table; stop editing
        editor.stopCellEditing();
      }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getCellEditorValue() {
      // Returns the value contained in the editor.
      final var comp = currentEditor;
      if (comp instanceof JTextField field) {
        return field.getText();
      } else if (comp instanceof JComboBox box) {
        return box.getSelectedItem();
      } else {
        return null;
      }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex, int columnIndex) {
      final var attrModel = tableModel.attrModel;
      final var row = attrModel.getRow(rowIndex);
      AttrTableModelRow[] rows = null;
      int[] rowIndexes = null;
      multiEditActive = false;

      if ((columnIndex == 0) || (rowIndex == 0)) {
        return new JLabel(row.getLabel());
      }

      if (currentEditor != null) {
        currentEditor.transferFocus();
      }

      var editor = row.getEditor(parent);
      if (editor instanceof JComboBox box) {
        box.addActionListener(this);
        editor.addFocusListener(this);
        rowIndexes = table.getSelectedRows();
        if (isSelected && rowIndexes.length > 1) {
          multiEditActive = true;
          rows = new AttrTableModelRow[rowIndexes.length];
          for (var i = 0; i < rowIndexes.length; i++) {
            rows[i] = attrModel.getRow(rowIndexes[i]);
            if (!row.multiEditCompatible(rows[i])) {
              multiEditActive = false;
              rowIndexes = null;
              rows = null;
              break;
            }
          }
        } else {
          rowIndexes = null;
        }
      } else if (editor instanceof JInputDialog dlog) {
        dlog.setVisible(true);
        final var retVal = dlog.getValue();
        try {
          row.setValue(parent, retVal);
        } catch (AttrTableSetException e) {
          OptionPane.showMessageDialog(
              parent,
              e.getMessage(),
              S.get("attributeChangeInvalidTitle"),
              OptionPane.WARNING_MESSAGE);
        }
        editor = null;
      } else if (editor instanceof JInputComponent input) {
        final var dialog = new MyDialog(input);
        dialog.setVisible(true);
        final var retVal = dialog.getValue();
        try {
          row.setValue(parent, retVal);
        } catch (AttrTableSetException e) {
          OptionPane.showMessageDialog(
              parent,
              e.getMessage(),
              S.get("attributeChangeInvalidTitle"),
              OptionPane.WARNING_MESSAGE);
        }
        editor = null;
      } else {
        editor.addFocusListener(this);
      }

      currentRow = row;
      currentRows = rows;
      currentRowIndexes = rowIndexes;
      currentEditor = editor;
      return editor;
    }

    public boolean isEditing(AttrTableModelRow row) {
      if (currentRow == row) return true;
      if (currentRows == null) return false;
      for (AttrTableModelRow r : currentRows) if (r == row) return true;
      return false;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
      // Asks the editor if it can start editing using anEvent.
      return true;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
      // Removes a listener from the list that's notified
      listeners.remove(l);
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
      // Returns true if the editing cell should be selected,
      // false otherwise.
      return !multiEditActive;
    }

    @Override
    public boolean stopCellEditing() {
      // Tells the editor to stop editing and accept any partially
      // edited value as the value of the editor.
      fireEditingStopped();
      return true;
    }
  }

  private class TableModelAdapter implements TableModel, AttrTableModelListener {

    final Window parent;
    final LinkedList<TableModelListener> listeners;
    AttrTableModel attrModel;

    TableModelAdapter(Window parent, AttrTableModel attrModel) {
      this.parent = parent;
      this.listeners = new LinkedList<>();
      this.attrModel = attrModel;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
      listeners.add(l);
    }

    @Override
    public void attrStructureChanged(AttrTableModelEvent e) {
      if (e.getSource() != attrModel) {
        attrModel.removeAttrTableModelListener(this);
        return;
      }
      final var ed = table.getCellEditor();
      if (ed != null) {
        ed.cancelCellEditing();
      }
      fireTableChanged();
    }

    //
    // AttrTableModelListener methods
    //
    @Override
    public void attrTitleChanged(AttrTableModelEvent e) {
      if (e.getSource() != attrModel) {
        attrModel.removeAttrTableModelListener(this);
        return;
      }
      updateTitle();
    }

    @Override
    public void attrValueChanged(AttrTableModelEvent e) {
      if (e.getSource() != attrModel) {
        attrModel.removeAttrTableModelListener(this);
        return;
      }
      int row = e.getRowIndex();

      final var ed = table.getCellEditor();
      if (row >= 0
          && ed instanceof AttrTable.CellEditor cellEd
          && cellEd.isEditing(attrModel.getRow(row))) {
        ed.cancelCellEditing();
      }

      fireTableChanged();
    }

    void fireTableChanged() {
      final var e = new TableModelEvent(this);
      for (final var l : new ArrayList<>(listeners)) {
        l.tableChanged(e);
      }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return String.class;
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
      return (columnIndex == 0)
             ? "Attribute"
             : "Value";
    }

    @Override
    public int getRowCount() {
      return attrModel.getRowCount();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return (columnIndex == 0)
          ? attrModel.getRow(rowIndex).getLabel()
          : attrModel.getRow(rowIndex).getValue();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex > 0 && attrModel.getRow(rowIndex).isValueEditable() && rowIndex > 0;
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
      listeners.remove(l);
    }

    void setAttrTableModel(AttrTableModel value) {
      if (attrModel != value) {
        attrModel.removeAttrTableModelListener(this);
        attrModel = value;
        attrModel.addAttrTableModelListener(this);
        fireTableChanged();
      }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
      if (columnIndex > 0) {
        try {
          attrModel.getRow(rowIndex).setValue(parent, value);
        } catch (AttrTableSetException e) {
          OptionPane.showMessageDialog(
              parent,
              e.getMessage(),
              S.get("attributeChangeInvalidTitle"),
              OptionPane.WARNING_MESSAGE);
        }
      }
    }
  }
}
