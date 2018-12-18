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

import com.hepia.logisim.chronodata.SignalData;
import com.hepia.logisim.chronodata.SignalDataBus;

/**
 * Handles graphical actions and events on Chronoframe Left area: SignalNames
 * RightArea: TimeLine and SignalDraw
 */
public class DrawAreaManager implements IDrawAreaEvents {

	private ChronoFrame mChronoFrame;

	public DrawAreaManager(ChronoFrame chronoFrame) {
		mChronoFrame = chronoFrame;
	}

	/**
	 * Draw the red vertical line on every signal If no PoxX is given, redraw
	 * the line at the same position
	 */
	public void drawVerticalMouseClicked() {
		if (mChronoFrame.getRightPanel() != null) {
			mChronoFrame.getRightPanel().drawVerticalMouseClicked();
		}
	}

	/**
	 * Draw the red vertical line on every signal
	 * 
	 * @param posX
	 *            position of the red vertical line
	 */
	public void drawVerticalMouseClicked(int posX) {
		if (mChronoFrame.getRightPanel() != null) {
			mChronoFrame.getRightPanel().drawVerticalMouseClicked(posX);
		}
	}

	/**
	 * Highlight a signal in bold
	 */
	public void highlight(SignalData signalToHighlight) {
		if (mChronoFrame.getRightPanel() != null
				&& mChronoFrame.getLeftPanel() != null) {
			mChronoFrame.getRightPanel().highlight(signalToHighlight);
			mChronoFrame.getLeftPanel().highlight(signalToHighlight);
		}
	}

	/**
	 * Mouse dragged
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 * @param posX
	 *            position of cursor on X axis
	 */
	@Override
	public void mouseDragged(SignalData signalDataSource, int posX) {
		setSignalsValues(posX);
		drawVerticalMouseClicked(posX);
	}

	/**
	 * Mouse entered
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 */
	@Override
	public void mouseEntered(SignalData signalDataSource) {
		highlight(signalDataSource);
	}

	/**
	 * Mouse exited
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 */
	@Override
	public void mouseExited(SignalData signalDataSource) {
	}

	// ////////////////////////////
	// Event from IDrawAreaEvents//
	// ////////////////////////////

	/**
	 * Mouse pressed
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 * @param posX
	 *            position of cursor on X axis
	 */
	@Override
	public void mousePressed(SignalData signalDataSource, int posX) {
		setSignalsValues(posX);
		drawVerticalMouseClicked(posX);
	}

	/**
	 * Refresh the display of each signal value in the left bar
	 */
	public void refreshSignalsValues() {
		if (mChronoFrame.getLeftPanel() != null
				&& mChronoFrame.getRightPanel() != null) {
			mChronoFrame.getLeftPanel().refreshSignalsValues();
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
	@Override
	public void setCodingFormat(SignalDataBus signalDataSource, String format) {
		signalDataSource.setFormat(format);
		mChronoFrame.repaintAll(true);
	}

	/**
	 * Refresh the display of each signal value in the left bar
	 * 
	 * @param posX
	 *            the position of the cursor. it defines the value to display
	 */
	public void setSignalsValues(int posX) {
		if (mChronoFrame.getLeftPanel() != null
				&& mChronoFrame.getRightPanel() != null) {
			int tickWidth = mChronoFrame.getRightPanel().getTickWidth();
			int elementPosition = (posX + tickWidth) / tickWidth;
			mChronoFrame.getLeftPanel().setSignalsValues(elementPosition);
		}
	}

	/**
	 * Expand or close a bus
	 * 
	 * @param signalDataSource
	 *            SignalDataBus that correspond to the bus that trigger the
	 *            event and/or the bus to apply the transformation
	 * @param expand
	 *            if true expand the bus into multiple signals, close it
	 *            otherwise
	 */
	@Override
	public void toggleBusExpand(SignalDataBus signalDataSource, boolean expand) {
		mChronoFrame.toggleBusExpand(signalDataSource, expand);
	}

	/**
	 * Zoom in or zoom out the signal Focus on the red vertical line
	 * (mousePosXClicked)
	 */
	public void zoom(int sens, int posX) {
		if (mChronoFrame.getRightPanel() != null) {
			mChronoFrame.getRightPanel().zoom(sens, posX);
		}
	}

	/**
	 * Zoom
	 * 
	 * @param signalDataSource
	 *            SignalData that correspond to the object that trigger the
	 *            event
	 * @param sens
	 *            1=zoom in, -1=zoom out
	 * @param posX
	 *            position of cursor on X axis
	 */
	@Override
	public void zoom(SignalData signalDataSource, int sens, int val) {
		zoom(sens, val);
	}
}
