/**
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

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

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
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.SyntaxChecker;
import com.cburch.logisim.vhdl.base.VhdlContent;
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

public class ProjectCircuitActions {
  private static void analyzeError(Project proj, String message) {
    OptionPane.showMessageDialog(
        proj.getFrame(), message, S.get("analyzeErrorTitle"), OptionPane.ERROR_MESSAGE);
    return;
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
    String name = promptForCircuitName(proj.getFrame(), proj.getLogisimFile(), "");
    if (name != null) {
      JLabel error = null;
      /* Checking for valid names */
      if (name.isEmpty()) {
        error = new JLabel(S.get("circuitNameMissingError"));
      } else if (CorrectLabel.IsKeyword(name, false)) {
        error = new JLabel("\"" + name + "\": " + S.get("circuitNameKeyword"));
      } else if (!SyntaxChecker.isVariableNameAcceptable(name, false)) {
        error = new JLabel("\"" + name + "\": " + S.get("circuitNameInvalidName"));
      } else if (NameIsInUse(proj, name)) {
        error = new JLabel("\"" + name + "\": " + S.get("circuitNameExists"));
      }
      if (error != null) {
        OptionPane.showMessageDialog(
            proj.getFrame(), error, S.get("circuitCreateTitle"), OptionPane.ERROR_MESSAGE);
      } else {
        Circuit circuit = new Circuit(name, proj.getLogisimFile(), proj);
        proj.doAction(LogisimFileActions.addCircuit(circuit));
        proj.setCurrentCircuit(circuit);
      }
    }
  }

  private static boolean NameIsInUse(Project proj, String Name) {
    for (Library mylib : proj.getLogisimFile().getLibraries()) {
      if (NameIsInLibraries(mylib, Name)) return true;
    }
    for (AddTool mytool : proj.getLogisimFile().getTools()) {
      if (Name.toUpperCase().equals(mytool.getName().toUpperCase())) return true;
    }
    return false;
  }

  private static boolean NameIsInLibraries(Library lib, String Name) {
    for (Library mylib : lib.getLibraries()) {
      if (NameIsInLibraries(mylib, Name)) return true;
    }
    for (Tool mytool : lib.getTools()) {
      if (Name.toUpperCase().equals(mytool.getName().toUpperCase())) return true;
    }
    return false;
  }

  public static void doAddVhdl(Project proj) {
    String name = promptForVhdlName(proj.getFrame(), proj.getLogisimFile(), "");
    if (name != null) {
      VhdlContent content = VhdlContent.create(name, proj.getLogisimFile());
      if (content == null) return;
      proj.doAction(LogisimFileActions.addVhdl(content));
      proj.setCurrentHdlModel(content);
    }
  }

  public static void doImportVhdl(Project proj) {
    String vhdl = proj.getLogisimFile().getLoader().vhdlImportChooser(proj.getFrame());
    if (vhdl == null) return;
    VhdlContent content = VhdlContent.parse(null, vhdl, proj.getLogisimFile());
    if (content == null) return;
    if (VhdlContent.labelVHDLInvalidNotify(content.getName(), proj.getLogisimFile())) {
      return;
    }
    proj.doAction(LogisimFileActions.addVhdl(content));
    proj.setCurrentHdlModel(content);
  }

  public static void doAnalyze(Project proj, Circuit circuit) {
    Map<Instance, String> pinNames = Analyze.getPinLabels(circuit);
    ArrayList<Var> inputVars = new ArrayList<Var>();
    ArrayList<Var> outputVars = new ArrayList<Var>();
    int numInputs = 0, numOutputs = 0;
    for (Map.Entry<Instance, String> entry : pinNames.entrySet()) {
      Instance pin = entry.getKey();
      boolean isInput = Pin.FACTORY.isInputPin(pin);
      int width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
      Var v = new Var(entry.getValue(), width);
      if (isInput) {
        inputVars.add(v);
        numInputs += width;
      } else {
        outputVars.add(v);
        numOutputs += width;
      }
    }
    if (numInputs > AnalyzerModel.MAX_INPUTS) {
      analyzeError(
          proj,
          StringUtil.format(S.get("analyzeTooManyInputsError"), "" + AnalyzerModel.MAX_INPUTS));
      return;
    }
    if (numOutputs > AnalyzerModel.MAX_OUTPUTS) {
      analyzeError(
          proj,
          StringUtil.format(S.get("analyzeTooManyOutputsError"), "" + AnalyzerModel.MAX_OUTPUTS));
      return;
    }

    Analyzer analyzer = AnalyzerManager.getAnalyzer(proj.getFrame());
    analyzer.getModel().setCurrentCircuit(proj, circuit);
    configureAnalyzer(proj, circuit, analyzer, pinNames, inputVars, outputVars);
    if (!analyzer.isVisible()) {
      analyzer.setVisible(true);
    }
    analyzer.toFront();
  }

  public static void doMoveCircuit(Project proj, Circuit cur, int delta) {
    AddTool tool = proj.getLogisimFile().getAddTool(cur);
    if (tool != null) {
      int oldPos = proj.getLogisimFile().indexOfCircuit(cur);
      int newPos = oldPos + delta;
      int toolsCount = proj.getLogisimFile().getTools().size();
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
    String name = promptForNewName(frame, file, initialValue, true);
    if (name == null) return null;
    if (VhdlContent.labelVHDLInvalidNotify(name, file)) {
      return null;
    }
    return name;
  }

  private static String promptForNewName(
      JFrame frame, Library lib, String initialValue, boolean vhdl) {
    String title, prompt;
    if (vhdl) {
      title = S.get("vhdlNameDialogTitle");
      prompt = S.get("vhdlNamePrompt");
    } else {
      title = S.get("circuitNameDialogTitle");
      prompt = S.get("circuitNamePrompt");
    }
    JLabel label = new JLabel(prompt);
    final JTextField field = new JTextField(15);
    field.setText(initialValue);
    JLabel error = new JLabel(" ");
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    JPanel strut = new JPanel(null);
    strut.setPreferredSize(new Dimension(3 * field.getPreferredSize().width / 2, 0));
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
    JOptionPane pane =
        new JOptionPane(panel, OptionPane.QUESTION_MESSAGE, OptionPane.OK_CANCEL_OPTION);
    pane.setInitialValue(field);
    JDialog dlog = pane.createDialog(frame, title);
    dlog.addWindowFocusListener(
        new WindowFocusListener() {
          public void windowGainedFocus(WindowEvent arg0) {
            field.requestFocus();
          }

          public void windowLostFocus(WindowEvent arg0) {}
        });

    field.selectAll();
    dlog.pack();
    dlog.setVisible(true);
    field.requestFocusInWindow();
    Object action = pane.getValue();
    if (action == null
        || !(action instanceof Integer)
        || ((Integer) action).intValue() != OptionPane.OK_OPTION) {
      return null;
    }

    return field.getText().trim();
  }

  private ProjectCircuitActions() {}
}
