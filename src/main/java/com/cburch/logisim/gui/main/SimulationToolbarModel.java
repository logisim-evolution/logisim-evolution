/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.gui.icons.SimulationIcon;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.UnmodifiableList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SimulationToolbarModel extends AbstractToolbarModel implements ChangeListener {
  private static final SimulationIcon RunToggleIcon = new SimulationIcon(SimulationIcon.SIM_PLAY);
  private static final SimulationIcon EnableDisableIcon = new SimulationIcon(SimulationIcon.SIM_ENABLE);
  private final Project project;
  private final LogisimToolbarItem simRunToggle;
  private final LogisimToolbarItem simStep;
  private final LogisimToolbarItem tickEnable;
  private final LogisimToolbarItem tickHalf;
  private final LogisimToolbarItem tickFull;
  private final List<ToolbarItem> items;

  public SimulationToolbarModel(Project project, MenuListener menu) {
    this.project = project;

    simRunToggle =
        new LogisimToolbarItem(
            menu, RunToggleIcon, LogisimMenuBar.SIMULATE_RUN_TOGGLE, S.getter("simulateRunTip"));
    simStep =
        new LogisimToolbarItem(
            menu,
            new SimulationIcon(SimulationIcon.SIM_STEP),
            LogisimMenuBar.SIMULATE_STEP,
            S.getter("simulateStepTip"));
    tickEnable =
        new LogisimToolbarItem(
            menu,
            EnableDisableIcon,
            LogisimMenuBar.TICK_ENABLE,
            S.getter("simulateEnableTicksTip"));
    tickHalf =
        new LogisimToolbarItem(
            menu,
            new SimulationIcon(SimulationIcon.SIM_HALF_TICK),
            LogisimMenuBar.TICK_HALF,
            S.getter("simulateTickHalfTip"));
    tickFull =
        new LogisimToolbarItem(
            menu,
            new SimulationIcon(SimulationIcon.SIM_FULL_TICK),
            LogisimMenuBar.TICK_FULL,
            S.getter("simulateTickFullTip"));

    items =
        UnmodifiableList.create(
            new ToolbarItem[] {
              simRunToggle, simStep, tickEnable, tickHalf, tickFull,
            });

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
    if (item instanceof LogisimToolbarItem toolbarItem) {
      toolbarItem.doAction();
    }
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    final var sim = project.getSimulator();
    final var running = sim != null && sim.isAutoPropagating();
    final var ticking = sim != null && sim.isAutoTicking();
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
