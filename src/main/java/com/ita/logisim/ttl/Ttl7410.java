package com.ita.logisim.ttl;

import java.awt.Graphics;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.prefs.AppPreferences;

public class Ttl7410 extends AbstractTtlGate {
	
	private boolean inverted = true;
	private boolean IsAnd = true;

	public Ttl7410() {
		super("7410", (byte) 14, new byte[] { 6,8,12 });
	}

	public Ttl7410(String val, boolean inv) {
		super(val, (byte) 14, new byte[] { 6,8,12 });
		inverted = inv;
	}

	public Ttl7410(String val, boolean inv, boolean IsOr) {
		super(val, (byte) 14, new byte[] { 6,8,12 });
		inverted = inv;
		IsAnd = !IsOr;
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		super.paintBase(painter, false, false);
		Graphics g = painter.getGraphics();
		int LineOffset = ((!IsAnd)&(AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_SHAPED))) ? -4 : 0;
		if (IsAnd) {
			Drawgates.paintAnd(g, x+45, y+20, 10, 10, inverted);
			Drawgates.paintAnd(g, x+125, y+20, 10, 10, inverted);
			Drawgates.paintAnd(g, x+105, y+40, 10, 10, inverted);
		} else {
			Drawgates.paintOr(g, x+45, y+20, 10, 10, inverted);
			Drawgates.paintOr(g, x+125, y+20, 10, 10, inverted);
			Drawgates.paintOr(g, x+105, y+40, 10, 10, inverted);
		}
		int offset = inverted ? 0 : -4;
		int [] xpos = new int[] {x+49+offset,x+50,x+50};
		int [] ypos = new int[] {y+20,y+20,y+AbstractTtlGate.pinheight};
		g.drawPolyline(xpos, ypos, 3);
		xpos[0]=x+129+offset;
		xpos[1]=xpos[2]=x+130;
		g.drawPolyline(xpos, ypos, 3);
		xpos[0]=x+109+offset;
		xpos[1]=xpos[2]=x+110;
		ypos[0]=ypos[1]=y+40;
		ypos[2]=y+height-AbstractTtlGate.pinheight;
		g.drawPolyline(xpos, ypos, 3);
		xpos = new int[] {x+30,x+30,x+35+LineOffset};
		ypos = new int[] {y+AbstractTtlGate.pinheight,y+17,y+17};
		g.drawPolyline(xpos, ypos, 3);
		xpos = new int[] {x+10,x+10,x+35+LineOffset};
		ypos = new int[] {y+height-AbstractTtlGate.pinheight,y+20,y+20};
		g.drawPolyline(xpos, ypos, 3);
		xpos = new int[] {x+30,x+30,x+35+LineOffset};
		ypos = new int[] {y+height-AbstractTtlGate.pinheight,y+23,y+23};
		g.drawPolyline(xpos, ypos, 3);

		for (int i = 0; i< 3 ; i++) {
			xpos = new int[] {x+70+i*20,x+70+i*20,x+115+LineOffset};
			ypos = new int[] {y+AbstractTtlGate.pinheight,y+23-i*3,y+23-i*3};
			g.drawPolyline(xpos, ypos, 3);
			xpos = new int[] {x+50+i*20,x+50+i*20,x+95+LineOffset};
			ypos = new int[] {y+height-AbstractTtlGate.pinheight,y+37+i*3,y+37+i*3};
			g.drawPolyline(xpos, ypos, 3);
		}
}

	@Override
	public void ttlpropagate(InstanceState state) {
		Value val = (IsAnd) ? state.getPortValue(2).and(state.getPortValue(3).and(state.getPortValue(4))) :
			                  state.getPortValue(2).or(state.getPortValue(3).or(state.getPortValue(4)));
		state.setPort(5, inverted ? val.not() : val , 2);
		val = (IsAnd) ? state.getPortValue(0).and(state.getPortValue(1).and(state.getPortValue(11))) :
			            state.getPortValue(0).or(state.getPortValue(1).or(state.getPortValue(11)));
		state.setPort(10, inverted ? val.not() : val, 2);
		val = (IsAnd) ? state.getPortValue(7).and(state.getPortValue(8).and(state.getPortValue(9))) :
			            state.getPortValue(7).or(state.getPortValue(8).or(state.getPortValue(9)));
		state.setPort(6, inverted ? val.not() : val, 2);
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		StringBuffer CompleteName = new StringBuffer();
		CompleteName.append(CorrectLabel.getCorrectLabel("TTL"+this.getName())
				.toUpperCase());
		return CompleteName.toString();
	}
	
	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new Ttl7410HDLGenerator(inverted,IsAnd);
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
