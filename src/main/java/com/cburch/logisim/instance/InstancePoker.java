/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.data.Bounds;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public abstract class InstancePoker {
  public Bounds getBounds(InstancePainter painter) {
    return painter.getInstance().getBounds();
  }

  public boolean init(InstanceState state, MouseEvent e) {
    return true;
  }

  public void keyPressed(InstanceState state, KeyEvent e) {}

  public void keyReleased(InstanceState state, KeyEvent e) {}

  public void keyTyped(InstanceState state, KeyEvent e) {}

  public void mouseDragged(InstanceState state, MouseEvent e) {}

  public void mousePressed(InstanceState state, MouseEvent e) {}

  public void mouseReleased(InstanceState state, MouseEvent e) {}

  public void paint(InstancePainter painter) {}

  public void stopEditing(InstanceState state) {}
}
