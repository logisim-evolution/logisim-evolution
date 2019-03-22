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

package com.cburch.draw.shapes;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import java.awt.Color;

abstract class FillableCanvasObject extends AbstractCanvasObject {
  private AttributeOption paintType;
  private int strokeWidth;
  private Color strokeColor;
  private Color fillColor;

  public FillableCanvasObject() {
    paintType = DrawAttr.PAINT_STROKE;
    strokeWidth = 1;
    strokeColor = Color.BLACK;
    fillColor = Color.WHITE;
  }

  public AttributeOption getPaintType() {
    return paintType;
  }

  public int getStrokeWidth() {
    return strokeWidth;
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
    } else {
      return null;
    }
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof FillableCanvasObject) {
      FillableCanvasObject that = (FillableCanvasObject) other;
      boolean ret = this.paintType == that.paintType;
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
    } else {
      return false;
    }
  }

  @Override
  public int matchesHashCode() {
    int ret = paintType.hashCode();
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
      strokeWidth = ((Integer) value).intValue();
    }
  }
}
