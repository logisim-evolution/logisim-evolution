package com.ita.logisim.ttl;

import java.awt.Graphics;
import java.util.ArrayList;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7404 extends AbstractTtlGate {

	private class NotGateHDLGeneratorFactory extends AbstractGateHDLGenerator {

		@Override
		public boolean IsInverter() {
			return true;
		}

		@Override
		public String getComponentStringIdentifier() {
			return "TTL7404";
		}

		@Override
		public ArrayList<String> GetLogicFunction(int index, String HDLType) {
			ArrayList<String> Contents = new ArrayList<String>();
			if (HDLType.equals(VHDL))
				Contents.add("   gate_"+Integer.toString(index)+"_O <= NOT(gate_"+Integer.toString(index)+"_A);");
			else
				Contents.add("   assign gate_"+Integer.toString(index)+"_O = ~(gate_"+Integer.toString(index)+"_A);");
			Contents.add("");
			return Contents;
		}

	}

	public Ttl7404() {
		super("7404", (byte) 14, new byte[] { 2, 4, 6, 8, 10, 12 }, true);
	}

	public Ttl7404(String Name) {
		super(Name, (byte) 14, new byte[] { 2, 4, 6, 8, 10, 12 }, true);
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		Graphics g = painter.getGraphics();
		int portwidth = 12, portheight = 6;
		int youtput = y + (up ? 20 : 40);
		Drawgates.paintNot(g, x + 26, youtput, portwidth, portheight);
		Drawgates.paintOutputgate(g, x + 30, y, x + 26, youtput, up,height);
		Drawgates.paintSingleInputgate(g, x + 10, y, x + 26 - portwidth, youtput, up,height);
	}

	@Override
	public void ttlpropagate(InstanceState state) {
		for (byte i = 1; i < 6; i += 2) {
			state.setPort(i, state.getPortValue(i - 1).not(), 1);
		}
		for (byte i = 6; i < 12; i += 2) {
			state.setPort(i, state.getPortValue(i + 1).not(), 1);
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
			MyHDLGenerator = new NotGateHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}