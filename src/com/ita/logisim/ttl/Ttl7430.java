package com.ita.logisim.ttl;

import java.awt.Graphics;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7430 extends AbstractTtlGate {

	public Ttl7430() {
		super("7430", (byte) 14, new byte[] { 8 }, new byte[] {9,10,13},
				new String[] { "A", "B", "C", "D", "E", "F", "Y", "G", "H"});
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		super.paintBase(painter, false, false);
		Graphics g = painter.getGraphics();
		Drawgates.paintAnd(g, x+123, y+30, 10, 18, true);
		g.drawLine(x+70, y+AbstractTtlGate.pinheight, x+70, y+23);
		g.drawLine(x+50, y+AbstractTtlGate.pinheight, x+50, y+25);
		g.drawLine(x+10, y+height-AbstractTtlGate.pinheight, x+10, y+27);
		g.drawLine(x+30, y+height-AbstractTtlGate.pinheight, x+30, y+29);
		g.drawLine(x+50, y+height-AbstractTtlGate.pinheight, x+50, y+31);
		g.drawLine(x+70, y+height-AbstractTtlGate.pinheight, x+70, y+33);
		g.drawLine(x+90, y+height-AbstractTtlGate.pinheight, x+90, y+35);
		g.drawLine(x+110, y+height-AbstractTtlGate.pinheight, x+110, y+37);
		g.drawLine(x+70, y+23, x+113, y+23);
		g.drawLine(x+50, y+25, x+113, y+25);
		g.drawLine(x+10, y+27, x+113, y+27);
		g.drawLine(x+30, y+29, x+113, y+29);
		g.drawLine(x+50, y+31, x+113, y+31);
		g.drawLine(x+70, y+33, x+113, y+33);
		g.drawLine(x+90, y+35, x+113, y+35);
		g.drawLine(x+110, y+37, x+113, y+37);
		g.drawLine(x+128, y+30, x+130, y+30);
		g.drawLine(x+130, y+AbstractTtlGate.pinheight, x+130, y+30);
}

	@Override
	public void ttlpropagate(InstanceState state) {
		Value val1 = state.getPortValue(0).and(state.getPortValue(1).and(state.getPortValue(2).and(state.getPortValue(3))));
		Value val2 = val1.and(state.getPortValue(4).and(state.getPortValue(5).and(state.getPortValue(7).and(state.getPortValue(8)))));
		state.setPort(6, val2.not(), 1);
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
			MyHDLGenerator = new Ttl7430HDLGenerator();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
