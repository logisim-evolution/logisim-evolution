package com.ita.logisim.ttl;

import java.awt.Graphics;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7413 extends AbstractTtlGate {
	
	private boolean inverted = true;

	public Ttl7413(String Name, boolean inv) {
		super(Name, (byte) 14, new byte[] { 6,8 }, new byte[] {3,11},
				new String[] { "A0", "B0", "C0", "D0", "Y0", "Y1", "D1", "C1", "B1", "A1"});
		inverted = inv;
	}

	public Ttl7413(String Name) {
		super(Name, (byte) 14, new byte[] { 6,8 }, new byte[] {3,11},
				new String[] { "A0", "B0", "C0", "D0", "Y0", "Y1", "D1", "C1", "B1", "A1"});
	}

	public Ttl7413() {
		super("7413", (byte) 14, new byte[] { 6,8 }, new byte[] {3,11},
				new String[] { "A0", "B0", "C0", "D0", "Y0", "Y1", "D1", "C1", "B1", "A1"});
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		super.paintBase(painter, false, false);
		Graphics g = painter.getGraphics();
		Drawgates.paintAnd(g, x+125, y+20, 10, 10, inverted);
		Drawgates.paintAnd(g, x+105, y+40, 10, 10, inverted);
		int Offset = inverted ? 0 : -4;
		g.drawLine(x+129+Offset, y+20, x+130, y+20);
		g.drawLine(x+130, y+AbstractTtlGate.pinheight, x+130, y+20);
		g.drawLine(x+109+Offset, y+40, x+110, y+40);
		g.drawLine(x+110, y+height-AbstractTtlGate.pinheight, x+110, y+40);
		for (int i = 0 ; i < 5 ; i++) {
			if (i!=2) {
				g.drawLine(x+10+i*20, y+height-AbstractTtlGate.pinheight, x+10+i*20, y+36+i*2);
				g.drawLine(x+10+i*20, y+36+i*2, x+95, y+36+i*2);
				g.drawLine(x+30+i*20, y+AbstractTtlGate.pinheight, x+30+i*20, y+24-i*2);
				g.drawLine(x+30+i*20, y+24-i*2, x+115, y+24-i*2);
			}
		}
	}

	@Override
	public void ttlpropagate(InstanceState state) {
		Value val = state.getPortValue(0).and(state.getPortValue(1).and(state.getPortValue(2).and(state.getPortValue(3))));
		state.setPort(4, inverted ? val.not() : val , 3);
		val = state.getPortValue(6).and(state.getPortValue(7).and(state.getPortValue(8).and(state.getPortValue(9))));
		state.setPort(5, inverted ? val.not() : val , 4);
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
			MyHDLGenerator = new Ttl7413HDLGenerator(inverted);
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
