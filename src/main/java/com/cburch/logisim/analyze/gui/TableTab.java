/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.TruthTableEvent;
import com.cburch.logisim.analyze.model.TruthTableListener;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.PrintHandler;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

class TableTab extends AnalyzerTab implements Entry.EntryChangedListener {
  private class MyListener implements TruthTableListener, LocaleListener {
    public void rowsChanged(TruthTableEvent event) {
      updateTable();
    }

    public void cellsChanged(TruthTableEvent event) {
      repaint();
    }

    public void structureChanged(TruthTableEvent event) {
      updateTable();
    }

    void updateTable() {
      computePreferredSize();
      expand.setEnabled(getRowCount() < table.getRowCount());
      count.setText(S.fmt("tableRowsShown", getRowCount(), table.getRowCount()));
      body.setSize(new Dimension(body.getWidth(), table.getRowCount() * cellHeight));
      repaint();
    }

    @Override
    public void localeChanged() {
      expand.setText(S.get("tableExpand"));
      compact.setText(S.get("tableCompact"));
      count.setText(S.fmt("tableRowsShown", getRowCount(), table.getRowCount()));
    }
  }

  private static final long serialVersionUID = 1L;

  private Font HEAD_FONT;
  private Font BODY_FONT;
  private int HEADER_PADDING;
  private int HEADER_VSEP;
  private int COLUMNS_HSEP;
  private int DEFAULT_CELL_PADDING;
  private int DEFAULT_CELL_WIDTH;
  private int DEFAULT_CELL_HEIGHT;

  private MyListener myListener = new MyListener();
  private TruthTable table;
  private TableBody body;
  private TableHeader header;
  private JScrollPane bodyPane, headerPane;
  private int cellHeight;
  private int tableWidth, headerHeight, bodyHeight;
  private ColumnGroupDimensions inDim, outDim;
  private TableTabCaret caret;
  private TableTabClip clip;

  List<Var> inputVars, outputVars;

  private class ColumnGroupDimensions {
    int cellWidth, cellPadding, leftPadding, rightPadding;
    int width;
    List<Var> vars;

    ColumnGroupDimensions(List<Var> vars) {
      this.vars = vars;
    }

    void reset(List<Var> vars) {
      this.vars = vars;
      leftPadding = DEFAULT_CELL_PADDING / 2;
      rightPadding = DEFAULT_CELL_PADDING / 2;
      cellPadding = DEFAULT_CELL_PADDING;
      cellWidth = DEFAULT_CELL_WIDTH;
    }

    void calculate(FontMetrics fm) {
      for (int i = 1; i < vars.size(); i++) {
        Var v1 = vars.get(i - 1);
        Var v2 = vars.get(i);
        int hw1 = fm.stringWidth(v1.toString());
        int hw2 = fm.stringWidth(v2.toString());
        int hw = (hw1 - hw1 / 2) + HEADER_PADDING + (hw2 / 2);
        int cw1 = v1.width * cellWidth;
        int cw2 = v2.width * cellWidth;
        int cw = (cw1 - cw1 / 2) + cellPadding + (cw2 / 2);
        if (hw > cw) cellPadding += (hw - cw);
      }
      Var v;
      int w;
      v = vars.get(0);
      w = fm.stringWidth(v.toString());
      leftPadding = Math.max(DEFAULT_CELL_PADDING / 2, (w / 2) - (cellWidth * v.width / 2));
      v = vars.get(vars.size() - 1);
      w = fm.stringWidth(v.toString());
      rightPadding =
          Math.max(
              DEFAULT_CELL_PADDING / 2,
              (w - w / 2) - (cellWidth * v.width - cellWidth * v.width / 2));
      calculateWidth();
    }

    void calculateWidth() {
      int w = -cellPadding;
      for (Var v : vars) w += cellPadding + v.width * cellWidth;
      width = leftPadding + w + rightPadding;
    }

    int getColumn(int x) {
      if (x < leftPadding) return -1;
      x -= leftPadding;
      int col = 0;
      for (Var v : vars) {
        if (x < 0) return -1;
        if (x < v.width * cellWidth) return col + x / cellWidth;
        col += v.width;
        x -= v.width * cellWidth + cellPadding;
      }
      return -1;
    }

    // always returns valid column, unless there are none
    int getNearestColumn(int x) {
      if (x < leftPadding) return 0;
      x -= leftPadding;
      int col = 0;
      for (Var v : vars) {
        if (x < -(cellPadding / 2)) return col - 1;
        if (x < 0) return col;
        if (x < v.width * cellWidth) return col + x / cellWidth;
        col += v.width;
        x -= v.width * cellWidth + cellPadding;
      }
      return col - 1;
    }

    int getX(int col) {
      int x = leftPadding;
      for (Var v : vars) {
        if (col < 0) return x;
        if (col < v.width) return x + col * cellWidth;
        col -= v.width;
        x += v.width * cellWidth + cellPadding;
      }
      return x;
    }

    void paintHeaders(Graphics g, int x, int y) {
      FontMetrics fm = g.getFontMetrics();
      y += fm.getAscent() + 1;
      x += leftPadding;
      for (Var v : vars) {
        String s = v.toString();
        int sx = x + (v.width * cellWidth) / 2;
        int sw = fm.stringWidth(s);
        g.drawString(s, sx - (sw / 2), y);
        x += (v.width * cellWidth) + cellPadding;
      }
    }

    void paintRow(Graphics g, FontMetrics fm, int x, int y, int row, boolean isInput) {
      x += leftPadding;
      int cy = y + fm.getAscent();
      int col = 0;
      for (Var v : vars) {
        for (int b = v.width - 1; b >= 0; b--) {
          Entry entry =
              isInput
                  ? table.getVisibleInputEntry(row, col++)
                  : table.getVisibleOutputEntry(row, col++);
          if (entry.isError()) {
            g.setColor(Value.ERROR_COLOR);
            g.fillRect(x, y, cellWidth, cellHeight);
            g.setColor(Color.BLACK);
          }
          g.setColor( entry == Entry.BUS_ERROR ? Value.ERROR_COLOR : Color.BLACK);
          String label = entry.getDescription();
          int width = fm.stringWidth(label);
          g.drawString(label, x + (cellWidth - width) / 2, cy);
          x += cellWidth;
        }
        x += cellPadding;
      }
    }
  };

  private SquareButton one = new SquareButton(Entry.ONE);
  private SquareButton zero = new SquareButton(Entry.ZERO);
  private SquareButton dontcare = new SquareButton(Entry.DONT_CARE);
  private TightButton expand = new TightButton(S.get("tableExpand"));
  private TightButton compact = new TightButton(S.get("tableCompact"));
  private JLabel count = new JLabel(S.fmt("tableRowsShown", 0, 0), SwingConstants.CENTER);

  private class TightButton extends JButton {
    /** */
    private static final long serialVersionUID = 1L;

    TightButton(String s) {
      super(s);
      setMargin(new Insets(0, 0, 0, 0));
    }
  }

  private class SquareButton extends TightButton implements Entry.EntryChangedListener {
    /** */
    private static final long serialVersionUID = 1L;
    
    Entry myEntry;

    SquareButton(Entry e) {
      super(e.getDescription());
      myEntry = e;
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      int s = (int) d.getHeight();
      return new Dimension(s, s);
    }

	@Override
	public void EntryDesriptionChanged() {
	  setText(myEntry.getDescription());
	  repaint();
	}
  }

  public TableTab(TruthTable table) {
    this.table = table;
    Font MyFont = new Font("Serif", Font.PLAIN, 14);
    HEAD_FONT = AppPreferences.getScaledFont(MyFont).deriveFont(Font.BOLD);
    BODY_FONT = AppPreferences.getScaledFont(MyFont);
    HEADER_PADDING = AppPreferences.getScaled(10);
    HEADER_VSEP = AppPreferences.getScaled(4);
    COLUMNS_HSEP = AppPreferences.getScaled(4);
    DEFAULT_CELL_PADDING = AppPreferences.getScaled(12);
    DEFAULT_CELL_WIDTH = AppPreferences.getScaled(12);
    DEFAULT_CELL_HEIGHT = AppPreferences.getScaled(16);
    header = new TableHeader();
    body = new TableBody();
    bodyPane =
        new JScrollPane(
            body,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    bodyPane.addComponentListener(new ComponentAdapter() {
          public void componentResized(ComponentEvent event) {
            int width = bodyPane.getViewport().getWidth();
            body.setSize(new Dimension(width, body.getHeight()));
          }
        });
    bodyPane.setVerticalScrollBar(getVerticalScrollBar());
    headerPane =
        new JScrollPane(
            header,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    headerPane.addComponentListener(new ComponentAdapter() {
    	public void componentResized(ComponentEvent event) {
            int width = headerPane.getViewport().getWidth();
            header.setSize(new Dimension(width, header.getHeight()));
          }
        });
    headerPane.getHorizontalScrollBar().setModel(bodyPane.getHorizontalScrollBar().getModel());
    bodyPane.setBorder(null);
    body.setBorder(null);
    headerPane.setBorder(null);
    header.setBorder(null);

    JPanel toolbar = new JPanel();
    toolbar.setLayout(new FlowLayout());
    Entry.DONT_CARE.addListener(dontcare);
    Entry.ZERO.addListener(zero);
    Entry.ONE.addListener(one);
    Entry.DONT_CARE.addListener(this);
    Entry.ZERO.addListener(this);
    Entry.ONE.addListener(this);
    toolbar.add(dontcare);
    toolbar.add(one);
    toolbar.add(zero);
    toolbar.add(compact);
    toolbar.add(expand);
    one.setActionCommand("1");
    zero.setActionCommand("0");
    dontcare.setActionCommand("x");
    compact.setActionCommand("compact");
    expand.setActionCommand("expand");

    expand.setEnabled(getRowCount() < table.getRowCount());
    count.setText(S.fmt("tableRowsShown", getRowCount(), table.getRowCount()));

    GridBagLayout layout = new GridBagLayout();
    setLayout(layout);
    GridBagConstraints gc = new GridBagConstraints();
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 1;
    layout.setConstraints(toolbar, gc);
    add(toolbar);
    gc.gridy++;
    layout.setConstraints(count, gc);
    add(count);
    gc.gridy++;
    layout.setConstraints(headerPane, gc);
    add(headerPane);
    gc.fill = GridBagConstraints.BOTH;
    gc.gridy++;
    gc.weighty = 1;
    layout.setConstraints(bodyPane, gc);
    add(bodyPane);
    inDim = new ColumnGroupDimensions(table.getInputVariables());
    outDim = new ColumnGroupDimensions(table.getOutputVariables());
    table.addTruthTableListener(myListener);
    setToolTipText(" ");
    caret = new TableTabCaret(this);
    one.addActionListener(caret.getListener());
    zero.addActionListener(caret.getListener());
    dontcare.addActionListener(caret.getListener());
    compact.addActionListener(caret.getListener());
    expand.addActionListener(caret.getListener());
    clip = new TableTabClip(this);
    computePreferredSize();
    this.addComponentListener(new ComponentAdapter() {
      boolean done;
      public void componentShown(ComponentEvent e) {
        TableTab.this.removeComponentListener(this);
        if (done)
          return;
        done = true;
        // account for missing scrollbar on header portion
        int pad = bodyPane.getVerticalScrollBar().getWidth();
        GridBagConstraints gc = layout.getConstraints(headerPane);
        Insets i = gc.insets;
        gc.insets.set(i.top, i.left, i.bottom, i.right + pad);
        layout.setConstraints(headerPane, gc);
        invalidate();
        repaint();
      }
    });
    editHandler.computeEnabled();
    LocaleManager.addLocaleListener(myListener);
  }

  public JPanel getBody() {
    return body;
  }

  private static Canvas canvas = new Canvas();

  private void computePreferredSize() {
    inputVars = table.getInputVariables();
    outputVars = table.getOutputVariables();
    if (inputVars.size() == 0) {
      inputVars = new ArrayList<>();
      inputVars.add(new Var(S.get("tableNoInputs"), 0));
    }

    if (outputVars.size() == 0) {
      outputVars = new ArrayList<>();
      outputVars.add(new Var(S.get("tableNoOutputs"), 0));
    }

    cellHeight = DEFAULT_CELL_HEIGHT;
    inDim.reset(inputVars);
    outDim.reset(outputVars);
    Graphics g = getGraphics();
    FontMetrics fm = (g != null ? g.getFontMetrics(HEAD_FONT) : canvas.getFontMetrics(HEAD_FONT));
    cellHeight = fm.getHeight();
    inDim.calculate(fm);
    outDim.calculate(fm);

    tableWidth = inDim.width + COLUMNS_HSEP + outDim.width;

    computePreferredHeight();
  }

  private void computePreferredHeight() {
    bodyHeight = cellHeight * getRowCount();
    headerHeight = HEADER_VSEP + cellHeight + HEADER_VSEP;
    int tableHeight = cellHeight + headerHeight;

    header.setPreferredSize(new Dimension(tableWidth, headerHeight));
    headerPane.setMinimumSize(new Dimension(tableWidth, headerHeight));
    headerPane.setPreferredSize(new Dimension(tableWidth, headerHeight));

    body.setPreferredSize(new Dimension(tableWidth, bodyHeight));
    bodyPane.setPreferredSize(new Dimension(tableWidth, 1));

    setPreferredSize(new Dimension(tableWidth+40, tableHeight));
    revalidate();
    repaint();
  }

  TableTabCaret getCaret() {
    return caret;
  }

  int getCellHeight() {
    return cellHeight;
  }

  public int getColumn(MouseEvent event) {
    int left = (body.getWidth() - tableWidth) / 2;
    int mid = left + inDim.width + COLUMNS_HSEP;
    int x = event.getX();
    if (x < mid) {
      return inDim.getColumn(x - left);
    } else {
      int c = outDim.getColumn(x - mid);
      return c < 0 ? -1 : table.getInputColumnCount() + c;
    }
  }

  public int getNearestColumn(MouseEvent event) {
    int inputs = table.getInputColumnCount();
    int outputs = table.getOutputColumnCount();
    if (inputs + outputs == 0) return -1;
    int left = (body.getWidth() - tableWidth) / 2;
    int mid = left + inDim.width + COLUMNS_HSEP;
    int x = event.getX();
    if (x < left) return 0;
    else if (x >= mid + outDim.width) return inputs + outputs - 1;
    else if (x < mid - COLUMNS_HSEP / 2 && inputs > 0) return inDim.getNearestColumn(x - left);
    else return inputs + outDim.getNearestColumn(x - mid);
  }

  int getColumnCount() {
    int inputs = table.getInputColumnCount();
    int outputs = table.getOutputColumnCount();
    return inputs + outputs;
  }

  public int getOutputColumn(MouseEvent event) {
    int c = getColumn(event);
    int inputs = table.getInputColumnCount();
    return (c < inputs ? -1 : c - inputs);
  }

  public int getRow(MouseEvent event) {
    int y = event.getY();
    if (y < 0) return -1;
    int ret = y / cellHeight;
    int rows = getRowCount();
    return ret >= 0 && ret < rows ? ret : -1;
  }

  public int getNearestRow(MouseEvent event) {
    int y = event.getY();
    if (y < 0) return 0;
    int ret = y / cellHeight;
    int rows = getRowCount();
    return ret < 0 ? 0 : ret >= rows ? rows - 1 : ret;
  }

  public int getRowCount() {
    return table.getVisibleRowCount();
  }

  public int getInputColumnCount() {
    return table.getInputColumnCount();
  }

  public int getOutputColumnCount() {
    return table.getOutputColumnCount();
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    int row = getRow(event);
    if (row < 0) return null;
    int col = getOutputColumn(event);
    if (col < 0) return null;
    Entry entry = table.getVisibleOutputEntry(row, col);
    return entry.getErrorMessage();
  }

  public TruthTable getTruthTable() {
    return table;
  }

  JScrollBar getVerticalScrollBar() {
    return new JScrollBar() {
      private static final long serialVersionUID = 1L;

      @Override
      public int getBlockIncrement(int direction) {
        int curY = getValue();
        int curHeight = getVisibleAmount();
        int numCells = curHeight / cellHeight - 1;
        if (numCells <= 0) numCells = 1;
        if (direction > 0) {
          return curY > 0 ? numCells * cellHeight : numCells * cellHeight + HEADER_VSEP;
        } else {
          return curY > cellHeight + HEADER_VSEP
              ? numCells * cellHeight
              : numCells * cellHeight + HEADER_VSEP;
        }
      }

      @Override
      public int getUnitIncrement(int direction) {
        int curY = getValue();
        if (direction > 0) {
          return curY > 0 ? cellHeight : cellHeight + HEADER_VSEP;
        } else {
          return curY > cellHeight + HEADER_VSEP ? cellHeight : cellHeight + HEADER_VSEP;
        }
      }
    };
  }

  int getXLeft(int col) {
    int left = Math.max(0, (body.getWidth() - tableWidth) / 2);
    int mid = left + inDim.width + COLUMNS_HSEP;
    int inputs = table.getInputColumnCount();
    if (col < inputs) return left + inDim.getX(col);
    else return mid + outDim.getX(col - inputs);
  }

  int getXRight(int col) {
    int left = Math.max(0, (body.getWidth() - tableWidth) / 2);
    int mid = left + inDim.width + COLUMNS_HSEP;
    int inputs = table.getInputColumnCount();
    if (col < inputs) return left + inDim.getX(col) + inDim.cellWidth;
    else return mid + outDim.getX(col - inputs) + outDim.cellWidth;
  }

  int getCellWidth(int col) {
    int inputs = table.getInputColumnCount();
    return (col < inputs) ? inDim.cellWidth : outDim.cellWidth;
  }

  int getY(int row) {
    return row * cellHeight;
  }

  void localeChanged() {
    computePreferredSize();
    repaint();
  }

  void updateTab() {
	    editHandler.computeEnabled();
  }


  private class TableBody extends JPanel {
    /** */
    private static final long serialVersionUID = 1L;

    @Override
    public void paintComponent(Graphics g) {
      try {
          paintComponent(g, false, getWidth(), getHeight());
      } catch (Exception e) {
            // this can happen during transitions between circuits
          }
    }
    
    public void paintComponent(Graphics g, boolean printView, int canvasWidth, int canvasHeight) {
      /* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      if (!printView) {
        super.paintComponent(g);
        caret.paintBackground(g);
      }

      int top = 0;
      int left = Math.max(0, (canvasWidth - tableWidth) / 2);
      int mid = left + inDim.width + COLUMNS_HSEP;

      g.setColor(Color.GRAY);
      int lineX = left + inDim.width + COLUMNS_HSEP / 2;
      g.drawLine(lineX, top, lineX, top + bodyHeight);

      g.setFont(BODY_FONT);
      FontMetrics fm = g.getFontMetrics();
      int y = top;

      Rectangle clip = g.getClipBounds();
      int firstRow = Math.max(0, (clip.y - y) / cellHeight);
      int lastRow = Math.min(getRowCount(), 2 + (clip.y + clip.height - y) / cellHeight);
      y += firstRow * cellHeight;

      for (int row = firstRow; row < lastRow; row++) {
        inDim.paintRow(g, fm, left, y, row, true);
        outDim.paintRow(g, fm, mid, y, row, false);
        y += cellHeight;
      }
      if (!printView)
        caret.paintForeground(g);
    }
  }

  private class TableHeader extends JPanel {
    /** */
    private static final long serialVersionUID = 1L;

    @Override
    public void paintComponent(Graphics g) {
    	paintComponent(g, false, getWidth(), getHeight());
    }
    
    public void paintComponent(Graphics g, boolean printView, int canvasWidth, int canvasHeight) {
      /* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      if (!printView)
        super.paintComponent(g);

      int top = canvasHeight - cellHeight - HEADER_VSEP;
      int left = Math.max(0, (canvasWidth - tableWidth) / 2);

      g.setColor(Color.GRAY);
      int lineX = left + inDim.width + COLUMNS_HSEP / 2;
      int lineY = top + cellHeight + HEADER_VSEP / 2;
      g.drawLine(left, lineY, left + tableWidth, lineY);
      g.drawLine(lineX, top, lineX, cellHeight);

      g.setColor(Color.BLACK);
      g.setFont(HEAD_FONT);
      inDim.paintHeaders(g, left, top);
      outDim.paintHeaders(g, left + inDim.width + COLUMNS_HSEP, top);
    }
  }

  @Override
  EditHandler getEditHandler() {
    return editHandler;
  }

  EditHandler editHandler = new EditHandler() {
    @Override
    public void computeEnabled() {
      boolean sel = caret.hasSelection();
      setEnabled(LogisimMenuBar.CUT, sel);
      setEnabled(LogisimMenuBar.COPY, sel);
      setEnabled(LogisimMenuBar.PASTE, sel);
      setEnabled(LogisimMenuBar.DELETE, sel);
      setEnabled(LogisimMenuBar.DUPLICATE, false);
      setEnabled(LogisimMenuBar.SELECT_ALL, table.getRowCount() > 0);
      setEnabled(LogisimMenuBar.RAISE, false);
      setEnabled(LogisimMenuBar.LOWER, false);
      setEnabled(LogisimMenuBar.RAISE_TOP, false);
      setEnabled(LogisimMenuBar.LOWER_BOTTOM, false);
      setEnabled(LogisimMenuBar.ADD_CONTROL, false);
      setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
    }

    @Override
    public void copy() {
      requestFocus();
      clip.copy();
    }

    @Override
    public void paste() {
      requestFocus();
      clip.paste();
    }

    @Override
    public void selectAll() {
      caret.selectAll();
    }

    @Override
    public void delete() {
      requestFocus();
      Rectangle s = caret.getSelection();
      int inputs = table.getInputColumnCount();
      for (int c = s.x; c < s.x + s.width; c++) {
        if (c < inputs)
          continue; // todo: allow input row delete?
        for (int r = s.y; r < s.y + s.height; r++) {
          table.setVisibleOutputEntry(r, c - inputs, Entry.DONT_CARE);
        }
      }
    }
  };
  
  @Override
  PrintHandler getPrintHandler() {
    return printHandler;
  }

  PrintHandler printHandler = new PrintHandler() {
    @Override
    public Dimension getExportImageSize() {
      int width = tableWidth;
      int height = headerHeight + bodyHeight;
      return new Dimension(width, height);
    }
    
    @Override
    public void paintExportImage(BufferedImage img, Graphics2D g) {
      int width = img.getWidth();
      int height = img.getHeight();
      g.setClip(0, 0, width, height);
      header.paintComponent(g, true, width, headerHeight);
      g.translate(0, headerHeight);
      body.paintComponent(g, true, width, bodyHeight);
    }
    
    @Override
    public int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h) {
      FontMetrics fm = g.getFontMetrics();

      // shrink horizontally to fit
      double scale = 1.0;
      if (tableWidth > w)
        scale = w / tableWidth;

      // figure out how many pages we will need
      int n = getRowCount();
      double headHeight = (fm.getHeight() * 1.5 + headerHeight * scale);
      int rowsPerPage = (int)((h - headHeight) / (cellHeight * scale));
      int numPages = (n + rowsPerPage - 1) / rowsPerPage;
      if (pageNum >= numPages)
        return Printable.NO_SUCH_PAGE;

      // g.drawRect(0, 0, (int)w-1, (int)h-1); // bage border
      GraphicsUtil.drawText(g,
          String.format("Combinational Analysis (page %d of %d)", pageNum+1, numPages),
          (int)(w/2), 0, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);

      g.translate(0, fm.getHeight() * 1.5);
      g.scale(scale, scale);
      header.paintComponent(g, true, (int)(w/scale), headerHeight);
      g.translate(0, headerHeight);

      int yHeight = cellHeight * rowsPerPage;
      int yTop = pageNum * yHeight;
      g.translate(0, -yTop);
      g.setClip(0, yTop, (int)(w/scale), yHeight);
      body.paintComponent(g, true, (int)(w/scale), bodyHeight);

      return Printable.PAGE_EXISTS;
    }
  };

  @Override
  public void EntryDesriptionChanged() { this.repaint(); }
}
