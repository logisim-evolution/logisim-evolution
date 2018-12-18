package com.ita.logisim.ttl;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl74283 extends AbstractTtlGate {

	public Ttl74283() {
		super("74283", (byte) 16, new byte[] { 1, 4, 9, 10, 13 },
				new String[] { "∑2", "B2", "A2", "∑1", "A1", "B1", "CIN", "C4", "∑4", "B4", "A4", "∑3", "A3", "B3" });
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		super.paintBase(painter, true, false);
		Drawgates.paintPortNames(painter, x, y, height, super.portnames);
	}

	@Override
	public void ttlpropagate(InstanceState state) {
		byte A1 = state.getPortValue(4) == Value.TRUE ? (byte) 1 : 0;
		byte A2 = state.getPortValue(2) == Value.TRUE ? (byte) 2 : 0;
		byte A3 = state.getPortValue(12) == Value.TRUE ? (byte) 4 : 0;
		byte A4 = state.getPortValue(10) == Value.TRUE ? (byte) 8 : 0;
		byte B1 = state.getPortValue(5) == Value.TRUE ? (byte) 1 : 0;
		byte B2 = state.getPortValue(1) == Value.TRUE ? (byte) 2 : 0;
		byte B3 = state.getPortValue(13) == Value.TRUE ? (byte) 4 : 0;
		byte B4 = state.getPortValue(9) == Value.TRUE ? (byte) 8 : 0;
		byte CIN = state.getPortValue(6) == Value.TRUE ? (byte) 1 : 0;
		byte sum = (byte) (A1 + A2 + A3 + A4 + B1 + B2 + B3 + B4 + CIN);
		Value output = Value.createKnown(BitWidth.create(5), sum);
		state.setPort(3, output.get(0), 1);
		state.setPort(0, output.get(1), 1);
		state.setPort(11, output.get(2), 1);
		state.setPort(8, output.get(3), 1);
		state.setPort(7, output.get(4), 1);
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
			MyHDLGenerator = new Ttl74283HDLGenerator();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
