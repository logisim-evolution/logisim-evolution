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

package com.cburch.gray;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

/**
 * When the user clicks a counter using the Poke Tool, a CounterPoker object is
 * created, and that object will handle all user events. Note that CounterPoker
 * is a class specific to GrayCounter, and that it must be a subclass of
 * InstancePoker in the com.cburch.logisim.instance package.
 */
public class CounterPoker extends InstancePoker {
	public CounterPoker() {
	}

	/**
	 * Determines whether the location the mouse was pressed should result in
	 * initiating a poke.
	 */
	@Override
	public boolean init(InstanceState state, MouseEvent e) {
		return state.getInstance().getBounds().contains(e.getX(), e.getY());
		// Anywhere in the main rectangle initiates the poke. The user might
		// have clicked within a label, but that will be outside the bounds.
	}

	/** Processes a key by just adding it onto the end of the current value. */
	@Override
	public void keyTyped(InstanceState state, KeyEvent e) {
		// convert it to a hex digit; if it isn't a hex digit, abort.
		int val = Character.digit(e.getKeyChar(), 16);
		BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
		if (val < 0 || (val & width.getMask()) != val)
			return;

		// compute the next value
		CounterData cur = CounterData.get(state, width);
		int newVal = (cur.getValue().toIntValue() * 16 + val) & width.getMask();
		Value newValue = Value.createKnown(width, newVal);
		cur.setValue(newValue);
		state.fireInvalidated();

		// You might be tempted to propagate the value immediately here, using
		// state.setPort. However, the circuit may currently be propagating in
		// another thread, and invoking setPort directly could interfere with
		// that. Using fireInvalidated notifies the propagation thread to
		// invoke propagate on the counter at its next opportunity.
	}

	/**
	 * Draws an indicator that the caret is being selected. Here, we'll draw a
	 * red rectangle around the value.
	 */
	@Override
	public void paint(InstancePainter painter) {
		Bounds bds = painter.getBounds();
		BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
		int len = (width.getWidth() + 3) / 4;

		Graphics g = painter.getGraphics();
		g.setColor(Color.RED);
		int wid = 7 * len + 2; // width of caret rectangle
		int ht = 16; // height of caret rectangle
		g.drawRect(bds.getX() + (bds.getWidth() - wid) / 2,
				bds.getY() + (bds.getHeight() - ht) / 2, wid, ht);
		g.setColor(Color.BLACK);
	}
}
