/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.swing.Icon;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.Selection.Event;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.gates.GateKeyboardModifier;
import com.cburch.logisim.tools.key.KeyConfigurationEvent;
import com.cburch.logisim.tools.key.KeyConfigurationResult;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.tools.move.MoveGesture;
import com.cburch.logisim.tools.move.MoveRequestListener;
import com.cburch.logisim.tools.move.MoveResult;
import com.cburch.logisim.util.AutoLabel;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;

public class SelectTool extends Tool {
	private static class ComputingMessage implements StringGetter {
		private int dx;
		private int dy;

		public ComputingMessage(int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
		}

		public String toString() {
			return Strings.get("moveWorkingMsg");
		}
	}

	private class Listener implements Selection.Listener {
		public void selectionChanged(Event event) {
			keyHandlers = null;
		}
	}

	private static class MoveRequestHandler implements MoveRequestListener {
		private Canvas canvas;

		MoveRequestHandler(Canvas canvas) {
			this.canvas = canvas;
		}

		public void requestSatisfied(MoveGesture gesture, int dx, int dy) {
			clearCanvasMessage(canvas, dx, dy);
		}
	}

	private static void clearCanvasMessage(Canvas canvas, int dx, int dy) {
		Object getter = canvas.getErrorMessage();
		if (getter instanceof ComputingMessage) {
			ComputingMessage msg = (ComputingMessage) getter;
			if (msg.dx == dx && msg.dy == dy) {
				canvas.setErrorMessage(null);
				canvas.repaint();
			}
		}
	}

	private static final Cursor selectCursor = Cursor
			.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	private static final Cursor rectSelectCursor = Cursor
			.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	private static final Cursor moveCursor = Cursor
			.getPredefinedCursor(Cursor.MOVE_CURSOR);

	private static final int IDLE = 0;
	private static final int MOVING = 1;
	private static final int RECT_SELECT = 2;
	private static final Icon toolIcon = Icons.getIcon("select.gif");

	private static final Color COLOR_UNMATCHED = new Color(192, 0, 0);

	private static final Color COLOR_COMPUTING = new Color(96, 192, 96);

	private static final Color COLOR_RECT_SELECT = new Color(0, 64, 128, 255);
	private static final Color BACKGROUND_RECT_SELECT = new Color(192, 192,
			255, 192);
	private Location start;
	private int state;
	private int curDx;
	private int curDy;
	private boolean drawConnections;
	private MoveGesture moveGesture;
	private HashMap<Component, KeyConfigurator> keyHandlers;

	private HashSet<Selection> selectionsAdded;
	private AutoLabel AutoLabler = new AutoLabel();

	private Listener selListener;

	public SelectTool() {
		start = null;
		state = IDLE;
		selectionsAdded = new HashSet<Selection>();
		selListener = new Listener();
		keyHandlers = null;
	}

	private void computeDxDy(Project proj, MouseEvent e, Graphics g) {
		Bounds bds = proj.getSelection().getBounds(g);
		int dx;
		int dy;
		if (bds == Bounds.EMPTY_BOUNDS) {
			dx = e.getX() - start.getX();
			dy = e.getY() - start.getY();
		} else {
			dx = Math.max(e.getX() - start.getX(), -bds.getX());
			dy = Math.max(e.getY() - start.getY(), -bds.getY());
		}

		Selection sel = proj.getSelection();
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
		Project proj = canvas.getProject();
		int dx = curDx;
		int dy = curDy;
		if (state == MOVING) {
			proj.getSelection().drawGhostsShifted(context, dx, dy);

			MoveGesture gesture = moveGesture;
			if (gesture != null && drawConnections && (dx != 0 || dy != 0)) {
				MoveResult result = gesture.findResult(dx, dy);
				if (result != null) {
					Collection<Wire> wiresToAdd = result.getWiresToAdd();
					Graphics g = context.getGraphics();
					GraphicsUtil.switchToWidth(g, 3);
					g.setColor(Color.GRAY);
					for (Wire w : wiresToAdd) {
						Location loc0 = w.getEnd0();
						Location loc1 = w.getEnd1();
						g.drawLine(loc0.getX(), loc0.getY(), loc1.getX(),
								loc1.getY());
					}
					GraphicsUtil.switchToWidth(g, 1);
					g.setColor(COLOR_UNMATCHED);
					for (Location conn : result.getUnconnectedLocations()) {
						int connX = conn.getX();
						int connY = conn.getY();
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

			Graphics gBase = context.getGraphics();
			int w = right - left - 1;
			int h = bot - top - 1;
			if (w > 2 && h > 2) {
				gBase.setColor(BACKGROUND_RECT_SELECT);
				gBase.fillRect(left + 1, top + 1, w - 1, h - 1);
			}

			Circuit circ = canvas.getCircuit();
			Bounds bds = Bounds.create(left, top, right - left, bot - top);
			for (Component c : circ.getAllWithin(bds)) {
				Location cloc = c.getLocation();
				Graphics gDup = gBase.create();
				context.setGraphics(gDup);
				c.getFactory().drawGhost(context, COLOR_RECT_SELECT,
						cloc.getX(), cloc.getY(), c.getAttributeSet());
				gDup.dispose();
			}

			gBase.setColor(COLOR_RECT_SELECT);
			GraphicsUtil.switchToWidth(gBase, 2);
			if (w < 0)
				w = 0;
			if (h < 0)
				h = 0;
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
		return state == IDLE ? selectCursor
				: (state == RECT_SELECT ? rectSelectCursor : moveCursor);
	}

	@Override
	public String getDescription() {
		return Strings.get("selectToolDesc");
	}

	@Override
	public String getDisplayName() {
		return Strings.get("selectTool");
	}

	@Override
	public Set<Component> getHiddenComponents(Canvas canvas) {
		if (state == MOVING) {
			int dx = curDx;
			int dy = curDy;
			if (dx == 0 && dy == 0) {
				return null;
			}

			Set<Component> sel = canvas.getSelection().getComponents();
			MoveGesture gesture = moveGesture;
			if (gesture != null && drawConnections) {
				MoveResult result = gesture.findResult(dx, dy);
				if (result != null) {
					HashSet<Component> ret = new HashSet<Component>(sel);
					ret.addAll(result.getReplacementMap().getRemovals());
					return ret;
				}
			}
			return sel;
		} else {
			return null;
		}
	}

	@Override
	public String getName() {
		return "Select Tool";
	}

	private void handleMoveDrag(Canvas canvas, int dx, int dy, int modsEx) {
		boolean connect = shouldConnect(canvas, modsEx);
		drawConnections = connect;
		if (connect) {
			MoveGesture gesture = moveGesture;
			if (gesture == null) {
				gesture = new MoveGesture(new MoveRequestHandler(canvas),
						canvas.getCircuit(), canvas.getSelection()
								.getAnchoredComponents());
				moveGesture = gesture;
			}
			if (dx != 0 || dy != 0) {
				boolean queued = gesture.enqueueRequest(dx, dy);
				if (queued) {
					canvas.setErrorMessage(new ComputingMessage(dx, dy),
							COLOR_COMPUTING);
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
			SortedSet<Component> comps = AutoLabel.Sort(canvas.getProject().getSelection().getComponents()); 
			int KeybEvent = e.getKeyCode();
			boolean KeyTaken=false;
			for (Component comp : comps) {
				SetAttributeAction act = new SetAttributeAction(
						canvas.getCircuit(),
						Strings.getter("changeComponentAttributesAction"));
				KeyTaken |= GateKeyboardModifier.TookKeyboardStrokes(KeybEvent, comp , comp.getAttributeSet(), canvas,act,true);
				if (!act.isEmpty())
					canvas.getProject().doAction(act);
			}
			if (!KeyTaken) {
				for (Component comp : comps) {
					SetAttributeAction act = new SetAttributeAction(
							canvas.getCircuit(),
							Strings.getter("changeComponentAttributesAction"));
					KeyTaken |= AutoLabler.LabelKeyboardHandler(KeybEvent, comp.getAttributeSet(), comp.getFactory().getDisplayName(), comp,comp.getFactory(),canvas.getCircuit(),act,true);
					if (!act.isEmpty())
						canvas.getProject().doAction(act);
				}
			}
			if (!KeyTaken) switch (KeybEvent) {
			case KeyEvent.VK_BACK_SPACE:
			case KeyEvent.VK_DELETE:
				if (!canvas.getSelection().isEmpty()) {
					Action act = SelectionActions.clear(canvas.getSelection());
					canvas.getProject().doAction(act);
					e.consume();
				}
				break;
			default:
				processKeyEvent(canvas, e, KeyConfigurationEvent.KEY_PRESSED);
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
			Project proj = canvas.getProject();
			computeDxDy(proj, e, g);
			handleMoveDrag(canvas, curDx, curDy, e.getModifiersEx());
		} else if (state == RECT_SELECT) {
			Project proj = canvas.getProject();
			curDx = e.getX() - start.getX();
			curDy = e.getY() - start.getY();
			proj.repaintCanvas();
		}
	}

	@Override
	public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) {
		canvas.requestFocusInWindow();
	}

	@Override
	public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
		Project proj = canvas.getProject();
		Selection sel = proj.getSelection();
		Circuit circuit = canvas.getCircuit();
		start = Location.create(e.getX(), e.getY());
		curDx = 0;
		curDy = 0;
		moveGesture = null;

		// if the user clicks into the selection,
		// selection is being modified
		Collection<Component> in_sel = sel.getComponentsContaining(start, g);
		if (!in_sel.isEmpty()) {
			if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
				setState(proj, MOVING);
				proj.repaintCanvas();
				return;
			} else {
				Action act = SelectionActions.drop(sel, in_sel);
				if (act != null) {
					proj.doAction(act);
				}
			}
		}

		// if the user clicks into a component outside selection, user
		// wants to add/reset selection
		Collection<Component> clicked = circuit.getAllContaining(start, g);
		if (!clicked.isEmpty()) {
			if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
				if (sel.getComponentsContaining(start).isEmpty()) {
					Action act = SelectionActions.dropAll(sel);
					if (act != null) {
						proj.doAction(act);
					}
				}
			}
			for (Component comp : clicked) {
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
		if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
			Action act = SelectionActions.dropAll(sel);
			if (act != null) {
				proj.doAction(act);
			}
		}
		setState(proj, RECT_SELECT);
		proj.repaintCanvas();
	}

	@Override
	public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
		Project proj = canvas.getProject();
		if (state == MOVING) {
			setState(proj, IDLE);
			computeDxDy(proj, e, g);
			int dx = curDx;
			int dy = curDy;
			if (dx != 0 || dy != 0) {
				if (!proj.getLogisimFile().contains(canvas.getCircuit())) {
					canvas.setErrorMessage(Strings.getter("cannotModifyError"));
				} else if (proj.getSelection().hasConflictWhenMoved(dx, dy)) {
					canvas.setErrorMessage(Strings.getter("exclusiveError"));
				} else {
					boolean connect = shouldConnect(canvas, e.getModifiersEx());
					drawConnections = false;
					ReplacementMap repl;
					if (connect) {
						MoveGesture gesture = moveGesture;
						if (gesture == null) {
							gesture = new MoveGesture(new MoveRequestHandler(
									canvas), canvas.getCircuit(), canvas
									.getSelection().getAnchoredComponents());
						}
						canvas.setErrorMessage(new ComputingMessage(dx, dy),
								COLOR_COMPUTING);
						MoveResult result = gesture.forceRequest(dx, dy);
						clearCanvasMessage(canvas, dx, dy);
						repl = result.getReplacementMap();
					} else {
						repl = null;
					}
					Selection sel = proj.getSelection();
					proj.doAction(SelectionActions.translate(sel, dx, dy, repl));
				}
			}
			moveGesture = null;
			proj.repaintCanvas();
		} else if (state == RECT_SELECT) {
			Bounds bds = Bounds.create(start).add(start.getX() + curDx,
					start.getY() + curDy);
			Circuit circuit = canvas.getCircuit();
			Selection sel = proj.getSelection();
			Collection<Component> in_sel = sel.getComponentsWithin(bds, g);
			for (Component comp : circuit.getAllWithin(bds, g)) {
				if (!in_sel.contains(comp))
					sel.add(comp);
			}
			Action act = SelectionActions.drop(sel, in_sel);
			if (act != null) {
				proj.doAction(act);
			}
			setState(proj, IDLE);
			proj.repaintCanvas();
		}
		if (e.getClickCount()>=2) {
			Set<Component> comps = canvas.getProject().getSelection().getComponents();
			if (comps.size()==1) {
				for (Component comp : comps) {
					if (comp.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
                        String OldLabel = comp.getAttributeSet().getValue(StdAttr.LABEL);
    					SetAttributeAction act = new SetAttributeAction(
    							canvas.getCircuit(),
    							Strings.getter("changeComponentAttributesAction"));
    					AutoLabler.AskAndSetLabel(comp.getFactory().getDisplayName(),OldLabel,canvas.getCircuit(),comp,comp.getFactory(),comp.getAttributeSet(),act,true);
    					if (!act.isEmpty())
    						canvas.getProject().doAction(act);
					}
				}
			}
		}
	}

	@Override
	public void paintIcon(ComponentDrawContext c, int x, int y) {
		Graphics g = c.getGraphics();
		if (toolIcon != null) {
			toolIcon.paintIcon(c.getDestination(), g, x + 2, y + 2);
		} else {
			int[] xp = { x + 5, x + 5, x + 9, x + 12, x + 14, x + 11, x + 16 };
			int[] yp = { y, y + 17, y + 12, y + 18, y + 18, y + 12, y + 12 };
			g.setColor(java.awt.Color.black);
			g.fillPolygon(xp, yp, xp.length);
		}
	}

	private void processKeyEvent(Canvas canvas, KeyEvent e, int type) {
		HashMap<Component, KeyConfigurator> handlers = keyHandlers;
		if (handlers == null) {
			handlers = new HashMap<Component, KeyConfigurator>();
			Selection sel = canvas.getSelection();
			for (Component comp : sel.getComponents()) {
				ComponentFactory factory = comp.getFactory();
				AttributeSet attrs = comp.getAttributeSet();
				Object handler = factory.getFeature(KeyConfigurator.class,
						attrs);
				if (handler != null) {
					KeyConfigurator base = (KeyConfigurator) handler;
					handlers.put(comp, base.clone());
				}
			}
			keyHandlers = handlers;
		}

		if (!handlers.isEmpty()) {
			boolean consume = false;
			ArrayList<KeyConfigurationResult> results;
			results = new ArrayList<KeyConfigurationResult>();
			for (Map.Entry<Component, KeyConfigurator> entry : handlers
					.entrySet()) {
				Component comp = entry.getKey();
				KeyConfigurator handler = entry.getValue();
				KeyConfigurationEvent event = new KeyConfigurationEvent(type,
						comp.getAttributeSet(), e, comp);
				KeyConfigurationResult result = handler.keyEventReceived(event);
				consume |= event.isConsumed();
				if (result != null) {
					results.add(result);
				}
			}
			if (consume) {
				e.consume();
			}
			if (!results.isEmpty()) {
				SetAttributeAction act = new SetAttributeAction(
						canvas.getCircuit(),
						Strings.getter("changeComponentAttributesAction"));
				for (KeyConfigurationResult result : results) {
					Component comp = (Component) result.getEvent().getData();
					Map<Attribute<?>, Object> newValues = result
							.getAttributeValues();
					for (Map.Entry<Attribute<?>, Object> entry : newValues
							.entrySet()) {
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
		Selection sel = canvas.getSelection();
		if (!selectionsAdded.contains(sel)) {
			sel.addListener(selListener);
		}
	}

	private void setState(Project proj, int new_state) {
		if (state == new_state)
			return; // do nothing if state not new

		state = new_state;
		proj.getFrame().getCanvas().setCursor(getCursor());
	}

	private boolean shouldConnect(Canvas canvas, int modsEx) {
		boolean shiftReleased = (modsEx & MouseEvent.SHIFT_DOWN_MASK) == 0;
		boolean dflt = AppPreferences.MOVE_KEEP_CONNECT.getBoolean();
		if (shiftReleased) {
			return dflt;
		} else {
			return !dflt;
		}
	}
}
