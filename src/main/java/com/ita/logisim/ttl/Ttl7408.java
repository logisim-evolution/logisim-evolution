package com.ita.logisim.ttl;

import java.awt.Graphics;
import java.util.ArrayList;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7408 extends AbstractTtlGate {

	private class AndGateHDLGeneratorFactory extends AbstractGateHDLGenerator {

		@Override
		public String getComponentStringIdentifier() {
			return "TTL7408";
		}

		@Override
		public ArrayList<String> GetLogicFunction(int index, String HDLType) {
			ArrayList<String> Contents = new ArrayList<String>();
			if (HDLType.equals(VHDL))
				Contents.add("   gate_"+Integer.toString(index)+"_O <= gate_"+Integer.toString(index)+"_A"
						+ " AND gate_"+Integer.toString(index)+"_B;");
			else
				Contents.add("   assign gate_"+Integer.toString(index)+"_O = gate_"+Integer.toString(index)+"_A"
						+ " & gate_"+Integer.toString(index)+"_B;");
			Contents.add("");
			return Contents;
		}

	}

	public Ttl7408() {
		super("7408", (byte) 14, new byte[] { 3, 6, 8, 11 }, true);
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		Graphics g = painter.getGraphics();
		int portwidth = 15, portheight = 15;
		int youtput = y + (up ? 20 : 40);
		Drawgates.paintAnd(g, x + 44, youtput, portwidth, portheight, false);
		// output line
		Drawgates.paintOutputgate(g, x + 50, y, x + 44, youtput, up,height);
		// input lines
		Drawgates.paintDoubleInputgate(g, x + 30, y, x + 44 - portwidth, youtput, portheight, up,height);
	}

	@Override
	public void ttlpropagate(InstanceState state) {
		for (byte i = 2; i < 6; i += 3) {
			state.setPort(i, state.getPortValue(i - 1).and(state.getPortValue(i - 2)), 1);
		}
		for (byte i = 6; i < 12; i += 3) {
			state.setPort(i, state.getPortValue(i + 1).and(state.getPortValue(i + 2)), 1);
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
			MyHDLGenerator = new AndGateHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}