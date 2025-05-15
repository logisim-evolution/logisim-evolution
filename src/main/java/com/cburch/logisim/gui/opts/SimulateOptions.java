/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.opts;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.TableLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class SimulateOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private final MyListener myListener = new MyListener();
  private final JLabel simLimitLabel = new JLabel();

  @SuppressWarnings({"unchecked", "rawtypes"})
  private final JComboBox simLimit =
      new JComboBox(
          new Integer[] {
            200, 500, 1000, 2000, 5000, 10000, 20000, 50000,
          });

  private final JCheckBox simRandomness = new JCheckBox();
  private final JCheckBox memUnknown = new JCheckBox();
  private final JLabel gateUndefinedLabel = new JLabel();

  @SuppressWarnings({"rawtypes", "unchecked"})
  private final JComboBox gateUndefined =
      new JComboBox(
          new Object[] {
            new ComboOption(Options.GATE_UNDEFINED_IGNORE),
            new ComboOption(Options.GATE_UNDEFINED_ERROR)
          });

  public SimulateOptions(OptionsFrame window) {
    super(window);

    final var simLimitPanel = new JPanel();
    simLimitPanel.add(simLimitLabel);
    simLimitPanel.add(simLimit);
    simLimit.addActionListener(myListener);

    final var gateUndefinedPanel = new JPanel();
    gateUndefinedPanel.add(gateUndefinedLabel);
    gateUndefinedPanel.add(gateUndefined);
    gateUndefined.addActionListener(myListener);

    simRandomness.addActionListener(myListener);

    memUnknown.addActionListener(myListener);
    memUnknown.setSelected(AppPreferences.Memory_Startup_Unknown.get());

    setLayout(new TableLayout(1));
    add(memUnknown);
    add(simLimitPanel);
    add(gateUndefinedPanel);
    add(simRandomness);

    window.getOptions().getAttributeSet().addAttributeListener(myListener);
    final var attrs = getOptions().getAttributeSet();
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
    memUnknown.setText(S.get("MemoriesStartupUnknown"));
  }

  private class MyListener implements ActionListener, AttributeListener {
    @Override
    public void actionPerformed(ActionEvent event) {
      final var source = event.getSource();
      if (source == simLimit) {
        final var opt = (Integer) simLimit.getSelectedItem();
        if (opt != null) {
          final var attrs = getOptions().getAttributeSet();
          getProject().doAction(OptionsActions.setAttribute(attrs, Options.ATTR_SIM_LIMIT, opt));
        }
      } else if (source == simRandomness) {
        final var attrs = getOptions().getAttributeSet();
        Object val = simRandomness.isSelected() ? Options.SIM_RAND_DFLT : Integer.valueOf(0);
        getProject().doAction(OptionsActions.setAttribute(attrs, Options.ATTR_SIM_RAND, val));
      } else if (source == gateUndefined) {
        final var opt = (ComboOption) gateUndefined.getSelectedItem();
        if (opt != null) {
          final var attrs = getOptions().getAttributeSet();
          getProject()
              .doAction(
                  OptionsActions.setAttribute(attrs, Options.ATTR_GATE_UNDEFINED, opt.getValue()));
        }
      } else if (source == memUnknown) {
        AppPreferences.Memory_Startup_Unknown.set(memUnknown.isSelected());
        final var sim = getProject().getSimulator();
        if (sim != null) sim.reset();
      }
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      final Attribute<?> attr = e.getAttribute();
      final var val = e.getValue();
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
      final var value = val;
      final var model = simLimit.getModel();
      for (var i = 0; i < model.getSize(); i++) {
        final var opt = (Integer) model.getElementAt(i);
        if (opt.equals(value)) {
          simLimit.setSelectedItem(opt);
        }
      }
    }

    private void loadSimRandomness(Integer val) {
      simRandomness.setSelected(val > 0);
    }
  }
}
