/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CircuitPins {
  private class MyComponentListener implements ComponentListener, AttributeListener {
    @Override
    public void attributeValueChanged(AttributeEvent e) {
      final var attr = e.getAttribute();
      if (attr == StdAttr.FACING || attr == StdAttr.LABEL || attr == Pin.ATTR_TYPE) {
        appearanceManager.updatePorts();
      }
    }

    @Override
    public void endChanged(ComponentEvent e) {
      appearanceManager.updatePorts();
    }
  }

  private final PortManager appearanceManager;
  private final MyComponentListener myComponentListener;
  private final Set<Instance> pins;

  CircuitPins(PortManager appearanceManager) {
    this.appearanceManager = appearanceManager;
    myComponentListener = new MyComponentListener();
    pins = new HashSet<>();
  }

  public Collection<Instance> getPins() {
    return new ArrayList<>(pins);
  }

  public void transactionCompleted(ReplacementMap repl) {
    // determine the changes
    final var adds = new HashSet<Instance>();
    final var removes = new HashSet<Instance>();
    final var replaces = new HashMap<Instance, Instance>();
    for (final var comp : repl.getAdditions()) {
      if (comp.getFactory() instanceof Pin) {
        final var in = Instance.getInstanceFor(comp);
        var added = pins.add(in);
        if (added) {
          comp.addComponentListener(myComponentListener);
          in.getAttributeSet().addAttributeListener(myComponentListener);
          adds.add(in);
        }
      }
    }
    for (final var comp : repl.getRemovals()) {
      if (comp.getFactory() instanceof Pin) {
        final var in = Instance.getInstanceFor(comp);
        final var removed = pins.remove(in);
        if (removed) {
          comp.removeComponentListener(myComponentListener);
          in.getAttributeSet().removeAttributeListener(myComponentListener);
          final var rs = repl.getReplacementsFor(comp);
          if (rs.isEmpty()) {
            removes.add(in);
          } else {
            final var r = rs.iterator().next();
            final var rIn = Instance.getInstanceFor(r);
            adds.remove(rIn);
            replaces.put(in, rIn);
          }
        }
      }
    }

    appearanceManager.updatePorts(adds, removes, replaces, getPins());
  }
}
