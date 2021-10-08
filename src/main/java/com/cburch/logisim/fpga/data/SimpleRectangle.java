/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import com.cburch.logisim.fpga.gui.BoardManipulator;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public class SimpleRectangle {

  private int x, y, width, height;
  private final FpgaIoInformationContainer toBeModified;
  private final boolean movemode;
  private boolean show;
  private final boolean fill;

  public SimpleRectangle(MouseEvent e) {
    toBeModified = null;
    x = e.getX();
    y = e.getY();
    width = 1;
    height = 1;
    show = true;
    fill = false;
    movemode = false;
  }

  public SimpleRectangle(MouseEvent e, FpgaIoInformationContainer IOinfo, float scale) {
    toBeModified = IOinfo;
    BoardRectangle rect = IOinfo.getRectangle();
    x = AppPreferences.getScaled(rect.getXpos(), scale);
    y = AppPreferences.getScaled(rect.getYpos(), scale);
    width = AppPreferences.getScaled(rect.getWidth(), scale);
    height = AppPreferences.getScaled(rect.getHeight(), scale);
    int offset = AppPreferences.getScaled(5, scale);
    BoardRectangle test =
        new BoardRectangle(x + width - offset, y + height - offset, offset, offset);
    show = true;
    fill = true;
    movemode = !test.isPointInside(e.getX(), e.getY());
  }

  public Rectangle resizeAndGetUpdate(MouseEvent e) {
    int xmin = 0, xmax = 0, ymin = 0, ymax = 0;
    if (movemode) {
      xmin = Math.min(Math.min(x, e.getX()), e.getX() + width);
      xmax = Math.max(Math.max(x, x + width), e.getX() + width);
      ymin = Math.min(Math.min(y, e.getY()), e.getY() + height);
      ymax = Math.max(Math.max(y, y + height), e.getY() + height);
      x = e.getX();
      y = e.getY();
    } else {
      xmin = Math.min(Math.min(x, e.getX()), x + width);
      xmax = Math.max(Math.max(x, e.getX()), x + width);
      ymin = Math.min(Math.min(y, e.getY()), y + height);
      ymax = Math.max(Math.max(y, e.getY()), y + height);
      width = e.getX() - x;
      height = e.getY() - y;
    }
    int off = AppPreferences.getScaled(2);
    int off2 = off << 1;
    return new Rectangle(xmin - off, ymin - off, off2 + xmax - xmin, off2 + ymax - ymin);
  }

  public FpgaIoInformationContainer getIoInfo() {
    return toBeModified;
  }

  public Rectangle resizeRemoveAndgetUpdate(MouseEvent e) {
    show = false;
    return resizeAndGetUpdate(e);
  }

  public BoardRectangle getBoardRectangle(float scale) {
    int xmin = Math.min(x, x + width);
    int xmax = Math.max(x, x + width);
    int ymin = Math.min(y, y + height);
    int ymax = Math.max(y, y + height);
    int width = Math.max(AppPreferences.getScaled(5, scale), xmax - xmin);
    int height = Math.max(AppPreferences.getScaled(5, scale), ymax - ymin);
    return new BoardRectangle(
        AppPreferences.getDownScaled(xmin, scale),
        AppPreferences.getDownScaled(ymin, scale),
        AppPreferences.getDownScaled(width, scale),
        AppPreferences.getDownScaled(height, scale));
  }

  public void paint(Graphics2D g) {
    if (!show) return;
    int xmin = Math.min(x, x + width);
    int xmax = Math.max(x, x + width);
    int ymin = Math.min(y, y + height);
    int ymax = Math.max(y, y + height);
    Graphics2D g1 = (Graphics2D) g.create();
    g1.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    if (fill) {
      g1.setColor(movemode ? BoardManipulator.moveColor : BoardManipulator.resizeColor);
      g1.fillRect(xmin, ymin, xmax - xmin, ymax - ymin);
    } else {
      g1.setColor(BoardManipulator.defineColor);
      g1.drawRect(xmin, ymin, xmax - xmin, ymax - ymin);
    }
    g1.dispose();
  }

}
