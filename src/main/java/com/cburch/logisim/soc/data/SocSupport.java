/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.StringUtil;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.swing.JMenuItem;

public class SocSupport {

  private static final long LONG_MASK = (1L << 32) - 1L;

  public static long convUnsignedInt(int value) {
    return ((long) value) & LONG_MASK;
  }

  public static int convUnsignedLong(long value) {
    return (int) (value & LONG_MASK);
  }

  public static void addAllFunctions(PrintWriter h, PrintWriter c, String compName, String functName, int base, int index) {
    addSetterFunction(h, compName, functName, base, index, true);
    addGetterFunction(h, compName, functName, base, index, true);
    h.println();
    addSetterFunction(c, compName, functName, base, index, false);
    addGetterFunction(c, compName, functName, base, index, false);
  }

  public static void addGetterFunction(PrintWriter w, String compName, String functName, int base, int index, boolean header) {
    w.print("unsigned int get" + compName + functName + "()");
    if (header) {
      w.println(";");
      return;
    } else w.println(" {");
    w.println("  volatile unsigned int* base;");
    w.println("  base = (unsigned int *) " + String.format("0x%X", base) + ";");
    w.println("  return base[" + index + "];");
    w.println("}\n");
  }

  public static void addSetterFunction(PrintWriter w, String compName, String functName, int base, int index, boolean header) {
    w.print("void set" + compName + functName + "(unsigned int value)");
    if (header) {
      w.println(";");
      return;
    } else w.println(" {");
    w.println("  volatile unsigned int* base;");
    w.println("  base = (unsigned int *) " + String.format("0x%X", base) + ";");
    w.println("  base[" + index + "] = value;");
    w.println("}\n");
  }

  public static JMenuItem createItem(ActionListener l, String label) {
    JMenuItem ret = new JMenuItem(label);
    ret.setEnabled(true);
    ret.addActionListener(l);
    return ret;
  }

  public static String getComponentName(Component comp) {
    var name = comp.getAttributeSet().getValue(StdAttr.LABEL);
    if (StringUtil.isNullOrEmpty(name)) {
      final var loc = comp.getLocation();
      name = String.format("%s@%d,%d", comp.getFactory().getDisplayName(), loc.getX(), loc.getY());
    }
    return name;
  }

  private static String getMasterHierName(CircuitState state) {
    ArrayList<CircuitState> states = new ArrayList<>();
    CircuitState s = state;
    while (s.isSubstate()) {
      states.add(s);
      s = s.getParentState();
    }
    StringBuilder name = new StringBuilder();
    name.append(s.getCircuit().getName()).append(":");
    for (int i = states.size() - 1; i >= 0; i--) {
      for (Component c : s.getCircuit().getNonWires()) {
        if (c.getFactory() instanceof SubcircuitFactory) {
          CircuitState tmp = (CircuitState) s.getData(c);
          if (tmp.equals(states.get(i)))
            name.append(getComponentName(c)).append(":");
        }
      }
      s = states.get(i);
    }
    return name.toString();
  }

  public static String getMasterName(CircuitState state, Component comp) {
    if (!state.isSubstate())
      return getComponentName(comp);
    return getMasterHierName(state) + getComponentName(comp);
  }

  public static String getMasterName(CircuitState state, String compName) {
    if (!state.isSubstate())
      return compName;
    return getMasterHierName(state) + compName;
  }

}
