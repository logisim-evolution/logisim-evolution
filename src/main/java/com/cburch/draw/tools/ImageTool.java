package com.cburch.draw.tools;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.icons.DrawImageIcon;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Image;
import com.cburch.logisim.data.Attribute;
import java.awt.Graphics;
import java.util.List;
import javax.swing.Icon;

/**
 * Image shape tool.
 */
public class ImageTool extends RectangularTool {
  private final DrawingAttributeSet attrs;

  public ImageTool(DrawingAttributeSet attrs) {
    this.attrs = attrs;
  }

  @Override
  public Icon getIcon() {
    return new DrawImageIcon();
  }

  @Override
  public CanvasObject createShape(int x, int y, int w, int h) {
    return attrs.applyTo(new Image("", x, y, w, h));
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
    return DrawAttr.ATTRS_IMAGE;
  }

  @Override
  public String getDescription() {
    return S.get("shapeImage");
  }
}
