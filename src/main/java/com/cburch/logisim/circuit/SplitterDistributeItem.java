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

    final var attrs = (SplitterAttributes) splitter.getAttributeSet();
    final var actual = attrs.bitEnd;
    final var desired = SplitterAttributes.computeDistribution(attrs.fanout, actual.length, order);
    var same = actual.length == desired.length;
    for (var i = 0; same && i < desired.length; i++) {
      if (actual[i] != desired[i]) {
        same = false;
        break;
      }
    }
    setEnabled(!same);
    setText(toGetter().toString());
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final var attrs = (SplitterAttributes) splitter.getAttributeSet();
    final var actual = attrs.bitEnd;
    final var desired = SplitterAttributes.computeDistribution(attrs.fanout, actual.length, order);
    final var xn = new CircuitMutation(proj.getCircuitState().getCircuit());
    for (int i = 0, n = Math.min(actual.length, desired.length); i < n; i++) {
      if (actual[i] != desired[i]) {
        xn.set(splitter, attrs.getBitOutAttribute(i), (int) desired[i]);
      }
    }
    proj.doAction(xn.toAction(toGetter()));
  }

  private StringGetter toGetter() {
    if (order > 0) {
      return S.getter("splitterDistributeAscending");
    } else {
      return S.getter("splitterDistributeDescending");
    }
  }
}
