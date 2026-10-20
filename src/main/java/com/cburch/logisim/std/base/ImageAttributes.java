/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.base;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ImageAttributes extends AbstractAttributeSet {
  private static final List<Attribute<?>> ATTRIBUTES = Arrays.asList(
      Image.ATTR_IMAGE, Image.ATTR_WIDTH, Image.ATTR_HEIGHT, Image.ATTR_SCALE, Image.ATTR_DATA_SIZE, Image.ATTR_LICENSE, Image.ATTR_ATTRIBUTION);

  private String imageSource;
  private Integer width;
  private Integer height;
  private AttributeOption scale;
  private AttributeOption license;
  private String attribution;
  private Bounds offsetBounds;
  private BufferedImage cachedImage;
  private boolean userSetDimensions;

  public ImageAttributes() {
    imageSource = "";
    width = 64;
    height = 64;
    scale = Image.ATTR_SCALE.parse("fit");
    license = Image.LICENSE_UNSPECIFIED;
    attribution = "";
    offsetBounds = Bounds.create(0, 0, width, height);
    cachedImage = null;
    userSetDimensions = false;
  }

  @Override
  protected void copyInto(AbstractAttributeSet destObj) {
    final var dest = (ImageAttributes) destObj;
    dest.imageSource = this.imageSource;
    dest.width = this.width;
    dest.height = this.height;
    dest.scale = this.scale;
    dest.license = this.license;
    dest.attribution = this.attribution;
    dest.offsetBounds = this.offsetBounds;
    dest.cachedImage = this.cachedImage;
    dest.userSetDimensions = this.userSetDimensions;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    if (attr == Image.ATTR_DATA_SIZE) return true;
    return super.isReadOnly(attr);
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    if (attr == Image.ATTR_DATA_SIZE) return false;
    return super.isToSave(attr);
  }

  public String getImageSource() {
    return imageSource;
  }

  public int getWidth() {
    return width == null ? 64 : width;
  }

  public int getHeight() {
    return height == null ? 64 : height;
  }

  public AttributeOption getScale() {
    return scale;
  }

  public Bounds getOffsetBounds() {
    return offsetBounds;
  }

  public boolean setOffsetBounds(Bounds value) {
    Bounds old = offsetBounds;
    boolean same = Objects.equals(old, value);
    if (!same) {
      offsetBounds = value;
    }
    return !same;
  }

  public BufferedImage getCachedImage() {
    return cachedImage;
  }

  public void setCachedImage(BufferedImage img) {
    this.cachedImage = img;
  }

  public String getDataSizeFormatted() {
    return ImageUtil.formatDataSize(imageSource);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == Image.ATTR_IMAGE)
      return (V) imageSource;
    if (attr == Image.ATTR_WIDTH)
      return (V) width;
    if (attr == Image.ATTR_HEIGHT)
      return (V) height;
    if (attr == Image.ATTR_SCALE)
      return (V) scale;
    if (attr == Image.ATTR_DATA_SIZE)
      return (V) getDataSizeFormatted();
    if (attr == Image.ATTR_LICENSE)
      return (V) license;
    if (attr == Image.ATTR_ATTRIBUTION)
      return (V) attribution;
    return null;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == Image.ATTR_IMAGE) {
      imageSource = (String) value;
      cachedImage = ImageUtil.loadBufferedImage(imageSource);
      if (cachedImage != null && !userSetDimensions) {
        width = cachedImage.getWidth();
        height = cachedImage.getHeight();
        offsetBounds = Bounds.create(0, 0, getWidth(), getHeight());
      }
    } else if (attr == Image.ATTR_WIDTH) {
      final var val = (Integer) value;
      if (val <= 0 && cachedImage != null) {
        width = cachedImage.getWidth();
        height = cachedImage.getHeight();
      } else {
        width = Math.max(1, val);
      }
      userSetDimensions = true;
      offsetBounds = Bounds.create(0, 0, getWidth(), getHeight());
    } else if (attr == Image.ATTR_HEIGHT) {
      final var val = (Integer) value;
      if (val <= 0 && cachedImage != null) {
        width = cachedImage.getWidth();
        height = cachedImage.getHeight();
      } else {
        height = Math.max(1, val);
      }
      userSetDimensions = true;
      offsetBounds = Bounds.create(0, 0, getWidth(), getHeight());
    } else if (attr == Image.ATTR_SCALE) {
      scale = (AttributeOption) value;
    } else if (attr == Image.ATTR_LICENSE) {
      license = (AttributeOption) value;
    } else if (attr == Image.ATTR_ATTRIBUTION) {
      attribution = value == null ? "" : (String) value;
    } else {
      return;
    }
    fireAttributeValueChanged(attr, value, null);
    if (attr == Image.ATTR_IMAGE) {
      fireAttributeValueChanged(Image.ATTR_DATA_SIZE, getDataSizeFormatted(), null);
    }
  }
}
