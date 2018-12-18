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

package com.cburch.logisim.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.cburch.logisim.Main;
import com.cburch.logisim.proj.Project;

class MenuProject extends Menu {
	private class MyListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Object src = event.getSource();
			Project proj = menubar.getProject();
			if (src == loadBuiltin) {
				ProjectLibraryActions.doLoadBuiltinLibrary(proj);
			} else if (src == loadLogisim) {
				ProjectLibraryActions.doLoadLogisimLibrary(proj);
			} else if (src == loadJar) {
				ProjectLibraryActions.doLoadJarLibrary(proj);
			} else if (src == unload) {
				ProjectLibraryActions.doUnloadLibraries(proj);
			} else if (src == options) {
				JFrame frame = proj.getOptionsFrame(true);
				frame.setVisible(true);
			}
		}
	}

	private static final long serialVersionUID = 1L;

	private LogisimMenuBar menubar;
	private MyListener myListener = new MyListener();

	private MenuItemImpl addCircuit = new MenuItemImpl(this,
			LogisimMenuBar.ADD_CIRCUIT);
	private JMenu loadLibrary = new JMenu();
	private JMenuItem loadBuiltin = new JMenuItem();
	private JMenuItem loadLogisim = new JMenuItem();
	private JMenuItem loadJar = new JMenuItem();
	private JMenuItem unload = new JMenuItem();
	private MenuItemImpl moveUp = new MenuItemImpl(this,
			LogisimMenuBar.MOVE_CIRCUIT_UP);
	private MenuItemImpl moveDown = new MenuItemImpl(this,
			LogisimMenuBar.MOVE_CIRCUIT_DOWN);
	private MenuItemImpl remove = new MenuItemImpl(this,
			LogisimMenuBar.REMOVE_CIRCUIT);
	private MenuItemImpl setAsMain = new MenuItemImpl(this,
			LogisimMenuBar.SET_MAIN_CIRCUIT);
	private MenuItemImpl revertAppearance = new MenuItemImpl(this,
			LogisimMenuBar.REVERT_APPEARANCE);
	private MenuItemImpl layout = new MenuItemImpl(this,
			LogisimMenuBar.EDIT_LAYOUT);
	private MenuItemImpl appearance = new MenuItemImpl(this,
			LogisimMenuBar.EDIT_APPEARANCE);
	private MenuItemImpl viewToolbox = new MenuItemImpl(this,
			LogisimMenuBar.VIEW_TOOLBOX);
	private MenuItemImpl viewSimulation = new MenuItemImpl(this,
			LogisimMenuBar.VIEW_SIMULATION);
	private MenuItemImpl analyze = new MenuItemImpl(this,
			LogisimMenuBar.ANALYZE_CIRCUIT);
	private MenuItemImpl stats = new MenuItemImpl(this,
			LogisimMenuBar.CIRCUIT_STATS);
	private JMenuItem options = new JMenuItem();

	MenuProject(LogisimMenuBar menubar) {
		this.menubar = menubar;

		menubar.registerItem(LogisimMenuBar.ADD_CIRCUIT, addCircuit);
		loadBuiltin.addActionListener(myListener);
		loadLogisim.addActionListener(myListener);
		loadJar.addActionListener(myListener);
		unload.addActionListener(myListener);
		menubar.registerItem(LogisimMenuBar.MOVE_CIRCUIT_UP, moveUp);
		menubar.registerItem(LogisimMenuBar.MOVE_CIRCUIT_DOWN, moveDown);
		menubar.registerItem(LogisimMenuBar.SET_MAIN_CIRCUIT, setAsMain);
		menubar.registerItem(LogisimMenuBar.REMOVE_CIRCUIT, remove);
		menubar.registerItem(LogisimMenuBar.REVERT_APPEARANCE, revertAppearance);
		menubar.registerItem(LogisimMenuBar.EDIT_LAYOUT, layout);
		menubar.registerItem(LogisimMenuBar.EDIT_APPEARANCE, appearance);
		menubar.registerItem(LogisimMenuBar.VIEW_TOOLBOX, viewToolbox);
		menubar.registerItem(LogisimMenuBar.VIEW_SIMULATION, viewSimulation);
		if (Main.ANALYZE) {
			menubar.registerItem(LogisimMenuBar.ANALYZE_CIRCUIT, analyze);
		}
		menubar.registerItem(LogisimMenuBar.CIRCUIT_STATS, stats);
		options.addActionListener(myListener);

		loadLibrary.add(loadBuiltin);
		loadLibrary.add(loadLogisim);
		loadLibrary.add(loadJar);

		add(addCircuit);
		add(loadLibrary);
		add(unload);
		addSeparator();
		add(moveUp);
		add(moveDown);
		add(setAsMain);
		add(remove);
		add(revertAppearance);
		addSeparator();
		add(viewToolbox);
		add(viewSimulation);
		add(layout);
		add(appearance);
		addSeparator();
		if (Main.ANALYZE) {
			add(analyze);
		}
		add(stats);
		addSeparator();
		add(options);

		boolean known = menubar.getProject() != null;
		loadLibrary.setEnabled(known);
		loadBuiltin.setEnabled(known);
		loadLogisim.setEnabled(known);
		loadJar.setEnabled(known);
		unload.setEnabled(known);
		options.setEnabled(known);
		computeEnabled();
	}

    @Override
	void computeEnabled() {
		setEnabled(menubar.getProject() != null || addCircuit.hasListeners()
				|| moveUp.hasListeners() || moveDown.hasListeners()
				|| setAsMain.hasListeners() || remove.hasListeners()
				|| layout.hasListeners() || revertAppearance.hasListeners()
				|| appearance.hasListeners() || viewToolbox.hasListeners()
				|| viewSimulation.hasListeners() || analyze.hasListeners()
				|| stats.hasListeners());
		menubar.fireEnableChanged();
	}

	public void localeChanged() {
		setText(Strings.get("projectMenu"));
		addCircuit.setText(Strings.get("projectAddCircuitItem"));
		loadLibrary.setText(Strings.get("projectLoadLibraryItem"));
		loadBuiltin.setText(Strings.get("projectLoadBuiltinItem"));
		loadLogisim.setText(Strings.get("projectLoadLogisimItem"));
		loadJar.setText(Strings.get("projectLoadJarItem"));
		unload.setText(Strings.get("projectUnloadLibrariesItem"));
		moveUp.setText(Strings.get("projectMoveCircuitUpItem"));
		moveDown.setText(Strings.get("projectMoveCircuitDownItem"));
		setAsMain.setText(Strings.get("projectSetAsMainItem"));
		remove.setText(Strings.get("projectRemoveCircuitItem"));
		revertAppearance.setText(Strings.get("projectRevertAppearanceItem"));
		layout.setText(Strings.get("projectEditCircuitLayoutItem"));
		appearance.setText(Strings.get("projectEditCircuitAppearanceItem"));
		viewToolbox.setText(Strings.get("projectViewToolboxItem"));
		viewSimulation.setText(Strings.get("projectViewSimulationItem"));
		analyze.setText(Strings.get("projectAnalyzeCircuitItem"));
		stats.setText(Strings.get("projectGetCircuitStatisticsItem"));
		options.setText(Strings.get("projectOptionsItem"));
	}
}
