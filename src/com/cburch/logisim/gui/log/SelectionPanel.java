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
package com.cburch.logisim.gui.log;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import com.hepia.logisim.chronodata.TimelineParam;

class SelectionPanel extends LogPanel {
	private class Listener extends MouseAdapter implements ActionListener,
			TreeSelectionListener, ListSelectionListener, ItemListener {

		public void actionPerformed(ActionEvent event) {
			Object src = event.getSource();
			if (src == addTool) {
				doAdd(selector.getSelectedItems());
			} else if (src == changeBase) {
				SelectionItem sel = (SelectionItem) list.getSelectedValue();
				if (sel != null) {
					int radix = sel.getRadix();
					switch (radix) {
					case 2:
						sel.setRadix(10);
						break;
					case 10:
						sel.setRadix(16);
						break;
					default:
						sel.setRadix(2);
					}
				}
			} else if (src == moveUp) {
				doMove(-1);
			} else if (src == moveDown) {
				doMove(1);
			} else if (src == remove) {
				Selection sel = getSelection();
				Object[] toRemove = list.getSelectedValuesList().toArray();
				boolean changed = false;
				for (int i = 0; i < toRemove.length; i++) {
					int index = sel.indexOf((SelectionItem) toRemove[i]);
					if (index >= 0) {
						sel.remove(index);
						changed = true;
					}
				}
				if (changed) {
					list.clearSelection();
				}
				createClkChooser(getSelection());
			}
		}

		private void computeEnabled() {
			int index = list.getSelectedIndex();
			addTool.setEnabled(selector.hasSelectedItems());
			changeBase.setEnabled(index >= 0);
			moveUp.setEnabled(index > 0);
			moveDown.setEnabled(index >= 0
					&& index < list.getModel().getSize() - 1);
			remove.setEnabled(index >= 0);
		}

		private void doAdd(List<SelectionItem> selectedItems) {
			if (selectedItems != null && selectedItems.size() > 0) {
				SelectionItem last = null;
				for (SelectionItem item : selectedItems) {
					if (!getSelection().contains(item)) {
						getSelection().add(item);
						last = item;
					}
				}
				list.setSelectedValue(last, true);
				createClkChooser(getSelection());
			}
		}

		private void doMove(int delta) {
			Selection sel = getSelection();
			int oldIndex = list.getSelectedIndex();
			int newIndex = oldIndex + delta;
			if (oldIndex >= 0 && newIndex >= 0 && newIndex < sel.size()) {
				sel.move(oldIndex, newIndex);
				list.setSelectedIndex(newIndex);
			}
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			Object source = e.getItemSelectable();
			if (source == enableChoosePanelCheckBox) {
				for (Component com : chooseClkPanel.getComponents()) {
					com.setEnabled(enableChoosePanelCheckBox.isSelected());
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				TreePath path = selector.getPathForLocation(e.getX(), e.getY());
				if (path != null && listener != null) {
					doAdd(selector.getSelectedItems());
				}
			}
		}

		public void valueChanged(ListSelectionEvent event) {
			computeEnabled();
		}

		public void valueChanged(TreeSelectionEvent event) {
			computeEnabled();
		}
	}

	private static final long serialVersionUID = 1L;
	private Listener listener = new Listener();
	private ComponentSelector selector;
	private JButton addTool;
	private JButton changeBase;
	private JButton moveUp;
	private JButton moveDown;
	private JButton remove;
	private SelectionList list;
	private JCheckBox enableChoosePanelCheckBox;
	private JLabel enableChoosePanelCheckLabel;
	private JPanel enableChoosePanelCheckPanel;
	private JLabel chooseClkLabel;
	private JPanel chooseClkPanel;
	@SuppressWarnings("rawtypes")
	private JComboBox chooseClkCombo;
	private JLabel chooseClkFrequencyLabel;
	private JTextField chooseClkFrequencyTF;
	@SuppressWarnings("rawtypes")
	private JComboBox chooseClkUnitCombo;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SelectionPanel(LogFrame window) {
		super(window);
		selector = new ComponentSelector(getModel());
		addTool = new JButton();
		changeBase = new JButton();
		moveUp = new JButton();
		moveDown = new JButton();
		remove = new JButton();
		list = new SelectionList();
		list.setSelection(getSelection());

		JPanel buttons = new JPanel(new GridLayout(5, 1));
		buttons.add(addTool);
		buttons.add(changeBase);
		buttons.add(moveUp);
		buttons.add(moveDown);
		buttons.add(remove);

		addTool.addActionListener(listener);
		changeBase.addActionListener(listener);
		moveUp.addActionListener(listener);
		moveDown.addActionListener(listener);
		remove.addActionListener(listener);
		selector.addMouseListener(listener);
		selector.addTreeSelectionListener(listener);
		list.addListSelectionListener(listener);
		listener.computeEnabled();

		// === setup clk panel === //
		// enable area
		enableChoosePanelCheckPanel = new JPanel(new FlowLayout());
		enableChoosePanelCheckLabel = new JLabel(
				Strings.get("timeSelectionEnable"));
		enableChoosePanelCheckBox = new JCheckBox();
		enableChoosePanelCheckBox.setSelected(false);
		enableChoosePanelCheckBox.addItemListener(listener);
		enableChoosePanelCheckPanel.add(enableChoosePanelCheckLabel);
		enableChoosePanelCheckPanel.add(enableChoosePanelCheckBox);
		// freq area
		chooseClkLabel = new JLabel();
		chooseClkCombo = new JComboBox<>();
		chooseClkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		chooseClkFrequencyLabel = new JLabel();
		chooseClkFrequencyTF = new JTextField(5);
		chooseClkFrequencyTF.setText("1");
		chooseClkUnitCombo = new JComboBox(TimelineParam.units);
		chooseClkUnitCombo.setSelectedIndex(0);
		chooseClkPanel.add(chooseClkLabel);
		chooseClkPanel.add(chooseClkCombo);
		chooseClkPanel.add(chooseClkFrequencyLabel);
		chooseClkPanel.add(chooseClkFrequencyTF);
		chooseClkPanel.add(chooseClkUnitCombo);
		// disable all component in chooseClkPanel
		for (Component com : chooseClkPanel.getComponents()) {
			com.setEnabled(false);
		}

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout(gridbag);
		JScrollPane explorerPane = new JScrollPane(selector,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JScrollPane listPane = new JScrollPane(list,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gridbag.setConstraints(explorerPane, gbc);
		add(explorerPane);
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 0.0;
		gridbag.setConstraints(buttons, gbc);
		add(buttons);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gridbag.setConstraints(listPane, gbc);
		add(listPane);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.0;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gridbag.setConstraints(enableChoosePanelCheckPanel, gbc);
		add(enableChoosePanelCheckPanel);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.0;
		gbc.gridwidth = 3;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gridbag.setConstraints(chooseClkPanel, gbc);
		add(chooseClkPanel);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void createClkChooser(Selection selection) {
		ArrayList<String> allSelected = new ArrayList<String>();
		for (int i = 0; i < selection.size(); ++i) {
			String name = selection.get(i).toString();
			if (!name.equalsIgnoreCase("sysclk")) {
				allSelected.add(name);
			}
		}
		chooseClkCombo.removeAll();
		chooseClkCombo
				.setModel(new DefaultComboBoxModel(allSelected.toArray()));
		int defaultPos = allSelected.indexOf("clk");
		if (defaultPos != -1) {
			chooseClkCombo.setSelectedIndex(defaultPos);
		}
	}

	@Override
	public String getHelpText() {
		return Strings.get("selectionHelp");
	}

	public TimelineParam getTimelineParam() {
		String selFreqUnit = (String) chooseClkUnitCombo.getSelectedItem();
		String selClk = (String) chooseClkCombo.getSelectedItem();
		int selFreq = 0;
		try {
			selFreq = Integer.parseInt(chooseClkFrequencyTF.getText());
		} catch (Exception e) {
		}

		if (enableChoosePanelCheckBox.isSelected() && selFreqUnit != null
				&& selClk != null && selFreq > 0) {
			return new TimelineParam(selFreqUnit, selClk, selFreq);
		} else {
			return null;
		}
	}

	@Override
	public String getTitle() {
		return Strings.get("selectionTab");
	}

	@Override
	public void localeChanged() {
		addTool.setText(Strings.get("selectionAdd"));
		changeBase.setText(Strings.get("selectionChangeBase"));
		moveUp.setText(Strings.get("selectionMoveUp"));
		moveDown.setText(Strings.get("selectionMoveDown"));
		remove.setText(Strings.get("selectionRemove"));
		chooseClkLabel.setText(Strings.get("timeSelectionClock"));
		chooseClkFrequencyLabel.setText(Strings.get("timeSelectionFrequency"));
		selector.localeChanged();
		list.localeChanged();
	}

	@Override
	public void modelChanged(Model oldModel, Model newModel) {
		if (getModel() == null) {
			selector.setLogModel(newModel);
			list.setSelection(null);
		} else {
			selector.setLogModel(newModel);
			list.setSelection(getSelection());
		}
		listener.computeEnabled();
	}
}
