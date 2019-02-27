package com.cburch.logisim.vhdl.base;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.std.hdl.VhdlEntityComponent;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorTop;

public class VhdlSimConstants {

	public static List<Component> getVhdlComponents(CircuitState s, boolean newStyle) {

		LinkedList<Component> vhdlComp = new LinkedList<Component>();

		/* Add current circuits comp */
		for (Component comp : s.getCircuit().getNonWires()) {
			if (comp.getFactory().getClass().equals(VhdlEntityComponent.class)) {
				vhdlComp.add(comp);
			}
			if (comp.getFactory().getClass().equals(VhdlEntity.class) && newStyle) {
				vhdlComp.add(comp);
			}
		}

		/* Add subcircuits comp */
		for (CircuitState sub : s.getSubstates()) {
			vhdlComp.addAll(getVhdlComponents(sub,newStyle));
		}

		return vhdlComp;
	}

	public static enum State {
		DISABLED, ENABLED, STARTING, RUNNING;
	}

	public final static Logger logger = LoggerFactory.getLogger(VhdlSimulatorTop.class);
	public final static Charset ENCODING = StandardCharsets.UTF_8;
	public final static String VHDL_TEMPLATES_PATH = "/resources/logisim/hdl/";
	public final static String SIM_RESOURCES_PATH = "/resources/logisim/sim/";
	public final static String SIM_PATH = System.getProperty("java.io.tmpdir")
			+ "/logisim/sim/";
	public final static String SIM_SRC_PATH = SIM_PATH + "src/";
	public final static String SIM_COMP_PATH = SIM_PATH + "comp/";
	public final static String SIM_TOP_FILENAME = "top_sim.vhdl";
}
