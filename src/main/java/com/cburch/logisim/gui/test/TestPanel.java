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
  public void changeColumnValueRadix(int i) {
    if (i == 0 || i == 1 || i == 2) return; // Show button, Set button, and status columns don't support radix changes
    TestVector vec = getModel().getVector();
    int offset = 3; // all excluded columns: Show button column + Set button column + status column
    // <set>, <seq> columns don't support radix changes
    if (vec.setNumbers != null) {
      if (i == offset) return;
      offset++;
    }
    if (vec.seqNumbers != null) {
      if (i == offset) return;
      offset++;
    }
    // Regular pin columns
    int pinIndex = i - offset;
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
    if (vec == null) return 0;
    int count = vec.columnName.length + 3; // +1 for status column, +2 for Show and Set button columns
    // Add columns for <set>, <seq> if they exist
    if (vec.setNumbers != null) count++;
    if (vec.seqNumbers != null) count++;
    return count;
  }

  @Override
  public String getColumnName(int i) {
    TestVector vec = getModel().getVector();
    if (i == 0) return ""; // Show button column - no header
    if (i == 1) return ""; // Set button column - no header
    if (i == 2) return S.get("statusHeader");
    int offset = 3; // Show button (0) + Set button (1) + status column (2)
    // Check if <set> column exists
    if (vec.setNumbers != null) {
      if (i == offset) return "<set>";
      offset++;
    }
    // Check if <seq> column exists
    if (vec.seqNumbers != null) {
      if (i == offset) return "<seq>";
      offset++;
    }
    // Regular pin columns
    return vec.columnName[i - offset];
  }

  @Override
  public int getColumnValueRadix(int i) {
    TestVector vec = getModel().getVector();
    if (i == 0) return 0; // Show button column
    if (i == 1) return 0; // Set button column
    if (i == 2) return 0; // Status column
    int offset = 3; // Show button (0) + Set button (1) + status column (2)
    // <set>, <seq> columns are always decimal
    if (vec.setNumbers != null) {
      if (i == offset) return 10;
      offset++;
    }
    if (vec.seqNumbers != null) {
      if (i == offset) return 10;
      offset++;
    }
    // Regular pin columns
    return vec.columnRadix[i - offset];
  }

  // ValueTable.Model implementation

  @Override
  public BitWidth getColumnValueWidth(int i) {
    TestVector vec = getModel().getVector();
    if (i == 0) return null; // Show button column
    if (i == 1) return null; // Set button column
    if (i == 2) return null; // Status column
    int offset = 3; // Show button (0) + Set button (1) + status column (2)
    // <set>, <seq> columns have no width (they're metadata)
    if (vec.setNumbers != null) {
      if (i == offset) return null;
      offset++;
    }
    if (vec.seqNumbers != null) {
      if (i == offset) return null;
      offset++;
    }
    // Regular pin columns
    return vec.columnWidth[i - offset];
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

    // Determine column offsets (accounting for Show button (0), Set button (1), and status column (2))
    int setColumnOffset = vec.setNumbers != null ? 3 : -1;
    int seqColumnOffset = vec.seqNumbers != null
        ? (setColumnOffset >= 0 ? 4 : 3) : -1;
    int pinColumnStart = 3; // Show button (0) + Set button (1) + status (2)
    if (setColumnOffset >= 0) pinColumnStart++;
    if (seqColumnOffset >= 0) pinColumnStart++;


    for (var i = firstRow; i < firstRow + numRows; i++) {
      final var row = model.sortedIndex(i);
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
      Color showButtonBg = activeShowRows.contains(row)
          ? activeButtonColor
          : inactiveButtonColor;
      rowData[i - firstRow][0] =
          new ValueTable.Cell("Show", showButtonBg, null, "Show the pin values for this test step");

      // Set button column (column 1)
      Color setButtonBg = activeSetRows.contains(row)
          ? activeButtonColor
          : inactiveButtonColor;
      rowData[i - firstRow][1] =
          new ValueTable.Cell("Set", setButtonBg, null, "Set the pin values of this test");

      // Status column (column 2)
      rowData[i - firstRow][2] =
          new ValueTable.Cell(status, rowmsg != null ? failColor : null, null, rowmsg);

      int colIndex = 3;

      // <set> column
      if (setColumnOffset >= 0) {
        int setValue = vec.setNumbers[row];
        rowData[i - firstRow][colIndex] =
            new ValueTable.Cell(Integer.toString(setValue), null, null, "Set: " + setValue);
        colIndex++;
      }

      // <seq> column
      if (seqColumnOffset >= 0) {
        int seqValue = vec.seqNumbers[row];
        String seqText = seqValue == 0 ? "comb" : Integer.toString(seqValue);
        String seqTooltip = seqValue == 0 ? "Combinational test (circuit reset)" : "Sequential test #" + seqValue;
        rowData[i - firstRow][colIndex] =
            new ValueTable.Cell(seqText, null, null, seqTooltip);
        colIndex++;
      }

      // Pin columns
      for (var col = 0; col < columns; col++) {
        String tooltip = msg[col];
        String displayText;
        Color bgColor = msg[col] != null ? failColor : null;

        // Check for special values first
        if (vec.isDontCare(row, col)) {
          displayText = "<DC>";
          tooltip = (tooltip != null ? tooltip + " | " : "") + "Don't Care (<DC>)";
        } else if (vec.isFloating(row, col)) {
          displayText = "<float>";
          tooltip = (tooltip != null ? tooltip + " | " : "") + "Floating (<float>)";
        } else {
          // Regular value - show computed value if there's an error, otherwise show expected
          Value displayValue = altdata[col] != null ? altdata[col] : data[col];
          displayText = displayValue.toDisplayString(getColumnValueRadix(pinColumnStart + col));
        }

        rowData[i - firstRow][colIndex] =
            new ValueTable.Cell(
                displayText,
                bgColor,
                null,
                tooltip);
        msg[col] = null;
        altdata[col] = null;
        colIndex++;
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
          e.getMessage(),
          "Test Execution Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void executeShowButton(int targetFileRow, int targetSeq, boolean resetFirst) throws TestException {
    Model model = getModel();
    TestVector vec = model.getVector();
    Project project = model.getProject();
    CircuitState state = project.getCircuitState();

    final var stepsToExecute = new ArrayList<Integer>();

    // Handle combinational tests (targetSeq == 0)
    if (targetSeq == 0) {
      stepsToExecute.add(targetFileRow);
    } else {
      // Sequential test - Get the target set number
      int targetSet = 0;
      if (vec.setNumbers != null && targetFileRow < vec.setNumbers.length) {
        targetSet = vec.setNumbers[targetFileRow];
      }

      // Find all rows in the same set with sequence numbers from 1 to targetSeq (inclusive)
      for (int row = 0; row < vec.data.size(); row++) {
        int rowSet = (vec.setNumbers != null && row < vec.setNumbers.length) ? vec.setNumbers[row] : 0;
        int rowSeq = (vec.seqNumbers != null && row < vec.seqNumbers.length) ? vec.seqNumbers[row] : 0;

        // Include rows with same set and sequence from 1 to targetSeq
        if (rowSet == targetSet && rowSeq > 0 && rowSeq <= targetSeq) {
          stepsToExecute.add(row);
        }
      }
    }

    // Sort by sequence number to execute in order
    stepsToExecute.sort((a, b) -> {
      int seqA = (vec.seqNumbers != null && a < vec.seqNumbers.length) ? vec.seqNumbers[a] : 0;
      int seqB = (vec.seqNumbers != null && b < vec.seqNumbers.length) ? vec.seqNumbers[b] : 0;
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
