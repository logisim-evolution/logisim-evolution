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
package com.hepia.logisim.chronodata;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.TreeMap;

import com.hepia.logisim.chronogui.ChronoFrame;

/**
 * Contains all data to be plotted
 */
public class ChronoData extends TreeMap<String, SignalData> {

	private static final long serialVersionUID = 1L;
	private ArrayList<String> mSignalOrder = new ArrayList<String>();

	public ChronoData() {
	}

	/**
	 * Loads and format all the data from logisimLogFile
	 * 
	 * @throws IOException
	 */
	public ChronoData(String logisimLogFile, ChronoFrame chronoFr)
			throws IOException, NoSysclkException {
		LineNumberReader lineReader = null;
		lineReader = new LineNumberReader(new FileReader(logisimLogFile));

		ArrayList<ArrayList<String>> rawData = new ArrayList<ArrayList<String>>();
		boolean sysclkFound = false;

		// read the first line with the signal name
		// The delimiter is the tabulation
		String line = lineReader.readLine();
		String[] splittedLine = line.split("\\t");
		for (int i = 0; i < splittedLine.length; ++i) {
			ArrayList<String> v = new ArrayList<String>();
			v.add(splittedLine[i]);
			if (splittedLine[i].equals("sysclk"))
				sysclkFound = true;
			rawData.add(v);
		}

		if (!sysclkFound) {
			lineReader.close();
			throw new NoSysclkException("No sysclk signal found");
		}

		// read the tick frequency
		line = lineReader.readLine();
		try {
			chronoFr.setTimelineParam(new TimelineParam(line));
		} catch (Exception e) {
			chronoFr.setTimelineParam(null);
		}

		// read the rest of the file
		while ((line = lineReader.readLine()) != null) {
			splittedLine = line.split("\\t");
			for (int i = 0; i < splittedLine.length; ++i) {
				// if the signal added is a bus wider than 4bit, we have to
				// remove spaces
				// (there is a space every 4 bits in a bus in the log file)
				rawData.get(i).add(splittedLine[i].replaceAll("\\s", ""));
			}
		}
		lineReader.close();

		// creates the SignalData et SignalDataBus
		// and store the signal name order
		mSignalOrder = new ArrayList<String>();
		for (ArrayList<String> vs : rawData) {
			String name = vs.get(0);
			mSignalOrder.add(name);
			if (vs.get(1).length() > 1) {
				this.put(name, new SignalDataBus(name, vs));
			} else {
				this.put(name, new SignalData(name, vs));
			}
			vs.remove(0);
		}

		normalize();
	}

	public void appendValueToSignal(String signalName, String signalValue) {
		this.get(signalName).getSignalValues()
				.add(signalValue.replaceAll("\\s", ""));
	}

	/**
	 * Hide all signals that compose busName
	 */
	public void contractBus(SignalDataBus sd) {
		if (sd.getSignalValues().size() > 0) {
			int signalNbr = sd.getSignalValues().get(0).length();
			int busNamePos = (mSignalOrder.indexOf(sd.getName()));

			for (int signalI = 0; signalI < signalNbr; ++signalI) {
				String name = sd.getName() + "__s__" + signalI;
				this.remove(name);
				mSignalOrder.remove(busNamePos + 1);
			}
			sd.setExpanded(false);
		}
	}

	/**
	 * Display all signals that compose busName
	 */
	public void expandBus(SignalDataBus sd) {
		if (sd.getSignalValues().size() > 0) {
			int signalNbr = sd.getSignalValues().get(0).length();
			int busNamePos = (mSignalOrder.indexOf(sd.getName()));
			// for each signal that defines the bus

			for (int signalI = 0; signalI < signalNbr; ++signalI) {
				int bitPos = signalNbr - signalI - 1;
				ArrayList<String> sig = new ArrayList<String>();
				String name = sd.getName() + "__s__" + signalI;
				for (String s : sd.getSignalValues()) {
					sig.add(s.substring(bitPos, bitPos + 1));
				}
				// add signalData
				this.put(name, new SignalData(name, sig));
				// insert new signal in name signal order
				mSignalOrder.add(busNamePos + signalI + 1, name);
			}
			sd.setExpanded(true);
		}
	}

	public ArrayList<String> getSignalOrder() {
		return mSignalOrder;
	}

	/**
	 * Remove if the sysclk has 2 or more identical states
	 */
	private void normalize() {
		try {
			ArrayList<String> vClk = this.get("sysclk").getSignalValues();
			int i = 0;
			while (i < vClk.size() - 1) {
				if (vClk.get(i).equals(vClk.get(i + 1))) {
					for (SignalData sd : this.values()) {
						sd.getSignalValues().remove(i);
					}
				} else {
					i++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setSignalOrder(ArrayList<String> order) {
		mSignalOrder = new ArrayList<String>(order);
	}

	/**
	 * In real time mode, if a bus is expanded we need to add the new data to
	 * every signal
	 */
	public void updateRealTimeExpandedBus() {
		for (java.util.Map.Entry<String, SignalData> entry : this.entrySet()) {
			if (entry.getValue() instanceof SignalDataBus) {
				SignalDataBus sdb = (SignalDataBus) entry.getValue();
				if (sdb.isExpanded()) {
					int signalNbr = sdb.getSignalValues().get(0).length();
					for (int signalI = 0; signalI < signalNbr; ++signalI) {
						int bitPos = signalNbr - signalI - 1;
						String name = sdb.getName() + "__s__" + signalI;
						this.get(name)
								.getSignalValues()
								.add(sdb.getSignalValues()
										.get(sdb.getSignalValues().size() - 1)
										.substring(bitPos, bitPos + 1));
					}
				}
			}
		}
	}
}
