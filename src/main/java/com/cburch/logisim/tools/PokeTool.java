/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import static com.cburch.logisim.tools.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;

public class PokeTool extends Tool {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Poke Tool";

  private class Listener implements CircuitListener {
    @Override
    public void circuitChanged(CircuitEvent event) {
      final var circ = pokedCircuit;
      if (event.getCircuit() == circ
          && circ != null
          && (event.getAction() == CircuitEvent.ACTION_REMOVE
              || event.getAction() == CircuitEvent.ACTION_CLEAR)
          && !circ.contains(pokedComponent)) {
        removeCaret(false);
      }
    }
  }

  private static class WireCaret extends AbstractCaret {
    final Canvas canvas;
    final Wire wire;
    final int x;
    final int y;

    WireCaret(Canvas canvas, Wire wire, int x, int y, AttributeSet opts) {
      this.canvas = canvas;
      this.wire = wire;
      this.x = x;
      this.y = y;
    }

    @Override
    public void draw(Graphics g) {
      final var v = canvas.getCircuitState().getValue(wire.getEnd0());
      var radix1 = RadixOption.decode(AppPreferences.POKE_WIRE_RADIX1.get());
      var radix2 = RadixOption.decode(AppPreferences.POKE_WIRE_RADIX2.get());
      if (radix1 == null) radix1 = RadixOption.RADIX_2;
      var vStr = radix1.toString(v);
      if (radix2 != null && v.getWidth() > 1) {
        vStr += " / " + radix2.toString(v);
      }

      if (v.getWidth() == 32 || v.getWidth() == 64) {
        vStr += " / " + RadixOption.RADIX_FLOAT.toString(v);
      }
      final var fm = g.getFontMetrics();
      g.setColor(caretColor);

      var margin = 2;
      var width = fm.stringWidth(vStr) + 2 * margin;
      var pad = 0;
      if (width < 45) {
        pad = (45 - width) / 2;
        width = 45;
      }
      var h = fm.getAscent() + fm.getDescent() + 2 * margin;

      final var rect = canvas.getViewableRect();
      final var dx = Math.max(0, width - (rect.x + rect.width - x));
      // offset of callout stem
      final var dxx1 = (dx > width / 2) ? -30 : 15;
      // offset of callout stem
      final var dxx2 = (dx > width / 2) ? -15 : 30;
      if (y - 15 - h <= rect.y) {
        // callout below cursor - bottom left corner of box
        final var xx = x - dx;
        final var yy = y + 15 + h;
        final int[] xp = {xx, xx, x + dxx1, x, x + dxx2, xx + width, x + width};
        final int[] yp = {yy, yy - h, yy - h, y, yy - h, yy - h, yy};
        g.fillPolygon(xp, yp, xp.length);
        g.setColor(Color.BLACK);
        g.drawPolygon(xp, yp, xp.length);
        g.drawString(vStr, xx + margin + pad, yy - margin - fm.getDescent());
      } else {
        // callout above cursor - bottom left corner of box
        final var xx = x - dx;
        final var yy = y - 15;
        final int[] xp = {xx, xx, xx + width, xx + width, x + dxx2, x, x + dxx1};
        final int[] yp = {yy, yy - h, yy - h, yy, yy, y, yy};
        g.fillPolygon(xp, yp, xp.length);
        g.setColor(Color.BLACK);
        g.drawPolygon(xp, yp, xp.length);
        g.drawString(vStr, xx + margin + pad, yy - margin - fm.getDescent());
      }
    }
  }

  private static final Color caretColor = new Color(255, 255, 150);

  private static final Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
  private static final Cursor move = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

  private final Listener listener;
  private Circuit pokedCircuit;
  private Component pokedComponent;
  private Caret pokeCaret;
  private Point oldPosition;

  public PokeTool() {
    this.listener = new Listener();
  }

  @Override
  public void deselect(Canvas canvas) {
    removeCaret(true);
    canvas.setHighlightedWires(WireSet.EMPTY);
  }

  @Override
  public void draw(Canvas canvas, ComponentDrawContext context) {
    if (pokeCaret != null) pokeCaret.draw(context.getGraphics());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof PokeTool;
  }

  @Override
  public Cursor getCursor() {
    return cursor;
  }

  @Override
  public String getDescription() {
    return S.get("pokeToolDesc");
  }

  @Override
  public String getDisplayName() {
    return S.get("pokeTool");
  }

  @Override
  public int hashCode() {
    return PokeTool.class.hashCode();
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    if (pokeCaret != null) {
      pokeCaret.keyPressed(e);
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent e) {
    if (pokeCaret != null) {
      pokeCaret.keyReleased(e);
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent e) {
    if (pokeCaret != null) {
      pokeCaret.keyTyped(e);
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
    if (pokeCaret != null) {
      pokeCaret.mouseDragged(e);
      canvas.getProject().repaintCanvas();
    } else {
      // move scrollpane dragging hand
      final var m = canvas.getMousePosition();
      if (oldPosition == null || m == null) {
        oldPosition = m;
        return;
      }
      int x = (int) (oldPosition.getX() - m.getX());
      int y = (int) (oldPosition.getY() - m.getY());
      canvas.setCursor(move);
      canvas.setScrollBar(canvas.getHorizzontalScrollBar() + x, canvas.getVerticalScrollBar() + y);
    }
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    final var loc = Location.create(x, y);
    var dirty = false;
    canvas.setHighlightedWires(WireSet.EMPTY);
    if (pokeCaret != null && !pokeCaret.getBounds(g).contains(loc)) {
      dirty = true;
      removeCaret(true);
    }
    if (pokeCaret == null) {
      final var event = new ComponentUserEvent(canvas, x, y);
      final var circ = canvas.getCircuit();
      for (final var c : circ.getAllContaining(loc, g)) {
        if (pokeCaret != null) break;

        if (c instanceof Wire wire) {
          final var caret = new WireCaret(canvas, wire, x, y, canvas.getProject().getOptions().getAttributeSet());
          setPokedComponent(circ, c, caret);
          canvas.setHighlightedWires(circ.getWireSet((Wire) c));
        } else {
          final var p = (Pokable) c.getFeature(Pokable.class);
          if (p != null) {
            final var caret = p.getPokeCaret(event);
            setPokedComponent(circ, c, caret);
            final var attrs = c.getAttributeSet();
            if (attrs != null && !attrs.getAttributes().isEmpty()) {
              final var proj = canvas.getProject();
              proj.getFrame().viewComponentAttributes(circ, c);
            }
          }
        }
      }
    }
    if (pokeCaret != null) {
      dirty = true;
      pokeCaret.mousePressed(e);
    }
    if (dirty) canvas.getProject().repaintCanvas();
  }

  @Override
  public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
    oldPosition = null;
    if (pokeCaret != null) {
      pokeCaret.mouseReleased(e);
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void paintIcon(ComponentDrawContext c, int x, int y) {
    final var g2 = (Graphics2D) c.getGraphics().create();
    g2.translate(x, y);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    final var p = new GeneralPath();
    p.moveTo(scale(6), scale(15));
    p.quadTo(scale(5), scale(10), scale(1), scale(7));
    p.quadTo(scale(2.5), scale(4), scale(4), scale(7));
    p.quadTo(scale(6), scale(6), scale(6), scale(10));
    p.lineTo(scale(6), scale(1));
    p.quadTo(scale(7), scale(-1), scale(8), scale(1));
    p.lineTo(scale(8), scale(8));
    p.quadTo(scale(9), scale(2), scale(10), scale(8));
    p.quadTo(scale(11), scale(2), scale(12), scale(8));
    p.lineTo(scale(12), scale(9));
    p.quadTo(scale(13), scale(4), scale(14), scale(9));
    p.quadTo(scale(12), scale(11), scale(13), scale(15));
    g2.setColor(new Color(240, 184, 160));
    g2.fill(p);
    g2.setColor(Color.BLACK);
    g2.draw(p);
    g2.dispose();
  }

  private static double scale(double s) {
    return AppPreferences.getScaled(s);
  }

  private void removeCaret(boolean normal) {
    final var caret = pokeCaret;
    if (caret != null) {
      final var circ = pokedCircuit;
      if (normal)
        caret.stopEditing();
      else
        caret.cancelEditing();
      circ.removeCircuitListener(listener);
      pokedCircuit = null;
      pokedComponent = null;
      pokeCaret = null;
    }
  }

  private void setPokedComponent(Circuit circ, Component comp, Caret caret) {
    removeCaret(true);
    pokedCircuit = circ;
    pokedComponent = comp;
    pokeCaret = caret;
    if (caret != null) {
      circ.addCircuitListener(listener);
    }
  }

  public boolean isScrollable() {
    return pokeCaret != null && !(pokeCaret instanceof WireCaret);
  }
}
