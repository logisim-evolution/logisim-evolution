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

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TestVectorEvaluator {
  CircuitState state;
  TestVector vector;
  Instance[] pins;
  volatile java.util.ArrayList<Integer> stepsToExecute = null;
  volatile boolean allowReset;
  volatile boolean checkResults = false;
  boolean canceled = false;
  BiConsumer<Integer, FailException> lineReportAction;
  Consumer<Instance[]> callback;

  public TestVectorEvaluator(CircuitState state, TestVector vector, ArrayList<Integer> steps,
                             boolean allowReset, boolean checkResults, BiConsumer<Integer, FailException> lineReportAction,
                             Consumer<Instance[]> callback) throws TestException {
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

  public void setLineReportAction(BiConsumer<Integer, FailException> reportAction) {
    lineReportAction = reportAction;
  }

  public void evaluate() throws TestException {
    Propagator prop = state.getPropagator();
    int currentSet = -1; // Track current set (sequence ID)
    canceled = false;

    // Execute each step in sequence (just set values and propagate, don't check outputs)
    final var stepsToExecute = this.stepsToExecute;
    for (int stepRow : stepsToExecute) {
      if (canceled) {
        break;
      }

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
      }

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

      if (prop.isOscillating()) {
        throw new TestException("Oscillation detected at sequence step "
            + (vector.seqNumbers != null && stepRow < vector.seqNumbers.length ? vector.seqNumbers[stepRow] : 0));
      }

      if (checkResults && vector != null) {
        final var val = vector.data.get(stepRow);
        FailException err = null;

        for (var i = 0; i < pins.length; i++) {
          final var pinState = state.getInstanceState(pins[i]);
          if (Pin.FACTORY.isInputPin(pins[i])) {
            continue;
          }

          final var v = Pin.FACTORY.getValue(pinState);

          // Check for don't care - always pass
          if (stepRow >= 0 && vector.isDontCare(stepRow, i)) {
            continue; // Skip comparison for don't care values
          }

          // Check for floating - expect UNKNOWN
          if (stepRow >= 0 && vector.isFloating(stepRow, i)) {
            if (!Value.UNKNOWN.equals(v)) {
              if (err == null) {
                err = new FailException(i, pinState.getAttributeValue(StdAttr.LABEL), Value.UNKNOWN, v);
              } else {
                err.add(new FailException(i, pinState.getAttributeValue(StdAttr.LABEL), Value.UNKNOWN, v));
              }
            }
            continue;
          }

          // Normal value comparison
          if (!val[i].compatible(v)) {
            if (err == null) {
              err = new FailException(i, pinState.getAttributeValue(StdAttr.LABEL), val[i], v);
            } else {
              err.add(new FailException(i, pinState.getAttributeValue(StdAttr.LABEL), val[i], v));
            }
          }
        }

        if (lineReportAction != null) {
          lineReportAction.accept(stepRow, err);
        } else {
          if (err != null) {
            throw err;
          }
        }
      }
    }

    if (callback != null) {
      callback.accept(pins);
    }
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
