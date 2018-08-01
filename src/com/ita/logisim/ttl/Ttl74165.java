package com.ita.logisim.ttl;

import java.awt.Font;
import java.awt.Graphics;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class Ttl74165 extends AbstractTtlGate {

	public Ttl74165() {
		super("74165", (byte) 16, new byte[] { 7, 9 }, new String[] { "Shift/Load", "Clock", "P4", "P5", "P6", "P7",
				"Q7n", "Q7", "Serial Input", "P0", "P1", "P2", "P3", "Clock Inhibit" });
	}

	private ShiftRegisterData getData(InstanceState state) {
		ShiftRegisterData data = (ShiftRegisterData) state.getData();
		if (data == null) {
			data = new ShiftRegisterData(BitWidth.ONE, 8);
			state.setData(data);
		}
		return data;
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		Graphics g = painter.getGraphics();
		super.paintBase(painter, false, false);
		Drawgates.paintPortNames(painter, x, y, height, new String[] { "ShLd", "CK", "P4", "P5", "P6", "P7", "Q7n",
				"Q7", "SER", "P0", "P1", "P2", "P3", "CkIh" });
		ShiftRegisterData data = getData(painter);
		String s = "";
		for (byte i = 0; i < 8; i++)
			s += data.get(7 - i).toHexString();
		g.setFont(new Font(Font.DIALOG_INPUT, Font.PLAIN, 14));
		GraphicsUtil.drawCenteredText(g, s, x + 80, y + height / 2 - 3);
	}

	@Override
	public void ttlpropagate(InstanceState state) {
		ShiftRegisterData data = getData(state);
		boolean triggered = data.updateClock(state.getPortValue(1), StdAttr.TRIG_RISING);
		if (triggered && state.getPortValue(13) != Value.TRUE) {
			if (state.getPortValue(0) == Value.FALSE) {// load
				data.clear();
				data.push(state.getPortValue(9));
				data.push(state.getPortValue(10));
				data.push(state.getPortValue(11));
				data.push(state.getPortValue(12));
				data.push(state.getPortValue(2));
				data.push(state.getPortValue(3));
				data.push(state.getPortValue(4));
				data.push(state.getPortValue(5));
			} else if (state.getPortValue(0) == Value.TRUE) {// shift
				data.push(state.getPortValue(8));
			}
		}
		state.setPort(6, data.get(0).not(), 4);
		state.setPort(7, data.get(0), 4);
	}
	@Override
	public boolean CheckForGatedClocks(NetlistComponent comp) {
		return true;
	}
	
	@Override
	public int ClockPinIndex(NetlistComponent comp) {
		return 1;
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
			MyHDLGenerator = new Ttl74165HDLGenerator();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
