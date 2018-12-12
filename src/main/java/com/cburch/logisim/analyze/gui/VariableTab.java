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

package com.cburch.logisim.analyze.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.analyze.model.VariableListEvent;
import com.cburch.logisim.analyze.model.VariableListListener;
import com.cburch.logisim.util.StringUtil;

class VariableTab extends AnalyzerTab implements TabInterface {
	private class MyListener implements ActionListener, DocumentListener,
			ListSelectionListener {
		public void actionPerformed(ActionEvent event) {
			Object src = event.getSource();
			if ((src == add || src == field) && add.isEnabled()) {
				String name = field.getText().trim();
				if (!name.equals("")) {
					data.add(name);
					if (data.contains(name)) {
						list.setSelectedValue(name, true);
					}
					field.setText("");
					field.grabFocus();
				}
			} else if (src == rename) {
				String oldName = (String) list.getSelectedValue();
				String newName = field.getText().trim();
				if (oldName != null && !newName.equals("")) {
					data.replace(oldName, newName);
					field.setText("");
					field.grabFocus();
				}
			} else if (src == remove) {
				String name = (String) list.getSelectedValue();
				if (name != null)
					data.remove(name);
			} else if (src == moveUp) {
				String name = (String) list.getSelectedValue();
				if (name != null) {
					data.move(name, -1);
					list.setSelectedValue(name, true);
				}
			} else if (src == moveDown) {
				String name = (String) list.getSelectedValue();
				if (name != null) {
					data.move(name, 1);
					list.setSelectedValue(name, true);
				}
			}
		}

		public void changedUpdate(DocumentEvent event) {
			insertUpdate(event);
		}

		public void insertUpdate(DocumentEvent event) {
			computeEnabled();
		}

		public void removeUpdate(DocumentEvent event) {
			insertUpdate(event);
		}

		public void valueChanged(ListSelectionEvent event) {
			computeEnabled();
		}

		/*
		 * public void listChanged(VariableListEvent event) { switch
		 * (event.getType()) { case VariableListEvent.ALL_REPLACED:
		 * list.setSelectedIndices(new int[0]); break; case
		 * VariableListEvent.REMOVE: if
		 * (event.getVariable().equals(list.getSelectedValue())) { int index =
		 * ((Integer) event.getData()).intValue(); if (index >= data.size()) {
		 * if (data.isEmpty()) { list.setSelectedIndices(new int[0]); } index =
		 * data.size() - 1; } list.setSelectedValue(data.get(index), true); }
		 * break; case VariableListEvent.ADD: case VariableListEvent.MOVE: case
		 * VariableListEvent.REPLACE: break; } list.validate(); }
		 */
	}

	@SuppressWarnings("rawtypes")
	private static class VariableListModel extends AbstractListModel implements
			VariableListListener {
		private static final long serialVersionUID = 1L;
		private VariableList list;
		private String[] listCopy;

		public VariableListModel(VariableList list) {
			this.list = list;
			updateCopy();
			list.addVariableListListener(this);
		}

		public Object getElementAt(int index) {
			return index >= 0 && index < listCopy.length ? listCopy[index]
					: null;
		}

		public int getSize() {
			return listCopy.length;
		}

		public void listChanged(VariableListEvent event) {
			String[] oldCopy = listCopy;
			updateCopy();
			int index;
			switch (event.getType()) {
			case VariableListEvent.ALL_REPLACED:
				fireContentsChanged(this, 0, oldCopy.length);
				return;
			case VariableListEvent.ADD:
				index = list.indexOf(event.getVariable());
				fireIntervalAdded(this, index, index);
				return;
			case VariableListEvent.REMOVE:
				index = ((Integer) event.getData()).intValue();
				fireIntervalRemoved(this, index, index);
				return;
			case VariableListEvent.MOVE:
				fireContentsChanged(this, 0, getSize());
				return;
			case VariableListEvent.REPLACE:
				index = ((Integer) event.getData()).intValue();
				fireContentsChanged(this, index, index);
				return;
			}
		}

		private void update() {
			String[] oldCopy = listCopy;
			updateCopy();
			fireContentsChanged(this, 0, oldCopy.length);
		}

		private void updateCopy() {
			listCopy = list.toArray(new String[list.size()]);
		}
	}

	private static final long serialVersionUID = 1L;

	private VariableList data;
	private MyListener myListener = new MyListener();
	private VariableTab Othertab = null;
	private String OtherId;

	@SuppressWarnings("rawtypes")
	private JList list = new JList();
	private JTextField field = new JTextField();
	private JButton remove = new JButton();
	private JButton moveUp = new JButton();
	private JButton moveDown = new JButton();
	private JButton add = new JButton();
	private JButton rename = new JButton();
	private JLabel error = new JLabel(" ");

	@SuppressWarnings("unchecked")
	VariableTab(VariableList data) {
		this.data = data;

		list.setModel(new VariableListModel(data));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(myListener);
		remove.addActionListener(myListener);
		moveUp.addActionListener(myListener);
		moveDown.addActionListener(myListener);
		add.addActionListener(myListener);
		rename.addActionListener(myListener);
		field.addActionListener(myListener);
		field.getDocument().addDocumentListener(myListener);

		JScrollPane listPane = new JScrollPane(list,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		listPane.setPreferredSize(new Dimension(100, 100));

		JPanel topPanel = new JPanel(new GridLayout(3, 1));
		topPanel.add(remove);
		topPanel.add(moveUp);
		topPanel.add(moveDown);

		JPanel fieldPanel = new JPanel();
		fieldPanel.add(rename);
		fieldPanel.add(add);

		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		setLayout(gb);
		Insets oldInsets = gc.insets;

		gc.insets = new Insets(10, 10, 0, 0);
		gc.fill = GridBagConstraints.BOTH;
		gc.weightx = 1.0;
		gb.setConstraints(listPane, gc);
		add(listPane);

		gc.fill = GridBagConstraints.NONE;
		gc.anchor = GridBagConstraints.PAGE_START;
		gc.weightx = 0.0;
		gb.setConstraints(topPanel, gc);
		add(topPanel);

		gc.insets = new Insets(10, 10, 0, 10);
		gc.gridwidth = GridBagConstraints.REMAINDER;
		gc.gridx = 0;
		gc.gridy = GridBagConstraints.RELATIVE;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gb.setConstraints(field, gc);
		add(field);

		gc.insets = oldInsets;
		gc.fill = GridBagConstraints.NONE;
		gc.anchor = GridBagConstraints.LINE_END;
		gb.setConstraints(fieldPanel, gc);
		add(fieldPanel);

		gc.fill = GridBagConstraints.HORIZONTAL;
		gb.setConstraints(error, gc);
		add(error);

		if (!data.isEmpty())
			list.setSelectedValue(data.get(0), true);
		computeEnabled();
	}
	
	public void SetCompanion(VariableTab tab , String id) {
		Othertab = tab;
		OtherId = id;
	}

	private void computeEnabled() {
		int index = list.getSelectedIndex();
		int max = list.getModel().getSize();
		boolean selected = index >= 0 && index < max;
		remove.setEnabled(selected);
		moveUp.setEnabled(selected && index > 0);
		moveDown.setEnabled(selected && index < max);

		boolean ok = validateInput();
		add.setEnabled(ok && data.size() < data.getMaximumSize());
		rename.setEnabled(ok && selected);
	}

	public void copy() {
		field.requestFocus();
		field.copy();
	}

	public void delete() {
		field.requestFocus();
		field.replaceSelection("");
	}

	@Override
	void localeChanged() {
		remove.setText(Strings.get("variableRemoveButton"));
		moveUp.setText(Strings.get("variableMoveUpButton"));
		moveDown.setText(Strings.get("variableMoveDownButton"));
		add.setText(Strings.get("variableAddButton"));
		rename.setText(Strings.get("variableRenameButton"));
		validateInput();
	}

	public void paste() {
		field.requestFocus();
		field.paste();
	}

	void registerDefaultButtons(DefaultRegistry registry) {
		registry.registerDefaultButton(field, add);
	}

	public void selectAll() {
		field.requestFocus();
		field.selectAll();
	}

	@Override
	void updateTab() {
		VariableListModel model = (VariableListModel) list.getModel();
		model.update();
	}
	
	public boolean contains(String label) {
		for (int i = 0, n = data.size(); i < n; i++) {
			String other = data.get(i);
			if (label.toLowerCase().equals(other.toLowerCase()))
				return true;
		}
		return false;
	}

	private boolean validateInput() {
		String text = field.getText().trim();
		boolean ok = true;
		boolean errorShown = true;
		if (text.length() == 0) {
			errorShown = false;
			ok = false;
		} else if (!Character.isJavaIdentifierStart(text.charAt(0))) {
			error.setText(Strings.get("variableStartError"));
			ok = false;
		} else {
			for (int i = 1; i < text.length() && ok; i++) {
				char c = text.charAt(i);
				if (!Character.isJavaIdentifierPart(c)) {
					error.setText(StringUtil.format(
							Strings.get("variablePartError"), "" + c));
					ok = false;
				}
			}
		}
		if (ok) {
			if (!CorrectLabel.IsCorrectLabel(text)) {
				ok = false;
				if (CorrectLabel.IsKeyword(text, false)) {
					error.setText(Strings.get("HdlKeyword"));
				} else {
					String wrong = CorrectLabel.FirstInvalidCharacter(text);
					error.setText(StringUtil.format(Strings.get("InvalidCharacter"),wrong));
				}
			}
		}
		if (ok) {
			if (contains(text)) {
				error.setText(Strings.get("variableDuplicateError"));
				ok = false;
			}
		}
		if (ok&&(Othertab!=null)) {
			if (Othertab.contains(text)) {
				error.setText(StringUtil.format(Strings.get("variableDuplicateError1"), OtherId));
				ok = false;
			}
		}
		if (ok || !errorShown) {
			if (data.size() >= data.getMaximumSize()) {
				error.setText(StringUtil.format(
						Strings.get("variableMaximumError"),
						"" + data.getMaximumSize()));
			} else {
				error.setText(" ");
			}
		}
		return ok;
	}
}
