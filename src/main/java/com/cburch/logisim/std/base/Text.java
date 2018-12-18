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

package com.cburch.logisim.std.base;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;

public class Text extends InstanceFactory {
	public static Attribute<String> ATTR_TEXT = Attributes.forString("text",
			Strings.getter("textTextAttr"));
	public static Attribute<Font> ATTR_FONT = Attributes.forFont("font",
			Strings.getter("textFontAttr"));
	public static Attribute<AttributeOption> ATTR_HALIGN = Attributes
			.forOption(
					"halign",
					Strings.getter("textHorzAlignAttr"),
					new AttributeOption[] {
							new AttributeOption(Integer
									.valueOf(TextField.H_LEFT), "left", Strings
									.getter("textHorzAlignLeftOpt")),
							new AttributeOption(Integer
									.valueOf(TextField.H_RIGHT), "right",
									Strings.getter("textHorzAlignRightOpt")),
							new AttributeOption(Integer
									.valueOf(TextField.H_CENTER), "center",
									Strings.getter("textHorzAlignCenterOpt")), });
	public static Attribute<AttributeOption> ATTR_VALIGN = Attributes
			.forOption(
					"valign",
					Strings.getter("textVertAlignAttr"),
					new AttributeOption[] {
							new AttributeOption(Integer
									.valueOf(TextField.V_TOP), "top", Strings
									.getter("textVertAlignTopOpt")),
							new AttributeOption(Integer
									.valueOf(TextField.V_BASELINE), "base",
									Strings.getter("textVertAlignBaseOpt")),
							new AttributeOption(Integer
									.valueOf(TextField.V_BOTTOM), "bottom",
									Strings.getter("textVertAlignBottomOpt")),
							new AttributeOption(Integer
									.valueOf(TextField.H_CENTER), "center",
									Strings.getter("textVertAlignCenterOpt")), });

	public static final Text FACTORY = new Text();

	private Text() {
		super("Text", Strings.getter("textComponent"));
		setIconName("text.gif");
		setShouldSnap(false);
	}

	private void configureLabel(Instance instance) {
		TextAttributes attrs = (TextAttributes) instance.getAttributeSet();
		Location loc = instance.getLocation();
		instance.setTextField(ATTR_TEXT, ATTR_FONT, loc.getX(), loc.getY(),
				attrs.getHorizontalAlign(), attrs.getVerticalAlign());
	}

	//
	// methods for instances
	//
	@Override
	protected void configureNewInstance(Instance instance) {
		configureLabel(instance);
		instance.addAttributeListener();
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new TextAttributes();
	}

	private Bounds estimateBounds(TextAttributes attrs) {
		// TODO - you can imagine being more clever here
		String text = attrs.getText();
		if (text == null || text.length() == 0)
			return Bounds.EMPTY_BOUNDS;
		int size = attrs.getFont().getSize();
		int h = size;
		int w = size * text.length() / 2;
		int ha = attrs.getHorizontalAlign();
		int va = attrs.getVerticalAlign();
		int x;
		int y;
		if (ha == TextField.H_LEFT) {
			x = 0;
		} else if (ha == TextField.H_RIGHT) {
			x = -w;
		} else {
			x = -w / 2;
		}
		if (va == TextField.V_TOP) {
			y = 0;
		} else if (va == TextField.V_CENTER) {
			y = -h / 2;
		} else {
			y = -h;
		}
		return Bounds.create(x, y, w, h);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrsBase) {
		TextAttributes attrs = (TextAttributes) attrsBase;
		String text = attrs.getText();
		if (text == null || text.equals("")) {
			return Bounds.EMPTY_BOUNDS;
		} else {
			Bounds bds = attrs.getOffsetBounds();
			if (bds == null) {
				bds = estimateBounds(attrs);
				attrs.setOffsetBounds(bds);
			}
			return bds == null ? Bounds.EMPTY_BOUNDS : bds;
		}
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		return true;
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == ATTR_HALIGN || attr == ATTR_VALIGN) {
			configureLabel(instance);
		}
	}

	//
	// graphics methods
	//
	@Override
	public void paintGhost(InstancePainter painter) {
		TextAttributes attrs = (TextAttributes) painter.getAttributeSet();
		String text = attrs.getText();
		if (text == null || text.equals(""))
			return;

		int halign = attrs.getHorizontalAlign();
		int valign = attrs.getVerticalAlign();
		Graphics g = painter.getGraphics();
		Font old = g.getFont();
		g.setFont(attrs.getFont());
		GraphicsUtil.drawText(g, text, 0, 0, halign, valign);

		String textTrim = text.endsWith(" ") ? text.substring(0,
				text.length() - 1) : text;
		Bounds newBds;
		if (textTrim.equals("")) {
			newBds = Bounds.EMPTY_BOUNDS;
		} else {
			Rectangle bdsOut = GraphicsUtil.getTextBounds(g, textTrim, 0, 0,
					halign, valign);
			newBds = Bounds.create(bdsOut).expand(4);
		}
		if (attrs.setOffsetBounds(newBds)) {
			Instance instance = painter.getInstance();
			if (instance != null)
				instance.recomputeBounds();
		}

		g.setFont(old);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Location loc = painter.getLocation();
		int x = loc.getX();
		int y = loc.getY();
		Graphics g = painter.getGraphics();
		g.translate(x, y);
		g.setColor(Color.BLACK);
		paintGhost(painter);
		g.translate(-x, -y);
	}

	@Override
	public void propagate(InstanceState state) {
	}
}
