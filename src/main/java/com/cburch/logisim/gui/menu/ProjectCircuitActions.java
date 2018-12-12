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

package com.cburch.logisim.gui.menu;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.analyze.gui.Analyzer;
import com.cburch.logisim.analyze.gui.AnalyzerManager;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.circuit.AnalyzeException;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.SyntaxChecker;

public class ProjectCircuitActions {
	private static void analyzeError(Project proj, String message) {
		JOptionPane.showMessageDialog(proj.getFrame(), message,
				Strings.get("analyzeErrorTitle"), JOptionPane.ERROR_MESSAGE);
		return;
	}

	private static void configureAnalyzer(Project proj, Circuit circuit,
			Analyzer analyzer, Map<Instance, String> pinNames,
			ArrayList<String> inputNames, ArrayList<String> outputNames) {
		analyzer.getModel().setVariables(inputNames, outputNames);

		// If there are no inputs, we stop with that tab selected
		if (inputNames.size() == 0) {
			analyzer.setSelectedTab(Analyzer.INPUTS_TAB);
			return;
		}

		// If there are no outputs, we stop with that tab selected
		if (outputNames.size() == 0) {
			analyzer.setSelectedTab(Analyzer.OUTPUTS_TAB);
			return;
		}

		// Attempt to show the corresponding expression
		try {
			Analyze.computeExpression(analyzer.getModel(), circuit, pinNames);
			analyzer.setSelectedTab(Analyzer.EXPRESSION_TAB);
			return;
		} catch (AnalyzeException ex) {
			JOptionPane.showMessageDialog(proj.getFrame(), ex.getMessage(),
					Strings.get("analyzeNoExpressionTitle"),
					JOptionPane.INFORMATION_MESSAGE);
		}

		// As a backup measure, we compute a truth table.
		Analyze.computeTable(analyzer.getModel(), proj, circuit, pinNames);
		analyzer.setSelectedTab(Analyzer.TABLE_TAB);
	}

	public static void doAddCircuit(Project proj) {
		String name = promptForCircuitName(proj.getFrame(),
				proj.getLogisimFile(), "");
		if (name != null) {
			JLabel error = null;
			/* Checking for valid names */
			if (name.isEmpty()) {
				error = new JLabel(Strings.get("circuitNameMissingError"));
			} else
				if (CorrectLabel.IsKeyword(name,false)) {
					error = new JLabel("\""+name+"\": "+Strings.get("circuitNameKeyword"));
				} else
			if (!SyntaxChecker.isVariableNameAcceptable(name,false)) {
				error = new JLabel("\""+name+"\": "+Strings.get("circuitNameInvalidName"));
			} else
			if (NameIsInUse(proj,name)) {
				error = new JLabel("\""+name+"\": "+Strings.get("circuitNameExists"));
			}
			if (error != null) {
				JOptionPane.showMessageDialog(proj.getFrame(), error,
				Strings.get("circuitCreateTitle"), JOptionPane.ERROR_MESSAGE);
			} else {
				Circuit circuit = new Circuit(name, proj.getLogisimFile(),proj);
				proj.doAction(LogisimFileActions.addCircuit(circuit));
				proj.setCurrentCircuit(circuit);
			}
		}
	}
	
	private static boolean NameIsInUse(Project proj, String Name) {
		for (Library mylib : proj.getLogisimFile().getLibraries()) {
			if (NameIsInLibraries(mylib,Name))
				return true;
		}
		for (AddTool mytool : proj.getLogisimFile().getTools()) {
			if (Name.toUpperCase().equals(mytool.getName().toUpperCase()))
				return true;
		}
		return false;
	}
	
	private static boolean NameIsInLibraries(Library lib, String Name) {
		for (Library mylib : lib.getLibraries()) {
			if (NameIsInLibraries(mylib,Name))
				return true;
		}
		for (Tool mytool : lib.getTools()) {
			if (Name.toUpperCase().equals(mytool.getName().toUpperCase()))
				return true;
		}
		return false;
	}

	public static void doAnalyze(Project proj, Circuit circuit) {
		Map<Instance, String> pinNames = Analyze.getPinLabels(circuit);
		ArrayList<String> inputNames = new ArrayList<String>();
		ArrayList<String> outputNames = new ArrayList<String>();
		for (Map.Entry<Instance, String> entry : pinNames.entrySet()) {
			Instance pin = entry.getKey();
			boolean isInput = Pin.FACTORY.isInputPin(pin);
			if (isInput) {
				inputNames.add(entry.getValue());
			} else {
				outputNames.add(entry.getValue());
			}
			if (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1) {
				if (isInput) {
					analyzeError(proj, Strings.get("analyzeMultibitInputError"));
				} else {
					analyzeError(proj,
							Strings.get("analyzeMultibitOutputError"));
				}
				return;
			}
		}
		if (inputNames.size() > AnalyzerModel.MAX_INPUTS) {
			analyzeError(proj, StringUtil.format(
					Strings.get("analyzeTooManyInputsError"), ""
							+ AnalyzerModel.MAX_INPUTS));
			return;
		}
		if (outputNames.size() > AnalyzerModel.MAX_OUTPUTS) {
			analyzeError(proj, StringUtil.format(
					Strings.get("analyzeTooManyOutputsError"), ""
							+ AnalyzerModel.MAX_OUTPUTS));
			return;
		}

		Analyzer analyzer = AnalyzerManager.getAnalyzer();
		analyzer.getModel().setCurrentCircuit(proj, circuit);
		configureAnalyzer(proj, circuit, analyzer, pinNames, inputNames,
				outputNames);
		analyzer.setVisible(true);
		analyzer.toFront();
	}

	public static void doMoveCircuit(Project proj, Circuit cur, int delta) {
		AddTool tool = proj.getLogisimFile().getAddTool(cur);
		if (tool != null) {
			int oldPos = proj.getLogisimFile().getCircuits().indexOf(cur);
			int newPos = oldPos + delta;
			int toolsCount = proj.getLogisimFile().getTools().size();
			if (newPos >= 0 && newPos < toolsCount) {
				proj.doAction(LogisimFileActions.moveCircuit(tool, newPos));
			}
		}
	}

	public static void doRemoveCircuit(Project proj, Circuit circuit) {
		if (proj.getLogisimFile().getTools().size() == 1) {
			JOptionPane.showMessageDialog(proj.getFrame(),
					Strings.get("circuitRemoveLastError"),
					Strings.get("circuitRemoveErrorTitle"),
					JOptionPane.ERROR_MESSAGE);
		} else if (!proj.getDependencies().canRemove(circuit)) {
			JOptionPane.showMessageDialog(proj.getFrame(),
					Strings.get("circuitRemoveUsedError"),
					Strings.get("circuitRemoveErrorTitle"),
					JOptionPane.ERROR_MESSAGE);
		} else {
			proj.doAction(LogisimFileActions.removeCircuit(circuit));
		}
	}

	public static void doSetAsMainCircuit(Project proj, Circuit circuit) {
		proj.doAction(LogisimFileActions.setMainCircuit(circuit));
	}

	/**
	 * Ask the user for the name of the new circuit to create. If the name is
	 * valid, then it returns it, otherwise it displays an error message and
	 * returns null.
	 * 
	 * @param frame
	 *            Project's frame
	 * @param lib
	 *            Project's logisim file
	 * @param initialValue
	 *            Default suggested value (can be empty if no initial value)
	 */
	private static String promptForCircuitName(JFrame frame, Library lib,
			String initialValue) {
		JLabel label = new JLabel(Strings.get("circuitNamePrompt"));
		final JTextField field = new JTextField(15);
		field.setText(initialValue);
		JLabel error = new JLabel(" ");
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		JPanel strut = new JPanel(null);
		strut.setPreferredSize(new Dimension(
				3 * field.getPreferredSize().width / 2, 0));
		JPanel panel = new JPanel(gb);
		gc.gridx = 0;
		gc.gridy = GridBagConstraints.RELATIVE;
		gc.weightx = 1.0;
		gc.fill = GridBagConstraints.NONE;
		gc.anchor = GridBagConstraints.LINE_START;
		gb.setConstraints(label, gc);
		panel.add(label);
		gb.setConstraints(field, gc);
		panel.add(field);
		gb.setConstraints(error, gc);
		panel.add(error);
		gb.setConstraints(strut, gc);
		panel.add(strut);
		JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		pane.setInitialValue(field);
		JDialog dlog = pane.createDialog(frame,
				Strings.get("circuitNameDialogTitle"));
		dlog.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent arg0) {
				field.requestFocus();
			}

			public void windowLostFocus(WindowEvent arg0) {
			}
		});

		field.selectAll();
		dlog.pack();
		dlog.setVisible(true);
		field.requestFocusInWindow();
		Object action = pane.getValue();
		if (action == null || !(action instanceof Integer)
				|| ((Integer) action).intValue() != JOptionPane.OK_OPTION) {
			return null;
		}
		
		return field.getText().trim();
	}

	private ProjectCircuitActions() {
	}
}
