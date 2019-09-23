/**
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

package com.cburch.logisim.soc.data;

import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JMenuItem;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;

public class SocSupport {


  private static final long LongMask = (1L<<32)-1L;

  public static long convUnsignedInt(int value) {
    return ((long)value)&LongMask;
  }
  
  public static int convUnsignedLong(long value) {
    return (int)(value&LongMask);
  }

  public static void addAllFunctions(PrintWriter h , PrintWriter c , String compName, String functName, int base, int index) {
    addSetterFunction(h,compName,functName,base,index,true);
    addGetterFunction(h,compName,functName,base,index,true);
    h.println();
    addSetterFunction(c,compName,functName,base,index,false);
    addGetterFunction(c,compName,functName,base,index,false);
  }
  
  public static void addGetterFunction(PrintWriter w , String compName, String functName, int base, int index, boolean header) {
    w.print("unsigned int get"+compName+functName+"()");
    if (header) {
      w.println(";");
      return;
    } else w.println(" {");
    w.println("  volatile unsigned int* base;");
    w.println("  base = (unsigned int *) "+String.format("0x%X", base)+";");
    w.println("  return base["+index+"];");
    w.println("}\n");
  }
  
  public static void addSetterFunction(PrintWriter w , String compName, String functName, int base, int index, boolean header) {
    w.print("void set"+compName+functName+"(unsigned int value)");
    if (header) {
      w.println(";");
      return;
    } else w.println(" {");
    w.println("  volatile unsigned int* base;");
    w.println("  base = (unsigned int *) "+String.format("0x%X", base)+";");
    w.println("  base["+index+"] = value;");
    w.println("}\n");
  }

  public static JMenuItem createItem(ActionListener l, String label) {
    JMenuItem ret = new JMenuItem(label);
    ret.setEnabled(true);
    ret.addActionListener(l);
    return ret;
  }
  
  public static String getComponentName(Component comp) {
    String name = comp.getAttributeSet().getValue(StdAttr.LABEL);
    if (name == null || name.isEmpty()) {
      Location loc = comp.getLocation();
      name = comp.getFactory().getDisplayName()+"@"+loc.getX()+","+loc.getY();
    }
    return name;
  }
  
  private static String getMasterHierName(CircuitState state) {
    ArrayList<CircuitState> states = new ArrayList<CircuitState>();
    CircuitState s = state;
    while (s.isSubstate()) {
      states.add(s);
      s = s.getParentState();
    }
    StringBuffer name = new StringBuffer();
    name.append(s.getCircuit().getName()+":");
    for (int i = states.size()-1 ; i >= 0 ; i--) {
      for (Component c : s.getCircuit().getNonWires()) {
        if (c.getFactory() instanceof SubcircuitFactory) {
          CircuitState tmp = (CircuitState)s.getData(c);
          if (tmp.equals(states.get(i)))
            name.append(getComponentName(c)+":");
        }
      }
      s = states.get(i);
    }
    return name.toString();
  }
  
  public static String getMasterName(CircuitState state, Component comp) {
    if (!state.isSubstate())
      return getComponentName(comp);
    return getMasterHierName(state)+getComponentName(comp);
  }
  
  public static String getMasterName(CircuitState state, String compName) {
    if (!state.isSubstate())
      return compName;
    return getMasterHierName(state)+compName;
  }
  
}
