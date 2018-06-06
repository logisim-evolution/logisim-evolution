package com.cburch.logisim.gui.test;

import java.io.File;
import java.util.HashMap;

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


public class TestBench {

	private Project proj;
	/* Watch out the order matters*/
	private String[] outputSignals = {"test_bench_done_o", "test_bench_ok_o"};
	private Instance[] pinsOutput;

	public TestBench(String Path, SplashScreen mon, HashMap<File, File> subs ) {
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

		for (String outName: outputSignals) {
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

		for (String output: outputSignals) {
			for (com.cburch.logisim.comp.Component comp : circuit.getNonWires()) {
				if (!(comp.getFactory() instanceof Pin))
					continue;

				/* Retrieve instance of component to then retrieve instance of
				 * pins
				 */
				Instance inst = Instance.getInstanceFor(comp);
				InstanceState pinState = state.getInstanceState(comp);
				String label = pinState.getAttributeValue(StdAttr.LABEL);

				if (label == null && checkMatchPinName(label))
					continue;

				if (inst == null) {
					/* TODO ERROR*/
					return false;
					//throw new TestException(" has no matching pin");
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

		sim.getCircuitState().getProject().getVhdlSimulator().enable();
		/* Start Simulation */
		sim.setIsRunning(true);

		/* TODO Timeout */
		while(!sim.getCircuitState().getProject().getVhdlSimulator().isEnabled()) {
			Thread.yield();
		}

		return true;
	}

	/* Main method in charge of launching the test bench */
	public boolean startTestBench() throws LoadFailedException  {
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
		if(!searchmatchingPins(circuit)) {
			System.out.println("Error finding the pins");
			return false;
		}

		/* Start the tests  */
		return circuit.doTestBench(proj, pinsOutput, val);
	}
}
