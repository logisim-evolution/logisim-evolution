/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import javax.swing.JComponent;

public class CanvasPanZoomHelper {

  public static final byte ZOOM_BUTTON_SIZE = 52;
  public static final byte ZOOM_BUTTON_MARGIN = 30;
  public static final Color DEFAULT_ZOOM_BUTTON_COLOR = Color.WHITE;
  private static final Color TICK_RATE_COLOR = new Color(0, 0, 92, 92);

  private int panStartX;
  private int panStartY;
  private int panStartScrollX;
  private int panStartScrollY;
  private boolean isPanning = false;
  private Color zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR;
  private boolean zoomButtonVisible = false;

  private final JComponent canvasTarget;
  private final CanvasPane canvasPane;
  private final Runnable centerAction;

  public CanvasPanZoomHelper(JComponent canvasTarget, CanvasPane canvasPane, Runnable centerAction) {
    this.canvasTarget = canvasTarget;
    this.canvasPane = canvasPane;
    this.centerAction = centerAction;
  }

  public static boolean autoZoomButtonClicked(Dimension sz, double x, double y) {
    return Point2D.distance(
            x,
            y,
            sz.width - ZOOM_BUTTON_SIZE / 2 - ZOOM_BUTTON_MARGIN,
            sz.height - ZOOM_BUTTON_MARGIN - ZOOM_BUTTON_SIZE / 2)
        <= ZOOM_BUTTON_SIZE / 2;
  }

  public static void paintAutoZoomButton(Graphics g, Dimension sz, Color zoomButtonColor) {
    final var oldColor = g.getColor();
    g.setColor(TICK_RATE_COLOR);
    g.fillOval(
        sz.width - ZOOM_BUTTON_SIZE - 33,
        sz.height - ZOOM_BUTTON_SIZE - 33,
        ZOOM_BUTTON_SIZE + 6,
        ZOOM_BUTTON_SIZE + 6);
    g.setColor(zoomButtonColor);
    g.fillOval(
        sz.width - ZOOM_BUTTON_SIZE - 30,
        sz.height - ZOOM_BUTTON_SIZE - 30,
        ZOOM_BUTTON_SIZE,
        ZOOM_BUTTON_SIZE);
    g.setColor(Value.unknownColor);
    GraphicsUtil.switchToWidth(g, 3);
    int width = sz.width - ZOOM_BUTTON_MARGIN;
    int height = sz.height - ZOOM_BUTTON_MARGIN;
    g.drawOval(
        width - ZOOM_BUTTON_SIZE * 3 / 4,
        height - ZOOM_BUTTON_SIZE * 3 / 4,
        ZOOM_BUTTON_SIZE / 2,
        ZOOM_BUTTON_SIZE / 2);
    g.drawLine(
        width - ZOOM_BUTTON_SIZE / 4 + 4,
        height - ZOOM_BUTTON_SIZE / 2,
        width - ZOOM_BUTTON_SIZE * 3 / 4 - 4,
        height - ZOOM_BUTTON_SIZE / 2);
    g.drawLine(
        width - ZOOM_BUTTON_SIZE / 2,
        height - ZOOM_BUTTON_SIZE / 4 + 4,
        width - ZOOM_BUTTON_SIZE / 2,
        height - ZOOM_BUTTON_SIZE * 3 / 4 - 4);
    g.setColor(oldColor);
  }

  public void drawZoomAndArrows(Graphics g, Dimension sz, Bounds bds, Rectangle viewableBase, double zoom) {
    Rectangle viewable = new Rectangle(
        (int) (viewableBase.x / zoom), (int) (viewableBase.y / zoom),
        (int) (viewableBase.width / zoom), (int) (viewableBase.height / zoom));

    int x0 = bds.getX();
    int y0 = bds.getY();
    if (x0 < 0) x0 = 0;
    if (y0 < 0) y0 = 0;

    int x1 = x0 + bds.getWidth();
    int y1 = y0 + bds.getHeight();

    boolean isWest = x0 < viewable.x;
    boolean isEast = x1 > viewable.x + viewable.width;
    boolean isNorth = y0 < viewable.y;
    boolean isSouth = y1 > viewable.y + viewable.height;

    boolean isNortheast = false, isNorthwest = false, isSoutheast = false, isSouthwest = false;
    boolean pureNorth = false, pureSouth = false, pureEast = false, pureWest = false;

    if (isNorth) {
      if (isEast) isNortheast = true;
      if (isWest) isNorthwest = true;
      if (!isWest && !isEast) pureNorth = true;
    }
    if (isSouth) {
      if (isEast) isSoutheast = true;
      if (isWest) isSouthwest = true;
      if (!isWest && !isEast) pureSouth = true;
    }
    if (isEast && !isSoutheast && !isNortheast) pureEast = true;
    if (isWest && !isSouthwest && !isNorthwest) pureWest = true;

    zoomButtonVisible = isNorth || isSouth || isEast || isWest || isNortheast || isNorthwest || isSoutheast || isSouthwest;

    if (zoomButtonVisible) {
      paintAutoZoomButton(g, sz, zoomButtonColor);
      
      final var oldColor = g.getColor();
      g.setColor(TICK_RATE_COLOR);

      if (pureNorth)   GraphicsUtil.drawArrow2(g, sz.width / 2 - 20, 15, sz.width / 2, 5, sz.width / 2 + 20, 15);
      if (pureSouth)   GraphicsUtil.drawArrow2(g, sz.width / 2 - 20, sz.height - 15, sz.width / 2, sz.height - 5, sz.width / 2 + 20, sz.height - 15);
      if (pureEast)    GraphicsUtil.drawArrow2(g, sz.width - 15, sz.height / 2 + 20, sz.width - 5, sz.height / 2, sz.width - 15, sz.height / 2 - 20);
      if (pureWest)    GraphicsUtil.drawArrow2(g, 15, sz.height / 2 + 20, 5, sz.height / 2, 15, sz.height / 2 + (-20));
      if (isNortheast) GraphicsUtil.drawArrow2(g, sz.width - 30, 5, sz.width - 5, 5, sz.width - 5, 30);
      if (isNorthwest) GraphicsUtil.drawArrow2(g, 30, 5, 5, 5, 5, 30);
      if (isSoutheast) GraphicsUtil.drawArrow2(g, sz.width - 30, sz.height - 5, sz.width - 5, sz.height - 5, sz.width - 5, sz.height - 30);
      if (isSouthwest) GraphicsUtil.drawArrow2(g, 30, sz.height - 5, 5, sz.height - 5, 5, sz.height - 30);
      
      g.setColor(oldColor);
    }
  }

  public boolean processMouseEvent(MouseEvent e, double unscaledX, double unscaledY) {
    // 1. Handle Auto-Center Button Clicks
    if (e.getButton() == MouseEvent.BUTTON1 && zoomButtonVisible) {
      if (autoZoomButtonClicked(canvasPane.getViewport().getSize(), unscaledX, unscaledY)) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
          zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR.darker();
          canvasTarget.repaint();
          e.consume();
          return true;
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
          zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR;
          centerAction.run();
          canvasTarget.repaint();
          e.consume();
          return true;
        }
      }
    }

    if (e.getID() == MouseEvent.MOUSE_RELEASED && zoomButtonColor != DEFAULT_ZOOM_BUTTON_COLOR) {
      zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR;
      canvasTarget.repaint();
    }

    // Panning Anchors
    if (e.getButton() == MouseEvent.BUTTON2 || javax.swing.SwingUtilities.isMiddleMouseButton(e)) {
      if (e.getID() == MouseEvent.MOUSE_PRESSED) {
        isPanning = true;
        panStartX = e.getXOnScreen();
        panStartY = e.getYOnScreen();
        if (canvasPane != null) {
          panStartScrollX = canvasPane.getHorizontalScrollBar().getValue();
          panStartScrollY = canvasPane.getVerticalScrollBar().getValue();
        }
        e.consume();
        return true;
      } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
        isPanning = false;
        e.consume();
        return true;
      }
    }
    return false;
  }

  public boolean processMouseMotionEvent(MouseEvent e) {
    if (isPanning && e.getID() == MouseEvent.MOUSE_DRAGGED) {
      int dx = e.getXOnScreen() - panStartX;
      int dy = e.getYOnScreen() - panStartY;

      if (canvasPane != null) {
        canvasPane.getHorizontalScrollBar().setValue(panStartScrollX - dx);
        canvasPane.getVerticalScrollBar().setValue(panStartScrollY - dy);
      }
      e.consume();
      return true; 
    }
    return false;
  }
}
