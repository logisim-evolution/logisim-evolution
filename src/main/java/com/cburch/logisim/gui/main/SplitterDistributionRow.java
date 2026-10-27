/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.gui.generic.AttrTableModelRow;
import com.cburch.logisim.proj.Project;
import java.awt.Component;
import java.awt.Window;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;

class SplitterDistributionRow implements AttrTableModelRow {
  private final Project project;
  private final Circuit circuit;
  private final Splitter splitter;
  private final BooleanSupplier current;

  SplitterDistributionRow(
      Project project, Circuit circuit, Splitter splitter, BooleanSupplier current) {
    this.project = project;
    this.circuit = circuit;
    this.splitter = splitter;
    this.current = current;
  }

  @Override
  public Component getEditor(Window parent) {
    final var button = new JButton(getValue());
    button.setEnabled(isValueEditable());
    button.addActionListener(event -> {
      if (isValueEditable()) splitter.redistribute(project, circuit, 1);
    });
    return button;
  }

  @Override
  public String getLabel() {
    return S.get("splitterComponent");
  }

  @Override
  public String getValue() {
    return S.get("splitterDistributeAscending");
  }

  @Override
  public boolean isValueEditable() {
    return current.getAsBoolean()
        && circuit != null
        && project.getLogisimFile().contains(circuit)
        && circuit.contains(splitter)
        && splitter.distributionChanges(1);
  }

  @Override
  public boolean multiEditCompatible(AttrTableModelRow other) {
    return false;
  }

  @Override
  public void setValue(Window parent, Object value) {
    // This row is a command; only button activation changes the circuit.
  }
}
