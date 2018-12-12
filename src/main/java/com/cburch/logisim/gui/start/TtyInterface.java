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

package com.cburch.logisim.gui.start;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Propagator;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.FileStatistics;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.io.Keyboard;
import com.cburch.logisim.std.io.Tty;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UniquelyNamedThread;

public class TtyInterface {

	// It's possible to avoid using the separate thread using
	// System.in.available(),
	// but this doesn't quite work because on some systems, the keyboard input
	// is not interactively echoed until System.in.read() is invoked.
	private static class StdinThread extends UniquelyNamedThread {
		private LinkedList<char[]> queue; // of char[]

		public StdinThread() {
			super("TtyInterface-StdInThread");
			queue = new LinkedList<char[]>();
		}

		public char[] getBuffer() {
			synchronized (queue) {
				if (queue.isEmpty()) {
					return null;
				} else {
					return queue.removeFirst();
				}
			}
		}

		@Override
		public void run() {
			InputStreamReader stdin = new InputStreamReader(System.in);
			char[] buffer = new char[32];
			while (true) {
				try {
					int nbytes = stdin.read(buffer);
					if (nbytes > 0) {
						char[] add = new char[nbytes];
						System.arraycopy(buffer, 0, add, 0, nbytes);
						synchronized (queue) {
							queue.addLast(add);
						}
					}
				} catch (IOException e) {
				}
			}
		}
	}

	private static int countDigits(int num) {
		int digits = 1;
		int lessThan = 10;
		while (num >= lessThan) {
			digits++;
			lessThan *= 10;
		}
		return digits;
	}

	private static void displaySpeed(long tickCount, long elapse) {
		double hertz = (double) tickCount / elapse * 1000.0;
		double precision;
		if (hertz >= 100)
			precision = 1.0;
		else if (hertz >= 10)
			precision = 0.1;
		else if (hertz >= 1)
			precision = 0.01;
		else if (hertz >= 0.01)
			precision = 0.0001;
		else
			precision = 0.0000001;
		hertz = (int) (hertz / precision) * precision;
		String hertzStr = hertz == (int) hertz ? "" + (int) hertz : "" + hertz;
		Object[] paramArray = { StringUtil.format(Strings.get("ttySpeedMsg")),
				hertzStr, tickCount, elapse };
		logger.info("{}", paramArray);
	}

	private static void displayStatistics(LogisimFile file) {
		FileStatistics stats = FileStatistics.compute(file,
				file.getMainCircuit());
		FileStatistics.Count total = stats.getTotalWithSubcircuits();
		int maxName = 0;
		for (FileStatistics.Count count : stats.getCounts()) {
			int nameLength = count.getFactory().getDisplayName().length();
			if (nameLength > maxName)
				maxName = nameLength;
		}
		String fmt = "%" + countDigits(total.getUniqueCount()) + "d\t" + "%"
				+ countDigits(total.getRecursiveCount()) + "d\t";
		String fmtNormal = fmt + "%-" + maxName + "s\t%s\n";
		for (FileStatistics.Count count : stats.getCounts()) {
			Library lib = count.getLibrary();
			String libName = lib == null ? "-" : lib.getDisplayName();
			System.out.printf(fmtNormal, // OK
					Integer.valueOf(count.getUniqueCount()), Integer
							.valueOf(count.getRecursiveCount()), count
							.getFactory().getDisplayName(), libName);
		}
		FileStatistics.Count totalWithout = stats.getTotalWithoutSubcircuits();
		System.out.printf(
				fmt + "%s\n", // OK
				Integer.valueOf(totalWithout.getUniqueCount()),
				Integer.valueOf(totalWithout.getRecursiveCount()),
				Strings.get("statsTotalWithout"));
		System.out.printf(
				fmt + "%s\n", // OK
				Integer.valueOf(total.getUniqueCount()),
				Integer.valueOf(total.getRecursiveCount()),
				Strings.get("statsTotalWith"));
	}

	private static void displayTableRow(ArrayList<Value> prevOutputs,
			ArrayList<Value> curOutputs) {
		boolean shouldPrint = false;
		if (prevOutputs == null) {
			shouldPrint = true;
		} else {
			for (int i = 0; i < curOutputs.size(); i++) {
				Value a = prevOutputs.get(i);
				Value b = curOutputs.get(i);
				if (!a.equals(b)) {
					shouldPrint = true;
					break;
				}
			}
		}
		if (shouldPrint) {
			for (int i = 0; i < curOutputs.size(); i++) {
				if (i != 0)
					System.out.print("\t"); // OK
				System.out.print(curOutputs.get(i)); // OK
			}
			System.out.println(); // OK
		}
	}

	private static void ensureLineTerminated() {
		if (!lastIsNewline) {
			lastIsNewline = true;
			System.out.print('\n'); // OK
		}
	}

	private static boolean loadRam(CircuitState circState, File loadFile)
			throws IOException {
		if (loadFile == null)
			return false;

		boolean found = false;
		for (Component comp : circState.getCircuit().getNonWires()) {
			if (comp.getFactory() instanceof Ram) {
				Ram ramFactory = (Ram) comp.getFactory();
				InstanceState ramState = circState.getInstanceState(comp);
				ramFactory.loadImage(ramState, loadFile);
				found = true;
			}
		}

		for (CircuitState sub : circState.getSubstates()) {
			found |= loadRam(sub, loadFile);
		}
		return found;
	}

	private static boolean prepareForTty(CircuitState circState,
			ArrayList<InstanceState> keybStates) {
		boolean found = false;
		for (Component comp : circState.getCircuit().getNonWires()) {
			Object factory = comp.getFactory();
			if (factory instanceof Tty) {
				Tty ttyFactory = (Tty) factory;
				InstanceState ttyState = circState.getInstanceState(comp);
				ttyFactory.sendToStdout(ttyState);
				found = true;
			} else if (factory instanceof Keyboard) {
				keybStates.add(circState.getInstanceState(comp));
				found = true;
			}
		}

		for (CircuitState sub : circState.getSubstates()) {
			found |= prepareForTty(sub, keybStates);
		}
		return found;
	}

	public static void run(Startup args) {
		File fileToOpen = args.getFilesToOpen().get(0);
		Loader loader = new Loader(null);
		LogisimFile file;
		try {
			file = loader.openLogisimFile(fileToOpen, args.getSubstitutions());
		} catch (LoadFailedException e) {
			logger.error("{}",
					Strings.get("ttyLoadError", fileToOpen.getName()));

			System.exit(-1);
			return;
		}

		int format = args.getTtyFormat();
		if ((format & FORMAT_STATISTICS) != 0) {
			format &= ~FORMAT_STATISTICS;
			displayStatistics(file);
		}
		if (format == 0) { // no simulation remaining to perform, so just exit
			System.exit(0);
		}

		Project proj = new Project(file);
		Circuit circuit = file.getMainCircuit();
		Map<Instance, String> pinNames = Analyze.getPinLabels(circuit);
		ArrayList<Instance> outputPins = new ArrayList<Instance>();
		Instance haltPin = null;
		for (Map.Entry<Instance, String> entry : pinNames.entrySet()) {
			Instance pin = entry.getKey();
			String pinName = entry.getValue();
			if (!Pin.FACTORY.isInputPin(pin)) {
				outputPins.add(pin);
				if (pinName.equals("halt")) {
					haltPin = pin;
				}
			}
		}

		CircuitState circState = new CircuitState(proj, circuit);
		// we have to do our initial propagation before the simulation starts -
		// it's necessary to populate the circuit with substates.
		circState.getPropagator().propagate();
		if (args.getLoadFile() != null) {
			try {
				boolean loaded = loadRam(circState, args.getLoadFile());
				if (!loaded) {
					logger.error("{}", Strings.get("loadNoRamError"));
					System.exit(-1);
				}
			} catch (IOException e) {
				logger.error("{}: {}", Strings.get("loadIoError"), e.toString());
				System.exit(-1);
			}
		}
		int ttyFormat = args.getTtyFormat();
		int simCode = runSimulation(circState, outputPins, haltPin, ttyFormat);
		System.exit(simCode);
	}

	private static int runSimulation(CircuitState circState,
			ArrayList<Instance> outputPins, Instance haltPin, int format) {
		boolean showTable = (format & FORMAT_TABLE) != 0;
		boolean showSpeed = (format & FORMAT_SPEED) != 0;
		boolean showTty = (format & FORMAT_TTY) != 0;
		boolean showHalt = (format & FORMAT_HALT) != 0;

		ArrayList<InstanceState> keyboardStates = null;
		StdinThread stdinThread = null;
		if (showTty) {
			keyboardStates = new ArrayList<InstanceState>();
			boolean ttyFound = prepareForTty(circState, keyboardStates);
			if (!ttyFound) {
				logger.error("{}", Strings.get("ttyNoTtyError"));
				System.exit(-1);
			}
			if (keyboardStates.isEmpty()) {
				keyboardStates = null;
			} else {
				stdinThread = new StdinThread();
				stdinThread.start();
			}
		}

		int retCode;
		long tickCount = 0;
		long start = System.currentTimeMillis();
		boolean halted = false;
		ArrayList<Value> prevOutputs = null;
		Propagator prop = circState.getPropagator();
		while (true) {
			ArrayList<Value> curOutputs = new ArrayList<Value>();
			for (Instance pin : outputPins) {
				InstanceState pinState = circState.getInstanceState(pin);
				Value val = Pin.FACTORY.getValue(pinState);
				if (pin == haltPin) {
					halted |= val.equals(Value.TRUE);
				} else if (showTable) {
					curOutputs.add(val);
				}
			}
			if (showTable) {
				displayTableRow(prevOutputs, curOutputs);
			}

			if (halted) {
				retCode = 0; // normal exit
				break;
			}
			if (prop.isOscillating()) {
				retCode = 1; // abnormal exit
				break;
			}
			if (keyboardStates != null) {
				char[] buffer = stdinThread.getBuffer();
				if (buffer != null) {
					for (InstanceState keyState : keyboardStates) {
						Keyboard.addToBuffer(keyState, buffer);
					}
				}
			}
			prevOutputs = curOutputs;
			tickCount++;
			prop.tick();
			prop.propagate();
		}
		long elapse = System.currentTimeMillis() - start;
		if (showTty)
			ensureLineTerminated();
		if (showHalt || retCode != 0) {
			if (retCode == 0) {
				logger.error("{}", Strings.get("ttyHaltReasonPin"));
			} else if (retCode == 1) {
				logger.error("{}", Strings.get("ttyHaltReasonOscillation"));
			}
		}
		if (showSpeed) {
			displaySpeed(tickCount, elapse);
		}
		return retCode;
	}

	public static void sendFromTty(char c) {
		lastIsNewline = c == '\n';
		System.out.print(c); // OK
	}

	final static Logger logger = LoggerFactory.getLogger(TtyInterface.class);

	public static final int FORMAT_TABLE = 1;

	public static final int FORMAT_SPEED = 2;

	public static final int FORMAT_TTY = 4;

	public static final int FORMAT_HALT = 8;

	public static final int FORMAT_STATISTICS = 16;

	private static boolean lastIsNewline = true;
}
