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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.bfh.logisim.hdlgenerator.CircuitHDLGeneratorFactory;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Text;
import com.cburch.logisim.circuit.appear.CircuitAppearance;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

public class SubcircuitFactory extends InstanceFactory {
	private class CircuitFeature implements StringGetter, MenuExtender,
			ActionListener {
		private Instance instance;
		private Project proj;

		public CircuitFeature(Instance instance) {
			this.instance = instance;
		}

		public void actionPerformed(ActionEvent e) {
			CircuitState superState = proj.getCircuitState();
			if (superState == null)
				return;

			CircuitState subState = getSubstate(superState, instance);
			if (subState == null)
				return;
			proj.setCircuitState(subState);
		}

		public void configureMenu(JPopupMenu menu, Project proj) {
			this.proj = proj;
			String name = instance.getFactory().getDisplayName();
			String text = Strings.get("subcircuitViewItem", name);
			JMenuItem item = new JMenuItem(text);
			item.addActionListener(this);
			menu.add(item);
		}

		public String toString() {
			return source.getName();
		}
	}

	private Circuit source;

	public SubcircuitFactory(Circuit source) {
		super("", null);
		this.source = source;
		setFacingAttribute(StdAttr.FACING);
		setDefaultToolTip(new CircuitFeature(null));
		setInstancePoker(SubcircuitPoker.class);
	}

	void computePorts(Instance instance) {
		Direction facing = instance.getAttributeValue(StdAttr.FACING);
		Map<Location, Instance> portLocs = source.getAppearance()
				.getPortOffsets(facing);
		Port[] ports = new Port[portLocs.size()];
		Instance[] pins = new Instance[portLocs.size()];
		int i = -1;
		for (Map.Entry<Location, Instance> portLoc : portLocs.entrySet()) {
			i++;
			Location loc = portLoc.getKey();
			Instance pin = portLoc.getValue();
			String type = Pin.FACTORY.isInputPin(pin) ? Port.INPUT
					: Port.OUTPUT;
			BitWidth width = pin.getAttributeValue(StdAttr.WIDTH);
			ports[i] = new Port(loc.getX(), loc.getY(), type, width);
			pins[i] = pin;

			String label = pin.getAttributeValue(StdAttr.LABEL);
			if (label != null && label.length() > 0) {
				ports[i].setToolTip(StringUtil.constantGetter(label));
			}
		}

		CircuitAttributes attrs = (CircuitAttributes) instance
				.getAttributeSet();
		attrs.setPinInstances(pins);
		instance.setPorts(ports);
		instance.recomputeBounds();
		configureLabel(instance); // since this affects the circuit's bounds
	}

	private void configureLabel(Instance instance) {
		Bounds bds = instance.getBounds();
		Direction loc = instance
				.getAttributeValue(CircuitAttributes.LABEL_LOCATION_ATTR);

		int x = bds.getX() + bds.getWidth() / 2;
		int y = bds.getY() + bds.getHeight() / 2;
		int ha = GraphicsUtil.H_CENTER;
		int va = GraphicsUtil.V_CENTER;
		if (loc == Direction.EAST) {
			x = bds.getX() + bds.getWidth() + 2;
			ha = GraphicsUtil.H_LEFT;
		} else if (loc == Direction.WEST) {
			x = bds.getX() - 2;
			ha = GraphicsUtil.H_RIGHT;
		} else if (loc == Direction.SOUTH) {
			y = bds.getY() + bds.getHeight() + 2;
			va = GraphicsUtil.V_TOP;
		} else {
			y = bds.getY() - 2;
			va = GraphicsUtil.V_BASELINE;
		}
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, ha, va);
	}

	//
	// methods for configuring instances
	//
	@Override
	public void configureNewInstance(Instance instance) {
		CircuitAttributes attrs = (CircuitAttributes) instance
				.getAttributeSet();
		attrs.setSubcircuit(instance);

		instance.addAttributeListener();
		computePorts(instance);
		// configureLabel(instance); already done in computePorts
	}

	/**
	 * Code taken from Cornell's version of Logisim:
	 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
	 */
	@Override
	public boolean contains(Location loc, AttributeSet attrs) {
		if (super.contains(loc, attrs)) {
			Direction facing = attrs.getValue(StdAttr.FACING);
			Direction defaultFacing = source.getAppearance().getFacing();
			Location query;

			if (facing.equals(defaultFacing)) {
				query = loc;
			} else {
				query = loc.rotate(facing, defaultFacing, 0, 0);
			}

			return source.getAppearance().contains(query);
		} else {
			return false;
		}
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new CircuitAttributes(source);
	}
	/**
	 * This function is in charge of displaying a subcircuit,
	 * 	and to take care of rotating the strings accordingly.
	 * @param painter the painter gotten from paint event
	 * @param bds			border limits
	 * @param facing		Direction in case of rotation
	 * @param defaultFacing Default direction when element was
	 * 						created
	 */
	private void drawCircuitName(InstancePainter painter, Bounds bds,
		Direction facing, Direction defaultFacing) {
		String label = source.getName();
		Map<Direction, List<Instance>> edge;
		int x;
		int y;
		int y_label = 0;
		
		/* This is made to count the pins on each side */
		edge = new HashMap<Direction, List<Instance>>();
		edge.put(Direction.EAST, new ArrayList<Instance>());
		edge.put(Direction.WEST, new ArrayList<Instance>());

		Collection<Instance> pins = source.getAppearance().GetCircuitPin().getPins();

		
		if (!pins.isEmpty()) {
			for (Instance pin : pins) {
				Direction pinEdge;
				if (pin.getAttributeValue(Pin.ATTR_TYPE)) {
					pinEdge=Direction.EAST;
				}
				else {
					pinEdge=Direction.WEST;
				}
				List<Instance> e = edge.get(pinEdge);
				e.add(pin);
			}
		}
		
		int maxNumber = Math.max(edge.get(Direction.EAST).size(),
				edge.get(Direction.WEST).size());
		maxNumber--;
		
		if (label != null && !label.equals("")) {
			Direction up = Direction.NORTH;
			Font font = DrawAttr.DEFAULT_NAME_FONT;
			Graphics g = painter.getGraphics().create();
			FontMetrics m = g.getFontMetrics(font);

			double angle;
			
			if (facing == Direction.WEST) {
				angle = Math.PI / 2
					- (up.toRadians() - defaultFacing.toRadians());
				x = bds.getX() + bds.getWidth() / 2;
				y = bds.getY() + m.getAscent() / 2 ;
				y_label = y ;
				
				g.fillRect(bds.getX() + CircuitAppearance.PINLENGTH , bds.getY(),
						   bds.getWidth() - 2 * CircuitAppearance.PINLENGTH, m.getHeight()
						   );
			} else {
				angle = Math.PI / 2
						- (up.toRadians() - defaultFacing.toRadians())
						- facing.toRadians();
				
				if ( facing == Direction.NORTH) {
					x = bds.getX() + bds.getWidth() / 2 + m.getAscent();
					y = bds.getY() + bds.getHeight() / 2;
					/* Get position regarding the box itself meaning that when rotated the x\y axis follows */
					y_label = y + maxNumber *  m.getAscent();
					
					g.fillRect(bds.getX() + bds.getWidth() - m.getHeight() - 1, bds.getY() + CircuitAppearance.PINLENGTH,
							 m.getHeight(), bds.getHeight() - 2 * CircuitAppearance.PINLENGTH
							   );
				} else if ( facing == Direction.EAST) {
					x = bds.getX() + bds.getWidth() / 2;
					y = bds.getY() + bds.getHeight() - m.getAscent();
					/* Get position regarding the box itself meaning that when rotated the  x\y axis follows */
					y_label = y;
					
					g.fillRect(bds.getX() + CircuitAppearance.PINLENGTH, bds.getY() + bds.getHeight() -  m.getHeight() ,
							 bds.getWidth() - 2 * CircuitAppearance.PINLENGTH, m.getHeight()
							   );
				} else {
					x = bds.getX() + bds.getWidth() / 2 - m.getAscent();
					y = bds.getY() + bds.getHeight() /2;
					
					y_label = y + maxNumber *  m.getAscent();

					g.fillRect(bds.getX() + 1 , bds.getY() + CircuitAppearance.PINLENGTH,
							 m.getHeight(), bds.getHeight() - 2 * CircuitAppearance.PINLENGTH
							   );
				}
			}
			
			if (g instanceof Graphics2D && Math.abs(angle) > 0.01) {
				Graphics2D g2 = (Graphics2D) g;
				g2.rotate(angle, x, y);
			}

			g.setFont(font);
			GraphicsUtil.drawCenteredColoredText(g, label, Color.WHITE, Color.BLACK, x, y_label);
			g.dispose();
		}
	}
	
	/**
	 * This function will be in charge of placing the name of the pin and rotate them
	 * 		accordingly avoid the names being upside down
	 * @param painter
	 * @param edge
	 * @param x
	 * @param y
	 * @param dx
	 * @param dy
	 * @param LeftSide
	 * @param angle
	 */
    private static void placePins(InstancePainter painter, Map<Direction, List<Instance>> edge,
			int x, int y, int width, int height, double angle, Direction facing) {
		/* Here is checking which side of the circuit has
		 * the most pins
		 */
		int ldx = 0;
		int ldy = 0;
		int maxVert = Math.max(edge.get(Direction.EAST).size(),
								 edge.get(Direction.WEST).size());
		List<Instance> pins_west = edge.get(Direction.WEST);
		List<Instance> pins_east = edge.get(Direction.EAST);
		Font pins_font = DrawAttr.DEFAULT_FIXED_PICH_FONT;

		
		for (int i = 0; i < maxVert ; i++) {
			/* Draw East side pins */
			if (pins_east.size() > i) {
				Instance pin_east = pins_east.get(i);
				if (pin_east.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
					Graphics g1 = painter.getGraphics().create();
					FontMetrics m = g1.getFontMetrics(pins_font);

					if (Direction.WEST == facing) {
						ldx = 20;
						ldy = - (i* (m.getHeight() + 5) - 2);
					}
					else {
						ldx = - m.stringWidth(pin_east.getAttributeValue(StdAttr.LABEL)) -20;
						ldy = (i* (m.getHeight() + 5) + 5);
					}
					
					((Graphics2D) g1).rotate(angle);
					((Graphics2D) g1).setFont(pins_font);
					((Graphics2D) g1).drawString( pin_east.getAttributeValue(StdAttr.LABEL),
							 ldx, ldy);
							
					g1.dispose();
				}
			}
			
			/* Draw West side pins */
			if (pins_west.size() > i) {
				Instance pin_west = pins_west.get(i);
				if (pin_west.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
					Graphics g1 = painter.getGraphics().create();
					FontMetrics m = g1.getFontMetrics(pins_font);
					((Graphics2D) g1).setFont(pins_font);
					((Graphics2D) g1).rotate(angle);
					
					if (Direction.SOUTH == facing) {
						ldx = -height + 20;
						ldy = (i* (m.getHeight() + 5) + 5);
					}
					else if (Direction.WEST == facing) {
						ldx = width - m.stringWidth(pin_west.getAttributeValue(StdAttr.LABEL)) -20;
						ldy = - (i* (m.getHeight() + 5) - 2);
					}
					else if (Direction.EAST == facing) {
						ldx = -width + 20;
						ldy = (i* (m.getHeight() + 5) + 5);
					}
					else if (Direction.NORTH == facing) {
						ldx = -height + 20;
						ldy = (i* (m.getHeight() + 5) + 5);
					}
					
					((Graphics2D) g1).drawString(pin_west.getAttributeValue(StdAttr.LABEL),
							ldx, ldy);
					
					g1.dispose();
				}
			}
		}
	}

	private void drawPinsName(InstancePainter painter, Bounds bds,
			Direction facing, Direction defaultFacing) {
		Map<Direction, List<Instance>> edge;
		Collection<Instance> pins = source.getAppearance().GetCircuitPin().getPins();
		edge = new HashMap<Direction, List<Instance>>();
		edge.put(Direction.EAST, new ArrayList<Instance>());
		edge.put(Direction.WEST, new ArrayList<Instance>());
		int MaxLeftLabelLength = 0;
		int MaxRightLabelLength = 0;
		double angle;

		for (Instance pin : pins) {
			Direction pinEdge;
			Text label = new Text(0,0,pin.getAttributeValue(StdAttr.LABEL));
			int LabelWidth = label.getText().length()*DrawAttr.FixedFontCharWidth;
			if (pin.getAttributeValue(Pin.ATTR_TYPE)) {
				pinEdge=Direction.EAST;
				if (LabelWidth>MaxRightLabelLength)
					MaxRightLabelLength = LabelWidth;
			}
			else {
				pinEdge=Direction.WEST;
				if (LabelWidth>MaxLeftLabelLength)
					MaxLeftLabelLength = LabelWidth;
			}
			List<Instance> e = edge.get(pinEdge);
			e.add(pin);
		}
		
		for (Map.Entry<Direction, List<Instance>> entry : edge.entrySet()) {
			source.getAppearance().sortPinsList(entry.getValue(), entry.getKey());
		}

		
		/* Here we manage to have always the beginning of the pin label
		 * close to the pin and avoid the label being upside down.
		 */
		angle = facing.toRadians();
		angle = Math.toRadians(-Math.toDegrees(angle)  % Math.toDegrees(Math.PI) );

		placePins(painter, edge, bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), angle, facing);
	}

	private void drawCircuitLabel(InstancePainter painter, Bounds bds,
		Direction facing, Direction defaultFacing) {
		AttributeSet staticAttrs = source.getStaticAttributes();

		String label = staticAttrs
				.getValue(CircuitAttributes.CIRCUIT_LABEL_ATTR);
		
		if (label != null && !label.equals("")) {
			Direction up = staticAttrs
					.getValue(CircuitAttributes.CIRCUIT_LABEL_FACING_ATTR);
			Font font = staticAttrs
					.getValue(CircuitAttributes.CIRCUIT_LABEL_FONT_ATTR);

			int back = label.indexOf('\\');
			int lines = 1;
			boolean backs = false;
			while (back >= 0 && back <= label.length() - 2) {
				char c = label.charAt(back + 1);
				if (c == 'n')
					lines++;
				else if (c == '\\')
					backs = true;
				back = label.indexOf('\\', back + 2);
			}

			int x = bds.getX() + bds.getWidth() / 2;
			int y = bds.getY() + bds.getHeight() / 2;
			Graphics g = painter.getGraphics().create();
			double angle = Math.PI / 2
					- (up.toRadians() - defaultFacing.toRadians())
					- facing.toRadians();
			if (g instanceof Graphics2D && Math.abs(angle) > 0.01) {
				Graphics2D g2 = (Graphics2D) g;
				g2.rotate(angle, x, y);
			}
			g.setFont(font);
			if (lines == 1 && !backs) {
				GraphicsUtil.drawCenteredText(g, label, x, y);
			} else {
				FontMetrics fm = g.getFontMetrics();
				int height = fm.getHeight();
				y = y - (height * lines - fm.getLeading()) / 2 + fm.getAscent();
				back = label.indexOf('\\');
				while (back >= 0 && back <= label.length() - 2) {
					char c = label.charAt(back + 1);
					if (c == 'n') {
						String line = label.substring(0, back);
						GraphicsUtil.drawText(g, line, x, y,
								GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);
						y += height;
						label = label.substring(back + 2);
						back = label.indexOf('\\');
					} else if (c == '\\') {
						label = label.substring(0, back)
								+ label.substring(back + 1);
						back = label.indexOf('\\', back + 1);
					} else {
						back = label.indexOf('\\', back + 2);
					}
				}
				GraphicsUtil.drawText(g, label, x, y, GraphicsUtil.H_CENTER,
						GraphicsUtil.V_BASELINE);
			}
			g.dispose();
		}
	}

	@Override
	public StringGetter getDisplayGetter() {
		return StringUtil.constantGetter(source.getName());
	}

	@Override
	public Object getInstanceFeature(Instance instance, Object key) {
		if (key == MenuExtender.class)
			return new CircuitFeature(instance);
		return super.getInstanceFeature(instance, key);
	}

	@Override
	public String getName() {
		return source.getName();
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		Direction defaultFacing = source.getAppearance().getFacing();
		Bounds bds = source.getAppearance().getOffsetBounds();
		return bds.rotate(defaultFacing, facing, 0, 0);
	}

	public Circuit getSubcircuit() {
		return source;
	}
	
	public void setSubcircuit(Circuit sub) {
		source = sub;
	}

	public CircuitState getSubstate(CircuitState superState, Component comp) {
		return getSubstate(createInstanceState(superState, comp));
	}

	//
	// propagation-oriented methods
	//
	public CircuitState getSubstate(CircuitState superState, Instance instance) {
		return getSubstate(createInstanceState(superState, instance));
	}

	private CircuitState getSubstate(InstanceState instanceState) {
		CircuitState subState = (CircuitState) instanceState.getData();
		if (subState == null) {
			subState = new CircuitState(instanceState.getProject(), source);
			instanceState.setData(subState);
			instanceState.fireInvalidated();
		}
		return subState;
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new CircuitHDLGeneratorFactory(this.source);
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	public void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			computePorts(instance);
		} else if (attr == CircuitAttributes.LABEL_LOCATION_ATTR) {
			configureLabel(instance);
		}
	}

	private void paintBase(InstancePainter painter, Graphics g) {
		CircuitAttributes attrs = (CircuitAttributes) painter.getAttributeSet();
		Direction facing = attrs.getFacing();
		Direction defaultFacing = source.getAppearance().getFacing();
		Location loc = painter.getLocation();
		g.translate(loc.getX(), loc.getY());
		source.getAppearance().paintSubcircuit(g, facing);
		drawCircuitLabel(painter, getOffsetBounds(attrs), facing, defaultFacing);
		g.translate(-loc.getX(), -loc.getY());
		painter.drawLabel();
	}

	//
	// user interface features
	//
	@Override
	public void paintGhost(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		Color fg = g.getColor();
		int v = fg.getRed() + fg.getGreen() + fg.getBlue();
		Composite oldComposite = null;
		if (g instanceof Graphics2D && v > 50) {
			oldComposite = ((Graphics2D) g).getComposite();
			Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.5f);
			((Graphics2D) g).setComposite(c);
		}
		paintBase(painter, g);
		if (oldComposite != null) {
			((Graphics2D) g).setComposite(oldComposite);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		paintBase(painter, painter.getGraphics());
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState superState) {
		CircuitState subState = getSubstate(superState);

		CircuitAttributes attrs = (CircuitAttributes) superState
				.getAttributeSet();
		Instance[] pins = attrs.getPinInstances();
		for (int i = 0; i < pins.length; i++) {
			Instance pin = pins[i];
			InstanceState pinState = subState.getInstanceState(pin);
			if (Pin.FACTORY.isInputPin(pin)) {
				Value newVal = superState.getPortValue(i);
				Value oldVal = Pin.FACTORY.getValue(pinState);
				if (!newVal.equals(oldVal)) {
					Pin.FACTORY.setValue(pinState, newVal);
					Pin.FACTORY.propagate(pinState);
				}
			} else { // it is output-only
				Value val = pinState.getPortValue(0);
				superState.setPort(i, val, 1);
			}
		}
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}

	/*
	 * TODO public String getToolTip(ComponentUserEvent e) { return
	 * StringUtil.format(Strings.get("subcircuitCircuitTip"),
	 * source.getDisplayName()); }
	 */
}
