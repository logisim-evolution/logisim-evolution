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
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.SyntaxChecker;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

public final class InstanceComponent implements Component, AttributeListener, ToolTipMaker {
  private EventSourceWeakSupport<ComponentListener> listeners;
  @Getter @Setter private InstanceFactory factory;
  @Getter private final Instance instance;
  @Getter private final Location location;
  @Getter private Bounds bounds;
  @Getter private List<Port> ports;
  private EndData[] endArray;
  @Getter private List<EndData> ends;
  private boolean hasToolTips;
  private HashSet<Attribute<BitWidth>> widthAttrs;
  @Getter private final AttributeSet attributeSet;
  private boolean attrListenRequested;
  private InstanceTextField textField;
  @Getter private InstanceStateImpl instanceStateImpl;
  private boolean doMarkInstance;
  private boolean doMarkLabel;

  public InstanceComponent(InstanceFactory factory, Location loc, AttributeSet attrs) {
    this.listeners = null;
    this.factory = factory;
    this.instance = Instance.makeFor(this);
    this.location = loc;
    this.bounds = factory.getOffsetBounds(attrs).translate(loc.getX(), loc.getY());
    this.ports = factory.getPorts();
    this.endArray = null;
    this.hasToolTips = false;
    this.attributeSet = attrs;
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
    val attr = e.getAttribute();
    if (e.getAttribute().equals(StdAttr.LABEL)) {
      @SuppressWarnings("unchecked")
      val lattr = (Attribute<String>) e.getAttribute();
      val value = (String) e.getSource().getValue(e.getAttribute());
      val oldValue = e.getOldValue() != null ? (String) e.getOldValue() : "";
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
    val ports = this.ports;
    val esOld = endArray;
    val esOldLength = esOld == null ? 0 : esOld.length;
    var es = esOld;
    if (es == null || es.length != ports.size()) {
      es = new EndData[ports.size()];
      if (esOldLength > 0) {
        val toCopy = Math.min(esOldLength, es.length);
        System.arraycopy(esOld, 0, es, 0, toCopy);
      }
    }
    HashSet<Attribute<BitWidth>> wattrs = null;
    var toolTipFound = false;
    ArrayList<EndData> endsChangedOld = null;
    ArrayList<EndData> endsChangedNew = null;
    val pit = ports.iterator();
    for (var i = 0; pit.hasNext() || i < esOldLength; i++) {
      val p = pit.hasNext() ? pit.next() : null;
      val oldEnd = i < esOldLength ? esOld[i] : null;
      val newEnd = p == null ? null : p.toEnd(location, attributeSet);
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
        val attr = p.getWidthAttribute();
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
      val oldWattrs = widthAttrs;
      if (wattrs == null && oldWattrs != null) {
        getAttributeSet().removeAttributeListener(this);
      } else if (wattrs != null && oldWattrs == null) {
        getAttributeSet().addAttributeListener(this);
      }
    }
    if (es != esOld) {
      endArray = es;
      ends = new UnmodifiableList<>(es);
    }
    widthAttrs = wattrs;
    hasToolTips = toolTipFound;
    if (endsChangedOld != null) {
      fireEndsChanged(endsChangedOld, endsChangedNew);
    }
  }

  @Override
  public boolean contains(Location pt) {
    val translated = pt.translate(-location.getX(), -location.getY());
    val factory = instance.getFactory();
    return factory.contains(translated, instance.getAttributeSet());
  }

  @Override
  public boolean contains(Location pt, Graphics g) {
    val field = textField;
    if (field != null && field.getBounds(g).contains(pt)) return true;
    else return contains(pt);
  }

  //
  // drawing methods
  //
  @Override
  public void draw(ComponentDrawContext context) {
    val painter = context.getInstancePainter();
    painter.setInstance(this);
    factory.paintInstance(painter);
    if (doMarkInstance) {
      val gfx = painter.getGraphics();
      val bounds = painter.getBounds();
      val current = gfx.getColor();
      gfx.setColor(Netlist.DRC_INSTANCE_MARK_COLOR);
      GraphicsUtil.switchToWidth(gfx, 2);
      gfx.drawRoundRect(bounds.getX() - 10, bounds.getY() - 10, bounds.getWidth() + 20, bounds.getHeight() + 20, 40, 40);
      GraphicsUtil.switchToWidth(gfx, 1);
      gfx.setColor(current);
    }
  }

  //
  // methods for InstancePainter
  //
  void drawLabel(ComponentDrawContext context) {
    val field = textField;
    if (field != null) {
      field.draw(this, context);
      if (doMarkLabel) {
        val gfx = context.getGraphics();
        val bounds = field.getBounds(gfx);
        val currentColor = gfx.getColor();
        gfx.setColor(Netlist.DRC_LABEL_MARK_COLOR);
        GraphicsUtil.switchToWidth(gfx, 2);
        gfx.drawRoundRect(bounds.getX() - 10, bounds.getY() - 10, bounds.getWidth() + 20, bounds.getHeight() + 20, 40, 40);
        GraphicsUtil.switchToWidth(gfx, 1);
        gfx.setColor(currentColor);
      }
    }
  }

  @Override
  public boolean endsAt(Location pt) {
    for (val end : endArray) {
      if (end.getLocation().equals(pt)) return true;
    }
    return false;
  }

  @Override
  public void expose(ComponentDrawContext context) {
    val b = bounds;
    context.getDestination().repaint(b.getX(), b.getY(), b.getWidth(), b.getHeight());
  }

  private void fireLabelChanged(AttributeEvent attre) {
    val ls = listeners;
    if (ls != null) {
      ComponentEvent e = null;
      for (val l : ls) {
        if (e == null) e = new ComponentEvent(this, null, attre);
        l.LabelChanged(e);
      }
    }
  }

  private void fireEndsChanged(ArrayList<EndData> oldEnds, ArrayList<EndData> newEnds) {
    val ls = listeners;
    if (ls != null) {
      ComponentEvent e = null;
      for (val l : ls) {
        if (e == null) e = new ComponentEvent(this, oldEnds, newEnds);
        l.endChanged(e);
      }
    }
  }

  void fireInvalidated() {
    val ls = listeners;
    if (ls != null) {
      ComponentEvent e = null;
      for (val l : ls) {
        if (e == null) e = new ComponentEvent(this);
        l.componentInvalidated(e);
      }
    }
  }

  @Override
  public Bounds getBounds(Graphics g) {
    var ret = bounds;
    val field = textField;
    if (field != null) ret = ret.add(field.getBounds(g));
    return ret;
  }

  @Override
  public EndData getEnd(int index) {
    return endArray[index];
  }


  //
  // basic information methods
  //
  @Override
  public Object getFeature(Object key) {
    val ret = factory.getInstanceFeature(instance, key);
    if (ret != null) {
      return ret;
    } else if (key == ToolTipMaker.class) {
      val defaultTip = factory.getDefaultToolTip();
      if (hasToolTips || defaultTip != null) return this;
    } else if (key == TextEditable.class) {
      return textField;
    }
    return null;
  }

  //
  // location/extent methods
  //
  @Override
  public String getToolTip(ComponentUserEvent e) {
    val x = e.getX();
    val y = e.getY();
    var i = -1;
    for (val end : endArray) {
      i++;
      if (end.getLocation().manhattanDistanceTo(x, y) < 10) {
        return ports.get(i).getToolTip();
      }
    }
    val defaultTip = factory.getDefaultToolTip();
    return defaultTip == null ? null : defaultTip.toString();
  }

  @Override
  public void propagate(CircuitState state) {
    factory.propagate(state.getInstanceState(this));
  }

  void recomputeBounds() {
    val p = location;
    bounds = factory.getOffsetBounds(attributeSet).translate(p.getX(), p.getY());
  }

  @Override
  public void removeComponentListener(ComponentListener l) {
    if (listeners != null) {
      listeners.remove(l);
      if (listeners.isEmpty()) listeners = null;
    }
  }

  public void setInstanceStateImpl(InstanceStateImpl instanceState) {
    this.instanceStateImpl = instanceState;
  }

  void setPorts(Port[] ports) {
    val portsCopy = ports.clone();
    this.ports = new UnmodifiableList<>(portsCopy);
    computeEnds();
  }

  void setTextField(Attribute<String> labelAttr, Attribute<Font> fontAttr, int x, int y, int halign, int valign) {
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
    return "InstanceComponent{factory="
        + factory.getName()
        + ",loc=("
        + location
        + "),label="
        + attributeSet.getValue(StdAttr.LABEL)
        + "}@"
        + super.toString();
  }
}
