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

package com.cburch.logisim.analyze.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;

class TruthTableMouseListener implements MouseListener {
	private int cellX;
	private int cellY;
	private Entry oldValue;
	private Entry newValue;

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent event) {
		TruthTablePanel source = (TruthTablePanel) event.getSource();
		TruthTable model = source.getTruthTable();
		int cols = model.getInputColumnCount() + model.getOutputColumnCount();
		int rows = model.getRowCount();
		cellX = source.getOutputColumn(event);
		cellY = source.getRow(event);
		if (cellX < 0 || cellY < 0 || cellX >= cols || cellY >= rows)
			return;
		oldValue = source.getTruthTable().getOutputEntry(cellY, cellX);
		if (oldValue == Entry.ZERO)
			newValue = Entry.ONE;
		else if (oldValue == Entry.ONE)
			newValue = Entry.DONT_CARE;
		else
			newValue = Entry.ZERO;
		source.setEntryProvisional(cellY, cellX, newValue);
	}

	public void mouseReleased(MouseEvent event) {
		TruthTablePanel source = (TruthTablePanel) event.getSource();
		TruthTable model = source.getTruthTable();
		int cols = model.getInputColumnCount() + model.getOutputColumnCount();
		int rows = model.getRowCount();
		if (cellX < 0 || cellY < 0 || cellX >= cols || cellY >= rows)
			return;

		int x = source.getOutputColumn(event);
		int y = source.getRow(event);
		TruthTable table = source.getTruthTable();
		if (x == cellX && y == cellY) {
			table.setOutputEntry(y, x, newValue);
		}
		source.setEntryProvisional(cellY, cellX, null);
		cellX = -1;
		cellY = -1;
	}
}
