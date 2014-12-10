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

package com.cburch.draw.tools;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.actions.ModelEditTextAction;
import com.cburch.draw.actions.ModelRemoveAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Text;
import com.cburch.draw.util.EditableLabelField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.Icons;

public class TextTool extends AbstractTool {
	private class CancelListener extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			cancelText(curCanvas);
		}
	}

	private class FieldListener extends AbstractAction implements
			AttributeListener {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			commitText(curCanvas);
		}

		public void attributeListChanged(AttributeEvent e) {
			Text cur = curText;
			if (cur != null) {
				double zoom = curCanvas.getZoomFactor();
				cur.getLabel().configureTextField(field, zoom);
				curCanvas.repaint();
			}
		}

		public void attributeValueChanged(AttributeEvent e) {
			attributeListChanged(e);
		}
	}

	private DrawingAttributeSet attrs;
	private EditableLabelField field;
	private FieldListener fieldListener;

	private Text curText;
	private Canvas curCanvas;
	private boolean isTextNew;

	public TextTool(DrawingAttributeSet attrs) {
		this.attrs = attrs;
		curText = null;
		isTextNew = false;
		field = new EditableLabelField();

		fieldListener = new FieldListener();
		InputMap fieldInput = field.getInputMap();
		fieldInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "commit");
		fieldInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
		ActionMap fieldAction = field.getActionMap();
		fieldAction.put("commit", fieldListener);
		fieldAction.put("cancel", new CancelListener());
	}

	private void cancelText(Canvas canvas) {
		Text cur = curText;
		if (cur != null) {
			curText = null;
			cur.removeAttributeListener(fieldListener);
			canvas.remove(field);
			canvas.getSelection().clearSelected();
			canvas.repaint();
		}
	}

	private void commitText(Canvas canvas) {
		Text cur = curText;
		boolean isNew = isTextNew;
		String newText = field.getText();
		if (cur == null) {
			return;
		}
		cancelText(canvas);

		if (isNew) {
			if (!newText.equals("")) {
				cur.setText(newText);
				canvas.doAction(new ModelAddAction(canvas.getModel(), cur));
			}
		} else {
			String oldText = cur.getText();
			if (newText.equals("")) {
				canvas.doAction(new ModelRemoveAction(canvas.getModel(), cur));
			} else if (!oldText.equals(newText)) {
				canvas.doAction(new ModelEditTextAction(canvas.getModel(), cur,
						newText));
			}
		}
	}

	@Override
	public void draw(Canvas canvas, Graphics g) {
		; // actually, there's nothing to do here - it's handled by the field
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return DrawAttr.ATTRS_TEXT_TOOL;
	}

	@Override
	public Cursor getCursor(Canvas canvas) {
		return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
	}

	@Override
	public Icon getIcon() {
		return Icons.getIcon("text.gif");
	}

	@Override
	public void mousePressed(Canvas canvas, MouseEvent e) {
		if (curText != null) {
			commitText(canvas);
		}

		Text clicked = null;
		boolean found = false;
		int mx = e.getX();
		int my = e.getY();
		Location mloc = Location.create(mx, my);
		for (CanvasObject o : canvas.getModel().getObjectsFromTop()) {
			if (o instanceof Text && o.contains(mloc, true)) {
				clicked = (Text) o;
				found = true;
				break;
			}
		}
		if (!found) {
			clicked = attrs.applyTo(new Text(mx, my, ""));
		}

		curText = clicked;
		curCanvas = canvas;
		isTextNew = !found;
		clicked.getLabel().configureTextField(field, canvas.getZoomFactor());
		field.setText(clicked.getText());
		canvas.add(field);

		Point fieldLoc = field.getLocation();
		double zoom = canvas.getZoomFactor();
		fieldLoc.x = (int) Math.round(mx * zoom - fieldLoc.x);
		fieldLoc.y = (int) Math.round(my * zoom - fieldLoc.y);
		int caret = field.viewToModel(fieldLoc);
		if (caret >= 0) {
			field.setCaretPosition(caret);
		}
		field.requestFocus();

		canvas.getSelection().setSelected(clicked, true);
		canvas.getSelection().setHidden(Collections.singleton(clicked), true);
		clicked.addAttributeListener(fieldListener);
		canvas.repaint();
	}

	@Override
	public void toolDeselected(Canvas canvas) {
		commitText(canvas);
	}

	@Override
	public void toolSelected(Canvas canvas) {
		cancelText(canvas);
	}

	@Override
	public void zoomFactorChanged(Canvas canvas) {
		Text t = curText;
		if (t != null) {
			t.getLabel().configureTextField(field, canvas.getZoomFactor());
		}
	}
}
