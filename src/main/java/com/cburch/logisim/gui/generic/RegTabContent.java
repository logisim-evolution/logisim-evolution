/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.LoadedLibrary;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Register;
import com.cburch.logisim.util.AlphanumComparator;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.LocaleListener;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class RegTabContent extends JScrollPane implements LocaleListener, Simulator.Listener {
  private static final long serialVersionUID = 1L;
  private static final HashMap<String, Component> registers = new HashMap<>();
  private final JPanel panel = new JPanel(new GridBagLayout());
  private final GridBagConstraints gbc = new GridBagConstraints();
  private final Project proj;

  public RegTabContent(Frame frame) {
    super();
    setViewportView(panel);
    proj = frame.getProject();
    getVerticalScrollBar().setUnitIncrement(16);
    proj.getSimulator().addSimulatorListener(this);

    fillArray();
  }

  /**
   * This function will clear and fill the registers tab and refresh their value. It will start by
   * iterate over all circuits of the current project to register all the "Register" components
   * (providing their attributes are correctly set). It will then fill the panel with each register
   * found, including their current value.
   */
  private void fillArray() {
    registers.clear();
    panel.removeAll();
    for (final var circ : proj.getLogisimFile().getCircuits()) {
      getAllRegisters(circ);
    }
    if (proj.getLogisimFile().getLibrary("prodis_v1.3") instanceof LoadedLibrary loadedLib) {
      if (loadedLib.getBase() instanceof LogisimFile lsFile) {
        for (final var circ : lsFile.getCircuits()) {
          getAllRegisters(circ);
        }
      }
    }

    // FIXME: hardcoded strings
    final var col1 = new MyLabel("Circuit", Font.ITALIC | Font.BOLD);
    final var col2 = new MyLabel("Reg name", Font.BOLD);
    final var col3 = new MyLabel("Value", Font.BOLD);

    col1.setColor(Color.LIGHT_GRAY);
    col2.setColor(Color.LIGHT_GRAY);
    col3.setColor(Color.LIGHT_GRAY);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.ipady = 2;
    gbc.weighty = 0;

    var y = 0;
    gbc.gridy = y;
    gbc.gridx = 0;
    gbc.weightx = 0.3;
    panel.add(col1, gbc);
    gbc.gridx++;
    gbc.weightx = 0.5;
    panel.add(col2, gbc);
    gbc.gridx++;
    gbc.weightx = 0.2;
    panel.add(col3, gbc);
    y++;

    if (!registers.isEmpty()) {
      final var keys = registers.keySet().stream().sorted(new AlphanumComparator()).toList();
      for (final var key : keys) {
        gbc.gridy = y;
        gbc.gridx = 0;
        final var circuitName = key.split("/")[0];
        panel.add(new MyLabel(circuitName, Font.ITALIC, true), gbc);
        gbc.gridx++;
        final var registerName = key.split("/")[1];
        panel.add(new MyLabel(registerName), gbc);
        gbc.gridx++;
        final var selReg = registers.get(key);
        var mainCircState = proj.getCircuitState();
        if (mainCircState == null) continue;
        while (mainCircState.getParentState() != null) { // Get the main circuit
          mainCircState = mainCircState.getParentState();
        }
        final var val = findVal(mainCircState, circuitName, selReg.getEnd(0).getLocation()); // Get Q port location

        if (val != null) {
          final var hexLabel = new MyLabel(val.toHexString());
          hexLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, hexLabel.getFont().getSize()));
          panel.add(hexLabel, gbc);
        } else {
          panel.add(new MyLabel("-"), gbc);
        }
        y++;
      }
    }
    gbc.weighty = 1;
    gbc.gridy++;
    gbc.gridx = 0;
    gbc.weightx = 1;
    panel.add(new MyLabel(""), gbc);
    panel.validate();
  }

  /**
   * This function will search for the value at a given location in a circuit with the given name.
   * The function will search iteratively in all sub-circuits of the given circuit if it cannot be
   * found directly, and will return null if the value cannot be found.
   *
   * @param cs The state of the circuit in which the value is searched.
   * @param cn The name of the circuit in which the value must be found.
   * @param loc The location of the value in the circuit.
   * @return The value, or null if it cannot be found.
   */
  private synchronized Value findVal(CircuitState cs, String cn, Location loc) {
    if (cs.getCircuit().getName().equals(cn)) {
      final var value = cs.getValue(loc);
      if (value != null) return cs.getValue(loc);
    }

    if (CollectionUtil.isNotEmpty(cs.getSubstates())) {
      for (final var cst : cs.getSubstates()) {
        final var ret = findVal(cst, cn, loc);
        if (ret != null) return ret;
      }
    }
    return null;
  }

  /**
   * This function will register all the components of type "Register" contain in the given circuit.
   * The registers will only be registered if their ATTR_SHOW_IN_TAB is set to true, and if their
   * label is not empty.
   *
   * @param circuit The circuit in which the registers are searched.
   */
  private synchronized void getAllRegisters(Circuit circuit) {
    for (final var comp : circuit.getNonWires()) {
      if (comp.getFactory().getName().equals("Register")) {
        if (comp.getAttributeSet().getValue(Register.ATTR_SHOW_IN_TAB) && !comp.getAttributeSet().getValue(StdAttr.LABEL).equals("")) {
          registers.put(circuit.getName() + "/" + comp.getAttributeSet().getValue(StdAttr.LABEL), comp);
        }
      }
    }
  }

  @Override
  public void localeChanged() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void simulatorReset(Simulator.Event e) {
    fillArray();
  }

  @Override
  public void propagationCompleted(Simulator.Event e) {
    // throw new UnsupportedOperationException("Not supported yet.");
    fillArray();
  }

  @Override
  public void simulatorStateChanged(Simulator.Event e) {
    // FIXME: we should have some more advanced logic here?
    // throw new UnsupportedOperationException("Not supported yet.");
  }

  private static class MyLabel extends JLabel {
    private static final long serialVersionUID = 1L;

    private MyLabel(String text) {
      super(text);
    }

    private MyLabel(String text, int style) {
      super(text);
      setFont(getFont().deriveFont(style));
    }

    private MyLabel(String text, int style, boolean small) {
      super(text);
      setFont(getFont().deriveFont(style));
      setFont(getFont().deriveFont(getFont().getSize2D() - 2));
    }

    private void setColor(Color color) {
      setBackground(color);
      setOpaque(true);
    }
  }
}
