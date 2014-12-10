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

package com.cburch.hex;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Caret {
	private class Listener implements MouseListener, MouseMotionListener,
			KeyListener, FocusListener {
		public void focusGained(FocusEvent e) {
			expose(cursor, false);
		}

		public void focusLost(FocusEvent e) {
			expose(cursor, false);
		}

		public void keyPressed(KeyEvent e) {
			int cols = hex.getMeasures().getColumnCount();
			int rows;
			boolean shift = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0;
			switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				if (cursor >= cols)
					setDot(cursor - cols, shift);
				break;
			case KeyEvent.VK_LEFT:
				if (cursor >= 1)
					setDot(cursor - 1, shift);
				break;
			case KeyEvent.VK_DOWN:
				if (cursor >= hex.getModel().getFirstOffset()
						&& cursor <= hex.getModel().getLastOffset() - cols) {
					setDot(cursor + cols, shift);
				}
				break;
			case KeyEvent.VK_RIGHT:
				if (cursor >= hex.getModel().getFirstOffset()
						&& cursor <= hex.getModel().getLastOffset() - 1) {
					setDot(cursor + 1, shift);
				}
				break;
			case KeyEvent.VK_HOME:
				if (cursor >= 0) {
					int dist = (int) (cursor % cols);
					if (dist == 0)
						setDot(0, shift);
					else
						setDot(cursor - dist, shift);
					break;
				}
			case KeyEvent.VK_END:
				if (cursor >= 0) {
					HexModel model = hex.getModel();
					long dest = (cursor / cols * cols) + cols - 1;
					if (model != null) {
						long end = model.getLastOffset();
						if (dest > end || dest == cursor)
							dest = end;
						setDot(dest, shift);
					} else {
						setDot(dest, shift);
					}
				}
				break;
			case KeyEvent.VK_PAGE_DOWN:
				rows = hex.getVisibleRect().height
						/ hex.getMeasures().getCellHeight();
				if (rows > 2)
					rows--;
				if (cursor >= 0) {
					long max = hex.getModel().getLastOffset();
					if (cursor + rows * cols <= max) {
						setDot(cursor + rows * cols, shift);
					} else {
						long n = cursor;
						while (n + cols < max)
							n += cols;
						setDot(n, shift);
					}
				}
				break;
			case KeyEvent.VK_PAGE_UP:
				rows = hex.getVisibleRect().height
						/ hex.getMeasures().getCellHeight();
				if (rows > 2)
					rows--;
				if (cursor >= rows * cols)
					setDot(cursor - rows * cols, shift);
				else if (cursor >= cols)
					setDot(cursor % cols, shift);
				break;
			}
		}

		public void keyReleased(KeyEvent e) {
		}

		public void keyTyped(KeyEvent e) {
			int mask = e.getModifiers();
			if ((mask & ~InputEvent.SHIFT_MASK) != 0)
				return;

			char c = e.getKeyChar();
			int cols = hex.getMeasures().getColumnCount();
			switch (c) {
			case ' ':
				if (cursor >= 0)
					setDot(cursor + 1, (mask & InputEvent.SHIFT_MASK) != 0);
				break;
			case '\n':
				if (cursor >= 0)
					setDot(cursor + cols, (mask & InputEvent.SHIFT_MASK) != 0);
				break;
			case '\u0008':
			case '\u007f':
				hex.delete();
				// setDot(cursor - 1, (mask & InputEvent.SHIFT_MASK) != 0);
				break;
			default:
				int digit = Character.digit(e.getKeyChar(), 16);
				if (digit >= 0) {
					HexModel model = hex.getModel();
					if (model != null && cursor >= model.getFirstOffset()
							&& cursor <= model.getLastOffset()) {
						int curValue = model.get(cursor);
						int newValue = 16 * curValue + digit;
						model.set(cursor, newValue);
					}
				}
			}
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
			Measures measures = hex.getMeasures();
			long loc = measures.toAddress(e.getX(), e.getY());
			setDot(loc, true);

			// TODO should repeat dragged events when mouse leaves the
			// component
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			Measures measures = hex.getMeasures();
			long loc = measures.toAddress(e.getX(), e.getY());
			setDot(loc, (e.getModifiers() & InputEvent.SHIFT_MASK) != 0);
			if (!hex.isFocusOwner())
				hex.requestFocus();
		}

		public void mouseReleased(MouseEvent e) {
			mouseDragged(e);
		}
	}

	private static Color SELECT_COLOR = new Color(192, 192, 255);

	private static final Stroke CURSOR_STROKE = new BasicStroke(2.0f);

	private HexEditor hex;
	private List<ChangeListener> listeners;
	private long mark;
	private long cursor;
	private Object highlight;

	Caret(HexEditor hex) {
		this.hex = hex;
		this.listeners = new ArrayList<ChangeListener>();
		this.cursor = -1;

		Listener l = new Listener();
		hex.addMouseListener(l);
		hex.addMouseMotionListener(l);
		hex.addKeyListener(l);
		hex.addFocusListener(l);

		InputMap imap = hex.getInputMap();
		ActionMap amap = hex.getActionMap();
		AbstractAction nullAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
			}
		};
		String nullKey = "null";
		amap.put(nullKey, nullAction);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), nullKey);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), nullKey);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), nullKey);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), nullKey);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), nullKey);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), nullKey);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), nullKey);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), nullKey);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), nullKey);
	}

	public void addChangeListener(ChangeListener l) {
		listeners.add(l);
	}

	private void expose(long loc, boolean scrollTo) {
		if (loc >= 0) {
			Measures measures = hex.getMeasures();
			int x = measures.toX(loc);
			int y = measures.toY(loc);
			int w = measures.getCellWidth();
			int h = measures.getCellHeight();
			hex.repaint(x - 1, y - 1, w + 2, h + 2);
			if (scrollTo) {
				hex.scrollRectToVisible(new Rectangle(x, y, w, h));
			}
		}
	}

	public long getDot() {
		return cursor;
	}

	public long getMark() {
		return mark;
	}

	void paintForeground(Graphics g, long start, long end) {
		if (cursor >= start && cursor < end && hex.isFocusOwner()) {
			Measures measures = hex.getMeasures();
			int x = measures.toX(cursor);
			int y = measures.toY(cursor);
			Graphics2D g2 = (Graphics2D) g;
			Stroke oldStroke = g2.getStroke();
			g2.setColor(hex.getForeground());
			g2.setStroke(CURSOR_STROKE);
			g2.drawRect(x, y, measures.getCellWidth() - 1,
					measures.getCellHeight() - 1);
			g2.setStroke(oldStroke);
		}
	}

	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}

	public void setDot(long value, boolean keepMark) {
		HexModel model = hex.getModel();
		if (model == null || value < model.getFirstOffset()
				|| value > model.getLastOffset()) {
			value = -1;
		}
		if (cursor != value) {
			long oldValue = cursor;
			if (highlight != null) {
				hex.getHighlighter().remove(highlight);
				highlight = null;
			}
			if (!keepMark) {
				mark = value;
			} else if (mark != value) {
				highlight = hex.getHighlighter().add(mark, value, SELECT_COLOR);
			}
			cursor = value;
			expose(oldValue, false);
			expose(value, true);
			if (!listeners.isEmpty()) {
				ChangeEvent event = new ChangeEvent(this);
				for (ChangeListener l : listeners) {
					l.stateChanged(event);
				}
			}
		}
	}
}
