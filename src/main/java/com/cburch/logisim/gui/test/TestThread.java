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
    this.vector = model.getVector();

    matchPins();

    model.getCircuit().addCircuitListener(this);
  }

  // used only for automated testing via command line arguments
  private TestThread(Project proj, Circuit circuit, TestVector vec) throws TestException {
    super("TestThread-Project");
    this.project = proj;
    this.circuit = circuit;
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

    int numPass = 0;
    int numFail = 0;
    for (int i = 0; i < vec.data.size(); i++) {
      try {
        System.out.print((i + 1) + " \r");
        tester.test(i);
        numPass++;
      } catch (FailException e) {
        System.out.println();
        System.err.println(S.get("testFailed", Integer.toString(i + 1)));
        for (FailException e1 : e.getAll()) System.out.println("  " + e1.getMessage());
        numFail++;
      } catch (TestException e) {
        System.out.println();
        System.err.println(S.get("testFailed", (i + 1) + " " + e.getMessage()));
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
    CircuitState state = CircuitState.createRootState(this.project, this.circuit);

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
      for (int i = 0; i < vector.data.size() && !canceled; i++) {
        while (paused) {
          if (canceled) return;
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignored) {
          }
        }
        try {
          test(i);
          canceled = canceled || !model.setResult(vector, i, null);
        } catch (TestException e) {
          canceled = canceled || !model.setResult(vector, i, e);
        }
        Thread.yield();
      }
    } finally {
      model.stop();
    }
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  private void test(int idx) throws TestException {
    circuit.doTestVector(project, pin, vector.data.get(idx));
  }
}
