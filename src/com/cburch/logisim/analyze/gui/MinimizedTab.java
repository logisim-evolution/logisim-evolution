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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.OutputExpressions;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;

class MinimizedTab extends AnalyzerTab {
	@SuppressWarnings("rawtypes")
	private static class FormatModel extends AbstractListModel implements
			ComboBoxModel {
		static int getFormatIndex(int choice) {
			switch (choice) {
			case AnalyzerModel.FORMAT_PRODUCT_OF_SUMS:
				return 1;
			default:
				return 0;
			}
		}

		private static final long serialVersionUID = 1L;

		private String[] choices;
		private int selected;

		private FormatModel() {
			selected = 0;
			choices = new String[2];
			localeChanged();
		}

		public Object getElementAt(int index) {
			return choices[index];
		}

		int getSelectedFormat() {
			switch (selected) {
			case 1:
				return AnalyzerModel.FORMAT_PRODUCT_OF_SUMS;
			default:
				return AnalyzerModel.FORMAT_SUM_OF_PRODUCTS;
			}
		}

		public Object getSelectedItem() {
			return choices[selected];
		}

		public int getSize() {
			return choices.length;
		}

		void localeChanged() {
			choices[0] = Strings.get("minimizedSumOfProducts");
			choices[1] = Strings.get("minimizedProductOfSums");
			fireContentsChanged(this, 0, choices.length);
		}

		public void setSelectedItem(Object value) {
			for (int i = 0; i < choices.length; i++) {
				if (choices[i].equals(value)) {
					selected = i;
				}
			}
		}
	}

	private class MyListener implements OutputExpressionsListener,
			ActionListener, ItemListener {
		public void actionPerformed(ActionEvent event) {
			String output = getCurrentVariable();
			int format = outputExprs.getMinimizedFormat(output);
			formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
			outputExprs.setExpression(output,
					outputExprs.getMinimalExpression(output));
		}

		public void expressionChanged(OutputExpressionsEvent event) {
			String output = getCurrentVariable();
			if (event.getType() == OutputExpressionsEvent.OUTPUT_MINIMAL
					&& event.getVariable().equals(output)) {
				minimizedExpr.setExpression(outputExprs
						.getMinimalExpression(output));
				MinimizedTab.this.validate();
			}
			setAsExpr.setEnabled(output != null
					&& !outputExprs.isExpressionMinimal(output));
			int format = outputExprs.getMinimizedFormat(output);
			formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
		}

		public void itemStateChanged(ItemEvent event) {
			if (event.getSource() == formatChoice) {
				String output = getCurrentVariable();
				FormatModel model = (FormatModel) formatChoice.getModel();
				outputExprs.setMinimizedFormat(output,
						model.getSelectedFormat());
			} else {
				updateTab();
			}
		}
	}

	private static final long serialVersionUID = 1L;

	private OutputSelector selector;
	private KarnaughMapPanel karnaughMap;
	private JLabel formatLabel = new JLabel();
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JComboBox formatChoice = new JComboBox<>(new FormatModel());
	private ExpressionView minimizedExpr = new ExpressionView();
	private JButton setAsExpr = new JButton();

	private MyListener myListener = new MyListener();
	// private AnalyzerModel model;
	private OutputExpressions outputExprs;

	public MinimizedTab(AnalyzerModel model) {
		// this.model = model;
		this.outputExprs = model.getOutputExpressions();
		outputExprs.addOutputExpressionsListener(myListener);

		selector = new OutputSelector(model);
		selector.addItemListener(myListener);
		karnaughMap = new KarnaughMapPanel(model);
		karnaughMap.addMouseListener(new TruthTableMouseListener());
		setAsExpr.addActionListener(myListener);
		formatChoice.addItemListener(myListener);
		
		JPanel buttons = new JPanel(new GridLayout(1, 1));
		buttons.add(setAsExpr);

		JPanel formatPanel = new JPanel();
		formatPanel.add(formatLabel);
		formatPanel.add(formatChoice);

		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		setLayout(gb);
		gc.gridx = 0;
		gc.gridy = 0;
		addRow(gb, gc, selector.getLabel(), selector.getComboBox());
		addRow(gb, gc, formatLabel, formatChoice);

		gc.weightx = 0.0;
		gc.gridx = 0;
		gc.gridwidth = 2;
		gc.gridy = GridBagConstraints.RELATIVE;
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.CENTER;
		gb.setConstraints(karnaughMap, gc);
		add(karnaughMap);
		Insets oldInsets = gc.insets;
		gc.insets = new Insets(20, 0, 0, 0);
		gb.setConstraints(minimizedExpr, gc);
		add(minimizedExpr);
		gc.insets = oldInsets;
		gc.fill = GridBagConstraints.NONE;
		gb.setConstraints(buttons, gc);
		add(buttons);

		String selected = selector.getSelectedOutput();
		setAsExpr.setEnabled(selected != null
				&& !outputExprs.isExpressionMinimal(selected));
	}

	private void addRow(GridBagLayout gb, GridBagConstraints gc, JLabel label,
			@SuppressWarnings("rawtypes") JComboBox choice) {
		Insets oldInsets = gc.insets;
		gc.weightx = 0.0;
		gc.gridx = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.LINE_START;
		gc.insets = new Insets(5, 5, 5, 5);
		gb.setConstraints(label, gc);
		add(label);
		gc.gridx = 1;
		gc.fill = GridBagConstraints.VERTICAL;
		gb.setConstraints(choice, gc);
		add(choice);
		gc.gridy++;
		gc.insets = oldInsets;
	}

	private String getCurrentVariable() {
		return selector.getSelectedOutput();
	}

	@Override
	void localeChanged() {
		selector.localeChanged();
		karnaughMap.localeChanged();
		minimizedExpr.localeChanged();
		setAsExpr.setText(Strings.get("minimizedSetButton"));
		formatLabel.setText(Strings.get("minimizedFormat"));
		((FormatModel) formatChoice.getModel()).localeChanged();
	}

	@Override
	void updateTab() {
		String output = getCurrentVariable();
		karnaughMap.setOutput(output);
		int format = outputExprs.getMinimizedFormat(output);
		formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
		minimizedExpr.setExpression(outputExprs.getMinimalExpression(output));
		setAsExpr.setEnabled(output != null
				&& !outputExprs.isExpressionMinimal(output));
	}
}
