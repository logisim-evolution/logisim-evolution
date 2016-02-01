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

package com.cburch.logisim.instance;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.comp.TextFieldEvent;
import com.cburch.logisim.comp.TextFieldListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.SetAttributeAction;
import com.cburch.logisim.tools.TextEditable;

public class InstanceTextField implements AttributeListener, TextFieldListener,
		TextEditable {
	private Canvas canvas;
	private InstanceComponent comp;
	private TextField field;
	private Attribute<String> labelAttr;
	private Attribute<Font> fontAttr;
	private boolean LabelIsVisable = true;
	private Color fontColor;
	private int fieldX;
	private int fieldY;
	private int halign;
	private int valign;

	InstanceTextField(InstanceComponent comp) {
		this.comp = comp;
		this.field = null;
		this.labelAttr = null;
		this.fontAttr = null;
		fontColor = StdAttr.DEFAULT_LABEL_COLOR;
	}

	public void attributeListChanged(AttributeEvent e) {
	}
	
	public void attributeValueChanged(AttributeEvent e) {
		Attribute<?> attr = e.getAttribute();
		if (attr == labelAttr) {
			updateField(comp.getAttributeSet());
		} else if (attr == fontAttr) {
			if (field != null)
				field.setFont((Font) e.getValue());
		} else if (attr == StdAttr.LABEL_COLOR) {
			fontColor = (Color) e.getValue(); 
		} else if (attr == StdAttr.LABEL_VISIBILITY) {
			LabelIsVisable = (Boolean) e.getValue();
		}
	}

	private void createField(AttributeSet attrs, String text) {
		Font font = attrs.getValue(fontAttr);
		field = new TextField(fieldX, fieldY, halign, valign, font);
		field.setText(text);
		field.addTextFieldListener(this);
	}

	void draw(Component comp, ComponentDrawContext context) {
		if (field != null&&LabelIsVisable) {
			Graphics g = context.getGraphics().create();
			Color currentColor = g.getColor();
			g.setColor(fontColor);
			field.draw(g);
			g.setColor(currentColor);
			g.dispose();
		}
	}

	Bounds getBounds(Graphics g) {
		return field == null || !LabelIsVisable ? Bounds.EMPTY_BOUNDS : field.getBounds(g);
	}

	public Action getCommitAction(Circuit circuit, String oldText,
			String newText) {
		SetAttributeAction act = new SetAttributeAction(circuit,
				Strings.getter("changeLabelAction"));
		act.set(comp, labelAttr, newText);
		return act;
	}

	public Caret getTextCaret(ComponentUserEvent event) {
		canvas = event.getCanvas();
		Graphics g = canvas.getGraphics();

		// if field is absent, create it empty
		// and if it is empty, just return a caret at its beginning
		if (field == null)
			createField(comp.getAttributeSet(), "");
		String text = field.getText();
		if (text == null || text.equals(""))
			return field.getCaret(g, 0);

		Bounds bds = field.getBounds(g);
		if (bds.getWidth() < 4 || bds.getHeight() < 4) {
			Location loc = comp.getLocation();
			bds = bds.add(Bounds.create(loc).expand(2));
		}

		int x = event.getX();
		int y = event.getY();
		if (bds.contains(x, y))
			return field.getCaret(g, x, y);
		else
			return null;
	}

	private boolean shouldRegister() {
		return labelAttr != null || fontAttr != null;
	}

	public void textChanged(TextFieldEvent e) {
		String prev = e.getOldText();
		String next = e.getText();
		if (!next.equals(prev)) {
			comp.getAttributeSet().setValue(labelAttr, next);
		}
	}

	void update(Attribute<String> labelAttr, Attribute<Font> fontAttr, int x,
			int y, int halign, int valign) {
		boolean wasReg = shouldRegister();
		this.labelAttr = labelAttr;
		this.fontAttr = fontAttr;
		this.fieldX = x;
		this.fieldY = y;
		this.halign = halign;
		this.valign = valign;
		boolean shouldReg = shouldRegister();
		AttributeSet attrs = comp.getAttributeSet();
		if (attrs.containsAttribute(StdAttr.LABEL_VISIBILITY))
			LabelIsVisable = attrs.getValue(StdAttr.LABEL_VISIBILITY);
		if (!wasReg && shouldReg)
			attrs.addAttributeListener(this);
		if (wasReg && !shouldReg)
			attrs.removeAttributeListener(this);

		updateField(attrs);
	}

	private void updateField(AttributeSet attrs) {
		String text = attrs.getValue(labelAttr);
		if (text == null || text.equals("")) {
			if (field != null) {
				field.removeTextFieldListener(this);
				field = null;
			}
		} else {
			if (field == null) {
				createField(attrs, text);
			} else {
				Font font = attrs.getValue(fontAttr);
				if (font != null)
					field.setFont(font);
				field.setLocation(fieldX, fieldY, halign, valign);
				field.setText(text);
			}
		}
	}
}
