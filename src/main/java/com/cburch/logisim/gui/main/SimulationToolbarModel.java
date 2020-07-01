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

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.gui.icons.SimulationIcon;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.UnmodifiableList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class SimulationToolbarModel extends AbstractToolbarModel implements ChangeListener {
  private Project project;
  private LogisimToolbarItem simRunToggle;
  private LogisimToolbarItem simStep;
  private LogisimToolbarItem tickEnable;
  private LogisimToolbarItem tickHalf;
  private LogisimToolbarItem tickFull;
  private List<ToolbarItem> items;
  
  private static final SimulationIcon RunToggleIcon = new SimulationIcon(SimulationIcon.SIM_PLAY);
  private static final SimulationIcon EnableDisableIcon = new SimulationIcon(SimulationIcon.SIM_ENABLE);

  public SimulationToolbarModel(Project project, MenuListener menu) {
    this.project = project;

    simRunToggle = new LogisimToolbarItem(menu, RunToggleIcon,
            LogisimMenuBar.SIMULATE_RUN_TOGGLE, S.getter("simulateRunTip"));
    simStep = new LogisimToolbarItem(menu, new SimulationIcon(SimulationIcon.SIM_STEP), 
    		LogisimMenuBar.SIMULATE_STEP, S.getter("simulateStepTip"));
    tickEnable = new LogisimToolbarItem(menu, EnableDisableIcon, 
    		LogisimMenuBar.TICK_ENABLE, S.getter("simulateEnableTicksTip"));
    tickHalf = new LogisimToolbarItem(menu, new SimulationIcon(SimulationIcon.SIM_HALF_TICK),
            LogisimMenuBar.TICK_HALF, S.getter("simulateTickHalfTip"));
    tickFull = new LogisimToolbarItem(menu,  new SimulationIcon(SimulationIcon.SIM_FULL_TICK),
            LogisimMenuBar.TICK_FULL, S.getter("simulateTickFullTip"));
    
    items = UnmodifiableList.create(new ToolbarItem[] {simRunToggle, simStep, tickEnable, tickHalf, tickFull, });

    menu.getMenuBar().addEnableListener(this);
    stateChanged(null);
  }

  @Override
  public List<ToolbarItem> getItems() {
    return items;
  }

  @Override
  public boolean isSelected(ToolbarItem item) {
    return false;
  }

  @Override
  public void itemSelected(ToolbarItem item) {
    if (item instanceof LogisimToolbarItem) {
      ((LogisimToolbarItem) item).doAction();
    }
  }

  public void stateChanged(ChangeEvent e) {
    Simulator sim = project.getSimulator();
    boolean running = sim != null && sim.isRunning();
    boolean ticking = sim != null && sim.isTicking();
    if (running) {
      RunToggleIcon.setType(SimulationIcon.SIM_PAUSE);
      simRunToggle.setToolTip(S.getter("simulateStopTip"));
    } else {
      RunToggleIcon.setType(SimulationIcon.SIM_PLAY);
      simRunToggle.setToolTip(S.getter("simulateRunTip"));
    }
    if (ticking) {
      EnableDisableIcon.setType(SimulationIcon.SIM_DISABLE);    
      tickEnable.setToolTip(S.getter("simulateDisableTicksTip"));
    } else {
      EnableDisableIcon.setType(SimulationIcon.SIM_ENABLE);    
      tickEnable.setToolTip(S.getter("simulateEnableTicksTip"));
    }
    fireToolbarAppearanceChanged();
  }
}
