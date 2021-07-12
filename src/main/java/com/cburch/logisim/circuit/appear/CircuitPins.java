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

public class CircuitPins {
  private class MyComponentListener implements ComponentListener, AttributeListener {
    @Override
    public void attributeValueChanged(AttributeEvent e) {
      Attribute<?> attr = e.getAttribute();
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
    Set<Instance> adds = new HashSet<>();
    Set<Instance> removes = new HashSet<>();
    Map<Instance, Instance> replaces = new HashMap<>();
    for (Component comp : repl.getAdditions()) {
      if (comp.getFactory() instanceof Pin) {
        Instance in = Instance.getInstanceFor(comp);
        boolean added = pins.add(in);
        if (added) {
          comp.addComponentListener(myComponentListener);
          in.getAttributeSet().addAttributeListener(myComponentListener);
          adds.add(in);
        }
      }
    }
    for (Component comp : repl.getRemovals()) {
      if (comp.getFactory() instanceof Pin) {
        Instance in = Instance.getInstanceFor(comp);
        boolean removed = pins.remove(in);
        if (removed) {
          comp.removeComponentListener(myComponentListener);
          in.getAttributeSet().removeAttributeListener(myComponentListener);
          Collection<Component> rs = repl.getReplacementsFor(comp);
          if (rs.isEmpty()) {
            removes.add(in);
          } else {
            Component r = rs.iterator().next();
            Instance rin = Instance.getInstanceFor(r);
            adds.remove(rin);
            replaces.put(in, rin);
          }
        }
      }
    }

    appearanceManager.updatePorts(adds, removes, replaces, getPins());
  }
}
