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

package com.cburch.logisim.circuit;

import java.awt.Graphics;

import javax.swing.JPopupMenu;

import com.bfh.logisim.designrulecheck.Netlist;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.tools.WireRepair;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

public class Splitter extends ManagedComponent implements WireRepair,
		ToolTipMaker, MenuExtender, AttributeListener {
	private static void appendBuf(StringBuilder buf, int start, int end) {
		if (buf.length() > 0)
			buf.append(",");
		if (start == end) {
			buf.append(start);
		} else {
			buf.append(start + "-" + end);
		}
	}
	
	private boolean isMarked = false;

	public void SetMarked(boolean value) {
		isMarked = value;
	}
	
	public boolean isMarked() {
		return isMarked;
	}

	// basic data
	byte[] bit_thread; // how each bit maps to thread within end

	// derived data
	CircuitWires.SplitterData wire_data;

	public Splitter(Location loc, AttributeSet attrs) {
		super(loc, attrs, 3);
		configureComponent();
		attrs.addAttributeListener(this);
	}

	//
	// AttributeListener methods
	//
	public void attributeListChanged(AttributeEvent e) {
	}

	public void attributeValueChanged(AttributeEvent e) {
		configureComponent();
	}

	private synchronized void configureComponent() {
		SplitterAttributes attrs = (SplitterAttributes) getAttributeSet();
		SplitterParameters parms = attrs.getParameters();
		int fanout = attrs.fanout;
		byte[] bit_end = attrs.bit_end;

		// compute width of each end
		bit_thread = new byte[bit_end.length];
		byte[] end_width = new byte[fanout + 1];
		end_width[0] = (byte) bit_end.length;
		for (int i = 0; i < bit_end.length; i++) {
			byte thr = bit_end[i];
			if (thr > 0) {
				bit_thread[i] = end_width[thr];
				end_width[thr]++;
			} else {
				bit_thread[i] = -1;
			}
		}

		// compute end positions
		Location origin = getLocation();
		int x = origin.getX() + parms.getEnd0X();
		int y = origin.getY() + parms.getEnd0Y();
		int dx = parms.getEndToEndDeltaX();
		int dy = parms.getEndToEndDeltaY();

		EndData[] ends = new EndData[fanout + 1];
		ends[0] = new EndData(origin, BitWidth.create(bit_end.length),
				EndData.INPUT_OUTPUT);
		for (int i = 0; i < fanout; i++) {
			ends[i + 1] = new EndData(Location.create(x, y),
					BitWidth.create(end_width[i + 1]), EndData.INPUT_OUTPUT);
			x += dx;
			y += dy;
		}
		wire_data = new CircuitWires.SplitterData(fanout);
		setEnds(ends);
		recomputeBounds();
		fireComponentInvalidated(new ComponentEvent(this));
	}

	public void configureMenu(JPopupMenu menu, Project proj) {
		menu.addSeparator();
		menu.add(new SplitterDistributeItem(proj, this, 1));
		menu.add(new SplitterDistributeItem(proj, this, -1));
	}

	@Override
	public boolean contains(Location loc) {
		if (super.contains(loc)) {
			Location myLoc = getLocation();
			Direction facing = getAttributeSet().getValue(StdAttr.FACING);
			if (facing == Direction.EAST || facing == Direction.WEST) {
				return Math.abs(loc.getX() - myLoc.getX()) > 5
						|| loc.manhattanDistanceTo(myLoc) <= 5;
			} else {
				return Math.abs(loc.getY() - myLoc.getY()) > 5
						|| loc.manhattanDistanceTo(myLoc) <= 5;
			}
		} else {
			return false;
		}
	}

	//
	// user interface methods
	//
	public void draw(ComponentDrawContext context) {
		SplitterAttributes attrs = (SplitterAttributes) getAttributeSet();
		if (attrs.appear == SplitterAttributes.APPEAR_LEGACY) {
			SplitterPainter.drawLegacy(context, attrs, getLocation());
		} else {
			Location loc = getLocation();
			SplitterPainter.drawLines(context, attrs, loc);
			SplitterPainter.drawLabels(context, attrs, loc);
			context.drawPins(this);
		}
		if (isMarked) {
			Graphics g = context.getGraphics();
			Bounds bds = this.getBounds();
			g.setColor(Netlist.DRC_INSTANCE_MARK_COLOR);
			GraphicsUtil.switchToWidth(g, 2);
			g.drawRoundRect(bds.getX()-10, bds.getY()-10, bds.getWidth()+20, bds.getHeight()+20, 20, 20);
		}
	}

	public byte[] GetEndpoints() {
		SplitterAttributes attrs = (SplitterAttributes) getAttributeSet();
		return attrs.bit_end;
	}

	//
	// abstract ManagedComponent methods
	//
	@Override
	public ComponentFactory getFactory() {
		return SplitterFactory.instance;
	}

	@Override
	public Object getFeature(Object key) {
		if (key == WireRepair.class)
			return this;
		if (key == ToolTipMaker.class)
			return this;
		if (key == MenuExtender.class)
			return this;
		else
			return super.getFeature(key);
	}

	public String getToolTip(ComponentUserEvent e) {
		int end = -1;
		for (int i = getEnds().size() - 1; i >= 0; i--) {
			if (getEndLocation(i).manhattanDistanceTo(e.getX(), e.getY()) < 10) {
				end = i;
				break;
			}
		}

		if (end == 0) {
			return Strings.get("splitterCombinedTip");
		} else if (end > 0) {
			int bits = 0;
			StringBuilder buf = new StringBuilder();
			SplitterAttributes attrs = (SplitterAttributes) getAttributeSet();
			byte[] bit_end = attrs.bit_end;
			boolean inString = false;
			int beginString = 0;
			for (int i = 0; i < bit_end.length; i++) {
				if (bit_end[i] == end) {
					bits++;
					if (!inString) {
						inString = true;
						beginString = i;
					}
				} else {
					if (inString) {
						appendBuf(buf, i - 1, beginString);
						inString = false;
					}
				}
			}
			if (inString)
				appendBuf(buf, bit_end.length - 1, beginString);
			String base;
			switch (bits) {
			case 0:
				base = Strings.get("splitterSplit0Tip");
				break;
			case 1:
				base = Strings.get("splitterSplit1Tip");
				break;
			default:
				base = Strings.get("splitterSplitManyTip");
				break;
			}
			return StringUtil.format(base, buf.toString());
		} else {
			return null;
		}
	}

	@Override
	public void propagate(CircuitState state) {
		; // handled by CircuitWires, nothing to do
	}

	public boolean shouldRepairWire(WireRepairData data) {
		return true;
	}
}