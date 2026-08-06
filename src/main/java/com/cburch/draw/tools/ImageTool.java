/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.tools;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.ImageShape;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.icons.ImageIcon;

import java.awt.Graphics;
import java.util.List;
import javax.swing.Icon;

public class ImageTool extends RectangularTool {
  private static final ImageIcon ICON = new ImageIcon();
  private final DrawingAttributeSet attrs;

  public ImageTool(DrawingAttributeSet attrs) {
    this.attrs = attrs;
  }

  @Override
  public CanvasObject createShape(int x, int y, int w, int h) {
    return attrs.applyTo(new ImageShape(x, y, w, h));
  }

  @Override
  public void drawShape(Graphics g, int x, int y, int w, int h) {
    g.drawRect(x, y, w, h);
    g.drawLine(x, y, x + w, y + h);
    g.drawLine(x, y + h, x + w, y);
  }

  @Override
  public void fillShape(Graphics g, int x, int y, int w, int h) {
    g.fillRect(x, y, w, h);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return List.of(ImageShape.SCALE_ATTR, ImageShape.LICENSE_ATTR, ImageShape.ATTRIBUTION_ATTR);
  }

  @Override
  public String getDescription() {
    return S.get("shapeImage");
  }

  @Override
  public Icon getIcon() {
    return ICON;
  }
}
