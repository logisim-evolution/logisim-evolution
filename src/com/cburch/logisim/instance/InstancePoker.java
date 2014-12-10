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

package com.cburch.logisim.instance;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.cburch.logisim.data.Bounds;

public abstract class InstancePoker {
	public Bounds getBounds(InstancePainter painter) {
		return painter.getInstance().getBounds();
	}

	public boolean init(InstanceState state, MouseEvent e) {
		return true;
	}

	public void keyPressed(InstanceState state, KeyEvent e) {
	}

	public void keyReleased(InstanceState state, KeyEvent e) {
	}

	public void keyTyped(InstanceState state, KeyEvent e) {
	}

	public void mouseDragged(InstanceState state, MouseEvent e) {
	}

	public void mousePressed(InstanceState state, MouseEvent e) {
	}

	public void mouseReleased(InstanceState state, MouseEvent e) {
	}

	public void paint(InstancePainter painter) {
	}

	public void stopEditing(InstanceState state) {
	}
}
