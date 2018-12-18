package com.ita.logisim.ttl;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7447 extends AbstractTtlGate {

	public Ttl7447() {
		super("7447", (byte) 16, new byte[] { 9, 10, 11, 12, 13, 14, 15 },
				new String[] { "B", "C", "LT", "BI", "RBI", "D", "A", "e", "d", "c", "b", "a", "g", "f" });
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		super.paintBase(painter, true, false);
		Drawgates.paintPortNames(painter, x, y, height, super.portnames);
	}

	@Override
	public void ttlpropagate(InstanceState state) {
		DisplayDecoder.ComputeDisplayDecoderOutputs(state, DisplayDecoder.getdecval(state, false, 0, 6, 0, 1, 5), 11,
				10, 9, 8, 7, 13, 12, 2, 3, 4);
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
			MyHDLGenerator = new Ttl7447HDLGenerator();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
