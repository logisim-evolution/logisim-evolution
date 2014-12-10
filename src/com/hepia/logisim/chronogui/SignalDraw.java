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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import com.hepia.logisim.chronodata.SignalData;
import com.hepia.logisim.chronodata.SignalDataBus;

/**
 * Draw a single signal or bus in the chronogram right area
 */
public class SignalDraw extends JPanel {

	private class MyListener implements MouseListener, MouseMotionListener,
			MouseWheelListener {
		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			int posX = e.getX();
			if (posX < 0)
				posX = 0;
			if (posX > getWidth())
				posX = getWidth() - 1;
			mDrawAreaEventManager.fireMouseDragged(mSignalData, posX);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			mDrawAreaEventManager.fireMouseEntered(mSignalData);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			mDrawAreaEventManager.fireMouseExited(mSignalData);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			int posX = e.getX() >= 0 ? e.getX() : 0;
			mDrawAreaEventManager.fireMousePressed(mSignalData, posX);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (e.getWheelRotation() > 0)
				mDrawAreaEventManager.fireZoom(mSignalData, -1, e.getPoint().x);
			else
				mDrawAreaEventManager.fireZoom(mSignalData, 1, e.getPoint().x);
		}
	}

	private static final long serialVersionUID = 1L;
	private int tickWidth;
	private int busCrossingPosition;
	private Color lightGray = new Color(180, 180, 180, 100);

	private int lineTickness = 1;
	private int lowPos;
	private int highPos;
	private int width = 10;

	private int height;
	private BufferedImage signalDrawBuffered;

	private boolean isBufferObsolete = true;
	private SignalData mSignalData;
	private DrawAreaEventManager mDrawAreaEventManager;

	private RightPanel mRightPanel;

	private MyListener myListener = new MyListener();

	public SignalDraw(RightPanel rightPanel,
			DrawAreaEventManager drawAreaEventManager, SignalData signalData,
			int height) {
		this.mRightPanel = rightPanel;
		this.mDrawAreaEventManager = drawAreaEventManager;
		this.mSignalData = signalData;
		this.tickWidth = rightPanel.getTickWidth();
		this.width = tickWidth * signalData.getSignalValues().size();
		if (this.width < 10)
			this.width = 10;

		this.height = height;
		this.busCrossingPosition = computeBusCrossingPosition(tickWidth);

		this.lowPos = height - 6;
		this.highPos = 6;

		this.setBackground(Color.white);
		this.setMaximumSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
		this.setDoubleBuffered(true);

		this.addMouseListener(myListener);
		this.addMouseMotionListener(myListener);
		this.addMouseWheelListener(myListener);
		this.addMouseListener(new PopupMenu(drawAreaEventManager, signalData));
	}

	/**
	 * Compute the size of the cross (when a bus value change)
	 */
	private int computeBusCrossingPosition(int tickWidth) {
		return tickWidth - 5 < 1 ? 0 : 5;
	}

	/**
	 * Draw the signals and buses
	 */
	private void drawSignal(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(new BasicStroke(lineTickness));
		int middleHeight = getHeight() / 2;

		int posX = 0;
		String prec, suiv;

		// get the index of data in SignalData that correspond to the display
		float posPercent = (float) mRightPanel.getDisplayOffsetX()
				/ (float) mRightPanel.getSignalWidth();
		int i = Math.round(mSignalData.getSignalValues().size() * posPercent);

		// drawing
		prec = mSignalData.getSignalValues().get(i++);
		while (posX < mRightPanel.getDisplayOffsetX() + getVisibleRect().width
				+ (10 * tickWidth)
				&& i < mSignalData.getSignalValues().size()) {
			suiv = mSignalData.getSignalValues().get(i++);

			String transi = prec + suiv;
			if (suiv.contains("E")) {
				g.setColor(Color.red);
				g.drawLine(posX, highPos, posX + tickWidth, middleHeight);
				g.drawLine(posX, middleHeight, posX + tickWidth, highPos);
				g.drawLine(posX, middleHeight, posX + tickWidth, lowPos);
				g.drawLine(posX, lowPos, posX + tickWidth, middleHeight);
				g.setColor(Color.black);
			} else if (suiv.contains("x")) {
				g.setColor(Color.blue);
				g.drawLine(posX, highPos, posX + tickWidth, middleHeight);
				g.drawLine(posX, middleHeight, posX + tickWidth, highPos);
				g.drawLine(posX, middleHeight, posX + tickWidth, lowPos);
				g.drawLine(posX, lowPos, posX + tickWidth, middleHeight);
				g.setColor(Color.black);
			} else if (suiv.equals("0")) {
				g.drawLine(posX, lowPos, posX + tickWidth, lowPos);
			} else if (suiv.equals("1")) {
				g.setColor(lightGray);
				g.fillRect(posX + 1, highPos, tickWidth, lowPos - highPos);
				g.setColor(Color.black);
				g.drawLine(posX, highPos, posX + tickWidth, highPos);
			}

			else {
				if (mSignalData instanceof SignalDataBus) {
					SignalDataBus sdb = (SignalDataBus) mSignalData;
					// first value
					if (i == 2)
						g.drawString(sdb.getValueInFormat(suiv), posX + 2,
								getHeight() / 2);
					// bus transition
					if (!suiv.contains("x") && !suiv.contains("E")
							&& !suiv.equals(prec)) {
						g.drawLine(posX, lowPos, posX + busCrossingPosition,
								highPos);
						g.drawLine(posX, highPos, posX + busCrossingPosition,
								lowPos);
						g.drawLine(posX + busCrossingPosition, highPos, posX
								+ tickWidth, highPos);
						g.drawLine(posX + busCrossingPosition, lowPos, posX
								+ tickWidth, lowPos);
						g.drawString(sdb.getValueInFormat(suiv), posX
								+ tickWidth, getHeight() / 2);
					} else {
						g.drawLine(posX, lowPos, posX + tickWidth, lowPos);
						g.drawLine(posX, highPos, posX + tickWidth, highPos);
					}
				}
			}

			// transition
			if (transi.equals("10")) {
				g.drawLine(posX, highPos, posX, lowPos);
			} else if (transi.equals("01")) {
				g.drawLine(posX, lowPos, posX, highPos);
			}

			prec = suiv;
			posX += tickWidth;
		}
	}

	public SignalData getSignalData() {
		return mSignalData;
	}

	/**
	 * if on, the signal is displayed thicker
	 */
	public void highlight(boolean on) {
		if (lineTickness == 2) {
			if (!on) {
				isBufferObsolete = true;
				lineTickness = 1;
				this.repaint();
			}
		} else {
			if (on) {
				isBufferObsolete = true;
				lineTickness = 2;
				this.repaint();
			}
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (mSignalData.getSignalValues().size() > 1) {
			Graphics2D g2 = (Graphics2D) g;

			// if scroll or zoom, redraw everything into buffer
			if (isBufferObsolete) {
				signalDrawBuffered = (BufferedImage) (this.createImage(
						mRightPanel.getVisibleWidth() * 2, height));
				Graphics2D g2a = signalDrawBuffered.createGraphics();
				g2a.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
						RenderingHints.VALUE_STROKE_DEFAULT);
				drawSignal(g2a);
				isBufferObsolete = false;
			}
			g2.drawImage(signalDrawBuffered, null,
					mRightPanel.getDisplayOffsetX(), 0);
		}
	}

	/**
	 * Call this function when the drawed signal is outdated ex: after zoom or
	 * scroll.
	 */
	public void setBufferObsolete() {
		isBufferObsolete = true;
	}

	public void setSignalDrawSize(int width, int height) {
		this.width = width;
		this.height = height;
		this.setMaximumSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
	}

	public void setTickWidth(int tickWidth) {
		isBufferObsolete = true;
		this.tickWidth = tickWidth;
		this.busCrossingPosition = computeBusCrossingPosition(tickWidth);
		int width = tickWidth * mSignalData.getSignalValues().size();
		setSignalDrawSize(width, height);
	}
}
