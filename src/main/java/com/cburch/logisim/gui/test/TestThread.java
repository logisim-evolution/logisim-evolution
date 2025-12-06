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

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.FailException;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.UniquelyNamedThread;

public class TestThread extends UniquelyNamedThread implements CircuitListener {

  private final Project project;
  private final Circuit circuit;
  private final CircuitState circuitState;
  private final TestVector vector;
  private Instance[] pin;
  private Model model;
  private boolean canceled = false;
  private boolean paused = false;

  public TestThread(Model model) throws TestException {
    super("TestThread-Model");
    this.model = model;

    this.project = model.getProject();
    this.circuit = model.getCircuit();
    this.circuitState = project.getCircuitState().cloneAsNewRootState();
    this.circuitState.getPropagator().setPropagatorThread(this);
    this.vector = model.getVector();

    matchPins();

    model.getCircuit().addCircuitListener(this);
  }

  // used only for automated testing via command line arguments
  private TestThread(Project proj, Circuit circuit, TestVector vec) throws TestException {
    super("TestThread-Project");
    this.project = proj;
    this.circuit = circuit;
    this.circuitState = project.getCircuitState().cloneAsNewRootState();
    this.circuitState.getPropagator().setPropagatorThread(this);
    this.vector = vec;

    matchPins();
  }

  // used only for automated testing via command line arguments
  public static int doTestVector(Project proj, Circuit circuit, String vectorname) {
    System.out.println(S.get("testLoadingVector", vectorname));
    TestVector vec;
    try {
      vec = new TestVector(vectorname);
    } catch (Exception e) {
      System.err.println(S.get("testLoadingFailed", e.getMessage()));
      return -1;
    }

    TestThread tester;
    try {
      tester = new TestThread(proj, circuit, vec);
    } catch (TestException e) {
      System.err.println(S.get("testSetupFailed", e.getMessage()));
      return -1;
    }

    System.out.println(S.get("testRunning", Integer.toString(vec.data.size())));

    if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
      // Debug: Show all sequence numbers in file order
      com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
          "=== Test Vector Sequence Analysis ===");
      for (int i = 0; i < vec.data.size(); i++) {
        int setNum = (vec.setNumbers != null && i < vec.setNumbers.length) ? vec.setNumbers[i] : 0;
        int seqNum = (vec.seqNumbers != null && i < vec.seqNumbers.length) ? vec.seqNumbers[i] : 0;
        com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
            "Row {}: set={}, seq={}", i, setNum, seqNum);
      }
    }

    // Create sorted list of test indices: by set first, then sequence
    java.util.ArrayList<Integer> sortedIndices = new java.util.ArrayList<>();
    for (int i = 0; i < vec.data.size(); i++) {
      sortedIndices.add(i);
    }

    // Sort by set first, then by sequence
    sortedIndices.sort((a, b) -> {
      int setA = (vec.setNumbers != null && a < vec.setNumbers.length) ? vec.setNumbers[a] : 0;
      int setB = (vec.setNumbers != null && b < vec.setNumbers.length) ? vec.setNumbers[b] : 0;
      int seqA = (vec.seqNumbers != null && a < vec.seqNumbers.length) ? vec.seqNumbers[a] : 0;
      int seqB = (vec.seqNumbers != null && b < vec.seqNumbers.length) ? vec.seqNumbers[b] : 0;

      // First compare by set
      int setCompare = Integer.compare(setA, setB);
      if (setCompare != 0) return setCompare;

      // Then compare by sequence
      int seqCompare = Integer.compare(seqA, seqB);
      if (seqCompare != 0) return seqCompare;

      // If set and seq are the same, maintain original order (by index)
      return Integer.compare(a, b);
    });

    int numPass = 0;
    int numFail = 0;
    int currentSet = -1; // Track current set (sequence ID)
    boolean shouldReset = true;

    // Execute tests in sorted order
    for (int sortedIdx = 0; sortedIdx < sortedIndices.size(); sortedIdx++) {
      int i = sortedIndices.get(sortedIdx);
      try {
        System.out.print((sortedIdx + 1) + " \r");

        // Determine set number (sequence ID) for this test
        int testSet = 0;
        if (vec.setNumbers != null && i < vec.setNumbers.length) {
          testSet = vec.setNumbers[i];
        }

        // Determine sequence number (step number within set) for this test
        int testSeq = 0;
        if (vec.seqNumbers != null && i < vec.seqNumbers.length) {
          testSeq = vec.seqNumbers[i];
        }

        // Determine if we should reset
        // Reset if: starting a new set (sequence ID), or test is combinational (seq == 0)
        if (testSeq == 0 || testSet != currentSet) {
          shouldReset = true;
          currentSet = testSet;

          if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
            if (testSeq == 0) {
              com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
                  "=== Starting combinational test (Set {}, Seq {}) ===", testSet, testSeq);
            } else {
              // Count how many steps are in this set (sequence)
              int sequenceStepCount = 0;
              for (int j = sortedIdx; j < sortedIndices.size(); j++) {
                int rowIdx = sortedIndices.get(j);
                int rowSet = (vec.setNumbers != null && rowIdx < vec.setNumbers.length)
                    ? vec.setNumbers[rowIdx] : 0;
                int rowSeq = (vec.seqNumbers != null && rowIdx < vec.seqNumbers.length)
                    ? vec.seqNumbers[rowIdx] : 0;
                // Count steps in this set (skip seq=0 as they're combinational)
                if (rowSet == testSet && rowSeq != 0) {
                  sequenceStepCount++;
                } else if (rowSet != testSet) {
                  break; // End of this set
                }
              }
              com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
                  "=== Starting sequential test execution (Set {}) ===", testSet);
              com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
                  "Total steps in this sequence: {}", sequenceStepCount);
            }
          }
        } else {
          // Same set (sequence) - don't reset, preserve state
          shouldReset = false;

          if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
            com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
                "--- Continuing in Set {} (Seq {}, no reset) ---", testSet, testSeq);
          }
        }

        if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
          // Log inputs before execution
          StringBuilder inputStr = new StringBuilder("Running set ").append(testSet)
              .append(" seq ").append(testSeq)
              .append(" (test ").append(sortedIdx + 1).append(", row ").append(i)
              .append(") - Setting inputs: ");
          for (int j = 0; j < tester.pin.length; j++) {
            if (com.cburch.logisim.std.wiring.Pin.FACTORY.isInputPin(tester.pin[j])) {
              String pinName = vec.columnName[j];
              com.cburch.logisim.data.Value val = vec.data.get(i)[j];
              if (vec.isFloating(i, j)) {
                inputStr.append(pinName).append("=<float> ");
              } else {
                inputStr.append(pinName).append("=").append(val.toDisplayString()).append(" ");
              }
            }
          }
          com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG, inputStr.toString());
        }

        tester.test(i, shouldReset);

        if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
          // Log outputs after execution
          com.cburch.logisim.circuit.CircuitState state = proj.getCircuitState();
          StringBuilder outputStr = new StringBuilder("Output result: ");
          for (int j = 0; j < tester.pin.length; j++) {
            if (!com.cburch.logisim.std.wiring.Pin.FACTORY.isInputPin(tester.pin[j])) {
              String pinName = vec.columnName[j];
              com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(tester.pin[j]);
              com.cburch.logisim.data.Value actualValue = com.cburch.logisim.std.wiring.Pin.FACTORY.getValue(pinState);
              com.cburch.logisim.data.Value expectedValue = vec.data.get(i)[j];
              outputStr.append(pinName).append("=").append(actualValue.toDisplayString());
              if (vec.isDontCare(i, j)) {
                outputStr.append("(<DC>) ");
              } else if (vec.isFloating(i, j)) {
                outputStr.append("(<float>) ");
              } else {
                boolean matches = expectedValue.compatible(actualValue);
                outputStr.append(matches ? "(OK) " : "(FAIL) ");
              }
            }
          }
          com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG, outputStr.toString());
          com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG, "Test {}: PASS", sortedIdx + 1);
        }

        numPass++;
      } catch (FailException e) {
        System.out.println();
        if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
          com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
              "Test {}: FAIL - {}", sortedIdx + 1, e.getMessage());
        }
        System.err.println(S.get("testFailed", Integer.toString(sortedIdx + 1)));
        for (FailException e1 : e.getAll()) System.out.println("  " + e1.getMessage());
        numFail++;
      } catch (TestException e) {
        System.out.println();
        if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
          com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
              "Test {}: FAIL - {}", sortedIdx + 1, e.getMessage());
        }
        System.err.println(S.get("testFailed", (sortedIdx + 1) + " " + e.getMessage()));
        numFail++;
      }
    }
    System.out.println();
    System.out.println(S.get("testResults", Integer.toString(numPass), Integer.toString(numFail)));
    return 0;
  }

  public void cancel() {
    canceled = true;
  }

  @Override
  public void circuitChanged(CircuitEvent event) {
    int action = event.getAction();
    if (action == CircuitEvent.ACTION_SET_NAME) return;
    else model.clearResults();
  }

  void matchPins() throws TestException {
    int n = vector.columnName.length;
    pin = new Instance[n];
    CircuitState state = circuitState; //CircuitState.createRootState(this.project, this.circuit);

    for (int i = 0; i < n; i++) {
      String columnName = vector.columnName[i];
      for (Component comp : circuit.getNonWires()) {
        if (!(comp.getFactory() instanceof Pin)) continue;
        Instance inst = Instance.getInstanceFor(comp);
        InstanceState pinState = state.getInstanceState(comp);
        String label = pinState.getAttributeValue(StdAttr.LABEL);
        if (label == null || !label.equals(columnName)) continue;
        if (Pin.FACTORY.getWidth(inst).getWidth() != vector.columnWidth[i].getWidth())
          throw new TestException(
              "test vector column '"
                  + columnName
                  + "' has width "
                  + vector.columnWidth[i]
                  + ", but pin has width "
                  + Pin.FACTORY.getWidth(inst));
        pin[i] = inst;
        break;
      }
      if (pin[i] == null)
        throw new TestException("test vector column '" + columnName + "' has no matching pin");
    }
  }

  @Override
  public void run() {
    try {
      executeSequentialTests();
    } finally {
      model.stop();
    }
  }

  private void executeSequentialTests() {
    // Create sorted list of test indices: by set first, then sequence
    java.util.ArrayList<Integer> sortedIndices = new java.util.ArrayList<>();
    for (int i = 0; i < vector.data.size(); i++) {
      sortedIndices.add(i);
    }

    // Sort by set first, then by sequence
    sortedIndices.sort((a, b) -> {
      int setA = (vector.setNumbers != null && a < vector.setNumbers.length) ? vector.setNumbers[a] : 0;
      int setB = (vector.setNumbers != null && b < vector.setNumbers.length) ? vector.setNumbers[b] : 0;
      int seqA = (vector.seqNumbers != null && a < vector.seqNumbers.length) ? vector.seqNumbers[a] : 0;
      int seqB = (vector.seqNumbers != null && b < vector.seqNumbers.length) ? vector.seqNumbers[b] : 0;

      // First compare by set
      int setCompare = Integer.compare(setA, setB);
      if (setCompare != 0) return setCompare;

      // Then compare by sequence
      int seqCompare = Integer.compare(seqA, seqB);
      if (seqCompare != 0) return seqCompare;

      // If set and seq are the same, maintain original order (by index)
      return Integer.compare(a, b);
    });

    int currentSet = -1; // Track current set (sequence ID)
    boolean shouldReset = true;

    // Execute tests in sorted order
    for (int sortedIdx = 0; sortedIdx < sortedIndices.size() && !canceled; sortedIdx++) {
      int i = sortedIndices.get(sortedIdx);

      while (paused) {
        if (canceled) return;
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
      }

      // Determine set number (sequence ID) for this test
      int testSet = 0;
      if (vector.setNumbers != null && i < vector.setNumbers.length) {
        testSet = vector.setNumbers[i];
      }

      // Determine sequence number (step number within set) for this test
      int testSeq = 0;
      if (vector.seqNumbers != null && i < vector.seqNumbers.length) {
        testSeq = vector.seqNumbers[i];
      }

      // Determine if we should reset
      // Reset if: starting a new set (sequence ID), or test is combinational (seq == 0)
      if (testSeq == 0 || testSet != currentSet) {
        shouldReset = true;
        currentSet = testSet;

        if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
          if (testSeq == 0) {
            com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
                "=== Starting combinational test (Set {}, Seq {}) ===", testSet, testSeq);
          } else {
            com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
                "=== Starting sequential test execution (Set {}) ===", testSet);
          }
        }
      } else {
        // Same set (sequence) - don't reset, preserve state
        shouldReset = false;

        if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
          com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
              "--- Continuing in Set {} (Seq {}, no reset) ---", testSet, testSeq);
        }
      }

      if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
        // Log inputs before execution
        StringBuilder inputStr = new StringBuilder("Running set ").append(testSet)
            .append(" seq ").append(testSeq)
            .append(" (test ").append(sortedIdx + 1).append(", row ").append(i)
            .append(") - Setting inputs: ");
        for (int j = 0; j < pin.length; j++) {
          if (com.cburch.logisim.std.wiring.Pin.FACTORY.isInputPin(pin[j])) {
            String pinName = vector.columnName[j];
            com.cburch.logisim.data.Value val = vector.data.get(i)[j];
            if (vector.isFloating(i, j)) {
              inputStr.append(pinName).append("=<float> ");
            } else {
              inputStr.append(pinName).append("=").append(val.toDisplayString()).append(" ");
            }
          }
        }
        com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG, inputStr.toString());
      }

      try {
        test(i, shouldReset);

        if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
          // Log outputs after execution
          com.cburch.logisim.circuit.CircuitState state = circuitState;
          StringBuilder outputStr = new StringBuilder("Output result: ");
          for (int j = 0; j < pin.length; j++) {
            if (!com.cburch.logisim.std.wiring.Pin.FACTORY.isInputPin(pin[j])) {
              String pinName = vector.columnName[j];
              com.cburch.logisim.instance.InstanceState pinState = state.getInstanceState(pin[j]);
              com.cburch.logisim.data.Value actualValue = com.cburch.logisim.std.wiring.Pin.FACTORY.getValue(pinState);
              com.cburch.logisim.data.Value expectedValue = vector.data.get(i)[j];
              outputStr.append(pinName).append("=").append(actualValue.toDisplayString());
              if (vector.isDontCare(i, j)) {
                outputStr.append("(<DC>) ");
              } else if (vector.isFloating(i, j)) {
                outputStr.append("(<float>) ");
              } else {
                boolean matches = expectedValue.compatible(actualValue);
                outputStr.append(matches ? "(OK) " : "(FAIL) ");
              }
            }
          }
          com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG, outputStr.toString());
          com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG, "Test {}: PASS", sortedIdx + 1);
        }

        canceled = canceled || !model.setResult(vector, i, null);
      } catch (TestException e) {
        if (com.cburch.logisim.util.Debug.isLevel(com.cburch.logisim.util.Debug.Level.DEBUG)) {
          com.cburch.logisim.util.Debug.log(com.cburch.logisim.util.Debug.Level.DEBUG,
              "Test {}: FAIL - {}", sortedIdx + 1, e.getMessage());
        }
        canceled = canceled || !model.setResult(vector, i, e);
      }
      Thread.yield();
    }
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  private void test(int idx, boolean resetState) throws TestException {
    circuit.doTestVector(circuitState, pin, vector.data.get(idx), resetState, vector, idx);
  }
}
