package com.ita.logisim.ttl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
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
import com.cburch.logisim.util.GraphicsUtil;

public abstract class AbstractTtlGate extends InstanceFactory {

	protected static final int pinwidth = 10, pinheight = 7;
	private int height = 60;
	protected byte pinnumber;
	private String name;
	private byte ngatestodraw = 0;
	protected String[] portnames = null;
	private HashSet<Byte> outputports = new HashSet<Byte>();
	private HashSet<Byte> unusedpins  = new HashSet<Byte>();

	/**
	 * @param name
	 *            = name to display in the center of the TTl
	 * @param pins
	 *            = the total number of pins (GND and VCC included)
	 * @param outputports
	 *            = an array with the indexes of the output ports (indexes are the
	 *            same you can find on Google searching the TTL you want to add)
	 **/
	protected AbstractTtlGate(String name, byte pins, byte[] outputports) {
		super(name);
		setIconName("ttl.gif");
		setAttributes(
				new Attribute[] { StdAttr.FACING, TTL.VCC_GND, TTL.DRAW_INTERNAL_STRUCTURE, StdAttr.LABEL },
				new Object[] { Direction.EAST, false, false, "" });
		setFacingAttribute(StdAttr.FACING);
		this.name = name;
		this.pinnumber = pins;
		for (int i = 0 ; i < outputports.length ; i++)
			this.outputports.add(outputports[i]);
	}

	protected AbstractTtlGate(String name, byte pins, byte[] outputports, byte[] NotUsedPins) {
		this(name, pins, outputports);
		if (NotUsedPins == null)
			return;
		for (int i = 0; i < NotUsedPins.length ; i++)
			unusedpins.add(NotUsedPins[i]);
	}

	/**
	 * @param name
	 *            = name to display in the center of the TTl
	 * @param pins
	 *            = the total number of pins (GND and VCC included)
	 * @param outputports
	 *            = an array with the indexes of the output ports (indexes are the
	 *            same you can find on Google searching the TTL you want to add)
	 * @param drawgates
	 *            = if true, it calls the paintInternal method many times as the
	 *            number of output ports passing the coordinates
	 **/
	protected AbstractTtlGate(String name, byte pins, byte[] outputports, boolean drawgates) {
		this(name, pins, outputports);
		this.ngatestodraw = (byte) (drawgates ? outputports.length : 0);
	}

	/**
	 * @param name
	 *            = name to display in the center of the TTl
	 * @param pins
	 *            = the total number of pins (GND and VCC included)
	 * @param outputports
	 *            = an array with the indexes of the output ports (indexes are the
	 *            same you can find on Google searching the TTL you want to add)
	 * @param Ttlportnames
	 *            = an array of strings which will be tooltips of the corresponding
	 *            port in the order you pass
	 **/
	protected AbstractTtlGate(String name, byte pins, byte[] outputports, String[] Ttlportnames) {
		// the ttl name, the total number of pins and an array with the indexes of
		// output ports (indexes are the one you can find on Google), an array of
		// strings which will be tooltips of the corresponding port in order
		this(name, pins, outputports);
		this.portnames = Ttlportnames;
	}
	
	protected AbstractTtlGate(String name, byte pins, byte[] outputports, byte[] NotUsedPins, String[] Ttlportnames) {
		this(name, pins, outputports);
		portnames = Ttlportnames;
		if (NotUsedPins == null)
			return;
		for (int i = 0; i < NotUsedPins.length ; i++)
			unusedpins.add(NotUsedPins[i]);
	}

	protected AbstractTtlGate(String name, byte pins, byte[] outputports, String[] Ttlportnames,int height) {
		// the ttl name, the total number of pins and an array with the indexes of
		// output ports (indexes are the one you can find on Google), an array of
		// strings which will be tooltips of the corresponding port in order
		this(name, pins, outputports);
		this.height = height;
		this.portnames = Ttlportnames;
	}
	

	private void computeTextField(Instance instance) {
		Bounds bds = instance.getBounds();
		Direction dir = instance.getAttributeValue(StdAttr.FACING);
		if (dir == Direction.EAST || dir == Direction.WEST)
			instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, 
					bds.getX() + bds.getWidth() + 3, bds.getY() + bds.getHeight() / 2, GraphicsUtil.H_LEFT,
					GraphicsUtil.V_CENTER_OVERALL);
		else
			instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT,
					bds.getX() + bds.getWidth() / 2, bds.getY() - 3, GraphicsUtil.H_CENTER,
					GraphicsUtil.V_CENTER_OVERALL);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		updateports(instance);
		computeTextField(instance);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction dir = attrs.getValue(StdAttr.FACING);
		return Bounds.create(0, -30, this.pinnumber * 10, height).rotate(Direction.EAST, dir, 0, 0);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			instance.recomputeBounds();
			updateports(instance);
			computeTextField(instance);
		} else if (attr == TTL.VCC_GND) {
			updateports(instance);
		}
	}
	
	static Point TTLGetTranslatedXY(InstanceState state, MouseEvent e) {
		int x=0,y=0;
		Location loc = state.getInstance().getLocation();
		int height = state.getInstance().getBounds().getHeight();
		int width = state.getInstance().getBounds().getWidth();
		Direction dir = state.getAttributeValue(StdAttr.FACING);
		if (dir.equals(Direction.EAST)) {
			x = e.getX()-loc.getX();
			y = e.getY()+30-loc.getY();
		} else if (dir.equals(Direction.WEST)) {
			x = loc.getX()-e.getX();
			y = height-(e.getY()+(height-30)-loc.getY());
		} else if (dir.equals(Direction.NORTH)) {
			x = loc.getY()-e.getY();
			y = width-(loc.getX()+(width-30)-e.getX());
		} else {
			x = e.getY()-loc.getY();
			y = (loc.getX()+30-e.getX());
		}
		return new Point(x,y);
	}

	protected void paintBase(InstancePainter painter, boolean drawname, boolean ghost) {
		Direction dir = painter.getAttributeValue(StdAttr.FACING);
		Graphics2D g = (Graphics2D) painter.getGraphics();
		Bounds bds = painter.getBounds();
		int x = bds.getX();
		int y = bds.getY();
		int xp = x, yp = y;
		int width = bds.getWidth();
		int height = bds.getHeight();
		for (byte i = 0; i < this.pinnumber; i++) {
			if (i < this.pinnumber / 2) {
				if (dir == Direction.WEST || dir == Direction.EAST)
					xp = i * 20 + (10 - pinwidth / 2) + x;
				else
					yp = i * 20 + (10 - pinwidth / 2) + y;
			} else {
				if (dir == Direction.WEST || dir == Direction.EAST) {
					xp = (i - this.pinnumber / 2) * 20 + (10 - pinwidth / 2) + x;
					yp = height + y - pinheight;
				} else {
					yp = (i - this.pinnumber / 2) * 20 + (10 - pinwidth / 2) + y;
					xp = width + x - pinheight;
				}
			}
			if (dir == Direction.WEST || dir == Direction.EAST) {
				// fill the background of white if selected from preferences
				g.drawRect(xp, yp, pinwidth, pinheight);
			} else {
				// fill the background of white if selected from preferences
				g.drawRect(xp, yp, pinheight, pinwidth);
			}
		}
		if (dir == Direction.SOUTH) {
			// fill the background of white if selected from preferences
			g.drawRoundRect(x + pinheight, y, bds.getWidth() - pinheight * 2, bds.getHeight(), 10, 10);
			g.drawArc(x + width / 2 - 7, y - 7, 14, 14, 180, 180);
		} else if (dir == Direction.WEST) {
			// fill the background of white if selected from preferences
			g.drawRoundRect(x, y + pinheight, bds.getWidth(), bds.getHeight() - pinheight * 2, 10, 10);
			g.drawArc(x + width - 7, y + height / 2 - 7, 14, 14, 90, 180);
		} else if (dir == Direction.NORTH) {
			// fill the background of white if selected from preferences
			g.drawRoundRect(x + pinheight, y, bds.getWidth() - pinheight * 2, bds.getHeight(), 10, 10);
			g.drawArc(x + width / 2 - 7, y + height - 7, 14, 14, 0, 180);
		} else {// east
			// fill the background of white if selected from preferences
			g.drawRoundRect(x, y + pinheight, bds.getWidth(), bds.getHeight() - pinheight * 2, 10, 10);
			g.drawArc(x - 7, y + height / 2 - 7, 14, 14, 270, 180);
		}
		g.rotate(Math.toRadians(-dir.toDegrees()), x + width / 2, y + height / 2);
		if (drawname) {
			g.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 14));
			GraphicsUtil.drawCenteredText(g, this.name, x + bds.getWidth() / 2, y + bds.getHeight() / 2 - 4);
		}
		if (dir == Direction.WEST || dir == Direction.EAST) {
			xp = x;
			yp = y;
		} else {
			xp = x + (width - height) / 2;
			yp = y + (height - width) / 2;
			width = bds.getHeight();
			height = bds.getWidth();
		}
		g.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 7));
		GraphicsUtil.drawCenteredText(g, "Vcc", xp + 10, yp + pinheight + 4);
		GraphicsUtil.drawCenteredText(g, "GND", xp + width - 10, yp + height - pinheight - 7);
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		paintBase(painter, true, true);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		painter.drawPorts();
		Graphics2D g = (Graphics2D) painter.getGraphics();
		painter.drawLabel();
		if (!painter.getAttributeValue(TTL.DRAW_INTERNAL_STRUCTURE)) {
			Direction dir = painter.getAttributeValue(StdAttr.FACING);
			Bounds bds = painter.getBounds();
			int x = bds.getX();
			int y = bds.getY();
			int xp = x, yp = y;
			int width = bds.getWidth();
			int height = bds.getHeight();
			for (byte i = 0; i < this.pinnumber; i++) {
				if (i == this.pinnumber / 2) {
					xp = x;
					yp = y;
					if (dir == Direction.WEST || dir == Direction.EAST) {
						g.setColor(Color.DARK_GRAY.darker());
						g.fillRoundRect(xp, yp + pinheight, width, height - pinheight * 2 + 2, 10, 10);
						g.setColor(Color.DARK_GRAY);
						g.fillRoundRect(xp, yp + pinheight, width, height - pinheight * 2 - 2, 10, 10);
						g.setColor(Color.BLACK);
						g.drawRoundRect(xp, yp + pinheight, width, height - pinheight * 2 - 2, 10, 10);
						g.drawRoundRect(xp, yp + pinheight, width, height - pinheight * 2 + 2, 10, 10);
					} else {
						g.setColor(Color.DARK_GRAY.darker());
						g.fillRoundRect(xp + pinheight, yp, width - pinheight * 2, height, 10, 10);
						g.setColor(Color.DARK_GRAY);
						g.fillRoundRect(xp + pinheight, yp, width - pinheight * 2, height - 4, 10, 10);
						g.setColor(Color.BLACK);
						g.drawRoundRect(xp + pinheight, yp, width - pinheight * 2, height - 4, 10, 10);
						g.drawRoundRect(xp + pinheight, yp, width - pinheight * 2, height, 10, 10);
					}
					if (dir == Direction.SOUTH)
						g.fillArc(xp + width / 2 - 7, yp - 7, 14, 14, 180, 180);
					else if (dir == Direction.WEST)
						g.fillArc(xp + width - 7, yp + height / 2 - 7, 14, 14, 90, 180);
					else if (dir == Direction.NORTH)
						g.fillArc(xp + width / 2 - 7, yp + height - 11, 14, 14, 0, 180);
					else // east
						g.fillArc(xp - 7, yp + height / 2 - 7, 14, 14, 270, 180);
				}
				if (i < this.pinnumber / 2) {
					if (dir == Direction.WEST || dir == Direction.EAST)
						xp = i * 20 + (10 - pinwidth / 2) + x;
					else
						yp = i * 20 + (10 - pinwidth / 2) + y;
				} else {
					if (dir == Direction.WEST || dir == Direction.EAST) {
						xp = (i - this.pinnumber / 2) * 20 + (10 - pinwidth / 2) + x;
						yp = height + y - pinheight;
					} else {
						yp = (i - this.pinnumber / 2) * 20 + (10 - pinwidth / 2) + y;
						xp = width + x - pinheight;
					}
				}
				if (dir == Direction.WEST || dir == Direction.EAST) {
					g.setColor(Color.LIGHT_GRAY);
					g.fillRect(xp, yp, pinwidth, pinheight);
					g.setColor(Color.BLACK);
					g.drawRect(xp, yp, pinwidth, pinheight);
				} else {
					g.setColor(Color.LIGHT_GRAY);
					g.fillRect(xp, yp, pinheight, pinwidth);
					g.setColor(Color.BLACK);
					g.drawRect(xp, yp, pinheight, pinwidth);
				}
			}

			g.setColor(Color.LIGHT_GRAY.brighter());
			g.rotate(Math.toRadians(-dir.toDegrees()), x + width / 2, y + height / 2);
			g.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 14));
			GraphicsUtil.drawCenteredText(g, this.name, x + width / 2, y + height / 2 - 4);
			g.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 7));
			if (dir == Direction.WEST || dir == Direction.EAST) {
				xp = x;
				yp = y;
			} else {
				xp = x + (width - height) / 2;
				yp = y + (height - width) / 2;
			}
			if (dir == Direction.SOUTH) {
				GraphicsUtil.drawCenteredText(g, "Vcc", xp + 10, yp + pinheight + 4);
				GraphicsUtil.drawCenteredText(g, "GND", xp + height - 14, yp + width - pinheight - 8);
			} else if (dir == Direction.WEST) {
				GraphicsUtil.drawCenteredText(g, "Vcc", xp + 10, yp + pinheight + 6);
				GraphicsUtil.drawCenteredText(g, "GND", xp + width - 10, yp + height - pinheight - 8);
			} else if (dir == Direction.NORTH) {
				GraphicsUtil.drawCenteredText(g, "Vcc", xp + 14, yp + pinheight + 4);
				GraphicsUtil.drawCenteredText(g, "GND", xp + height - 10, yp + width - pinheight - 8);
			} else { // east
				GraphicsUtil.drawCenteredText(g, "Vcc", xp + 10, yp + pinheight + 4);
				GraphicsUtil.drawCenteredText(g, "GND", xp + width - 10, yp + height - pinheight - 10);
			}
		} else
			paintInternalBase(painter);
	}

	/**
	 * @param painter
	 *            = the instance painter you have to use to create Graphics
	 *            (Graphics g = painter.getGraphics())
	 * @param x
	 *            = if drawgates is false or not used, the component's left side; if
	 *            drawgates is true it gets the component's width, subtracts 20 (for
	 *            GND or Vcc) and divides for the number of outputs for each side,
	 *            you'll get the x coordinate of the leftmost input -10 before the
	 *            last output
	 * @param y
	 *            = the component's upper side
	 * @param height
	 *            = the component's height
	 * @param up
	 *            = true if drawgates is true when drawing the gates in the upper
	 *            side (introduced this because can't draw upside down so you have
	 *            to write what to draw if down and up)
	 **/
	abstract public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up);

	private void paintInternalBase(InstancePainter painter) {
		Direction dir = painter.getAttributeValue(StdAttr.FACING);
		Bounds bds = painter.getBounds();
		int x = bds.getX();
		int y = bds.getY();
		int width = bds.getWidth();
		int height = bds.getHeight();
		if (dir == Direction.SOUTH || dir == Direction.NORTH) {
			x += (width - height) / 2;
			y += (height - width) / 2;
			width = bds.getHeight();
			height = bds.getWidth();
		}

		if (this.ngatestodraw == 0)
			paintInternal(painter, x, y, height, false);
		else {
			paintBase(painter, false, false);
			for (byte i = 0; i < this.ngatestodraw; i++) {
				paintInternal(painter,
						x + (i < this.ngatestodraw / 2 ? i : i - this.ngatestodraw / 2)
								* ((width - 20) / (this.ngatestodraw / 2)) + (i < this.ngatestodraw / 2 ? 0 : 20),
						y, height, i >= this.ngatestodraw / 2);
			}
		}
	}

	/**
	 * Here you have to write the logic of your component
	 **/
	@Override
	public void propagate(InstanceState state) {
		int NrOfUnusedPins = unusedpins.size();
		if (state.getAttributeValue(TTL.VCC_GND) && (state.getPortValue(this.pinnumber - 2 - NrOfUnusedPins) != Value.FALSE
				|| state.getPortValue(this.pinnumber - 1 - NrOfUnusedPins) != Value.TRUE)) {
			int port = 0;
			for (byte i = 1; i <= pinnumber; i++) {
				if (!unusedpins.contains(i)&&(i != (pinnumber/2))) {
					if (outputports.contains(i))
						state.setPort(port, Value.UNKNOWN, 1);
					port++;
				}
			}
		} else
			ttlpropagate(state);
	}

	abstract public void ttlpropagate(InstanceState state);

	private void updateports(Instance instance) {
		Bounds bds = instance.getBounds();
		Direction dir = instance.getAttributeValue(StdAttr.FACING);
		int dx = 0, dy = 0, width = bds.getWidth(), height = bds.getHeight();
		byte portindex = 0;
		boolean isoutput = false, hasvccgnd = instance.getAttributeValue(TTL.VCC_GND);
		boolean skip = false;
		int NrOfUnusedPins = unusedpins.size();
		/*
		 * array port is composed in this order: lower ports less GND, upper ports less
		 * Vcc, GND, Vcc
		 */
		Port[] ps = new Port[hasvccgnd ? this.pinnumber-NrOfUnusedPins : this.pinnumber - 2-NrOfUnusedPins];

		for (byte i = 0; i < this.pinnumber; i++) {
			isoutput = outputports.contains((byte) (i+1));
			skip = unusedpins.contains((byte) (i+1));
			// set the position
			if (i < this.pinnumber / 2) {
				if (dir == Direction.EAST) {
					dx = i * 20 + 10;
					dy = height-30;
				} else if (dir == Direction.WEST) {
					dx = -10 - 20 * i;
					dy = 30-height;
				} else if (dir == Direction.NORTH) {
					dx = width-30;
					dy = -10 - 20 * i;
				} else {// SOUTH
					dx = 30-width;
					dy = i * 20 + 10;
				}
			} else {
				if (dir == Direction.EAST) {
					dx = width - (i - this.pinnumber / 2) * 20 - 10;
					dy = -30;
				} else if (dir == Direction.WEST) {
					dx = -width + (i - this.pinnumber / 2) * 20 + 10;
					dy = 30;
				} else if (dir == Direction.NORTH) {
					dx = -30;
					dy = -height + (i - this.pinnumber / 2) * 20 + 10;
				} else {// SOUTH
					dx = 30;
					dy = height - (i - this.pinnumber / 2) * 20 - 10;
				}
			}
			// Set the port (output/input)
			if (skip) {
				portindex--;
			} else if (isoutput) {// output port
				ps[portindex] = new Port(dx, dy, Port.OUTPUT, 1);
				if (this.portnames == null || this.portnames.length <= portindex)
					ps[portindex].setToolTip(Strings.getter("demultiplexerOutTip", ": " + String.valueOf(i + 1)));
				else
					ps[portindex].setToolTip(Strings.getter("demultiplexerOutTip",
							String.valueOf(i + 1) + ": " + this.portnames[portindex]));
			} else {// input port
				if (hasvccgnd && i == this.pinnumber - 1) { // Vcc
					ps[ps.length - 1] = new Port(dx, dy, Port.INPUT, 1);
					ps[ps.length - 1].setToolTip(Strings.getter("Vcc: " + this.pinnumber));
				} else if (i == this.pinnumber / 2 - 1) {// GND
					if (hasvccgnd) {
						ps[ps.length - 2] = new Port(dx, dy, Port.INPUT, 1);
						ps[ps.length - 2].setToolTip(Strings.getter("GND: " + this.pinnumber / 2));
					}
					portindex--;
				} else if (i != this.pinnumber - 1 && i != this.pinnumber / 2 - 1) {// normal output
					ps[portindex] = new Port(dx, dy, Port.INPUT, 1);
					if (this.portnames == null || this.portnames.length <= portindex)
						ps[portindex].setToolTip(Strings.getter("multiplexerInTip", ": " + String.valueOf(i + 1)));
					else
						ps[portindex].setToolTip(Strings.getter("multiplexerInTip",
								String.valueOf(i + 1) + ": " + this.portnames[portindex]));
				}
			}
			portindex++;
		}
		instance.setPorts(ps);
	}
}
