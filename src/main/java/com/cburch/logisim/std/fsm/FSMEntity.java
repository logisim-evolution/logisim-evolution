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

package com.cburch.logisim.std.fsm;

import java.awt.Color; 
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Window;
import java.util.WeakHashMap;

import org.apache.log4j.spi.LoggerFactory;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.InputPort;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.parser.FSMSerializer;
import com.cburch.logisim.statemachine.simulator.FSMSimulator;
import com.cburch.logisim.std.memory.Ram.Logger;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;


public class FSMEntity extends InstanceFactory {

	static class ContentAttribute extends Attribute<FSMContent> {

		public ContentAttribute() {
			super("content", Strings.getter("fsmContentAttr"));
		}

		@Override
		public java.awt.Component getCellEditor(Window source, FSMContent value) {
			Project proj = source instanceof Frame ? ((Frame) source)
					.getProject() : null;
			return FSMEntityAttributes.getContentEditor(source, value, proj);
		}

		@Override
		public FSMContent parse(String value) {
			return new FSMContent(value);
		}

		@Override
		public String toDisplayString(FSMContent value) {
			return Strings.get("fsmContentValue");
		}

		@Override
		public String toStandardString(FSMContent value) {
			return FSMSerializer.saveAsString(value.getFsm());
		}
	}

	static class FSMEntityListener implements FSMModelListener {

		Instance instance;

		FSMEntityListener(Instance instance) {
			this.instance = instance;
		}

		@Override
		public void contentSet(FSMContent source) {
			instance.fireInvalidated();
			instance.recomputeBounds();
		}
	}

	//final static Logger logger = LoggerFactory.getLogger(FSMEntity.class);


	static final Attribute<FSMContent> CONTENT_ATTR = new ContentAttribute();
	static final int WIDTH = 140;
	static final int HEIGHT = 40;
	static final int PORT_GAP = 20;

	static final int X_PADDING = 5;

	private static final int CLK = 0;
	private static final int RST = 1;
	private static final int EN = 2;

	private static final int DELAY = 0;

	private WeakHashMap<Instance, FSMEntityListener> contentListeners;


	public static final Attribute<Boolean> ATTR_SHOW_IN_TAB = Attributes.forBoolean("showInTab",
			Strings.getter("registerShowInTab"));

	public FSMEntity() {
		super("FSM Entity", Strings.getter("fsmComponent"));
		this.contentListeners = new WeakHashMap<Instance, FSMEntityListener>();
		
		setAttributes(
					new Attribute[] { 
							StdAttr.WIDTH, 
							StdAttr.TRIGGER, 
							StdAttr.LABEL, 
							StdAttr.LABEL_FONT,
							ATTR_SHOW_IN_TAB, 
					},
					new Object[] { 
							BitWidth.create(8), 
							StdAttr.TRIG_RISING, 
							"fsm0", 
							StdAttr.DEFAULT_LABEL_FONT, 
							false, 
					}
				);
		setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
		setIconName("RAM.gif");

	}

	@Override
	public String getName() {
		return super.getName();
	}

	@Override
	public HDLGeneratorFactory getHDLGenerator(String HDLIdentifier, AttributeSet attrs) {
		return super.getHDLGenerator(HDLIdentifier, attrs);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new FSMHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	
	@Override
	protected void configureNewInstance(Instance instance) {
		FSMContent content = instance.getAttributeValue(CONTENT_ATTR);
		FSMEntityListener listener = new FSMEntityListener(instance);

		Bounds bds = instance.getBounds();
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, bds.getX()
				+ bds.getWidth() / 2, bds.getY() - 3, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);

		contentListeners.put(instance, listener);
		content.addFSMModelListener(listener);

		instance.addAttributeListener();
		updatePorts(instance);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new FSMEntityAttributes();
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		return attrs.getValue(CONTENT_ATTR).getName().toLowerCase();
	}



	@Override
	public String getHDLTopName(AttributeSet attrs) {

		String label = "";

		if (attrs.getValue(StdAttr.LABEL) != "")
			label = "_" + attrs.getValue(StdAttr.LABEL).toLowerCase();

		return getHDLName(attrs) + label;
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		FSMContent content = attrs.getValue(CONTENT_ATTR);
		int nbInputs = content.getInputsNumber();
		int nbOutputs = content.getOutputsNumber();

		int nbio = content.getControls().length+Math.max(nbInputs, nbOutputs);
		return Bounds.create(0, 0, WIDTH, nbio* PORT_GAP + HEIGHT);
	}

	

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == CONTENT_ATTR) {
			updatePorts(instance);
			instance.recomputeBounds();
			instance.fireInvalidated();
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		FSMContent content = painter.getAttributeValue(CONTENT_ATTR);
		FSMSimulator fsmSimulator = (FSMSimulator) painter.getData();
		if (fsmSimulator == null) {
			fsmSimulator = new FSMSimulator(content.getFsm());
			painter.setData(fsmSimulator);
		}

		getOffsetBounds(painter.getAttributeSet());
		FontMetrics metric = g.getFontMetrics();

		Bounds bds = painter.getBounds();
		int x0 = bds.getX() + (bds.getWidth() / 2);
		int y0 = bds.getY() + metric.getHeight() + 12;
		GraphicsUtil.drawText(g,
				StringUtil.resizeString("FSM:"+content.getName(), metric, WIDTH), x0,
				y0, GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);

		State currentState = fsmSimulator.getCurrentState();
		if (painter.getShowState()) {
			g.setColor(Color.LIGHT_GRAY);
			//g.fillRect(x0 + 20, y0 + 20, 80, 16);
			g.setColor(Color.black);
			String cs = "Undefined";
			if(currentState!=null) {
				cs = currentState.getName();
				String code = currentState.getCode();
				if(code!=null)	{
					GraphicsUtil.drawCenteredText(g, code, x0, y0 + 8+metric.getHeight());
				}
			}
			GraphicsUtil.drawCenteredText(g, cs, x0, y0 + 8);
			g.setColor(Color.black);
		}

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

		Port[] ctrl = content.getControls();
		Port[] inputs = content.getInputs();
		Port[] outputs = content.getOutputs();
		String names[] =new String[]{"CLK","RST","EN"};
		for (int i = 0; i < ctrl.length; i++)
			GraphicsUtil.drawText(g, StringUtil.resizeString(
					names[i], metric, (WIDTH / 2) - X_PADDING),
					bds.getX() + 5, bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		
		for (int i = 0; i < inputs.length; i++) {
			String name = inputs[i].getToolTip();
			int width2 = inputs[i].getFixedBitWidth().getWidth();
			if (width2!=1) {
				name = name + "["+(width2-1)+":0]";
			}
			GraphicsUtil.drawText(g, StringUtil.resizeString(
					name, metric, (WIDTH / 2) - X_PADDING),
					bds.getX() + 5, bds.getY() + HEIGHT - 2 + ((i+ctrl.length) * PORT_GAP),
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			
		}
		for (int i = 0; i < outputs.length; i++) {
			String name = outputs[i].getToolTip();
			int width2 = outputs[i].getFixedBitWidth().getWidth();
			if (width2!=1) {
				name = name + "["+(width2-1)+":0]";
			}
			GraphicsUtil.drawText(g, StringUtil.resizeString(
					name, metric, (WIDTH / 2) - X_PADDING),
					bds.getX() + WIDTH - 5, bds.getY() + HEIGHT - 2
							+ ((i+ctrl.length) * PORT_GAP), GraphicsUtil.H_RIGHT,
					GraphicsUtil.V_CENTER);
		}
		painter.drawBounds();
		painter.drawPorts();
	}

	@Override
	/**
	 * Propagate signals through the VHDL component.
	 * Logisim doesn't have a VHDL simulation tool. So we need to use an external tool.
	 * We send signals to Questasim/Modelsim through a socket and a tcl binder. Then,
	 * a simulation step is done and the tcl server sends the output signals back to
	 * Logisim. Then we can set the VHDL component output properly.
	 *
	 * This can be done only if Logisim could connect to the tcl server (socket). This is
	 * done in Simulation.java.
	 */

	
	public void propagate(InstanceState istate) {
		FSMSimulator fsmSim = (FSMSimulator) istate.getData();
		FSMContent content = istate.getAttributeValue(CONTENT_ATTR);
		updatePorts(istate.getInstance());
		FSM fsm = content.getFsm();
		System.out.println("Propagate event for FSM "+fsm.getName()+":"+fsm.hashCode());

		if (fsmSim == null ) { 
			fsmSim = new FSMSimulator(fsm);
			istate.setData(fsmSim);
		} else if (fsmSim.getFSM()!=fsm) {
			System.out.println("FSM changed, refreshing model");
			State oldState = fsmSim.getCurrentState();
			fsmSim = new FSMSimulator(fsm);
			istate.setData(fsmSim);
			for(State s : fsm.getStates()) {
				if (s.getName().equals(oldState.getName())) {
					fsmSim.setCurrentState(s);
				}
			}
			fsmSim.refreshInputPorts();
			fsmSim.restoreOutputPorts();
		}

		Value clk = istate.getPortValue(CLK);
		Value clear = istate.getPortValue(RST);
		Value enable = istate.getPortValue(EN);
		boolean triggered = fsmSim.updateClock(clk, null);// triggerType);
		int offsetInput = content.getControls().length;
		boolean error= false;
		for (int i =0; i< content.getInputsNumber();i++) {
			Value in = istate.getPortValue(i+offsetInput);
			InputPort ip = content.inMap.get(content.inputs[i]);
			if (in.isFullyDefined()) {
				fsmSim.updateInput(ip, "\""+in.toBinaryString()+"\"");
			} else {
				System.err.println("Warning : undefined input value for "+ip.getName()+ "="+in.toBinaryString());
				fsmSim.updateInput(ip, "\""+Value.createUnknown(in.getBitWidth()).toBinaryString()+"\"");
				error=true;// FIXME : propagate 
			}
		}

		if (clear == Value.TRUE) {
			fsmSim.reset();
		} else {
			
			if (triggered && enable != Value.FALSE && fsmSim.getCurrentState()!=null) {
				
				if(content.getInputsNumber()!= content.getFsm().getIn().size()) {
					throw new RuntimeException("Inconsistent state for input port mapping in "+this.getClass().getSimpleName());
				}
				if(content.getOutputsNumber()!= content.getFsm().getOut().size()) {
					throw new RuntimeException("Inconsistent state for Output port mapping in "+this.getClass().getSimpleName());
				}
				if(!error) {
					System.out.println("Update state");
					State nextState = fsmSim.updateState();
					if(nextState==null) throw new RuntimeException("Error : no next state ");
				}
				
			}
		}
		if (fsmSim.getCurrentState()!=null) {
			fsmSim.refreshInputPorts();
			fsmSim.restoreOutputPorts();
			fsmSim.updateCommands();
			for (int i =0; i< content.getOutputsNumber();i++) {
				String res = fsmSim.getOutput(i);
				String substring = res.substring(1, res.length()-1);
				int portIndex = istate.getPortIndex(content.outputs[i]);
				try {
					Value v= null;
					if(substring.length()>64) {
 						throw new RuntimeException("Commands with wordlength larger than 63 bits are not supported in FSM");
					} else {
						long parseLong= Long.parseLong(substring,2);
						String hexstr = Long.toHexString(parseLong);
						v= Value.fromLogString(BitWidth.create(substring.length()), "0x"+hexstr);
						istate.setPort(portIndex, v,DELAY); 
					}
				} catch (Exception e) {
					Port key = content.outputs[i];
					Value v= Value.createUnknown(key.getFixedBitWidth());
					istate.setPort(portIndex, v,DELAY); 
				}
				
			}
		} else {
			for (int i =0; i< content.getOutputsNumber();i++) {
				Port key = content.outputs[i];
				Value v= Value.createUnknown(key.getFixedBitWidth());
				int portIndex = istate.getPortIndex(key);
				istate.setPort(portIndex, v,DELAY); 
			}
			
		}
		
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}


	void updatePorts(Instance instance) {
		FSMContent content = instance.getAttributeValue(CONTENT_ATTR);
		instance.setPorts(content.getPorts());
	}
}
