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
package com.hepia.logisim.chronogui;

import javax.swing.event.EventListenerList;

import com.hepia.logisim.chronodata.SignalData;
import com.hepia.logisim.chronodata.SignalDataBus;

/**
 * Manage the events that can happends on the chronogram
 */
public class DrawAreaEventManager {

	private final EventListenerList listeners = new EventListenerList();

	public DrawAreaEventManager() {
	}

	public void addDrawAreaListener(IDrawAreaEvents listener) {
		listeners.add(IDrawAreaEvents.class, listener);
	}

	/**
	 * Fire Expand or close the selected bus
	 * 
	 * @param signalDataSource
	 *            SignalDataBus that correspond to the object that trigger the
	 *            event
	 * @param expand
	 *            if true expand the bus into multiple signals, close it
	 *            otherwise
	 */
	public void fireExpand(SignalDataBus signalDataSource, boolean expand) {
		for (IDrawAreaEvents listener : getListeners()) {
			listener.toggleBusExpand(signalDataSource, expand);
		}
	}

	/**
	 * Fire mouse dragged event
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 * @param posX
	 *            position of cursor on X axis
	 */
	public void fireMouseDragged(SignalData signalDataSource, int posX) {
		for (IDrawAreaEvents listener : getListeners()) {
			listener.mouseDragged(signalDataSource, posX);
		}
	}

	/**
	 * Fire mouse entered
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 */
	public void fireMouseEntered(SignalData signalDataSource) {
		for (IDrawAreaEvents listener : getListeners()) {
			listener.mouseEntered(signalDataSource);
		}
	}

	/**
	 * Fire mouse exited
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 */
	public void fireMouseExited(SignalData signalDataSource) {
		for (IDrawAreaEvents listener : getListeners()) {
			listener.mouseExited(signalDataSource);
		}
	}

	/**
	 * Fire mouse pressed event
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 * @param posX
	 *            position of cursor on X axis
	 */
	public void fireMousePressed(SignalData signalDataSource, int posX) {
		for (IDrawAreaEvents listener : getListeners()) {
			listener.mousePressed(signalDataSource, posX);
		}
	}

	/**
	 * Change coding format on a bus
	 * 
	 * @param signalDataSource
	 *            SignalDataBus that correspond to the bus that trigger the
	 *            event and/or the bus to apply the transformation
	 * @param format
	 *            new coding format
	 */
	public void fireSetCodingFormat(SignalDataBus signalDataSource,
			String format) {
		for (IDrawAreaEvents listener : getListeners()) {
			listener.setCodingFormat(signalDataSource, format);
		}
	}

	/**
	 * Fire zoom event
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 * @param sens
	 *            1=zoom in, -1=zoom out
	 * @param posX
	 *            position of cursor on X axis
	 */
	public void fireZoom(SignalData signalDataSource, int sens, int posX) {
		for (IDrawAreaEvents listener : getListeners()) {
			listener.zoom(signalDataSource, sens, posX);
		}
	}

	public IDrawAreaEvents[] getListeners() {
		return listeners.getListeners(IDrawAreaEvents.class);
	}

	public void removeDrawAreaListener(IDrawAreaEvents listener) {
		listeners.remove(IDrawAreaEvents.class, listener);
	}
}
