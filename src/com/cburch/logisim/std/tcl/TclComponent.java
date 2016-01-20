/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.std.tcl;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.WeakHashMap;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

/**
 * This is the component one should extend to implement the different TCL
 * components.
 *
 * @author christian.mueller@heig-vd.ch
 */
public abstract class TclComponent extends InstanceFactory {

	public static class PortDescription {

		private String name;
		private String type;
		private BitWidth width;

		public PortDescription(String name, String type, int width) {
			this.name = name;
			this.type = type;
			this.width = BitWidth.create(width);
		}

		public String getName() {
			return this.name;
		}

		public String getType() {
			return this.type;
		}

		public BitWidth getWidth() {
			return this.width;
		}
	}

	protected static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	static final int WIDTH = 140;
	static final int HEIGHT = 40;

	static final int PORT_GAP = 10;
	static final int X_PADDING = 5;
        
	private Port[] inputs;
	private Port[] outputs;
        
	private WeakHashMap<Instance, TclComponentListener> contentListeners;

	public TclComponent(String name, StringGetter displayName) {
		super(name, displayName);

		this.contentListeners = new WeakHashMap<Instance, TclComponentListener>();

		inputs = new Port[0];
		outputs = new Port[0];

		setIconName("tcl.gif");
	}

	//
	// methods for instances
	//
	@Override
	protected void configureNewInstance(Instance instance) {
		TclComponentListener listener = new TclComponentListener(instance);

		contentListeners.put(instance, listener);

		instance.addAttributeListener();
		updatePorts(instance);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new TclComponentAttributes();
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		int nbInputs = inputs.length;
		int nbOutputs = outputs.length;

		return Bounds.create(0, 0, WIDTH, Math.max(nbInputs, nbOutputs)
				* PORT_GAP + HEIGHT);
	}

	/**
	 * We cannot make a VHDL architecture equivalent to the TCL script
	 * 
	 * @return false
	 */
	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		return false;
	}

	/**
	 * This was taken from VHDL component
	 *
	 * @param painter
	 */
	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		FontMetrics metric = g.getFontMetrics();

		Bounds bds = painter.getBounds();
		int x0 = bds.getX() + (bds.getWidth() / 2);
		int y0 = bds.getY() + metric.getHeight() + 12;
		GraphicsUtil.drawText(g,
				StringUtil.resizeString(getDisplayName(), metric, WIDTH), x0,
				y0, GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);

		String glbLabel = painter.getAttributeValue(StdAttr.LABEL);
		if (glbLabel != null) {
			Font font = g.getFont();
			g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
			GraphicsUtil.drawCenteredText(g, glbLabel,
					bds.getX() + bds.getWidth() / 2, bds.getY()
							- g.getFont().getSize());
			g.setFont(font);
		}

		g.setColor(Color.GRAY);
		g.setFont(g.getFont().deriveFont((float) 10));
		metric = g.getFontMetrics();

		int i = 0;
		for (Port p : inputs) {
			GraphicsUtil.drawText(
					g,
					StringUtil.resizeString(p.getToolTip(), metric, (WIDTH / 2)
							- X_PADDING), bds.getX() + 5, bds.getY() + HEIGHT
							- 2 + (i++ * PORT_GAP), GraphicsUtil.H_LEFT,
					GraphicsUtil.V_CENTER);
		}

		i = 0;
		for (Port p : outputs) {
			GraphicsUtil.drawText(
					g,
					StringUtil.resizeString(p.getToolTip(), metric, (WIDTH / 2)
							- X_PADDING), bds.getX() + WIDTH - 5, bds.getY()
							+ HEIGHT - 2 + (i++ * PORT_GAP),
					GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
		}

		painter.drawBounds();
		painter.drawPorts();
	}

	/**
	 * This creates a new TCL process executing the TCL content file.
	 * Communication is done through a socket
	 *
	 * @param state
	 */
	@Override
	public void propagate(InstanceState state) {

            /*
             * The ComponentData is the persistent thing through logisim usage. It
             * doesn't change when you move the component when InstanceComponent
             * does.
             */
            TclComponentData tclComponentData = TclComponentData.get(state);

            tclComponentData.getTclWrapper().start();

            /*
             * Here we may miss the first clock if the TCL process is not soon fast
             * enought You may change this behavior, but blocking here seemed bad to
             * me
             */
            if (tclComponentData.isConnected()) {

                /* Send port values to the TCL wrapper */
                for (Port p : state.getInstance().getPorts()) {
                    int index = state.getPortIndex(p);
                    Value val = state.getPortValue(index);
                    String message = p.getType() + ":" + p.getToolTip() + ":"
                                    + val.toBinaryString() + ":" + index;

                    tclComponentData.send(message);
                }

                /* 
                 * If it is a new tick, ask the console to force the sti in the
                 * console and set them in Logisim in return. If it is not a new
                 * tick, simply send the updated obs to the console.
                 */
                if (tclComponentData.isNewTick()) {
                    tclComponentData.send("sync_force");
                    getPortsFromServer(state, tclComponentData);
                } else {
                    tclComponentData.send("sync_examine");
                    String server_response;
                    
                    /* Ignore all messages until "sync" is recieved */
                    while ((server_response = tclComponentData.receive()) != null 
                            && server_response.length() > 0
                            && !server_response.equals("sync"));
                }
            }
	}

        void getPortsFromServer(InstanceState state, TclComponentData tclComponentData) {
            String server_response;
            while ((server_response = tclComponentData.receive()) != null
                            && server_response.length() > 0
                            && !server_response.equals("sync")) {

                    String[] parameters = server_response.split("\\:");

                    /* Skip if we receive crap, still better than an out of range */
                    if (parameters.length < 2)
                            continue;

                    String busValue = parameters[1];
                    int portId = Integer.parseInt(parameters[2]);

                    // Expected response width
                    int width = state.getFactory().getPorts().get(portId)
                                    .getFixedBitWidth().getWidth();

                    /*
                     * If the received string is too long, cut the leftmost part to
                     * match the expected length
                     */
                    if (busValue.length() > width)
                            busValue = busValue.substring(busValue.length() - width);

                    /*
                     * If the received value is not wide enough, complete with X on
                     * the MSB
                     */
                    Value vector_values[] = new Value[width];
                    for (int i = width - 1; i >= busValue.length(); i--) {
                            vector_values[i] = Value.UNKNOWN;
                    }

                    /* Transform char to Logisim Value */
                    int k = busValue.length() - 1;
                    for (char bit : busValue.toCharArray()) {

                            try {
                                    switch (Character.getNumericValue(bit)) {
                                    case 0:
                                            vector_values[k] = Value.FALSE;
                                            break;
                                    case 1:
                                            vector_values[k] = Value.TRUE;
                                            break;
                                    default:
                                            vector_values[k] = Value.UNKNOWN;
                                            break;
                                    }
                            } catch (NumberFormatException e) {
                                    vector_values[k] = Value.ERROR;
                            }
                            k--;
                    }

                    /* Affect the value to the port */
                    state.setPort(portId, Value.create(vector_values), 1);
            }
        }
        
	/**
	 * When setting ports we also set some local attributes so we can manage
	 * inputs and outputs separately
	 *
	 * @param inputs
	 * @param outputs
	 */
	void setPorts(Port[] inputs, Port[] outputs) {
		this.inputs = inputs;
		this.outputs = outputs;
		setPorts(concat(inputs, outputs));
	}

	void updatePorts(Instance instance) {
		// Ports are static so they are never updated (only values will but
		// that's not this handler
	}

}
