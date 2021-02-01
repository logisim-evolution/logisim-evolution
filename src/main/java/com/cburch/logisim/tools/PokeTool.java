/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;

public class PokeTool extends Tool {
  private class Listener implements CircuitListener {
    public void circuitChanged(CircuitEvent event) {
      Circuit circ = pokedCircuit;
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
    //
    Canvas canvas;
    Wire wire;
    int x;
    int y;

    WireCaret(Canvas c, Wire w, int x, int y, AttributeSet opts) {
      canvas = c;
      wire = w;
      this.x = x;
      this.y = y;
      // this.opts = opts;
    }

    @Override
    public void draw(Graphics g) {
      Value v = canvas.getCircuitState().getValue(wire.getEnd0());
      RadixOption radix1 = RadixOption.decode(AppPreferences.POKE_WIRE_RADIX1.get());
      RadixOption radix2 = RadixOption.decode(AppPreferences.POKE_WIRE_RADIX2.get());
      if (radix1 == null) radix1 = RadixOption.RADIX_2;
      String vStr = radix1.toString(v);
      if (radix2 != null && v.getWidth() > 1) {
        vStr += " / " + radix2.toString(v);
      }

      FontMetrics fm = g.getFontMetrics();
      g.setColor(caretColor);

      int margin = 2;
      int w = fm.stringWidth(vStr) + 2 * margin;
      int pad = 0;
      if (w < 45) {
        pad = (45 - w) / 2;
        w = 45;
      }
      int h = fm.getAscent() + fm.getDescent() + 2 * margin;

      Rectangle r = canvas.getViewableRect();
      int dx = Math.max(0, w - (r.x + r.width - x));
      int dxx1 = (dx > w / 2) ? -30 : 15; // offset of callout stem
      int dxx2 = (dx > w / 2) ? -15 : 30; // offset of callout stem
      if (y - 15 - h <= r.y) {
        // callout below cursor
        int xx = x - dx, yy = y + 15 + h; // bottom left corner of box
        int[] xp = {xx, xx, x + dxx1, x, x + dxx2, xx + w, x + w};
        int[] yp = {yy, yy - h, yy - h, y, yy - h, yy - h, yy};
        g.fillPolygon(xp, yp, xp.length);
        g.setColor(Color.BLACK);
        g.drawPolygon(xp, yp, xp.length);
        g.drawString(vStr, xx + margin + pad, yy - margin - fm.getDescent());
      } else {
        // callout above cursor
        int xx = x - dx, yy = y - 15; // bottom left corner of box
        int[] xp = {xx, xx, xx + w, xx + w, x + dxx2, x, x + dxx1};
        int[] yp = {yy, yy - h, yy - h, yy, yy, y, yy};
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
  private Point OldPosition;

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
  public String getName() {
    return "Poke Tool";
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
      Point m = canvas.getMousePosition();
      if (OldPosition == null || m == null) {
        OldPosition = m;
        return;
      }
      int x = (int) (OldPosition.getX() - m.getX());
      int y = (int) (OldPosition.getY() - m.getY());
      canvas.setCursor(move);
      canvas.setScrollBar(canvas.getHorizzontalScrollBar() + x, canvas.getVerticalScrollBar() + y);
    }
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    Location loc = Location.create(x, y);
    boolean dirty = false;
    canvas.setHighlightedWires(WireSet.EMPTY);
    if (pokeCaret != null && !pokeCaret.getBounds(g).contains(loc)) {
      dirty = true;
      removeCaret(true);
    }
    if (pokeCaret == null) {
      ComponentUserEvent event = new ComponentUserEvent(canvas, x, y);
      Circuit circ = canvas.getCircuit();
      for (Component c : circ.getAllContaining(loc, g)) {
        if (pokeCaret != null) break;

        if (c instanceof Wire) {
          Caret caret =
              new WireCaret(
                  canvas, (Wire) c, x, y, canvas.getProject().getOptions().getAttributeSet());
          setPokedComponent(circ, c, caret);
          canvas.setHighlightedWires(circ.getWireSet((Wire) c));
        } else {
          Pokable p = (Pokable) c.getFeature(Pokable.class);
          if (p != null) {
            Caret caret = p.getPokeCaret(event);
            setPokedComponent(circ, c, caret);
            AttributeSet attrs = c.getAttributeSet();
            if (attrs != null && attrs.getAttributes().size() > 0) {
              Project proj = canvas.getProject();
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
    OldPosition = null;
    if (pokeCaret != null) {
      pokeCaret.mouseReleased(e);
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void paintIcon(ComponentDrawContext c, int x, int y) {
    Graphics2D g2 = (Graphics2D) c.getGraphics().create();
    g2.translate(x, y);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    GeneralPath p = new GeneralPath();
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
    g2.setColor(new Color(240,184,160));
    g2.fill(p);
    g2.setColor(Color.BLACK);
    g2.draw(p);
    g2.dispose();
  }
  
  private static double scale(double s) {
    return AppPreferences.getScaled(s);
  }

  private void removeCaret(boolean normal) {
    Circuit circ = pokedCircuit;
    Caret caret = pokeCaret;
    if (caret != null) {
      if (normal) caret.stopEditing();
      else caret.cancelEditing();
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
