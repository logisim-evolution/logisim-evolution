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

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.base.Text;

public class TextTool extends Tool {
	private class MyListener implements CaretListener, CircuitListener {
		public void circuitChanged(CircuitEvent event) {
			if (event.getCircuit() != caretCircuit) {
				event.getCircuit().removeCircuitListener(this);
				return;
			}
			int action = event.getAction();
			if (action == CircuitEvent.ACTION_REMOVE) {
				if (event.getData() == caretComponent) {
					caret.cancelEditing();
				}
			} else if (action == CircuitEvent.ACTION_CLEAR) {
				if (caretComponent != null) {
					caret.cancelEditing();
				}
			}
		}

		public void editingCanceled(CaretEvent e) {
			if (e.getCaret() != caret) {
				e.getCaret().removeCaretListener(this);
				return;
			}
			caret.removeCaretListener(this);
			caretCircuit.removeCircuitListener(this);

			caretCircuit = null;
			caretComponent = null;
			caretCreatingText = false;
			caret = null;
		}

		public void editingStopped(CaretEvent e) {
			if (e.getCaret() != caret) {
				e.getCaret().removeCaretListener(this);
				return;
			}
			caret.removeCaretListener(this);
			caretCircuit.removeCircuitListener(this);

			String val = caret.getText();
			boolean isEmpty = (val == null || val.equals(""));
			Action a;
			Project proj = caretCanvas.getProject();
			if (caretCreatingText) {
				if (!isEmpty) {
					CircuitMutation xn = new CircuitMutation(caretCircuit);
					xn.add(caretComponent);
					a = xn.toAction(Strings.getter("addComponentAction",
							Text.FACTORY.getDisplayGetter()));
				} else {
					a = null; // don't add the blank text field
				}
			} else {
				if (isEmpty && caretComponent.getFactory() instanceof Text) {
					CircuitMutation xn = new CircuitMutation(caretCircuit);
					xn.add(caretComponent);
					a = xn.toAction(Strings.getter("removeComponentAction",
							Text.FACTORY.getDisplayGetter()));
				} else {
					Object obj = caretComponent.getFeature(TextEditable.class);
					if (obj == null) { // should never happen
						a = null;
					} else {
						TextEditable editable = (TextEditable) obj;
						a = editable.getCommitAction(caretCircuit,
								e.getOldText(), e.getText());
					}
				}
			}

			caretCircuit = null;
			caretComponent = null;
			caretCreatingText = false;
			caret = null;

			if (a != null)
				proj.doAction(a);
		}
	}

	private static Cursor cursor = Cursor
			.getPredefinedCursor(Cursor.TEXT_CURSOR);

	private MyListener listener = new MyListener();
	private AttributeSet attrs;
	private Caret caret = null;
	private boolean caretCreatingText = false;
	private Canvas caretCanvas = null;
	private Circuit caretCircuit = null;
	private Component caretComponent = null;

	public TextTool() {
		attrs = Text.FACTORY.createAttributeSet();
	}

	@Override
	public void deselect(Canvas canvas) {
		if (caret != null) {
			caret.stopEditing();
			caret = null;
		}
	}

	@Override
	public void draw(Canvas canvas, ComponentDrawContext context) {
		if (caret != null)
			caret.draw(context.getGraphics());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TextTool;
	}

	@Override
	public AttributeSet getAttributeSet() {
		return attrs;
	}

	@Override
	public Cursor getCursor() {
		return cursor;
	}

	@Override
	public String getDescription() {
		return Strings.get("textToolDesc");
	}

	@Override
	public String getDisplayName() {
		return Strings.get("textTool");
	}

	@Override
	public String getName() {
		return "Text Tool";
	}

	@Override
	public int hashCode() {
		return TextTool.class.hashCode();
	}

	@Override
	public void keyPressed(Canvas canvas, KeyEvent e) {
		if (caret != null) {
			caret.keyPressed(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void keyReleased(Canvas canvas, KeyEvent e) {
		if (caret != null) {
			caret.keyReleased(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void keyTyped(Canvas canvas, KeyEvent e) {
		if (caret != null) {
			caret.keyTyped(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
		// TODO: enhance label editing
	}

	@Override
	public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
		Project proj = canvas.getProject();
		Circuit circ = canvas.getCircuit();

		/*
		 * This is made to remove an annoying bug that do not unselect current selection
		 */
		Action act = SelectionActions.dropAll(canvas.getSelection());
		canvas.getProject().doAction(act);

		if (!proj.getLogisimFile().contains(circ)) {
			if (caret != null)
				caret.cancelEditing();
			canvas.setErrorMessage(Strings.getter("cannotModifyError"));
			return;
		}

		// Maybe user is clicking within the current caret.
		if (caret != null) {
			if (caret.getBounds(g).contains(e.getX(), e.getY())) { // Yes
				caret.mousePressed(e);
				proj.repaintCanvas();
				return;
			} else { // No. End the current caret.
				caret.stopEditing();
			}
		}
		// caret will be null at this point

		// Otherwise search for a new caret.
		int x = e.getX();
		int y = e.getY();
		Location loc = Location.create(x, y);
		ComponentUserEvent event = new ComponentUserEvent(canvas, x, y);

		// First search in selection.
		for (Component comp : proj.getSelection().getComponentsContaining(loc,
				g)) {
			TextEditable editable = (TextEditable) comp
					.getFeature(TextEditable.class);
			if (editable != null) {
				caret = editable.getTextCaret(event);
				if (caret != null) {
					proj.getFrame().viewComponentAttributes(circ, comp);
					caretComponent = comp;
					caretCreatingText = false;
					break;
				}
			}
		}

		// Then search in circuit
		if (caret == null) {
			for (Component comp : circ.getAllContaining(loc, g)) {
				TextEditable editable = (TextEditable) comp
						.getFeature(TextEditable.class);
				if (editable != null) {
					caret = editable.getTextCaret(event);
					if (caret != null) {
						proj.getFrame().viewComponentAttributes(circ, comp);
						caretComponent = comp;
						caretCreatingText = false;
						break;
					}
				}
			}
		}

		// if nothing found, create a new label
		if (caret == null) {
			if (loc.getX() < 0 || loc.getY() < 0)
				return;
			AttributeSet copy = (AttributeSet) attrs.clone();
			caretComponent = Text.FACTORY.createComponent(loc, copy);
			caretCreatingText = true;
			TextEditable editable = (TextEditable) caretComponent
					.getFeature(TextEditable.class);
			if (editable != null) {
				caret = editable.getTextCaret(event);
				proj.getFrame().viewComponentAttributes(circ, caretComponent);
			}
		}

		if (caret != null) {
			caretCanvas = canvas;
			caretCircuit = canvas.getCircuit();
			caret.addCaretListener(listener);
			caretCircuit.addCircuitListener(listener);
		}
		proj.repaintCanvas();
	}

	@Override
	public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
		// TODO: enhance label editing
	}

	@Override
	public void paintIcon(ComponentDrawContext c, int x, int y) {
		Text.FACTORY.paintIcon(c, x, y, null);
	}
}
