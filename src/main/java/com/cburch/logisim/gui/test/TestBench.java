/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.test;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.gui.start.SplashScreen;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.std.wiring.Pin;
import java.io.File;
import java.util.Map;

public class TestBench {

  /* Watch out the order matters*/
  private final String[] outputSignals = {"test_bench_done_o", "test_bench_ok_o"};
  private final Instance[] pinsOutput;
  private Project proj;

  public TestBench(String path, SplashScreen mon, Map<File, File> subs) {
    this.pinsOutput = new Instance[outputSignals.length];
    final var fileToOpen = new File(path);

    try {
      this.proj = ProjectActions.doOpenNoWindow(mon, fileToOpen);
    } catch (LoadFailedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      System.exit(-1);
    }
  }

  /* Check if the label correspond to any of the strings
   * located in outputSignals
   *  */
  private boolean checkMatchPinName(String label) {
    for (final var outName : outputSignals) {
      if (label.equals(outName)) {
        return true;
      }
    }
    return false;
  }

  /* Check if the label correspond to any of the output signals */
  private boolean searchMatchingPins(Circuit circuit) {
    /* Going to look for the matching output pin outputSignals */
    final var state = new CircuitState(proj, proj.getCurrentCircuit());
    var j = 0;
    var pinMatched = 0;

    for (final var output : outputSignals) {
      for (final var comp : circuit.getNonWires()) {
        if (!(comp.getFactory() instanceof Pin)) continue;

        /* Retrieve instance of component to then retrieve instance of
         * pins
         */
        final var inst = Instance.getInstanceFor(comp);
        final var pinState = state.getInstanceState(comp);
        final var label = pinState.getAttributeValue(StdAttr.LABEL);

        if (label == null && checkMatchPinName(label)) continue;

        if (inst == null) {
          /* TODO ERROR*/
          return false;
          // throw new TestException(" has no matching pin");
        }

        if (output.equals(label)) {
          pinsOutput[j] = inst;
          pinMatched++;
          break;
        }
      }
      j++;
    }

    return (pinMatched == outputSignals.length);
  }

  /* Start simulator */
  private boolean startSimulator() {
    final var sim = proj == null ? null : proj.getSimulator();
    if (sim == null) {
      // TODO ERROR
      // logger.error("FATAL ERROR - no simulator available");
      return false;
    }

    final var vhdlSim = sim.getCircuitState().getProject().getVhdlSimulator();
    vhdlSim.enable();
    sim.setAutoPropagation(true);
    /* TODO Timeout */
    while (vhdlSim.isEnabled()) {
      Thread.yield();
    }

    return true;
  }

  /* Main method in charge of launching the test bench */
  public boolean startTestBench() {
    final var circuit = (proj.getLogisimFile().getCircuit("logisim_test_verif"));
    proj.setCurrentCircuit(circuit);

    final var val = new Value[outputSignals.length];

    if (circuit == null) {
      System.out.println("Circuit is null");
      return false;
    }

    /* This is made to make comparison in logisim */
    for (int i = 0; i < val.length; i++) {
      val[i] = Value.createKnown(1, 1);
    }

    /* First launch the Simulator */
    if (!startSimulator()) {
      System.out.println("Error starting the simulator");
      return false;
    }

    /* Then try to find the pin to verify */
    if (!searchMatchingPins(circuit)) {
      System.out.println("Error finding the pins");
      return false;
    }

    /* Start the tests  */
    return circuit.doTestBench(proj, pinsOutput, val);
  }
}
