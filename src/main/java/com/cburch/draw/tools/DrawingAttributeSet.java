/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.tools;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class DrawingAttributeSet implements AttributeSet, Cloneable {
  static final List<Attribute<?>> ATTRS_ALL =
      UnmodifiableList.create(
          new Attribute<?>[] {
            DrawAttr.FONT,
            DrawAttr.HALIGNMENT,
            DrawAttr.VALIGNMENT,
            DrawAttr.PAINT_TYPE,
            DrawAttr.STROKE_WIDTH,
            DrawAttr.STROKE_COLOR,
            DrawAttr.FILL_COLOR,
            DrawAttr.TEXT_DEFAULT_FILL,
            DrawAttr.CORNER_RADIUS
          });
  static final List<Object> DEFAULTS_ALL =
      Arrays.asList(
          DrawAttr.DEFAULT_FONT, DrawAttr.HALIGN_CENTER, DrawAttr.VALIGN_MIDDLE,
          DrawAttr.PAINT_STROKE, 1, Color.BLACK,
          Color.WHITE, Color.BLACK, 10);
  private final List<Attribute<?>> attrs;
  private EventSourceWeakSupport<AttributeListener> listeners;
  private List<Object> values;

  public DrawingAttributeSet() {
    listeners = new EventSourceWeakSupport<>();
    attrs = ATTRS_ALL;
    values = DEFAULTS_ALL;
  }

  @Override
  public void addAttributeListener(AttributeListener l) {
    listeners.add(l);
  }

  public <E extends CanvasObject> E applyTo(E drawable) {
    AbstractCanvasObject d = (AbstractCanvasObject) drawable;
    // use a for(i...) loop since the attribute list may change as we go on
    for (var i = 0; i < d.getAttributes().size(); i++) {
      Attribute<?> attr = d.getAttributes().get(i);
      @SuppressWarnings("unchecked")
      Attribute<Object> a = (Attribute<Object>) attr;
      if (attr == DrawAttr.FILL_COLOR && this.containsAttribute(DrawAttr.TEXT_DEFAULT_FILL)) {
        d.setValue(a, this.getValue(DrawAttr.TEXT_DEFAULT_FILL));
      } else if (this.containsAttribute(a)) {
        d.setValue(a, this.getValue(a));
      }
    }
    return drawable;
  }

  @Override
  public Object clone() {
    try {
      DrawingAttributeSet ret = (DrawingAttributeSet) super.clone();
      ret.listeners = new EventSourceWeakSupport<>();
      ret.values = new ArrayList<>(this.values);
      return ret;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  @Override
  public boolean containsAttribute(Attribute<?> attr) {
    return attrs.contains(attr);
  }

  public AttributeSet createSubset(AbstractTool tool) {
    return new Restriction(tool);
  }

  @Override
  public Attribute<?> getAttribute(String name) {
    for (Attribute<?> attr : attrs) {
      if (attr.getName().equals(name)) return attr;
    }
    return null;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attrs;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    Iterator<Attribute<?>> ait = attrs.iterator();
    Iterator<Object> vit = values.iterator();
    while (ait.hasNext()) {
      Object a = ait.next();
      Object v = vit.next();
      if (a.equals(attr)) {
        return (V) v;
      }
    }
    return null;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return false;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave();
  }

  @Override
  public void removeAttributeListener(AttributeListener l) {
    listeners.remove(l);
  }

  @Override
  public void setReadOnly(Attribute<?> attr, boolean value) {
    throw new UnsupportedOperationException("setReadOnly");
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    final var ait = attrs.iterator();
    final var vit = values.listIterator();
    while (ait.hasNext()) {
      final var a = ait.next();
      vit.next();
      if (a.equals(attr)) {
        vit.set(value);
        var event = new AttributeEvent(this, attr, value, null);
        for (final var listener : listeners) {
          listener.attributeValueChanged(event);
        }
        if (attr == DrawAttr.PAINT_TYPE) {
          event = new AttributeEvent(this);
          for (final var listener : listeners) {
            listener.attributeListChanged(event);
          }
        }
        return;
      }
    }
    throw new IllegalArgumentException(attr.toString());
  }

  private class Restriction extends AbstractAttributeSet implements AttributeListener {
    private final AbstractTool tool;
    private List<Attribute<?>> selectedAttrs;
    private List<Attribute<?>> selectedView;

    Restriction(AbstractTool tool) {
      this.tool = tool;
      updateAttributes();
    }

    //
    // AttributeListener methods
    //
    @Override
    public void attributeListChanged(AttributeEvent e) {
      fireAttributeListChanged();
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      if (selectedAttrs.contains(e.getAttribute())) {
        @SuppressWarnings("unchecked")
        Attribute<Object> attr = (Attribute<Object>) e.getAttribute();
        fireAttributeValueChanged(attr, e.getValue(), e.getOldValue());
      }
      updateAttributes();
    }

    @Override
    protected void copyInto(AbstractAttributeSet dest) {
      DrawingAttributeSet.this.addAttributeListener(this);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
      return selectedView;
    }

    @Override
    public <V> V getValue(Attribute<V> attr) {
      return DrawingAttributeSet.this.getValue(attr);
    }

    @Override
    public <V> void setValue(Attribute<V> attr, V value) {
      DrawingAttributeSet.this.setValue(attr, value);
      updateAttributes();
    }

    private void updateAttributes() {
      List<Attribute<?>> toolAttrs;
      if (tool == null) {
        toolAttrs = Collections.emptyList();
      } else {
        toolAttrs = tool.getAttributes();
      }
      if (!toolAttrs.equals(selectedAttrs)) {
        selectedAttrs = new ArrayList<>(toolAttrs);
        selectedView = Collections.unmodifiableList(selectedAttrs);
        DrawingAttributeSet.this.addAttributeListener(this);
        fireAttributeListChanged();
      }
    }
  }
}
