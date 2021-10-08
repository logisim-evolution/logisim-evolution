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
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.icons.SelectIcon;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.Selection.Event;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.gates.GateKeyboardModifier;
import com.cburch.logisim.tools.key.KeyConfigurationEvent;
import com.cburch.logisim.tools.key.KeyConfigurationResult;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.tools.move.MoveGesture;
import com.cburch.logisim.tools.move.MoveRequestListener;
import com.cburch.logisim.util.AutoLabel;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SelectTool extends Tool {

  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Select Tool";

  private static final Cursor selectCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
  private static final Cursor rectSelectCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
  private static final Cursor moveCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

  private static final int IDLE = 0;
  private static final int MOVING = 1;
  private static final int RECT_SELECT = 2;

  private static final Color COLOR_UNMATCHED = new Color(192, 0, 0);
  private static final Color COLOR_COMPUTING = new Color(96, 192, 96);
  private static final Color COLOR_RECT_SELECT = new Color(0, 64, 128, 255);
  private static final Color BACKGROUND_RECT_SELECT = new Color(192, 192, 255, 192);

  private static final SelectIcon ICON = new SelectIcon();

  private Location start;
  private int state;
  private int curDx;
  private int curDy;
  private boolean drawConnections;
  private MoveGesture moveGesture;
  private HashMap<Component, KeyConfigurator> keyHandlers;


  private final HashSet<Selection> selectionsAdded;
  private final AutoLabel autoLabeler = new AutoLabel();

  private final Listener selListener;

  public SelectTool() {
    start = null;
    state = IDLE;
    selectionsAdded = new HashSet<>();
    selListener = new Listener();
    keyHandlers = null;
  }

  private static class ComputingMessage implements StringGetter {
    private final int dx;
    private final int dy;

    public ComputingMessage(int dx, int dy) {
      this.dx = dx;
      this.dy = dy;
    }

    @Override
    public String toString() {
      return S.get("moveWorkingMsg");
    }
  }

  private class Listener implements Selection.Listener {
    @Override
    public void selectionChanged(Event event) {
      keyHandlers = null;
    }
  }

  private static class MoveRequestHandler implements MoveRequestListener {
    private final Canvas canvas;

    MoveRequestHandler(Canvas canvas) {
      this.canvas = canvas;
    }

    @Override
    public void requestSatisfied(MoveGesture gesture, int dx, int dy) {
      clearCanvasMessage(canvas, dx, dy);
    }
  }

  private static void clearCanvasMessage(Canvas canvas, int dx, int dy) {
    Object getter = canvas.getErrorMessage();
    if (getter instanceof ComputingMessage msg) {
      if (msg.dx == dx && msg.dy == dy) {
        canvas.setErrorMessage(null);
        canvas.repaint();
      }
    }
  }



  private void computeDxDy(Project proj, MouseEvent e, Graphics g) {
    final var bds = proj.getSelection().getBounds(g);
    int dx;
    int dy;
    if (bds == Bounds.EMPTY_BOUNDS) {
      dx = e.getX() - start.getX();
      dy = e.getY() - start.getY();
    } else {
      dx = Math.max(e.getX() - start.getX(), -bds.getX());
      dy = Math.max(e.getY() - start.getY(), -bds.getY());
    }

    final var sel = proj.getSelection();
    if (sel.shouldSnap()) {
      dx = Canvas.snapXToGrid(dx);
      dy = Canvas.snapYToGrid(dy);
    }
    curDx = dx;
    curDy = dy;
  }

  @Override
  public void deselect(Canvas canvas) {
    moveGesture = null;
  }

  @Override
  public void draw(Canvas canvas, ComponentDrawContext context) {
    final var proj = canvas.getProject();
    var dx = curDx;
    var dy = curDy;
    if (state == MOVING) {
      proj.getSelection().drawGhostsShifted(context, dx, dy);

      final var gesture = moveGesture;
      if (gesture != null && drawConnections && (dx != 0 || dy != 0)) {
        final var result = gesture.findResult(dx, dy);
        if (result != null) {
          final var wiresToAdd = result.getWiresToAdd();
          final var g = context.getGraphics();
          GraphicsUtil.switchToWidth(g, 3);
          g.setColor(Color.GRAY);
          for (final var w : wiresToAdd) {
            final var loc0 = w.getEnd0();
            final var loc1 = w.getEnd1();
            g.drawLine(loc0.getX(), loc0.getY(), loc1.getX(), loc1.getY());
          }
          GraphicsUtil.switchToWidth(g, 1);
          g.setColor(COLOR_UNMATCHED);
          for (final var conn : result.getUnconnectedLocations()) {
            final var connX = conn.getX();
            final var connY = conn.getY();
            g.fillOval(connX - 3, connY - 3, 6, 6);
            g.fillOval(connX + dx - 3, connY + dy - 3, 6, 6);
          }
        }
      }
    } else if (state == RECT_SELECT) {
      int left = start.getX();
      int right = left + dx;
      if (left > right) {
        int i = left;
        left = right;
        right = i;
      }
      int top = start.getY();
      int bot = top + dy;
      if (top > bot) {
        int i = top;
        top = bot;
        bot = i;
      }

      final var gBase = context.getGraphics();
      int w = right - left - 1;
      int h = bot - top - 1;
      if (w > 2 && h > 2) {
        gBase.setColor(BACKGROUND_RECT_SELECT);
        gBase.fillRect(left + 1, top + 1, w - 1, h - 1);
      }

      final var circ = canvas.getCircuit();
      final var bds = Bounds.create(left, top, right - left, bot - top);
      for (final var c : circ.getAllWithin(bds)) {
        final var cloc = c.getLocation();
        final var gDup = gBase.create();
        context.setGraphics(gDup);
        c.getFactory()
            .drawGhost(context, COLOR_RECT_SELECT, cloc.getX(), cloc.getY(), c.getAttributeSet());
        gDup.dispose();
      }

      gBase.setColor(COLOR_RECT_SELECT);
      GraphicsUtil.switchToWidth(gBase, 2);
      if (w < 0) w = 0;
      if (h < 0) h = 0;
      gBase.drawRect(left, top, w, h);
    }
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof SelectTool;
  }

  @Override
  public AttributeSet getAttributeSet(Canvas canvas) {
    return canvas.getSelection().getAttributeSet();
  }

  @Override
  public Cursor getCursor() {
    return state == IDLE
           ? selectCursor
           : (state == RECT_SELECT
              ? rectSelectCursor
              : moveCursor);
  }

  @Override
  public String getDescription() {
    return S.get("selectToolDesc");
  }

  @Override
  public String getDisplayName() {
    return S.get("selectTool");
  }

  @Override
  public Set<Component> getHiddenComponents(Canvas canvas) {
    if (state == MOVING) {
      int dx = curDx;
      int dy = curDy;
      if (dx == 0 && dy == 0) {
        return null;
      }

      final var sel = canvas.getSelection().getComponents();
      final var gesture = moveGesture;
      if (gesture != null && drawConnections) {
        final var result = gesture.findResult(dx, dy);
        if (result != null) {
          final var ret = new HashSet<Component>(sel);
          ret.addAll(result.getReplacementMap().getRemovals());
          return ret;
        }
      }
      return sel;
    } else {
      return null;
    }
  }

  private void handleMoveDrag(Canvas canvas, int dx, int dy, int modsEx) {
    var connect = shouldConnect(modsEx);
    drawConnections = connect;
    if (connect) {
      var gesture = moveGesture;
      if (gesture == null) {
        gesture =
            new MoveGesture(
                new MoveRequestHandler(canvas),
                canvas.getCircuit(),
                canvas.getSelection().getAnchoredComponents());
        moveGesture = gesture;
      }
      if (dx != 0 || dy != 0) {
        var queued = gesture.enqueueRequest(dx, dy);
        if (queued) {
          canvas.setErrorMessage(new ComputingMessage(dx, dy), COLOR_COMPUTING);
          // maybe CPU scheduled led the request to be satisfied
          // just before the "if(queued)" statement. In any case, it
          // doesn't hurt to check to ensure the message belongs.
          if (gesture.findResult(dx, dy) != null) {
            clearCanvasMessage(canvas, dx, dy);
          }
        }
      }
    }
    canvas.repaint();
  }

  @Override
  public int hashCode() {
    return SelectTool.class.hashCode();
  }

  @Override
  public boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver) {
    return true;
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    if (state == MOVING && e.getKeyCode() == KeyEvent.VK_SHIFT) {
      handleMoveDrag(canvas, curDx, curDy, e.getModifiersEx());
    } else {
      final var comps = AutoLabel.sort(canvas.getProject().getSelection().getComponents());
      final var keybEvent = e.getKeyCode();
      var keyTaken = false;
      for (final var comp : comps) {
        final var act = new SetAttributeAction(canvas.getCircuit(), S.getter("changeComponentAttributesAction"));
        keyTaken |= GateKeyboardModifier.tookKeyboardStrokes(keybEvent, comp, comp.getAttributeSet(), canvas, act, true);
        if (!act.isEmpty()) canvas.getProject().doAction(act);
      }
      if (!keyTaken) {
        for (Component comp : comps) {
          final var act = new SetAttributeAction(canvas.getCircuit(), S.getter("changeComponentAttributesAction"));
          keyTaken |=
              autoLabeler.labelKeyboardHandler(
                  keybEvent,
                  comp.getAttributeSet(),
                  comp.getFactory().getDisplayName(),
                  comp,
                  comp.getFactory(),
                  canvas.getCircuit(),
                  act,
                  true);
          if (!act.isEmpty()) canvas.getProject().doAction(act);
        }
      }
      if (!keyTaken) {
        switch (keybEvent) {
          case KeyEvent.VK_BACK_SPACE:
          case KeyEvent.VK_DELETE:
            if (!canvas.getSelection().isEmpty()) {
              final var act = SelectionActions.clear(canvas.getSelection());
              canvas.getProject().doAction(act);
              e.consume();
            }
            break;

          default:
            processKeyEvent(canvas, e, KeyConfigurationEvent.KEY_PRESSED);
            break;
        }
      }
    }
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent e) {
    if (state == MOVING && e.getKeyCode() == KeyEvent.VK_SHIFT) {
      handleMoveDrag(canvas, curDx, curDy, e.getModifiersEx());
    } else {
      processKeyEvent(canvas, e, KeyConfigurationEvent.KEY_RELEASED);
    }
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent e) {
    processKeyEvent(canvas, e, KeyConfigurationEvent.KEY_TYPED);
  }

  @Override
  public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
    if (state == MOVING) {
      final var proj = canvas.getProject();
      computeDxDy(proj, e, g);
      handleMoveDrag(canvas, curDx, curDy, e.getModifiersEx());
    } else if (state == RECT_SELECT) {
      final var proj = canvas.getProject();
      curDx = e.getX() - start.getX();
      curDy = e.getY() - start.getY();
      proj.repaintCanvas();
    }
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    canvas.requestFocusInWindow();
    final var proj = canvas.getProject();
    final var sel = proj.getSelection();
    start = Location.create(e.getX(), e.getY());
    curDx = 0;
    curDy = 0;
    moveGesture = null;

    // if the user clicks into the selection,
    // selection is being modified
    final var inSel = sel.getComponentsContaining(start, g);
    if (!inSel.isEmpty()) {
      if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
        setState(proj, MOVING);
        proj.repaintCanvas();
        return;
      } else {
        final var act = SelectionActions.drop(sel, inSel);
        if (act != null) {
          proj.doAction(act);
        }
      }
    }

    // if the user clicks into a component outside selection, user wants to add/reset selection
    final var circuit = canvas.getCircuit();
    final var clicked = circuit.getAllContaining(start, g);
    if (!clicked.isEmpty()) {
      if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
        if (sel.getComponentsContaining(start).isEmpty()) {
          final var act = SelectionActions.dropAll(sel);
          if (act != null) {
            proj.doAction(act);
          }
        }
      }
      for (final var comp : clicked) {
        if (!inSel.contains(comp)) {
          sel.add(comp);
        }
      }
      setState(proj, MOVING);
      proj.repaintCanvas();
      return;
    }

    // The user clicked on the background. This is a rectangular
    // selection (maybe with the shift key down).
    if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
      final var act = SelectionActions.dropAll(sel);
      if (act != null) {
        proj.doAction(act);
      }
    }
    setState(proj, RECT_SELECT);
    proj.repaintCanvas();
  }

  @Override
  public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
    final var proj = canvas.getProject();
    if (state == MOVING) {
      setState(proj, IDLE);
      computeDxDy(proj, e, g);
      int dx = curDx;
      int dy = curDy;
      if (dx != 0 || dy != 0) {
        if (!proj.getLogisimFile().contains(canvas.getCircuit())) {
          canvas.setErrorMessage(S.getter("cannotModifyError"));
        } else if (proj.getSelection().hasConflictWhenMoved(dx, dy)) {
          canvas.setErrorMessage(S.getter("exclusiveError"));
        } else {
          final var connect = shouldConnect(e.getModifiersEx());
          drawConnections = false;
          ReplacementMap repl;
          if (connect) {
            var gesture = moveGesture;
            if (gesture == null) {
              gesture =
                  new MoveGesture(
                      new MoveRequestHandler(canvas),
                      canvas.getCircuit(),
                      canvas.getSelection().getAnchoredComponents());
            }
            canvas.setErrorMessage(new ComputingMessage(dx, dy), COLOR_COMPUTING);
            final var result = gesture.forceRequest(dx, dy);
            clearCanvasMessage(canvas, dx, dy);
            repl = result.getReplacementMap();
          } else {
            repl = null;
          }
          final var sel = proj.getSelection();
          proj.doAction(SelectionActions.translate(sel, dx, dy, repl));
        }
      }
      moveGesture = null;
      proj.repaintCanvas();
    } else if (state == RECT_SELECT) {
      final var bds = Bounds.create(start).add(start.getX() + curDx, start.getY() + curDy);
      final var circuit = canvas.getCircuit();
      final var sel = proj.getSelection();
      final var inSel = sel.getComponentsWithin(bds, g);
      for (final var comp : circuit.getAllWithin(bds, g)) {
        if (!inSel.contains(comp)) sel.add(comp);
      }
      final var act = SelectionActions.drop(sel, inSel);
      if (act != null) {
        proj.doAction(act);
      }
      setState(proj, IDLE);
      proj.repaintCanvas();
    }
    if (e.getClickCount() >= 2) {
      final var comps = canvas.getProject().getSelection().getComponents();
      if (comps.size() == 1) {
        for (final var comp : comps) {
          if (comp.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
            final var OldLabel = comp.getAttributeSet().getValue(StdAttr.LABEL);
            final var act =
                new SetAttributeAction(
                    canvas.getCircuit(), S.getter("changeComponentAttributesAction"));
            autoLabeler.askAndSetLabel(
                comp.getFactory().getDisplayName(),
                OldLabel,
                canvas.getCircuit(),
                comp,
                comp.getFactory(),
                comp.getAttributeSet(),
                act,
                true);
            if (!act.isEmpty()) canvas.getProject().doAction(act);
          }
        }
      }
    }
  }

  @Override
  public void paintIcon(ComponentDrawContext c, int x, int y) {
    ICON.paintIcon(null, c.getGraphics(), x, y);
  }

  private void processKeyEvent(Canvas canvas, KeyEvent e, int type) {
    var handlers = keyHandlers;
    if (handlers == null) {
      handlers = new HashMap<>();
      final var sel = canvas.getSelection();
      for (final var comp : sel.getComponents()) {
        final var factory = comp.getFactory();
        final var attrs = comp.getAttributeSet();
        Object handler = factory.getFeature(KeyConfigurator.class, attrs);
        if (handler != null) {
          final var base = (KeyConfigurator) handler;
          handlers.put(comp, base.clone());
        }
      }
      keyHandlers = handlers;
    }

    if (!handlers.isEmpty()) {
      var consume = false;
      ArrayList<KeyConfigurationResult> results;
      results = new ArrayList<>();
      for (final var entry : handlers.entrySet()) {
        final var comp = entry.getKey();
        final var handler = entry.getValue();
        final var event = new KeyConfigurationEvent(type, comp.getAttributeSet(), e, comp);
        final var result = handler.keyEventReceived(event);
        consume |= event.isConsumed();
        if (result != null) {
          results.add(result);
        }
      }
      if (consume) {
        e.consume();
      }
      if (!results.isEmpty()) {
        final var act = new SetAttributeAction(canvas.getCircuit(), S.getter("changeComponentAttributesAction"));
        for (final var result : results) {
          final var comp = (Component) result.getEvent().getData();
          final var newValues = result.getAttributeValues();
          for (final var entry : newValues.entrySet()) {
            act.set(comp, entry.getKey(), entry.getValue());
          }
        }
        if (!act.isEmpty()) {
          canvas.getProject().doAction(act);
        }
      }
    }
  }

  @Override
  public void select(Canvas canvas) {
    final var sel = canvas.getSelection();
    if (!selectionsAdded.contains(sel)) {
      sel.addListener(selListener);
    }
  }

  private void setState(Project proj, int newState) {
    if (state != newState) {
      state = newState;
      proj.getFrame().getCanvas().setCursor(getCursor());
    }
  }

  private boolean shouldConnect(int modsEx) {
    final var shiftReleased = (modsEx & MouseEvent.SHIFT_DOWN_MASK) == 0;
    final var defaultValue = AppPreferences.MOVE_KEEP_CONNECT.getBoolean();
    return shiftReleased ? defaultValue : !defaultValue;
  }
}
