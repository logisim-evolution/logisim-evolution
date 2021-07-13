/*
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
import com.cburch.logisim.util.LocaleListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class RegTabContent extends JScrollPane implements LocaleListener, Simulator.Listener {
  private static final long serialVersionUID = 1L;
  private static final HashMap<String, Component> registers = new HashMap<>();
  private final JPanel panel = new JPanel(new GridBagLayout());
  private final GridBagConstraints constraints = new GridBagConstraints();
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
    for (Circuit circ : proj.getLogisimFile().getCircuits()) {
      getAllRegisters(circ);
    }
    if (proj.getLogisimFile().getLibrary("prodis_v1.3") instanceof LoadedLibrary) {
      if (((LoadedLibrary) proj.getLogisimFile().getLibrary("prodis_v1.3")).getBase()
          instanceof LogisimFile) {
        for (Circuit circ :
            ((LogisimFile)
                    ((LoadedLibrary) proj.getLogisimFile().getLibrary("prodis_v1.3")).getBase())
                .getCircuits()) {
          getAllRegisters(circ);
        }
      }
    }

    var col1 = new MyLabel("Circuit", Font.ITALIC | Font.BOLD);
    var col2 = new MyLabel("Reg name", Font.BOLD);
    var col3 = new MyLabel("Value", Font.BOLD);

    col1.setColor(Color.LIGHT_GRAY);
    col2.setColor(Color.LIGHT_GRAY);
    col3.setColor(Color.LIGHT_GRAY);

    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.FIRST_LINE_START;
    constraints.ipady = 2;
    constraints.weighty = 0;

    int y = 0;
    constraints.gridy = y;
    constraints.gridx = 0;
    constraints.weightx = 0.3;
    panel.add(col1, constraints);
    constraints.gridx++;
    constraints.weightx = 0.5;
    panel.add(col2, constraints);
    constraints.gridx++;
    constraints.weightx = 0.2;
    panel.add(col3, constraints);
    y++;

    if (!registers.isEmpty()) {
      List<String> keys =
          registers.keySet().stream().sorted(new AlphanumComparator()).collect(Collectors.toList());
      for (String key : keys) {
        constraints.gridy = y;
        constraints.gridx = 0;
        String circuitName = key.split("/")[0];
        panel.add(new MyLabel(circuitName, Font.ITALIC, true), constraints);
        constraints.gridx++;
        String registerName = key.split("/")[1];
        panel.add(new MyLabel(registerName), constraints);
        constraints.gridx++;
        Component selReg = registers.get(key);
        CircuitState mainCircState = proj.getCircuitState();
        while (mainCircState.getParentState() != null) { // Get the main
          // circuit
          mainCircState = mainCircState.getParentState();
        }
        Value val =
            findVal(
                mainCircState, circuitName, selReg.getEnd(0).getLocation()); // Get Q port location

        if (val != null) {
          MyLabel hexLabel = new MyLabel(val.toHexString());
          hexLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, hexLabel.getFont().getSize()));
          panel.add(hexLabel, constraints);
        } else {
          panel.add(new MyLabel("-"), constraints);
        }
        y++;
      }
    }
    constraints.weighty = 1;
    constraints.gridy++;
    constraints.gridx = 0;
    constraints.weightx = 1;
    panel.add(new MyLabel(""), constraints);
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
    if (cs.containsKey(loc) && cs.getCircuit().getName().equals(cn)) {
      return cs.getValue(loc);
    } else {
      if (cs.getSubStates() != null && cs.getSubStates().size() > 0) {
        for (CircuitState cst : cs.getSubStates()) {
          Value ret;
          if ((ret = findVal(cst, cn, loc)) != null) {
            return ret;
          }
        }
      }
      return null;
    }
  }

  /**
   * This function will register all the components of type "Register" contain in the given circuit.
   * The registers will only be registered if their ATTR_SHOW_IN_TAB is set to true, and if their
   * label is not empty.
   *
   * @param circuit The circuit in which the registers are searched.
   */
  private synchronized void getAllRegisters(Circuit circuit) {
    for (Component comp : circuit.getNonWires()) {
      if (comp.getFactory().getName().equals("Register")) {
        if (comp.getAttributeSet().getValue(Register.ATTR_SHOW_IN_TAB)
            && !comp.getAttributeSet().getValue(StdAttr.LABEL).equals("")) {
          registers.put(
              circuit.getName() + "/" + comp.getAttributeSet().getValue(StdAttr.LABEL), comp);
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
