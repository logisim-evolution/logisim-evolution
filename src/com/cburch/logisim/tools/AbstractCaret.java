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

package com.cburch.logisim.tools;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cburch.logisim.data.Bounds;

public class AbstractCaret implements Caret {
	private ArrayList<CaretListener> listeners = new ArrayList<CaretListener>();
	private List<CaretListener> listenersView;
	private Bounds bds = Bounds.EMPTY_BOUNDS;

	public AbstractCaret() {
		listenersView = Collections.unmodifiableList(listeners);
	}

	// listener methods
	public void addCaretListener(CaretListener e) {
		listeners.add(e);
	}

	public void cancelEditing() {
	}

	// finishing
	public void commitText(String text) {
	}

	public void draw(Graphics g) {
	}

	public Bounds getBounds(Graphics g) {
		return bds;
	}

	protected List<CaretListener> getCaretListeners() {
		return listenersView;
	}

	// query/Graphics methods
	public String getText() {
		return "";
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
	}

	// events to handle
	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void removeCaretListener(CaretListener e) {
		listeners.remove(e);
	}

	// configuration methods
	public void setBounds(Bounds value) {
		bds = value;
	}

	public void stopEditing() {
	}
}
