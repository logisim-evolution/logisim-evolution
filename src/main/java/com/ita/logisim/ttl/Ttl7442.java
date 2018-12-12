package com.ita.logisim.ttl;

import java.awt.Graphics;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7442 extends AbstractTtlGate {
	
	private boolean IsExec3 = false;
	private boolean IsGray = false;

	public Ttl7442() {
		super("7442", (byte) 16, new byte[] { 1,2,3,4,5,6,7,9,10,11 },
				new String[] { "O0", "O1", "O2", "O3", "O4", "O5", "O6", "O7", "O8", "O9","D","C","B","A"});
	}

	public Ttl7442(String name, int encoding) {
		super(name, (byte) 16, new byte[] { 1,2,3,4,5,6,7,9,10,11 },
				new String[] { "O0", "O1", "O2", "O3", "O4", "O5", "O6", "O7", "O8", "O9","D","C","B","A"});
		IsExec3 = encoding == 1;
		IsGray = encoding == 2;
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		super.paintBase(painter, false, false);
		Graphics g = painter.getGraphics();
		g.drawRect(x+18, y+10, 84, 18);
		int mask = 1;
		for (int i = 0 ; i < 10 ; i++) {
			g.drawOval(x+22+i*8, y+28, 4, 4);
			g.drawLine(x+24+i*8, y+32, x+24+i*8, y+height-AbstractTtlGate.pinheight-(i+1)*2);
			g.drawString(Integer.toString(i), x+22+i*8, y+26);
			if (i<4) {
				g.drawString(Integer.toString(mask), x+27+i*20, y+16);
				mask <<= 1;
				g.drawLine(x+30+i*20, y+AbstractTtlGate.pinheight, x+30+i*20, y+10);
			}
			if (i<7) {
				g.drawLine(x+10+i*20, y+height-AbstractTtlGate.pinheight, x+10+i*20, y+height-AbstractTtlGate.pinheight-(i+1)*2);
				g.drawLine(x+10+i*20, y+height-AbstractTtlGate.pinheight-(i+1)*2, x+24+i*8, y+height-AbstractTtlGate.pinheight-(i+1)*2);
			} else {
				int j = i==7 ? 9 : i== 9 ? 7 : 8;
				g.drawLine(x+i*20 - 30, y+AbstractTtlGate.pinheight, x+i*20 -30, y+height-AbstractTtlGate.pinheight-(j+1)*2);
				g.drawLine(x+i*20 - 30, y+height-AbstractTtlGate.pinheight-(j+1)*2, x+24+j*8, y+height-AbstractTtlGate.pinheight-(j+1)*2);
			}
		}
	}

	@Override
	public void ttlpropagate(InstanceState state) {
		int decode = -1;
		if (!(state.getPortValue(13).isErrorValue()|state.getPortValue(13).isUnknown())) {
			decode = state.getPortValue(13)==Value.TRUE ? 1 : 0;
			if (!(state.getPortValue(12).isErrorValue()|state.getPortValue(12).isUnknown())) {
				decode |= state.getPortValue(12)==Value.TRUE ? 2 : 0;
				if (!(state.getPortValue(11).isErrorValue()|state.getPortValue(11).isUnknown())) {
					decode |= state.getPortValue(11)==Value.TRUE ? 4 : 0;
					if (!(state.getPortValue(10).isErrorValue()|state.getPortValue(10).isUnknown())) {
						decode |= state.getPortValue(10)==Value.TRUE ? 8 : 0;
					} else decode = -1;
				} else decode = -1;
			} else decode = -1;
		}
		if (decode < 0) {
			state.setPort(0, Value.UNKNOWN, 1);
			state.setPort(1, Value.UNKNOWN, 1);
			state.setPort(2, Value.UNKNOWN, 1);
			state.setPort(3, Value.UNKNOWN, 1);
			state.setPort(4, Value.UNKNOWN, 1);
			state.setPort(5, Value.UNKNOWN, 1);
			state.setPort(6, Value.UNKNOWN, 1);
			state.setPort(7, Value.UNKNOWN, 1);
			state.setPort(8, Value.UNKNOWN, 1);
			state.setPort(9, Value.UNKNOWN, 1);
		} else if (IsGray) {
			state.setPort(0, decode == 2 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(1, decode == 6 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(2, decode == 7 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(3, decode == 5 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(4, decode == 4 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(5, decode == 12 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(6, decode == 13 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(7, decode == 15 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(8, decode == 14 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(9, decode == 10 ? Value.FALSE : Value.TRUE, 1);
		} else {
			if (IsExec3) decode -= 3;
			state.setPort(0, decode == 0 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(1, decode == 1 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(2, decode == 2 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(3, decode == 3 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(4, decode == 4 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(5, decode == 5 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(6, decode == 6 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(7, decode == 7 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(8, decode == 8 ? Value.FALSE : Value.TRUE, 1);
			state.setPort(9, decode == 9 ? Value.FALSE : Value.TRUE, 1);
		}
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
			MyHDLGenerator = new Ttl7442HDLGenerator(IsExec3,IsGray);
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
