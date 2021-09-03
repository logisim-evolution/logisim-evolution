/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.AppearancePort;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.io.Led;
import com.cburch.logisim.std.io.RgbLed;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Collection;
import java.util.Collections;
import javax.swing.JComponent;

public class LayoutThumbnail extends JComponent {
  private static final long serialVersionUID = 1L;

  private static final int BORDER = 10;

  private CircuitState circuitState;
  private Collection<Instance> ports;
  private Collection<Instance> elts;

  public LayoutThumbnail(Dimension size) {
    circuitState = null;
    ports = null;
    elts = null;
    setBackground(Color.LIGHT_GRAY);
    setPreferredSize(size);
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (AppPreferences.AntiAliassing.getBoolean()) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    if (circuitState != null) {
      var circuit = circuitState.getCircuit();
      var bounds = circuit.getBounds(g);
      var size = getSize();
      double scaleX = (double) (size.width - 2 * BORDER) / bounds.getWidth();
      double scaleY = (double) (size.height - 2 * BORDER) / bounds.getHeight();
      double scale = Math.min(1.0, Math.min(scaleX, scaleY));

      var gfxCopy = g.create();
      int borderX = (int) ((size.width - bounds.getWidth() * scale) / 2);
      int borderY = (int) ((size.height - bounds.getHeight() * scale) / 2);
      gfxCopy.translate(borderX, borderY);
      if (scale != 1.0 && g instanceof Graphics2D) {
        ((Graphics2D) gfxCopy).scale(scale, scale);
      }
      gfxCopy.translate(-bounds.getX(), -bounds.getY());

      var context = new ComponentDrawContext(this, circuit, circuitState, g, gfxCopy);
      context.setShowState(false);
      context.setShowColor(false);
      circuit.draw(context, Collections.emptySet());
      if (ports != null && ports.size() > 0) {
        gfxCopy.setColor(AppearancePort.COLOR);
        int width = Math.max(4, (int) ((2 / scale) + 0.5));
        GraphicsUtil.switchToWidth(gfxCopy, width);
        for (Instance port : ports) {
          Bounds b = port.getBounds();
          int x = b.getX();
          int y = b.getY();
          int w = b.getWidth();
          int h = b.getHeight();
          if (Pin.FACTORY.isInputPin(port)) {
            gfxCopy.drawRect(x, y, w, h);
          } else {
            if (b.getWidth() > 25) {
              gfxCopy.drawRoundRect(x, y, w, h, 4, 4);
            } else {
              gfxCopy.drawOval(x, y, w, h);
            }
          }
        }
      }
      if (elts != null && elts.size() > 0) {
        gfxCopy.setColor(DynamicElement.COLOR);
        int width = Math.max(4, (int) ((2 / scale) + 0.5));
        GraphicsUtil.switchToWidth(gfxCopy, width);
        for (Instance elt : elts) {
          Bounds b = elt.getBounds();
          int x = b.getX();
          int y = b.getY();
          int w = b.getWidth();
          int h = b.getHeight();
          if (elt.getFactory() instanceof Led || elt.getFactory() instanceof RgbLed) {
            gfxCopy.drawOval(x, y, w, h);
          } else {
            gfxCopy.drawRect(x, y, w, h);
          }
        }
      }
      gfxCopy.dispose();

      g.setColor(Color.BLACK);
      GraphicsUtil.switchToWidth(g, 2);
      g.drawRect(0, 0, size.width - 2, size.height - 2);
    }
  }

  public void setCircuit(
      CircuitState circuitState, Collection<Instance> ports, Collection<Instance> elts) {
    this.circuitState = circuitState;
    this.ports = ports;
    this.elts = elts;
    repaint();
  }
}
