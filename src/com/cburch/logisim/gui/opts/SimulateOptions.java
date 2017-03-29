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

package com.cburch.logisim.gui.opts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.TableLayout;

class SimulateOptions extends OptionsPanel {
	private class MyListener implements ActionListener, AttributeListener {
		public void actionPerformed(ActionEvent event) {
			Object source = event.getSource();
			if (source == simLimit) {
				Integer opt = (Integer) simLimit.getSelectedItem();
				if (opt != null) {
					AttributeSet attrs = getOptions().getAttributeSet();
					getProject().doAction(
							OptionsActions.setAttribute(attrs,
									Options.sim_limit_attr, opt));
				}
			} else if (source == simRandomness) {
				AttributeSet attrs = getOptions().getAttributeSet();
				Object val = simRandomness.isSelected() ? Options.sim_rand_dflt
						: Integer.valueOf(0);
				getProject().doAction(
						OptionsActions.setAttribute(attrs,
								Options.sim_rand_attr, val));
			} else if (source == gateUndefined) {
				ComboOption opt = (ComboOption) gateUndefined.getSelectedItem();
				if (opt != null) {
					AttributeSet attrs = getOptions().getAttributeSet();
					getProject()
							.doAction(
									OptionsActions.setAttribute(attrs,
											Options.ATTR_GATE_UNDEFINED,
											opt.getValue()));
				}
			} else if (source == tickMainStep) {
				ComboOption opt = (ComboOption) tickMainStep.getSelectedItem();
				if (opt != null) {
					AttributeSet attrs = getOptions().getAttributeSet();
					getProject().doAction(
							OptionsActions.setAttribute(attrs,
									Options.ATTR_TICK_MAIN, opt.getValue()));
				}
			} else if (source == MemUnknown) {
				AppPreferences.Memory_Startup_Unknown.set(MemUnknown.isSelected());
				Simulator sim = getProject().getSimulator();
				if (sim != null)
					sim.requestReset();
			}
		}

		public void attributeListChanged(AttributeEvent e) {
		}

		public void attributeValueChanged(AttributeEvent e) {
			Attribute<?> attr = e.getAttribute();
			Object val = e.getValue();
			if (attr == Options.sim_limit_attr) {
				loadSimLimit((Integer) val);
			} else if (attr == Options.sim_rand_attr) {
				loadSimRandomness((Integer) val);
			}
		}

		private void loadGateUndefined(Object val) {
			ComboOption.setSelected(gateUndefined, val);
		}

		@SuppressWarnings("rawtypes")
		private void loadSimLimit(Integer val) {
			int value = val.intValue();
			ComboBoxModel model = simLimit.getModel();
			for (int i = 0; i < model.getSize(); i++) {
				Integer opt = (Integer) model.getElementAt(i);
				if (opt.intValue() == value) {
					simLimit.setSelectedItem(opt);
				}
			}
		}

		private void loadSimRandomness(Integer val) {
			simRandomness.setSelected(val.intValue() > 0);
		}

		private void loadTickMain(Object val) {
			ComboOption.setSelected(tickMainStep, val);
		}
	}

	private static final long serialVersionUID = 1L;

	private MyListener myListener = new MyListener();

	private JLabel simLimitLabel = new JLabel();
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JComboBox simLimit = new JComboBox(new Integer[] {
			Integer.valueOf(200), Integer.valueOf(500), Integer.valueOf(1000),
			Integer.valueOf(2000), Integer.valueOf(5000),
			Integer.valueOf(10000), Integer.valueOf(20000),
			Integer.valueOf(50000), });
	private JCheckBox simRandomness = new JCheckBox();
	private JCheckBox MemUnknown = new JCheckBox();
	private JLabel gateUndefinedLabel = new JLabel();
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JComboBox gateUndefined = new JComboBox(new Object[] {
			new ComboOption(Options.GATE_UNDEFINED_IGNORE),
			new ComboOption(Options.GATE_UNDEFINED_ERROR) });
	private JLabel tickMainLabel = new JLabel();
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JComboBox tickMainStep = new JComboBox(new Object[] {
			new ComboOption(Options.TICK_MAIN_HALF_PERIOD),
			new ComboOption(Options.TICK_MAIN_PERIOD) });

	public SimulateOptions(OptionsFrame window) {
		super(window);


		JPanel simLimitPanel = new JPanel();
		simLimitPanel.add(simLimitLabel);
		simLimitPanel.add(simLimit);
		simLimit.addActionListener(myListener);

		JPanel gateUndefinedPanel = new JPanel();
		gateUndefinedPanel.add(gateUndefinedLabel);
		gateUndefinedPanel.add(gateUndefined);
		gateUndefined.addActionListener(myListener);

		simRandomness.addActionListener(myListener);
		
		MemUnknown.addActionListener(myListener);
		MemUnknown.setSelected(AppPreferences.Memory_Startup_Unknown.get());

		JPanel tickMainPanel = new JPanel();
		tickMainPanel.add(tickMainLabel);
		tickMainPanel.add(tickMainStep);
		tickMainStep.addActionListener(myListener);

		setLayout(new TableLayout(1));
		add(MemUnknown);
		add(simLimitPanel);
		add(gateUndefinedPanel);
		add(simRandomness);
		add(tickMainPanel);

		window.getOptions().getAttributeSet().addAttributeListener(myListener);
		AttributeSet attrs = getOptions().getAttributeSet();
		myListener.loadSimLimit(attrs.getValue(Options.sim_limit_attr));
		myListener.loadGateUndefined(attrs
				.getValue(Options.ATTR_GATE_UNDEFINED));
		myListener.loadSimRandomness(attrs.getValue(Options.sim_rand_attr));
		myListener.loadTickMain(attrs.getValue(Options.ATTR_TICK_MAIN));
	}

	@Override
	public String getHelpText() {
		return Strings.get("simulateHelp");
	}

	@Override
	public String getTitle() {
		return Strings.get("simulateTitle");
	}

	@Override
	public void localeChanged() {
		simLimitLabel.setText(Strings.get("simulateLimit"));
		gateUndefinedLabel.setText(Strings.get("gateUndefined"));
		simRandomness.setText(Strings.get("simulateRandomness"));
		tickMainLabel.setText(Strings.get("mainTick"));
		MemUnknown.setText(Strings.get("MemoriesStartupUnknown"));
	}
}
