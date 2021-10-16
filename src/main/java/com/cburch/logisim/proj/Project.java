/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.proj;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitLocker;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.gui.opts.OptionsFrame;
import com.cburch.logisim.gui.test.TestFrame;
import com.cburch.logisim.gui.test.TestThread;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.vhdl.base.HdlModel;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorTop;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFileChooser;

public class Project {
  private static class ActionData {
    final CircuitState circuitState;
    final HdlModel hdlModel;
    final Action action;

    public ActionData(CircuitState circuitState, HdlModel hdlModel, Action action) {
      this.circuitState = circuitState;
      this.hdlModel = hdlModel;
      this.action = action;
    }
  }

  private class MyListener implements Selection.Listener, LibraryListener {
    @Override
    public void libraryChanged(LibraryEvent event) {
      int action = event.getAction();
      if (action == LibraryEvent.REMOVE_LIBRARY) {
        final var unloaded = (Library) event.getData();
        if (tool != null && unloaded.containsFromSource(tool)) {
          setTool(null);
        }
      } else if (action == LibraryEvent.REMOVE_TOOL) {
        Object data = event.getData();
        if (data instanceof AddTool) {
          Object factory = ((AddTool) data).getFactory();
          if (factory instanceof SubcircuitFactory fact) {
            if (fact.getSubcircuit() == getCurrentCircuit()) {
              setCurrentCircuit(file.getMainCircuit());
            }
          }
        }
      }
    }

    @Override
    public void selectionChanged(Selection.Event e) {
      fireEvent(ProjectEvent.ACTION_SELECTION, e.getSource());
    }
  }

  private static final int MAX_UNDO_SIZE = 64;

  private final Simulator simulator = new Simulator();
  private VhdlSimulatorTop vhdlSimulator = null;

  private LogisimFile file;
  private HdlModel hdlModel;
  private CircuitState circuitState; // active sim state
  private final HashMap<Circuit, CircuitState> recentRootState =
      new HashMap<>(); // most recent root sim state for each circuit
  private final LinkedList<CircuitState> allRootStates =
      new LinkedList<>(); // all root sim states, in display order
  private Frame frame = null;
  private OptionsFrame optionsFrame = null;
  private LogFrame logFrame = null;
  private TestFrame testFrame = null;
  private Tool tool = null;
  private final LinkedList<ActionData> undoLog = new LinkedList<>();
  private int undoMods = 0;
  private final LinkedList<ActionData> redoLog = new LinkedList<>();
  private final EventSourceWeakSupport<ProjectListener> projectListeners =
      new EventSourceWeakSupport<>();
  private final EventSourceWeakSupport<LibraryListener> fileListeners =
      new EventSourceWeakSupport<>();
  private final EventSourceWeakSupport<CircuitListener> circuitListeners =
      new EventSourceWeakSupport<>();
  private Dependencies depends;
  private final MyListener myListener = new MyListener();
  private boolean startupScreen = false;
  private boolean forcedDirty = false;

  public Project(LogisimFile file) {
    addLibraryListener(myListener);
    setLogisimFile(file);
  }

  public void addCircuitListener(CircuitListener value) {
    circuitListeners.add(value);
    final var current = getCurrentCircuit();
    if (current != null) current.addCircuitListener(value);
  }

  public void addLibraryListener(LibraryListener value) {
    fileListeners.add(value);
    if (file != null) file.addLibraryListener(value);
  }

  //
  // Listener methods
  //
  public void addProjectListener(ProjectListener what) {
    projectListeners.add(what);
  }

  public boolean confirmClose(String title) {
    return frame.confirmClose(title);
  }

  public JFileChooser createChooser() {
    if (file == null) return JFileChoosers.create();
    final var loader = file.getLoader();
    return loader == null ? JFileChoosers.create() : loader.createChooser();
  }

  public void doAction(Action act) {
    if (act == null) {
      return;
    }
    Action toAdd = act;
    startupScreen = false;
    redoLog.clear();

    if (!undoLog.isEmpty() && act.shouldAppendTo(getLastAction())) {
      final var firstData = undoLog.removeLast();
      final var first = firstData.action;
      if (first.isModification()) {
        --undoMods;
      }
      toAdd = first.append(act);
      if (toAdd != null) {
        undoLog.add(new ActionData(circuitState, hdlModel, toAdd));
        if (toAdd.isModification()) ++undoMods;
      }
      fireEvent(new ProjectEvent(ProjectEvent.ACTION_START, this, act));
      try {
        act.doIt(this);
      } catch (CircuitLocker.LockException e) {
        System.out.println("*** Circuit Lock Bug Diagnostics ***");
        System.out.println("This thread: " + Thread.currentThread());
        System.out.println("attempted to access without any locks:");
        System.out.printf(
            "  circuit \"%s\" [lock serial: %d/%d]\n",
            e.getCircuit().getName(),
            e.getSerialNumber(),
            e.getCircuit().getLocker().getSerialNumber());
        System.out.println("  owned by thread: " + e.getMutatingThread());
        System.out.println("  with mutator: " + e.getCircuitMutator());
        throw e;
      }
      file.setDirty(isFileDirty());
      fireEvent(new ProjectEvent(ProjectEvent.ACTION_COMPLETE, this, act));
      fireEvent(new ProjectEvent(ProjectEvent.ACTION_MERGE, this, first, toAdd));
      return;
    }
    undoLog.add(new ActionData(circuitState, hdlModel, toAdd));
    fireEvent(new ProjectEvent(ProjectEvent.ACTION_START, this, act));
    try {
      act.doIt(this);
    } catch (CircuitLocker.LockException e) {
      System.out.println("*** Circuit Lock Bug Diagnostics ***");
      System.out.println("This thread: " + Thread.currentThread());
      System.out.println("attempted to access without any locks:");
      System.out.printf(
          "  circuit \"%s\" [lock serial: %d/%d]\n",
          e.getCircuit().getName(),
          e.getSerialNumber(),
          e.getCircuit().getLocker().getSerialNumber());
      System.out.println("  owned by thread: " + e.getMutatingThread());
      System.out.println("  with mutator: " + e.getCircuitMutator());
      throw e;
    }
    while (undoLog.size() > MAX_UNDO_SIZE) {
      undoLog.removeFirst();
    }
    if (toAdd.isModification()) {
      ++undoMods;
    }
    file.setDirty(isFileDirty());
    fireEvent(new ProjectEvent(ProjectEvent.ACTION_COMPLETE, this, act));
  }

  public int doTestVector(String vectorname, String name) {
    final var circuit = (name == null ? file.getMainCircuit() : file.getCircuit(name));
    if (circuit == null) {
      System.err.println("Circuit '" + name + "' not found.");
      return -1;
    }
    setCurrentCircuit(circuit);
    return TestThread.doTestVector(this, circuit, vectorname);
  }

  private void fireEvent(int action, Object data) {
    fireEvent(new ProjectEvent(action, this, data));
  }

  private void fireEvent(int action, Object old, Object data) {
    fireEvent(new ProjectEvent(action, this, old, data));
  }

  private void fireEvent(ProjectEvent event) {
    for (final var l : projectListeners) {
      l.projectChanged(event);
    }
  }

  /**
   * Decide whether or not you can redo.
   *
   * @return if we can redo
   */
  public boolean getCanRedo() {
    // If there's a redo option found...
    // We can redo
    // Otherwise we can't.
    return redoLog.size() > 0;
  }

  public List<CircuitState> getRootCircuitStates() {
    return allRootStates;
  }

  public CircuitState getCircuitState() {
    return circuitState;
  }

  public CircuitState getCircuitState(Circuit circuit) {
    if (circuitState != null && circuitState.getCircuit() == circuit) {
      return circuitState;
    } else {
      var ret = recentRootState.get(circuit);
      if (ret == null) {
        ret = new CircuitState(this, circuit);
        recentRootState.put(circuit, ret);
        allRootStates.add(ret);
      }
      return ret;
    }
  }

  public Circuit getCurrentCircuit() {
    return circuitState == null ? null : circuitState.getCircuit();
  }

  public HdlModel getCurrentHdl() {
    return hdlModel;
  }

  public void setCurrentHdlModel(HdlModel hdl) {
    if (hdlModel == hdl) return;
    setTool(null);
    final var old = circuitState;
    final var oldHdl = hdlModel;
    final var oldCircuit = (old == null) ? null : old.getCircuit();
    if (oldCircuit != null) {
      for (final var l : circuitListeners) {
        oldCircuit.removeCircuitListener(l);
      }
    }
    circuitState = null;
    hdlModel = hdl;
    if (old != null) {
      simulator.setCircuitState(null);
    }
    Object oldActive = old;
    if (oldHdl != null) oldActive = oldHdl;
    fireEvent(ProjectEvent.ACTION_SET_CURRENT, oldActive, hdl);
    if (old != null) fireEvent(ProjectEvent.ACTION_SET_STATE, old, null);
    if (oldCircuit != null) oldCircuit.displayChanged();
    if (oldHdl != null) oldHdl.displayChanged();
    hdl.displayChanged();
  }

  public Dependencies getDependencies() {
    return depends;
  }

  public Frame getFrame() {
    return frame;
  }

  public Action getLastAction() {
    if (undoLog.size() == 0) {
      return null;
    } else {
      return undoLog.getLast().action;
    }
  }

  /**
   * Returns the action of the last entry in the redo log.
   *
   * @return last action in redo log
   */
  public Action getLastRedoAction() {
    if (redoLog.size() == 0) return null;
    else return redoLog.getLast().action;
  }

  public LogFrame getLogFrame() {
    if (logFrame == null) logFrame = new LogFrame(this);
    return logFrame;
  }

  public LogisimFile getLogisimFile() {
    return file;
  }

  public Options getOptions() {
    return file.getOptions();
  }

  public OptionsFrame getOptionsFrame() {
    if (optionsFrame == null) optionsFrame = new OptionsFrame(this);
    return optionsFrame;
  }

  public Selection getSelection() {
    if (frame == null) return null;
    final var canvas = frame.getCanvas();
    if (canvas == null) return null;
    return canvas.getSelection();
  }

  public Simulator getSimulator() {
    return simulator;
  }

  public TestFrame getTestFrame() {
    if (testFrame == null) testFrame = new TestFrame(this);
    return testFrame;
  }

  public Tool getTool() {
    return tool;
  }

  public VhdlSimulatorTop getVhdlSimulator() {
    if (vhdlSimulator == null) vhdlSimulator = new VhdlSimulatorTop(this);

    return vhdlSimulator;
  }

  public boolean isFileDirty() {
    return (undoMods > 0) || forcedDirty;
  }

  // We track whether this project is the empty project opened
  // at startup by default, because we want to close it
  // immediately as another project is opened, if there
  // haven't been any changes to it.
  public boolean isStartupScreen() {
    return startupScreen;
  }

  public void setForcedDirty() {
    forcedDirty = true;
    file.setDirty(true);
  }

  /** Redo actions that were previously undone. */
  public void redoAction() {
    // If there ARE things to undo...
    if (CollectionUtil.isNotEmpty(redoLog)) {
      // Add the last element of the undo log to the redo log
      undoLog.addLast(redoLog.getLast());
      ++undoMods;

      // Remove the last item in the redo log, but keep the data
      final var data = redoLog.removeLast();

      // Restore the circuit state to the redo's state
      if (data.circuitState != null) setCircuitState(data.circuitState);
      else if (data.hdlModel != null) setCurrentHdlModel(data.hdlModel);

      // Get the actions required to make that state change happen
      final var action = data.action;

      // Call the event
      fireEvent(new ProjectEvent(ProjectEvent.REDO_START, this, action));

      // Redo the action
      action.doIt(this);

      // Complete the redo
      fireEvent(new ProjectEvent(ProjectEvent.REDO_COMPLETE, this, action));
    }
  }

  public void removeCircuitListener(CircuitListener value) {
    circuitListeners.remove(value);
    final var current = getCurrentCircuit();
    if (current != null) current.removeCircuitListener(value);
  }

  public void removeLibraryListener(LibraryListener value) {
    fileListeners.remove(value);
    if (file != null) file.removeLibraryListener(value);
  }

  public void removeProjectListener(ProjectListener what) {
    projectListeners.remove(what);
  }

  public void repaintCanvas() {
    // for actions that ought not be logged (i.e., those that
    // change nothing, except perhaps the current values within
    // the circuit)
    fireEvent(new ProjectEvent(ProjectEvent.REPAINT_REQUEST, this, null));
  }

  public void setCircuitState(CircuitState value) {
    if (value == null || circuitState == value) return;

    final var old = circuitState;
    final var oldHdl = hdlModel;
    Object oldActive = old;
    if (oldHdl != null) oldActive = oldHdl;
    final var oldCircuit = old == null ? null : old.getCircuit();
    final var newCircuit = value.getCircuit();
    boolean circuitChanged = old == null || oldCircuit != newCircuit;
    if (circuitChanged) {
      final var canvas = frame == null ? null : frame.getCanvas();
      if (canvas != null) {
        if (tool != null) tool.deselect(canvas);
        final var selection = canvas.getSelection();
        if (selection != null) {
          final var act = SelectionActions.dropAll(selection);
          if (act != null) {
            doAction(act);
          }
        }
        if (tool != null) tool.select(canvas);
      }
      if (oldCircuit != null) {
        for (final var l : circuitListeners) {
          oldCircuit.removeCircuitListener(l);
        }
      }
    }
    hdlModel = null;
    circuitState = value;
    if (circuitState.getParentState() == null) {
      recentRootState.put(newCircuit, circuitState);
    }
    simulator.setCircuitState(circuitState);
    if (circuitChanged) {
      fireEvent(ProjectEvent.ACTION_SET_CURRENT, oldActive, newCircuit);
      if (newCircuit != null) {
        for (final var l : circuitListeners) {
          newCircuit.addCircuitListener(l);
        }
        final var circTickFrequency = newCircuit.getTickFrequency();
        final var simTickFrequency = simulator.getTickFrequency();
        if (circTickFrequency < 0) {
          newCircuit.setTickFrequency(simTickFrequency);
        } else if (circTickFrequency != simTickFrequency) {
          simulator.setTickFrequency(circTickFrequency);
        }
      }
      if (oldCircuit != null) oldCircuit.displayChanged();
      if (oldHdl != null) oldHdl.displayChanged();
      newCircuit.displayChanged();
    }
    fireEvent(ProjectEvent.ACTION_SET_STATE, old, circuitState);
  }

  public void setCurrentCircuit(Circuit circuit) {
    var circState = recentRootState.get(circuit);
    if (circState == null) {
      circState = new CircuitState(this, circuit);
      recentRootState.put(circuit, circState);
      allRootStates.add(circState);
    }
    setCircuitState(circState);
  }

  public void setFileAsClean() {
    undoMods = 0;
    forcedDirty = false;
    file.setDirty(isFileDirty());
  }

  public void setFileAsDirty() {
    file.setDirty(true);
  }

  public void setFrame(Frame value) {
    if (frame == value) return;
    final var oldValue = frame;
    frame = value;
    Projects.windowCreated(this, oldValue, value);
    value.getCanvas().getSelection().addListener(myListener);
  }

  public void setLogisimFile(LogisimFile value) {
    final var old = this.file;
    if (old != null) {
      for (final var l : fileListeners) {
        old.removeLibraryListener(l);
      }
    }
    if (optionsFrame != null) {
      optionsFrame.dispose();
      optionsFrame = null;
    }
    file = value;
    recentRootState.clear();
    allRootStates.clear();
    depends = new Dependencies(file);
    undoLog.clear();
    redoLog.clear();
    undoMods = 0;
    fireEvent(ProjectEvent.ACTION_SET_FILE, old, file);
    setCurrentCircuit(file.getMainCircuit());
    if (file != null) {
      for (final var l : fileListeners) {
        file.addLibraryListener(l);
      }
    }
    file.setDirty(true); // toggle it so that everybody hears the file is fresh
    file.setDirty(false);
  }

  //
  // actions
  //
  public void setStartupScreen(boolean value) {
    startupScreen = value;
  }

  public void setTool(Tool value) {
    if (tool == value) return;
    final var old = tool;
    final var canvas = frame.getCanvas();
    if (old != null) old.deselect(canvas);
    final var selection = canvas.getSelection();
    if (selection != null && !selection.isEmpty()) {
      if (value == null || !getOptions().getMouseMappings().containsSelectTool()) {
        final var act = SelectionActions.anchorAll(selection);
        /*
         * Circuit circuit = canvas.getCircuit(); CircuitMutation xn =
         * new CircuitMutation(circuit); if (value == null) { Action act
         * = SelectionActions.dropAll(selection); if (act != null) {
         * doAction(act); } } else if
         * (!getOptions().getMouseMappings().containsSelectTool()) {
         * Action act = SelectionActions.dropAll(selection);
         */
        if (act != null) {
          doAction(act);
        }
      }
      /*
       * if (!xn.isEmpty()) doAction(xn.toAction(null));
       */
    }
    startupScreen = false;
    tool = value;
    if (tool != null) tool.select(frame.getCanvas());
    fireEvent(ProjectEvent.ACTION_SET_TOOL, old, tool);
  }

  public void undoAction() {
    if (CollectionUtil.isNotEmpty(undoLog)) {
      redoLog.addLast(undoLog.getLast());
      final var data = undoLog.removeLast();
      if (data.circuitState != null) setCircuitState(data.circuitState);
      else if (data.hdlModel != null) setCurrentHdlModel(data.hdlModel);
      final var action = data.action;
      if (action.isModification()) {
        --undoMods;
      }
      fireEvent(new ProjectEvent(ProjectEvent.UNDO_START, this, action));
      action.undo(this);
      file.setDirty(isFileDirty());
      fireEvent(new ProjectEvent(ProjectEvent.UNDO_COMPLETE, this, action));
    }
  }
}
