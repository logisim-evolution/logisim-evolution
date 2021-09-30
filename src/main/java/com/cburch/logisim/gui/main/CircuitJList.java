/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.swing.JList;

@SuppressWarnings("rawtypes")
class CircuitJList extends JList {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unchecked")
  public CircuitJList(Project proj, boolean includeEmpty) {
    LogisimFile file = proj.getLogisimFile();
    Circuit current = proj.getCurrentCircuit();
    Vector<Circuit> options = new Vector<>();
    boolean currentFound = false;
    for (Circuit circ : file.getCircuits()) {
      if (!includeEmpty || circ.getBounds() != Bounds.EMPTY_BOUNDS) {
        if (circ == current) currentFound = true;
        options.add(circ);
      }
    }

    setListData(options);
    if (currentFound) setSelectedValue(current, true);
    setVisibleRowCount(Math.min(6, options.size()));
  }

  public List<Circuit> getSelectedCircuits() {
    Object[] selected = getSelectedValuesList().toArray();
    if (selected.length > 0) {
      ArrayList<Circuit> ret = new ArrayList<>(selected.length);
      for (Object sel : selected) {
        if (sel instanceof Circuit circ) ret.add(circ);
      }
      return ret;
    } else {
      return Collections.emptyList();
    }
  }
}
