/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

/**
 * Code taken from Cornell's version of Logisim:
 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */

package com.cburch.logisim.gui.test;

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
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UniquelyNamedThread;

public class TestThread extends UniquelyNamedThread implements CircuitListener {

	// used only for automated testing via command line arguments
	public static int doTestVector(Project proj, Circuit circuit,
			String vectorname) {
		System.out.println(StringUtil.format(Strings.get("testLoadingVector"),
				vectorname));
		TestVector vec;
		try {
			vec = new TestVector(vectorname);
		} catch (Exception e) {
			System.err.println(StringUtil.format(
					Strings.get("testLoadingFailed"), e.getMessage()));
			return -1;
		}

		TestThread tester;
		try {
			tester = new TestThread(proj, circuit, vec);
		} catch (TestException e) {
			System.err.println(StringUtil.format(
					Strings.get("testSetupFailed"), e.getMessage()));
			return -1;
		}

		System.out.println(StringUtil.format(Strings.get("testRunning"),
				Integer.toString(vec.data.size())));

		int numPass = 0, numFail = 0;
		for (int i = 0; i < vec.data.size(); i++) {
			try {
				System.out.print((i + 1) + " \r");
				tester.test(i);
				numPass++;
			} catch (FailException e) {
				System.out.println();
				System.err.println(StringUtil.format(Strings.get("testFailed"),
						Integer.toString(i + 1)));
				for (; e != null; e = e.getMore())
					System.out.println("  " + e.getMessage());
				numFail++;
				continue;
			} catch (TestException e) {
				System.out.println();
				System.err.println(StringUtil.format(Strings.get("testFailed"),
						Integer.toString(i + 1) + " " + e.getMessage()));
				numFail++;
				continue;
			}
		}
		System.out.println();
		System.out.println(StringUtil.format(Strings.get("testResults"),
				Integer.toString(numPass), Integer.toString(numFail)));
		return 0;
	}
	private Instance[] pin;
	private Project project;
	private Circuit circuit;

	private TestVector vector;
	private Model model;

	private boolean canceled = false, paused = false;

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
	private TestThread(Project proj, Circuit circuit, TestVector vec)
			throws TestException {
		super("TestThread-Project");
		this.project = proj;
		this.circuit = circuit;
		this.vector = vec;

		matchPins();
	}

	public void cancel() {
		canceled = true;
	}

	public void circuitChanged(CircuitEvent event) {
		int action = event.getAction();
		if (action == CircuitEvent.ACTION_SET_NAME)
			return;
		else
			model.clearResults();
	}

	void matchPins() throws TestException {
		int n = vector.columnName.length;
		pin = new Instance[n];
		CircuitState state = new CircuitState(this.project, this.circuit);

		for (int i = 0; i < n; i++) {
			String columnName = vector.columnName[i];
			for (Component comp : circuit.getNonWires()) {
				if (!(comp.getFactory() instanceof Pin))
					continue;
				Instance inst = Instance.getInstanceFor(comp);
				InstanceState pinState = state.getInstanceState(comp);
				String label = pinState.getAttributeValue(StdAttr.LABEL);
				if (label == null || !label.equals(columnName))
					continue;
				if (Pin.FACTORY.getWidth(inst).getWidth() != vector.columnWidth[i]
						.getWidth())
					throw new TestException("test vector column '" + columnName
							+ "' has width " + vector.columnWidth[i]
							+ ", but pin has width "
							+ Pin.FACTORY.getWidth(inst));
				pin[i] = inst;
				break;
			}
			if (pin[i] == null)
				throw new TestException("test vector column '" + columnName
						+ "' has no matching pin");
		}
	}

	public void run() {
		try {
			for (int i = 0; i < vector.data.size() && !canceled; i++) {
				while (paused) {
					if (canceled)
						return;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
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
