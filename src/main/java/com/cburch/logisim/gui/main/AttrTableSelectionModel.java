/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
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
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;
import com.cburch.logisim.util.AutoLabel;
import com.cburch.logisim.vhdl.base.VhdlContent;
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

    final var selection = frame.getCanvas().getSelection();
    for (final var comp : selection.getComponents()) {
      final var fact = comp.getFactory();
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
      final var circ = frame.getCanvas().getCircuit();
      if (circ != null) {
        final var circName = circ.getName();
        setInstance(circ.getSubcircuitFactory());
        return S.get("circuitAttrTitle", circName);
      } else {
        final var hdl = (VhdlContent) frame.getCanvas().getCurrentHdl();
        final var circName = hdl.getName();
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
  public void selectionChanged(final Event event) {
    fireTitleChanged();
    if (!frame.getEditorView().equals(Frame.EDIT_APPEARANCE)) {
      frame.setAttrTableModel(this);
    }
  }

  @Override
  public void setValueRequested(final Attribute<Object> attr, final Object value) throws AttrTableSetException {
    final var selection = frame.getCanvas().getSelection();
    final var circuit = frame.getCanvas().getCircuit();
    if (circuit != null && selection.isEmpty()) {
      final var circuitModel = new AttrTableCircuitModel(project, circuit);
      circuitModel.setValueRequested(attr, value);
    } else {
      final var act = new SetAttributeAction(circuit, S.getter("selectionAttributeAction"));
      AutoLabel labeler = null;
      if (attr.equals(StdAttr.LABEL)) {
        labeler = new AutoLabel((String) value, circuit);
      }
      final var comps = new TreeSet<>(new PositionComparator());
      comps.addAll(selection.getComponents());
      for (final var comp : comps) {
        if (!(comp instanceof Wire)) {
          if (comp.getFactory() instanceof SubcircuitFactory fac) {
            if (attr.equals(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE)
                || attr.equals(CircuitAttributes.NAME_ATTR)) {
              try {
                final var mutation = new CircuitMutation(fac.getSubcircuit());
                mutation.setForCircuit(attr, value);
                final var action = mutation.toAction(null);
                project.doAction(action);
              } catch (CircuitException ex) {
                OptionPane.showMessageDialog(project.getFrame(), ex.getMessage());
              }
              return;
            }
          }
          if (attr.equals(StdAttr.LABEL)) {
            if (labeler.hasNext(circuit)) {
              if (comps.size() > 1) {
                act.set(comp, attr, labeler.getNext(circuit, comp.getFactory()));
              } else {
                if (getAttributeSet().getValue(StdAttr.LABEL).equals(value)) return;
                else act.set(comp, attr, labeler.getCurrent(circuit, comp.getFactory()));
              }
            } else act.set(comp, attr, "");
          } else {
            final var compAttrSet = comp.getAttributeSet();
            if (compAttrSet != null) {
              final var mayBeChangedList = compAttrSet.attributesMayAlsoBeChanged(attr, value);
              if (mayBeChangedList != null) {
                for (final var mayChangeAttr : mayBeChangedList) {
                  // mayChangeAttr is set to its current value to have it restored on undo
                  act.set(comp, mayChangeAttr, compAttrSet.getValue(mayChangeAttr));
                }
              }
            }
            act.set(comp, attr, value);
          }
        }
      }
      project.doAction(act);
    }
  }
}
