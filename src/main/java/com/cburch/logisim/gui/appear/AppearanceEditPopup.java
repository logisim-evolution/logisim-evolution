/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.actions.ModelChangeAttributeAction;
import com.cburch.draw.model.AttributeMapKey;
import com.cburch.draw.shapes.ImageShape;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.EditPopup;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
import com.cburch.logisim.std.Strings;
import com.cburch.logisim.util.ImageUtil;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class AppearanceEditPopup extends EditPopup implements EditHandler.Listener {
  private static final long serialVersionUID = 1L;
  private final AppearanceCanvas canvas;
  private final EditHandler handler;
  private final Map<LogisimMenuItem, Boolean> enabled;

  public AppearanceEditPopup(AppearanceCanvas canvas) {
    super(true);
    this.canvas = canvas;
    handler = new AppearanceEditHandler(canvas);
    handler.setListener(this);
    enabled = new HashMap<>();
    handler.computeEnabled();
    initialize();
    for (final var obj : canvas.getSelection().getSelected()) {
      if (obj instanceof ImageShape) {
        addSeparator();

        // ── Reset to Original Size ──────────────────────────────────────────
        final var resetItem = new JMenuItem(Strings.S.get("imageResetSizeItem"));
        resetItem.addActionListener(e -> {
          for (final var selObj : canvas.getSelection().getSelected()) {
            if (selObj instanceof ImageShape shape) {
              final var img = ImageUtil.loadBufferedImage(shape.getImageSource());
              if (img == null) continue;
              final var origW = img.getWidth();
              final var origH = img.getHeight();
              final var oldVals = Map.<AttributeMapKey, Object>of(
                  new AttributeMapKey(ImageShape.WIDTH_ATTR, shape), shape.getWidth(),
                  new AttributeMapKey(ImageShape.HEIGHT_ATTR, shape), shape.getHeight());
              final var newVals = Map.<AttributeMapKey, Object>of(
                  new AttributeMapKey(ImageShape.WIDTH_ATTR, shape), origW,
                  new AttributeMapKey(ImageShape.HEIGHT_ATTR, shape), origH);
              canvas.doAction(new ModelChangeAttributeAction(canvas.getModel(), oldVals, newVals));
            }
          }
        });
        add(resetItem);

        // ── Optimize Image Size ─────────────────────────────────────────────
        final var optimizeItem = new JMenuItem(Strings.S.get("imageOptimizeItem"));
        optimizeItem.addActionListener(e -> {
          for (final var selObj : canvas.getSelection().getSelected()) {
            if (selObj instanceof ImageShape shape) {
              final var frameW = shape.getWidth();
              final var frameH = shape.getHeight();
              final var scaleOptAttr = shape.getValue(ImageShape.SCALE_ATTR);
              final var scaleOpt = scaleOptAttr == null ? "fit" : (String) scaleOptAttr.getValue();
              final var cachedImg = ImageUtil.loadBufferedImage(shape.getImageSource());
              if (cachedImg == null) continue;
              final var targetDims = ImageUtil.getOptimizedTargetDimensions(cachedImg, frameW, frameH, scaleOpt);
              final var targetW = targetDims[0];
              final var targetH = targetDims[1];
              final var origW = cachedImg.getWidth();
              final var origH = cachedImg.getHeight();

              if (origW == targetW && origH == targetH) {
                JOptionPane.showMessageDialog(null,
                    String.format(Strings.S.get("imageAlreadyOptimizedMessage"), origW, origH),
                    Strings.S.get("imageOptimizeConfirmTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
                return;
              }
              final var choice = JOptionPane.showConfirmDialog(null,
                  String.format(Strings.S.get("imageOptimizeConfirmMessage"), origW, origH, targetW, targetH),
                  Strings.S.get("imageOptimizeConfirmTitle"),
                  JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
              if (choice == JOptionPane.YES_OPTION) {
                final var scaled = ImageUtil.optimizeImage(cachedImg, frameW, frameH, scaleOpt);
                final var base64 = ImageUtil.bufferedImageToBase64(scaled);
                if (!base64.isBlank()) {
                  final var oldValsBuilder = new HashMap<AttributeMapKey, Object>();
                  final var newValsBuilder = new HashMap<AttributeMapKey, Object>();
                  oldValsBuilder.put(new AttributeMapKey(ImageShape.IMAGE_ATTR, shape), shape.getImageSource());
                  newValsBuilder.put(new AttributeMapKey(ImageShape.IMAGE_ATTR, shape), base64);
                  if ("fit".equalsIgnoreCase(scaleOpt) && (origW != targetW || origH != targetH)) {
                    oldValsBuilder.put(new AttributeMapKey(ImageShape.WIDTH_ATTR, shape), shape.getWidth());
                    oldValsBuilder.put(new AttributeMapKey(ImageShape.HEIGHT_ATTR, shape), shape.getHeight());
                    newValsBuilder.put(new AttributeMapKey(ImageShape.WIDTH_ATTR, shape), targetW);
                    newValsBuilder.put(new AttributeMapKey(ImageShape.HEIGHT_ATTR, shape), targetH);
                  }
                  canvas.doAction(new ModelChangeAttributeAction(canvas.getModel(), oldValsBuilder, newValsBuilder));
                }
              }
            }
          }
        });
        add(optimizeItem);

        // ── Make White Background Transparent ──────────────────────────────
        final var transparentItem = new JMenuItem(Strings.S.get("imageMakeWhiteTransparentItem"));
        transparentItem.addActionListener(e -> {
          for (final var selObj : canvas.getSelection().getSelected()) {
            if (selObj instanceof ImageShape shape) {
              final var img = ImageUtil.loadBufferedImage(shape.getImageSource());
              if (img == null) continue;
              final var transImg = ImageUtil.makeWhiteTransparent(img);
              final var base64 = ImageUtil.bufferedImageToBase64(transImg);
              if (!base64.isBlank()) {
                final var oldVals = Map.<AttributeMapKey, Object>of(
                    new AttributeMapKey(ImageShape.IMAGE_ATTR, shape), shape.getImageSource());
                final var newVals = Map.<AttributeMapKey, Object>of(
                    new AttributeMapKey(ImageShape.IMAGE_ATTR, shape), base64);
                canvas.doAction(new ModelChangeAttributeAction(canvas.getModel(), oldVals, newVals));
              }
            }
          }
        });
        add(transparentItem);
        break;
      }
    }
  }

  @Override
  public void enableChanged(EditHandler handler, LogisimMenuItem action, boolean value) {
    enabled.put(action, value);
  }

  @Override
  protected void fire(LogisimMenuItem item) {
    if (item == LogisimMenuBar.CUT) {
      handler.cut();
    } else if (item == LogisimMenuBar.COPY) {
      handler.copy();
    } else if (item == LogisimMenuBar.DELETE) {
      handler.delete();
    } else if (item == LogisimMenuBar.DUPLICATE) {
      handler.duplicate();
    } else if (item == LogisimMenuBar.RAISE) {
      handler.raise();
    } else if (item == LogisimMenuBar.LOWER) {
      handler.lower();
    } else if (item == LogisimMenuBar.RAISE_TOP) {
      handler.raiseTop();
    } else if (item == LogisimMenuBar.LOWER_BOTTOM) {
      handler.lowerBottom();
    } else if (item == LogisimMenuBar.ADD_CONTROL) {
      handler.addControlPoint();
    } else if (item == LogisimMenuBar.REMOVE_CONTROL) {
      handler.removeControlPoint();
    }
  }

  @Override
  protected boolean isEnabled(LogisimMenuItem item) {
    final var value = enabled.get(item);
    return (value != null) && value;
  }

  @Override
  protected boolean shouldShow(LogisimMenuItem item) {
    return (item == LogisimMenuBar.ADD_CONTROL || item == LogisimMenuBar.REMOVE_CONTROL)
        ? canvas.getSelection().getSelectedHandle() != null
        : true;
  }
}
