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
import com.cburch.logisim.circuit.TestVectorEvaluator;
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
  private final ArrayList<Integer> sortedIndices = new ArrayList<>();
  private boolean selected = false;
  private boolean running;
  private boolean paused;
  private TestThread tester;
  private int numPass = 0;
  private int numFail = 0;
  private TestVector vec = null;
  private ArrayList<TestVectorEvaluator.LineReport>[] results;

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

  public ArrayList<TestVectorEvaluator.LineReport>[] getResults() {
    return results;
  }

  public TestVector getVector() {
    return vec;
  }

  @SuppressWarnings("unchecked")
  public synchronized void setVector(TestVector v) {
    stop();
    synchronized (this) {
      vec = v;
      results = ((v != null) ? (new ArrayList[v.data.size()]) : null);
      numPass = numFail = 0;
      failed.clear();
      passed.clear();
      sortedIndices.clear();
      if (v != null) {
        updateSortedIndices();
      }
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

  public boolean setResult(TestVector v, int idx, ArrayList<TestVectorEvaluator.LineReport> report) {
    synchronized (this) {
      if (v != vec || idx < 0 || idx >= results.length || idx != numPass + numFail) return false;
      if (report == null || report.isEmpty()) {
        results[idx] = null;
        numPass++;
      } else {
        results[idx] = report;
        numFail++;
      }
    }
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(myUpdateResultSort);
    } else {
      updateResultSort();
    }
    return true;
  }

  public void updateResult(TestVector v, int idx, ArrayList<TestVectorEvaluator.LineReport> err) {
    synchronized (this) {
      if (v != vec || idx < 0 || idx >= results.length) return;
      // Update the result, adjusting pass/fail counts if needed
      ArrayList<TestVectorEvaluator.LineReport> oldResult = results[idx];
      boolean wasCounted = (idx < numPass + numFail);

      results[idx] = err;

      if (wasCounted) {
        // The old result was already counted, so adjust counts
        if (oldResult == null && err != null) {
          // Changed from pass to fail
          numPass--;
          numFail++;
        } else if (oldResult != null && err == null) {
          // Changed from fail to pass
          numPass++;
          numFail--;
        }
        // If both are null or both are non-null, counts don't change
      } else {
        // The old result was not counted, so add the new one
        if (err == null) {
          numPass++;
        } else {
          numFail++;
        }
      }
    }
    // Always update sorted indices and fire event, even for updates
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> {
        updateSortedIndices();
        fireTestResultsChanged();
      });
    } else {
      updateSortedIndices();
      fireTestResultsChanged();
    }
  }

  public int sortedIndex(int i) {
    if (i < sortedIndices.size()) {
      return sortedIndices.get(i);
    }
    // Fallback: if sortedIndices not populated, use old behavior
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
    updateSortedIndices();
    fireTestResultsChanged();
  }

  private void updateSortedIndices() {
    if (vec == null) return;
    sortedIndices.clear();

    // Create list of all row indices
    ArrayList<Integer> allIndices = new ArrayList<>();
    for (int i = 0; i < vec.data.size(); i++) {
      allIndices.add(i);
    }

    // Sort by set first, then by sequence, then by pass/fail status, then by original index
    allIndices.sort((a, b) -> {
      int setA = (vec.setNumbers != null && a < vec.setNumbers.length) ? vec.setNumbers[a] : 0;
      int setB = (vec.setNumbers != null && b < vec.setNumbers.length) ? vec.setNumbers[b] : 0;
      int seqA = (vec.seqNumbers != null && a < vec.seqNumbers.length) ? vec.seqNumbers[a] : 0;
      int seqB = (vec.seqNumbers != null && b < vec.seqNumbers.length) ? vec.seqNumbers[b] : 0;

      // First compare by set
      int setCompare = Integer.compare(setA, setB);
      if (setCompare != 0) return setCompare;

      // Then compare by sequence
      int seqCompare = Integer.compare(seqA, seqB);
      if (seqCompare != 0) return seqCompare;

      // Within same set/seq, show failed tests first (if results available)
      if (results != null && a < results.length && b < results.length) {
        boolean failedA = results[a] != null;
        boolean failedB = results[b] != null;
        if (failedA != failedB) {
          return failedA ? -1 : 1; // failed comes first
        }
      }

      // If set and seq are the same, maintain original order (by index)
      return Integer.compare(a, b);
    });

    sortedIndices.addAll(allIndices);
  }

  private class UpdateResultSort implements Runnable {

    @Override
    public void run() {
      updateResultSort();
    }
  }
}
