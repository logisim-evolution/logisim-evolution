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
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

public class SimpleRectangle {

  private int x, y, width, height, offsetX, offsetY;
  private final FpgaIoInformationContainer toBeModified;
  private final boolean movemode;
  private final boolean fill;

  public SimpleRectangle(MouseEvent e) {
    toBeModified = null;
    x = e.getX();
    y = e.getY();
    width = 1;
    height = 1;
    offsetX = 0;
    offsetY = 0;
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
    offsetX = x - e.getX();
    offsetY = y - e.getY();
    fill = true;
    movemode = SwingUtilities.isLeftMouseButton(e);
  }

  public void resizeAndGetUpdate(MouseEvent e, float scale) {
    int xmax = AppPreferences.getScaled(BoardManipulator.IMAGE_WIDTH, scale);
    int ymax = AppPreferences.getScaled(BoardManipulator.IMAGE_HEIGHT, scale);
    if (movemode) {
      x = Math.max(Math.min(e.getX() + offsetX, xmax - width), 0);
      y = Math.max(Math.min(e.getY() + offsetY, ymax - height), 0);
    } else {
      width = Math.max(0, Math.min(e.getX(), xmax)) - x;
      height = Math.max(0, Math.min(e.getY(), ymax)) - y;
    }
  }
 
  public FpgaIoInformationContainer getIoInfo() {
    return toBeModified;
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
