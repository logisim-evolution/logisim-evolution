/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import java.awt.Color;
import lombok.Getter;
import lombok.val;

abstract class FillableCanvasObject extends AbstractCanvasObject {
  @Getter private AttributeOption paintType;
  @Getter private int strokeWidth;
  private Color strokeColor;
  private Color fillColor;

  public FillableCanvasObject() {
    paintType = DrawAttr.PAINT_STROKE;
    strokeWidth = 1;
    strokeColor = Color.BLACK;
    fillColor = Color.WHITE;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == DrawAttr.PAINT_TYPE) {
      return (V) paintType;
    } else if (attr == DrawAttr.STROKE_COLOR) {
      return (V) strokeColor;
    } else if (attr == DrawAttr.FILL_COLOR) {
      return (V) fillColor;
    } else if (attr == DrawAttr.STROKE_WIDTH) {
      return (V) Integer.valueOf(strokeWidth);
    }
    return null;
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (!(other instanceof FillableCanvasObject)) {
      return false;
    }
    val that = (FillableCanvasObject) other;
    var ret = this.paintType == that.paintType;
    if (ret && this.paintType != DrawAttr.PAINT_FILL) {
      ret =
          ret
              && this.strokeWidth == that.strokeWidth
              && this.strokeColor.equals(that.strokeColor);
    }
    if (ret && this.paintType != DrawAttr.PAINT_STROKE) {
      ret = ret && this.fillColor.equals(that.fillColor);
    }
    return ret;
  }

  @Override
  public int matchesHashCode() {
    var ret = paintType.hashCode();
    if (paintType != DrawAttr.PAINT_FILL) {
      ret = ret * 31 + strokeWidth;
      ret = ret * 31 + strokeColor.hashCode();
    } else {
      ret = ret * 31 * 31;
    }
    if (paintType != DrawAttr.PAINT_STROKE) {
      ret = ret * 31 + fillColor.hashCode();
    } else {
      ret = ret * 31;
    }
    return ret;
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == DrawAttr.PAINT_TYPE) {
      paintType = (AttributeOption) value;
      fireAttributeListChanged();
    } else if (attr == DrawAttr.STROKE_COLOR) {
      strokeColor = (Color) value;
    } else if (attr == DrawAttr.FILL_COLOR) {
      fillColor = (Color) value;
    } else if (attr == DrawAttr.STROKE_WIDTH) {
      strokeWidth = (Integer) value;
    }
  }
}
