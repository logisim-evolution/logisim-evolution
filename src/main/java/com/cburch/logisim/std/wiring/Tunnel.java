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

package com.cburch.logisim.std.wiring;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Tunnel extends InstanceFactory {
	public static final Tunnel FACTORY = new Tunnel();

	static final int MARGIN = 3;
	static final int ARROW_MARGIN = 5;
	static final int ARROW_DEPTH = 4;
	static final int ARROW_MIN_WIDTH = 16;
	static final int ARROW_MAX_WIDTH = 20;

	public Tunnel() {
		super("Tunnel", Strings.getter("tunnelComponent"));
		setIconName("tunnel.gif");
		setFacingAttribute(StdAttr.FACING);
		setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
	}

	private Bounds computeBounds(TunnelAttributes attrs, int textWidth,
			int textHeight, Graphics g, String label) {
		int x = attrs.getLabelX();
		int y = attrs.getLabelY();
		int halign = attrs.getLabelHAlign();
		int valign = attrs.getLabelVAlign();

		int minDim = ARROW_MIN_WIDTH - 2 * MARGIN;
		int bw = Math.max(minDim, textWidth);
		int bh = Math.max(minDim, textHeight);
		int bx;
		int by;
		switch (halign) {
		case TextField.H_LEFT:
			bx = x;
			break;
		case TextField.H_RIGHT:
			bx = x - bw;
			break;
		default:
			bx = x - (bw / 2);
		}
		switch (valign) {
		case TextField.V_TOP:
			by = y;
			break;
		case TextField.V_BOTTOM:
			by = y - bh;
			break;
		default:
			by = y - (bh / 2);
		}

		if (g != null) {
			GraphicsUtil.drawText(g, label, bx + bw / 2, by + bh / 2,
					GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
		}

		return Bounds.create(bx, by, bw, bh).expand(MARGIN).add(0, 0);
	}

	//
	// private methods
	//
	private void configureLabel(Instance instance) {
		TunnelAttributes attrs = (TunnelAttributes) instance.getAttributeSet();
		Location loc = instance.getLocation();
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, loc.getX()
				+ attrs.getLabelX(), loc.getY() + attrs.getLabelY(),
				attrs.getLabelHAlign(), attrs.getLabelVAlign());
	}

	//
	// methods for instances
	//
	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		instance.setPorts(new Port[] { new Port(0, 0, Port.INOUT, StdAttr.WIDTH) });
		configureLabel(instance);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new TunnelAttributes();
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrsBase) {
		TunnelAttributes attrs = (TunnelAttributes) attrsBase;
		Bounds bds = attrs.getOffsetBounds();
		if (bds != null) {
			return bds;
		} else {
			int ht = attrs.getFont().getSize();
			int wd = ht * attrs.getLabel().length() / 2;
			bds = computeBounds(attrs, wd, ht, null, "");
			attrs.setOffsetBounds(bds);
			return bds;
		}
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		return true;
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			configureLabel(instance);
			instance.recomputeBounds();
		} else if (attr == StdAttr.LABEL || attr == StdAttr.LABEL_FONT) {
			instance.recomputeBounds();
		}
	}

	//
	// graphics methods
	//
	@Override
	public void paintGhost(InstancePainter painter) {
		TunnelAttributes attrs = (TunnelAttributes) painter.getAttributeSet();
		Direction facing = attrs.getFacing();
		String label = attrs.getLabel();

		Graphics g = painter.getGraphics();
		g.setFont(attrs.getFont());
		FontMetrics fm = g.getFontMetrics();
		Bounds bds = computeBounds(attrs, fm.stringWidth(label), fm.getAscent()
				+ fm.getDescent(), g, label);
		if (attrs.setOffsetBounds(bds)) {
			Instance instance = painter.getInstance();
			if (instance != null)
				instance.recomputeBounds();
		}

		int x0 = bds.getX();
		int y0 = bds.getY();
		int x1 = x0 + bds.getWidth();
		int y1 = y0 + bds.getHeight();
		int mw = ARROW_MAX_WIDTH / 2;
		int[] xp;
		int[] yp;
		if (facing == Direction.NORTH) {
			int yb = y0 + ARROW_DEPTH;
			if (x1 - x0 <= ARROW_MAX_WIDTH) {
				xp = new int[] { x0, 0, x1, x1, x0 };
				yp = new int[] { yb, y0, yb, y1, y1 };
			} else {
				xp = new int[] { x0, -mw, 0, mw, x1, x1, x0 };
				yp = new int[] { yb, yb, y0, yb, yb, y1, y1 };
			}
		} else if (facing == Direction.SOUTH) {
			int yb = y1 - ARROW_DEPTH;
			if (x1 - x0 <= ARROW_MAX_WIDTH) {
				xp = new int[] { x0, x1, x1, 0, x0 };
				yp = new int[] { y0, y0, yb, y1, yb };
			} else {
				xp = new int[] { x0, x1, x1, mw, 0, -mw, x0 };
				yp = new int[] { y0, y0, yb, yb, y1, yb, yb };
			}
		} else if (facing == Direction.EAST) {
			int xb = x1 - ARROW_DEPTH;
			if (y1 - y0 <= ARROW_MAX_WIDTH) {
				xp = new int[] { x0, xb, x1, xb, x0 };
				yp = new int[] { y0, y0, 0, y1, y1 };
			} else {
				xp = new int[] { x0, xb, xb, x1, xb, xb, x0 };
				yp = new int[] { y0, y0, -mw, 0, mw, y1, y1 };
			}
		} else {
			int xb = x0 + ARROW_DEPTH;
			if (y1 - y0 <= ARROW_MAX_WIDTH) {
				xp = new int[] { xb, x1, x1, xb, x0 };
				yp = new int[] { y0, y0, y1, y1, 0 };
			} else {
				xp = new int[] { xb, x1, x1, xb, xb, x0, xb };
				yp = new int[] { y0, y0, y1, y1, mw, 0, -mw };
			}
		}
		GraphicsUtil.switchToWidth(g, 2);
		g.drawPolygon(xp, yp, xp.length);
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
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		; // nothing to do - handled by circuit
	}

}