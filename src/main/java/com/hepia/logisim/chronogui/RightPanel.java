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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import com.hepia.logisim.chronodata.SignalData;

/**
 * Chronogram's right side Panel Composed of one TimeLine on top and multiple
 * SignalDraw
 */
public class RightPanel extends ChronoPanelTemplate {

	private static final long serialVersionUID = 1L;
	private ChronoFrame mChronoFrame;
	private DrawAreaEventManager mDrawAreaEventManager;
	private TimelineDraw mTimeLine;
	private CommonPanelParam mCommonPanelParam;

	private ArrayList<SignalDraw> allSignalDraw = new ArrayList<SignalDraw>();
	private Box rightBox;
	private JLayeredPane layeredPane;
	private Cursor mCursor;

	private int mousePosXClicked = 0;
	private static final int minTickWidth = 1;
	private int tickWidth = 20;
	private int displayOffsetX = 0;

	private int globalHeight;

	/**
	 * Standard constructor
	 */
	public RightPanel(ChronoFrame chronoFrame,
			DrawAreaEventManager drawAreaEventManager) {
		this.mChronoFrame = chronoFrame;
		this.mDrawAreaEventManager = drawAreaEventManager;
		this.mCommonPanelParam = chronoFrame.getCommonPanelParam();
		this.globalHeight = mCommonPanelParam.getSignalHeight()
				* chronoFrame.getChronoData().size();
		this.setLayout(new BorderLayout());
		this.setBackground(Color.white);
		createPanel();
	}

	/**
	 * Clone constructor
	 */
	public RightPanel(RightPanel oldPanel) {
		this.mChronoFrame = oldPanel.mChronoFrame;
		this.mDrawAreaEventManager = oldPanel.mDrawAreaEventManager;
		this.mCommonPanelParam = mChronoFrame.getCommonPanelParam();
		this.globalHeight = mCommonPanelParam.getSignalHeight()
				* mChronoFrame.getChronoData().size();
		this.tickWidth = oldPanel.tickWidth;
		this.mousePosXClicked = oldPanel.mousePosXClicked;
		this.displayOffsetX = oldPanel.displayOffsetX;
		this.setLayout(new BorderLayout());
		this.setBackground(Color.white);
		createPanel();
	}

	public void adjustmentValueChanged(int value) {
		float posPercent = (float) value / (float) getSignalWidth();
		int i = Math.round(mChronoFrame.getNbrOfTick() * posPercent);
		i = i > 5 ? i - 5 : 0;
		displayOffsetX = i * tickWidth;
		for (SignalDraw sDraw : allSignalDraw) {
			sDraw.setBufferObsolete();
			sDraw.repaint();
		}
	}

	/**
	 * Creates and add all component: -timeline -all signalDraw -cursor
	 */
	private void createPanel() {
		rightBox = Box.createVerticalBox();
		rightBox.setOpaque(true);

		// Add the time line
		mTimeLine = new TimelineDraw(mChronoFrame,
				mCommonPanelParam.getHeaderHeight(), tickWidth);

		// creates the SignalDraw
		for (String signalName : mChronoFrame.getChronoData().getSignalOrder()) {
			if (!signalName.equals("sysclk"))
				allSignalDraw.add(new SignalDraw(this, mDrawAreaEventManager,
						mChronoFrame.getChronoData().get(signalName),
						mCommonPanelParam.getSignalHeight()));
		}

		// add the signals to the box
		for (SignalDraw sDraw : allSignalDraw) {
			rightBox.add(sDraw);
		}

		// add the cursor
		mCursor = new Cursor();

		// creates a JLayeredPane, to put the Cursor in front of the SignalDraw
		// and the timeline
		layeredPane = new JLayeredPane();

		defineSizes();

		layeredPane.add(mCursor, new Integer(1));
		layeredPane.add(mTimeLine, new Integer(0));
		layeredPane.add(rightBox, new Integer(0));

		this.add(layeredPane, BorderLayout.WEST);
	}

	private void defineSizes() {
		int totalWidth = tickWidth * mChronoFrame.getNbrOfTick();
		layeredPane.setPreferredSize(new Dimension(totalWidth, globalHeight));
		rightBox.setBounds(0, mCommonPanelParam.getHeaderHeight(), totalWidth,
				globalHeight);
		mTimeLine.setBounds(0, 0, totalWidth,
				mCommonPanelParam.getHeaderHeight());
		mCursor.setBounds(0, 0, totalWidth, globalHeight);
	}

	/**
	 * Set the cursor position
	 */
	public void drawVerticalMouseClicked() {
		drawVerticalMouseClicked(mousePosXClicked);
	}

	/**
	 * Set the cursor position
	 */
	public void drawVerticalMouseClicked(int posX) {
		mCursor.setPosition(posX);
		mCursor.repaint();
		mousePosXClicked = posX;
	}

	public int getDisplayOffsetX() {
		return displayOffsetX;
	}

	public int getMousePosXClicked() {
		return mousePosXClicked;
	}

	public int getSignalWidth() {
		return mChronoFrame.getNbrOfTick() * tickWidth;
	}

	public int getTickWidth() {
		return tickWidth;
	}

	public int getVisibleWidth() {
		return mChronoFrame.getVisibleSignalsWidth();
	}

    public int getTotalWidth() {
        return (mChronoFrame.getNbrOfTick() * tickWidth);
    }

    public int getTotalHeight() {
        return globalHeight;
    }
	/**
	 * Highlight a signal in bold
	 */
	public void highlight(SignalData signalToHighlight) {
		for (SignalDraw sDraw : allSignalDraw) {
			sDraw.highlight(sDraw.getSignalData() == signalToHighlight);
		}
	}

    public ArrayList<SignalDraw> getAllSdraws() {
        return allSignalDraw;
    }

	/**
	 * Repaint the cursor and all signalDraw
	 */
	public void repaintAll() {
		mCursor.repaint();
		int width;
		for (SignalDraw sDraw : allSignalDraw) {
			sDraw.setBufferObsolete();
			sDraw.repaint();
			if (mChronoFrame.isRealTimeMode()) {
				width = getSignalWidth();
				sDraw.setSignalDrawSize(width,
						mCommonPanelParam.getSignalHeight());
				mTimeLine.setTimeLineSize(width);
				defineSizes();
			}
		}
	}

	public void zoom(int sens, int posX) {
		int nbrOfTick = mousePosXClicked / tickWidth;

		tickWidth += sens;
		if (tickWidth <= minTickWidth)
			tickWidth = minTickWidth;

		// make the mousePosXClicked follow the zoom
		int newPosX = nbrOfTick * tickWidth;
		mousePosXClicked = newPosX;
		// set the cusor position
		mCursor.setPosition(newPosX);

		// Scrollbar follow the zoom
		int scrollBarCursorPos = mCursor.getPosition()
				- (mChronoFrame.getVisibleSignalsWidth() / 2);

		// zoom on every signals
		for (SignalDraw sDraw : allSignalDraw) {
			sDraw.setTickWidth(tickWidth);
		}

		// zoom on the timeline
		mTimeLine.setTickWidth(tickWidth, mChronoFrame.getNbrOfTick());

		defineSizes();

		// force redraw everything
		SwingUtilities.updateComponentTreeUI(mChronoFrame);

		// scrollbar position
		mChronoFrame.setScrollbarPosition(scrollBarCursorPos);
	}
}