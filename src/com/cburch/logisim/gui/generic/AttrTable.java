/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/
package com.cburch.logisim.gui.generic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import com.bfh.logisim.hdlgenerator.HDLColorRenderer;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.JInputComponent;
import com.cburch.logisim.util.JInputDialog;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

@SuppressWarnings("serial")
public class AttrTable extends JPanel implements LocaleListener {

	private class CellEditor implements TableCellEditor, FocusListener,
			ActionListener {

		LinkedList<CellEditorListener> listeners = new LinkedList<CellEditorListener>();
		AttrTableModelRow currentRow;
		Component currentEditor;

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
			ChangeEvent e = new ChangeEvent(AttrTable.this);
			for (CellEditorListener l : new ArrayList<CellEditorListener>(
					listeners)) {
				l.editingCanceled(e);
			}
		}

		public void fireEditingStopped() {
			ChangeEvent e = new ChangeEvent(AttrTable.this);
			for (CellEditorListener l : new ArrayList<CellEditorListener>(
					listeners)) {
				l.editingStopped(e);
			}
		}

		@Override
		public void focusGained(FocusEvent e) {
		}

		//
		// FocusListener methods
		//
		@Override
		public void focusLost(FocusEvent e) {
			Object dst = e.getOppositeComponent();
			if (dst instanceof Component) {
				Component p = (Component) dst;
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
			Component comp = currentEditor;
			if (comp instanceof JTextField) {
				return ((JTextField) comp).getText();
			} else if (comp instanceof JComboBox) {
				return ((JComboBox) comp).getSelectedItem();
			} else {
				return null;
			}
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int rowIndex, int columnIndex) {
			AttrTableModel attrModel = tableModel.attrModel;
			AttrTableModelRow row = attrModel.getRow(rowIndex);

			if ((columnIndex == 0)||(rowIndex==0)) {
				return new JLabel(row.getLabel());
			} else {
				if (currentEditor != null) {
					currentEditor.transferFocus();
				}

				Component editor = row.getEditor(parent);
				if (editor instanceof JComboBox) {
					((JComboBox) editor).addActionListener(this);
					editor.addFocusListener(this);
				} else if (editor instanceof JInputDialog) {
					JInputDialog dlog = (JInputDialog) editor;
					dlog.setVisible(true);
					Object retval = dlog.getValue();
					try {
						row.setValue(retval);
					} catch (AttrTableSetException e) {
						JOptionPane.showMessageDialog(parent, e.getMessage(),
								Strings.get("attributeChangeInvalidTitle"),
								JOptionPane.WARNING_MESSAGE);
					}
					editor = null; 
				} else if (editor instanceof JInputComponent) {
					JInputComponent input = (JInputComponent) editor;
					MyDialog dlog;
					Window parent = AttrTable.this.parent;
					if (parent instanceof Frame) {
						dlog = new MyDialog((Frame) parent, input);
					} else {
						dlog = new MyDialog((Dialog) parent, input);
					}
					dlog.setVisible(true);
					Object retval = dlog.getValue();
					try {
						row.setValue(retval);
					} catch (AttrTableSetException e) {
						JOptionPane.showMessageDialog(parent, e.getMessage(),
								Strings.get("attributeChangeInvalidTitle"),
								JOptionPane.WARNING_MESSAGE);
					}
					editor = null;
				} else {
					editor.addFocusListener(this);
				}

				currentRow = row;
				currentEditor = editor;
				return editor;
			}
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
			return true;
		}

		@Override
		public boolean stopCellEditing() {
			// Tells the editor to stop editing and accept any partially
			// edited value as the value of the editor.
			fireEditingStopped();
			return true;
		}
	}

	private static class MyDialog extends JDialogOk {

		JInputComponent input;
		Object value;

		public MyDialog(Dialog parent, JInputComponent input) {
			super(parent, Strings.get("attributeDialogTitle"), true);
			configure(input);
		}

		public MyDialog(Frame parent, JInputComponent input) {
			super(parent, Strings.get("attributeDialogTitle"), true);
			configure(input);
		}

		private void configure(JInputComponent input) {
			this.input = input;
			this.value = input.getValue();

			// Thanks to Christophe Jacquet, who contributed a fix to this
			// so that when the dialog is resized, the component within it
			// is resized as well. (Tracker #2024479)
			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			// Hide the JFileChooser buttons, since we already have the
			// MyDialog ones
			if (input instanceof JFileChooser)
				((JFileChooser) input).setControlButtonsAreShown(false);
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

	private static class NullAttrModel implements AttrTableModel {

		@Override
		public void addAttrTableModelListener(AttrTableModelListener listener) {
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
		}
	}

	private class TableModelAdapter implements TableModel,
			AttrTableModelListener {

		Window parent;
		LinkedList<TableModelListener> listeners;
		AttrTableModel attrModel;

		TableModelAdapter(Window parent, AttrTableModel attrModel) {
			this.parent = parent;
			this.listeners = new LinkedList<TableModelListener>();
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
			TableCellEditor ed = table.getCellEditor();
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

			TableCellEditor ed = table.getCellEditor();
			if (row >= 0 && ed instanceof CellEditor
					&& attrModel.getRow(row) == ((CellEditor) ed).currentRow) {
				ed.cancelCellEditing();
			}

			fireTableChanged();
		}

		void fireTableChanged() {
			TableModelEvent e = new TableModelEvent(this);
			for (TableModelListener l : new ArrayList<TableModelListener>(
					listeners)) {
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
			if (columnIndex == 0) {
				return "Attribute";
			} else {
				return "Value";
			}
		}

		@Override
		public int getRowCount() {
			return attrModel.getRowCount();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return attrModel.getRow(rowIndex).getLabel();
			} else {
				return attrModel.getRow(rowIndex).getValue();
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex > 0
					&& attrModel.getRow(rowIndex).isValueEditable()
					&& rowIndex > 0;
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
					attrModel.getRow(rowIndex).setValue(value);
				} catch (AttrTableSetException e) {
					JOptionPane.showMessageDialog(parent, e.getMessage(),
							Strings.get("attributeChangeInvalidTitle"),
							JOptionPane.WARNING_MESSAGE);
				}
			}
		}
	}

	private static class TitleLabel extends JLabel {

		@Override
		public Dimension getMinimumSize() {
			Dimension ret = super.getMinimumSize();
			return new Dimension(1, ret.height);
		}
	}
	
	private static final AttrTableModel NULL_ATTR_MODEL = new NullAttrModel();
	private Window parent;
	private boolean titleEnabled;
	private JLabel title;
	private JTable table;
	private TableModelAdapter tableModel;
	private CellEditor editor = new CellEditor();
	private JTabbedPane tabPane;

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
		table.setRowHeight(AppPreferences.getScaled(AppPreferences.BoxSize));


		Font baseFont = title.getFont();
		int titleSize = Math.round(baseFont.getSize() * 1.2f);
		Font titleFont = baseFont.deriveFont(AppPreferences.getScaled((float)titleSize)).deriveFont(
				Font.BOLD);
		title.setFont(titleFont);
		Color bgColor = new Color(240, 240, 240);
		setBackground(bgColor);
		table.setBackground(bgColor);
		table.setDefaultRenderer(String.class, new HDLColorRenderer());

		tabPane = new JTabbedPane();
		JPanel propPanel = new JPanel(new BorderLayout(0, 0));
		JScrollPane tableScroll = new JScrollPane(table);

		propPanel.add(title, BorderLayout.PAGE_START);
		propPanel.add(tableScroll, BorderLayout.CENTER);
		tabPane.addTab("Properties", propPanel);

		this.add(tabPane, BorderLayout.CENTER);

		LocaleManager.addLocaleListener(this);
		localeChanged();
	}

	public AttrTableModel getAttrTableModel() {
		return tableModel.attrModel;
	}

	public JTabbedPane getTabPane() {
		return tabPane;
	}

	public boolean getTitleEnabled() {
		return titleEnabled;
	}

	@Override
	public void localeChanged() {
		updateTitle();
		tableModel.fireTableChanged();
	}

	public void setAttrTableModel(AttrTableModel value) {

		TableCellEditor editor = table.getCellEditor();

		if (editor != null)
			table.getCellEditor().cancelCellEditing();

		tableModel.setAttrTableModel(value == null ? NULL_ATTR_MODEL : value);
		updateTitle();
	}

	public void setTitleEnabled(boolean value) {
		titleEnabled = value;
		updateTitle();
	}

	private void updateTitle() {
		if (titleEnabled) {
			String text = tableModel.attrModel.getTitle();
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
}
