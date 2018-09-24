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

package com.cburch.logisim.circuit;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;

public class SubcircuitPoker extends InstancePoker {

	private static final Color MAGNIFYING_INTERIOR = new Color(200, 200, 255,
			64);
	private static final Color MAGNIFYING_INTERIOR_DOWN = new Color(128, 128,
			255, 192);

	private boolean mouseDown;

	@Override
	public Bounds getBounds(InstancePainter painter) {
		Bounds bds = painter.getInstance().getBounds();
		int cx = bds.getX() + bds.getWidth() / 2;
		int cy = bds.getY() + bds.getHeight() / 2;
		return Bounds.create(cx - 5, cy - 5, 15, 15);
	}

	private boolean isWithin(InstanceState state, MouseEvent e) {
		Bounds bds = state.getInstance().getBounds();
		int cx = bds.getX() + bds.getWidth() / 2;
		int cy = bds.getY() + bds.getHeight() / 2;
		int dx = e.getX() - cx;
		int dy = e.getY() - cy;
		return dx * dx + dy * dy <= 60;
	}

	@Override
	public void mousePressed(InstanceState state, MouseEvent e) {
		if (isWithin(state, e)) {
			mouseDown = true;
			state.getInstance().fireInvalidated();
		}
	}

	@Override
	public void mouseReleased(InstanceState state, MouseEvent e) {
		if (mouseDown) {
			mouseDown = false;
			Object sub = state.getData();
			if (e.getClickCount() == 2 && isWithin(state, e)
					&& sub instanceof CircuitState) {
				state.getProject().setCircuitState((CircuitState) sub);
			} else {
				state.getInstance().fireInvalidated();
			}
		}
	}

	@Override
	public void paint(InstancePainter painter) {
		if (painter.getDestination() instanceof Canvas
				&& painter.getData() instanceof CircuitState) {
			Bounds bds = painter.getInstance().getBounds();
			int cx = bds.getX() + bds.getWidth() / 2;
			int cy = bds.getY() + bds.getHeight() / 2;

			int tx = cx + 7;
			int ty = cy + 7;
			int[] xp = { tx - 2, cx + 13, cx + 15, tx + 2 };
			int[] yp = { ty + 2, cy + 15, cy + 13, ty - 2 };
			Graphics g = painter.getGraphics();
			if (mouseDown) {
				g.setColor(MAGNIFYING_INTERIOR_DOWN);
			} else {
				g.setColor(MAGNIFYING_INTERIOR);
			}
			g.fillOval(cx - 9, cy - 9, 18, 18);
			g.setColor(Color.BLACK);
			g.drawOval(cx - 9, cy - 9, 18, 18);
			g.fillPolygon(xp, yp, xp.length);
		}
	}
}
