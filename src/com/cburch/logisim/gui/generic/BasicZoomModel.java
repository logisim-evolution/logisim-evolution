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

package com.cburch.logisim.gui.generic;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.cburch.logisim.prefs.PrefMonitor;

public class BasicZoomModel implements ZoomModel {
	private double[] zoomOptions;

	private PropertyChangeSupport support;
	private double zoomFactor;
	private boolean showGrid;

	public BasicZoomModel(PrefMonitor<Boolean> gridPref,
			PrefMonitor<Double> zoomPref, double[] zoomOpts) {
		zoomOptions = zoomOpts;
		support = new PropertyChangeSupport(this);
		zoomFactor = 1.0;
		showGrid = true;

		setZoomFactor(zoomPref.get().doubleValue());
		setShowGrid(gridPref.getBoolean());
	}

	public void addPropertyChangeListener(String prop, PropertyChangeListener l) {
		support.addPropertyChangeListener(prop, l);
	}

	public boolean getShowGrid() {
		return showGrid;
	}

	public double getZoomFactor() {
		return zoomFactor;
	}

	public double[] getZoomOptions() {
		return zoomOptions;
	}

	public void removePropertyChangeListener(String prop,
			PropertyChangeListener l) {
		support.removePropertyChangeListener(prop, l);
	}

	public void setShowGrid(boolean value) {
		if (value != showGrid) {
			showGrid = value;
			support.firePropertyChange(ZoomModel.SHOW_GRID, !value, value);
		}
	}

	public void setZoomFactor(double value) {
		double oldValue = zoomFactor;
		if (value != oldValue) {
			zoomFactor = value;
			support.firePropertyChange(ZoomModel.ZOOM,
					Double.valueOf(oldValue), Double.valueOf(value));
		}
	}
}
