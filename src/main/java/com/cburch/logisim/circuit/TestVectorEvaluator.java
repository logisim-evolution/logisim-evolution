package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.FailException;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TestVectorEvaluator {
  final CircuitState state;
  final TestVector vector;
  final Instance[] pins;
  volatile java.util.ArrayList<Integer> stepsToExecute = null;
  volatile boolean allowReset;
  volatile boolean checkResults = false;
  boolean canceled = false;
  BiConsumer<Integer, ArrayList<LineReport>> lineReportAction;
  Consumer<Instance[]> callback;

  public record LineReport(int column, String columnName, Value expected, Value computed, boolean oscillating) {

    @Override
    public @NotNull String toString() {
      return columnName + " = " + computed.toDisplayString(2) + " (expected " + expected.toDisplayString(2) + ")"
          + (oscillating ? " oscillating" : "");
    }
  }

  public TestVectorEvaluator(CircuitState state, TestVector vector, ArrayList<Integer> steps,
                             boolean allowReset, boolean checkResults, BiConsumer<Integer, ArrayList<LineReport>> lineReportAction,
                             Consumer<Instance[]> callback) throws TestException {
    if (state == null || vector == null) {
      throw new TestException("TestVectorEvaluation requires non-null state and vector.");
    }
    this.state = state;
    this.vector = vector;
    this.stepsToExecute = steps;
    this.allowReset = allowReset;
    this.checkResults = checkResults;
    this.lineReportAction = lineReportAction;
    this.callback = callback;
    pins = getPinsForVector(vector, state);
  }

  public TestVectorEvaluator(CircuitState state, TestVector vector, ArrayList<Integer> steps,
                             Consumer<Instance[]> callback) throws TestException {
    this(state, vector, steps, true, false, null, callback);
  }

  public TestVectorEvaluator(CircuitState state, TestVector vector, ArrayList<Integer> steps) throws TestException {
    this(state, vector, steps, true, false, null, null);
  }

  public TestVectorEvaluator(CircuitState state, TestVector vector) throws TestException {
    this(state, vector, null, true, false, null, null);
  }

  public Instance[] getPins() {
    return pins;
  }

  public void setSteps(ArrayList<Integer> steps) {
    this.stepsToExecute = steps;
  }

  public void setAllowReset(boolean reset) {
    allowReset = reset;
  }

  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  public void setCheckResults(boolean check) {
    checkResults = check;
  }

  public void setLineReportAction(BiConsumer<Integer, ArrayList<LineReport>> reportAction) {
    lineReportAction = reportAction;
  }

  public int evaluate() {
    Propagator prop = state.getPropagator();
    int numFails = 0;
    int currentSet = -1; // Track current set (sequence ID)
    canceled = false;

    final var stepsToExecute = this.stepsToExecute;
    for (int stepRow : stepsToExecute) {
      if (canceled) {
        break;
      }
      if (stepRow < 0 || stepRow >= vector.data.size()) continue; // shouldn't happen.

      // Determine set number (sequence ID) for this test
      int testSet = 0;
      if (vector.setNumbers != null && stepRow < vector.setNumbers.length) {
        testSet = vector.setNumbers[stepRow];
      }

      // Determine sequence number (step number within set) for this test
      int testSeq = 0;
      if (vector.seqNumbers != null && stepRow < vector.seqNumbers.length) {
        testSeq = vector.seqNumbers[stepRow];
      }

      // Determine if we should reset
      // Reset if: starting a new set (sequence ID), or test is combinational (seq == 0)
      boolean shouldReset = (testSeq == 0 || testSet != currentSet);
      if (shouldReset) {
        currentSet = testSet;
      }

      // Reset circuit state before starting the sequence (if requested)
      if (allowReset && shouldReset) {
        prop.reset();
        prop.propagate();
      }

      if (!prop.isOscillating()) {
        // Set input pin values for this step
        for (int j = 0; j < pins.length; j++) {
          if (Pin.FACTORY.isInputPin(pins[j])) {
            InstanceState pinState = state.getInstanceState(pins[j]);
            Value driveValue = vector.data.get(stepRow)[j];
            if (vector.isFloating(stepRow, j)) {
              driveValue = com.cburch.logisim.data.Value.UNKNOWN;
            }
            Pin.FACTORY.driveInputPin(pinState, driveValue);
            // Mark the pin component as dirty so it gets processed during propagation
            state.markComponentAsDirty(pins[j].getComponent());
          }
        }

        // Propagate after setting values for this step
        prop.propagate();
      }

      if (checkResults) {
        final var val = vector.data.get(stepRow);
        ArrayList<LineReport> report = new ArrayList<LineReport>();

        // Compare pins with expected values
        for (var i = 0; i < pins.length; i++) {
          final var pinState = state.getInstanceState(pins[i]);
          if (!Pin.FACTORY.isInputPin(pins[i])) { // Don't look at input pins
            final var v = Pin.FACTORY.getValue(pinState);
            if (!vector.isDontCare(stepRow, i)) { // Skip comparison for don't care values
              if (prop.isOscillating()) { // Report oscillating circuit outputs as ERROR.
                report.add(new LineReport(i, pinState.getAttributeValue(StdAttr.LABEL), val[i], Value.ERROR, true));
              } else if (vector.isFloating(stepRow, i)) { // Check for floating - expect UNKNOWN
                if (!v.isUnknown()) {
                  report.add(new LineReport(i, pinState.getAttributeValue(StdAttr.LABEL), Value.UNKNOWN, v, false));
                }
              } else if (!val[i].compatible(v)) { // Normal value comparison
                report.add(new LineReport(i, pinState.getAttributeValue(StdAttr.LABEL), val[i], v, false));
              }
            }
          }
        }

        if (!report.isEmpty()) {
          numFails++;
        }
        if (lineReportAction != null) {
          lineReportAction.accept(stepRow, report);
        }
      }
    }
    if (callback != null) {
      callback.accept(pins);
    }
    return numFails;
  }

  public static Instance[] getPinsForVector(TestVector vec, CircuitState state) throws TestException {
    int n = vec.columnName.length;
    Instance[] pins = new Instance[n];
    for (int i = 0; i < n; i++) {
      String columnName = vec.columnName[i];
      for (Component comp : state.getCircuit().getNonWires()) {
        if (comp.getFactory() instanceof Pin) { //continue;
          Instance inst = Instance.getInstanceFor(comp);
          InstanceState pinState = state.getInstanceState(comp);
          String label = pinState.getAttributeValue(StdAttr.LABEL);
          if (label != null && label.equals(columnName)) { // continue;
            if (Pin.FACTORY.getWidth(inst).getWidth() == vec.columnWidth[i].getWidth()) { // continue;
              pins[i] = inst;
              break;
            } else {
              throw new TestException(
                  "test vector column '"
                      + columnName
                      + "' has width "
                      + vec.columnWidth[i]
                      + ", but pin has width "
                      + Pin.FACTORY.getWidth(inst));
            }
          }
        }
      }
      if (pins[i] == null) {
        throw new TestException("test vector column '" + columnName + "' has no matching pin");
      }
    }
    return pins;
  }
}
