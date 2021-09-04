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
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.val;

public class CircuitPins {
  private class MyComponentListener implements ComponentListener, AttributeListener {
    @Override
    public void attributeValueChanged(AttributeEvent e) {
      val attr = e.getAttribute();
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
    val adds = new HashSet<Instance>();
    val removes = new HashSet<Instance>();
    val replaces = new HashMap<Instance, Instance>();
    for (val comp : repl.getAdditions()) {
      if (comp.getFactory() instanceof Pin) {
        val in = Instance.getInstanceFor(comp);
        val added = pins.add(in);
        if (added) {
          comp.addComponentListener(myComponentListener);
          in.getAttributeSet().addAttributeListener(myComponentListener);
          adds.add(in);
        }
      }
    }
    for (val comp : repl.getRemovals()) {
      if (comp.getFactory() instanceof Pin) {
        val in = Instance.getInstanceFor(comp);
        val removed = pins.remove(in);
        if (removed) {
          comp.removeComponentListener(myComponentListener);
          in.getAttributeSet().removeAttributeListener(myComponentListener);
          val rs = repl.getReplacementsFor(comp);
          if (rs.isEmpty()) {
            removes.add(in);
          } else {
            val r = rs.iterator().next();
            val rin = Instance.getInstanceFor(r);
            adds.remove(rin);
            replaces.put(in, rin);
          }
        }
      }
    }

    appearanceManager.updatePorts(adds, removes, replaces, getPins());
  }
}
