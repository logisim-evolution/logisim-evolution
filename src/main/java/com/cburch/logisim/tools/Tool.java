/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.util.LibraryUtil;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Set;

//
// DRAWING TOOLS
//
public abstract class Tool implements AttributeDefaultProvider {
  private static final Cursor dflt_cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

  public Tool cloneTool() {
    return this;
  }

  public void deselect(Canvas canvas) {
    // no-op implementation
  }

  public void draw(Canvas canvas, ComponentDrawContext context) {
    draw(context);
  }

  // This was the draw method until 2.0.4 - As of 2.0.5, you should
  // use the other draw method.
  public void draw(ComponentDrawContext context) {
    // no-op implementation
  }

  public AttributeSet getAttributeSet() {
    return null;
  }

  public AttributeSet getAttributeSet(Canvas canvas) {
    return getAttributeSet();
  }

  public Cursor getCursor() {
    return dflt_cursor;
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    return null;
  }

  public abstract String getDescription();

  public abstract String getDisplayName();

  /**
   * Returns Tool unique ID as defined in its _ID constant.
   *
   * <p>For each tool extending this class, define public static final String _ID = "UNIQUE ID"
   *
   * @return unique ID of the tool
   */
  public String getName() {
    return LibraryUtil.getName(getClass());
  }

  public Set<Component> getHiddenComponents(Canvas canvas) {
    return null;
  }

  @Override
  public boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver) {
    return false;
  }

  public void keyPressed(Canvas canvas, KeyEvent e) {
    // no-op implementation
  }

  public void keyReleased(Canvas canvas, KeyEvent e) {
    // no-op implementation
  }

  public void keyTyped(Canvas canvas, KeyEvent e) {
    // no-op implementation
  }

  public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
    // no-op implementation
  }

  public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) {
    // no-op implementation
  }

  public void mouseExited(Canvas canvas, Graphics g, MouseEvent e) {
    // no-op implementation
  }

  public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) {
    // no-op implementation
  }

  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    // no-op implementation
  }

  public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
    // no-op implementation
  }

  public void paintIcon(ComponentDrawContext c, int x, int y) {
    // no-op implementation
  }

  public void select(Canvas canvas) {
    // no-op implementation
  }

  public void setAttributeSet(AttributeSet attrs) {
    // no-op implementation
  }

  public boolean sharesSource(Tool other) {
    return this == other;
  }

  @Override
  public String toString() {
    return getName();
  }
}
