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

package com.cburch.draw.shapes;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.draw.util.EditableLabel;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;

public class Text extends AbstractCanvasObject {
	private EditableLabel label;

	private Text(int x, int y, int halign, int valign, String text, Font font,
			Color color) {
		label = new EditableLabel(x, y, text, font);
		label.setColor(color);
		label.setHorizontalAlignment(halign);
		label.setVerticalAlignment(valign);
	}

	public Text(int x, int y, String text) {
		this(x, y, EditableLabel.LEFT, EditableLabel.BASELINE, text,
				DrawAttr.DEFAULT_FONT, Color.BLACK);
	}

	@Override
	public Text clone() {
		Text ret = (Text) super.clone();
		ret.label = this.label.clone();
		return ret;
	}

	@Override
	public boolean contains(Location loc, boolean assumeFilled) {
		return label.contains(loc.getX(), loc.getY());
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return DrawAttr.ATTRS_TEXT;
	}

	@Override
	public Bounds getBounds() {
		return label.getBounds();
	}

	@Override
	public String getDisplayName() {
		return Strings.get("shapeText");
	}

	public List<Handle> getHandles() {
		Bounds bds = label.getBounds();
		int x = bds.getX();
		int y = bds.getY();
		int w = bds.getWidth();
		int h = bds.getHeight();
		return UnmodifiableList.create(new Handle[] { new Handle(this, x, y),
				new Handle(this, x + w, y), new Handle(this, x + w, y + h),
				new Handle(this, x, y + h) });
	}

	@Override
	public List<Handle> getHandles(HandleGesture gesture) {
		return getHandles();
	}

	public EditableLabel getLabel() {
		return label;
	}

	public Location getLocation() {
		return Location.create(label.getX(), label.getY());
	}

	public String getText() {
		return label.getText();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		if (attr == DrawAttr.FONT) {
			return (V) label.getFont();
		} else if (attr == DrawAttr.FILL_COLOR) {
			return (V) label.getColor();
		} else if (attr == DrawAttr.ALIGNMENT) {
			int halign = label.getHorizontalAlignment();
			AttributeOption h;
			if (halign == EditableLabel.LEFT) {
				h = DrawAttr.ALIGN_LEFT;
			} else if (halign == EditableLabel.RIGHT) {
				h = DrawAttr.ALIGN_RIGHT;
			} else {
				h = DrawAttr.ALIGN_CENTER;
			}
			return (V) h;
		} else {
			return null;
		}
	}

	@Override
	public boolean matches(CanvasObject other) {
		if (other instanceof Text) {
			Text that = (Text) other;
			return this.label.equals(that.label);
		} else {
			return false;
		}
	}

	@Override
	public int matchesHashCode() {
		return label.hashCode();
	}

	@Override
	public void paint(Graphics g, HandleGesture gesture) {
		label.paint(g);
	}

	public void setText(String value) {
		label.setText(value);
	}

	@Override
	public Element toSvgElement(Document doc) {
		return SvgCreator.createText(doc, this);
	}

	@Override
	public void translate(int dx, int dy) {
		label.setLocation(label.getX() + dx, label.getY() + dy);
	}

	@Override
	public void updateValue(Attribute<?> attr, Object value) {
		if (attr == DrawAttr.FONT) {
			label.setFont((Font) value);
		} else if (attr == DrawAttr.FILL_COLOR) {
			label.setColor((Color) value);
		} else if (attr == DrawAttr.ALIGNMENT) {
			Integer intVal = (Integer) ((AttributeOption) value).getValue();
			label.setHorizontalAlignment(intVal.intValue());
		}
	}
}
