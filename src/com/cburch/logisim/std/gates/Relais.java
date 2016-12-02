/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.gates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.io.Button;
import com.cburch.logisim.std.io.Io;
import com.cburch.logisim.std.io.Button.Logger;
import com.cburch.logisim.std.io.Button.Poker;
import com.cburch.logisim.tools.WireRepair;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.util.GraphicsUtil;

public class Relais extends InstanceFactory {
	static final int DELAY = 10;
    static final int DELAY2 = 1;
    static final int SIZE = 40;

    static final AttributeOption PORT0IN=new AttributeOption("0",Strings.getter("Port0 Input"));
    static final AttributeOption PORT0OUT= new AttributeOption("1",Strings.getter("Port0 Output"));
    public static final Attribute<AttributeOption> ATTR_DIRECTION=Attributes.forOption("PortInput", 
    		Strings.getter("Select switch direction"),
    		new AttributeOption[]{PORT0IN,PORT0OUT});    
    
	public Relais() {
		super("Relais", Strings.getter("relaisComponent"));
        setAttributes(new Attribute[] {
                StdAttr.FACING,
                StdAttr.LABEL,
                StdAttr.LABEL_FONT,
                Relais.ATTR_DIRECTION
            }, new Object[] {
                Direction.EAST, 
                "",
                StdAttr.DEFAULT_LABEL_FONT,
                Relais.PORT0IN
            });
        setFacingAttribute(StdAttr.FACING);
    }
	
	protected void paintShape(InstancePainter painter, int width, int height) {
		AttributeSet attrs = painter.getAttributeSet();
		boolean direction;
		if(attrs.getValue(Relais.ATTR_DIRECTION)==PORT0IN) direction=false;
		else direction=true;
		
		RelaisData data = (RelaisData) painter.getData();
		painter.drawRectangle(0, 0, width, height, "");
		Graphics g = painter.getGraphics();
		
		// draw infield sticks
		g.drawLine(width / 2, 0, width / 2, 4);
		g.drawLine(width / 2, height - 4, width / 2, height);
		g.drawLine(width*3/4, height - 4, width*3/4, height);

		
		if (data != null) {
			boolean active = data.getValue();
			Value state = data.getSwitch();
			
			g.setColor(state.getColor());

			// draw switching stick
			if (active) {
				drawArrowLine(g,width / 2, 4, width*3/4, height - 4,8,4,direction);
			} else {
				drawArrowLine(g,width / 2, 4, width / 2, height - 4,8,4,direction);
			}
		}
	}// paint shape

	private void drawArrowLine(Graphics g, int xx1, int yy1, int xx2, int yy2, int d, int h,boolean dir){
		int x1;int x2;int y1;int y2;
		
		if(dir){
			x1=xx2;x2=xx1;y1=yy2;y2=yy1;
		}else{
			x1=xx1;x2=xx2;y1=yy1;y2=yy2;			
		}
		
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        double xm = D - d, xn = xm, ym = h, yn = -h, x;
        double sin = dy/D, cos = dx/D;

        x = xm*cos - ym*sin + x1;
        ym = xm*sin + ym*cos + y1;
        xm = x;

        x = xn*cos - yn*sin + x1;
        yn = xn*sin + yn*cos + y1;
        xn = x;

        int[] xpoints = {x2, (int) xm, (int) xn};
        int[] ypoints = {y2, (int) ym, (int) yn};

        g.drawLine(x1, y1, x2, y2);
        g.fillPolygon(xpoints, ypoints, 3);
     }


	@Override
	public Bounds getOffsetBounds(AttributeSet attrsBase) {
		Direction facing = attrsBase.getValue(StdAttr.FACING);
		int width = SIZE;

		int height = Math.max(30 , width);
		if (facing == Direction.SOUTH) {
			return Bounds.create(-height, 0, height, width);
		} else if (facing == Direction.NORTH) {
			return Bounds.create(0, -width, height, width);
		} else if (facing == Direction.WEST) {
			return Bounds.create(-width, -height, width, height);
		} else {
			return Bounds.create(0, 0, width, height);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		paintBase(painter);
		if (!painter.isPrintView()
				|| painter.getGateShape() == AppPreferences.SHAPE_RECTANGULAR) {
			painter.drawPorts();
		}
	}
	
	
	private void paintBase(InstancePainter painter) {
		AttributeSet attrs = painter.getAttributeSet();
		Direction facing = attrs.getValue(StdAttr.FACING);
		Location loc = painter.getLocation();
		Bounds bds = painter.getOffsetBounds();
		int width = bds.getWidth();
		int height = bds.getHeight();
		if (facing == Direction.NORTH || facing == Direction.SOUTH) {
			int t = width;
			width = height;
			height = t;
		}


		Graphics g = painter.getGraphics();
		Color baseColor = g.getColor();

		g.setColor(baseColor);
		g.translate(loc.getX(), loc.getY());
		double rotate = 0.0;
		if (facing != Direction.EAST && g instanceof Graphics2D) {
			rotate = -facing.toRadians();
			Graphics2D g2 = (Graphics2D) g;
			g2.rotate(rotate);
		}


		paintShape(painter, width, height);

		if (rotate != 0.0) {
			((Graphics2D) g).rotate(-rotate);
		}
		g.translate(-loc.getX(), -loc.getY());

		painter.drawLabel();
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		computePorts(instance);
		computeLabel(instance);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if ((attr == StdAttr.FACING)||(attr == Relais.ATTR_DIRECTION)) {
			instance.recomputeBounds();
			computePorts(instance);
			computeLabel(instance);
		}
	}

	private void computeLabel(Instance instance) {
		AttributeSet attrs = (AttributeSet) instance.getAttributeSet();
		Direction facing = attrs.getValue(StdAttr.FACING);
		int baseWidth = SIZE;

		int axis = baseWidth / 2;
		int perp = 0;
		if (AppPreferences.GATE_SHAPE.get().equals(
				AppPreferences.SHAPE_RECTANGULAR)) {
			perp += 6;
		}
		Location loc = instance.getLocation();
		int cx;
		int cy;
		if (facing == Direction.NORTH) {
			cx = loc.getX() + perp;
			cy = loc.getY() + axis;
		} else if (facing == Direction.SOUTH) {
			cx = loc.getX() - perp;
			cy = loc.getY() - axis;
		} else if (facing == Direction.WEST) {
			cx = loc.getX() + axis;
			cy = loc.getY() - perp;
		} else {
			cx = loc.getX() - axis;
			cy = loc.getY() + perp;
		}
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, cx, cy,
				TextField.H_CENTER, TextField.V_CENTER);
	}

	void computePorts(Instance instance) {
		AttributeSet attrs = (AttributeSet) instance.getAttributeSet();
		Direction facing = attrs.getValue(StdAttr.FACING);
			
		Bounds bds = instance.getBounds();
		int width = bds.getWidth();
		int height = bds.getHeight();

		String Typ0=Port.OUTPUT;
		String Typ3=Port.INPUT;
		String Typ4=Port.INPUT;
		
		if(attrs.getValue(Relais.ATTR_DIRECTION)==PORT0IN){
			Typ0=Port.INPUT;
			Typ3=Port.OUTPUT;
			Typ4=Port.OUTPUT;
		}

				
		Port[] ports = new Port[2 + 3];

		// ####################OUTPUTS################################
		if (facing == Direction.SOUTH) {
			ports[0] = new Port(0, height / 2, Typ0, 1);
			ports[3] = new Port(-width, height / 2, Typ3,1);
			ports[4] = new Port(-width, height - 10, Typ4,1);
		} else if (facing == Direction.NORTH) {
			ports[0] = new Port(0, -height / 2, Typ0, 1);
			ports[3] = new Port(width, -height / 2, Typ3,1);
			ports[4] = new Port(width, -height + 10, Typ4,1);
		} else if (facing == Direction.WEST) {
			ports[0] = new Port(-width / 2, 0, Typ0, 1);
			ports[3] = new Port(-width / 2, -height, Typ3,1);
			ports[4] = new Port(-width + 10, -height, Typ4,1);
		} else {
			ports[0] = new Port(width / 2, 0, Typ0, 1);
			ports[3] = new Port(width / 2, height, Typ3,1);
			ports[4] = new Port(width - 10, height, Typ4,1);
		}

		// ###################Port label##########################################################
		ports[0].setToolTip(Strings.getter("port0"));
		ports[3].setToolTip(Strings.getter("port3"));
		ports[4].setToolTip(Strings.getter("port4"));

		// ###############################INPUTS#########################################
		for (int i = 0; i < 2; i++) {
			Location offs = getInputOffset(attrs, i);
			ports[i + 1] = new Port(offs.getX(), offs.getY(), Port.INPUT, 1);
		}

		instance.setPorts(ports);
		
	}// compute Ports

	
	@Override
	public void propagate(InstanceState state) {
		AttributeSet attrs = (AttributeSet) state.getAttributeSet();

		boolean RelaisActive= ((state.getPortValue(1) == Value.FALSE)&&(state.getPortValue(2) == Value.TRUE))||
		((state.getPortValue(2) == Value.FALSE)&&(state.getPortValue(1) == Value.TRUE));
		
		if(attrs.getValue(Relais.ATTR_DIRECTION)==PORT0IN){
			state.setData(new RelaisData(RelaisActive,state.getPortValue(0)));		
			if( RelaisActive ){
				state.setPort(3, Value.NIL, DELAY2);
				state.setPort(4, state.getPortValue(0), DELAY);
				state.setPort(0, Value.NIL, DELAY2);
			} else{
				state.setPort(4, Value.NIL, DELAY2);
				state.setPort(3, state.getPortValue(0), DELAY);
				state.setPort(0, Value.NIL, DELAY2);
			}
		} else {
			if( RelaisActive ){
				state.setData(new RelaisData(RelaisActive,state.getPortValue(4)));		
				state.setPort(3, Value.NIL, DELAY2);
				state.setPort(0, state.getPortValue(4), DELAY);
				state.setPort(4, Value.NIL, DELAY2);					
			} else{
				state.setData(new RelaisData(RelaisActive,state.getPortValue(3)));		
				state.setPort(4, Value.NIL, DELAY2);
				state.setPort(0, state.getPortValue(3), DELAY);
				state.setPort(3, Value.NIL, DELAY2);
			}			
			
		}
		
	} // propagate

	Location getInputOffset(AttributeSet attrs, int index) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		int size = SIZE;

		int skipStart;
		int skipDist;
		int skipLowerEven = 10;

			if (size <= 40) {
				skipStart = 5; // -5
				skipDist = 10;
				skipLowerEven = 10;
			} else if (size < 60) {
				skipStart = -10; 
				skipDist = 20;
				skipLowerEven = 20;
			} else {
				skipStart = -15;
				skipDist = 30;
				skipLowerEven = 30;
			}
		

		int dy = skipStart * 2 + skipDist * index;
			if (index == 1) {
				dy += skipLowerEven;
			}

		if (facing == Direction.NORTH) {
			return Location.create(dy, 0);
		} else if (facing == Direction.SOUTH) {
			return Location.create(-dy, 0);
		} else if (facing == Direction.WEST) {
			return Location.create(0, -dy);
		} else {
			return Location.create(0, dy);
		}
	}

}
//################ Added Class to transfer Value[] ###########################
class RelaisData implements InstanceData, Cloneable {
	boolean active;
	Value switchVal;

	@Override
	public RelaisData clone() {
		return clone();

	}

	public RelaisData(boolean active,Value switchVal) {
		this.active = active;
		this.switchVal = switchVal;
	}

	public Value getSwitch() {
		return switchVal;
	}

	public boolean getValue() {
		return active;
	}

}
