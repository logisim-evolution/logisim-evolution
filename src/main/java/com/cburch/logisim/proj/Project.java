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

package com.cburch.logisim.proj;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitLocker;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.chronogram.chronogui.ChronoFrame;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.gui.opts.OptionsFrame;
import com.cburch.logisim.gui.test.TestFrame;
import com.cburch.logisim.gui.test.TestThread;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
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
    CircuitState circuitState;
    HdlModel hdlModel;
    Action action;

    public ActionData(CircuitState circuitState, HdlModel hdlModel, Action action) {
      this.circuitState = circuitState;
      this.hdlModel = hdlModel;
      this.action = action;
    }
  }

  private class MyListener implements Selection.Listener, LibraryListener {
    public void libraryChanged(LibraryEvent event) {
      int action = event.getAction();
      if (action == LibraryEvent.REMOVE_LIBRARY) {
        Library unloaded = (Library) event.getData();
        if (tool != null && unloaded.containsFromSource(tool)) {
          setTool(null);
        }
      } else if (action == LibraryEvent.REMOVE_TOOL) {
        Object data = event.getData();
        if (data instanceof AddTool) {
          Object factory = ((AddTool) data).getFactory();
          if (factory instanceof SubcircuitFactory) {
            SubcircuitFactory fact = (SubcircuitFactory) factory;
            if (fact.getSubcircuit() == getCurrentCircuit()) {
              setCurrentCircuit(file.getMainCircuit());
            }
          }
        }
      }
    }

    public void selectionChanged(Selection.Event e) {
      fireEvent(ProjectEvent.ACTION_SELECTION, e.getSource());
    }
  }

  private static final int MAX_UNDO_SIZE = 64;

  private Simulator simulator = new Simulator();
  private VhdlSimulatorTop VhdlSimulator = null;

  private LogisimFile file;
  private HdlModel hdlModel;
  private CircuitState circuitState; // active sim state
  private HashMap<Circuit, CircuitState> recentRootState = new HashMap<>(); // most recent root sim state for each circuit
  private LinkedList<CircuitState> allRootStates = new LinkedList<>(); // all root sim states, in display order
  private Frame frame = null;
  private OptionsFrame optionsFrame = null;
  private LogFrame logFrame = null;
  private TestFrame testFrame = null;
  private ChronoFrame chronoFrame = null;
  private Tool tool = null;
  private LinkedList<ActionData> undoLog = new LinkedList<ActionData>();
  private int undoMods = 0;
  private LinkedList<ActionData> redoLog = new LinkedList<ActionData>();
  private EventSourceWeakSupport<ProjectListener> projectListeners =
      new EventSourceWeakSupport<ProjectListener>();
  private EventSourceWeakSupport<LibraryListener> fileListeners =
      new EventSourceWeakSupport<LibraryListener>();
  private EventSourceWeakSupport<CircuitListener> circuitListeners =
      new EventSourceWeakSupport<CircuitListener>();
  private Dependencies depends;
  private MyListener myListener = new MyListener();
  private boolean startupScreen = false;
  private boolean forcedDirty = false;

  public Project(LogisimFile file) {
    addLibraryListener(myListener);
    setLogisimFile(file);
  }

  public void addCircuitListener(CircuitListener value) {
    circuitListeners.add(value);
    Circuit current = getCurrentCircuit();
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
    Loader loader = file.getLoader();
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
      ActionData firstData = undoLog.removeLast();
      Action first = firstData.action;
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
    Circuit circuit = (name == null ? file.getMainCircuit() : file.getCircuit(name));
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
    for (ProjectListener l : projectListeners) {
      l.projectChanged(event);
    }
  }

  /**
   * Decide whether or not you can redo
   *
   * @return if we can redo
   */
  public boolean getCanRedo() {
    // If there's a redo option found...
    if (redoLog.size() > 0)
      // We can redo
      return true;
    else
      // Otherwise we can't.
      return false;
  }

  public ChronoFrame getChronoFrame(boolean create) {
    if (logFrame == null) logFrame = new LogFrame(this);
    if (chronoFrame != null) chronoFrame.dispose();
    if (create) chronoFrame = new ChronoFrame(this, logFrame);
    return chronoFrame;
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
      CircuitState ret = recentRootState.get(circuit);
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
    CircuitState old = circuitState;
    HdlModel oldHdl = hdlModel;
    Circuit oldCircuit = old == null ? null : old.getCircuit();
    if (oldCircuit != null) {
      for (CircuitListener l : circuitListeners) {
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
   * Returns the action of the last entry in the redo log
   *
   * @return last action in redo log
   */
  public Action getLastRedoAction() {
    if (redoLog.size() == 0) return null;
    else return redoLog.getLast().action;
  }

  public LogFrame getLogFrame() {
    if (logFrame == null) {
      logFrame = new LogFrame(this);
    }
    return logFrame;
  }

  public LogisimFile getLogisimFile() {
    return file;
  }

  public Options getOptions() {
    return file.getOptions();
  }

  public OptionsFrame getOptionsFrame(boolean create) {
    if (optionsFrame == null || optionsFrame.getLogisimFile() != file) {
      if (create) optionsFrame = new OptionsFrame(this);
      else optionsFrame = null;
    }
    return optionsFrame;
  }

  public Selection getSelection() {
    if (frame == null) return null;
    Canvas canvas = frame.getCanvas();
    if (canvas == null) return null;
    return canvas.getSelection();
  }

  public Simulator getSimulator() {
    return simulator;
  }

  public TestFrame getTestFrame(boolean create) {
    if (testFrame == null) {
      if (create) testFrame = new TestFrame(this);
    }
    return testFrame;
  }

  public Tool getTool() {
    return tool;
  }

  public VhdlSimulatorTop getVhdlSimulator() {
    if (VhdlSimulator == null) VhdlSimulator = new VhdlSimulatorTop(this);

    return VhdlSimulator;
  }

  public boolean isFileDirty() {
    return (undoMods > 0)||forcedDirty;
  }

  // We track whether this project is the empty project opened
  // at startup by default, because we want to close it
  // immediately as another project is opened, if there
  // haven't been any changes to it.
  public boolean isStartupScreen() {
    return startupScreen;
  }
  
  public void setForcedDirty() {forcedDirty = true; file.setDirty(true);}

  /** Redo actions that were previously undone */
  public void redoAction() {
    // If there ARE things to undo...
    if (redoLog != null && redoLog.size() > 0) {
      // Add the last element of the undo log to the redo log
      undoLog.addLast(redoLog.getLast());
      ++undoMods;

      // Remove the last item in the redo log, but keep the data
      ActionData data = redoLog.removeLast();

      // Restore the circuit state to the redo's state
      if (data.circuitState != null) setCircuitState(data.circuitState);
      else if (data.hdlModel != null) setCurrentHdlModel(data.hdlModel);

      // Get the actions required to make that state change happen
      Action action = data.action;

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
    Circuit current = getCurrentCircuit();
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

    CircuitState old = circuitState;
    HdlModel oldHdl = hdlModel;
    Object oldActive = old;
    if (oldHdl != null) oldActive = oldHdl;
    Circuit oldCircuit = old == null ? null : old.getCircuit();
    Circuit newCircuit = value.getCircuit();
    boolean circuitChanged = old == null || oldCircuit != newCircuit;
    if (circuitChanged) {
      Canvas canvas = frame == null ? null : frame.getCanvas();
      if (canvas != null) {
        if (tool != null) tool.deselect(canvas);
        Selection selection = canvas.getSelection();
        if (selection != null) {
          Action act = SelectionActions.dropAll(selection);
          if (act != null) {
            doAction(act);
          }
        }
        if (tool != null) tool.select(canvas);
      }
      if (oldCircuit != null) {
        for (CircuitListener l : circuitListeners) {
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
        for (CircuitListener l : circuitListeners) {
          newCircuit.addCircuitListener(l);
        }
      }
      if (oldCircuit != null) oldCircuit.displayChanged();
      if (oldHdl != null) oldHdl.displayChanged();
      newCircuit.displayChanged();
    }
    fireEvent(ProjectEvent.ACTION_SET_STATE, old, circuitState);
  }

  public void setCurrentCircuit(Circuit circuit) {
    CircuitState circState = recentRootState.get(circuit);
    if (circState == null) {
      circState = new CircuitState(this, circuit);
      recentRootState.put(circuit, circState);
      allRootStates.add(circState);
    }
    setCircuitState(circState);
  }

  public void setFileAsClean() {
    undoMods = 0;
    forcedDirty=false;
    file.setDirty(isFileDirty());
  }

  public void setFileAsDirty() {
    file.setDirty(true);
  }

  public void setFrame(Frame value) {
    if (frame == value) return;
    Frame oldValue = frame;
    frame = value;
    Projects.windowCreated(this, oldValue, value);
    value.getCanvas().getSelection().addListener(myListener);
  }

  public void setLogisimFile(LogisimFile value) {
    LogisimFile old = this.file;
    if (old != null) {
      for (LibraryListener l : fileListeners) {
        old.removeLibraryListener(l);
      }
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
      for (LibraryListener l : fileListeners) {
        file.addLibraryListener(l);
      }
    }
    file.setDirty(true); // toggle it so that everybody hears the file is
    // fresh
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
    Tool old = tool;
    Canvas canvas = frame.getCanvas();
    if (old != null) old.deselect(canvas);
    Selection selection = canvas.getSelection();
    if (selection != null && !selection.isEmpty()) {
      if (value == null || !getOptions().getMouseMappings().containsSelectTool()) {
        Action act = SelectionActions.anchorAll(selection);
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
    if (undoLog != null && undoLog.size() > 0) {
      redoLog.addLast(undoLog.getLast());
      ActionData data = undoLog.removeLast();
      if (data.circuitState != null) setCircuitState(data.circuitState);
      else if (data.hdlModel != null) setCurrentHdlModel(data.hdlModel);
      Action action = data.action;
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
