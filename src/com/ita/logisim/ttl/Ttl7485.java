package com.ita.logisim.ttl;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7485 extends AbstractTtlGate {

	public Ttl7485() {
		super("7485", (byte) 16, new byte[] { 5, 6, 7 }, new String[] { "B3", "A<B", "A=B", "A>B", "A>B", "A=B", "A<B",
				"B0", "A0", "B1", "A1", "A2", "B2", "A3" });
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		super.paintBase(painter, true, false);
		Drawgates.paintPortNames(painter, x, y, height, super.portnames);
	}

	@Override
	public void ttlpropagate(InstanceState state) {
		byte A0 = state.getPortValue(8) == Value.TRUE ? (byte) 1 : 0;
		byte A1 = state.getPortValue(10) == Value.TRUE ? (byte) 2 : 0;
		byte A2 = state.getPortValue(11) == Value.TRUE ? (byte) 4 : 0;
		byte A3 = state.getPortValue(13) == Value.TRUE ? (byte) 8 : 0;
		byte B0 = state.getPortValue(7) == Value.TRUE ? (byte) 1 : 0;
		byte B1 = state.getPortValue(9) == Value.TRUE ? (byte) 2 : 0;
		byte B2 = state.getPortValue(12) == Value.TRUE ? (byte) 4 : 0;
		byte B3 = state.getPortValue(0) == Value.TRUE ? (byte) 8 : 0;
		byte A = (byte) (A3 + A2 + A1 + A0);
		byte B = (byte) (B3 + B2 + B1 + B0);
		if (A > B) {
			state.setPort(4, Value.TRUE, 1);
			state.setPort(5, Value.FALSE, 1);
			state.setPort(6, Value.FALSE, 1);
		} else if (A < B) {
			state.setPort(4, Value.FALSE, 1);
			state.setPort(5, Value.FALSE, 1);
			state.setPort(6, Value.TRUE, 1);
		} else {
			if (state.getPortValue(2) == Value.TRUE) {
				state.setPort(4, Value.FALSE, 1);
				state.setPort(5, Value.TRUE, 1);
				state.setPort(6, Value.FALSE, 1);
			} else if (state.getPortValue(1) == Value.TRUE && state.getPortValue(3) == Value.TRUE) {
				state.setPort(4, Value.FALSE, 1);
				state.setPort(5, Value.FALSE, 1);
				state.setPort(6, Value.FALSE, 1);
			} else if (state.getPortValue(1) == Value.TRUE) {
				state.setPort(4, Value.FALSE, 1);
				state.setPort(5, Value.FALSE, 1);
				state.setPort(6, Value.TRUE, 1);
			} else if (state.getPortValue(3) == Value.TRUE) {
				state.setPort(4, Value.TRUE, 1);
				state.setPort(5, Value.FALSE, 1);
				state.setPort(6, Value.FALSE, 1);
			} else {
				state.setPort(4, Value.TRUE, 1);
				state.setPort(5, Value.FALSE, 1);
				state.setPort(6, Value.TRUE, 1);
			}
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
			MyHDLGenerator = new Ttl7485HDLGenerator();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
