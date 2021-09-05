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

package com.cburch.logisim.instance;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.tools.TextEditable;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.SyntaxChecker;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public final class InstanceComponent implements Component, AttributeListener, ToolTipMaker {
  private EventSourceWeakSupport<ComponentListener> listeners;
  private InstanceFactory factory;
  private final Instance instance;
  private final Location loc;
  private Bounds bounds;
  private List<Port> portList;
  private EndData[] endArray;
  private List<EndData> endList;
  private boolean hasToolTips;
  private HashSet<Attribute<BitWidth>> widthAttrs;
  private final AttributeSet attrs;
  private boolean attrListenRequested;
  private InstanceTextField textField;
  private InstanceStateImpl instanceState;
  private boolean doMarkInstance;
  private boolean doMarkLabel;

  public InstanceComponent(InstanceFactory factory, Location loc, AttributeSet attrs) {
    this.listeners = null;
    this.factory = factory;
    this.instance = Instance.makeFor(this);
    this.loc = loc;
    this.bounds = factory.getOffsetBounds(attrs).translate(loc.getX(), loc.getY());
    this.portList = factory.getPorts();
    this.endArray = null;
    this.hasToolTips = false;
    this.attrs = attrs;
    this.attrListenRequested = false;
    this.textField = null;
    doMarkInstance = false;
    doMarkLabel = false;

    computeEnds();
  }

  void addAttributeListener(Instance instance) {
    if (!attrListenRequested) {
      attrListenRequested = true;
      if (widthAttrs == null) getAttributeSet().addAttributeListener(this);
    }
  }

  //
  // DRC mark functions
  //
  public boolean isMarked() {
    return doMarkInstance || doMarkLabel;
  }

  public void clearMarks() {
    doMarkInstance = false;
    doMarkLabel = false;
  }

  public void markInstance() {
    doMarkInstance = true;
  }

  public void markLabel() {
    doMarkLabel = true;
  }

  //
  // listening methods
  //
  @Override
  public void addComponentListener(ComponentListener l) {
    EventSourceWeakSupport<ComponentListener> ls = listeners;
    if (ls == null) {
      ls = new EventSourceWeakSupport<>();
      ls.add(l);
      listeners = ls;
    } else {
      ls.add(l);
    }
  }

  //
  // AttributeListener methods
  //
  @Override
  public void attributeValueChanged(AttributeEvent e) {
    Attribute<?> attr = e.getAttribute();
    if (e.getAttribute().equals(StdAttr.LABEL)) {
      @SuppressWarnings("unchecked")
      Attribute<String> lattr = (Attribute<String>) e.getAttribute();
      var value = (String) e.getSource().getValue(e.getAttribute());
      var oldValue = e.getOldValue() != null ? (String) e.getOldValue() : "";
      if (!oldValue.equals(value)) {
        if (!SyntaxChecker.isVariableNameAcceptable(value, true)) {
          e.getSource().setValue(lattr, oldValue);
        } else if (getFactory().getName().equalsIgnoreCase(value)) {
          OptionPane.showMessageDialog(null, S.get("MatchedLabelNameError"));
          e.getSource().setValue(lattr, oldValue);
        } else if (CorrectLabel.isKeyword(value, false)) {
          OptionPane.showMessageDialog(null, "\"" + value + "\": " + S.get("KeywordNameError"));
          e.getSource().setValue(lattr, oldValue);
        } else {
          fireLabelChanged(e);
        }
      }
    }
    if (widthAttrs != null && widthAttrs.contains(attr)) computeEnds();
    if (attrListenRequested) {
      factory.instanceAttributeChanged(instance, e.getAttribute());
    }
  }

  private void computeEnds() {
    List<Port> ports = portList;
    EndData[] esOld = endArray;
    int esOldLength = esOld == null ? 0 : esOld.length;
    EndData[] es = esOld;
    if (es == null || es.length != ports.size()) {
      es = new EndData[ports.size()];
      if (esOldLength > 0) {
        int toCopy = Math.min(esOldLength, es.length);
        System.arraycopy(esOld, 0, es, 0, toCopy);
      }
    }
    HashSet<Attribute<BitWidth>> wattrs = null;
    boolean toolTipFound = false;
    ArrayList<EndData> endsChangedOld = null;
    ArrayList<EndData> endsChangedNew = null;
    Iterator<Port> pit = ports.iterator();
    for (int i = 0; pit.hasNext() || i < esOldLength; i++) {
      Port p = pit.hasNext() ? pit.next() : null;
      EndData oldEnd = i < esOldLength ? esOld[i] : null;
      EndData newEnd = p == null ? null : p.toEnd(loc, attrs);
      if (oldEnd == null || !oldEnd.equals(newEnd)) {
        if (newEnd != null) es[i] = newEnd;
        if (endsChangedOld == null) {
          endsChangedOld = new ArrayList<>();
          endsChangedNew = new ArrayList<>();
        }
        endsChangedOld.add(oldEnd);
        endsChangedNew.add(newEnd);
      }

      if (p != null) {
        Attribute<BitWidth> attr = p.getWidthAttribute();
        if (attr != null) {
          if (wattrs == null) {
            wattrs = new HashSet<>();
          }
          wattrs.add(attr);
        }

        if (p.getToolTip() != null) toolTipFound = true;
      }
    }
    if (!attrListenRequested) {
      HashSet<Attribute<BitWidth>> oldWattrs = widthAttrs;
      if (wattrs == null && oldWattrs != null) {
        getAttributeSet().removeAttributeListener(this);
      } else if (wattrs != null && oldWattrs == null) {
        getAttributeSet().addAttributeListener(this);
      }
    }
    if (es != esOld) {
      endArray = es;
      endList = new UnmodifiableList<>(es);
    }
    widthAttrs = wattrs;
    hasToolTips = toolTipFound;
    if (endsChangedOld != null) {
      fireEndsChanged(endsChangedOld, endsChangedNew);
    }
  }

  @Override
  public boolean contains(Location pt) {
    Location translated = pt.translate(-loc.getX(), -loc.getY());
    InstanceFactory factory = instance.getFactory();
    return factory.contains(translated, instance.getAttributeSet());
  }

  @Override
  public boolean contains(Location pt, Graphics g) {
    InstanceTextField field = textField;
    if (field != null && field.getBounds(g).contains(pt)) return true;
    else return contains(pt);
  }

  //
  // drawing methods
  //
  @Override
  public void draw(ComponentDrawContext context) {
    InstancePainter painter = context.getInstancePainter();
    painter.setInstance(this);
    factory.paintInstance(painter);
    if (doMarkInstance) {
      final Graphics g = painter.getGraphics();
      final Bounds bds = painter.getBounds();
      final Color current = g.getColor();
      g.setColor(Netlist.DRC_INSTANCE_MARK_COLOR);
      GraphicsUtil.switchToWidth(g, 2);
      g.drawRoundRect(
          bds.getX() - 10, bds.getY() - 10, bds.getWidth() + 20, bds.getHeight() + 20, 40, 40);
      GraphicsUtil.switchToWidth(g, 1);
      g.setColor(current);
    }
  }

  //
  // methods for InstancePainter
  //
  void drawLabel(ComponentDrawContext context) {
    InstanceTextField field = textField;
    if (field != null) {
      field.draw(this, context);
      if (doMarkLabel) {
        final Graphics g = context.getGraphics();
        final Bounds bds = field.getBounds(g);
        final Color current = g.getColor();
        g.setColor(Netlist.DRC_LABEL_MARK_COLOR);
        GraphicsUtil.switchToWidth(g, 2);
        g.drawRoundRect(
            bds.getX() - 10, bds.getY() - 10, bds.getWidth() + 20, bds.getHeight() + 20, 40, 40);
        GraphicsUtil.switchToWidth(g, 1);
        g.setColor(current);
      }
    }
  }

  @Override
  public boolean endsAt(Location pt) {
    EndData[] ends = endArray;
    for (EndData end : ends) {
      if (end.getLocation().equals(pt)) return true;
    }
    return false;
  }

  @Override
  public void expose(ComponentDrawContext context) {
    Bounds b = bounds;
    context.getDestination().repaint(b.getX(), b.getY(), b.getWidth(), b.getHeight());
  }

  private void fireLabelChanged(AttributeEvent attre) {
    EventSourceWeakSupport<ComponentListener> ls = listeners;
    if (ls != null) {
      ComponentEvent e = null;
      for (ComponentListener l : ls) {
        if (e == null) e = new ComponentEvent(this, null, attre);
        l.LabelChanged(e);
      }
    }
  }

  private void fireEndsChanged(ArrayList<EndData> oldEnds, ArrayList<EndData> newEnds) {
    EventSourceWeakSupport<ComponentListener> ls = listeners;
    if (ls != null) {
      ComponentEvent e = null;
      for (ComponentListener l : ls) {
        if (e == null) e = new ComponentEvent(this, oldEnds, newEnds);
        l.endChanged(e);
      }
    }
  }

  void fireInvalidated() {
    EventSourceWeakSupport<ComponentListener> ls = listeners;
    if (ls != null) {
      ComponentEvent e = null;
      for (ComponentListener l : ls) {
        if (e == null) e = new ComponentEvent(this);
        l.componentInvalidated(e);
      }
    }
  }

  @Override
  public AttributeSet getAttributeSet() {
    return attrs;
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  @Override
  public Bounds getBounds(Graphics g) {
    Bounds ret = bounds;
    InstanceTextField field = textField;
    if (field != null) ret = ret.add(field.getBounds(g));
    return ret;
  }

  @Override
  public EndData getEnd(int index) {
    return endArray[index];
  }

  //
  // propagation methods
  //
  @Override
  public List<EndData> getEnds() {
    return endList;
  }

  //
  // basic information methods
  //
  @Override
  public ComponentFactory getFactory() {
    return factory;
  }

  @Override
  public void setFactory(ComponentFactory fact) {
    factory = (InstanceFactory) fact;
  }

  @Override
  public Object getFeature(Object key) {
    Object ret = factory.getInstanceFeature(instance, key);
    if (ret != null) {
      return ret;
    } else if (key == ToolTipMaker.class) {
      Object defaultTip = factory.getDefaultToolTip();
      if (hasToolTips || defaultTip != null) return this;
    } else if (key == TextEditable.class) {
      return textField;
    }
    return null;
  }

  //
  // methods for Instance
  //
  public Instance getInstance() {
    return instance;
  }

  public InstanceStateImpl getInstanceStateImpl() {
    return instanceState;
  }

  //
  // location/extent methods
  //
  @Override
  public Location getLocation() {
    return loc;
  }

  List<Port> getPorts() {
    return portList;
  }

  @Override
  public String getToolTip(ComponentUserEvent e) {
    int x = e.getX();
    int y = e.getY();
    int i = -1;
    for (EndData end : endArray) {
      i++;
      if (end.getLocation().manhattanDistanceTo(x, y) < 10) {
        Port p = portList.get(i);
        return p.getToolTip();
      }
    }
    StringGetter defaultTip = factory.getDefaultToolTip();
    return defaultTip == null ? null : defaultTip.toString();
  }

  @Override
  public void propagate(CircuitState state) {
    factory.propagate(state.getInstanceState(this));
  }

  void recomputeBounds() {
    Location p = loc;
    bounds = factory.getOffsetBounds(attrs).translate(p.getX(), p.getY());
  }

  @Override
  public void removeComponentListener(ComponentListener l) {
    if (listeners != null) {
      listeners.remove(l);
      if (listeners.isEmpty()) listeners = null;
    }
  }

  public void setInstanceStateImpl(InstanceStateImpl instanceState) {
    this.instanceState = instanceState;
  }

  void setPorts(Port[] ports) {
    Port[] portsCopy = ports.clone();
    portList = new UnmodifiableList<>(portsCopy);
    computeEnds();
  }

  void setTextField(
      Attribute<String> labelAttr, Attribute<Font> fontAttr, int x, int y, int halign, int valign) {
    InstanceTextField field = textField;
    if (field == null) {
      field = new InstanceTextField(this);
      field.update(labelAttr, fontAttr, x, y, halign, valign);
      textField = field;
    } else {
      field.update(labelAttr, fontAttr, x, y, halign, valign);
    }
  }

  @Override
  public String toString() {
    String label = attrs.getValue(StdAttr.LABEL);
    return "InstanceComponent{factory="
        + factory.getName()
        + ",loc=("
        + loc
        + "),label="
        + label
        + "}@"
        + super.toString();
  }
}
