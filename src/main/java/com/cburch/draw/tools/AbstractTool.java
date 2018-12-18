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

package com.cburch.draw.tools;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Icon;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.CanvasTool;
import com.cburch.logisim.data.Attribute;

public abstract class AbstractTool extends CanvasTool {
	public static AbstractTool[] getTools(DrawingAttributeSet attrs) {
		return new AbstractTool[] { new SelectTool(), new LineTool(attrs),
				new CurveTool(attrs), new PolyTool(false, attrs),
				new RectangleTool(attrs), new RoundRectangleTool(attrs),
				new OvalTool(attrs), new PolyTool(true, attrs), };
	}

	/**
	 * This is because a popup menu may result from the subsequent mouse release
	 */
	@Override
	public void cancelMousePress(Canvas canvas) {
	}

	@Override
	public void draw(Canvas canvas, Graphics g) {
	}

	public abstract List<Attribute<?>> getAttributes();

	//
	// CanvasTool methods
	//
	@Override
	public abstract Cursor getCursor(Canvas canvas);

	public String getDescription() {
		return null;
	}

	public abstract Icon getIcon();

	@Override
	public void keyPressed(Canvas canvas, KeyEvent e) {
	}

	@Override
	public void keyReleased(Canvas canvas, KeyEvent e) {
	}

	@Override
	public void keyTyped(Canvas canvas, KeyEvent e) {
	}

	@Override
	public void mouseDragged(Canvas canvas, MouseEvent e) {
	}

	@Override
	public void mouseEntered(Canvas canvas, MouseEvent e) {
	}

	@Override
	public void mouseExited(Canvas canvas, MouseEvent e) {
	}

	@Override
	public void mouseMoved(Canvas canvas, MouseEvent e) {
	}

	@Override
	public void mousePressed(Canvas canvas, MouseEvent e) {
	}

	@Override
	public void mouseReleased(Canvas canvas, MouseEvent e) {
	}

	@Override
	public void toolDeselected(Canvas canvas) {
	}

	@Override
	public void toolSelected(Canvas canvas) {
	}
}
