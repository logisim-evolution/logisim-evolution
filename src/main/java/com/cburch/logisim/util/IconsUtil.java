/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public final class IconsUtil {

  private static final String PATH = "resources/logisim/icons";

  private IconsUtil() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static ImageIcon getIcon(String name) {
    final var url = IconsUtil.class.getClassLoader().getResource(PATH + "/" + name);
    if (url == null) return null;
    final var icon = new ImageIcon(url);
    icon.setImage(
        icon.getImage()
            .getScaledInstance(
                AppPreferences.getScaled(AppPreferences.IconSize),
                AppPreferences.getScaled(AppPreferences.IconSize),
                Image.SCALE_SMOOTH));
    return icon;
  }

  public static void paintRotated(Graphics g, int x, int y, Direction dir, Icon icon, Component dest) {
    if (!(g instanceof Graphics2D) || dir == Direction.EAST) {
      icon.paintIcon(dest, g, x, y);
      return;
    }

    final var g2 = (Graphics2D) g.create();
    final var cx = x + icon.getIconWidth() / 2.0;
    final var cy = y + icon.getIconHeight() / 2.0;
    if (dir == Direction.WEST) {
      g2.rotate(Math.PI, cx, cy);
    } else if (dir == Direction.NORTH) {
      g2.rotate(-Math.PI / 2.0, cx, cy);
    } else if (dir == Direction.SOUTH) {
      g2.rotate(Math.PI / 2.0, cx, cy);
    } else {
      g2.translate(-x, -y);
    }
    icon.paintIcon(dest, g2, x, y);
    g2.dispose();
  }
}
