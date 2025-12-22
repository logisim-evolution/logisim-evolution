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

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.FailException;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.ValueTable;
import com.cburch.logisim.proj.Project;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.InputEvent;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cburch.logisim.util.Debug;

class TestPanel extends JPanel implements ValueTable.Model, com.cburch.logisim.circuit.Simulator.Listener {

  static final Color failColor = new Color(0xff9999);
  static final Color activeButtonColor = new Color(0x99ff99); // Light green
  static final Color inactiveButtonColor = new Color(0xE0E0FF); // Light blue
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(TestPanel.class);
  
  private final TestFrame testFrame;
  private final ValueTable table;
  private final MyListener myListener = new MyListener();
  
  // Track which rows' Show buttons are currently active (green)
  private java.util.Set<Integer> activeShowRows = new java.util.HashSet<>();
  // Track which rows' Set buttons are currently active (green)
  private java.util.Set<Integer> activeSetRows = new java.util.HashSet<>();
  // Store the pin values when Set is clicked, to detect changes
  private com.cburch.logisim.data.Value[] storedPinValues = null;

  public TestPanel(TestFrame frame) {
    this.testFrame = frame;
    table = new ValueTable(getModel() == null ? null : this);
    setLayout(new BorderLayout());
    add(table);
    modelChanged(null, getModel());
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
    TestException[] results = model.getResults();
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
        if (err instanceof FailException failEx) {
          failed = true;
          for (final var e : failEx.getAll()) {
            var col = e.getColumn();
            msg[col] = S.get("expectedValueMessage", e.getExpected().toDisplayString(getColumnValueRadix(pinColumnStart + col)));
            altdata[col] = e.getComputed();
          }
        } else if (err != null) {
          failed = true;
          rowmsg = err.getMessage();
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
      }
    }
    if (newModel != null) {
      newModel.addModelListener(myListener);
      // Add simulator listener to new project
      if (newModel.getProject() != null) {
        newModel.getProject().getSimulator().addSimulatorListener(this);
      }
    }
    // Reset active rows when model changes
      activeShowRows.clear();
      activeSetRows.clear();
      storedPinValues = null;
      table.setModel(newModel == null ? null : this);
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
    int menuMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    boolean resetFirst = (modifiersEx & menuMask) == 0;
    
    try {
      if (col == 0) {
        // Show button clicked
        if (seqValue > 0) {
          // Sequential test - show values without executing
          executeShowButton(fileRow, seqValue, resetFirst);
        } else {
          // Combinational test - just show values
          executeShowButton(fileRow, 0, resetFirst);
        }
      } else if (col == 1) {
        // Set button clicked - execute the test
        executeGoButton(fileRow, seqValue, resetFirst);
      }
    } catch (TestException e) {
      // Show error dialog
      javax.swing.JOptionPane.showMessageDialog(
          this,
          e.getMessage(),
          "Test Execution Error",
          javax.swing.JOptionPane.ERROR_MESSAGE);
    }
  }
  
  private void executeShowButton(int targetFileRow, int targetSeq, boolean resetFirst) throws TestException {
    // "Show" button - just set pin values without executing/checking the test
    Model model = getModel();
    TestVector vec = model.getVector();
    Project project = model.getProject();
    com.cburch.logisim.circuit.Circuit circuit = model.getCircuit();
    com.cburch.logisim.circuit.CircuitState state = project.getCircuitState();

    // Get pins once
    com.cburch.logisim.instance.Instance[] pins = getPinsForVector(vec, circuit, project);
    if (pins == null) {
      throw new TestException("Could not match pins to test vector columns");
    }

    // Handle combinational tests (targetSeq == 0)
    if (targetSeq == 0) {
      // Combinational test - just set values for this single row
      if (resetFirst) {
        state.reset();
      }
      
      // Set input pin values
      for (int j = 0; j < pins.length; j++) {
        if (com.cburch.logisim.std.wiring.Pin.FACTORY.isInputPin(pins[j])) {
          com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pins[j]);
          com.cburch.logisim.data.Value driveValue = vec.data.get(targetFileRow)[j];
          if (vec.isFloating(targetFileRow, j)) {
            driveValue = com.cburch.logisim.data.Value.UNKNOWN;
          }
          com.cburch.logisim.std.wiring.Pin.FACTORY.driveInputPin(pinState, driveValue);
          state.markComponentAsDirty(pins[j].getComponent());
        }
      }
      
      // Propagate
      com.cburch.logisim.circuit.Propagator prop = state.getPropagator();
      prop.propagate();
      
      if (prop.isOscillating()) {
        throw new TestException("Oscillation detected");
      }
      
      // Store pin values
      storedPinValues = new com.cburch.logisim.data.Value[pins.length];
      for (int j = 0; j < pins.length; j++) {
        com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pins[j]);
        storedPinValues[j] = com.cburch.logisim.std.wiring.Pin.FACTORY.getValue(pinState);
      }
      
      // Mark this row as active for Show button (highlight all previous + this one)
      activeShowRows.clear();
      activeShowRows.add(targetFileRow);
      // Update Set button highlight to match current state
      activeSetRows.clear();
      activeSetRows.add(targetFileRow);
      capturedInitialState = false;
      
      table.dataChanged();
      table.repaint();
      project.repaintCanvas();
      project.getSimulator().nudge();
      return;
    }

    // Sequential test - Get the target set number
    int targetSet = 0;
    if (vec.setNumbers != null && targetFileRow < vec.setNumbers.length) {
      targetSet = vec.setNumbers[targetFileRow];
    }

    // Find all rows in the same set with sequence numbers from 1 to targetSeq (inclusive)
    java.util.ArrayList<Integer> stepsToExecute = new java.util.ArrayList<>();
    for (int row = 0; row < vec.data.size(); row++) {
      int rowSet = (vec.setNumbers != null && row < vec.setNumbers.length) ? vec.setNumbers[row] : 0;
      int rowSeq = (vec.seqNumbers != null && row < vec.seqNumbers.length) ? vec.seqNumbers[row] : 0;
      
      // Include rows with same set and sequence from 1 to targetSeq
      if (rowSet == targetSet && rowSeq > 0 && rowSeq <= targetSeq) {
        stepsToExecute.add(row);
      }
    }

    // Sort by sequence number to execute in order
    stepsToExecute.sort((a, b) -> {
      int seqA = (vec.seqNumbers != null && a < vec.seqNumbers.length) ? vec.seqNumbers[a] : 0;
      int seqB = (vec.seqNumbers != null && b < vec.seqNumbers.length) ? vec.seqNumbers[b] : 0;
      return Integer.compare(seqA, seqB);
    });

    // Reset circuit state before starting the sequence (if requested)
    if (resetFirst) {
      state.reset();
    }

    // Execute each step in sequence (just set values and propagate, don't check outputs)
    for (int stepRow : stepsToExecute) {
      // Set input pin values for this step
      for (int j = 0; j < pins.length; j++) {
        if (com.cburch.logisim.std.wiring.Pin.FACTORY.isInputPin(pins[j])) {
          com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pins[j]);
          com.cburch.logisim.data.Value driveValue = vec.data.get(stepRow)[j];
          if (vec.isFloating(stepRow, j)) {
            driveValue = com.cburch.logisim.data.Value.UNKNOWN;
          }
          com.cburch.logisim.std.wiring.Pin.FACTORY.driveInputPin(pinState, driveValue);
          // Mark the pin component as dirty so it gets processed during propagation
          state.markComponentAsDirty(pins[j].getComponent());
        }
      }

      // Propagate after setting values for this step
      com.cburch.logisim.circuit.Propagator prop = state.getPropagator();
      prop.propagate();
      
      if (prop.isOscillating()) {
        throw new TestException("Oscillation detected at sequence step " + 
            (vec.seqNumbers != null && stepRow < vec.seqNumbers.length ? vec.seqNumbers[stepRow] : 0));
      }
    }
    
    // Store the pin values after all steps are executed
    storedPinValues = new com.cburch.logisim.data.Value[pins.length];
    for (int j = 0; j < pins.length; j++) {
      com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pins[j]);
      storedPinValues[j] = com.cburch.logisim.std.wiring.Pin.FACTORY.getValue(pinState);
    }
    
    // Mark all executed steps as active for Show button (will show green buttons)
    activeShowRows.clear();
    activeShowRows.addAll(stepsToExecute);
    // Update Set button highlight to match current state (the last step shown)
    activeSetRows.clear();
    if (!stepsToExecute.isEmpty()) {
      activeSetRows.add(stepsToExecute.get(stepsToExecute.size() - 1));
    }
    capturedInitialState = false; // Reset flag so we capture state after first propagation
    
    // Refresh the table to show the green button immediately
    table.dataChanged();
    table.repaint();
    
    // Trigger a repaint of the circuit canvas so the changes are visible immediately
    project.repaintCanvas();

    // Request a nudge to make sure the sim thread resumes
    project.getSimulator().nudge();
  }
  
  private void executeGoButton(int targetFileRow, int targetSeq, boolean resetFirst) throws TestException {
    Model model = getModel();
    TestVector vec = model.getVector();
    Project project = model.getProject();
    com.cburch.logisim.circuit.Circuit circuit = model.getCircuit();
    com.cburch.logisim.circuit.CircuitState state = project.getCircuitState();

    // Get pins once
    com.cburch.logisim.instance.Instance[] pins = getPinsForVector(vec, circuit, project);
    if (pins == null) {
      throw new TestException("Could not match pins to test vector columns");
    }

    // Handle combinational tests (targetSeq == 0)
    if (targetSeq == 0) {
      // Combinational test - execute single test using doTestVector
      TestException result = null;
      try {
        // Reset circuit and apply test values (this also checks outputs)
        circuit.doTestVector(project, pins, vec.data.get(targetFileRow), resetFirst, vec, targetFileRow);
      } catch (TestException e) {
        result = e;
        throw e; // Re-throw to show error dialog
      } finally {
        // Mark only this combinational row as active for Set button (will show green button)
        activeShowRows.clear(); // Clear Show button highlights
        activeSetRows.clear();
        activeSetRows.add(targetFileRow);
        
        // Update the result and refresh display
        model.updateResult(vec, targetFileRow, result);
        // The updateResult method will trigger fireTestResultsChanged() which calls table.dataChanged()
      }
      return;
    }

    // Sequential test - Set button: only set this one step (don't run sequence)
    // Just set the pin values for this single row without running previous steps
    
    // Set input pin values for this step only
    for (int j = 0; j < pins.length; j++) {
      if (com.cburch.logisim.std.wiring.Pin.FACTORY.isInputPin(pins[j])) {
        com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pins[j]);
        com.cburch.logisim.data.Value driveValue = vec.data.get(targetFileRow)[j];
        if (vec.isFloating(targetFileRow, j)) {
          driveValue = com.cburch.logisim.data.Value.UNKNOWN;
        }
        com.cburch.logisim.std.wiring.Pin.FACTORY.driveInputPin(pinState, driveValue);
        // Mark the pin component as dirty so it gets processed during propagation
        state.markComponentAsDirty(pins[j].getComponent());
      }
    }

    // Propagate after setting values
    com.cburch.logisim.circuit.Propagator prop = state.getPropagator();
    prop.propagate();
    
    if (prop.isOscillating()) {
      throw new TestException("Oscillation detected at sequence step " + targetSeq);
    }
    
    // Store the pin values after setting
    storedPinValues = new com.cburch.logisim.data.Value[pins.length];
    for (int j = 0; j < pins.length; j++) {
      com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pins[j]);
      storedPinValues[j] = com.cburch.logisim.std.wiring.Pin.FACTORY.getValue(pinState);
    }
    
    // Mark only this row as active for Set button (will show green button)
    activeShowRows.clear(); // Clear Show button highlights
    activeSetRows.clear();
    activeSetRows.add(targetFileRow);
    capturedInitialState = false;
    
    // Refresh the table to show the green button immediately
    table.dataChanged();
    table.repaint();
    
    // Trigger a repaint of the circuit canvas so the changes are visible immediately
    project.repaintCanvas();

    // Request a nudge to make sure the sim thread resumes
    project.getSimulator().nudge();
  }
  
  // Track if we've captured the initial state after setting values
  private boolean capturedInitialState = false;
  
  @Override
  public void propagationCompleted(com.cburch.logisim.circuit.Simulator.Event e) {
    // Check if we have active rows and stored pin values
    if (activeSetRows.isEmpty() || storedPinValues == null) {
      capturedInitialState = false;
      return;
    }
    
    Model model = getModel();
    if (model == null) {
      capturedInitialState = false;
      return;
    }
    
    TestVector vec = model.getVector();
    Project project = model.getProject();
    com.cburch.logisim.circuit.Circuit circuit = model.getCircuit();
    
    // Get pins
    com.cburch.logisim.instance.Instance[] pins = getPinsForVector(vec, circuit, project);
    if (pins == null) {
      capturedInitialState = false;
      return;
    }
    
    // Get current pin values
    com.cburch.logisim.circuit.CircuitState state = project.getCircuitState();
    
    // On the first propagation after setting values, capture the stabilized state
    // This accounts for propagation effects (e.g., outputs changing due to inputs)
    if (!capturedInitialState) {
      for (int j = 0; j < pins.length; j++) {
        com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pins[j]);
        storedPinValues[j] = com.cburch.logisim.std.wiring.Pin.FACTORY.getValue(pinState);
      }
      capturedInitialState = true;
      return; // Don't check for changes on the first propagation
    }
    
    // After initial state is captured, check if any pin values have changed
    boolean valuesChanged = false;
    for (int j = 0; j < pins.length; j++) {
      com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pins[j]);
      com.cburch.logisim.data.Value currentValue = com.cburch.logisim.std.wiring.Pin.FACTORY.getValue(pinState);
      
      // Compare with stored value
      if (storedPinValues[j] == null || !currentValue.equals(storedPinValues[j])) {
        valuesChanged = true;
        break;
      }
    }
    
    // If values changed, reset the active rows
    if (valuesChanged) {
      activeShowRows.clear();
      activeSetRows.clear();
      storedPinValues = null;
      capturedInitialState = false;
      // Refresh the table to remove green buttons
      javax.swing.SwingUtilities.invokeLater(() -> {
        table.dataChanged();
      });
    }
  }
  
  @Override
  public void simulatorReset(com.cburch.logisim.circuit.Simulator.Event e) {
    // Reset active rows when simulator is reset
    activeShowRows.clear();
    activeSetRows.clear();
    storedPinValues = null;
    capturedInitialState = false;
    javax.swing.SwingUtilities.invokeLater(() -> {
      table.dataChanged();
    });
  }
  
  @Override
  public void simulatorStateChanged(com.cburch.logisim.circuit.Simulator.Event e) {
    // Reset active rows when simulator state changes
    activeSetRows.clear();
    storedPinValues = null;
    capturedInitialState = false;
    javax.swing.SwingUtilities.invokeLater(() -> {
      table.dataChanged();
    });
  }
  
  /* Get the pins for a given vector
   * @param vec The test vector
   * @param circuit The circuit
   * @param project The project
   * @return The pins for the given vector, or null if no pins are found
   */
  private com.cburch.logisim.instance.Instance[] getPinsForVector(
      TestVector vec, com.cburch.logisim.circuit.Circuit circuit, Project project) {
    int n = vec.columnName.length;
    com.cburch.logisim.instance.Instance[] pins = new com.cburch.logisim.instance.Instance[n];
    com.cburch.logisim.circuit.CircuitState state = 
        com.cburch.logisim.circuit.CircuitState.createRootState(project, circuit);
    
    for (int i = 0; i < n; i++) {
      String columnName = vec.columnName[i];
      for (com.cburch.logisim.comp.Component comp : circuit.getNonWires()) {
        if (!(comp.getFactory() instanceof com.cburch.logisim.std.wiring.Pin)) continue;
        com.cburch.logisim.instance.Instance inst = com.cburch.logisim.instance.Instance.getInstanceFor(comp);
        com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(comp);
        String label = pinState.getAttributeValue(com.cburch.logisim.instance.StdAttr.LABEL);
        if (label == null || !label.equals(columnName)) continue;
        if (com.cburch.logisim.std.wiring.Pin.FACTORY.getWidth(inst).getWidth() 
            != vec.columnWidth[i].getWidth()) continue;
        pins[i] = inst;
        break;
      }
      if (pins[i] == null) return null;
    }
    return pins;
  }

  /* Get the pins for a given vector and all previous rows in the sequence
   * @param vec The test vector
   * @param circuit The circuit
   * @param project The project
   * @param getAllPreviousRows If true, return pins for all previous rows in the sequence
   * @return The pins for the given vector and previous rows, or null if no pins are found
   */
  private com.cburch.logisim.instance.Instance[][] getPinsForVector(
      TestVector vec, com.cburch.logisim.circuit.Circuit circuit, Project project, boolean getAllPreviousRows) {
    // For now, return a single-element array with the pins
    // This can be extended later to return pins for all previous rows
    com.cburch.logisim.instance.Instance[] pins = getPinsForVector(vec, circuit, project);
    if (pins == null) return null;
    return new com.cburch.logisim.instance.Instance[][] { pins };
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
      // Clear active rows when vector changes (e.g., new file loaded)
      activeShowRows.clear();
      activeSetRows.clear();
      storedPinValues = null;
      capturedInitialState = false;
      table.modelChanged();
    }
  }
}
