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
import lombok.val;

public class SelectTool extends Tool {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Select Tool";

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
    if (getter instanceof ComputingMessage) {
      val msg = (ComputingMessage) getter;
      if (msg.dx == dx && msg.dy == dy) {
        canvas.setErrorMessage(null);
        canvas.repaint();
      }
    }
  }

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
  private Location start;
  private int state;
  private int curDx;
  private int curDy;
  private boolean drawConnections;
  private MoveGesture moveGesture;
  private HashMap<Component, KeyConfigurator> keyHandlers;
  private static final SelectIcon ICON = new SelectIcon();

  private final HashSet<Selection> selectionsAdded;
  private final AutoLabel AutoLabler = new AutoLabel();

  private final Listener selListener;

  public SelectTool() {
    start = null;
    state = IDLE;
    selectionsAdded = new HashSet<>();
    selListener = new Listener();
    keyHandlers = null;
  }

  private void computeDxDy(Project proj, MouseEvent e, Graphics g) {
    val bds = proj.getSelection().getBounds(g);
    int dx;
    int dy;
    if (bds == Bounds.EMPTY_BOUNDS) {
      dx = e.getX() - start.getX();
      dy = e.getY() - start.getY();
    } else {
      dx = Math.max(e.getX() - start.getX(), -bds.getX());
      dy = Math.max(e.getY() - start.getY(), -bds.getY());
    }

    val sel = proj.getSelection();
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
    val proj = canvas.getProject();
    var dx = curDx;
    var dy = curDy;
    if (state == MOVING) {
      proj.getSelection().drawGhostsShifted(context, dx, dy);

      val gesture = moveGesture;
      if (gesture != null && drawConnections && (dx != 0 || dy != 0)) {
        val result = gesture.findResult(dx, dy);
        if (result != null) {
          val wiresToAdd = result.getWiresToAdd();
          val gfx = context.getGraphics();
          GraphicsUtil.switchToWidth(gfx, 3);
          gfx.setColor(Color.GRAY);
          for (val wire : wiresToAdd) {
            val loc0 = wire.getEnd0();
            val loc1 = wire.getEnd1();
            gfx.drawLine(loc0.getX(), loc0.getY(), loc1.getX(), loc1.getY());
          }
          GraphicsUtil.switchToWidth(gfx, 1);
          gfx.setColor(COLOR_UNMATCHED);
          for (val conn : result.getUnconnectedLocations()) {
            val connX = conn.getX();
            val connY = conn.getY();
            gfx.fillOval(connX - 3, connY - 3, 6, 6);
            gfx.fillOval(connX + dx - 3, connY + dy - 3, 6, 6);
          }
        }
      }
    } else if (state == RECT_SELECT) {
      var left = start.getX();
      var right = left + dx;
      if (left > right) {
        val oldLeft = left;
        left = right;
        right = oldLeft;
      }
      var top = start.getY();
      var bottom = top + dy;
      if (top > bottom) {
        val oldTop = top;
        top = bottom;
        bottom = oldTop;
      }

      val gBase = context.getGraphics();
      var width = right - left - 1;
      var height = bottom - top - 1;
      if (width > 2 && height > 2) {
        gBase.setColor(BACKGROUND_RECT_SELECT);
        gBase.fillRect(left + 1, top + 1, width - 1, height - 1);
      }

      val circ = canvas.getCircuit();
      val bounds = Bounds.create(left, top, right - left, bottom - top);
      for (val c : circ.getAllWithin(bounds)) {
        val cloc = c.getLocation();
        val gDup = gBase.create();
        context.setGraphics(gDup);
        c.getFactory().drawGhost(context, COLOR_RECT_SELECT, cloc.getX(), cloc.getY(), c.getAttributeSet());
        gDup.dispose();
      }

      gBase.setColor(COLOR_RECT_SELECT);
      GraphicsUtil.switchToWidth(gBase, 2);
      if (width < 0) width = 0;
      if (height < 0) height = 0;
      gBase.drawRect(left, top, width, height);
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
    return state == IDLE ? selectCursor : (state == RECT_SELECT ? rectSelectCursor : moveCursor);
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
    if (state != MOVING) {
      return null;
    }

    val dx = curDx;
    val dy = curDy;
    if (dx == 0 && dy == 0) {
      return null;
    }

    val sel = canvas.getSelection().getComponents();
    val gesture = moveGesture;
    if (gesture != null && drawConnections) {
      val result = gesture.findResult(dx, dy);
      if (result != null) {
        val ret = new HashSet<Component>(sel);
        ret.addAll(result.getReplacementMap().getRemovals());
        return ret;
      }
    }
    return sel;
  }

  private void handleMoveDrag(Canvas canvas, int dx, int dy, int modsEx) {
    var connect = shouldConnect(canvas, modsEx);
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
      val comps = AutoLabel.sort(canvas.getProject().getSelection().getComponents());
      val keyboardEvent = e.getKeyCode();
      var keyTaken = false;
      for (val comp : comps) {
        val act = new SetAttributeAction(canvas.getCircuit(), S.getter("changeComponentAttributesAction"));
        keyTaken |=
            GateKeyboardModifier.TookKeyboardStrokes(
                keyboardEvent, comp, comp.getAttributeSet(), canvas, act, true);
        if (!act.isEmpty()) canvas.getProject().doAction(act);
      }
      if (!keyTaken) {
        for (val comp : comps) {
          val act = new SetAttributeAction(canvas.getCircuit(), S.getter("changeComponentAttributesAction"));
          keyTaken |=
              AutoLabler.labelKeyboardHandler(
                  keyboardEvent,
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
      if (!keyTaken)
        switch (keyboardEvent) {
          case KeyEvent.VK_BACK_SPACE:
          case KeyEvent.VK_DELETE:
            if (!canvas.getSelection().isEmpty()) {
              val act = SelectionActions.clear(canvas.getSelection());
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
  public void mouseDragged(Canvas canvas, Graphics gfx, MouseEvent e) {
    if (state == MOVING) {
      val proj = canvas.getProject();
      computeDxDy(proj, e, gfx);
      handleMoveDrag(canvas, curDx, curDy, e.getModifiersEx());
    } else if (state == RECT_SELECT) {
      val proj = canvas.getProject();
      curDx = e.getX() - start.getX();
      curDy = e.getY() - start.getY();
      proj.repaintCanvas();
    }
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics gfx, MouseEvent e) {
    canvas.requestFocusInWindow();
    val proj = canvas.getProject();
    val sel = proj.getSelection();
    val circuit = canvas.getCircuit();
    start = Location.create(e.getX(), e.getY());
    curDx = 0;
    curDy = 0;
    moveGesture = null;

    // if the user clicks into the selection,
    // selection is being modified
    val in_sel = sel.getComponentsContaining(start, gfx);
    if (!in_sel.isEmpty()) {
      if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
        setState(proj, MOVING);
        proj.repaintCanvas();
        return;
      } else {
        val act = SelectionActions.drop(sel, in_sel);
        if (act != null) {
          proj.doAction(act);
        }
      }
    }

    // if the user clicks into a component outside selection, user
    // wants to add/reset selection
    val clicked = circuit.getAllContaining(start, gfx);
    if (!clicked.isEmpty()) {
      if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
        if (sel.getComponentsContaining(start).isEmpty()) {
          val act = SelectionActions.dropAll(sel);
          if (act != null) {
            proj.doAction(act);
          }
        }
      }
      for (val comp : clicked) {
        if (!in_sel.contains(comp)) {
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
      val act = SelectionActions.dropAll(sel);
      if (act != null) {
        proj.doAction(act);
      }
    }
    setState(proj, RECT_SELECT);
    proj.repaintCanvas();
  }

  @Override
  public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
    val proj = canvas.getProject();
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
          val connect = shouldConnect(canvas, e.getModifiersEx());
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
            val result = gesture.forceRequest(dx, dy);
            clearCanvasMessage(canvas, dx, dy);
            repl = result.getReplacementMap();
          } else {
            repl = null;
          }
          val sel = proj.getSelection();
          proj.doAction(SelectionActions.translate(sel, dx, dy, repl));
        }
      }
      moveGesture = null;
      proj.repaintCanvas();
    } else if (state == RECT_SELECT) {
      val bds = Bounds.create(start).add(start.getX() + curDx, start.getY() + curDy);
      val circuit = canvas.getCircuit();
      val sel = proj.getSelection();
      val in_sel = sel.getComponentsWithin(bds, g);
      for (val comp : circuit.getAllWithin(bds, g)) {
        if (!in_sel.contains(comp)) sel.add(comp);
      }
      val act = SelectionActions.drop(sel, in_sel);
      if (act != null) {
        proj.doAction(act);
      }
      setState(proj, IDLE);
      proj.repaintCanvas();
    }
    if (e.getClickCount() >= 2) {
      val comps = canvas.getProject().getSelection().getComponents();
      if (comps.size() == 1) {
        for (val comp : comps) {
          if (comp.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
            val oldLabel = comp.getAttributeSet().getValue(StdAttr.LABEL);
            val act =
                new SetAttributeAction(
                    canvas.getCircuit(), S.getter("changeComponentAttributesAction"));
            AutoLabler.askAndSetLabel(
                comp.getFactory().getDisplayName(),
                oldLabel,
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
      val sel = canvas.getSelection();
      for (val comp : sel.getComponents()) {
        val factory = comp.getFactory();
        val attrs = comp.getAttributeSet();
        val handler = factory.getFeature(KeyConfigurator.class, attrs);
        if (handler != null) {
          val base = (KeyConfigurator) handler;
          handlers.put(comp, base.clone());
        }
      }
      keyHandlers = handlers;
    }

    if (!handlers.isEmpty()) {
      var consume = false;
      val results = new ArrayList<KeyConfigurationResult>();
      for (val entry : handlers.entrySet()) {
        val comp = entry.getKey();
        val handler = entry.getValue();
        val event = new KeyConfigurationEvent(type, comp.getAttributeSet(), e, comp);
        val result = handler.keyEventReceived(event);
        consume |= event.isConsumed();
        if (result != null) {
          results.add(result);
        }
      }
      if (consume) {
        e.consume();
      }
      if (!results.isEmpty()) {
        val act = new SetAttributeAction(canvas.getCircuit(), S.getter("changeComponentAttributesAction"));
        for (val result : results) {
          val comp = (Component) result.getEvent().getData();
          val newValues = result.getAttributeValues();
          for (val entry : newValues.entrySet()) {
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
    val sel = canvas.getSelection();
    if (!selectionsAdded.contains(sel)) {
      sel.addListener(selListener);
    }
  }

  private void setState(Project proj, int new_state) {
    if (state == new_state) return; // do nothing if state not new

    state = new_state;
    proj.getFrame().getCanvas().setCursor(getCursor());
  }

  private boolean shouldConnect(Canvas canvas, int modsEx) {
    val shiftReleased = (modsEx & MouseEvent.SHIFT_DOWN_MASK) == 0;
    val dflt = AppPreferences.MOVE_KEEP_CONNECT.getBoolean();
    if (shiftReleased) {
      return dflt;
    } else {
      return !dflt;
    }
  }
}
