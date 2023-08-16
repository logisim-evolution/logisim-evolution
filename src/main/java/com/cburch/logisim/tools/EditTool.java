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

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.Selection.Event;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public class EditTool extends Tool {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Edit Tool";

  private class Listener implements CircuitListener, Selection.Listener {
    @Override
    public void circuitChanged(CircuitEvent event) {
      if (event.getAction() != CircuitEvent.ACTION_INVALIDATE) {
        lastX = -1;
        cache.clear();
        updateLocation(lastCanvas, lastRawX, lastRawY, lastMods);
      }
    }

    @Override
    public void selectionChanged(Event event) {
      lastX = -1;
      cache.clear();
      updateLocation(lastCanvas, lastRawX, lastRawY, lastMods);
    }
  }

  private static final int CACHE_MAX_SIZE = 32;

  private static final Location NULL_LOCATION =
      Location.create(Integer.MIN_VALUE, Integer.MIN_VALUE, false);

  private final Listener listener;
  private final SelectTool select;
  private final WiringTool wiring;
  private Tool current;
  private final LinkedHashMap<Location, Boolean> cache;
  private Canvas lastCanvas;
  private int lastRawX;
  private int lastRawY;
  private int lastX; // last coordinates where wiring was computed
  private int lastY;
  private int lastMods; // last modifiers for mouse event
  private Location wireLoc; // coordinates where to draw wiring indicator, if
  private int pressX; // last coordinate where mouse was pressed
  private int pressY; // (used to determine when a short wire has been
  // clicked)

  public EditTool(SelectTool select, WiringTool wiring) {
    this.listener = new Listener();
    this.select = select;
    this.wiring = wiring;
    this.current = select;
    this.cache = new LinkedHashMap<>();
    this.lastX = -1;
    this.wireLoc = NULL_LOCATION;
    this.pressX = -1;
  }

  /* This function will try to rotate if the componenent allows it
   * There is some duplication code here. The function attemptReface
   * can be merged with this one */
  private void attemptRotate(Canvas canvas, KeyEvent e) {
    final var circuit = canvas.getCircuit();
    final var sel = canvas.getSelection();
    final var act = new SetAttributeAction(circuit, S.getter("selectionRefaceAction"));
    for (final var comp : sel.getComponents()) {
      if (!(comp instanceof Wire)) {
        final var attr = getFacingAttribute(comp);
        var d = comp.getAttributeSet().getValue(StdAttr.FACING);
        if (d != null) {
          d = d.getRight();
          if (attr != null) {
            act.set(comp, attr, d);
          }
        }
      }
    }
    if (!act.isEmpty()) {
      canvas.getProject().doAction(act);
      e.consume();
    }
  }

  private void attemptReface(Canvas canvas, final Direction facing, KeyEvent e) {
    /* cancel the limit of no modifier*/
    final var circuit = canvas.getCircuit();
    final var sel = canvas.getSelection();
    final var act = new SetAttributeAction(circuit, S.getter("selectionRefaceAction"));
    for (final var comp : sel.getComponents()) {
      if (!(comp instanceof Wire)) {
        final var attr = getFacingAttribute(comp);
        if (attr != null) {
          act.set(comp, attr, facing);
        }
      }
    }
    if (!act.isEmpty()) {
      canvas.getProject().doAction(act);
      e.consume();
    }
  }

  @Override
  public void deselect(Canvas canvas) {
    current = select;
    canvas.getSelection().setSuppressHandles(null);
    cache.clear();
    final var circ = canvas.getCircuit();
    if (circ != null) {
      circ.removeCircuitListener(listener);
    }
    canvas.getSelection().removeListener(listener);
  }

  @Override
  public void draw(Canvas canvas, ComponentDrawContext context) {
    final var loc = wireLoc;
    if (loc != NULL_LOCATION && current != wiring) {
      final var x = loc.getX();
      final var y = loc.getY();
      final var g = context.getGraphics();
      g.setColor(Value.trueColor);
      GraphicsUtil.switchToWidth(g, 2);
      g.drawOval(x - 5, y - 5, 10, 10);
      g.setColor(Color.BLACK);
      GraphicsUtil.switchToWidth(g, 1);
    }
    current.draw(canvas, context);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof EditTool;
  }

  @Override
  public AttributeSet getAttributeSet() {
    return select.getAttributeSet();
  }

  @Override
  public AttributeSet getAttributeSet(Canvas canvas) {
    return canvas.getSelection().getAttributeSet();
  }

  @Override
  public Cursor getCursor() {
    return select.getCursor();
  }

  @Override
  public String getDescription() {
    return S.get("editToolDesc");
  }

  @Override
  public String getDisplayName() {
    return S.get("editTool");
  }

  private Attribute<Direction> getFacingAttribute(Component comp) {
    final var attrs = comp.getAttributeSet();
    Object key = ComponentFactory.FACING_ATTRIBUTE_KEY;
    Attribute<?> a = (Attribute<?>) comp.getFactory().getFeature(key, attrs);
    @SuppressWarnings("unchecked")
    Attribute<Direction> ret = (Attribute<Direction>) a;
    return ret;
  }

  @Override
  public Set<Component> getHiddenComponents(Canvas canvas) {
    return current.getHiddenComponents(canvas);
  }

  @Override
  public int hashCode() {
    return EditTool.class.hashCode();
  }

  @Override
  public boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver) {
    return true;
  }

  private boolean isClick(MouseEvent e) {
    final var px = pressX;
    if (px < 0) {
      return false;
    } else {
      final var dx = e.getX() - px;
      final var dy = e.getY() - pressY;
      if (dx * dx + dy * dy <= 4) {
        return true;
      } else {
        pressX = -1;
        return false;
      }
    }
  }

  private boolean isWiringPoint(Canvas canvas, Location loc, int modsEx) {
    final var wiring = (modsEx & MouseEvent.ALT_DOWN_MASK) == 0;
    final var select = !wiring;

    if (canvas != null && canvas.getSelection() != null) {
      Collection<Component> sel = canvas.getSelection().getComponents();
      if (sel != null) {
        for (final var c : sel) {
          if (c instanceof final Wire w) {
            if (w.contains(loc) && !w.endsAt(loc)) {
              return select;
            }
          }
        }
      }
    }

    final var circ = canvas.getCircuit();
    if (circ == null) {
      return false;
    }
    final var at = circ.getComponents(loc);
    if (CollectionUtil.isNotEmpty(at)) {
      return wiring;
    }
    for (final var w : circ.getWires()) {
      if (w.contains(loc)) {
        return wiring;
      }
    }
    return select;
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    int code = e.getKeyCode();
    int modifier = e.getModifiersEx();
    if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE) {
      if (!canvas.getSelection().isEmpty()) {
        final var act = SelectionActions.clear(canvas.getSelection());
        canvas.getProject().doAction(act);
        e.consume();
      } else {
        wiring.keyPressed(canvas, e);
      }
    } else if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_EDIT_TOOL_DUPLICATE)
        .compare(code, modifier)) {
      final var act = SelectionActions.duplicate(canvas.getSelection());
      canvas.getProject().doAction(act);
      e.consume();
    } else if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_DIR_NORTH).compare(code, modifier)) {
      attemptReface(canvas, Direction.NORTH, e);
    } else if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_DIR_SOUTH).compare(code, modifier)) {
      attemptReface(canvas, Direction.SOUTH, e);
    } else if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_DIR_EAST).compare(code, modifier)) {
      attemptReface(canvas, Direction.EAST, e);
    } else if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_DIR_WEST).compare(code, modifier)) {
      attemptReface(canvas, Direction.WEST, e);
    } else if (code == KeyEvent.VK_ALT) {
      updateLocation(canvas, e);
      e.consume();
    } else if (code == KeyEvent.VK_SPACE) {
      /* Check if ctrl was pressed or not */
      if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK) {
        attemptRotate(canvas, e);
      } else {
        select.keyPressed(canvas, e);
      }
    } else {
      select.keyPressed(canvas, e);
    }
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ALT) {
      updateLocation(canvas, e);
      e.consume();
    } else {
      select.keyReleased(canvas, e);
    }
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent e) {
    select.keyTyped(canvas, e);
  }

  @Override
  public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
    isClick(e);
    current.mouseDragged(canvas, g, e);
  }

  @Override
  public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) {
    pressX = -1;
    current.mouseEntered(canvas, g, e);
  }

  @Override
  public void mouseExited(Canvas canvas, Graphics g, MouseEvent e) {
    pressX = -1;
    current.mouseExited(canvas, g, e);
  }

  @Override
  public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) {
    updateLocation(canvas, e);
    select.mouseMoved(canvas, g, e);
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    canvas.requestFocusInWindow();
    var wire = updateLocation(canvas, e);
    final var oldWireLoc = wireLoc;
    wireLoc = NULL_LOCATION;
    lastX = Integer.MIN_VALUE;
    if (wire) {
      current = wiring;
      final var sel = canvas.getSelection();
      final var circ = canvas.getCircuit();
      final var selected = sel.getAnchoredComponents();
      ArrayList<Component> suppress = null;
      for (final var w : circ.getWires()) {
        if (selected.contains(w)) {
          if (w.contains(oldWireLoc)) {
            if (suppress == null) {
              suppress = new ArrayList<>();
            }
            suppress.add(w);
          }
        }
      }
      sel.setSuppressHandles(suppress);
    } else {
      current = select;
    }
    pressX = e.getX();
    pressY = e.getY();
    current.mousePressed(canvas, g, e);
  }

  @Override
  public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
    final var click = isClick(e) && current == wiring;
    canvas.getSelection().setSuppressHandles(null);
    current.mouseReleased(canvas, g, e);
    if (click) {
      wiring.resetClick();
      select.mousePressed(canvas, g, e);
      select.mouseReleased(canvas, g, e);
    }
    current = select;
    cache.clear();
    updateLocation(canvas, e);
  }

  @Override
  public void paintIcon(ComponentDrawContext c, int x, int y) {
    select.paintIcon(c, x, y);
  }

  @Override
  public void select(Canvas canvas) {
    current = select;
    lastCanvas = canvas;
    cache.clear();
    final var circ = canvas.getCircuit();
    if (circ != null) {
      circ.addCircuitListener(listener);
    }
    canvas.getSelection().addListener(listener);
    select.select(canvas);
  }

  @Override
  public void setAttributeSet(AttributeSet attrs) {
    select.setAttributeSet(attrs);
  }

  private boolean updateLocation(Canvas canvas, int mx, int my, int mods) {
    var snapx = Canvas.snapXToGrid(mx);
    var snapy = Canvas.snapYToGrid(my);
    final var dx = mx - snapx;
    final var dy = my - snapy;
    var isEligible = dx * dx + dy * dy < 36;
    if ((mods & MouseEvent.ALT_DOWN_MASK) != 0) {
      isEligible = true;
    }
    if (!isEligible) {
      snapx = -1;
      snapy = -1;
    }
    final var modsSame = lastMods == mods;
    lastCanvas = canvas;
    lastRawX = mx;
    lastRawY = my;
    lastMods = mods;
    if (lastX == snapx && lastY == snapy && modsSame) { // already computed
      return wireLoc != NULL_LOCATION;
    } else {
      final var snap = Location.create(snapx, snapy, false);
      if (modsSame) {
        Object o = cache.get(snap);
        if (o != null) {
          lastX = snapx;
          lastY = snapy;
          Location oldWireLoc = wireLoc;
          boolean ret = (Boolean) o;
          wireLoc = ret ? snap : NULL_LOCATION;
          repaintIndicators(canvas, oldWireLoc, wireLoc);
          return ret;
        }
      } else {
        cache.clear();
      }

      final var oldWireLoc = wireLoc;
      boolean ret = isEligible && isWiringPoint(canvas, snap, mods);
      wireLoc = ret ? snap : NULL_LOCATION;
      cache.put(snap, ret);
      int toRemove = cache.size() - CACHE_MAX_SIZE;
      Iterator<Location> it = cache.keySet().iterator();
      while (it.hasNext() && toRemove > 0) {
        it.next();
        it.remove();
        toRemove--;
      }

      lastX = snapx;
      lastY = snapy;
      repaintIndicators(canvas, oldWireLoc, wireLoc);
      return ret;
    }
  }

  private boolean updateLocation(Canvas canvas, KeyEvent e) {
    int x = lastRawX;
    if (x >= 0) {
      return updateLocation(canvas, x, lastRawY, e.getModifiersEx());
    } else {
      return false;
    }
  }

  private boolean updateLocation(Canvas canvas, MouseEvent e) {
    return updateLocation(canvas, e.getX(), e.getY(), e.getModifiersEx());
  }

  private void repaintIndicators(Canvas canvas, Location a, Location b) {
    if (a.equals(b)) {
      return;
    }
    if (a != NULL_LOCATION) {
      canvas.repaint(a.getX() - 6, a.getY() - 6, 12, 12);
    }
    if (b != NULL_LOCATION) {
      canvas.repaint(b.getX() - 6, b.getY() - 6, 12, 12);
    }
  }
}
