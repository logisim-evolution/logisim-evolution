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

import java.util.ArrayList;

import javax.swing.SwingUtilities;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.EventSourceWeakSupport;

class Model {

	private class UpdateResultSort implements Runnable {

		public void run() {
			updateResultSort();
		}

	}
	private EventSourceWeakSupport<ModelListener> listeners;
	private boolean selected = false;
	private Project project;
	private Circuit circuit;
	private boolean running, paused;
	private TestThread tester;
	private int numPass = 0, numFail = 0;
	private TestVector vec = null;

	private TestException results[];

	private UpdateResultSort myUpdateResultSort = new UpdateResultSort();

	private ArrayList<Integer> failed = new ArrayList<Integer>();

	private ArrayList<Integer> passed = new ArrayList<Integer>();

	public Model(Project proj, Circuit circuit) {
		listeners = new EventSourceWeakSupport<ModelListener>();
		this.circuit = circuit;
		this.project = proj;
	}

	public void addModelListener(ModelListener l) {
		listeners.add(l);
	}

	public void clearResults() {
		stop();
		synchronized (this) {
			if (vec == null || results == null)
				return;
			numPass = numFail = 0;
			failed.clear();
			passed.clear();
		}
		fireTestResultsChanged();
	}

	private void fireTestingChanged() {
		for (ModelListener listener : listeners) {
			listener.testingChanged();
		}
	}

	private void fireTestResultsChanged() {
		for (ModelListener listener : listeners) {
			listener.testResultsChanged(numPass, numFail);
		}
	}

	private void fireVectorChanged() {
		for (ModelListener listener : listeners) {
			listener.vectorChanged();
		}
	}

	public Circuit getCircuit() {
		return circuit;
	}

	public int getFail() {
		return numFail;
	}

	public int getPass() {
		return numPass;
	}

	public Project getProject() {
		return project;
	}

	public TestException[] getResults() {
		return results;
	}

	public TestVector getVector() {
		return vec;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isSelected() {
		return selected;
	}
	public void removeModelListener(ModelListener l) {
		listeners.remove(l);
	}

	public void setPaused(boolean paused) {
		synchronized (this) {
			if (running && tester != null)
				tester.setPaused(paused);
			this.paused = paused;
		}
		fireTestingChanged();
	}

	public boolean setResult(TestVector v, int idx, TestException err) {
		synchronized (this) {
			if (v != vec || idx < 0 || idx >= results.length
					|| idx != numPass + numFail)
				return false;
			results[idx] = err;
			if (err == null)
				numPass++;
			else
				numFail++;
		}
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(myUpdateResultSort);
		} else {
			updateResultSort();
		}
		return true;
	}

	public void setSelected(boolean value) {
		if (selected == value)
			return;
		selected = value;
		if (!selected)
			setPaused(true);
		else
			setPaused(false);
	}

	public synchronized void setVector(TestVector v) {
		stop();
		synchronized (this) {
			vec = v;
			results = (v != null ? new TestException[v.data.size()] : null);
			numPass = numFail = 0;
			failed.clear();
			passed.clear();
		}
		fireVectorChanged();
	}

	public int sortedIndex(int i) {
		if (i < failed.size())
			return (failed.get(i)).intValue();
		if (i < failed.size() + passed.size())
			return (passed.get(i - failed.size())).intValue();
		return i;
	}

	public void start() throws TestException {
		synchronized (this) {
			if (vec == null)
				return;
			if (running) {
				setPaused(false);
				return;
			}
			tester = new TestThread(this);
			running = true;
			paused = false;
			tester.start();
		}
		fireTestingChanged();
	}

	public void stop() {
		synchronized (this) {
			if (!running)
				return;
			running = false;
			if (tester != null)
				tester.cancel();
			tester = null;
		}
		fireTestingChanged();
	}

	private void updateResultSort() {
		if (vec == null)
			return;
		for (int i = failed.size() + passed.size(); i < numPass + numFail; i++) {
			if (results[i] == null)
				passed.add(new Integer(i));
			else
				failed.add(new Integer(i));
		}
		fireTestResultsChanged();
	}

}
