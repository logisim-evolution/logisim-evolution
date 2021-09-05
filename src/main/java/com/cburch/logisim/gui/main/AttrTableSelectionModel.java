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

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.PositionComparator;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Selection.Event;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;
import com.cburch.logisim.util.AutoLabel;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.util.SortedSet;
import java.util.TreeSet;

class AttrTableSelectionModel extends AttributeSetTableModel implements Selection.Listener {
  private final Project project;
  private final Frame frame;

  public AttrTableSelectionModel(Project project, Frame frame) {
    super(frame.getCanvas().getSelection().getAttributeSet());
    this.project = project;
    this.frame = frame;
    frame.getCanvas().getSelection().addListener(this);
  }

  @Override
  public void attributeValueChanged(AttributeEvent e) {
    super.attributeValueChanged(e);
    if (e.getAttribute().equals(StdAttr.LABEL)) fireTitleChanged();
  }

  @Override
  public String getTitle() {
    ComponentFactory wireFactory = null;
    ComponentFactory factory = null;
    String label = null;
    Location loc = null;
    int factoryCount = 0;
    int totalCount = 0;
    boolean variousFound = false;

    Selection selection = frame.getCanvas().getSelection();
    for (Component comp : selection.getComponents()) {
      ComponentFactory fact = comp.getFactory();
      if (fact.equals(factory)) {
        factoryCount++;
      } else if (comp instanceof Wire) {
        wireFactory = fact;
        if (factory == null) {
          factoryCount++;
        }
      } else if (factory == null) {
        factory = fact;
        factoryCount = 1;
        label = comp.getAttributeSet().getValue(StdAttr.LABEL);
        loc = comp.getLocation();
      } else {
        variousFound = true;
      }
      if (!(comp instanceof Wire)) {
        totalCount++;
      }
    }

    if (factory == null) {
      factory = wireFactory;
    }

    if (variousFound) {
      setInstance(factory);
      return S.get("selectionVarious", "" + totalCount);
    } else if (factoryCount == 0) {
      Circuit circ = frame.getCanvas().getCircuit();
      if (circ != null) {
        String circName = circ.getName();
        setInstance(circ.getSubcircuitFactory());
        return S.get("circuitAttrTitle", circName);
      } else {
        VhdlContent hdl = (VhdlContent) frame.getCanvas().getCurrentHdl();
        String circName = hdl.getName();
        setInstance(null);
        return S.get("hdlAttrTitle", circName);
      }
    } else if (factoryCount == 1) {
      setInstance(factory);
      if (label != null && label.length() > 0) {
        return factory.getDisplayName() + " \"" + label + "\"";
      } else if (loc != null) {
        return factory.getDisplayName() + " " + loc;
      } else {
        return factory.getDisplayName();
      }
    } else {
      setInstance(factory);
      return S.get("selectionMultiple", factory.getDisplayName(), "" + factoryCount);
    }
  }

  //
  // Selection.Listener methods
  @Override
  public void selectionChanged(Event event) {
    fireTitleChanged();
    if (!frame.getEditorView().equals(Frame.EDIT_APPEARANCE)) {
      frame.setAttrTableModel(this);
    }
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) throws AttrTableSetException {
    Selection selection = frame.getCanvas().getSelection();
    Circuit circuit = frame.getCanvas().getCircuit();
    if (selection.isEmpty() && circuit != null) {
      AttrTableCircuitModel circuitModel = new AttrTableCircuitModel(project, circuit);
      circuitModel.setValueRequested(attr, value);
    } else {
      SetAttributeAction act =
          new SetAttributeAction(circuit, S.getter("selectionAttributeAction"));
      AutoLabel labler = null;
      if (attr.equals(StdAttr.LABEL)) {
        labler = new AutoLabel((String) value, circuit);
      }
      SortedSet<Component> comps = new TreeSet<>(new PositionComparator());
      comps.addAll(selection.getComponents());
      for (Component comp : comps) {
        if (!(comp instanceof Wire)) {
          if (comp.getFactory() instanceof SubcircuitFactory) {
            SubcircuitFactory fac = (SubcircuitFactory) comp.getFactory();
            if (attr.equals(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE)
                || attr.equals(CircuitAttributes.NAME_ATTR)) {
              try {
                CircuitMutation mutation = new CircuitMutation(fac.getSubcircuit());
                mutation.setForCircuit(attr, value);
                Action action = mutation.toAction(null);
                project.doAction(action);
              } catch (CircuitException ex) {
                OptionPane.showMessageDialog(project.getFrame(), ex.getMessage());
              }
              return;
            }
          }
          if (attr.equals(StdAttr.LABEL)) {
            if (labler.hasNext(circuit)) {
              if (comps.size() > 1) {
                act.set(comp, attr, labler.getNext(circuit, comp.getFactory()));
              } else {
                if (getAttributeSet().getValue(StdAttr.LABEL).equals(value)) return;
                else act.set(comp, attr, labler.getCurrent(circuit, comp.getFactory()));
              }
            } else act.set(comp, attr, "");
          } else act.set(comp, attr, value);
        }
      }
      project.doAction(act);
    }
  }
}
