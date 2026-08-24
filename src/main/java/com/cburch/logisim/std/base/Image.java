/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.base;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.icons.ImageIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Color;
import java.awt.Graphics2D;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.ImageUtil;
import com.cburch.logisim.util.StringUtil;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class Image extends InstanceFactory {
  public static final String _ID = "Image";

  public static final Attribute<String> ATTR_IMAGE = new ImageSourceAttribute("image");
  public static final Attribute<Integer> ATTR_WIDTH = Attributes.forInteger("width", S.getter("imageWidthAttr"));
  public static final Attribute<Integer> ATTR_HEIGHT = Attributes.forInteger("height", S.getter("imageHeightAttr"));
  public static final Attribute<AttributeOption> ATTR_SCALE = Attributes.forOption(
      "scale",
      S.getter("imageScaleAttr"),
      new AttributeOption[] {
          new AttributeOption("fit", "fit", S.getter("imageScaleFitOpt")),
          new AttributeOption("stretch", "stretch", S.getter("imageScaleStretchOpt")),
          new AttributeOption("cover", "cover", S.getter("imageScaleCoverOpt")),
      });

  public static final AttributeOption LICENSE_UNSPECIFIED = new AttributeOption("Unspecified", S.getter("imageLicenseUnspecifiedOpt"));
  public static final AttributeOption LICENSE_PUBLIC_DOMAIN = new AttributeOption("Public Domain / CC0", StringUtil.constantGetter("Public Domain / CC0"));
  public static final AttributeOption LICENSE_CC_BY = new AttributeOption("CC BY (Attribution)", StringUtil.constantGetter("CC BY (Attribution)"));
  public static final AttributeOption LICENSE_CC_BY_SA = new AttributeOption("CC BY-SA (Attribution-ShareAlike)", StringUtil.constantGetter("CC BY-SA (Attribution-ShareAlike)"));
  public static final AttributeOption LICENSE_CC_BY_NC = new AttributeOption("CC BY-NC (NonCommercial)", StringUtil.constantGetter("CC BY-NC (NonCommercial)"));
  public static final AttributeOption LICENSE_CC_BY_ND = new AttributeOption("CC BY-ND (NoDerivatives)", StringUtil.constantGetter("CC BY-ND (NoDerivatives)"));
  public static final AttributeOption LICENSE_FAIR_USE = new AttributeOption("Fair Use / Educational", StringUtil.constantGetter("Fair Use / Educational"));
  public static final AttributeOption LICENSE_PROPRIETARY = new AttributeOption("Proprietary / Copyrighted", StringUtil.constantGetter("Proprietary / Copyrighted"));
  public static final AttributeOption LICENSE_OTHER = new AttributeOption("Other", S.getter("imageLicenseOtherOpt"));

  public static final Attribute<AttributeOption> ATTR_LICENSE = Attributes.forOption(
      "license",
      S.getter("imageLicenseAttr"),
      new AttributeOption[] {
          LICENSE_UNSPECIFIED,
          LICENSE_PUBLIC_DOMAIN,
          LICENSE_CC_BY,
          LICENSE_CC_BY_SA,
          LICENSE_CC_BY_NC,
          LICENSE_CC_BY_ND,
          LICENSE_FAIR_USE,
          LICENSE_PROPRIETARY,
          LICENSE_OTHER
      });

  public static final Attribute<String> ATTR_ATTRIBUTION = Attributes.forString(
      "attribution",
      S.getter("imageAttributionAttr"));

  public static final Attribute<String> ATTR_DATA_SIZE = Attributes.forString(
      "dataSize",
      S.getter("imageDataSizeAttr"));

  public static final Image FACTORY = new Image();

  private Image() {
    super(_ID, S.getter("imageComponent"));
    setShouldSnap(false);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == MenuExtender.class) {
      return (MenuExtender) (menu, proj) -> {
        final var resetItem = new JMenuItem(S.get("imageResetSizeItem"));
        resetItem.addActionListener(e -> {
          final var attrs = (ImageAttributes) instance.getAttributeSet();
          final var img = attrs.getCachedImage();
          if (img != null) {
            final var circuit = proj.getCurrentCircuit();
            if (circuit != null) {
              final var mutation = new CircuitMutation(circuit);
              mutation.set(instance.getComponent(), ATTR_WIDTH, img.getWidth());
              mutation.set(instance.getComponent(), ATTR_HEIGHT, img.getHeight());
              proj.doAction(mutation.toAction(S.getter("imageResetSizeItem")));
            }
          }
        });
        menu.add(resetItem);

        final var optimizeItem = new JMenuItem(S.get("imageOptimizeItem"));
        optimizeItem.addActionListener(e -> {
          final var attrs = (ImageAttributes) instance.getAttributeSet();
          final var img = attrs.getCachedImage();
          if (img != null) {
            final var frameW = attrs.getWidth();
            final var frameH = attrs.getHeight();
            final var scaleOpt = attrs.getScale() == null ? "fit" : (String) attrs.getScale().getValue();
            final var targetDims = ImageUtil.getOptimizedTargetDimensions(img, frameW, frameH, scaleOpt);
            final var targetW = targetDims[0];
            final var targetH = targetDims[1];
            final var origW = img.getWidth();
            final var origH = img.getHeight();

            if (origW == targetW && origH == targetH) {
              JOptionPane.showMessageDialog(
                  null,
                  String.format(S.get("imageAlreadyOptimizedMessage"), origW, origH),
                  S.get("imageOptimizeConfirmTitle"),
                  JOptionPane.INFORMATION_MESSAGE);
              return;
            }
            final var choice = JOptionPane.showConfirmDialog(
                null,
                String.format(S.get("imageOptimizeConfirmMessage"), origW, origH, targetW, targetH),
                S.get("imageOptimizeConfirmTitle"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
              final var scaled = ImageUtil.optimizeImage(img, frameW, frameH, scaleOpt);
              final var base64 = ImageUtil.bufferedImageToBase64(scaled);
              final var circuit = proj.getCurrentCircuit();
              if (circuit != null && !base64.isBlank()) {
                final var mutation = new CircuitMutation(circuit);
                mutation.set(instance.getComponent(), ATTR_IMAGE, base64);
                if ("fit".equalsIgnoreCase(scaleOpt) && (origW != targetW || origH != targetH)) {
                  mutation.set(instance.getComponent(), ATTR_WIDTH, targetW);
                  mutation.set(instance.getComponent(), ATTR_HEIGHT, targetH);
                }
                proj.doAction(mutation.toAction(S.getter("imageOptimizeItem")));
              }
            }
          }
        });
        menu.add(optimizeItem);

        final var transparentItem = new JMenuItem(S.get("imageMakeWhiteTransparentItem"));
        transparentItem.addActionListener(e -> {
          final var attrs = (ImageAttributes) instance.getAttributeSet();
          final var img = attrs.getCachedImage();
          if (img != null) {
            final var transImg = ImageUtil.makeWhiteTransparent(img);
            final var base64 = ImageUtil.bufferedImageToBase64(transImg);
            final var circuit = proj.getCurrentCircuit();
            if (circuit != null && !base64.isBlank()) {
              final var mutation = new CircuitMutation(circuit);
              mutation.set(instance.getComponent(), ATTR_IMAGE, base64);
              proj.doAction(mutation.toAction(S.getter("imageMakeWhiteTransparentItem")));
            }
          }
        });
        menu.add(transparentItem);
      };
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_WIDTH || attr == ATTR_HEIGHT || attr == ATTR_IMAGE || attr == ATTR_SCALE) {
      instance.recomputeBounds();
      instance.fireInvalidated();
    }
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new ImageAttributes();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrsBase) {
    ImageAttributes attrs = (ImageAttributes) attrsBase;
    return attrs.getOffsetBounds();
  }

  @Override
  public boolean isHDLSupportedComponent(AttributeSet attrs) {
    return false;
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    final var attrs = (ImageAttributes) painter.getAttributeSet();
    final var g = painter.getGraphics();
    final var w = attrs.getWidth();
    final var h = attrs.getHeight();
    final var img = attrs.getCachedImage();

    if (img != null) {
      final var drawImg = painter.isPrintView() ? ImageUtil.toGrayscale(img) : img;
      final var scaleOpt = attrs.getScale() == null ? "fit" : (String) attrs.getScale().getValue();
      if ("stretch".equals(scaleOpt)) {
        g.drawImage(drawImg, 0, 0, w, h, null);
      } else if ("cover".equals(scaleOpt)) {
        final var imgW = drawImg.getWidth();
        final var imgH = drawImg.getHeight();
        final var scale = Math.max((double) w / imgW, (double) h / imgH);
        final var targetW = Math.max(1, (int) (imgW * scale));
        final var targetH = Math.max(1, (int) (imgH * scale));
        final var targetX = (w - targetW) / 2;
        final var targetY = (h - targetH) / 2;
        final var oldClip = g.getClip();
        g.clipRect(0, 0, w, h);
        g.drawImage(drawImg, targetX, targetY, targetW, targetH, null);
        g.setClip(oldClip);
      } else { // fit — preserve aspect ratio
        final var imgW = drawImg.getWidth();
        final var imgH = drawImg.getHeight();
        final var scale = Math.min((double) w / imgW, (double) h / imgH);
        final var targetW = Math.max(1, (int) (imgW * scale));
        final var targetH = Math.max(1, (int) (imgH * scale));
        final var targetX = (w - targetW) / 2;
        final var targetY = (h - targetH) / 2;
        g.drawImage(drawImg, targetX, targetY, targetW, targetH, null);
      }
    } else {
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(0, 0, w, h);
      g.setColor(Color.GRAY);
      g.drawRect(0, 0, w - 1, h - 1);
      g.drawLine(0, 0, w - 1, h - 1);
      g.drawLine(0, h - 1, w - 1, 0);
    }
  }

  @Override
  public void paintIcon(InstancePainter painter) {
    Graphics2D g2 = (Graphics2D) painter.getGraphics().create();
    ImageIcon icon = new ImageIcon();
    icon.paintIcon(null, g2, 0, 0);
    g2.dispose();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    final var g = painter.getGraphics();
    g.translate(x, y);
    paintGhost(painter);
    g.translate(-x, -y);
  }

  @Override
  public void propagate(InstanceState state) {
  }
}
