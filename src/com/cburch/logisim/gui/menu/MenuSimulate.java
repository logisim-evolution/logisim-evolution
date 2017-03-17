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
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.gui.test.TestFrame;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.util.StringUtil;

@SuppressWarnings("serial")
public class MenuSimulate extends Menu {

	private class CircuitStateMenuItem extends JMenuItem implements
			CircuitListener, ActionListener {

		private CircuitState circuitState;

		public CircuitStateMenuItem(CircuitState circuitState) {
			this.circuitState = circuitState;

			Circuit circuit = circuitState.getCircuit();
			circuit.addCircuitListener(this);
			this.setText(circuit.getName());
			addActionListener(this);
		}

	    @Override
		public void actionPerformed(ActionEvent e) {
			menubar.fireStateChanged(currentSim, circuitState);
		}

		@Override
		public void circuitChanged(CircuitEvent event) {
			if (event.getAction() == CircuitEvent.ACTION_SET_NAME) {
				this.setText(circuitState.getCircuit().getName());
			}
		}

		void unregister() {
			Circuit circuit = circuitState.getCircuit();
			circuit.removeCircuitListener(this);
		}
	}

	private class MyListener implements ActionListener, SimulatorListener,
			ChangeListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			Project proj = menubar.getProject();
			Simulator sim = proj == null ? null : proj.getSimulator();
			if (src == run || src == LogisimMenuBar.SIMULATE_ENABLE) {
				if (sim != null) {
					sim.setIsRunning(!sim.isRunning());
					proj.repaintCanvas();
				}
			} else if (src == reset) {
				if (sim != null) {

					/* Restart VHDL simulation (in QuestaSim) */
					if (sim.getCircuitState().getProject().getVhdlSimulator()
							.isRunning()) {
						sim.getCircuitState().getProject().getVhdlSimulator()
								.reset();

						/*
						 * We have to wait until the restart finishes, otherwise
						 * the signal reset will be sent to the VHDL simulator
						 * before the sim is loaded and errors will occur Time
						 * (0,5s) is arbitrary
						 * 
						 * FIXME: if you find a way to make a blocking reset
						 * until it's restarted, feel free to go on
						 */
						try {
							Thread.sleep(500);
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}

					sim.requestReset();
				}
			} else if (src == step || src == LogisimMenuBar.SIMULATE_STEP) {
				if (sim != null) {
					sim.step();
				}
			} else if (src == tickOnce || src == LogisimMenuBar.TICK_STEP) {
				if (sim != null) {
					sim.tick();
				}
			} else if (src == simulate_vhdl_enable
					|| src == LogisimMenuBar.SIMULATE_VHDL_ENABLE) {
				if (proj.getVhdlSimulator() != null) {
					proj.getVhdlSimulator().setEnabled(
							!proj.getVhdlSimulator().isEnabled());
				}
			} else if (src == vhdl_sim_files
					|| src == LogisimMenuBar.GENERATE_VHDL_SIM_FILES) {
				if (proj.getVhdlSimulator() != null) {
					proj.getVhdlSimulator().restart();
				}
			} else if (src == tickOnce || src == LogisimMenuBar.TICK_STEP_MAIN) {
				int ticks = 0;
				for (com.cburch.logisim.comp.Component clock : proj
						.getLogisimFile().getMainCircuit().getClocks()) {
					if (clock.getAttributeSet().getValue(StdAttr.LABEL)
							.contentEquals("clk")) {
						if (proj.getOptions().getAttributeSet()
								.getValue(Options.ATTR_TICK_MAIN)
								.equals(Options.TICK_MAIN_HALF_PERIOD)) {
							if (currentState.getValue(clock.getLocation())
									.toIntValue() == 0) {
								ticks = clock.getAttributeSet().getValue(
										Clock.ATTR_LOW);
							} else {
								ticks = clock.getAttributeSet().getValue(
										Clock.ATTR_HIGH);
							}
						} else {
							ticks = clock.getAttributeSet().getValue(
									Clock.ATTR_LOW)
									+ clock.getAttributeSet().getValue(
											Clock.ATTR_HIGH);
						}
						break;
					}
				}
				sim.tickMain(ticks);
			} else if (src == ticksEnabled || src == LogisimMenuBar.TICK_ENABLE) {
				if (sim != null) {
					sim.setIsTicking(!sim.isTicking());
				}
			} else if (src == log) {
				LogFrame frame = menubar.getProject().getLogFrame(true);
				frame.setVisible(true);
			} else if (src == assemblyWindow) {
				if (assWin == null || assWin.isVisible() == false) {
					assWin = new AssemblyWindow(proj);
					assWin.setVisible(true);
				} else {
					assWin.toFront();
				}
			} else if (src == test) {
				TestFrame frame = menubar.getProject().getTestFrame(true);
				frame.setVisible(true);
			}
		}

		@Override
		public void propagationCompleted(SimulatorEvent e) {
		}

		@Override
		public void simulatorStateChanged(SimulatorEvent e) {
			Simulator sim = e.getSource();
			if (sim != currentSim) {
				return;
			}
			computeEnabled();
			run.setSelected(sim.isRunning());
			ticksEnabled.setSelected(sim.isTicking());
			double freq = sim.getTickFrequency();
			for (int i = 0; i < tickFreqs.length; i++) {
				TickFrequencyChoice item = tickFreqs[i];
				item.setSelected(freq == item.freq);
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			step.setEnabled(run.isEnabled() && !run.isSelected());
		}

		@Override
		public void tickCompleted(SimulatorEvent e) {
		}
	}

    private class TickFrequencyChoice extends JRadioButtonMenuItem implements
			ActionListener {

		private double freq;

		public TickFrequencyChoice(double value) {
			freq = value;
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (currentSim != null) {
				currentSim.setTickFrequency(freq);
			}
		}

		public void localeChanged() {
			double f = freq;
			if (f < 1000) {
				String hzStr;
				if (Math.abs(f - Math.round(f)) < 0.0001) {
					hzStr = "" + (int) Math.round(f);
				} else {
					hzStr = "" + f;
				}
				setText(StringUtil.format(Strings.get("simulateTickFreqItem"),
						hzStr));
			} else {
				String kHzStr;
				double kf = Math.round(f / 100) / 10.0;
				if (kf == Math.round(kf)) {
					kHzStr = "" + (int) kf;
				} else {
					kHzStr = "" + kf;
				}
				setText(StringUtil.format(Strings.get("simulateTickKFreqItem"),
						kHzStr));
			}
		}
	}

	public static ArrayList<String> getTickFrequencyStrings() {
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < SupportedTickFrequencies.length; i++) {
			if (SupportedTickFrequencies[i] < 1000) {
				String hzStr;
				if (Math.abs(SupportedTickFrequencies[i]
						- Math.round(SupportedTickFrequencies[i])) < 0.0001) {
					hzStr = "" + (int) Math.round(SupportedTickFrequencies[i]);
				} else {
					hzStr = "" + SupportedTickFrequencies[i];
				}
				result.add(StringUtil.format(
						Strings.get("simulateTickFreqItem"), hzStr));
			} else {
				String kHzStr;
				double kf = Math.round(SupportedTickFrequencies[i] / 100) / 10.0;
				if (kf == Math.round(kf)) {
					kHzStr = "" + (int) kf;
				} else {
					kHzStr = "" + kf;
				}
				result.add(StringUtil.format(
						Strings.get("simulateTickKFreqItem"), kHzStr));
			}

		}
		return result;
	}

	public static final Double[] SupportedTickFrequencies = { 4096.0, 2048.0,
			1024.0, 512.0, 256.0, 128.0, 64.0, 32.0, 16.0, 8.0, 4.0, 2.0, 1.0,
			0.5, 0.25 };
	private LogisimMenuBar menubar;
	private MyListener myListener = new MyListener();
	private CircuitState currentState = null;
	private CircuitState bottomState = null;
	private Simulator currentSim = null;
	private MenuItemCheckImpl run;
	private JMenuItem reset = new JMenuItem();
	private MenuItemImpl step;
	private MenuItemImpl vhdl_sim_files;
	private MenuItemCheckImpl simulate_vhdl_enable;
	private MenuItemCheckImpl ticksEnabled;
	private MenuItemImpl tickOnce;
	private MenuItemImpl tickOnceMain;
	private JMenu tickFreq = new JMenu();
	private TickFrequencyChoice[] tickFreqs = new TickFrequencyChoice[SupportedTickFrequencies.length];
	private JMenu downStateMenu = new JMenu();
	private ArrayList<CircuitStateMenuItem> downStateItems = new ArrayList<CircuitStateMenuItem>();
	private JMenu upStateMenu = new JMenu();
	private ArrayList<CircuitStateMenuItem> upStateItems = new ArrayList<CircuitStateMenuItem>();
	private JMenuItem log = new JMenuItem();
	private JMenuItem test = new JMenuItem();
	private JMenuItem assemblyWindow = new JMenuItem();

	AssemblyWindow assWin = null;

	public MenuSimulate(LogisimMenuBar menubar) {
		this.menubar = menubar;
		run = new MenuItemCheckImpl(this, LogisimMenuBar.SIMULATE_ENABLE);
		step = new MenuItemImpl(this, LogisimMenuBar.SIMULATE_STEP);
		simulate_vhdl_enable = new MenuItemCheckImpl(this,
				LogisimMenuBar.SIMULATE_VHDL_ENABLE);
		vhdl_sim_files = new MenuItemImpl(this,
				LogisimMenuBar.GENERATE_VHDL_SIM_FILES);
		ticksEnabled = new MenuItemCheckImpl(this, LogisimMenuBar.TICK_ENABLE);
		tickOnce = new MenuItemImpl(this, LogisimMenuBar.TICK_STEP);
		tickOnceMain = new MenuItemImpl(this, LogisimMenuBar.TICK_STEP_MAIN);

		menubar.registerItem(LogisimMenuBar.SIMULATE_ENABLE, run);
		menubar.registerItem(LogisimMenuBar.SIMULATE_STEP, step);
		menubar.registerItem(LogisimMenuBar.SIMULATE_VHDL_ENABLE,
				simulate_vhdl_enable);
		menubar.registerItem(LogisimMenuBar.GENERATE_VHDL_SIM_FILES,
				vhdl_sim_files);
		menubar.registerItem(LogisimMenuBar.TICK_ENABLE, ticksEnabled);
		menubar.registerItem(LogisimMenuBar.TICK_STEP, tickOnce);
		menubar.registerItem(LogisimMenuBar.TICK_STEP_MAIN, tickOnceMain);

		int menuMask = getToolkit().getMenuShortcutKeyMask();
		run.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, menuMask));
		reset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, menuMask));
		step.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, menuMask));
		tickOnce.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, menuMask));
		tickOnceMain.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		ticksEnabled.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K,
				menuMask));

		ButtonGroup bgroup = new ButtonGroup();
		for (int i = 0; i < SupportedTickFrequencies.length; i++) {
			tickFreqs[i] = new TickFrequencyChoice(SupportedTickFrequencies[i]);
			bgroup.add(tickFreqs[i]);
			tickFreq.add(tickFreqs[i]);
		}

		add(run);
		add(reset);
		add(step);
		add(simulate_vhdl_enable);
		add(vhdl_sim_files);
		addSeparator();
		add(upStateMenu);
		add(downStateMenu);
		addSeparator();
		add(tickOnce);
		add(tickOnceMain);
		add(ticksEnabled);
		add(tickFreq);
		addSeparator();
		add(log);
		add(test);
		addSeparator();
		add(assemblyWindow);

		setEnabled(false);
		run.setEnabled(false);
		reset.setEnabled(false);
		step.setEnabled(false);
		simulate_vhdl_enable.setEnabled(false);
		vhdl_sim_files.setEnabled(false);
		upStateMenu.setEnabled(false);
		downStateMenu.setEnabled(false);
		tickOnce.setEnabled(false);
		tickOnceMain.setEnabled(false);
		ticksEnabled.setEnabled(false);
		tickFreq.setEnabled(false);

		run.addChangeListener(myListener);
		menubar.addActionListener(LogisimMenuBar.SIMULATE_ENABLE, myListener);
		menubar.addActionListener(LogisimMenuBar.SIMULATE_STEP, myListener);
		menubar.addActionListener(LogisimMenuBar.SIMULATE_VHDL_ENABLE,
				myListener);
		menubar.addActionListener(LogisimMenuBar.GENERATE_VHDL_SIM_FILES,
				myListener);
		menubar.addActionListener(LogisimMenuBar.TICK_ENABLE, myListener);
		menubar.addActionListener(LogisimMenuBar.TICK_STEP, myListener);
		menubar.addActionListener(LogisimMenuBar.TICK_STEP_MAIN, myListener);
		// run.addActionListener(myListener);
		reset.addActionListener(myListener);
		// step.addActionListener(myListener);
		// tickOnce.addActionListener(myListener);
		// ticksEnabled.addActionListener(myListener);
		log.addActionListener(myListener);
		test.addActionListener(myListener);
		assemblyWindow.addActionListener(myListener);

		computeEnabled();
	}

	private void clearItems(ArrayList<CircuitStateMenuItem> items) {
		for (CircuitStateMenuItem item : items) {
			item.unregister();
		}
		items.clear();
	}

	@Override
	void computeEnabled() {
		boolean present = currentState != null;
		Simulator sim = this.currentSim;
		boolean simRunning = sim != null && sim.isRunning();
		setEnabled(present);
		run.setEnabled(present);
		reset.setEnabled(present);
		step.setEnabled(present && !simRunning);
		simulate_vhdl_enable.setEnabled(present);
		vhdl_sim_files.setEnabled(present);
		upStateMenu.setEnabled(present);
		downStateMenu.setEnabled(present);
		tickOnce.setEnabled(present);
		tickOnceMain.setEnabled(present);
		ticksEnabled.setEnabled(present && simRunning);
		tickFreq.setEnabled(present);
		menubar.fireEnableChanged();
	}

	public void localeChanged() {
		this.setText(Strings.get("simulateMenu"));
		run.setText(Strings.get("simulateRunItem"));
		reset.setText(Strings.get("simulateResetItem"));
		step.setText(Strings.get("simulateStepItem"));
		simulate_vhdl_enable.setText(Strings.get("simulateVhdlEnableItem"));
		vhdl_sim_files.setText(Strings.get("simulateGenVhdlFilesItem"));
		tickOnce.setText(Strings.get("simulateTickOnceItem"));
		tickOnceMain.setText(Strings.get("simulateTickOnceMainItem"));
		ticksEnabled.setText(Strings.get("simulateTickItem"));
		tickFreq.setText(Strings.get("simulateTickFreqMenu"));
		for (int i = 0; i < tickFreqs.length; i++) {
			tickFreqs[i].localeChanged();
		}
		downStateMenu.setText(Strings.get("simulateDownStateMenu"));
		upStateMenu.setText(Strings.get("simulateUpStateMenu"));
		log.setText(Strings.get("simulateLogItem"));
		test.setText(Strings.get("simulateTestItem"));
		assemblyWindow.setText("Assembly viewer");
	}

	private void recreateStateMenu(JMenu menu,
			ArrayList<CircuitStateMenuItem> items, int code) {
		menu.removeAll();
		menu.setEnabled(items.size() > 0);
		boolean first = true;
		int mask = getToolkit().getMenuShortcutKeyMask();
		for (int i = items.size() - 1; i >= 0; i--) {
			JMenuItem item = items.get(i);
			menu.add(item);
			if (first) {
				item.setAccelerator(KeyStroke.getKeyStroke(code, mask));
				first = false;
			} else {
				item.setAccelerator(null);
			}
		}
	}

	private void recreateStateMenus() {
		recreateStateMenu(downStateMenu, downStateItems, KeyEvent.VK_RIGHT);
		recreateStateMenu(upStateMenu, upStateItems, KeyEvent.VK_LEFT);
	}

	public void setCurrentState(Simulator sim, CircuitState value) {
		if (currentState == value) {
			return;
		}
		Simulator oldSim = currentSim;
		CircuitState oldState = currentState;
		currentSim = sim;
		currentState = value;
		if (bottomState == null) {
			bottomState = currentState;
		} else if (currentState == null) {
			bottomState = null;
		} else {
			CircuitState cur = bottomState;
			while (cur != null && cur != currentState) {
				cur = cur.getParentState();
			}
			if (cur == null) {
				bottomState = currentState;
			}
		}

		boolean oldPresent = oldState != null;
		boolean present = currentState != null;
		if (oldPresent != present) {
			computeEnabled();
		}

		if (currentSim != oldSim) {
			double freq = currentSim == null ? 1.0 : currentSim
					.getTickFrequency();
			for (int i = 0; i < tickFreqs.length; i++) {
				tickFreqs[i]
						.setSelected(Math.abs(tickFreqs[i].freq - freq) < 0.001);
			}

			if (oldSim != null) {
				oldSim.removeSimulatorListener(myListener);
			}
			if (currentSim != null) {
				currentSim.addSimulatorListener(myListener);
			}
			myListener.simulatorStateChanged(new SimulatorEvent(sim));
		}

		clearItems(downStateItems);
		CircuitState cur = bottomState;
		while (cur != null && cur != currentState) {
			downStateItems.add(new CircuitStateMenuItem(cur));
			cur = cur.getParentState();
		}
		if (cur != null) {
			cur = cur.getParentState();
		}
		clearItems(upStateItems);
		while (cur != null) {
			upStateItems.add(0, new CircuitStateMenuItem(cur));
			cur = cur.getParentState();
		}
		recreateStateMenus();
	}
}
