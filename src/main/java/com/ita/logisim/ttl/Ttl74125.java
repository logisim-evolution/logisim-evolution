package com.ita.logisim.ttl;

import java.awt.Graphics;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl74125 extends AbstractTtlGate {

	public Ttl74125() {
		super("74125", (byte) 14, new byte[] { 3, 6, 8, 11 }, true);
	}

	@Override
	public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
		Graphics g = painter.getGraphics();
		int portwidth = 15, portheight = 8;
		int youtput = y + (up ? 20 : 40);
		Drawgates.paintBuffer(g, x + 50, youtput, portwidth, portheight);
		// output line
		Drawgates.paintOutputgate(g, x + 50, y, x + 45, youtput, up,height);
		// input line
		Drawgates.paintSingleInputgate(g, x + 30, y, x + 35, youtput, up,height);
		// enable line
		if (!up) {
			Drawgates.paintSingleInputgate(g, x + 10, y, x + 41, youtput - 7, up,height);
			g.drawLine(x + 41, youtput - 5, x + 41, youtput - 7);
			g.drawOval(x + 40, youtput - 5, 3, 3);
		} else {
			Drawgates.paintSingleInputgate(g, x + 10, y, x + 41, youtput + 7, up,height);
			g.drawLine(x + 41, youtput + 5, x + 41, youtput + 7);
			g.drawOval(x + 40, youtput + 2, 3, 3);
		}
	}

	@Override
	public void ttlpropagate(InstanceState state) {
		for (byte i = 2; i < 6; i += 3) {

			if (state.getPortValue(i - 2) == Value.TRUE)
				state.setPort(i, Value.UNKNOWN, 1);
			else
				state.setPort(i, state.getPortValue(i - 1), 1);
		}
		for (byte i = 6; i < 11; i += 3) {
			if (state.getPortValue(i + 2) == Value.TRUE)
				state.setPort(i, Value.UNKNOWN, 1);
			else
				state.setPort(i, state.getPortValue(i + 1), 1);
		}
	}

}
