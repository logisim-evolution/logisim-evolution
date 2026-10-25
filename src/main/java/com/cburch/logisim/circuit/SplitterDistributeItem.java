/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;

class SplitterDistributeItem extends JMenuItem implements ActionListener {
  private static final long serialVersionUID = 1L;
  private final Project proj;
  private final Splitter splitter;
  private final int order;

  public SplitterDistributeItem(Project proj, Splitter splitter, int order) {
    this.proj = proj;
    this.splitter = splitter;
    this.order = order;
    addActionListener(this);

    setEnabled(splitter.distributionChanges(order));
    setText(toGetter().toString());
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    splitter.redistribute(proj, proj.getCircuitState().getCircuit(), order);
  }

  private StringGetter toGetter() {
    if (order > 0) {
      return S.getter("splitterDistributeAscending");
    } else {
      return S.getter("splitterDistributeDescending");
    }
  }
}
