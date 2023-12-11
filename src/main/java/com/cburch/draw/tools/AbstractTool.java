/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.tools;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.CanvasTool;
import com.cburch.logisim.data.Attribute;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Icon;

public abstract class AbstractTool extends CanvasTool {
  public static AbstractTool[] getTools(DrawingAttributeSet attrs) {
    return new AbstractTool[] {
      new SelectTool(),
      new LineTool(attrs),
      new CurveTool(attrs),
      new PolyTool(false, attrs),
      new RectangleTool(attrs),
      new RoundRectangleTool(attrs),
      new OvalTool(attrs),
      new PolyTool(true, attrs),
      new ImageTool(attrs),
    };
  }

  /** This is because a popup menu may result from the subsequent mouse release. */
  @Override
  public void cancelMousePress(Canvas canvas) {
    // dummy
  }

  @Override
  public void draw(Canvas canvas, Graphics gfx) {
    // dummy
  }

  public abstract List<Attribute<?>> getAttributes();

  //
  // CanvasTool methods
  //
  @Override
  public abstract Cursor getCursor(Canvas canvas);

  public String getDescription() {
    return null;
  }

  public abstract Icon getIcon();

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    // dummy
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent e) {
    // dummy
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent e) {
    // dummy
  }

  @Override
  public void mouseDragged(Canvas canvas, MouseEvent e) {
    // dummy
  }

  @Override
  public void mouseEntered(Canvas canvas, MouseEvent e) {
    // dummy
  }

  @Override
  public void mouseExited(Canvas canvas, MouseEvent e) {
    // dummy
  }

  @Override
  public void mouseMoved(Canvas canvas, MouseEvent e) {
    // dummy
  }

  @Override
  public void mousePressed(Canvas canvas, MouseEvent e) {
    // dummy
  }

  @Override
  public void mouseReleased(Canvas canvas, MouseEvent e) {
    // dummy
  }

  @Override
  public void toolDeselected(Canvas canvas) {
    // dummy
  }

  @Override
  public void toolSelected(Canvas canvas) {
    // dummy
  }
}
