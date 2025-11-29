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
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cburch.logisim.util.Debug;

class TestPanel extends JPanel implements ValueTable.Model {

  static final Color failColor = new Color(0xff9999);
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(TestPanel.class);
  
  private final TestFrame testFrame;
  private final ValueTable table;
  private final MyListener myListener = new MyListener();

  public TestPanel(TestFrame frame) {
    this.testFrame = frame;
    table = new ValueTable(getModel() == null ? null : this);
    setLayout(new BorderLayout());
    add(table);
    modelChanged(null, getModel());
  }

  @Override
  public void changeColumnValueRadix(int i) {
    if (i == 0 || i == 1) return; // Button and status columns don't support radix changes
    TestVector vec = getModel().getVector();
    int offset = 2; // Button column (0) + status column (1)
    // <set> and <seq> columns don't support radix changes
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
    int count = vec.columnName.length + 2; // +1 for status column, +1 for button column
    // Add columns for <set>, <seq>, and <iter> if they exist
    if (vec.setNumbers != null) count++;
    if (vec.seqNumbers != null) count++;
    if (vec.iterNumbers != null) count++;
    return count;
  }

  @Override
  public String getColumnName(int i) {
    TestVector vec = getModel().getVector();
    if (i == 0) return ""; // Button column - no header
    if (i == 1) return S.get("statusHeader");
    int offset = 2; // Button column (0) + status column (1)
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
    // Check if <iter> column exists
    if (vec.iterNumbers != null) {
      if (i == offset) return "<iter>";
      offset++;
    }
    // Regular pin columns
    return vec.columnName[i - offset];
  }

  @Override
  public int getColumnValueRadix(int i) {
    TestVector vec = getModel().getVector();
    if (i == 0) return 0; // Button column
      if (i == 1) return 0; // Status column
      int offset = 2; // Button column (0) + status column (1)
      // <set>, <seq>, and <iter> columns are always decimal
      if (vec.setNumbers != null) {
        if (i == offset) return 10;
        offset++;
      }
      if (vec.seqNumbers != null) {
        if (i == offset) return 10;
        offset++;
      }
      if (vec.iterNumbers != null) {
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
    if (i == 0) return null; // Button column
      if (i == 1) return null; // Status column
      int offset = 2; // Button column (0) + status column (1)
      // <set>, <seq>, and <iter> columns have no width (they're metadata)
      if (vec.setNumbers != null) {
        if (i == offset) return null;
        offset++;
      }
      if (vec.seqNumbers != null) {
        if (i == offset) return null;
        offset++;
      }
      if (vec.iterNumbers != null) {
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
    
    // Determine column offsets (accounting for button column at 0 and status column at 1)
    int setColumnOffset = vec.setNumbers != null ? 2 : -1;
    int seqColumnOffset = vec.seqNumbers != null ? (setColumnOffset >= 0 ? 3 : 2) : -1;
    int iterColumnOffset = vec.iterNumbers != null ? 
        (seqColumnOffset >= 0 ? seqColumnOffset + 1 : (setColumnOffset >= 0 ? 3 : 2)) : -1;
    int pinColumnStart = 2; // Button (0) + status (1)
    if (setColumnOffset >= 0) pinColumnStart++;
    if (seqColumnOffset >= 0) pinColumnStart++;
    if (iterColumnOffset >= 0) pinColumnStart++;

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

      // Button column (column 0)
      String buttonText = "Set";
      String buttonTooltip = "Set the pin values of this test";
      
      // Check if this is a sequential test with previous steps
      int buttonSeqValue = (vec.seqNumbers != null && row < vec.seqNumbers.length) ? vec.seqNumbers[row] : 0;
      int buttonSetValue = (vec.setNumbers != null && row < vec.setNumbers.length) ? vec.setNumbers[row] : 0;
      if (buttonSeqValue > 0) {
        // Check if there are other steps in the same set before this one
        boolean hasPreviousSteps = false;
        for (int checkRow = 0; checkRow < row; checkRow++) {
          int otherSet = (vec.setNumbers != null && checkRow < vec.setNumbers.length) ? vec.setNumbers[checkRow] : 0;
          int otherSeq = (vec.seqNumbers != null && checkRow < vec.seqNumbers.length) ? vec.seqNumbers[checkRow] : 0;
          if (otherSet == buttonSetValue && otherSeq > 0 && otherSeq < buttonSeqValue) {
            hasPreviousSteps = true;
            break;
          }
        }
        if (hasPreviousSteps) {
          buttonTooltip += " (Does not run previous steps)";
        }
      }
      // Use a light blue background to make buttons more visible and clickable
      Color buttonBg = new Color(0xE0E0FF); // Light blue
      rowData[i - firstRow][0] =
          new ValueTable.Cell(buttonText, buttonBg, null, buttonTooltip);
      
      // Status column (column 1)
      rowData[i - firstRow][1] =
          new ValueTable.Cell(status, rowmsg != null ? failColor : null, null, rowmsg);

      int colIndex = 2;
      
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

      // <iter> column
      if (iterColumnOffset >= 0) {
        int iterValue = vec.getIterations(row);
        String iterTooltip = "Propagation iterations: " + iterValue;
        rowData[i - firstRow][colIndex] =
            new ValueTable.Cell(Integer.toString(iterValue), null, null, iterTooltip);
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
    if (oldModel != null) oldModel.removeModelListener(myListener);
    if (newModel != null) newModel.addModelListener(myListener);
    table.setModel(newModel == null ? null : this);
  }
  
  @Override
  public boolean isButtonColumn(int col) {
    return col == 0; // Button column is always column 0
  }
  
  @Override
  public void handleButtonClick(int displayRow) {
    Model model = getModel();
    if (model == null) return;
    TestVector vec = model.getVector();
    if (vec == null) return;
    
    // Convert display row to file row index
    int fileRow = model.sortedIndex(displayRow);
    
    // Get sequence number for this row
    int seqValue = (vec.seqNumbers != null && fileRow < vec.seqNumbers.length) 
        ? vec.seqNumbers[fileRow] : 0;
    
    try {
      if (seqValue == 0) {
        // Combinational test - "Set" button
        executeSetButton(fileRow);
      } else {
        // Set button - just sets pin values
        executeGoButton(fileRow, seqValue);
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
  
  private void executeSetButton(int fileRow) throws TestException {
    Model model = getModel();
    TestVector vec = model.getVector();
    Project project = model.getProject();
    com.cburch.logisim.circuit.Circuit circuit = model.getCircuit();
    
    // Get pins
    com.cburch.logisim.instance.Instance[] pins = getPinsForVector(vec, circuit, project);
    if (pins == null) {
      throw new TestException("Could not match pins to test vector columns");
    }
    
    // Get set number for debug output
    int setNum = 0;
    if (vec.setNumbers != null && fileRow < vec.setNumbers.length) {
      setNum = vec.setNumbers[fileRow];
    }
    
    if (Debug.isLevel(Debug.Level.DEBUG)) {
      Debug.log(Debug.Level.DEBUG, "=== Starting combinational test (Set button) ===");
      Debug.log(Debug.Level.DEBUG, "Set: {}, Row: {}", setNum, fileRow);
      
      // Log inputs
      StringBuilder inputStr = new StringBuilder("Setting inputs: ");
      for (int j = 0; j < pins.length; j++) {
        if (com.cburch.logisim.std.wiring.Pin.FACTORY.isInputPin(pins[j])) {
          String pinName = vec.columnName[j];
          Value val = vec.data.get(fileRow)[j];
          if (vec.isFloating(fileRow, j)) {
            inputStr.append(pinName).append("=<float> ");
          } else {
            inputStr.append(pinName).append("=").append(val.toDisplayString()).append(" ");
          }
        }
      }
      Debug.log(Debug.Level.DEBUG, inputStr.toString());
    }
    
    // Execute test and capture result
    TestException result = null;
    try {
      // Reset circuit and apply test values (this also checks outputs)
      circuit.doTestVector(project, pins, vec.data.get(fileRow), true, vec, fileRow);
      
      if (Debug.isLevel(Debug.Level.DEBUG)) {
        // Log outputs after execution
        com.cburch.logisim.circuit.CircuitState state = project.getCircuitState();
        StringBuilder outputStr = new StringBuilder("Output result: ");
        for (int j = 0; j < pins.length; j++) {
          if (!com.cburch.logisim.std.wiring.Pin.FACTORY.isInputPin(pins[j])) {
            String pinName = vec.columnName[j];
            com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pins[j]);
            Value actualValue = com.cburch.logisim.std.wiring.Pin.FACTORY.getValue(pinState);
            Value expectedValue = vec.data.get(fileRow)[j];
            outputStr.append(pinName).append("=").append(actualValue.toDisplayString());
            if (vec.isDontCare(fileRow, j)) {
              outputStr.append("(<DC>) ");
            } else if (vec.isFloating(fileRow, j)) {
              outputStr.append("(<float>) ");
            } else {
              boolean matches = expectedValue.compatible(actualValue);
              outputStr.append(matches ? "(OK) " : "(FAIL) ");
            }
          }
        }
        Debug.log(Debug.Level.DEBUG, outputStr.toString());
        Debug.log(Debug.Level.DEBUG, "=== Final result: PASS ===");
      }
    } catch (TestException e) {
      result = e;
      if (Debug.isLevel(Debug.Level.DEBUG)) {
        Debug.log(Debug.Level.DEBUG, "=== Final result: FAIL - {} ===", e.getMessage());
      }
      throw e; // Re-throw to show error dialog
    } finally {
      // Update the result and refresh display
      model.updateResult(vec, fileRow, result);
      // The updateResult method will trigger fireTestResultsChanged() which calls table.dataChanged()
    }
  }
  
  private void executeGoButton(int targetFileRow, int targetSeq) throws TestException {
    // Simplified: Just set the pin values for this test row and mark them dirty
    // No propagation, no running previous steps - just set the values
    Model model = getModel();
    TestVector vec = model.getVector();
    Project project = model.getProject();
    com.cburch.logisim.circuit.Circuit circuit = model.getCircuit();
    
    // Get pins
    com.cburch.logisim.instance.Instance[] pins = getPinsForVector(vec, circuit, project);
    if (pins == null) {
      throw new TestException("Could not match pins to test vector columns");
    }
    
    // Just set the input values and mark them dirty
    com.cburch.logisim.circuit.CircuitState state = project.getCircuitState();
    
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
    
    // Trigger a repaint of the circuit canvas so the changes are visible immediately
    project.repaintCanvas();
    
    // Request a nudge to trigger propagation on the simulation thread
    // This ensures the circuit propagates the new input values
    com.cburch.logisim.circuit.Simulator sim = project.getSimulator();
    sim.nudge();
    
    // No result to update - we're just setting values, not running a test
    // The circuit will propagate naturally through the simulation thread
  }
  
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

  private class MyListener implements ModelListener {

    @Override
    public void testingChanged() {}

    @Override
    public void testResultsChanged(int numPass, int numFail) {
      table.dataChanged();
    }

    @Override
    public void vectorChanged() {
      table.modelChanged();
    }
  }
}
