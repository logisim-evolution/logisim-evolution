package com.cburch.draw.shapes;

import static com.cburch.draw.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import java.awt.Color;
import java.awt.Graphics;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Custom circuit appearance image shape.
 */
public class Image extends Rectangular {
  private String imageData;
  private java.awt.Image img;

  /**
   * Creates a new image shape.

   * @param imageData - Base64 image data
   * @param x - Image x position
   * @param y - Image y position
   * @param w - Image width
   * @param h - Image height
   */
  public Image(String imageData, int x, int y, int w, int h) {
    super(x, y, w, h);

    this.imageData = imageData;
    loadImage();

  }

  public String getImageData() {
    return imageData;
  }

  @Override
  public Element toSvgElement(Document doc) {
    return SvgCreator.createImage(doc, this);
  }

  @Override
  public String getDisplayName() {
    return S.get("shapeImage");
  }

  @Override
  protected boolean contains(int x, int y, int w, int h, Location q) {
    return isInRect(q.getX(), q.getY(), x, y, w, h);
  }

  @Override
  protected void draw(Graphics g, int x, int y, int w, int h) {
    if (img == null) {
      g.setColor(Color.BLACK);
      g.drawRect(x, y, w, h);
      return;
    }
    g.drawImage(this.img, x, y, x + w, y + h, 0, 0, img.getWidth(null), img.getHeight(null), null);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.ATTRS_IMAGE;
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    super.updateValue(attr, value);
    if (attr == DrawAttr.IMAGE_DATA) {
      imageData = (String) value;
      loadImage();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == DrawAttr.IMAGE_DATA) {
      return (V) imageData;
    }
    return super.getValue(attr);
  }

  private void loadImage() {
    try {
      img = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(imageData)));
    } catch (IOException e) {
      System.err.println("Failed to load base64 image of circuit appearance.");
    }
  }
}
