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

package com.cburch.logisim.gui.opts;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.TableLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class SimulateOptions extends OptionsPanel {
  private class MyListener implements ActionListener, AttributeListener {
    public void actionPerformed(ActionEvent event) {
      Object source = event.getSource();
      if (source == simLimit) {
        Integer opt = (Integer) simLimit.getSelectedItem();
        if (opt != null) {
          AttributeSet attrs = getOptions().getAttributeSet();
          getProject().doAction(OptionsActions.setAttribute(attrs, Options.ATTR_SIM_LIMIT, opt));
        }
      } else if (source == simRandomness) {
        AttributeSet attrs = getOptions().getAttributeSet();
        Object val = simRandomness.isSelected() ? Options.sim_rand_dflt : Integer.valueOf(0);
        getProject().doAction(OptionsActions.setAttribute(attrs, Options.ATTR_SIM_RAND, val));
      } else if (source == gateUndefined) {
        ComboOption opt = (ComboOption) gateUndefined.getSelectedItem();
        if (opt != null) {
          AttributeSet attrs = getOptions().getAttributeSet();
          getProject().doAction(
                  OptionsActions.setAttribute(attrs, Options.ATTR_GATE_UNDEFINED, opt.getValue()));
        }
      } else if (source == MemUnknown) {
        AppPreferences.Memory_Startup_Unknown.set(MemUnknown.isSelected());
        Simulator sim = getProject().getSimulator();
        if (sim != null) sim.requestReset();
      }
    }

    public void attributeListChanged(AttributeEvent e) {}

    public void attributeValueChanged(AttributeEvent e) {
      Attribute<?> attr = e.getAttribute();
      Object val = e.getValue();
      if (attr == Options.ATTR_SIM_LIMIT) {
        loadSimLimit((Integer) val);
      } else if (attr == Options.ATTR_SIM_RAND) {
        loadSimRandomness((Integer) val);
      } else if (attr == Options.ATTR_GATE_UNDEFINED) {
        loadGateUndefined(val);
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
  }

  private static final long serialVersionUID = 1L;

  private MyListener myListener = new MyListener();

  private JLabel simLimitLabel = new JLabel();

  @SuppressWarnings({"unchecked", "rawtypes"})
  private JComboBox simLimit =
      new JComboBox(
          new Integer[] {
            Integer.valueOf(200),
            Integer.valueOf(500),
            Integer.valueOf(1000),
            Integer.valueOf(2000),
            Integer.valueOf(5000),
            Integer.valueOf(10000),
            Integer.valueOf(20000),
            Integer.valueOf(50000),
          });

  private JCheckBox simRandomness = new JCheckBox();
  private JCheckBox MemUnknown = new JCheckBox();
  private JLabel gateUndefinedLabel = new JLabel();

  @SuppressWarnings({"rawtypes", "unchecked"})
  private JComboBox gateUndefined =
      new JComboBox(
          new Object[] {
            new ComboOption(Options.GATE_UNDEFINED_IGNORE),
            new ComboOption(Options.GATE_UNDEFINED_ERROR)
          });


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

    setLayout(new TableLayout(1));
    add(MemUnknown);
    add(simLimitPanel);
    add(gateUndefinedPanel);
    add(simRandomness);

    window.getOptions().getAttributeSet().addAttributeListener(myListener);
    AttributeSet attrs = getOptions().getAttributeSet();
    myListener.loadSimLimit(attrs.getValue(Options.ATTR_SIM_LIMIT));
    myListener.loadGateUndefined(attrs.getValue(Options.ATTR_GATE_UNDEFINED));
    myListener.loadSimRandomness(attrs.getValue(Options.ATTR_SIM_RAND));
  }

  @Override
  public String getHelpText() {
    return S.get("simulateHelp");
  }

  @Override
  public String getTitle() {
    return S.get("simulateTitle");
  }

  @Override
  public void localeChanged() {
    simLimitLabel.setText(S.get("simulateLimit"));
    gateUndefinedLabel.setText(S.get("gateUndefined"));
    simRandomness.setText(S.get("simulateRandomness"));
    MemUnknown.setText(S.get("MemoriesStartupUnknown"));
  }
}
