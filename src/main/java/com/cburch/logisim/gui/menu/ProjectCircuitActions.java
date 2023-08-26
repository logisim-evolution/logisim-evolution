/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.contracts.BaseWindowFocusListenerContract;
import com.cburch.logisim.analyze.gui.Analyzer;
import com.cburch.logisim.analyze.gui.AnalyzerManager;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.circuit.AnalyzeException;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.SyntaxChecker;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ProjectCircuitActions {
  private ProjectCircuitActions() {
    // dummy, private
  }

  private static void analyzeError(Project proj, String message) {
    OptionPane.showMessageDialog(
        proj.getFrame(), message, S.get("analyzeErrorTitle"), OptionPane.ERROR_MESSAGE);
  }

  private static void configureAnalyzer(
      Project proj,
      Circuit circuit,
      Analyzer analyzer,
      Map<Instance, String> pinNames,
      ArrayList<Var> inputVars,
      ArrayList<Var> outputVars) {
    analyzer.getModel().setVariables(inputVars, outputVars);

    // If there are no inputs or outputs, we stop with that tab selected
    if (inputVars.size() == 0 || outputVars.size() == 0) {
      analyzer.setSelectedTab(Analyzer.IO_TAB);
      return;
    }

    // Attempt to show the corresponding expression
    try {
      Analyze.computeExpression(analyzer.getModel(), circuit, pinNames);
      analyzer.setSelectedTab(Analyzer.EXPRESSION_TAB);
      return;
    } catch (AnalyzeException ex) {
      OptionPane.showMessageDialog(
          proj.getFrame(),
          ex.getMessage(),
          S.get("analyzeNoExpressionTitle"),
          OptionPane.INFORMATION_MESSAGE);
    }

    // As a backup measure, we compute a truth table.
    Analyze.computeTable(analyzer.getModel(), proj, circuit, pinNames);
    analyzer.setSelectedTab(Analyzer.TABLE_TAB);
  }

  public static void doAddCircuit(Project proj) {
    final var name = promptForCircuitName(proj.getFrame(), proj.getLogisimFile(), "");
    if (name != null) {
      String error = null;
      /* Checking for valid names */
      if (name.isEmpty()) {
        error = S.get("circuitNameMissingError");
      } else if (CorrectLabel.isKeyword(name, false)) {
        error = "\"" + name + "\": " + S.get("circuitNameKeyword");
      } else if (nameIsInUse(proj, name)) {
        error = "\"" + name + "\": " + S.get("circuitNameExists");
      }
      else {
        String nameMessage = SyntaxChecker.getErrorMessage(name);
        if (nameMessage != null) {
          error = "\"" + name + "\": " + nameMessage;
        }
      }
      if (error != null) {
        OptionPane.showMessageDialog(
            proj.getFrame(), error, S.get("circuitCreateTitle"), OptionPane.ERROR_MESSAGE);
      } else {
        final var circuit = new Circuit(name, proj.getLogisimFile(), proj);
        proj.doAction(LogisimFileActions.addCircuit(circuit));
        proj.setCurrentCircuit(circuit);
      }
    }
  }

  private static boolean nameIsInUse(Project proj, String name) {
    for (Library mylib : proj.getLogisimFile().getLibraries()) {
      if (nameIsInLibraries(mylib, name)) return true;
    }
    for (AddTool mytool : proj.getLogisimFile().getTools()) {
      if (name.equalsIgnoreCase(mytool.getName())) return true;
    }
    return false;
  }

  private static boolean nameIsInLibraries(Library lib, String name) {
    for (final var myLib : lib.getLibraries()) {
      if (nameIsInLibraries(myLib, name)) return true;
    }
    for (final var myTool : lib.getTools()) {
      if (name.equalsIgnoreCase(myTool.getName())) return true;
    }
    return false;
  }

  public static void doAddVhdl(Project proj) {
    final var name = promptForVhdlName(proj.getFrame(), proj.getLogisimFile(), "");
    if (name != null) {
      final var content = VhdlContent.create(name, proj.getLogisimFile());
      if (content != null) {
        proj.doAction(LogisimFileActions.addVhdl(content));
        proj.setCurrentHdlModel(content);
      }
    }
  }

  public static void doImportVhdl(Project proj) {
    final var vhdl = proj.getLogisimFile().getLoader().vhdlImportChooser(proj.getFrame());
    if (vhdl == null) return;

    final var content = VhdlContent.parse(null, vhdl, proj.getLogisimFile());
    if (content != null) return;
    if (VhdlContent.labelVHDLInvalidNotify(content.getName(), proj.getLogisimFile())) return;

    proj.doAction(LogisimFileActions.addVhdl(content));
    proj.setCurrentHdlModel(content);
  }

  public static void doAnalyze(Project proj, Circuit circuit) {
    final var pinNames = Analyze.getPinLabels(circuit);
    final var inputVars = new ArrayList<Var>();
    final var outputVars = new ArrayList<Var>();
    var numInputs = 0;
    var numOutputs = 0;
    for (final var entry : pinNames.entrySet()) {
      final var pin = entry.getKey();
      final var isInput = Pin.FACTORY.isInputPin(pin);
      final var width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
      final var v = new Var(entry.getValue(), width);
      if (isInput) {
        inputVars.add(v);
        numInputs += width;
      } else {
        outputVars.add(v);
        numOutputs += width;
      }
    }
    if (numInputs > AnalyzerModel.MAX_INPUTS) {
      analyzeError(proj, S.get("analyzeTooManyInputsError", "" + AnalyzerModel.MAX_INPUTS));
      return;
    }
    if (numOutputs > AnalyzerModel.MAX_OUTPUTS) {
      analyzeError(proj, S.get("analyzeTooManyOutputsError", "" + AnalyzerModel.MAX_OUTPUTS));
      return;
    }

    final var analyzer = AnalyzerManager.getAnalyzer(proj.getFrame());
    analyzer.getModel().setCurrentCircuit(proj, circuit);
    configureAnalyzer(proj, circuit, analyzer, pinNames, inputVars, outputVars);
    if (!analyzer.isVisible()) {
      analyzer.setVisible(true);
    }
    analyzer.toFront();
  }

  public static void doMoveCircuit(Project proj, Circuit cur, int delta) {
    final var tool = proj.getLogisimFile().getAddTool(cur);
    if (tool != null) {
      final var oldPos = proj.getLogisimFile().indexOfCircuit(cur);
      final var newPos = oldPos + delta;
      final var toolsCount = proj.getLogisimFile().getTools().size();
      if (newPos >= 0 && newPos < toolsCount) {
        proj.doAction(LogisimFileActions.moveCircuit(tool, newPos));
      }
    }
  }

  public static void doRemoveCircuit(Project proj, Circuit circuit) {
    if (proj.getLogisimFile().getCircuits().size() == 1) {
      OptionPane.showMessageDialog(
          proj.getFrame(),
          S.get("circuitRemoveLastError"),
          S.get("circuitRemoveErrorTitle"),
          OptionPane.ERROR_MESSAGE);
    } else if (!proj.getDependencies().canRemove(circuit)) {
      OptionPane.showMessageDialog(
          proj.getFrame(),
          S.get("circuitRemoveUsedError"),
          S.get("circuitRemoveErrorTitle"),
          OptionPane.ERROR_MESSAGE);
    } else {
      proj.doAction(LogisimFileActions.removeCircuit(circuit));
    }
  }

  public static void doRemoveVhdl(Project proj, VhdlContent vhdl) {
    if (!proj.getDependencies().canRemove(vhdl)) {
      OptionPane.showMessageDialog(
          proj.getFrame(),
          S.get("circuitRemoveUsedError"),
          S.get("circuitRemoveErrorTitle"),
          OptionPane.ERROR_MESSAGE);
    } else {
      proj.doAction(LogisimFileActions.removeVhdl(vhdl));
    }
  }

  public static void doSetAsMainCircuit(Project proj, Circuit circuit) {
    proj.doAction(LogisimFileActions.setMainCircuit(circuit));
  }

  /**
   * Ask the user for the name of the new circuit to create. If the name is valid, then it returns
   * it, otherwise it displays an error message and returns null.
   *
   * @param frame Project's frame
   * @param lib Project's logisim file
   * @param initialValue Default suggested value (can be empty if no initial value)
   */
  private static String promptForCircuitName(JFrame frame, Library lib, String initialValue) {
    return promptForNewName(frame, lib, initialValue, false);
  }

  private static String promptForVhdlName(JFrame frame, LogisimFile file, String initialValue) {
    final var name = promptForNewName(frame, file, initialValue, true);
    if (name == null) return null;
    if (VhdlContent.labelVHDLInvalidNotify(name, file)) return null;
    return name;
  }

  private static String promptForNewName(
      JFrame frame, Library lib, String initialValue, boolean vhdl) {
    String title;
    String prompt;
    if (vhdl) {
      title = S.get("vhdlNameDialogTitle");
      prompt = S.get("vhdlNamePrompt");
    } else {
      title = S.get("circuitNameDialogTitle");
      prompt = S.get("circuitNamePrompt");
    }
    final var field = new JTextField(15);
    field.setText(initialValue);
    final var gbl = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    final var strut = new JPanel(null);
    strut.setPreferredSize(new Dimension(3 * field.getPreferredSize().width / 2, 0));
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.LINE_START;
    final var label = new JLabel(prompt);
    gbl.setConstraints(label, gbc);
    final var panel = new JPanel(gbl);
    panel.add(label);
    gbl.setConstraints(field, gbc);
    panel.add(field);
    final var error = new JLabel(" ");
    gbl.setConstraints(error, gbc);
    panel.add(error);
    gbl.setConstraints(strut, gbc);
    panel.add(strut);
    final var pane =
        new JOptionPane(panel, OptionPane.QUESTION_MESSAGE, OptionPane.OK_CANCEL_OPTION);
    pane.setInitialValue(field);
    final var dlog = pane.createDialog(frame, title);
    dlog.addWindowFocusListener(
        new BaseWindowFocusListenerContract() {
          @Override
          public void windowGainedFocus(WindowEvent arg0) {
            field.requestFocus();
          }
        });

    field.selectAll();
    dlog.pack();
    dlog.setVisible(true);
    field.requestFocusInWindow();

    final var action = pane.getValue();
    if (!(action instanceof Integer) || (Integer) action != OptionPane.OK_OPTION) {
      return null;
    }

    return field.getText().trim();
  }
}
