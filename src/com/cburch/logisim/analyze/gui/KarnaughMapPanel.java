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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Implicant;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.TruthTableEvent;
import com.cburch.logisim.analyze.model.TruthTableListener;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

class KarnaughMapPanel extends JPanel implements TruthTablePanel {
	private class MyListener implements OutputExpressionsListener,
			TruthTableListener {
		public void cellsChanged(TruthTableEvent event) {
			repaint();
		}

		public void expressionChanged(OutputExpressionsEvent event) {
			if (event.getType() == OutputExpressionsEvent.OUTPUT_MINIMAL
					&& event.getVariable().equals(output)) {
				repaint();
			}
		}

		public void structureChanged(TruthTableEvent event) {
			computePreferredSize();
		}

	}

	private static final long serialVersionUID = 1L;

	private static final Color[] IMP_COLORS = new Color[] {
			new Color(255, 0, 0, 128), new Color(0, 150, 0, 128),
			new Color(0, 0, 255, 128), new Color(255, 0, 255, 128),
			new Color(0, 255, 255, 128), new Color(80, 80, 80, 128),};

	private static final int MAX_VARS = 5;
	private static final int[] ROW_VARS = { 0, 0, 1, 1, 2 , 2 };
	private static final int[] COL_VARS = { 0, 1, 1, 2, 2 , 3 };
	private static final int[] BigCOL_Index = {0,1,3,2,6,7,5,4};
	private static final int[] BigCOL_Place = {0,1,3,2,7,6,4,5};
	private static final int CELL_HORZ_SEP = 10;
	private static final int CELL_VERT_SEP = 10;
	private static final int IMP_INSET = 4;

	private static final int IMP_RADIUS = 5;

	private MyListener myListener = new MyListener();
	private AnalyzerModel model;
	private String output;
	private int headHeight;
	private int cellWidth = 1;
	private int cellHeight = 1;
	private int tableWidth;
	private int tableHeight;
	private int provisionalX;
	private int provisionalY;
	private Entry provisionalValue = null;
	private Font HeaderFont;
	private Font EntryFont;

	public KarnaughMapPanel(AnalyzerModel model) {
		this.model = model;
		EntryFont = AppPreferences.getScaledFont(getFont());
		HeaderFont = EntryFont.deriveFont(Font.BOLD);
		model.getOutputExpressions().addOutputExpressionsListener(myListener);
		model.getTruthTable().addTruthTableListener(myListener);
		setToolTipText(" ");
	}

	private int computeMargin(int compDim, int tableDim) {
		int ret = (compDim - tableDim) / 2;
		return ret >= 0 ? ret : Math.max(-headHeight, compDim - tableDim);
	}

	private void computePreferredSize() {
		Graphics g = getGraphics();
		TruthTable table = model.getTruthTable();

		String message = null;
		if (output == null) {
			message = Strings.get("karnaughNoOutputError");
		} else if (table.getInputColumnCount() > MAX_VARS) {
			message = Strings.get("karnaughTooManyInputsError");
		}
		if (message != null) {
			if (g == null) {
				tableHeight = AppPreferences.getScaled(AppPreferences.BoxSize);
				tableWidth = AppPreferences.getScaled(100);
			} else {
				FontMetrics fm = g.getFontMetrics(EntryFont);
				tableHeight = fm.getHeight();
				tableWidth = fm.stringWidth(message);
			}
			setPreferredSize(new Dimension(tableWidth, tableHeight));
			repaint();
			return;
		}

		if (g == null) {
			headHeight = 16;
			cellHeight = 16;
			cellWidth = 24;
		} else {
			FontMetrics headFm = g.getFontMetrics(HeaderFont);
			headHeight = headFm.getHeight();

			FontMetrics fm = g.getFontMetrics(EntryFont);
			cellHeight = fm.getAscent() + CELL_VERT_SEP;
			cellWidth = fm.stringWidth("00") + CELL_HORZ_SEP;
		}

		int rows = 1 << ROW_VARS[table.getInputColumnCount()];
		int cols = 1 << COL_VARS[table.getInputColumnCount()];
		tableWidth = headHeight + cellWidth * (cols) +15;
		tableHeight = headHeight + cellHeight * (rows)+15;
		if ((cols>=4)&&(rows>=4)) {
			tableWidth += headHeight+11;
		}
		if (cols>=4) {
			tableHeight += headHeight+11;
		}
		if (cols > 4) {
			tableHeight += headHeight+11;
		}
		setPreferredSize(new Dimension(tableWidth, tableHeight));
		invalidate();
		repaint();
	}

	private int getCol(int tableRow, int rows, int cols) {
		int ret = tableRow % cols;
		if (cols > 4) {
			return BigCOL_Place[ret];
		}
		switch (ret) {
		case 2:
			return 3;
		case 3:
			return 2;
		default:
			return ret;
		}
	}

	public int getOutputColumn(MouseEvent event) {
		return model.getOutputs().indexOf(output);
	}

	private int getRow(int tableRow, int rows, int cols) {
		int ret = tableRow / cols;
		switch (ret) {
		case 2:
			return 3;
		case 3:
			return 2;
		default:
			return ret;
		}
	}

	public int getRow(MouseEvent event) {
		TruthTable table = model.getTruthTable();
		int inputs = table.getInputColumnCount();
		if (inputs >= ROW_VARS.length)
			return -1;
		int left = computeMargin(getWidth(), tableWidth);
		int top = computeMargin(getHeight(), tableHeight);
		int x = event.getX() - left - headHeight - 11;
		int y = event.getY() - top - headHeight - 11;
		if (x < 0 || y < 0)
			return -1;
		int row = y / cellHeight;
		int col = x / cellWidth;
		int rows = 1 << ROW_VARS[inputs];
		int cols = 1 << COL_VARS[inputs];
		if (row >= rows || col >= cols)
			return -1;
		return getTableRow(row, col, rows, cols);
	}

	private int getTableRow(int row, int col, int rows, int cols) {
		return toRow(row, rows) * cols + toCol(col, cols);
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		TruthTable table = model.getTruthTable();
		int row = getRow(event);
		int col = getOutputColumn(event);
		Entry entry = table.getOutputEntry(row, col);
		return entry.getErrorMessage();
	}

	public TruthTable getTruthTable() {
		return model.getTruthTable();
	}

	void localeChanged() {
		computePreferredSize();
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
		
		TruthTable table = model.getTruthTable();
		int inputCount = table.getInputColumnCount();
		Dimension sz = getSize();
		String message = null;
		if (output == null) {
			message = Strings.get("karnaughNoOutputError");
		} else if (inputCount > MAX_VARS) {
			message = Strings.get("karnaughTooManyInputsError");
		}
		if (message != null) {
			g.setFont(HeaderFont);
			GraphicsUtil.drawCenteredText(g, message, sz.width / 2,
					sz.height / 2);
			return;
		}

		int left = computeMargin(sz.width, tableWidth);
		int top = computeMargin(sz.height, tableHeight);
		int x = left;
		int y = top;
		int rowVars = ROW_VARS[inputCount];
		int colVars = COL_VARS[inputCount];
		int rows = 1 << rowVars;
		int cols = 1 << colVars;

		g.setFont(HeaderFont);
		FontMetrics headFm = g.getFontMetrics(HeaderFont);
		for (int i = 0 ; i < inputCount ; i++) {
			String header = model.getInputs().get(i);
			Boolean rotated = false;
			int middleOffset = (headFm.stringWidth(header)>>1);
			int xoffset = headHeight+11;
			int yoffset = headHeight+11;
			switch (i) {
				case 0 : if (inputCount == 1) {
					         rotated = false;
					         xoffset += cellWidth+cellWidth/2;
					         yoffset = headFm.getAscent();
				         } else {
				        	 rotated = true;
				        	 yoffset += (rows-1)*cellHeight;
				        	 if (inputCount < 4)
				        		 yoffset += cellHeight/2;
				        	 xoffset = headFm.getAscent();
				         }
				         break;
				case 1  : if (inputCount==2) {
			                 rotated = false;
			                 xoffset += cellWidth+cellWidth/2;
			                 yoffset = headFm.getAscent();
						  } else if (inputCount==3) {
				                 rotated = false;
				                 xoffset += 3*cellWidth;
				                 yoffset = headFm.getAscent();
						  } else {
							  rotated=true;
							  xoffset += 4*cellWidth+11+headFm.getAscent();
							  yoffset += 2*cellHeight;
							  if (inputCount > 4) {
								  xoffset += 4*cellWidth;
							  }
						  }
				          break;
				case 2  : rotated = false;
			      		  if (inputCount==3) {
					         xoffset += 2*cellWidth;
					         yoffset += 11+2*cellHeight+headFm.getAscent();
				          } else if (inputCount == 4 ){
				        	 xoffset += 3*cellWidth;
				        	 yoffset = headFm.getAscent();
				          } else {
				        	  xoffset += 6*cellWidth;
				        	  yoffset += 11+4*cellHeight+headFm.getAscent();
				          }
				          break;
				case 3  : rotated = false;
				          if (inputCount == 4) {
				        	  xoffset += 2*cellWidth;
				        	  yoffset += 11+4*cellHeight+headFm.getAscent();
				          } else {
				        	  xoffset += 4*cellWidth;
					          yoffset = headFm.getAscent();
				          }
				          break;
				case 4 : rotated = false;
						 xoffset += 2*cellWidth;
			        	 yoffset += 11+4*cellHeight+headFm.getAscent()+headHeight;
			        	 break;
				default : break;
			}
			if (g instanceof Graphics2D) {
				Graphics2D g2 = (Graphics2D) g.create();
				if (rotated) {
					g2.translate(xoffset+x, yoffset+y);
					g2.rotate(-Math.PI / 2.0);
					g2.drawString(header, -middleOffset, 0 );
				} else
				g2.drawString(header, xoffset+x-middleOffset, yoffset+y);
				if (i==4)
					g2.drawString(header, 4*cellWidth+xoffset+x-middleOffset, yoffset+y);
			}
		}

		x += headHeight;
		y += headHeight;
		g.setFont(EntryFont);
		FontMetrics fm = g.getFontMetrics();
		int dy = (cellHeight + fm.getAscent()) / 2;
		x += 11;
		y += 11;
		/* Here the 0/1 labels are placed */
		switch (cols) {
			case 2 :
				g.drawLine(x+cellWidth, y-8, x+2*cellWidth, y-8);
				g.drawLine(x+cellWidth, y-9, x+2*cellWidth, y-9);
				break;
			case 4 :
				g.drawLine(x+2*cellWidth, y-8, x+4*cellWidth, y-8);
				g.drawLine(x+2*cellWidth, y-9, x+4*cellWidth, y-9);
				g.drawLine(x+cellWidth, y+8+rows*cellHeight, x+3*cellWidth, y+8+rows*cellHeight);
				g.drawLine(x+cellWidth, y+9+rows*cellHeight, x+3*cellWidth, y+9+rows*cellHeight);
				break;
			case 8 :
				g.drawLine(x+cellWidth, y+8+rows*cellHeight+headHeight, x+3*cellWidth, y+8+rows*cellHeight+headHeight);
				g.drawLine(x+cellWidth, y+9+rows*cellHeight+headHeight, x+3*cellWidth, y+9+rows*cellHeight+headHeight);
				g.drawLine(x+5*cellWidth, y+8+rows*cellHeight+headHeight, x+7*cellWidth, y+8+rows*cellHeight+headHeight);
				g.drawLine(x+5*cellWidth, y+9+rows*cellHeight+headHeight, x+7*cellWidth, y+9+rows*cellHeight+headHeight);
				g.drawLine(x+2*cellWidth, y-8, x+6*cellWidth, y-8);
				g.drawLine(x+2*cellWidth, y-9, x+6*cellWidth, y-9);
				g.drawLine(x+4*cellWidth, y+8+rows*cellHeight, x+8*cellWidth, y+8+rows*cellHeight);
				g.drawLine(x+4*cellWidth, y+9+rows*cellHeight, x+8*cellWidth, y+9+rows*cellHeight);
				break;
		}
		switch (rows) {
			case 2 :
				g.drawLine(x-8, y+cellHeight, x-8, y+2*cellHeight);
				g.drawLine(x-9, y+cellHeight, x-9, y+2*cellHeight);
				break;
			case 4 :
				g.drawLine(x-8, y+2*cellHeight, x-8, y+4*cellHeight);
				g.drawLine(x-9, y+2*cellHeight, x-9, y+4*cellHeight);
				g.drawLine(x+cols*cellWidth+8, y+cellHeight, x+cols*cellWidth+8, y+3*cellHeight);
				g.drawLine(x+cols*cellWidth+9, y+cellHeight, x+cols*cellWidth+9, y+3*cellHeight);
				break;
		}

		int outputColumn = table.getOutputIndex(output);
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				int row = getTableRow(i, j, rows, cols);
				Entry entry = table.getOutputEntry(row, outputColumn);
				if (provisionalValue != null && row == provisionalY
						&& outputColumn == provisionalX)
					entry = provisionalValue;
				if (entry.isError()) {
					g.setColor(ERROR_COLOR);
					g.fillRect(x + j * cellWidth, y + i * cellHeight,
							cellWidth, cellHeight);
					g.setColor(Color.BLACK);
				}
				g.drawRect(x + j * cellWidth, y + i * cellHeight,
							cellWidth, cellHeight);
				g.drawRect(x + j * cellWidth+1, y + i * cellHeight+1,
						cellWidth-2, cellHeight-2);
			}
		}

		g.drawRect(x,y,cols*cellWidth,rows*cellHeight);
		g.drawRect(x-1,y-1,cols*cellWidth+2,rows*cellHeight+2);
		List<Implicant> implicants = model.getOutputExpressions()
				.getMinimalImplicants(output);
		if (implicants != null) {
			int index = 0;
			for (Implicant imp : implicants) {
				g.setColor(IMP_COLORS[index % IMP_COLORS.length]);
				paintImplicant(g, imp, x, y, rows, cols);
				index++;
			}
		}

		if (outputColumn < 0)
			return;

		g.setColor(Color.BLUE);
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				int row = getTableRow(i, j, rows, cols);
				if (provisionalValue != null && row == provisionalY
						&& outputColumn == provisionalX) {
					String text = provisionalValue.getDescription();
					g.setColor(Color.ORANGE);
					g.drawString(
							text,
							x + j * cellWidth
									+ (cellWidth - fm.stringWidth(text)) / 2, y
									+ i * cellHeight + dy);
					g.setColor(Color.BLUE);
				} else {
					Entry entry = table.getOutputEntry(row, outputColumn);
					String text = entry.getDescription();
					g.drawString(
							text,
							x + j * cellWidth
									+ (cellWidth - fm.stringWidth(text)) / 2, y
									+ i * cellHeight + dy);
				}
			}
		}
	}

	private void paintImplicant(Graphics g, Implicant imp, int x, int y,
			int rows, int cols) {
		int rowMax = -1;
		int rowMin = rows;
		int colMax = -1;
		int colMin = cols;
		boolean oneRowFound = false;
		int count = 0;
		for (Implicant sq : imp.getTerms()) {
			int tableRow = sq.getRow();
			int row = getRow(tableRow, rows, cols);
			int col = getCol(tableRow, rows, cols);
			if (row == 1)
				oneRowFound = true;
			if (row > rowMax)
				rowMax = row;
			if (row < rowMin)
				rowMin = row;
			if (col > colMax)
				colMax = col;
			if (col < colMin)
				colMin = col;
			++count;
		}

		int numCols = colMax - colMin + 1;
		int numRows = rowMax - rowMin + 1;
		int covered = numCols * numRows;
		int d = 2 * IMP_RADIUS;
		if (covered == count) {
			g.fillRoundRect(x + colMin * cellWidth + IMP_INSET, y + rowMin
					* cellHeight + IMP_INSET, numCols * cellWidth - 2
					* IMP_INSET, numRows * cellHeight - 2 * IMP_INSET, d, d);
			/*easy case*/
		} else if (numCols > 4) {
			/* only for tables bigger than 16 entries */
			/* TODO: Make the group marking more clear */
			for (Implicant sq : imp.getTerms()) {
				int tableRow = sq.getRow();
				int row = getRow(tableRow, rows, cols);
				int col = getCol(tableRow, rows, cols);
				int w = cellWidth-3;
				int h = cellHeight-3;
				g.fillRect(2+x+col*cellWidth, 2+y+row*cellHeight, w, h);
			}
		} else if (covered == 16) {
			if (count == 4) {
				int w = cellWidth - IMP_INSET;
				int h = cellHeight - IMP_INSET;
				int x1 = x + 3 * cellWidth + IMP_INSET;
				int y1 = y + 3 * cellHeight + IMP_INSET;
				g.fillRoundRect(x+colMin*cellWidth, y, w, h, d, d);
				g.fillRoundRect(x1+colMin*cellWidth, y, w, h, d, d);
				g.fillRoundRect(x+colMin*cellWidth, y1, w, h, d, d);
				g.fillRoundRect(x1+colMin*cellWidth, y1, w, h, d, d);
			} else if (oneRowFound) { // first and last columns
				int w = cellWidth - IMP_INSET;
				int h = 4 * cellHeight - 2 * IMP_INSET;
				int x1 = x + 3 * cellWidth + IMP_INSET;
				g.fillRoundRect(x+colMin*cellWidth, y + IMP_INSET, w, h, d, d);
				g.fillRoundRect(x1+colMin*cellWidth, y + IMP_INSET, w, h, d, d);
			} else { // first and last rows
				int w = 4 * cellWidth - 2 * IMP_INSET;
				int h = cellHeight - IMP_INSET;
				int y1 = y + 3 * cellHeight + IMP_INSET;
				g.fillRoundRect(x+colMin*cellWidth + IMP_INSET, y, w, h, d, d);
				g.fillRoundRect(x+colMin*cellWidth + IMP_INSET, y1, w, h, d, d);
			}
		} else if (numCols == 4) {
			int top = y + rowMin * cellHeight + IMP_INSET;
			int w = cellWidth - IMP_INSET;
			int h = numRows * cellHeight - 2 * IMP_INSET;
			// handle half going off left edge
			g.fillRoundRect(x+colMin*cellWidth, top, w, h, d, d);
			// handle half going off right edge
			g.fillRoundRect(x + 3 * cellWidth+colMin*cellWidth + IMP_INSET, top, w, h, d, d);
			/*
			 * This is the proper way, with no rounded rectangles along the
			 * table's edge; but I found that the different regions were liable
			 * to overlap, particularly the arcs with the rectangles. (Plus, I
			 * was too lazy to figure this out for the 16 case.) int y0 = y +
			 * rowMin * cellHeight + IMP_INSET; int y1 = y + rowMax * cellHeight
			 * + cellHeight - IMP_INSET; int dy = y1 - y0; int x0 = x +
			 * cellWidth - IMP_INSET; int x1 = x + 3 * cellWidth + IMP_INSET;
			 * 
			 * // half going off left edge g.fillRect(x, y0, cellWidth -
			 * IMP_INSET - IMP_RADIUS, dy); g.fillRect(x0 - IMP_RADIUS, y0 +
			 * IMP_RADIUS, IMP_RADIUS, dy - d); g.fillArc(x0 - d, y0, d, d, 0,
			 * 90); g.fillArc(x0 - d, y1 - d, d, d, 0, -90);
			 * 
			 * // half going off right edge g.fillRect(x1 + IMP_RADIUS, y0,
			 * cellWidth - IMP_INSET - IMP_RADIUS, dy); g.fillRect(x1, y0 +
			 * IMP_RADIUS, IMP_RADIUS, dy - d); g.fillArc(x1, y0, d, d, 180,
			 * 90); g.fillArc(x1, y1 - d, d, d, 180, -90);
			 */
		} else { // numRows == 4
			int left = x + colMin * cellWidth + IMP_INSET;
			int w = numCols * cellWidth - 2 * IMP_INSET;
			int h = cellHeight - IMP_INSET;
			// handle half going off top edge
			g.fillRoundRect(left, y, w, h, d, d);
			// handle half going off right edge
			g.fillRoundRect(left, y + 3 * cellHeight + IMP_INSET, w, h, d, d);
		}
	}

	public void setEntryProvisional(int y, int x, Entry value) {
		provisionalY = y;
		provisionalX = x;
		provisionalValue = value;
		repaint();
	}

	public void setOutput(String value) {
		boolean recompute = (output == null || value == null)
				&& output != value;
		output = value;
		if (recompute)
			computePreferredSize();
		else
			repaint();
	}

	private int toRow(int row, int rows) {
		if (rows == 4) {
			switch (row) {
			case 2:
				return 3;
			case 3:
				return 2;
			default:
				return row;
			}
		} else {
			return row;
		}
	}

	private int toCol(int col, int cols) {
		if (cols > 4) {
			return BigCOL_Index[col];
		}
		if (cols == 4) {
			switch (col) {
			case 2:
				return 3;
			case 3:
				return 2;
			default:
				return col;
			}
		} else {
			return col;
		}
	}

}
