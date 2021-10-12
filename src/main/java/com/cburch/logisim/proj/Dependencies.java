/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.proj;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.util.Dag;
import com.cburch.logisim.vhdl.base.VhdlContent;
import com.cburch.logisim.vhdl.base.VhdlEntity;

public class Dependencies {
  private class MyListener implements LibraryListener, CircuitListener {
    @Override
    public void circuitChanged(CircuitEvent e) {
      Component comp;
      switch (e.getAction()) {
        case CircuitEvent.ACTION_ADD:
          comp = (Component) e.getData();
          if (comp.getFactory() instanceof SubcircuitFactory factory) {
            depends.addEdge(e.getCircuit(), factory.getSubcircuit());
          } else if (comp.getFactory() instanceof VhdlEntity factory) {
            depends.addEdge(e.getCircuit(), factory.getContent());
          }
          break;
        case CircuitEvent.ACTION_REMOVE:
          comp = (Component) e.getData();
          if (comp.getFactory() instanceof SubcircuitFactory factory) {
            var found = false;
            for (final var o : e.getCircuit().getNonWires()) {
              if (o.getFactory() == factory) {
                found = true;
                break;
              }
            }
            if (!found) depends.removeEdge(e.getCircuit(), factory.getSubcircuit());
          } else if (comp.getFactory() instanceof VhdlEntity factory) {
            var found = false;
            for (final var o : e.getCircuit().getNonWires()) {
              if (o.getFactory() == factory) {
                found = true;
                break;
              }
            }
            if (!found) depends.removeEdge(e.getCircuit(), factory.getContent());
          }
          break;
        case CircuitEvent.ACTION_CLEAR:
          depends.removeNode(e.getCircuit());
          break;
      }
    }

    @Override
    public void libraryChanged(LibraryEvent e) {
      switch (e.getAction()) {
        case LibraryEvent.ADD_TOOL:
          if (e.getData() instanceof AddTool) {
            final var factory = ((AddTool) e.getData()).getFactory();
            if (factory instanceof SubcircuitFactory circFact) {
              processCircuit(circFact.getSubcircuit());
            }
          }
          break;
        case LibraryEvent.REMOVE_TOOL:
          if (e.getData() instanceof AddTool) {
            final var factory = ((AddTool) e.getData()).getFactory();
            if (factory instanceof SubcircuitFactory circFact) {
              final var circ = circFact.getSubcircuit();
              depends.removeNode(circ);
              circ.removeCircuitListener(this);
            } else if (factory instanceof VhdlEntity circFact) {
              depends.removeNode(circFact.getContent());
            }
          }
          break;
      }
    }
  }

  private final MyListener myListener = new MyListener();
  private final Dag depends = new Dag();

  Dependencies(LogisimFile file) {
    addDependencies(file);
  }

  private void addDependencies(LogisimFile file) {
    file.addLibraryListener(myListener);
    for (Circuit circuit : file.getCircuits()) {
      processCircuit(circuit);
    }
  }

  public boolean canAdd(Circuit circ, Circuit sub) {
    return depends.canFollow(sub, circ);
  }

  public boolean canRemove(Circuit circ) {
    return !depends.hasPredecessors(circ);
  }

  public boolean canRemove(VhdlContent vhdl) {
    return !depends.hasPredecessors(vhdl);
  }

  private void processCircuit(Circuit circ) {
    circ.addCircuitListener(myListener);
    for (final var comp : circ.getNonWires()) {
      if (comp.getFactory() instanceof SubcircuitFactory factory) {
        depends.addEdge(circ, factory.getSubcircuit());
      } else if (comp.getFactory() instanceof VhdlEntity factory) {
        depends.addEdge(circ, factory.getContent());
      }
    }
  }
}
