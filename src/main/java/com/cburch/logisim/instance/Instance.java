/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Font;
import java.util.List;

public final class Instance implements Location.At {

  public static Instance getInstanceFor(Component comp) {
    return (comp instanceof InstanceComponent instComp)
           ? instComp.getInstance()
           : null;
  }

  public static InstanceComponent getComponentFor(Instance instance) {
    return instance.comp;
  }

  static Instance makeFor(InstanceComponent comp) {
    return new Instance(comp);
  }

  private Instance(InstanceComponent comp) {
    this.comp = comp;
  }

  private final InstanceComponent comp;

  public void addAttributeListener() {
    comp.addAttributeListener(this);
  }

  public void fireInvalidated() {
    comp.fireInvalidated();
  }

  public AttributeSet getAttributeSet() {
    return comp.getAttributeSet();
  }

  public <E> E getAttributeValue(Attribute<E> attr) {
    return comp.getAttributeSet().getValue(attr);
  }

  public Bounds getBounds() {
    return comp.getBounds();
  }

  public InstanceComponent getComponent() {
    return comp;
  }

  public InstanceData getData(CircuitState state) {
    return (InstanceData) state.getData(comp);
  }

  public InstanceFactory getFactory() {
    return (InstanceFactory) comp.getFactory();
  }

  @Override
  public Location getLocation() {
    return comp.getLocation();
  }

  public Location getPortLocation(int index) {
    return comp.getEnd(index).getLocation();
  }

  public List<Port> getPorts() {
    return comp.getPorts();
  }

  public void recomputeBounds() {
    comp.recomputeBounds();
  }

  public void setAttributeReadOnly(Attribute<?> attr, boolean value) {
    comp.getAttributeSet().setReadOnly(attr, value);
  }

  public void setData(CircuitState state, InstanceData data) {
    state.setData(comp, data);
  }

  public void setPorts(Port[] ports) {
    comp.setPorts(ports);
  }

  public void setTextField(Attribute<String> labelAttr, Attribute<Font> fontAttr, int x, int y, int hAlign, int vAlign) {
    comp.setTextField(labelAttr, fontAttr, x, y, hAlign, vAlign);
  }

  public static final int AVOID_TOP = 1;
  public static final int AVOID_RIGHT = 2;
  public static final int AVOID_BOTTOM = 4;
  public static final int AVOID_LEFT = 8;
  public static final int AVOID_SIDES = AVOID_LEFT | AVOID_RIGHT;
  public static final int AVOID_CENTER = 16;

  public void computeLabelTextField(int avoid) {
    Object labelLoc = getAttributeValue(StdAttr.LABEL_LOC);
    computeLabelTextField(avoid, labelLoc);
  }

  public void computeLabelTextField(int avoid, Object labelLoc) {
    if (avoid != 0) {
      final var facing = getAttributeValue(StdAttr.FACING);
      if (facing == Direction.NORTH)
        avoid = (avoid & 0x10) | ((avoid << 1) & 0xf) | ((avoid & 0xf) >> 3);
      else if (facing == Direction.EAST)
        avoid = (avoid & 0x10) | ((avoid << 2) & 0xf) | ((avoid & 0xf) >> 2);
      else if (facing == Direction.SOUTH)
        avoid = (avoid & 0x10) | ((avoid << 3) & 0xf) | ((avoid & 0xf) >> 1);
    }

    final var bds = getBounds();
    var x = bds.getX() + bds.getWidth() / 2;
    var y = bds.getY() + bds.getHeight() / 2;
    var hAlign = GraphicsUtil.H_CENTER;
    var vAlign = GraphicsUtil.V_CENTER;
    if (labelLoc == StdAttr.LABEL_CENTER) {
      var offset = 0;
      if ((avoid & AVOID_CENTER) != 0) offset = 3;
      x = bds.getX() + (bds.getWidth() - offset) / 2;
      y = bds.getY() + (bds.getHeight() - offset) / 2;
    } else if (labelLoc == Direction.NORTH) {
      y = bds.getY() - 2;
      vAlign = GraphicsUtil.V_BOTTOM;
      if ((avoid & AVOID_TOP) != 0) {
        x += 2;
        hAlign = GraphicsUtil.H_LEFT;
      }
    } else if (labelLoc == Direction.SOUTH) {
      y = bds.getY() + bds.getHeight() + 2;
      vAlign = GraphicsUtil.V_TOP;
      if ((avoid & AVOID_BOTTOM) != 0) {
        x += 2;
        hAlign = GraphicsUtil.H_LEFT;
      }
    } else if (labelLoc == Direction.EAST) {
      x = bds.getX() + bds.getWidth() + 2;
      hAlign = GraphicsUtil.H_LEFT;
      if ((avoid & AVOID_RIGHT) != 0) {
        y -= 2;
        vAlign = GraphicsUtil.V_BOTTOM;
      }
    } else if (labelLoc == Direction.WEST) {
      x = bds.getX() - 2;
      hAlign = GraphicsUtil.H_RIGHT;
      if ((avoid & AVOID_LEFT) != 0) {
        y -= 2;
        vAlign = GraphicsUtil.V_BOTTOM;
      }
    }
    setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, hAlign, vAlign);
  }
}
