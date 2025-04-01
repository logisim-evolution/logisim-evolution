/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics;
import java.util.Objects;
import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractCanvasObject implements AttributeSet, CanvasObject, Cloneable {
  private static final int OVERLAP_TRIES = 50;
  private static final int GENERATE_RANDOM_TRIES = 20;

  private EventSourceWeakSupport<AttributeListener> listeners;

  public AbstractCanvasObject() {
    listeners = new EventSourceWeakSupport<>();
  }

  @Override
  public void addAttributeListener(AttributeListener l) {
    listeners.add(l);
  }

  @Override
  public Handle canDeleteHandle(Location loc) {
    return null;
  }

  @Override
  public Handle canInsertHandle(Location desired) {
    return null;
  }

  @Override
  public boolean canMoveHandle(Handle handle) {
    return false;
  }

  @Override
  public boolean canRemove() {
    return true;
  }

  @Override
  public CanvasObject clone() {
    try {
      final var ret = (AbstractCanvasObject) super.clone();
      ret.listeners = new EventSourceWeakSupport<>();
      return ret;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  @Override
  public boolean containsAttribute(Attribute<?> attr) {
    return getAttributes().contains(attr);
  }

  @Override
  public Handle deleteHandle(Handle handle) {
    throw new UnsupportedOperationException("deleteHandle");
  }

  protected void fireAttributeListChanged() {
    final var e = new AttributeEvent(this);
    for (final var listener : listeners) {
      listener.attributeListChanged(e);
    }
  }

  @Override
  public Attribute<?> getAttribute(String name) {
    for (final var attr : getAttributes()) {
      if (attr.getName().equals(name)) return attr;
    }
    return null;
  }

  // methods required by AttributeSet interface
  @Override
  public AttributeSet getAttributeSet() {
    return this;
  }

  @Override
  public String getDisplayNameAndLabel() {
    return getDisplayName();
  }

  protected Location getRandomPoint(Bounds bds, Random rand) {
    final var x = bds.getX();
    final var y = bds.getY();
    final var w = bds.getWidth();
    final var h = bds.getHeight();
    for (var i = 0; i < GENERATE_RANDOM_TRIES; i++) {
      final var loc = Location.create(x + rand.nextInt(w), y + rand.nextInt(h), false);
      if (contains(loc, false)) return loc;
    }
    return null;
  }

  @Override
  public void insertHandle(Handle desired, Handle previous) {
    throw new UnsupportedOperationException("insertHandle");
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
  public Handle moveHandle(HandleGesture gesture) {
    throw new UnsupportedOperationException("moveHandle");
  }


  @Override
  public boolean overlaps(CanvasObject other) {
    final var a = this.getBounds();
    final var b = other.getBounds();
    final var c = a.intersect(b);
    final var rand = new Random();

    if (c.getWidth() == 0 || c.getHeight() == 0) {
      return false;
    }

    if (other instanceof AbstractCanvasObject that) {
      return checkOverlapWithRotation(that, rand, c);
    } else {
      return checkOverlapWithoutRotation(other, rand, c);
    }
  }

  private boolean checkOverlapWithRotation(AbstractCanvasObject that, Random rand, Bounds c) {
    for (var i = 0; i < OVERLAP_TRIES; i++) {
      final var loc = (i % 2 == 0) ? this.getRandomPoint(c, rand) : that.getRandomPoint(c, rand);
      if (loc != null && checkContains(that, loc)) {
        return true;
      }
    }
    return false;
  }

  private boolean checkOverlapWithoutRotation(CanvasObject other, Random rand, Bounds c) {
    for (var i = 0; i < OVERLAP_TRIES; i++) {
      final var loc = this.getRandomPoint(c, rand);
      if (loc != null && checkContains(other, loc)) {
        return true;
      }
    }
    return false;
  }

  private boolean checkContains(CanvasObject obj, Location loc) {
    return obj.contains(loc, false);
  }


  @Override
  public void removeAttributeListener(AttributeListener l) {
    listeners.remove(l);
  }

  protected boolean setForFill(Graphics g) {
    final var attrs = getAttributes();
    if (attrs.contains(DrawAttr.PAINT_TYPE)) {
      final var value = getValue(DrawAttr.PAINT_TYPE);
      if (value == DrawAttr.PAINT_STROKE) return false;
    }

    final var color = getValue(DrawAttr.FILL_COLOR);
    if (color != null && color.getAlpha() == 0) {
      return false;
    } else {
      if (color != null) g.setColor(color);
      return true;
    }
  }

  protected boolean setForStroke(Graphics g) {
    final var attrs = getAttributes();
    if (attrs.contains(DrawAttr.PAINT_TYPE)) {
      final var value = getValue(DrawAttr.PAINT_TYPE);
      if (value == DrawAttr.PAINT_FILL) return false;
    }

    final var width = getValue(DrawAttr.STROKE_WIDTH);
    if (width != null && width > 0) {
      final var color = getValue(DrawAttr.STROKE_COLOR);
      if (color != null && color.getAlpha() == 0) {
        return false;
      } else {
        GraphicsUtil.switchToWidth(g, width);
        if (color != null) g.setColor(color);
        return true;
      }
    } else {
      return false;
    }
  }

  @Override
  public void setReadOnly(Attribute<?> attr, boolean value) {
    throw new UnsupportedOperationException("setReadOnly");
  }

  @Override
  public final <V> void setValue(Attribute<V> attr, V value) {
    final var old = getValue(attr);
    final var same = Objects.equals(old, value);
    if (!same) {
      updateValue(attr, value);
      final var e = new AttributeEvent(this, attr, value, old);
      for (final var listener : listeners) {
        listener.attributeValueChanged(e);
      }
    }
  }

  public abstract Element toSvgElement(Document doc);

  protected abstract void updateValue(Attribute<?> attr, Object value);
}
