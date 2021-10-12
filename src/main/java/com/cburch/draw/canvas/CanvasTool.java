/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.canvas;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public abstract class CanvasTool {
  /** This is because a popup menu may result from the subsequent mouse release. */
  public void cancelMousePress(Canvas canvas) {
    // no-op implementation
  }

  public void draw(Canvas canvas, Graphics gfx) {
    // no-op implementation
  }

  public abstract Cursor getCursor(Canvas canvas);

  public void keyPressed(Canvas canvas, KeyEvent e) {
    // no-op implementation
  }

  public void keyReleased(Canvas canvas, KeyEvent e) {
    // no-op implementation
  }

  public void keyTyped(Canvas canvas, KeyEvent e) {
    // no-op implementation
  }

  public void mouseDragged(Canvas canvas, MouseEvent e) {
    // no-op implementation
  }

  public void mouseEntered(Canvas canvas, MouseEvent e) {
    // no-op implementation
  }

  public void mouseExited(Canvas canvas, MouseEvent e) {
    // no-op implementation
  }

  public void mouseMoved(Canvas canvas, MouseEvent e) {
    // no-op implementation
  }

  public void mousePressed(Canvas canvas, MouseEvent e) {
    // no-op implementation
  }

  public void mouseReleased(Canvas canvas, MouseEvent e) {
    // no-op implementation
  }

  public void toolDeselected(Canvas canvas) {
    // no-op implementation
  }

  public void toolSelected(Canvas canvas) {
    // no-op implementation
  }

  public void zoomFactorChanged(Canvas canvas) {
    // no-op implementation
  }
}
