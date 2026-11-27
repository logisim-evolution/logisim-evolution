/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import javax.swing.JTabbedPane;

final class FpgaWindowPlacer {
  private FpgaWindowPlacer() {}

  static void place(Window w, Component parent) {
    if (parent == null || !parent.isShowing()) {
      w.setLocationRelativeTo(parent);
      return;
    }
    final var gc = parent.getGraphicsConfiguration();
    final var scr = gc.getBounds();
    final var in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
    final var usable = new Rectangle(scr.x + in.left, scr.y + in.top,
        scr.width - in.left - in.right, scr.height - in.top - in.bottom);

    final var ins = w.getInsets();
    w.setSize(parent.getWidth() + ins.left + ins.right, w.getHeight());

    final var p = parent.getLocationOnScreen();
    final var x = Math.max(usable.x,
        Math.min(p.x - ins.left, usable.x + usable.width - w.getWidth()));
    final var bottom = usable.y + usable.height;

    var y = p.y + parent.getHeight();
    if (y + w.getHeight() <= bottom) {
      w.setLocation(x, y);
      return;
    }

    var strip = 30;
    if (parent instanceof JTabbedPane tp && tp.getTabCount() > 0) {
      final var r = tp.getBoundsAt(0);
      if (r != null)
        strip = r.y + r.height;
    }
    y = Math.min(p.y + strip, bottom - w.getHeight());
    w.setLocation(x, Math.max(usable.y, y));
  }
}