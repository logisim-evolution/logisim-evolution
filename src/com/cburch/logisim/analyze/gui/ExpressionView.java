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

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

import javax.swing.JPanel;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.ExpressionVisitor;
import com.cburch.logisim.prefs.AppPreferences;

class ExpressionView extends JPanel {
	private static class ExpressionData {
		String text;
		final ArrayList<NotData> nots = new ArrayList<NotData>();
		int[] badness;

		ExpressionData(Expression expr) {
			if (expr == null) {
				text = "";
				badness = new int[0];
			} else {
				computeText(expr);
				computeBadnesses();
			}
		}

		private void computeBadnesses() {
			badness = new int[text.length() + 1];
			badness[text.length()] = 0;
			if (text.length() == 0)
				return;

			badness[0] = Integer.MAX_VALUE;
			NotData curNot = nots.isEmpty() ? null : (NotData) nots.get(0);
			int curNotIndex = 0;
			char prev = text.charAt(0);
			for (int i = 1; i < text.length(); i++) {
				// invariant: curNot.stopIndex >= i (and is first such),
				// or curNot == null if none such exists
				char cur = text.charAt(i);
				if (cur == ' ') {
					badness[i] = BADNESS_BEFORE_SPACE;
					;
				} else if (Character.isJavaIdentifierPart(cur)) {
					if (Character.isJavaIdentifierPart(prev)) {
						badness[i] = BADNESS_IDENT_BREAK;
					} else {
						badness[i] = BADNESS_BEFORE_AND;
					}
				} else if (cur == '+') {
					badness[i] = BADNESS_BEFORE_OR;
				} else if (cur == '^') {
					badness[i] = BADNESS_BEFORE_XOR;
				} else if (cur == ')') {
					badness[i] = BADNESS_BEFORE_SPACE;
				} else { // cur == '('
					badness[i] = BADNESS_BEFORE_AND;
				}

				while (curNot != null && curNot.stopIndex <= i) {
					++curNotIndex;
					curNot = (curNotIndex >= nots.size() ? null
							: (NotData) nots.get(curNotIndex));
				}

				if (curNot != null && badness[i] < BADNESS_IDENT_BREAK) {
					int depth = 0;
					NotData nd = curNot;
					int ndi = curNotIndex;
					while (nd != null && nd.startIndex < i) {
						if (nd.stopIndex > i)
							++depth;
						++ndi;
						nd = ndi < nots.size() ? (NotData) nots.get(ndi) : null;
					}
					if (depth > 0) {
						badness[i] += BADNESS_NOT_BREAK + (depth - 1)
								* BADNESS_PER_NOT_BREAK;
					}
				}

				prev = cur;
			}
		}

		private void computeText(Expression expr) {
			final StringBuilder text = new StringBuilder();
			expr.visit(new ExpressionVisitor<Object>() {
				private Object binary(Expression a, Expression b, int level,
						String op) {
					if (a.getPrecedence() < level) {
						text.append("(");
						a.visit(this);
						text.append(")");
					} else {
						a.visit(this);
					}
					text.append(op);
					if (b.getPrecedence() < level) {
						text.append("(");
						b.visit(this);
						text.append(")");
					} else {
						b.visit(this);
					}
					return null;
				}

				public Object visitAnd(Expression a, Expression b) {
					return binary(a, b, Expression.AND_LEVEL, " ");
				}

				public Object visitConstant(int value) {
					text.append("" + Integer.toString(value, 16));
					return null;
				}

				public Object visitNot(Expression a) {
					NotData notData = new NotData();
					notData.startIndex = text.length();
					nots.add(notData);
					a.visit(this);
					notData.stopIndex = text.length();
					return null;
				}

				public Object visitOr(Expression a, Expression b) {
					return binary(a, b, Expression.OR_LEVEL, " + ");
				}

				public Object visitVariable(String name) {
					text.append(name);
					return null;
				}

				public Object visitXor(Expression a, Expression b) {
					return binary(a, b, Expression.XOR_LEVEL, " ^ ");
				}
			});
			this.text = text.toString();
		}
	}

	private class MyListener implements ComponentListener {
		public void componentHidden(ComponentEvent arg0) {
		}

		public void componentMoved(ComponentEvent arg0) {
		}

		public void componentResized(ComponentEvent arg0) {
			int width = getWidth();
			if (renderData != null && Math.abs(renderData.width - width) > 2) {
				Graphics g = getGraphics();
				FontMetrics fm = g == null ? null : g.getFontMetrics();
				renderData = new RenderData(renderData.exprData, width, fm);
				setPreferredSize(renderData.getPreferredSize());
				revalidate();
				repaint();
			}
		}

		public void componentShown(ComponentEvent arg0) {
		}
	}

	private static class NotData {
		int startIndex;
		int stopIndex;
		int depth;
	}

	private static class RenderData {
		ExpressionData exprData;
		int prefWidth;
		int width;
		int height;
		String[] lineText;
		ArrayList<ArrayList<NotData>> lineNots;
		int[] lineY;

		RenderData(ExpressionData exprData, int width, FontMetrics fm) {
			this.exprData = exprData;
			this.width = width;
			height = MINIMUM_HEIGHT;

			if (fm == null) {
				lineText = new String[] { exprData.text };
				lineNots = new ArrayList<ArrayList<NotData>>();
				lineNots.add(exprData.nots);
				computeNotDepths();
				lineY = new int[] { MINIMUM_HEIGHT };
			} else {
				if (exprData.text.length() == 0) {
					lineText = new String[] { Strings.get("expressionEmpty") };
					lineNots = new ArrayList<ArrayList<NotData>>();
					lineNots.add(new ArrayList<NotData>());
				} else {
					computeLineText(fm);
					computeLineNots();
					computeNotDepths();
				}
				computeLineY(fm);
				prefWidth = lineText.length > 1 ? width : fm
						.stringWidth(lineText[0]);
			}
		}

		private void computeLineNots() {
			ArrayList<NotData> allNots = exprData.nots;
			lineNots = new ArrayList<ArrayList<NotData>>();
			for (int i = 0; i < lineText.length; i++) {
				lineNots.add(new ArrayList<NotData>());
			}
			for (NotData nd : allNots) {
				int pos = 0;
				for (int j = 0; j < lineNots.size() && pos < nd.stopIndex; j++) {
					String line = lineText[j];
					int nextPos = pos + line.length();
					if (nextPos > nd.startIndex) {
						NotData toAdd = new NotData();
						toAdd.startIndex = Math.max(pos, nd.startIndex) - pos;
						toAdd.stopIndex = Math.min(nextPos, nd.stopIndex) - pos;
						lineNots.get(j).add(toAdd);
					}
					pos = nextPos;
				}
			}
		}

		private void computeLineText(FontMetrics fm) {
			String text = exprData.text;
			int[] badness = exprData.badness;

			if (fm.stringWidth(text) <= width) {
				lineText = new String[] { text };
				return;
			}

			int startPos = 0;
			ArrayList<String> lines = new ArrayList<String>();
			while (startPos < text.length()) {
				int stopPos = startPos + 1;
				String bestLine = text.substring(startPos, stopPos);
				if (stopPos >= text.length()) {
					lines.add(bestLine);
					break;
				}
				int bestStopPos = stopPos;
				int lineWidth = fm.stringWidth(bestLine);
				int bestBadness = badness[stopPos] + (width - lineWidth)
						* BADNESS_PER_PIXEL;
				while (stopPos < text.length()) {
					++stopPos;
					String line = text.substring(startPos, stopPos);
					lineWidth = fm.stringWidth(line);
					if (lineWidth > width)
						break;

					int lineBadness = badness[stopPos] + (width - lineWidth)
							* BADNESS_PER_PIXEL;
					if (lineBadness < bestBadness) {
						bestBadness = lineBadness;
						bestStopPos = stopPos;
						bestLine = line;
					}
				}
				lines.add(bestLine);
				startPos = bestStopPos;
			}
			lineText = lines.toArray(new String[lines.size()]);
		}

		private void computeLineY(FontMetrics fm) {
			lineY = new int[lineNots.size()];
			int curY = 0;
			for (int i = 0; i < lineY.length; i++) {
				int maxDepth = -1;
				ArrayList<NotData> nots = lineNots.get(i);
				for (NotData nd : nots) {
					if (nd.depth > maxDepth)
						maxDepth = nd.depth;
				}
				lineY[i] = curY + maxDepth * NOT_SEP;
				curY = lineY[i] + fm.getHeight() + EXTRA_LEADING;
			}
			height = Math.max(MINIMUM_HEIGHT, curY - fm.getLeading()
					- EXTRA_LEADING);
		}

		private void computeNotDepths() {
			for (ArrayList<NotData> nots : lineNots) {
				int n = nots.size();
				int[] stack = new int[n];
				for (int i = 0; i < nots.size(); i++) {
					NotData nd = nots.get(i);
					int depth = 0;
					int top = 0;
					stack[0] = nd.stopIndex;
					for (int j = i + 1; j < nots.size(); j++) {
						NotData nd2 = nots.get(j);
						if (nd2.startIndex >= nd.stopIndex)
							break;
						while (nd2.startIndex >= stack[top])
							top--;
						++top;
						stack[top] = nd2.stopIndex;
						if (top > depth)
							depth = top;
					}
					nd.depth = depth;
				}
			}
		}

		public Dimension getPreferredSize() {
			return new Dimension(10, height);
		}

		public void paint(Graphics g, int x, int y) {
			FontMetrics fm = g.getFontMetrics();
			int i = -1;
			for (String line : lineText) {
				i++;
				g.drawString(line, x, y + lineY[i] + fm.getAscent());

				ArrayList<NotData> nots = lineNots.get(i);
				for (NotData nd : nots) {
					int notY = y + lineY[i] - nd.depth * NOT_SEP;
					int startX = x
							+ fm.stringWidth(line.substring(0, nd.startIndex));
					int stopX = x
							+ fm.stringWidth(line.substring(0, nd.stopIndex));
					g.drawLine(startX, notY, stopX, notY);
					g.drawLine(startX, notY-1, stopX, notY-1);
				}
			}
		}
	}

	private static final long serialVersionUID = 1L;
	private static final int BADNESS_IDENT_BREAK = 10000;
	private static final int BADNESS_BEFORE_SPACE = 500;
	private static final int BADNESS_BEFORE_AND = 50;
	private static final int BADNESS_BEFORE_XOR = 30;

	private static final int BADNESS_BEFORE_OR = 0;
	private static final int BADNESS_NOT_BREAK = 100;
	private static final int BADNESS_PER_NOT_BREAK = 30;

	private static final int BADNESS_PER_PIXEL = 1;

	private static final int NOT_SEP = 3;
	private static final int EXTRA_LEADING = 4;

	private static final int MINIMUM_HEIGHT = 25;

	private MyListener myListener = new MyListener();

	private RenderData renderData;

	public ExpressionView() {
		addComponentListener(myListener);
		setExpression(null);
	}

	void localeChanged() {
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		if (AppPreferences.AntiAliassing.getBoolean()) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		
		super.paintComponent(g);
		
		if (renderData != null) {
			int x = Math.max(0, (getWidth() - renderData.prefWidth) / 2);
			int y = Math.max(0, (getHeight() - renderData.height) / 2);
			renderData.paint(g, x, y);
		}
	}

	public void setExpression(Expression expr) {
		ExpressionData exprData = new ExpressionData(expr);
		Graphics g = getGraphics();
		FontMetrics fm = g == null ? null : g.getFontMetrics();
		renderData = new RenderData(exprData, getWidth(), fm);
		setPreferredSize(renderData.getPreferredSize());
		revalidate();
		repaint();
	}
}
