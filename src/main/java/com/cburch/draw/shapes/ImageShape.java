/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.std.Strings;
import com.cburch.logisim.std.base.Image;
import com.cburch.logisim.util.ImageUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ImageShape extends Rectangular {
  public static final Attribute<String> IMAGE_ATTR = Image.ATTR_IMAGE;
  public static final Attribute<Integer> ATTR_X = Attributes.forInteger("x", Strings.S.getter("imageLocationXAttr"));
  public static final Attribute<Integer> ATTR_Y = Attributes.forInteger("y", Strings.S.getter("imageLocationYAttr"));
  public static final Attribute<Integer> WIDTH_ATTR = Image.ATTR_WIDTH;
  public static final Attribute<Integer> HEIGHT_ATTR = Image.ATTR_HEIGHT;
  public static final Attribute<AttributeOption> SCALE_ATTR = Image.ATTR_SCALE;
  public static final Attribute<String> DATA_SIZE_ATTR = Image.ATTR_DATA_SIZE;
  public static final Attribute<AttributeOption> LICENSE_ATTR = Image.ATTR_LICENSE;
  public static final Attribute<String> ATTRIBUTION_ATTR = Image.ATTR_ATTRIBUTION;

  private String imageSource = "";
  private BufferedImage cachedImage = null;
  private AttributeOption scale = SCALE_ATTR.parse("fit");
  private AttributeOption license = Image.LICENSE_UNSPECIFIED;
  private String attribution = "";

  public ImageShape(int x, int y, int w, int h) {
    super(x, y, w, h);
  }

  public String getImageSource() {
    return imageSource;
  }

  public void setImageSource(String source) {
    this.imageSource = source;
    this.cachedImage = ImageUtil.loadBufferedImage(source);
  }

  public String getAttribution() {

    return attribution;
  }

  @Override
  protected boolean contains(int x, int y, int w, int h, Location q) {
    return isInRect(q.getX(), q.getY(), x, y, w, h);
  }

  @Override
  public void draw(Graphics g, int x, int y, int w, int h) {
    if (cachedImage == null && imageSource != null && !imageSource.isBlank()) {
      cachedImage = ImageUtil.loadBufferedImage(imageSource);
    }

    if (cachedImage != null) {
      var drawImg = cachedImage;
      if (g.getClass().getName().contains("ComponentDrawContext")) {
        try {
          final var m = g.getClass().getMethod("isPrintView");
          if ((Boolean) m.invoke(g)) {
            drawImg = ImageUtil.toGrayscale(cachedImage);
          }
        } catch (Exception ignored) {
        }
      }
      final var scaleOpt = scale == null ? "fit" : (String) scale.getValue();
      if ("stretch".equals(scaleOpt)) {
        g.drawImage(drawImg, x, y, w, h, null);
      } else if ("cover".equals(scaleOpt)) {
        final var imgW = drawImg.getWidth();
        final var imgH = drawImg.getHeight();
        final var scaleFactor = Math.max((double) w / imgW, (double) h / imgH);
        final var targetW = Math.max(1, (int) (imgW * scaleFactor));
        final var targetH = Math.max(1, (int) (imgH * scaleFactor));
        final var targetX = x + (w - targetW) / 2;
        final var targetY = y + (h - targetH) / 2;
        final var oldClip = g.getClip();
        g.clipRect(x, y, w, h);
        g.drawImage(drawImg, targetX, targetY, targetW, targetH, null);
        g.setClip(oldClip);
      } else { // fit — preserve aspect ratio
        final var imgW = drawImg.getWidth();
        final var imgH = drawImg.getHeight();
        final var scaleFactor = Math.min((double) w / imgW, (double) h / imgH);
        final var targetW = Math.max(1, (int) (imgW * scaleFactor));
        final var targetH = Math.max(1, (int) (imgH * scaleFactor));
        final var targetX = x + (w - targetW) / 2;
        final var targetY = y + (h - targetH) / 2;
        g.drawImage(drawImg, targetX, targetY, targetW, targetH, null);
      }
    } else {
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(x, y, w, h);
      g.setColor(Color.DARK_GRAY);
      g.drawRect(x, y, w, h);
      g.drawLine(x, y, x + w, y + h);
      g.drawLine(x, y + h, x + w, y);
    }
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return List.of(IMAGE_ATTR, ATTR_X, ATTR_Y, WIDTH_ATTR, HEIGHT_ATTR, SCALE_ATTR, DATA_SIZE_ATTR, LICENSE_ATTR, ATTRIBUTION_ATTR);
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    if (attr == DATA_SIZE_ATTR) return true;
    return super.isReadOnly(attr);
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    if (attr == DATA_SIZE_ATTR) return false;
    return super.isToSave(attr);
  }

  public String getDataSizeFormatted() {
    return ImageUtil.formatDataSize(imageSource);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == IMAGE_ATTR) return (V) imageSource;
    if (attr == ATTR_X) return (V) Integer.valueOf(getX());
    if (attr == ATTR_Y) return (V) Integer.valueOf(getY());
    if (attr == WIDTH_ATTR) return (V) Integer.valueOf(getWidth());
    if (attr == HEIGHT_ATTR) return (V) Integer.valueOf(getHeight());
    if (attr == SCALE_ATTR) return (V) scale;
    if (attr == DATA_SIZE_ATTR) return (V) getDataSizeFormatted();
    if (attr == LICENSE_ATTR) return (V) license;
    if (attr == ATTRIBUTION_ATTR) return (V) attribution;
    return super.getValue(attr);
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == IMAGE_ATTR) {
      setImageSource((String) value);
    } else if (attr == ATTR_X) {
      final var newX = (Integer) value;
      setBounds(newX, getY(), getWidth(), getHeight());
    } else if (attr == ATTR_Y) {
      final var newY = (Integer) value;
      setBounds(getX(), newY, getWidth(), getHeight());
    } else if (attr == WIDTH_ATTR) {
      final var val = (Integer) value;
      if (val <= 0 && cachedImage != null) {
        setBounds(getX(), getY(), cachedImage.getWidth(), cachedImage.getHeight());
      } else {
        setBounds(getX(), getY(), Math.max(1, val), getHeight());
      }
    } else if (attr == HEIGHT_ATTR) {
      final var val = (Integer) value;
      if (val <= 0 && cachedImage != null) {
        setBounds(getX(), getY(), cachedImage.getWidth(), cachedImage.getHeight());
      } else {
        setBounds(getX(), getY(), getWidth(), Math.max(1, val));
      }
    } else if (attr == SCALE_ATTR) {
      scale = (AttributeOption) value;
    } else if (attr == LICENSE_ATTR) {
      license = (AttributeOption) value;
    } else if (attr == ATTRIBUTION_ATTR) {
      attribution = value == null ? "" : (String) value;
    } else {
      super.updateValue(attr, value);
    }
  }

  @Override
  public String getDisplayName() {
    return S.get("shapeImage");
  }

  @Override
  public ImageShape clone() {
    final var ret = (ImageShape) super.clone();
    ret.imageSource = this.imageSource;
    ret.cachedImage = this.cachedImage;
    ret.license = this.license;
    ret.attribution = this.attribution;
    return ret;
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof ImageShape that) {
      return super.matches(other) && Objects.equals(this.imageSource, that.imageSource);
    }
    return false;
  }

  @Override
  public String toString() {
    return "ImageShape:" + getBounds();
  }

  @Override
  public Element toSvgElement(Document doc) {
    return SvgCreator.createImage(doc, this);
  }
}
