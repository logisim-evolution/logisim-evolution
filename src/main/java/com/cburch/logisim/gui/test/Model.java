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
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.EventSourceWeakSupport;

import java.util.ArrayList;
import javax.swing.SwingUtilities;

class Model {

  private final EventSourceWeakSupport<ModelListener> listeners;
  private final Project project;
  private final Circuit circuit;
  private final UpdateResultSort myUpdateResultSort = new UpdateResultSort();
  private final ArrayList<Integer> failed = new ArrayList<>();
  private final ArrayList<Integer> passed = new ArrayList<>();
  private boolean selected = false;
  private boolean running;
  private boolean paused;
  private TestThread tester;
  private int numPass = 0;
  private int numFail = 0;
  private TestVector vec = null;
  private TestException[] results;

  public Model(Project proj, Circuit circuit) {
    listeners = new EventSourceWeakSupport<>();
    this.circuit = circuit;
    this.project = proj;
  }

  public void addModelListener(ModelListener l) {
    listeners.add(l);
  }

  public void clearResults() {
    stop();
    synchronized (this) {
      if (vec == null || results == null) return;
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

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    synchronized (this) {
      if (running && tester != null) tester.setPaused(paused);
      this.paused = paused;
    }
    fireTestingChanged();
  }

  public boolean isRunning() {
    return running;
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean value) {
    if (selected == value) return;
    selected = value;
    setPaused(!selected);
  }

  public void removeModelListener(ModelListener l) {
    listeners.remove(l);
  }

  public boolean setResult(TestVector v, int idx, TestException err) {
    synchronized (this) {
      if (v != vec || idx < 0 || idx >= results.length || idx != numPass + numFail) return false;
      results[idx] = err;
      if (err == null) numPass++;
      else numFail++;
    }
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(myUpdateResultSort);
    } else {
      updateResultSort();
    }
    return true;
  }

  public int sortedIndex(int i) {
    if (i < failed.size()) return failed.get(i);
    if (i < failed.size() + passed.size()) return passed.get(i - failed.size());
    return i;
  }

  public void start() throws TestException {
    synchronized (this) {
      if (vec == null) return;
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
      if (!running) return;
      running = false;
      if (tester != null) tester.cancel();
      tester = null;
    }
    fireTestingChanged();
  }

  private void updateResultSort() {
    if (vec == null) return;
    for (int i = failed.size() + passed.size(); i < numPass + numFail; i++) {
      if (results[i] == null) passed.add(i);
      else failed.add(i);
    }
    fireTestResultsChanged();
  }

  private class UpdateResultSort implements Runnable {

    @Override
    public void run() {
      updateResultSort();
    }
  }
}
