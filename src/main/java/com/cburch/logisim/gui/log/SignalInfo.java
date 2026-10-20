/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import javax.swing.Icon;

// SignalInfo identifies a component within a top-level circuit or one of the
// nested sub-circuits within that top-level circuit. The path[] identifies how
// to get from the top-level circuit to the identified component.
//
// The path[] must not be empty. If it contains one component (e.g. a Pin or
// Led), then that component is one that appears in the top-level circuit.
// Otherwise, path[0] is a subcircuit component within the top level circuit,
// path[1] is a subcircuit component within path[0]'s circuit, etc., and
// path[n] is the actual component (e.g. a Pin or Led) within path[n-1]'s
// circuit.
//
// Internally, all the relevant circuits are also kept as well, such that
// circ[0] is the top-level circuit, and each other circ[i] is the circuit for
// path[i-1] in which path[i] appears.
//
// To summarize:
//
// (top)              (A)              (B)              (C)
// circ[0]       .-> circ[1]      .-> circ[2]      .-> circ[3]
// |holds      /     |holds     /     |holds     /     |holds
// |        is/      |       is/      |       is/      |
// v     ____/       v     ___/       v     ___/       v
// path[0]           path[1]          path[2]          path[3]
// (subcirc A)       (subcirc B)      (subcirc C)         (Pin)
//

public class SignalInfo implements AttributeListener, CircuitListener, Location.At {
  private final int n;
  private final Component[] path; // n-1 subcircuit Components, then a Loggable Component
  private final Circuit[] circ; // top-level circuit, then circuits for first n-1 path Components
  private final Object option;
  private RadixOption radix = RadixOption.RADIX_2;
  private String nickname; // short "LogName" of just the last Component of path
  private String fullname; // a path-like name, with slashes, ending with the nickname
  private int width = -1; // stored here so we can monitor for changes

  private boolean obsoleted;
  private Listener listener; // only one supported, for now, usally just the LogModel

  public interface Listener {
    void signalInfoNameChanged(SignalInfo s);

    void signalInfoObsoleted(SignalInfo s); // e.g. component was removed from circuit
  }

  public SignalInfo(Circuit root, Component[] p, Object o) {
    path = p;
    option = o;
    n = path.length;
    circ = new Circuit[n];

    circ[0] = root;
    for (int i = 1; i < n; i++) {
      final var f = (SubcircuitFactory) path[i - 1].getFactory();
      circ[i] = f.getSubcircuit();
    }
    computeName();

    // Listen to the top-level circuit, because either (a) this comp might
    // reside in the top-level, i.e., when path has only one Component, and
    // might get deleted from that circuit, or (b) this comp is buried in some
    // nested subcircuit of the top-level and that entire subcircuit component,
    // i.e., path[0], might get removed from the top-level circuit. Also listen
    // to each other circuit at each level of the path, for similar reasons.
    for (Circuit t : circ) t.addCircuitListener(this);

    // Listen to attributes of subcircuits, at each level of the path, including
    // the final component at the end of the path, because it affects our name
    // via changes to the location coordinates and/or labels.
    for (Component c : path) c.getAttributeSet().addAttributeListener(this);
  }

  public void setListener(Listener l) {
    if (listener != null && l != null && l != listener) throw new IllegalStateException("already have a different listener");
    listener = l;
  }

  @Override
  public void attributeValueChanged(AttributeEvent e) {
    recomputeName();
  }

  @Override
  public void circuitChanged(CircuitEvent event) {
    if (obsoleted) {
      return; // this SelectionItem doesn't appear to be alive any more
    }
    final var action = event.getAction();
    if (action == CircuitEvent.ACTION_CLEAR) {
      // This happens only when analyzer is replacing an entire circuit. Can we
      // match up pin names perhaps? todo later
      remove();
    } else if (action == CircuitEvent.TRANSACTION_DONE) {
      // This happens after a set of add/remove or other changes to a circuit.
      // This could remove a component that is on our path, or alter our name.

      // Walk through each level of circuit to see if we got removed or
      var changed = false;
      for (int i = 0; i < n; i++) {
        final var t = circ[i];
        final var c = path[i];

        final var repl = event.getResult().getReplacementMap(t);
        if (repl.isEmpty()) continue; // no changes at all to circuit at this level

        if (!repl.getRemovals().contains(c))
          continue; // changes at this level don't affect our path

        Component componentNew = null;
        final var newComps = repl.getReplacementsFor(c);
        for (final var c2 : newComps) {
          if (c2 == c || c2.getFactory() == c.getFactory()) {
            componentNew = c2;
            break;
          }
        }
        if (componentNew == c) {
          // component replaced by itself (strange...?)
          continue;
        } else if (componentNew != null) {
          // component replaced by alternate version (e.g. moved location)
          changed = true;
          path[i].getAttributeSet().removeAttributeListener(this);
          path[i] = componentNew;
          path[i].getAttributeSet().addAttributeListener(this);
          if (i < n - 1) {
            // sanity check, path[i] should still lead to circ[i+1]
            final var factory = (SubcircuitFactory) path[i].getFactory();
            if (circ[i + 1] != factory.getSubcircuit()) {
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
        if (listener != null) listener.signalInfoNameChanged(this);
      }
    } else if (action == CircuitEvent.ACTION_INVALIDATE) {
      // This happens for seemingly many kinds of changes to component, e.g.,
      // when a Pin's width or type changes. We could be that pin.
      recomputeName();
    }
  }

  private static String normalize(String s, Object o) {
    return (s == null || s.equals("")) ? null : o == null ? s : (s + "." + o);
  }

  private static String logName(Component c, Object option) {
    String s = null;
    final var log = (LoggableContract) c.getFeature(LoggableContract.class);
    if (log != null) s = normalize(log.getLogName(option), null);
    if (s == null) s = normalize(c.getAttributeSet().getValue(StdAttr.LABEL), option);
    if (s == null) s = normalize(c.getFactory().getDisplayName() + c.getLocation(), option);
    return s;
  }

  private boolean recomputeName() {
    if (computeName()) {
      // System.out.println(">>>>> new name is " + fullname);
      if (listener != null) listener.signalInfoNameChanged(this);
      return true;
    }
    return false;
  }

  private boolean computeName() {
    boolean changed = false;

    final var log = (LoggableContract) path[n - 1].getFeature(LoggableContract.class);
    BitWidth bw = null;
    if (log != null) bw = log.getBitWidth(option);
    if (bw == null) bw = path[n - 1].getAttributeSet().getValue(StdAttr.WIDTH);
    final var w = bw.getWidth();
    if (w != width) {
      changed = true;
      width = w;
    }
    String s = logName(path[n - 1], option);
    if (!s.equals(nickname)) {
      changed = true;
      nickname = s;
    }

    final var buf = new StringBuilder();
    for (var i = 0; i < n - 1; i++) buf.append(logName(path[i], null)).append("/");
    buf.append(nickname);
    if (width > 1) buf.append("[").append(width - 1).append("..0]");
    final var f = buf.toString();
    if (!f.equals(fullname)) {
      changed = true;
      fullname = f;
    }

    return changed;
  }

  public Value fetchValue(CircuitState root) {
    final var log = (LoggableContract) path[n - 1].getFeature(LoggableContract.class);
    if (log == null) return Value.NIL;
    var cur = root;
    for (var i = 0; i < n - 1; i++) cur = circ[i].getSubcircuitFactory().getSubstate(cur, path[i]);
    return log.getLogValue(cur, option);
  }

  public Component getComponent() {
    return path[n - 1];
  }

  public Circuit getTopLevelCircuit() {
    return circ[0];
  }

  public int getDepth() {
    return n;
  }

  public boolean isInput(Object option) {
    LoggableContract log = (LoggableContract) path[n - 1].getFeature(LoggableContract.class);
    return log != null && log.isInput(option);
  }

  public RadixOption getRadix() {
    return radix;
  }

  public String format(Value v) {
    return radix.toString(v);
  }

  public boolean setRadix(RadixOption value) {
    if (value == radix) return false;
    radix = value;
    return true;
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

  @Override
  public Location getLocation() {
    return path[n - 1].getLocation();
  }

  public int getWidth() {
    return width;
  }

  public Object getOption() {
    return option;
  }

  public final Icon icon =
      new Icon() {
        @Override
        public int getIconHeight() {
          return 20;
        }

        @Override
        public int getIconWidth() {
          return 20;
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
          SignalInfo.paintIcon(path[n - 1], option, c, g, x, y);
        }
      };

  public static void paintIcon(Component comp, Object opt, java.awt.Component c, Graphics g, int x, int y) {
    if (comp == null) return;
    if (opt != null) {
      // todo
      g.setColor(Color.MAGENTA);
      g.fillRect(x + 3, x + 3, 15, 15);
    } else {
      final var g2 = g.create();
      final var context = new ComponentDrawContext(c, null, null, g, g2);
      comp.getFactory().paintIcon(context, x, y, comp.getAttributeSet());
      g2.dispose();
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof SignalInfo o)) return false;
    return Arrays.equals(path, o.path) && Objects.equals(option, o.option);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(path), option);
  }

  private void remove() {
    if (obsoleted) return;
    obsoleted = true;
    for (final var t : circ) t.removeCircuitListener(this);
    for (final var c : path) c.getAttributeSet().removeAttributeListener(this);
    if (listener != null) listener.signalInfoObsoleted(this);
  }

  public static class List extends ArrayList<SignalInfo> implements Transferable {
    public static final DataFlavor dataFlavor;

    static {
      DataFlavor f = null;
      try {
        f =
            new DataFlavor(
                String.format(
                    "%s;class=\"%s\"",
                    DataFlavor.javaJVMLocalObjectMimeType, SignalInfo.List.class.getName()));
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      dataFlavor = f;
    }

    public static final DataFlavor[] dataFlavors = new DataFlavor[] {dataFlavor};

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
      return this;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return dataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return dataFlavor.equals(flavor);
    }
  }
}
