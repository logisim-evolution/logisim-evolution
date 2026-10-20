/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.tools;

import com.cburch.draw.icons.DrawShapeIcon;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.data.Attribute;
import java.awt.Graphics;
import java.util.List;
import javax.swing.Icon;

public class RectangleTool extends RectangularTool {
  private final DrawingAttributeSet attrs;

  public RectangleTool(DrawingAttributeSet attrs) {
    this.attrs = attrs;
  }

  @Override
  public CanvasObject createShape(int x, int y, int w, int h) {
    return attrs.applyTo(new Rectangle(x, y, w, h));
  }

  @Override
  public void drawShape(Graphics g, int x, int y, int w, int h) {
    g.drawRect(x, y, w, h);
  }

  @Override
  public void fillShape(Graphics g, int x, int y, int w, int h) {
    g.fillRect(x, y, w, h);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getFillAttributes(attrs.getValue(DrawAttr.PAINT_TYPE));
  }

  @Override
  public Icon getIcon() {
    return new DrawShapeIcon(DrawShapeIcon.RECTANGLE);
  }
}
