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

package com.cburch.logisim.gui.main;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.UnmodifiableList;

class SimulationToolbarModel extends AbstractToolbarModel implements
		ChangeListener {
	private Project project;
	private LogisimToolbarItem simEnable;
	private LogisimToolbarItem simStep;
	private LogisimToolbarItem tickEnable;
	private LogisimToolbarItem tickStep;
	private LogisimToolbarItem tickStepMain;
	private List<ToolbarItem> items;

	public SimulationToolbarModel(Project project, MenuListener menu) {
		this.project = project;

		simEnable = new LogisimToolbarItem(menu, "simplay.png",
				LogisimMenuBar.SIMULATE_ENABLE,
				Strings.getter("simulateEnableStepsTip"));
		simStep = new LogisimToolbarItem(menu, "simstep.png",
				LogisimMenuBar.SIMULATE_STEP, Strings.getter("simulateStepTip"));
		tickEnable = new LogisimToolbarItem(menu, "simtplay.png",
				LogisimMenuBar.TICK_ENABLE,
				Strings.getter("simulateEnableTicksTip"));
		tickStep = new LogisimToolbarItem(menu, "simtstep.png",
				LogisimMenuBar.TICK_STEP, Strings.getter("simulateTickTip"));
		tickStepMain = new LogisimToolbarItem(menu, "clock.gif",
				LogisimMenuBar.TICK_STEP_MAIN,
				Strings.getter("simulateTickMainTip"));

		items = UnmodifiableList.create(new ToolbarItem[] { simEnable, simStep,
				tickEnable, tickStep, tickStepMain, });

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

	//
	// ChangeListener methods
	//
	public void stateChanged(ChangeEvent e) {
		Simulator sim = project.getSimulator();
		boolean running = sim != null && sim.isRunning();
		boolean ticking = sim != null && sim.isTicking();
		simEnable.setIcon(running ? "simstop.png" : "simplay.png");
		simEnable.setToolTip(running ? Strings
				.getter("simulateDisableStepsTip") : Strings
				.getter("simulateEnableStepsTip"));
		tickEnable.setIcon(ticking ? "simtstop.png" : "simtplay.png");
		tickEnable.setToolTip(ticking ? Strings
				.getter("simulateDisableTicksTip") : Strings
				.getter("simulateEnableTicksTip"));
		fireToolbarAppearanceChanged();
	}
}
