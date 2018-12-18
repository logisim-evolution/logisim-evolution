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

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import javax.swing.JPanel;

import com.hepia.logisim.chronodata.SignalData;
import com.hepia.logisim.chronodata.TimelineParam;

/**
 * The chronogram timebase displayed on top of all SignalDraw
 */
public class TimelineDraw extends JPanel {

	private static final long serialVersionUID = 1L;
	private TimelineParam timelineParam;
	private SignalData clk = null;
	private int tickWidth;
	private int width;
	private int height;

	public TimelineDraw(ChronoFrame chronoFrame, int height, int tickWidth) {
		this.timelineParam = chronoFrame.getTimelineParam();
		if (this.timelineParam != null)
			this.clk = chronoFrame.getChronoData().get(
					timelineParam.getSignalClkName());

		this.width = chronoFrame.getNbrOfTick() * tickWidth - tickWidth;
		this.height = height;
		this.tickWidth = tickWidth;
		this.setMaximumSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
	}

	private void drawTimeLine(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g.drawLine(0, 5, width, 5);
		int minimalWidthToDisp = 60;
		int nbrTick = 0;
		int lastDispPos = -minimalWidthToDisp;

		if (clk != null) {
			for (int i = 1; i < clk.getSignalValues().size(); ++i) {
				// is it a clk rising edge ?
				if (clk.getSignalValues().get(i - 1).equals("0")
						&& clk.getSignalValues().get(i).equals("1")) {

					// is there enough place to display the text?
					if ((i - 1) * tickWidth - lastDispPos > minimalWidthToDisp) {
						lastDispPos = (i - 1) * tickWidth;
						g2.setStroke(new BasicStroke(2));
						g2.drawLine(lastDispPos, 6, lastDispPos, 12);
						g2.setStroke(new BasicStroke(1));
						g.drawString(getTimeString(nbrTick), lastDispPos + 3,
								20);
					}
					nbrTick++;
				}
			}
		}
	}

	private double getTimeFromFreq(int i) {
		return 1d / timelineParam.getFrequency() * i;
	}

	private String getTimeString(int i) {
		String out = "";
		double a = getTimeFromFreq(i);
		if (timelineParam.getSelectedUnit().equals("GHz")) {
			out += "ns";
		} else if (timelineParam.getSelectedUnit().equals("MHz")) {
			out += "us";
		} else if (timelineParam.getSelectedUnit().equals("KHz")) {
			out += "ms";
		} else {
			out += "s";
		}
		DecimalFormat df = new DecimalFormat("#.##");
		return df.format(a) + out;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (timelineParam != null) {
			drawTimeLine(g);
		}
	}

	public void setTickWidth(int tickWidth, int width) {
		this.tickWidth = tickWidth;
		this.width = tickWidth * width;
		setTimeLineSize(this.width);
	}

	public void setTimeLineSize(int width) {
		this.width = width - tickWidth;
		this.setMaximumSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
	}
}
