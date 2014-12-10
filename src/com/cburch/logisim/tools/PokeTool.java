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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.Icons;

public class PokeTool extends Tool {
	private class Listener implements CircuitListener {
		public void circuitChanged(CircuitEvent event) {
			Circuit circ = pokedCircuit;
			if (event.getCircuit() == circ
					&& circ != null
					&& (event.getAction() == CircuitEvent.ACTION_REMOVE || event
							.getAction() == CircuitEvent.ACTION_CLEAR)
					&& !circ.contains(pokedComponent)) {
				removeCaret(false);
			}
		}
	}

	private static class WireCaret extends AbstractCaret {
		//
		Canvas canvas;
		Wire wire;
		int x;
		int y;

		WireCaret(Canvas c, Wire w, int x, int y, AttributeSet opts) {
			canvas = c;
			wire = w;
			this.x = x;
			this.y = y;
			// this.opts = opts;
		}

		@Override
		public void draw(Graphics g) {
			Value v = canvas.getCircuitState().getValue(wire.getEnd0());
			RadixOption radix1 = RadixOption
					.decode(AppPreferences.POKE_WIRE_RADIX1.get());
			RadixOption radix2 = RadixOption
					.decode(AppPreferences.POKE_WIRE_RADIX2.get());
			if (radix1 == null)
				radix1 = RadixOption.RADIX_2;
			String vStr = radix1.toString(v);
			if (radix2 != null && v.getWidth() > 1) {
				vStr += " / " + radix2.toString(v);
			}

			FontMetrics fm = g.getFontMetrics();
			g.setColor(caretColor);
			g.fillRect(x + 2, y + 2, fm.stringWidth(vStr) + 4, fm.getAscent()
					+ fm.getDescent() + 4);
			g.setColor(Color.BLACK);
			g.drawRect(x + 2, y + 2, fm.stringWidth(vStr) + 4, fm.getAscent()
					+ fm.getDescent() + 4);
			g.fillOval(x - 2, y - 2, 5, 5);
			g.drawString(vStr, x + 4, y + 4 + fm.getAscent());
		}
	}

	private static final Icon toolIcon = Icons.getIcon("poke.gif");

	private static final Color caretColor = new Color(255, 255, 150);

	private static Cursor cursor = Cursor
			.getPredefinedCursor(Cursor.HAND_CURSOR);

	private Listener listener;
	private Circuit pokedCircuit;
	private Component pokedComponent;
	private Caret pokeCaret;

	public PokeTool() {
		this.listener = new Listener();
	}

	@Override
	public void deselect(Canvas canvas) {
		removeCaret(true);
		canvas.setHighlightedWires(WireSet.EMPTY);
	}

	@Override
	public void draw(Canvas canvas, ComponentDrawContext context) {
		if (pokeCaret != null)
			pokeCaret.draw(context.getGraphics());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PokeTool;
	}

	@Override
	public Cursor getCursor() {
		return cursor;
	}

	@Override
	public String getDescription() {
		return Strings.get("pokeToolDesc");
	}

	@Override
	public String getDisplayName() {
		return Strings.get("pokeTool");
	}

	@Override
	public String getName() {
		return "Poke Tool";
	}

	@Override
	public int hashCode() {
		return PokeTool.class.hashCode();
	}

	@Override
	public void keyPressed(Canvas canvas, KeyEvent e) {
		if (pokeCaret != null) {
			pokeCaret.keyPressed(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void keyReleased(Canvas canvas, KeyEvent e) {
		if (pokeCaret != null) {
			pokeCaret.keyReleased(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void keyTyped(Canvas canvas, KeyEvent e) {
		if (pokeCaret != null) {
			pokeCaret.keyTyped(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
		if (pokeCaret != null) {
			pokeCaret.mouseDragged(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		Location loc = Location.create(x, y);
		boolean dirty = false;
		canvas.setHighlightedWires(WireSet.EMPTY);
		if (pokeCaret != null && !pokeCaret.getBounds(g).contains(loc)) {
			dirty = true;
			removeCaret(true);
		}
		if (pokeCaret == null) {
			ComponentUserEvent event = new ComponentUserEvent(canvas, x, y);
			Circuit circ = canvas.getCircuit();
			for (Component c : circ.getAllContaining(loc, g)) {
				if (pokeCaret != null)
					break;

				if (c instanceof Wire) {
					Caret caret = new WireCaret(canvas, (Wire) c, x, y, canvas
							.getProject().getOptions().getAttributeSet());
					setPokedComponent(circ, c, caret);
					canvas.setHighlightedWires(circ.getWireSet((Wire) c));
				} else {
					Pokable p = (Pokable) c.getFeature(Pokable.class);
					if (p != null) {
						Caret caret = p.getPokeCaret(event);
						setPokedComponent(circ, c, caret);
						AttributeSet attrs = c.getAttributeSet();
						if (attrs != null && attrs.getAttributes().size() > 0) {
							Project proj = canvas.getProject();
							proj.getFrame().viewComponentAttributes(circ, c);
						}
					}
				}
			}
		}
		if (pokeCaret != null) {
			dirty = true;
			pokeCaret.mousePressed(e);
		}
		if (dirty)
			canvas.getProject().repaintCanvas();
	}

	@Override
	public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
		if (pokeCaret != null) {
			pokeCaret.mouseReleased(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void paintIcon(ComponentDrawContext c, int x, int y) {
		Graphics g = c.getGraphics();
		if (toolIcon != null) {
			toolIcon.paintIcon(c.getDestination(), g, x + 2, y + 2);
		} else {
			g.setColor(java.awt.Color.black);
			g.drawLine(x + 4, y + 2, x + 4, y + 17);
			g.drawLine(x + 4, y + 17, x + 1, y + 11);
			g.drawLine(x + 4, y + 17, x + 7, y + 11);

			g.drawLine(x + 15, y + 2, x + 15, y + 17);
			g.drawLine(x + 15, y + 2, x + 12, y + 8);
			g.drawLine(x + 15, y + 2, x + 18, y + 8);
		}
	}

	private void removeCaret(boolean normal) {
		Circuit circ = pokedCircuit;
		Caret caret = pokeCaret;
		if (caret != null) {
			if (normal)
				caret.stopEditing();
			else
				caret.cancelEditing();
			circ.removeCircuitListener(listener);
			pokedCircuit = null;
			pokedComponent = null;
			pokeCaret = null;
		}
	}

	private void setPokedComponent(Circuit circ, Component comp, Caret caret) {
		removeCaret(true);
		pokedCircuit = circ;
		pokedComponent = comp;
		pokeCaret = caret;
		if (caret != null) {
			circ.addCircuitListener(listener);
		}
	}
}
