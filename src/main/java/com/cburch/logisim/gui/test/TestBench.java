/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.test;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.gui.start.SplashScreen;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorTop;
import java.io.File;
import java.util.HashMap;

public class TestBench {

  private Project proj;
  /* Watch out the order matters*/
  private String[] outputSignals = {"test_bench_done_o", "test_bench_ok_o"};
  private Instance[] pinsOutput;

  public TestBench(String Path, SplashScreen mon, HashMap<File, File> subs) {
    this.pinsOutput = new Instance[outputSignals.length];
    File fileToOpen = new File(Path);

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

    for (String outName : outputSignals) {
      if (label.equals(outName)) {
        return true;
      }
    }
    return false;
  }

  /* Check if the label correspond to any of the output signals */
  private boolean searchmatchingPins(Circuit circuit) {
    /* Going to look for the matching output pin outputSignals */
    CircuitState state = new CircuitState(proj, proj.getCurrentCircuit());
    int j = 0;
    int pinMatched = 0;

    for (String output : outputSignals) {
      for (com.cburch.logisim.comp.Component comp : circuit.getNonWires()) {
        if (!(comp.getFactory() instanceof Pin)) continue;

        /* Retrieve instance of component to then retrieve instance of
         * pins
         */
        Instance inst = Instance.getInstanceFor(comp);
        InstanceState pinState = state.getInstanceState(comp);
        String label = pinState.getAttributeValue(StdAttr.LABEL);

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
    Simulator sim = proj == null ? null : proj.getSimulator();
    if (sim == null) {
      /* TODO ERROR*/
      //	logger.error("FATAL ERROR - no simulator available");
      return false;
    }

    VhdlSimulatorTop vsim = sim.getCircuitState().getProject().getVhdlSimulator();
    vsim.enable();
    sim.setIsRunning(true);
    /* TODO Timeout */
    while (vsim.isEnabled()) {
      Thread.yield();
    }

    return true;
  }

  /* Main method in charge of launching the test bench */
  public boolean startTestBench() throws LoadFailedException {
    Circuit circuit = (proj.getLogisimFile().getCircuit("logisim_test_verif"));
    proj.setCurrentCircuit(circuit);

    Value[] val = new Value[outputSignals.length];

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
    if (!searchmatchingPins(circuit)) {
      System.out.println("Error finding the pins");
      return false;
    }

    /* Start the tests  */
    return circuit.doTestBench(proj, pinsOutput, val);
  }
}
