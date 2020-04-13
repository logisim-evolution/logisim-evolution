/**
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

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class WiringTool extends Tool {
  private static Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

  private static final int HORIZONTAL = 1;
  private static final int VERTICAL = 2;

  private boolean exists = false;
  private boolean inCanvas = false;
  private Location start = Location.create(0, 0);
  private Location cur = Location.create(0, 0);
  private boolean hasDragged = false;
  private boolean startShortening = false;
  private Wire shortening = null;
  private Action lastAction = null;
  private int direction = 0;

  public WiringTool() {
    super.select(null);
  }

  private Wire checkForRepairs(Canvas canvas, Wire w, Location end) {
    if (w.getLength() <= 10) return w; // don't repair a short wire to nothing
    if (!canvas.getCircuit().getNonWires(end).isEmpty()) return w;

    int delta = (end.equals(w.getEnd0()) ? 10 : -10);
    Location cand;
    if (w.isVertical()) {
      cand = Location.create(end.getX(), end.getY() + delta);
    } else {
      cand = Location.create(end.getX() + delta, end.getY());
    }

    for (Component comp : canvas.getCircuit().getNonWires(cand)) {
      if (comp.getBounds().contains(end, 2)) {
        WireRepair repair = (WireRepair) comp.getFeature(WireRepair.class);
        if (repair != null && repair.shouldRepairWire(new WireRepairData(w, cand))) {
          w = Wire.create(w.getOtherEnd(end), cand);
          canvas.repaint(end.getX() - 13, end.getY() - 13, 26, 26);
          return w;
        }
      }
    }
    return w;
  }

  private boolean computeMove(int newX, int newY) {
    if (cur.getX() == newX && cur.getY() == newY) return false;
    Location start = this.start;
    if (direction == 0) {
      if (newX != start.getX()) direction = HORIZONTAL;
      else if (newY != start.getY()) direction = VERTICAL;
    } else if (direction == HORIZONTAL && newX == start.getX()) {
      if (newY == start.getY()) direction = 0;
      else direction = VERTICAL;
    } else if (direction == VERTICAL && newY == start.getY()) {
      if (newX == start.getX()) direction = 0;
      else direction = HORIZONTAL;
    }
    return true;
  }

  @Override
  public void draw(Canvas canvas, ComponentDrawContext context) {
    Graphics g = context.getGraphics();
    if (exists) {
      Location e0 = start;
      Location e1 = cur;
      Wire shortenBefore = willShorten(start, cur);
      if (shortenBefore != null) {
        Wire shorten = getShortenResult(shortenBefore, start, cur);
        if (shorten == null) {
          return;
        } else {
          e0 = shorten.getEnd0();
          e1 = shorten.getEnd1();
        }
      }
      int x0 = e0.getX();
      int y0 = e0.getY();
      int x1 = e1.getX();
      int y1 = e1.getY();

      g.setColor(Color.BLACK);
      GraphicsUtil.switchToWidth(g, 3);
      if (direction == HORIZONTAL) {
        if (x0 != x1) g.drawLine(x0, y0, x1, y0);
        if (y0 != y1) g.drawLine(x1, y0, x1, y1);
      } else if (direction == VERTICAL) {
        if (y0 != y1) g.drawLine(x0, y0, x0, y1);
        if (x0 != x1) g.drawLine(x0, y1, x1, y1);
      }
    } else if (AppPreferences.ADD_SHOW_GHOSTS.getBoolean() && inCanvas) {
      g.setColor(Color.GRAY);
      g.fillOval(cur.getX() - 2, cur.getY() - 2, 5, 5);
    }
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof WiringTool;
  }

  @Override
  public Cursor getCursor() {
    return cursor;
  }

  @Override
  public String getDescription() {
    return S.get("wiringToolDesc");
  }

  @Override
  public String getDisplayName() {
    return S.get("wiringTool");
  }

  @Override
  public Set<Component> getHiddenComponents(Canvas canvas) {
    Component shorten = willShorten(start, cur);
    if (shorten != null) {
      return Collections.singleton(shorten);
    } else {
      return null;
    }
  }

  @Override
  public String getName() {
    return "Wiring Tool";
  }

  private Wire getShortenResult(Wire shorten, Location drag0, Location drag1) {
    if (shorten == null) {
      return null;
    } else {
      Location e0;
      Location e1;
      if (shorten.endsAt(drag0)) {
        e0 = drag1;
        e1 = shorten.getOtherEnd(drag0);
      } else if (shorten.endsAt(drag1)) {
        e0 = drag0;
        e1 = shorten.getOtherEnd(drag1);
      } else {
        return null;
      }
      return e0.equals(e1) ? null : Wire.create(e0, e1);
    }
  }

  @Override
  public int hashCode() {
    return WiringTool.class.hashCode();
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent event) {
    switch (event.getKeyCode()) {
      case KeyEvent.VK_BACK_SPACE:
        if (lastAction != null && canvas.getProject().getLastAction() == lastAction) {
          canvas.getProject().undoAction();
          lastAction = null;
        }
    }
  }

  @Override
  public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
    if (exists) {
      Canvas.snapToGrid(e);
      int curX = e.getX();
      int curY = e.getY();
      if (!computeMove(curX, curY)) return;
      hasDragged = true;

      Rectangle rect = new Rectangle();
      rect.add(start.getX(), start.getY());
      rect.add(cur.getX(), cur.getY());
      rect.add(curX, curY);
      rect.grow(3, 3);

      cur = Location.create(curX, curY);
      super.mouseDragged(canvas, g, e);

      Wire shorten = null;
      if (startShortening) {
        for (Wire w : canvas.getCircuit().getWires(start)) {
          if (w.contains(cur)) {
            shorten = w;
            break;
          }
        }
      }
      if (shorten == null) {
        for (Wire w : canvas.getCircuit().getWires(cur)) {
          if (w.contains(start)) {
            shorten = w;
            break;
          }
        }
      }
      shortening = shorten;

      canvas.repaint(rect);
    }
  }

  @Override
  public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) {
    inCanvas = true;
    canvas.getProject().repaintCanvas();
  }

  @Override
  public void mouseExited(Canvas canvas, Graphics g, MouseEvent e) {
    inCanvas = false;
    canvas.getProject().repaintCanvas();
  }

  @Override
  public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) {
    if (exists) {
      mouseDragged(canvas, g, e);
    } else {
      Canvas.snapToGrid(e);
      inCanvas = true;
      int curX = e.getX();
      int curY = e.getY();
      if (cur.getX() != curX || cur.getY() != curY) {
        cur = Location.create(curX, curY);
      }
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    if (!canvas.getProject().getLogisimFile().contains(canvas.getCircuit())) {
      exists = false;
      canvas.setErrorMessage(S.getter("cannotModifyError"));
      return;
    }

    if (exists) {
      mouseDragged(canvas, g, e);
    } else {
      Canvas.snapToGrid(e);
      start = Location.create(e.getX(), e.getY());
      cur = start;
      exists = true;
      hasDragged = false;

      startShortening = !canvas.getCircuit().getWires(start).isEmpty();
      shortening = null;

      super.mousePressed(canvas, g, e);
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
    if (!exists) return;

    Canvas.snapToGrid(e);
    int curX = e.getX();
    int curY = e.getY();
    if (computeMove(curX, curY)) {
      cur = Location.create(curX, curY);
    }
    if (hasDragged) {
      exists = false;
      super.mouseReleased(canvas, g, e);

      ArrayList<Wire> ws = new ArrayList<Wire>(2);
      if (cur.getY() == start.getY() || cur.getX() == start.getX()) {
        Wire w = Wire.create(cur, start);
        w = checkForRepairs(canvas, w, w.getEnd0());
        w = checkForRepairs(canvas, w, w.getEnd1());
        if (performShortening(canvas, start, cur)) {
          return;
        }
        if (w.getLength() > 0) ws.add(w);
      } else {
        Location m;
        if (direction == HORIZONTAL) {
          m = Location.create(cur.getX(), start.getY());
        } else {
          m = Location.create(start.getX(), cur.getY());
        }
        Wire w0 = Wire.create(start, m);
        Wire w1 = Wire.create(m, cur);
        w0 = checkForRepairs(canvas, w0, start);
        w1 = checkForRepairs(canvas, w1, cur);
        if (w0.getLength() > 0) ws.add(w0);
        if (w1.getLength() > 0) ws.add(w1);
      }
      if (ws.size() > 0) {
        CircuitMutation mutation = new CircuitMutation(canvas.getCircuit());
        mutation.addAll(ws);
        StringGetter desc;
        if (ws.size() == 1) desc = S.getter("addWireAction");
        else desc = S.getter("addWiresAction");
        Action act = mutation.toAction(desc);
        canvas.getProject().doAction(act);
        lastAction = act;
      }
    }
  }

  @Override
  public void paintIcon(ComponentDrawContext c, int x, int y) {
    Graphics2D g2 = (Graphics2D) c.getGraphics().create();
    g2.translate(x, y);
    int[] points = {3,13,8,13,8,3,13,3};
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    g2.setColor(Color.BLACK);
    for (int i = 0 ; i < points.length-2 ; i+=2)
      g2.drawLine(AppPreferences.getScaled(points[i]), AppPreferences.getScaled(points[i+1]), 
    		AppPreferences.getScaled(points[i+2]), AppPreferences.getScaled(points[i+3]));
    g2.setColor(Value.TRUE_COLOR);
    int wh = AppPreferences.getScaled(5);
    g2.fillOval(AppPreferences.getScaled(1), AppPreferences.getScaled(11), wh, wh);
    g2.setColor(Value.UNKNOWN_COLOR);
    g2.fillOval(AppPreferences.getScaled(11), AppPreferences.getScaled(1), wh, wh);
    g2.dispose();
  }

  private boolean performShortening(Canvas canvas, Location drag0, Location drag1) {
    Wire shorten = willShorten(drag0, drag1);
    if (shorten == null) {
      return false;
    } else {
      CircuitMutation xn = new CircuitMutation(canvas.getCircuit());
      StringGetter actName;
      Wire result = getShortenResult(shorten, drag0, drag1);
      if (result == null) {
        xn.remove(shorten);
        actName = S.getter("removeComponentAction", shorten.getFactory().getDisplayGetter());
      } else {
        xn.replace(shorten, result);
        actName = S.getter("shortenWireAction");
      }
      canvas.getProject().doAction(xn.toAction(actName));
      return true;
    }
  }

  private void reset() {
    exists = false;
    inCanvas = false;
    start = Location.create(0, 0);
    cur = Location.create(0, 0);
    startShortening = false;
    shortening = null;
    direction = 0;
  }

  void resetClick() {
    exists = false;
  }

  @Override
  public void select(Canvas canvas) {
    super.select(canvas);
    lastAction = null;
    reset();
  }

  private Wire willShorten(Location drag0, Location drag1) {
    Wire shorten = shortening;
    if (shorten == null) {
      return null;
    } else if (shorten.endsAt(drag0) || shorten.endsAt(drag1)) {
      return shorten;
    } else {
      return null;
    }
  }
}
