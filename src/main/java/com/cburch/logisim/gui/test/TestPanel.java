/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.test;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.TestVectorEvaluator;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.ValueTable;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPanel extends JPanel implements ValueTable.Model, Simulator.Listener {

  static final Color failColor = new Color(0xff9999);
  static final Color activeButtonColor = new Color(0x99ff99); // Light green
  static final Color inactiveButtonColor = new Color(0xE0E0FF); // Light blue
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(TestPanel.class);

  private final TestFrame testFrame;
  private final ValueTable table;
  private final MyListener myListener = new MyListener();
  ComponentAdapter componentAdapter = null;
  private boolean simulatorListening = false;

  // Track which rows' Show buttons are currently active (green)
  private final Set<Integer> activeShowRows = new HashSet<>();
  // Track which rows' Set buttons are currently active (green)
  private final Set<Integer> activeSetRows = new HashSet<>();
  private final String [] specialHeaders = {"", "", S.get("statusHeader"), "<set>", "<seq>"};

  // Store the pin values when Show or Set is clicked, to detect changes. Used by simulation thread.
  private com.cburch.logisim.data.Value[] storedPinValues = null;
  private TestVectorEvaluator propagationEvaluator = null;

  public TestPanel(TestFrame frame) {
    this.testFrame = frame;
    table = new ValueTable(getModel() == null ? null : this);
    setLayout(new BorderLayout());
    add(table);
    modelChanged(null, getModel());
    setComponentAdapter();
  }

  @Override
  public String specialColumnEntry(int i) {
    if (i < specialHeaders.length) return null; // special columns are handled separately.
    TestVector vec = getModel().getVector();
    return vec.specialColumnEntry(i - specialHeaders.length);
  }

  @Override
  public void changeColumnValueRadix(int i) {
    if (i < specialHeaders.length) return; // special columns have no radix.
    TestVector vec = getModel().getVector();
    // Regular pin columns
    int pinIndex = i - specialHeaders.length;
    switch (vec.columnRadix[pinIndex]) {
      case 2 -> vec.columnRadix[pinIndex] = 10;
      case 10 -> vec.columnRadix[pinIndex] = 16;
      default -> vec.columnRadix[pinIndex] = 2;
    }
    table.modelChanged();
  }

  @Override
  public int getColumnCount() {
    TestVector vec = getModel().getVector();
    return vec == null ? 0 : vec.columnName.length + specialHeaders.length;
  }

  @Override
  public String getColumnName(int i) {
    if (i < specialHeaders.length) return specialHeaders[i];
    TestVector vec = getModel().getVector();
    return vec.columnName[i - specialHeaders.length];
  }

  @Override
  public int getColumnValueRadix(int i) {
    if (i < 3) return 0; // first three have no radix.
    if (i < specialHeaders.length) return 10; // <set> and <seq> are displayed in decimal.
    TestVector vec = getModel().getVector();
    return vec.columnRadix[i - specialHeaders.length];
  }

  // ValueTable.Model implementation

  @Override
  public BitWidth getColumnValueWidth(int i) {
    if (i < specialHeaders.length) return null;
    TestVector vec = getModel().getVector();
    return vec.columnWidth[i - specialHeaders.length];
  }

  Model getModel() {
    return testFrame.getModel();
  }

  @Override
  public int getRowCount() {
    TestVector vec = getModel().getVector();
    return vec == null ? 0 : vec.data.size();
  }

  @Override
  public void getRowData(int firstRow, int numRows, ValueTable.Cell[][] rowData) {
    Model model = getModel();
    ArrayList<TestVectorEvaluator.LineReport>[] results = model.getResults();
    var numPass = model.getPass();
    var numFail = model.getFail();
    final var vec = model.getVector();
    int columns = vec.columnName.length;
    final var msg = new String[columns];
    final var altdata = new Value[columns];
    final var passMsg = S.get("passStatus");
    final var failMsg = S.get("failStatus");

    final int pinColumnStart = specialHeaders.length;

    for (var outRow = 0; outRow < numRows; outRow++) {
      final var row = model.sortedIndex(outRow + firstRow);
      final var data = vec.data.get(row);
      String rowmsg = null;
      String status = null;
      var failed = false;
      if (row < numPass + numFail) {
        final var err = results[row];
        if (err != null) { // err instanceof FailException failEx) {
          failed = true;
          for (final var e : err) {
            var col = e.column();
            msg[col] = S.get("expectedValueMessage", e.expected().toDisplayString(getColumnValueRadix(pinColumnStart + col)));
            altdata[col] = e.computed();
          }
        }
        status = failed ? failMsg : passMsg;
      }

      // Show button column (column 0)
      Color showButtonBg = activeShowRows.contains(row) ? activeButtonColor : inactiveButtonColor;
      rowData[outRow][0] = new ValueTable.Cell("Show", showButtonBg, null, S.get("toolTipShow"));

      // Set button column (column 1)
      Color setButtonBg = activeSetRows.contains(row) ? activeButtonColor : inactiveButtonColor;
      rowData[outRow][1] = new ValueTable.Cell("Set", setButtonBg, null, S.get("toolTipSet"));

      // Status column (column 2)
      rowData[outRow][2] = new ValueTable.Cell(status, rowmsg != null ? failColor : null, null, rowmsg);

      // <set> column (column 3)
      int setValue = vec.setNumbers[row];
      rowData[outRow][3] = new ValueTable.Cell(Integer.toString(setValue), null, null, "Set: " + setValue);

      // <seq> column (column 4)
      int seqValue = vec.seqNumbers[row];
      String seqText = seqValue == 0 ? "comb" : Integer.toString(seqValue);
      String seqTooltip = seqValue == 0 ? S.get("toolTipCombinational") : S.get("toolTipSequential", "" + seqValue);
      rowData[outRow][4] = new ValueTable.Cell(seqText, null, null, seqTooltip);

      // Pin columns
      for (var col = 0; col < columns; col++) {
        int colIndex = col + specialHeaders.length;
        String tooltip = msg[col];
        String displayText;
        Color bgColor = msg[col] != null ? failColor : null;

        // Check for special values first
        if (vec.isDontCare(row, col)) {
          displayText = "<DC>";
          tooltip = (tooltip != null ? tooltip + " | " : "") + S.get("toolTipDontCare", displayText);
        } else if (vec.isFloating(row, col)) {
          if (altdata[col] != null) {
            displayText = altdata[col].toDisplayString(getColumnValueRadix(pinColumnStart + col));
          } else {
            displayText = "<float>";
            tooltip = (tooltip != null ? tooltip + " | " : "") + S.get("toolTipFloating", displayText);
          }
        } else {
          // Regular value - show computed value if there's an error, otherwise show expected
          Value displayValue = altdata[col] != null ? altdata[col] : data[col];
          displayText = displayValue.toDisplayString(getColumnValueRadix(pinColumnStart + col));
        }

        rowData[outRow][colIndex] = new ValueTable.Cell(displayText, bgColor, null, tooltip);
        msg[col] = null;
        altdata[col] = null;
      }
    }
  }

  public void localeChanged() {
    table.modelChanged();
  }

  public void modelChanged(Model oldModel, Model newModel) {
    if (oldModel != null) {
      oldModel.removeModelListener(myListener);
      // Remove simulator listener from old project
      if (oldModel.getProject() != null) {
        oldModel.getProject().getSimulator().removeSimulatorListener(this);
        simulatorListening = false;
      }
    }
    if (newModel != null) {
      newModel.addModelListener(myListener);
      // Add simulator listener to new project
      if (newModel.getProject() != null && !simulatorListening && testFrame.isShowing()) {
        newModel.getProject().getSimulator().addSimulatorListener(this);
        simulatorListening = true;
      }
    }
    // Reset active rows when model changes
    resetActiveRows();
    table.setModel(newModel == null ? null : this);
  }

  private void setComponentAdapter() {
    if (getModel() != null && componentAdapter == null) {
      componentAdapter = new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent e) {
          if (getModel() != null && !simulatorListening) {
            getModel().getProject().getSimulator().addSimulatorListener(TestPanel.this);
            simulatorListening = true;
          }
        }

        @Override
        public void componentHidden(ComponentEvent e) {
          if (getModel() != null) {
            getModel().getProject().getSimulator().removeSimulatorListener(TestPanel.this);
            simulatorListening = false;
          }
        }
      };
      testFrame.addComponentListener(componentAdapter);
    }
  }

  private void resetActiveRows() {
    activeShowRows.clear();
    activeSetRows.clear();
    storedPinValues = null;
  }


  @Override
  public boolean isButtonColumn(int col) {
    return col == 0 || col == 1; // Show button (0) and Set button (1) are button columns
  }

  @Override
  public void handleButtonClick(int displayRow, int col, int modifiersEx) {
    Model model = getModel();
    if (model == null) return;
    if (model.getProject().getSimulator().isAutoTicking()) {
      JOptionPane.showMessageDialog(this,
          S.get("testButtonWhileTickingMessage"), S.get("testButtonWhileTickingTitle"),
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    TestVector vec = model.getVector();
    if (vec == null) return;

    // Convert display row to file row index
    int fileRow = model.sortedIndex(displayRow);

    // Get sequence number for this row
    int seqValue = (vec.seqNumbers != null && fileRow < vec.seqNumbers.length)
        ? vec.seqNumbers[fileRow] : 0;

    // Check if menu shortcut key (Ctrl/Cmd) is pressed - if so, don't reset
    int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    boolean resetFirst = (modifiersEx & menuMask) == 0;

    try {
      if (col == 0) {
        // Show button clicked
        executeShowButton(fileRow, seqValue, resetFirst);
      } else if (col == 1) {
        // Set button clicked - execute the test
        executeGoButton(fileRow, seqValue, resetFirst);
      }
    } catch (TestException e) {
      // Show error dialog
      JOptionPane.showMessageDialog(
          this,
          e.getMessage(), S.get("testExecutionErrorTitle"),
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void executeShowButton(int targetFileRow, int targetSeq, boolean resetFirst) throws TestException {
    Model model = getModel();
    TestVector vec = model.getVector();
    Project project = model.getProject();
    CircuitState state = project.getCircuitState();

    if (vec.setNumbers == null || targetFileRow >= vec.setNumbers.length) {
      return;
    }
    final var stepsToExecute = new ArrayList<Integer>();

    // Handle combinational tests (targetSeq == 0)
    if (targetSeq == 0) {
      stepsToExecute.add(targetFileRow);
    } else {
      // Sequential test - Get the target set number
      int targetSet = vec.setNumbers[targetFileRow];

      // Find all rows in the same set with sequence numbers from 1 to targetSeq (inclusive)
      for (int row = 0; row < vec.data.size(); row++) {
        int rowSet = vec.setNumbers[row];
        int rowSeq = vec.seqNumbers[row];

        // Include rows with same set and sequence from 1 to targetSeq
        if (rowSet == targetSet && rowSeq > 0 && rowSeq <= targetSeq) {
          stepsToExecute.add(row);
        }
      }
    }

    // Sort by sequence number to execute in order
    stepsToExecute.sort((a, b) -> {
      int seqA = vec.seqNumbers[a];
      int seqB = vec.seqNumbers[b];
      return Integer.compare(seqA, seqB);
    });
    // Mark all executed steps as active for Show button (will show green buttons)
    activeShowRows.clear();
    activeShowRows.addAll(stepsToExecute);
    // Update Set button highlight to match current state (the last step shown)
    activeSetRows.clear();
    if (!stepsToExecute.isEmpty()) {
      activeSetRows.add(stepsToExecute.get(stepsToExecute.size() - 1));
    }

    final var sim = project.getSimulator();
    TestVectorEvaluator evaluator = new TestVectorEvaluator(state, vec, stepsToExecute, tve -> {
      setStoredPinValues(state, tve);
      SwingUtilities.invokeLater(this::finishShowTestVector);
    });
    evaluator.setPropagateOnLast(sim.isAutoPropagating());
    sim.showTestVector(evaluator);
  }

  private void setStoredPinValues(CircuitState state, TestVectorEvaluator evaluator) {
    // Store the pin values after all steps are executed
    Instance[] pins = evaluator.getPins();
    final var storedPinValues = new Value[pins.length];
    for (int j = 0; j < pins.length; j++) {
      final var isPin = pins[j].getFactory() instanceof Pin;
      InstanceState instanceState = state.getInstanceState(pins[j]);
      storedPinValues[j] = isPin ? Pin.FACTORY.getValue(instanceState) : Clock.FACTORY.getValue(instanceState);
    }
    propagationEvaluator = evaluator;
    this.storedPinValues = storedPinValues;
  }

  private void finishShowTestVector() {
    // Refresh the table to show the green button
    table.dataChanged();
    table.repaint();
  }

  private void executeGoButton(int targetFileRow, int targetSeq, boolean resetFirst) throws TestException {
    Model model = getModel();
    TestVector vec = model.getVector();
    Project project = model.getProject();
    CircuitState state = project.getCircuitState();

    final var stepsToExecute = new ArrayList<Integer>();
    stepsToExecute.add(targetFileRow);
    final var simReset = targetSeq == 0 ? resetFirst : false;
    activeShowRows.clear(); // Clear Show button highlights
    activeSetRows.clear();
    activeSetRows.add(targetFileRow);

    final var sim = project.getSimulator();
    TestVectorEvaluator evaluator = new TestVectorEvaluator(state, vec, stepsToExecute, tve -> {
      setStoredPinValues(state, tve);
      SwingUtilities.invokeLater(this::finishShowTestVector);
    });
    evaluator.setAllowReset(simReset);
    evaluator.setPropagateOnLast(sim.isAutoPropagating());
    sim.showTestVector(evaluator);
  }

  @Override
  public void propagationCompleted(Simulator.Event e) {
    Model model = getModel();
    // Check if we have active rows and stored pin values
    if (model == null || activeSetRows.isEmpty() || storedPinValues == null || propagationEvaluator == null) {
      storedPinValues = null;
      propagationEvaluator = null;
      return;
    }
    CircuitState state = model.getProject().getCircuitState();
    Instance[] pins = propagationEvaluator.getPins();

    // Check if any pin values have changed
    for (int j = 0; j < pins.length; j++) {
      final var isPin = pins[j].getFactory() instanceof Pin;
      Value currentValue = null;
      try {
        InstanceState instanceState = state.getInstanceState(pins[j]);
        currentValue = isPin ? Pin.FACTORY.getValue(instanceState) : Clock.FACTORY.getValue(instanceState);
      } catch (Exception ex) {
        // state has been modified. Leave currentValue as null.
      }
      if (storedPinValues[j] == null || currentValue == null || !currentValue.equals(storedPinValues[j])) {
        storedPinValues = null;
        propagationEvaluator = null;
        resetActiveRowsAndNotifyTableChanged();
        break;
      }
    }
  }

  @Override
  public void simulatorReset(com.cburch.logisim.circuit.Simulator.Event e) {
    resetActiveRowsAndNotifyTableChanged();
  }

  @Override
  public void simulatorStateChanged(com.cburch.logisim.circuit.Simulator.Event e) {
    resetActiveRowsAndNotifyTableChanged();
  }

  private void resetActiveRowsAndNotifyTableChanged() {
    SwingUtilities.invokeLater(() -> {
      resetActiveRows();
      table.dataChanged();
    });
  }

  private class MyListener implements ModelListener {

    @Override
    public void testingChanged() {}

    @Override
    public void testResultsChanged(int numPass, int numFail) {
      table.dataChanged();
    }

    @Override
    public void vectorChanged() {
      resetActiveRows();
      table.modelChanged();
    }
  }
}
