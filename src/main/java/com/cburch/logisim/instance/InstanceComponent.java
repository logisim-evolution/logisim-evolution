/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
import com.cburch.logisim.util.SyntaxChecker;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashSet;
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
    var ls = listeners;
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
    final var attr = e.getAttribute();
    if (e.getAttribute().equals(StdAttr.LABEL)) {
      @SuppressWarnings("unchecked")
      final var lAttr = (Attribute<String>) e.getAttribute();
      final var value = (String) e.getSource().getValue(e.getAttribute());
      final var oldValue = e.getOldValue() != null ? (String) e.getOldValue() : "";
      if (!oldValue.equals(value)) {
        if (!SyntaxChecker.isVariableNameAcceptable(value, true)) {
          e.getSource().setValue(lAttr, oldValue);
        } else if (getFactory().getName().equalsIgnoreCase(value)) {
          OptionPane.showMessageDialog(null, S.get("MatchedLabelNameError"));
          e.getSource().setValue(lAttr, oldValue);
        } else if (CorrectLabel.isKeyword(value, false)) {
          OptionPane.showMessageDialog(null, "\"" + value + "\": " + S.get("KeywordNameError"));
          e.getSource().setValue(lAttr, oldValue);
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
    final var ports = portList;
    final var esOld = endArray;
    final var esOldLength = esOld == null ? 0 : esOld.length;
    var es = esOld;
    if (es == null || es.length != ports.size()) {
      es = new EndData[ports.size()];
      if (esOldLength > 0) {
        final var toCopy = Math.min(esOldLength, es.length);
        System.arraycopy(esOld, 0, es, 0, toCopy);
      }
    }
    HashSet<Attribute<BitWidth>> wAttrs = null;
    var toolTipFound = false;
    ArrayList<EndData> endsChangedOld = null;
    ArrayList<EndData> endsChangedNew = null;
    final var portIt = ports.iterator();
    for (var i = 0; portIt.hasNext() || i < esOldLength; i++) {
      final var p = portIt.hasNext() ? portIt.next() : null;
      final var oldEnd = i < esOldLength ? esOld[i] : null;
      final var newEnd = p == null ? null : p.toEnd(loc, attrs);
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
        final var attr = p.getWidthAttribute();
        if (attr != null) {
          if (wAttrs == null) {
            wAttrs = new HashSet<>();
          }
          wAttrs.add(attr);
        }

        if (p.getToolTip() != null) toolTipFound = true;
      }
    }
    if (!attrListenRequested) {
      final var oldWidthAttrs = widthAttrs;
      if (wAttrs == null && oldWidthAttrs != null) {
        getAttributeSet().removeAttributeListener(this);
      } else if (wAttrs != null && oldWidthAttrs == null) {
        getAttributeSet().addAttributeListener(this);
      }
    }
    if (es != esOld) {
      endArray = es;
      endList = new UnmodifiableList<>(es);
    }
    widthAttrs = wAttrs;
    hasToolTips = toolTipFound;
    if (endsChangedOld != null) {
      fireEndsChanged(endsChangedOld, endsChangedNew);
    }
  }

  @Override
  public boolean contains(Location pt) {
    final var translated = pt.translate(-loc.getX(), -loc.getY());
    final var factory = instance.getFactory();
    return factory.contains(translated, instance.getAttributeSet());
  }

  @Override
  public boolean contains(Location pt, Graphics g) {
    final var field = textField;
    return (field != null && field.getBounds(g).contains(pt)) ? true : contains(pt);
  }

  //
  // drawing methods
  //
  @Override
  public void draw(ComponentDrawContext context) {
    final var painter = context.getInstancePainter();
    painter.setInstance(this);
    factory.paintInstance(painter);
    if (doMarkInstance) {
      final var g = painter.getGraphics();
      final var bds = painter.getBounds();
      final var current = g.getColor();
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
    final var field = textField;
    if (field != null) {
      field.draw(this, context);
      if (doMarkLabel) {
        final var g = context.getGraphics();
        final var bds = field.getBounds(g);
        final var current = g.getColor();
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
    final var ends = endArray;
    for (final var end : ends) {
      if (end.getLocation().equals(pt)) return true;
    }
    return false;
  }

  @Override
  public void expose(ComponentDrawContext context) {
    context
        .getDestination()
        .repaint(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
  }

  private void fireLabelChanged(AttributeEvent attre) {
    final var listeners = this.listeners;
    if (listeners != null) {
      ComponentEvent e = null;
      for (final var listener : listeners) {
        if (e == null) e = new ComponentEvent(this, null, attre);
        listener.labelChanged(e);
      }
    }
  }

  private void fireEndsChanged(ArrayList<EndData> oldEnds, ArrayList<EndData> newEnds) {
    final var listeners = this.listeners;
    if (listeners != null) {
      ComponentEvent e = null;
      for (final var listener : listeners) {
        if (e == null) e = new ComponentEvent(this, oldEnds, newEnds);
        listener.endChanged(e);
      }
    }
  }

  void fireInvalidated() {
    final var listeners = this.listeners;
    if (listeners != null) {
      ComponentEvent e = null;
      for (final var listener : listeners) {
        if (e == null) e = new ComponentEvent(this);
        listener.componentInvalidated(e);
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
    var ret = bounds;
    if (textField != null) ret = ret.add(textField.getBounds(g));
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
    var i = 0;
    for (final var end : endArray) {
      if (end.getLocation().manhattanDistanceTo(e.getX(), e.getY()) < 10) {
        return portList.get(i).getToolTip();
      }
      i++;
    }
    final var defaultTip = factory.getDefaultToolTip();
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
    final var portsCopy = ports.clone();
    portList = new UnmodifiableList<>(portsCopy);
    computeEnds();
  }

  void setTextField(
      Attribute<String> labelAttr, Attribute<Font> fontAttr, int x, int y, int halign, int valign) {
    var field = textField;
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
    final var label = attrs.getValue(StdAttr.LABEL);
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
