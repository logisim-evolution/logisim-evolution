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

package com.cburch.logisim.gui.log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;

//Notes: The item belongs to a particular model, for a particular simulation
//of a particular top-level circuit. Thus:
//  model --> circuitState --> top-level circuit
//
//The path[] must not be empty. If it contains one component (e.g. a Pin or
//Led), then that component is one that appears in the top-level circuit.
//Otherwise, path[0] is a subcircuit component within the top level circuit,
//path[1] is a subcircuit component within path[0]'s circuit, etc., and
//path[n] is the actual component (e.g. a Pin or Led) within path[n-1]'s
//circuit.
//
//Internally, all the relevant circuits are also kept as well, such that
//circ[0] is the top-level circuit, and each other circ[i] is the circuit for
//path[i-1] in which path[i] appears.
//
//To summarize:
//
//   (top)              (A)              (B)              (C)
//  circ[0]       .-> circ[1]      .-> circ[2]      .-> circ[3]     
//   |holds      /     |holds     /     |holds     /     |holds     
//   |        is/      |       is/      |       is/      |           
//   v     ____/       v     ___/       v     ___/       v           
// path[0]           path[1]          path[2]          path[3]
//(subcirc A)       (subcirc B)      (subcirc C)         (Pin)
//

public class SelectionItem implements AttributeListener, CircuitListener, Location.At {
  private final Model model;
  private int n;
  private Component[] path; // n-1 subcircuit Components, then a Loggable Component
  private Circuit[] circ; // top-level circuit, then circuits for first n-1 path Components
  private Object option; 
  private RadixOption radix = RadixOption.RADIX_2;
  private String nickname; // short "LogName" of just the last Component of path
  private String fullname; // a path-like name, with slashes, ending with the nickname
  private int width = -1; // stored here so we can monitor for changes

  public SelectionItem(Model m, Component[] p, Object o) {
    model = m;
    path = p;
    option = o;
    n = path.length;
    circ = new Circuit[n];

    circ[0] = model.getCircuitState().getCircuit();
    for (int i = 1; i < n; i++) {
      SubcircuitFactory f = (SubcircuitFactory)path[i-1].getFactory();
      circ[i] = f.getSubcircuit();
    }
    computeName();

    // Listen to the top-level circuit, because either (a) this comp might
    // reside in the top-level, i.e., when path has only one Component, and
    // might get deleted from that circuit, or (b) this comp is buried in some
    // nested subcircuit of the top-level and that entire subcircuit component,
    // i.e., path[0], might get removed from the top-level circuit. Also listen
    // to each other circuit at each level of the path, for similar reasons.
    for (Circuit t : circ)
      t.addCircuitListener(this);

    // Listen to attributes of subcircuits, at each level of the path, including
    // the final component at the end of the path, because it affects our name
    // via changes to the location coordinates and/or labels.
    for (Component c : path)
      c.getAttributeSet().addAttributeListener(this);
  }
  
  public void attributeListChanged(AttributeEvent e) { }
  
  public void attributeValueChanged(AttributeEvent e) {
    recomputeName();
  }

  public void circuitChanged(CircuitEvent event) {
    int index = model.getSelection().indexOf(this);
    if (index < 0) {
      return; // this SelectionItem doesn't appear to be alive any more
    }
    int action = event.getAction();
    if (action == CircuitEvent.ACTION_CLEAR) {
      // This happens only when analyzer is replacing an entire circuit. Can we
      // match up pin names perhaps? todo later
      remove();
    }

    else if (action == CircuitEvent.TRANSACTION_DONE) {
      // This happens after a set of add/remove or other changes to a circuit.
      // This could remove a component that is on our path, or alter our name.

      // Walk through each level of circuit to see if we got removed or
      boolean changed = false;
      for (int i = 0; i < n; i++) {
        Circuit t = circ[i];
        Component c = path[i];

        ReplacementMap repl = event.getResult().getReplacementMap(t);
        if (repl.isEmpty())
          continue; // no changes at all to circuit at this level

        if (!repl.getRemovals().contains(c))
          continue; // changes at this level don't affect our path

        Component cNew = null;
        Collection<Component> newComps = repl.getReplacementsFor(c);
        for (Component c2 : newComps) {
          if (c2 == c || c2.getFactory() == c.getFactory()) {
            cNew = c2;
            break;          
          }
        }
      if (cNew == c) {
        // component replaced by itself (strange...?)
        continue;
      } else if (cNew != null) {
        // component replaced by alternate version (e.g. moved location)
        changed = true;
        path[i].getAttributeSet().removeAttributeListener(this);
        path[i] = cNew;
        path[i].getAttributeSet().addAttributeListener(this);
        if (i < n-1) {
          // sanity check, path[i] should still lead to circ[i+1]
          SubcircuitFactory f = (SubcircuitFactory)path[i].getFactory();
          if (circ[i+1] != f.getSubcircuit()) {
            System.out.println("**** failure: subcircuit changed!");
            remove();
            return;
          }
        }
      } else {
        // component replaced by something completely different (???)
        // System.out.printf("circuit %s replaced %s with something else\n", t, c);
        remove();
        return;
      }
    }
    if (changed) {
      computeName();
      model.fireSelectionChanged(new Model.Event()); // always, even if same name
    }
  } else if (action == CircuitEvent.ACTION_INVALIDATE) {
      // This happens for seemingly many kinds of changes to component, e.g.,
      // when a Pin's width or type changes. We could be that pin.
      recomputeName();   
    }
  }

  private static String normalize(String s, Object o) {
    return (s == null || s.equals("")) ? null
        : o == null ? s : (s + "." + o);
  }
  private static String logName(Component c, Object option) {
    String s = null;
    Loggable log = (Loggable)c.getFeature(Loggable.class);
    if (log != null)
      s = normalize(log.getLogName(option), null);
    if (s == null)
      s = normalize(c.getAttributeSet().getValue(StdAttr.LABEL), option);
    if (s == null)
      s = normalize(c.getFactory().getDisplayName() + c.getLocation(), option);
    return s;
  }

  private boolean recomputeName() {
    if (computeName()) {
      // System.out.println(">>>>> new name is " + fullname);
      model.fireSelectionChanged(new Model.Event());
      return true;
    }
    return false;
  }

  private boolean computeName() {
    boolean changed = false;

    Loggable log = (Loggable)path[n-1].getFeature(Loggable.class);
    BitWidth bw = null;
    if (log != null)
      bw = log.getBitWidth(option);
    if (bw == null)
      bw = path[n-1].getAttributeSet().getValue(StdAttr.WIDTH);
    int w = bw.getWidth();
    if (w != width) {
      changed = true;
      width = w;
    }
    String s = logName(path[n-1], option);
    if (!s.equals(nickname)) {
      changed = true;
      nickname = s;
    }

    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < n-1; i++)
      buf.append(logName(path[i], null) + "/");
    buf.append(nickname);
    if (width > 1)
      buf.append("[" + (width-1) + "..0]");
    String f = buf.toString();
    if (!f.equals(fullname)) {
      changed = true;
      fullname = f;
    }

    return changed;
  }

  public Value fetchValue(CircuitState root) {
    Loggable log = (Loggable) path[n-1].getFeature(Loggable.class);
    if (log == null) return Value.NIL;
    CircuitState cur = root;
    for (int i = 0; i < n-1; i++)
      cur = circ[i].getSubcircuitFactory().getSubstate(cur, path[i]);
    return log.getLogValue(cur, option);
  }

  public Component getComponent() {
    return path[n-1];
  }

  public RadixOption getRadix() {
    return radix;
  }
  
  public String format(Value v) {
    return radix.toString(v);
  }

  public void setRadix(RadixOption value) {
    if (value == radix) return;
    radix = value;
    model.fireSelectionChanged(new Model.Event());
  }

  public String getShortName() {
    return nickname;
  }

  @Override
  public String toString() {
    return fullname;
  }

  public String getDisplayName() {
    return fullname;
  }
 
  public Location getLocation() {
    return path[n-1].getLocation();
  }

  public int getWidth() {
    return width;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this)
      return true;
    if (other == null || !(other instanceof SelectionItem))
      return false;
    SelectionItem o = (SelectionItem)other;
    return model.equals(o.model)
        && Arrays.equals(path, o.path)
        && Objects.equals(option, o.option);
  }

  @Override
  public int hashCode() {
    return Objects.hash(model, Arrays.hashCode(path), option);
  }

  private void remove() {
    int index = model.getSelection().indexOf(this);
    model.getSelection().remove(index);
    for (Circuit t : circ)
      t.removeCircuitListener(this);
    for (Component c : path)
      c.getAttributeSet().removeAttributeListener(this);
  }

}
