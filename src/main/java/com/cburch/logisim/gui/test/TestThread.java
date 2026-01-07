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
import com.cburch.logisim.circuit.TestVectorEvaluator;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.UniquelyNamedThread;

import java.util.ArrayList;

public class TestThread extends UniquelyNamedThread implements CircuitListener {

  private final Project project;
  private final Circuit circuit;
  private final CircuitState circuitState;
  private final TestVector vector;
  private final TestVectorEvaluator evaluator;
  private Model model;
  private boolean canceled = false;
  private boolean paused = false;

  public TestThread(Model model) throws TestException {
    super("TestThread-Model");
    this.model = model;

    this.project = model.getProject();
    this.circuit = model.getCircuit();
    this.circuitState = CircuitState.createRootState(this.project, this.circuit, this);
    this.vector = model.getVector();
    this.evaluator = new TestVectorEvaluator(circuitState, vector);
    model.getCircuit().addCircuitListener(this);
  }

  // used only for automated testing via command line arguments
  private TestThread(Project proj, Circuit circuit, TestVector vec) throws TestException {
    super("TestThread-Project");
    this.project = proj;
    this.circuit = circuit;
    this.circuitState = CircuitState.createRootState(this.project, this.circuit, Thread.currentThread());
    this.vector = vec;
    evaluator = new TestVectorEvaluator(circuitState, vector);
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
    return tester.doTestVector();
  }

  public int doTestVector() {
    System.out.println(S.get("testRunning", Integer.toString(vector.data.size())));
    final var passFail = evaluator.evaluate((row, report) -> {
      System.out.print((row + 1) + " \r");
      if (report != null && !report.isEmpty()) {
        System.out.println();
        System.err.println(S.get("testFailed", Integer.toString(row + 1)));
        for (final var e1 : report) System.out.println("  " + e1);
      }
    });
    System.out.println();
    System.out.println(S.get("testResults", Integer.toString(passFail[0]), Integer.toString(passFail[1])));
    return passFail[1];
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

  @Override
  public void run() {
    try {
      executeSequentialTests();
    } finally {
      model.stop();
    }
  }

  private void executeSequentialTests() {
    evaluator.evaluate((row, report) -> {
      canceled = canceled || !model.setResult(vector, row, report);
      if (canceled) evaluator.setCanceled(canceled);
    });
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }
}
