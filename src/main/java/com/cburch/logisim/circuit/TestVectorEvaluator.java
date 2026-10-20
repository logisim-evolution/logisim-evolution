package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.cburch.logisim.gui.Strings.S;

/** Evaluates the given TestVector using the requested steps on the given root CircuitState, state. */
public class TestVectorEvaluator {
  final CircuitState state;
  final TestVector vector;
  final Instance[] pins;
  ArrayList<Integer> stepsToExecute = null;
  boolean allowReset = true;
  boolean propagateOnLast = true;
  boolean canceled = false;
  Consumer<TestVectorEvaluator> callback; // final callback with this evaluator

  public record LineReport(int column, String columnName, Value expected, Value computed, boolean oscillating) {
    @Override
    public String toString() {
      return String.format("%s = %s (%s %s)%s", columnName, computed.toDisplayString(2), S.get("tveExpected"),
          expected.toDisplayString(2), (oscillating ? (" " + S.get("tveOscillating")) : "")
      );
    }
  }

  public TestVectorEvaluator(CircuitState state, TestVector vector, ArrayList<Integer> steps,
                             Consumer<TestVectorEvaluator> callback) throws TestException {
    if (state == null || vector == null) {
      throw new TestException(S.get("tveRequiresNonNull"));
    }
    this.state = state;
    this.vector = vector;
    this.stepsToExecute = steps == null ? buildSortedIndices() : steps;
    this.callback = callback;
    pins = getPinsForVector(vector, state);
  }

  public TestVectorEvaluator(CircuitState state, TestVector vector, ArrayList<Integer> steps) throws TestException {
    this(state, vector, steps, null);
  }

  public TestVectorEvaluator(CircuitState state, TestVector vector) throws TestException {
    this(state, vector, null, null);
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

  public void setPropagateOnLast(boolean propagateOnLast) {
    this.propagateOnLast = propagateOnLast;
  }

  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  public ArrayList<Integer> buildSortedIndices() {
    ArrayList<Integer> sortedIndices = new ArrayList<>();
    for (int i = 0; i < vector.data.size(); i++) {
      sortedIndices.add(i);
    }
    return sortedIndices;
  }

  public int[] evaluate() {
    return evaluate(stepsToExecute, null);
  }

  public int[] evaluate(BiConsumer<Integer, ArrayList<LineReport>> lineReportAction) {
    return evaluate(stepsToExecute, lineReportAction);
  }

  /**
   * Evaluates the given steps. If callback is not null, call it with the pins before returning.
   *
   * @param stepsToDo an ArrayList of the steps to process.
   * @param lineReportAction if not null, call action for each step with an ArrayList of LineReport.
   * @return a 2 element matrix with the number of passing and number of failing lines.
   */
  public int[] evaluate(ArrayList<Integer> stepsToDo, BiConsumer<Integer, ArrayList<LineReport>> lineReportAction) {
    Propagator prop = state.getPropagator();
    int numPass = 0;
    int numFails = 0;
    int currentSet = -1; // Track current set.
    int currentSeq = 0; // Track current sequence.
    canceled = false;

    for (int stepRow : stepsToDo) {
      if (canceled) {
        break;
      }
      if (stepRow < 0 || stepRow >= vector.data.size()) continue; // shouldn't happen.

      // Reset if: starting a new set or test is combinational (seq == 0)
      final int testSet = vector.setNumbers[stepRow];
      final int testSeq = vector.seqNumbers[stepRow];
      final boolean shouldReset = (testSeq == 0 || currentSeq == 0 || testSet != currentSet);
      currentSet = testSet;
      currentSeq = testSeq;

      // Reset circuit state before starting the sequence (if requested)
      if (allowReset && shouldReset) {
        prop.reset();
        prop.propagate();
      }

      if (!shouldReset && !prop.isOscillating()) {
        prop.toggleClocks();
        prop.step(null); // make sure clock signal reaches wires before setting pins.
      }

      if (!prop.isOscillating()) {
        // Set input pin values for this step
        for (int j = 0; j < pins.length; j++) {
          if (pins[j].getFactory() instanceof Pin && Pin.FACTORY.isInputPin(pins[j])) {
            InstanceState pinState = state.getInstanceState(pins[j]);
            final var oldValue = Pin.FACTORY.getValue(pinState);
            Value driveValue = vector.data.get(stepRow)[j];
            if (!driveValue.equals(oldValue)) {
              Pin.FACTORY.driveInputPin(pinState, driveValue);
              // Mark the pin component as dirty so it gets processed during propagation
              state.markComponentAsDirty(pins[j].getComponent());
            }
          }
        }

        // Propagate after setting values for this step
        if (stepRow != stepsToDo.getLast() || propagateOnLast) {
          prop.propagate();
        }
      }

      if (lineReportAction != null) {
        final var val = vector.data.get(stepRow);
        ArrayList<LineReport> report = new ArrayList<LineReport>();

        // Compare pins with expected values
        for (var i = 0; i < pins.length; i++) {
          final var isClock = pins[i].getFactory() instanceof Clock;
          final var isInputPin = pins[i].getFactory() instanceof Pin && Pin.FACTORY.isInputPin(pins[i]);
          if (isClock || !isInputPin) {
            final var pinState = state.getInstanceState(pins[i]);
            Value v = isClock ? Clock.FACTORY.getValue(pinState) : Pin.FACTORY.getValue(pinState);
            if (!vector.isDontCare(stepRow, i)) { // Skip comparison for don't care values
              if (prop.isOscillating()) { // Report oscillating circuit outputs as ERROR.
                report.add(new LineReport(i, vector.columnName[i], val[i], Value.ERROR, true));
              } else if (vector.isFloating(stepRow, i)) { // Check for floating - expect UNKNOWN
                if (!v.isUnknown()) {
                  report.add(new LineReport(i, vector.columnName[i], val[i], v, false));
                }
              } else if (!val[i].compatible(v)) { // Normal value comparison
                report.add(new LineReport(i, vector.columnName[i], val[i], v, false));
              }
            }
          }
        }

        if (report.isEmpty()) {
          numPass++;
        } else {
          numFails++;
        }
        lineReportAction.accept(stepRow, report);
      }
    }
    if (callback != null) {
      callback.accept(this);
    }
    return new int[] {numPass, numFails};
  }

  /**
   * Finds the pin and clock instances in the state for the vec. It does not search substates of state.
   *
   * @param vec the TestVector
   * @param state the root CircuitState.
   * @return an array of instances of the pins and clocks used by the TestVector found in the state.
   * @throws TestException if there is a mismatch between vec and state.
   */
  private static Instance[] getPinsForVector(TestVector vec, CircuitState state) throws TestException {
    int n = vec.columnName.length;
    Instance[] pins = new Instance[n];
    for (int i = 0; i < n; i++) {
      final var columnName = vec.columnName[i];
      for (Component comp : state.getCircuit().getNonWires()) {
        final var factory = comp.getFactory();
        if (factory instanceof Pin || factory instanceof Clock) {
          final var inst = Instance.getInstanceFor(comp);
          final var instanceState = state.getInstanceState(comp);
          final var label = instanceState.getAttributeValue(StdAttr.LABEL);
          if (factory instanceof Clock) {
            if (columnName.equals(label)
                || ((label == null || label.isEmpty()) && columnName.equalsIgnoreCase("<clk>"))) {
              if (vec.columnWidth[i].getWidth() != 1) {
                throw new TestException(S.get("tveClockWidthMismatch", columnName, vec.columnWidth[i].getWidth()));
              }
              pins[i] = inst;
              break;
            }
          } else { // Pin
            if (columnName.equals(label)) {
              if (Pin.FACTORY.getWidth(inst).getWidth() != vec.columnWidth[i].getWidth()) {
                throw new TestException(S.get("tveWidthMismatch", columnName, vec.columnWidth[i].getWidth(),
                    Pin.FACTORY.getWidth(inst).getWidth()));
              }
              pins[i] = inst;
              break;
            }
          }
        }
      }
      if (pins[i] == null) {
        throw new TestException(S.get("tveColumnNoPin", columnName));
      }
    }
    return pins;
  }
}
