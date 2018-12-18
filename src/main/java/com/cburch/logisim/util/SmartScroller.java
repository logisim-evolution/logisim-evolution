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
package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

/**
 * The SmartScroller will attempt to keep the viewport positioned based on the
 * users interaction with the scrollbar. The normal behaviour is to keep the
 * viewport positioned to see new data as it is dynamically added.
 *
 * Assuming vertical scrolling and data is added to the bottom:
 *
 * - when the viewport is at the bottom and new data is added, then
 * automatically scroll the viewport to the bottom - when the viewport is not at
 * the bottom and new data is added, then do nothing with the viewport
 *
 * Assuming vertical scrolling and data is added to the top:
 *
 * - when the viewport is at the top and new data is added, then do nothing with
 * the viewport - when the viewport is not at the top and new data is added,
 * then adjust the viewport to the relative position it was at before the data
 * was added
 *
 * Similiar logic would apply for horizontal scrolling.
 * 
 * http://www.camick.com/java/source/SmartScroller.java
 */
public class SmartScroller implements AdjustmentListener {
	public final static int HORIZONTAL = 0;
	public final static int VERTICAL = 1;

	public final static int START = 0;
	public final static int END = 1;

	private int viewportPosition;

	private JScrollBar scrollBar;
	private boolean adjustScrollBar = true;

	private int previousValue = -1;
	private int previousMaximum = -1;

	/**
	 * Convenience constructor. Scroll direction is VERTICAL and viewport
	 * position is at the END.
	 *
	 * @param scrollPane
	 *            the scroll pane to monitor
	 */
	public SmartScroller(JScrollPane scrollPane) {
		this(scrollPane, VERTICAL, END);
	}

	/**
	 * Convenience constructor. Scroll direction is VERTICAL.
	 *
	 * @param scrollPane
	 *            the scroll pane to monitor
	 * @param viewportPosition
	 *            valid values are START and END
	 */
	public SmartScroller(JScrollPane scrollPane, int viewportPosition) {
		this(scrollPane, VERTICAL, viewportPosition);
	}

	/**
	 * Specify how the SmartScroller will function.
	 *
	 * @param scrollPane
	 *            the scroll pane to monitor
	 * @param scrollDirection
	 *            indicates which JScrollBar to monitor. Valid values are
	 *            HORIZONTAL and VERTICAL.
	 * @param viewportPosition
	 *            indicates where the viewport will normally be positioned as
	 *            data is added. Valid values are START and END
	 */
	public SmartScroller(JScrollPane scrollPane, int scrollDirection,
			int viewportPosition) {
		if (scrollDirection != HORIZONTAL && scrollDirection != VERTICAL)
			throw new IllegalArgumentException(
					"invalid scroll direction specified");

		if (viewportPosition != START && viewportPosition != END)
			throw new IllegalArgumentException(
					"invalid viewport position specified");

		this.viewportPosition = viewportPosition;

		if (scrollDirection == HORIZONTAL)
			scrollBar = scrollPane.getHorizontalScrollBar();
		else
			scrollBar = scrollPane.getVerticalScrollBar();

		scrollBar.addAdjustmentListener(this);

		// Turn off automatic scrolling for text components

		Component view = scrollPane.getViewport().getView();

		if (view instanceof JTextComponent) {
			JTextComponent textComponent = (JTextComponent) view;
			DefaultCaret caret = (DefaultCaret) textComponent.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		}
	}

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				checkScrollBar(e);
			}
		});
	}

	/*
	 * Analyze every adjustment event to determine when the viewport needs to be
	 * repositioned.
	 */
	private void checkScrollBar(AdjustmentEvent e) {
		// The scroll bar listModel contains information needed to determine
		// whether the viewport should be repositioned or not.

		JScrollBar scrollBar = (JScrollBar) e.getSource();
		BoundedRangeModel listModel = scrollBar.getModel();
		int value = listModel.getValue();
		int extent = listModel.getExtent();
		int maximum = listModel.getMaximum();

		boolean valueChanged = previousValue != value;
		boolean maximumChanged = previousMaximum != maximum;

		// Check if the user has manually repositioned the scrollbar

		if (valueChanged && !maximumChanged) {
			if (viewportPosition == START)
				adjustScrollBar = value != 0;
			else
				adjustScrollBar = value + extent >= maximum;
		}

		// Reset the "value" so we can reposition the viewport and
		// distinguish between a user scroll and a program scroll.
		// (ie. valueChanged will be false on a program scroll)

		if (adjustScrollBar && viewportPosition == END) {
			// Scroll the viewport to the end.
			scrollBar.removeAdjustmentListener(this);
			value = maximum - extent;
			scrollBar.setValue(value);
			scrollBar.addAdjustmentListener(this);
		}

		if (adjustScrollBar && viewportPosition == START) {
			// Keep the viewport at the same relative viewportPosition
			scrollBar.removeAdjustmentListener(this);
			value = value + maximum - previousMaximum;
			scrollBar.setValue(value);
			scrollBar.addAdjustmentListener(this);
		}

		previousValue = value;
		previousMaximum = maximum;
	}
}