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

package com.cburch.logisim.gui.menu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.fpga.menu.MenuFPGA;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class LogisimMenuBar extends JMenuBar {
  private class MyListener implements LocaleListener {
    public void localeChanged() {
      file.localeChanged();
      edit.localeChanged();
      project.localeChanged();
      fpga.localeChanged();
      simulate.localeChanged();
      help.localeChanged();
    }
  }

  public static final LogisimMenuItem PRINT = new LogisimMenuItem("Print");
  public static final LogisimMenuItem EXPORT_IMAGE = new LogisimMenuItem("ExportImage");
  public static final LogisimMenuItem CUT = new LogisimMenuItem("Cut");
  public static final LogisimMenuItem COPY = new LogisimMenuItem("Copy");
  public static final LogisimMenuItem PASTE = new LogisimMenuItem("Paste");
  public static final LogisimMenuItem DELETE = new LogisimMenuItem("Delete");
  public static final LogisimMenuItem DUPLICATE = new LogisimMenuItem("Duplicate");
  public static final LogisimMenuItem SELECT_ALL = new LogisimMenuItem("SelectAll");
  public static final LogisimMenuItem RAISE = new LogisimMenuItem("Raise");
  public static final LogisimMenuItem LOWER = new LogisimMenuItem("Lower");
  public static final LogisimMenuItem RAISE_TOP = new LogisimMenuItem("RaiseTop");
  public static final LogisimMenuItem LOWER_BOTTOM = new LogisimMenuItem("LowerBottom");
  public static final LogisimMenuItem ADD_CONTROL = new LogisimMenuItem("AddControl");
  public static final LogisimMenuItem REMOVE_CONTROL = new LogisimMenuItem("RemoveControl");
  public static final LogisimMenuItem[] EDIT_ITEMS = {
    // UNDO, REDO,
    CUT, COPY, PASTE,
    DELETE, DUPLICATE, SELECT_ALL,
    RAISE, LOWER, RAISE_TOP, LOWER_BOTTOM,
    ADD_CONTROL, REMOVE_CONTROL,
  };

  public static final LogisimMenuItem ADD_VHDL = new LogisimMenuItem("AddVhdl");
  public static final LogisimMenuItem IMPORT_VHDL = new LogisimMenuItem("ImportVhdl");
  public static final LogisimMenuItem ADD_CIRCUIT = new LogisimMenuItem("AddCircuit");
  public static final LogisimMenuItem MOVE_CIRCUIT_UP = new LogisimMenuItem("MoveCircuitUp");
  public static final LogisimMenuItem MOVE_CIRCUIT_DOWN = new LogisimMenuItem("MoveCircuitDown");
  public static final LogisimMenuItem SET_MAIN_CIRCUIT = new LogisimMenuItem("SetMainCircuit");
  public static final LogisimMenuItem REMOVE_CIRCUIT = new LogisimMenuItem("RemoveCircuit");
  public static final LogisimMenuItem EDIT_LAYOUT = new LogisimMenuItem("EditLayout");
  public static final LogisimMenuItem EDIT_APPEARANCE = new LogisimMenuItem("EditAppearance");
  public static final LogisimMenuItem TOGGLE_APPEARANCE = new LogisimMenuItem("ToggleEditLayoutAppearance");
  public static final LogisimMenuItem REVERT_APPEARANCE = new LogisimMenuItem("RevertAppearance");
  public static final LogisimMenuItem ANALYZE_CIRCUIT = new LogisimMenuItem("AnalyzeCircuit");

  public static final LogisimMenuItem CIRCUIT_STATS = new LogisimMenuItem("GetCircuitStatistics");
  public static final LogisimMenuItem SIMULATE_STOP = new LogisimMenuItem("SimulateStop");
  public static final LogisimMenuItem SIMULATE_RUN = new LogisimMenuItem("SimulateRun");
  public static final LogisimMenuItem SIMULATE_RUN_TOGGLE = new LogisimMenuItem("SimulateRun");
  public static final LogisimMenuItem SIMULATE_STEP = new LogisimMenuItem("SimulateStep");
  public static final LogisimMenuItem SIMULATE_VHDL_ENABLE = new LogisimMenuItem("SimulateVhdlEnable");
  public static final LogisimMenuItem GENERATE_VHDL_SIM_FILES = new LogisimMenuItem("GenerateVhdlSimFiles");
  public static final LogisimMenuItem TICK_ENABLE = new LogisimMenuItem("TickEnable");
  public static final LogisimMenuItem TICK_HALF = new LogisimMenuItem("TickHalf");
  public static final LogisimMenuItem TICK_FULL = new LogisimMenuItem("TickFull");

  private JFrame parent;
  private MyListener listener;
  private Project proj;
  private SimulateListener simulateListener = null;
  private HashMap<LogisimMenuItem, MenuItem> menuItems = new HashMap<LogisimMenuItem, MenuItem>();
  private ArrayList<ChangeListener> enableListeners;

  public final MenuFile file;
  public final MenuEdit edit;
  public final MenuProject project;
  public final MenuSimulate simulate;
  public final MenuHelp help;
  public final MenuFPGA fpga;

  public LogisimMenuBar(JFrame parent, Project proj) {
    this.parent = parent;
    this.listener = new MyListener();
    this.proj = proj;
    this.enableListeners = new ArrayList<ChangeListener>();
    add(file = new MenuFile(this));
    add(edit = new MenuEdit(this));
    add(project = new MenuProject(this));
    add(simulate = new MenuSimulate(this));
    add(fpga = new MenuFPGA(parent, this, proj));
    add(new WindowMenu(parent));
    add(help = new MenuHelp(this));

    LocaleManager.addLocaleListener(listener);
    listener.localeChanged();
  }

  public void disableFile() {
    file.setEnabled(false);
  }

  public void disableProject() {
    project.setEnabled(false);
  }

  public void addActionListener(LogisimMenuItem which, ActionListener l) {
    MenuItem item = menuItems.get(which);
    if (item != null) item.addActionListener(l);
  }

  public void addEnableListener(ChangeListener l) {
    enableListeners.add(l);
  }

  public void doAction(LogisimMenuItem which) {
    MenuItem item = menuItems.get(which);
    item.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, which.toString()));
  }
  
  public KeyStroke getAccelerator(LogisimMenuItem which) {
    MenuItem item = menuItems.get(which);
    return item == null ? null : item.getAccelerator();
  }



  void fireEnableChanged() {
    ChangeEvent e = new ChangeEvent(this);
    for (ChangeListener listener : enableListeners) {
      listener.stateChanged(e);
    }
  }

  void fireStateChanged(Simulator sim, CircuitState state) {
    if (simulateListener != null) {
      simulateListener.stateChangeRequested(sim, state);
    }
  }

  JFrame getParentWindow() {
    return parent;
  }

  public Project getProject() {
    return proj;
  }

  public boolean isEnabled(LogisimMenuItem item) {
    MenuItem menuItem = menuItems.get(item);
    return menuItem != null && menuItem.isEnabled();
  }

  void registerItem(LogisimMenuItem which, MenuItem item) {
    menuItems.put(which, item);
  }

  public void removeActionListener(LogisimMenuItem which, ActionListener l) {
    MenuItem item = menuItems.get(which);
    if (item != null) item.removeActionListener(l);
  }

  public void removeEnableListener(ChangeListener l) {
    enableListeners.remove(l);
  }

  public void setCircuitState(Simulator sim, CircuitState state) {
    simulate.setCurrentState(sim, state);
  }

  public void setEnabled(LogisimMenuItem which, boolean value) {
    MenuItem item = menuItems.get(which);
    if (item != null) item.setEnabled(value);
  }

  public void setSimulateListener(SimulateListener l) {
    simulateListener = l;
  }
}
