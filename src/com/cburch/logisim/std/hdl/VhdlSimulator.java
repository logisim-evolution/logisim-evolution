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
package com.cburch.logisim.std.hdl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.help.UnsupportedOperationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.SocketClient;

/**
 * VHDL simulator allows Logisim to simulate the behavior of VHDL architectures.
 * It delegate this task to Questasim. Communication between Logisim and
 * Questasim is done by a TCL socket (TclBinder). Path to Questasim has to be
 * specified in Logisim's preferences.
 *
 * @author christian.mueller@heig-vd.ch
 * @since 2.12.0
 */
public class VhdlSimulator implements CircuitListener {

	public enum State {
		DISABLED, ENABLED, STARTING, RUNNING;
	}

	public static List<Component> getVhdlComponents(CircuitState s) {

		LinkedList<Component> vhdlComp = new LinkedList<Component>();

		/* Add current circuits comp */
		for (Component comp : s.getCircuit().getNonWires()) {
			if (comp.getFactory().getClass().equals(VhdlEntity.class)) {
				vhdlComp.add(comp);
			}
		}

		/* Add subcircuits comp */
		for (CircuitState sub : s.getSubstates()) {
			vhdlComp.addAll(getVhdlComponents(sub));
		}

		return vhdlComp;
	}

	final static Logger logger = LoggerFactory.getLogger(VhdlSimulator.class);

	final static Charset ENCODING = StandardCharsets.UTF_8;
	final static String VHDL_TEMPLATES_PATH = "/resources/logisim/hdl/";
	final static String SIM_RESOURCES_PATH = "/resources/logisim/sim/";
	final static String SIM_PATH = System.getProperty("java.io.tmpdir")
			+ "/logisim/sim/";
	final static String SIM_SRC_PATH = SIM_PATH + "src/";

	final static String SIM_COMP_PATH = SIM_PATH + "comp/";

	final static String SIM_TOP_FILENAME = "top_sim.vhdl";

	private VhdlSimulatorVhdlTop vhdlTop = new VhdlSimulatorVhdlTop(this);
	private VhdlSimulatorTclComp tclRun = new VhdlSimulatorTclComp(this);
	private VhdlSimulatorTclBinder tclBinder;
	private SocketClient socketClient = new SocketClient();

	private Project project;

	private static ArrayList<VhdlSimulatorListener> listeners = new ArrayList<VhdlSimulatorListener>();

	private State state = State.DISABLED;

	public VhdlSimulator(Project circuitState) {
		this.project = circuitState;
	}

	public void addVhdlSimStateListener(VhdlSimulatorListener l) {
		listeners.add(l);
	}

	/**
	 * Circuit listener Each time the circuit changes, test if there is a VHDL
	 * component If yes, start the VHDL simulator, and if not, stop it
	 */
	@Override
	public void circuitChanged(CircuitEvent event) {

		if (hasVhdlComponent(getProject()))
			start();
		else
			stop();
	}

	/**
	 * Disable the simulator. If it is running, stops it first.
	 */
	public void disable() {

		switch (state) {
		case RUNNING:
			stop();
			break;

		case ENABLED:
			break;

		case DISABLED:
			return;

		default:
			throw new UnsupportedOperationException(
					"Cannot disable VHDL simulator from " + state + " state");
		}

		setState(State.DISABLED);

		/* Hide and empty console log */
		if (getProject().getFrame() != null) {
			getProject().getFrame().setVhdlSimulatorConsoleStatus(false);
			getProject().getFrame().getVhdlSimulatorConsole().clear();
		}
	}

	/**
	 * Enable the simulator. Here we also decide to start it directly as it is
	 * what the user generally wants.
	 */
	public void enable() {

		/*
		 * Init binder with qsim, test qsim path and ask for a valid one if it
		 * fails
		 */
		if (tclBinder == null) {
			tclBinder = new VhdlSimulatorTclBinder(this);
		}

		switch (state) {
		case RUNNING:
			stop();
			break;

		case ENABLED:
			return;

		case DISABLED:
			break;

		default:
			throw new UnsupportedOperationException(
					"Cannot enable VHDL simulator from " + state + " state");
		}

		int i = 0;
		/* Wait in case starting simulation to early*/
		while(getProject().getFrame() == null) {
			try {
				Thread.sleep(100);
				if (i == 10) {
					break;
				}
				i++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (i != 10) {
			getProject().getFrame().setVhdlSimulatorConsoleStatus(true);
		}

		setState(State.ENABLED);

		start();
	}

	/* At least one of the VHDL entity changed */
	public void fireInvalidated() {

		// File dir = new File(SIM_SRC_PATH);
		// for(File file: dir.listFiles()) file.delete();

		// vhdlTop.fireInvalidated();
		// tclRun.fireInvalidated();
	}

	private void fireVhdlSimStateChanged() {
		for (VhdlSimulatorListener l : listeners) {
			l.stateChanged();
		}
	}

	public void generateFiles() {

		vhdlTop.fireInvalidated();
		tclRun.fireInvalidated();

		new File(SIM_PATH).mkdirs();
		new File(SIM_SRC_PATH).mkdirs();
		new File(SIM_COMP_PATH).mkdirs();

		try {
			Files.copy(
					this.getClass().getResourceAsStream(
							SIM_RESOURCES_PATH + "questasim_binder.tcl"),
					Paths.get(SIM_PATH + "questasim_binder.tcl"),
					StandardCopyOption.REPLACE_EXISTING);
			Files.copy(
					this.getClass().getResourceAsStream(
							SIM_RESOURCES_PATH + "run.tcl"),
					Paths.get(SIM_PATH + "run.tcl"),
					StandardCopyOption.REPLACE_EXISTING);
			Files.copy(
					this.getClass().getResourceAsStream(
							SIM_RESOURCES_PATH + "modelsim.ini"),
					Paths.get(SIM_COMP_PATH + "modelsim.ini"),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.error("Cannot copy simulation files: {}", e.getMessage());
			e.printStackTrace();
		}

		vhdlTop.generate();
		tclRun.generate();

		/* Generate each component's file */
		for (Component comp : getVhdlComponents(project.getCircuitState())) {
			((VhdlEntity) comp.getFactory()).saveFile(comp.getAttributeSet());
		}
	}

	public Project getProject() {
		return project;
	}

	public SocketClient getSocketClient() {
		return socketClient;
	}

	public State getState() {
		return state;
	}

	private boolean hasVhdlComponent(CircuitState s) {

		/* Test current circuit */
		for (Component comp : s.getCircuit().getNonWires()) {
			if (comp.getFactory().getClass().equals(VhdlEntity.class)) {
				return true;
			}
		}

		/* Test sub-circuits */
		for (CircuitState sub : s.getSubstates()) {
			if (hasVhdlComponent(sub))
				return true;
		}

		return false;
	}

	/**
	 * Test if a project has a VHDL component
	 *
	 * @param p
	 * @return
	 */
	private boolean hasVhdlComponent(Project p) {
		return hasVhdlComponent(p.getCircuitState());
	}

	public boolean isEnabled() {
		return state != State.DISABLED;
	}

	public boolean isRunning() {
		return state == State.RUNNING;
	}

	/**
	 * Receive a message from the VHDL simulator
	 *
	 * @return
	 */
	public String receive() {
		if (!isRunning())
			throw new UnsupportedOperationException();

		return socketClient.receive();
	}

	public void removeVhdlSimStateListener(VhdlSimulatorListener l) {
		listeners.remove(l);
	}

	public void reset() {
		if (isEnabled())
			socketClient.send("restart");
	}

	/**
	 * Stop and restart. If not running, just start
	 */
	public void restart() {
		switch (state) {
		case DISABLED:
		case STARTING:
		case ENABLED:
			start();
			return;

		case RUNNING:
			stop();
			start();
			break;

		default:
			throw new UnsupportedOperationException(
					"Cannot restart VHDL simulator from " + state + " state");
		}
	}

	/**
	 * Send a message to the VHDL simulator
	 *
	 * @param message
	 */
	public void send(String message) {
		if (!isRunning())
			throw new UnsupportedOperationException();

		socketClient.send(message);
	}

	public void setEnabled(Boolean enable) {
		if (enable)
			enable();
		else
			disable();
	}

	public void setState(State newState) {
		state = newState;
		fireVhdlSimStateChanged();
	}

	/**
	 * Start both TCL binder and socket server
	 */
	public void start() {

		if (!hasVhdlComponent(getProject()))
			return;

		switch (state) {
		case DISABLED:
		case STARTING:
		case RUNNING:
			return;

		case ENABLED:
			setState(State.STARTING);

			generateFiles();
			tclBinder.start();
			break;

		default:
			throw new UnsupportedOperationException(
					"Cannot start VHDL simulator from " + state + " state");
		}
	}

	/**
	 * Stop the simulator, both TCL binder and socket server
	 */
	public void stop() {

		switch (state) {
		case DISABLED:
		case ENABLED:
			return;

		case RUNNING:
			break;

		case STARTING:
			// FIXME : start a thread that waits for the simulator to finish
			// starting and then stop it
			break;

		default:
			throw new UnsupportedOperationException(
					"Cannot stop VHDL simulator from " + state + " state");
		}

		tclBinder.stop();
		socketClient.stop();

		setState(State.ENABLED);
	}

	public void tclStartCallback() {
		socketClient.start();
		setState(State.RUNNING);
	}

}
