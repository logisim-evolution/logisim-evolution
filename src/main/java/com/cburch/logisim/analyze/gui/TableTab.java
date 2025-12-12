/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.TruthTableEvent;
import com.cburch.logisim.analyze.model.TruthTableListener;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ZoomIcon;
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
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

class TableTab extends AnalyzerTab implements Entry.EntryChangedListener {
  private class MyListener implements TruthTableListener, LocaleListener {
    @Override
    public void rowsChanged(TruthTableEvent event) {
      updateTable();
    }

    @Override
    public void cellsChanged(TruthTableEvent event) {
      repaint();
    }

    @Override
    public void structureChanged(TruthTableEvent event) {
      updateTable();
    }

    void updateTable() {
      computePreferredSize();
      expand.setEnabled(getRowCount() < table.getRowCount());
      count.setText(S.get("tableRowsShown", getRowCount(), table.getRowCount()));
      body.setSize(new Dimension(body.getWidth(), table.getRowCount() * cellHeight));
      repaint();
    }

    @Override
    public void localeChanged() {
      expand.setText(S.get("tableExpand"));
      compact.setText(S.get("tableCompact"));
      count.setText(S.get("tableRowsShown", getRowCount(), table.getRowCount()));
    }
  }

  private static final long serialVersionUID = 1L;
  private static final double ZOOM_FACTOR_STEP = 0.1;
  private static final double MIN_ZOOM = 1.0;
  private static final double MAX_ZOOM = 5.0;

  private Font headFont;
  private Font bodyFont;
  private int headerPadding;
  private int headerVertSep;
  private int headerHorizSep;
  private int defaultCellPadding;
  private int defaultCellWidth;
  private int defaultCellHeight;
  private double zoomFactor = 1.0;

  private final MyListener myListener = new MyListener();
  private final TruthTable table;
  private final TableBody body;
  private final TableHeader header;
  private final JScrollPane bodyPane;
  private final JScrollPane headerPane;
  private int cellHeight;
  private int tableWidth;
  private int headerHeight;
  private int bodyHeight;
  private final ColumnGroupDimensions inDim;
  private final ColumnGroupDimensions outDim;
  private final TableTabCaret caret;
  private final TableTabClip clip;

  List<Var> inputVars;
  List<Var> outputVars;

  private class ColumnGroupDimensions {
    int cellWidth;
    int cellPadding;
    int leftPadding;
    int rightPadding;
    int width;
    List<Var> vars;

    ColumnGroupDimensions(List<Var> vars) {
      this.vars = vars;
    }

    void reset(List<Var> vars) {
      this.vars = vars;
      leftPadding = defaultCellPadding / 2;
      rightPadding = defaultCellPadding / 2;
      cellPadding = defaultCellPadding;
      cellWidth = defaultCellWidth;
    }

    void calculate(FontMetrics fm) {
      for (int i = 1; i < vars.size(); i++) {
        final var v1 = vars.get(i - 1);
        final var v2 = vars.get(i);
        final var hw1 = fm.stringWidth(v1.toString());
        final var hw2 = fm.stringWidth(v2.toString());
        final var hw = (hw1 - hw1 / 2) + headerPadding + (hw2 / 2);
        final var cw1 = v1.width * cellWidth;
        final var cw2 = v2.width * cellWidth;
        final var cw = (cw1 - cw1 / 2) + cellPadding + (cw2 / 2);
        if (hw > cw) cellPadding += (hw - cw);
      }
      Var v;
      int w;
      v = vars.get(0);
      w = fm.stringWidth(v.toString());
      leftPadding = Math.max(defaultCellPadding / 2, (w / 2) - (cellWidth * v.width / 2));
      v = vars.get(vars.size() - 1);
      w = fm.stringWidth(v.toString());
      rightPadding =
          Math.max(
              defaultCellPadding / 2,
              (w - w / 2) - (cellWidth * v.width - cellWidth * v.width / 2));
      calculateWidth();
    }

    void calculateWidth() {
      var w = -cellPadding;
      for (Var v : vars) w += cellPadding + v.width * cellWidth;
      width = leftPadding + w + rightPadding;
    }

    int getColumn(int x) {
      if (x < leftPadding) return -1;
      x -= leftPadding;
      var col = 0;
      for (final var v : vars) {
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
      var col = 0;
      for (final var var : vars) {
        if (x < -(cellPadding / 2)) return col - 1;
        if (x < 0) return col;
        if (x < var.width * cellWidth) return col + x / cellWidth;
        col += var.width;
        x -= var.width * cellWidth + cellPadding;
      }
      return col - 1;
    }

    int getX(int col) {
      var x = leftPadding;
      for (final var var : vars) {
        if (col < 0) return x;
        if (col < var.width) return x + col * cellWidth;
        col -= var.width;
        x += var.width * cellWidth + cellPadding;
      }
      return x;
    }

    void paintHeaders(Graphics g, int x, int y) {
      final var fm = g.getFontMetrics();
      y += fm.getAscent() + 1;
      x += leftPadding;
      for (final var var : vars) {
        final var s = var.toString();
        final var sx = x + (var.width * cellWidth) / 2;
        final var sw = fm.stringWidth(s);
        g.drawString(s, sx - (sw / 2), y);
        x += (var.width * cellWidth) + cellPadding;
      }
    }

    void paintRow(Graphics g, FontMetrics fm, int x, int y, int row, boolean isInput) {
      x += leftPadding;
      final var cy = y + fm.getAscent();
      var col = 0;
      for (final var var : vars) {
        for (var b = var.width - 1; b >= 0; b--) {
          final var entry =
              isInput
                  ? table.getVisibleInputEntry(row, col++)
                  : table.getVisibleOutputEntry(row, col++);
          if (entry.isError()) {
            g.setColor(Value.errorColor);
            g.fillRect(x, y, cellWidth, cellHeight);
            g.setColor(Color.BLACK);
          }
          g.setColor(entry == Entry.BUS_ERROR ? Value.errorColor : Color.BLACK);
          final var label = entry.getDescription();
          final var width = fm.stringWidth(label);
          g.drawString(label, x + (cellWidth - width) / 2, cy);
          x += cellWidth;
        }
        x += cellPadding;
      }
    }
  }

  private final SquareButton one = new SquareButton(Entry.ONE);
  private final SquareButton zero = new SquareButton(Entry.ZERO);
  private final SquareButton dontcare = new SquareButton(Entry.DONT_CARE);
  private final TightButton expand = new TightButton(S.get("tableExpand"));
  private final TightButton compact = new TightButton(S.get("tableCompact"));
  private final JButton zoomIn = new JButton(new ZoomIcon(ZoomIcon.ZOOMIN));
  private final JButton zoomOut = new JButton(new ZoomIcon(ZoomIcon.ZOOMOUT));
  private final JLabel count = new JLabel(S.get("tableRowsShown", 0, 0), SwingConstants.CENTER);

  private static class TightButton extends JButton {
    private static final long serialVersionUID = 1L;

    TightButton(String s) {
      super(s);
      setMargin(new Insets(0, 0, 0, 0));
    }
  }

  private static class SquareButton extends TightButton implements Entry.EntryChangedListener {
    private static final long serialVersionUID = 1L;

    final Entry myEntry;

    SquareButton(Entry e) {
      super(e.getDescription());
      myEntry = e;
    }

    @Override
    public Dimension getPreferredSize() {
      final var d = super.getPreferredSize();
      final var s = (int) d.getHeight();
      return new Dimension(s, s);
    }

    @Override
    public void entryDesriptionChanged() {
      setText(myEntry.getDescription());
      repaint();
    }
  }

  public TableTab(TruthTable table) {
    this.table = table;
    updateScale();
    header = new TableHeader();
    body = new TableBody();
    bodyPane =
        new JScrollPane(
            body,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    bodyPane.addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent event) {
            int width = bodyPane.getViewport().getWidth();
            body.setSize(new Dimension(width, body.getHeight()));
          }
        });
    bodyPane.setVerticalScrollBar(getVerticalScrollBar());
    
    // Handle zooming with Ctrl + Mouse Wheel
    bodyPane.addMouseWheelListener(e -> {
      if (e.isControlDown()) {
        if (e.getWheelRotation() < 0) {
          zoomIn();
        } else {
          zoomOut();
        }
        e.consume();
      } else {
        // Forward to vertical scrollbar if Ctrl is not down (normal scrolling)
        bodyPane.dispatchEvent(SwingUtilities.convertMouseEvent(bodyPane, e, bodyPane.getParent()));
      }
    });

    headerPane =
        new JScrollPane(
            header,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    headerPane.addComponentListener(
        new ComponentAdapter() {
          @Override
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

    final var toolbar = new JPanel();
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
    final var sep = new JSeparator(JSeparator.VERTICAL);
    sep.setPreferredSize(new Dimension(3, 20));
    toolbar.add(sep);
    toolbar.add(zoomOut);
    toolbar.add(zoomIn);
    
    one.setActionCommand("1");
    zero.setActionCommand("0");
    dontcare.setActionCommand("x");
    compact.setActionCommand("compact");
    expand.setActionCommand("expand");
    
    zoomIn.addActionListener(e -> zoomIn());
    zoomOut.addActionListener(e -> zoomOut());
    // Basic styling for zoom buttons
    zoomIn.setMargin(new Insets(2, 2, 2, 2));
    zoomOut.setMargin(new Insets(2, 2, 2, 2));
    zoomIn.setToolTipText(S.get("tableZoomIn") + " (Ctrl + Plus)");
    zoomOut.setToolTipText(S.get("tableZoomOut") + " (Ctrl + Minus)");


    expand.setEnabled(getRowCount() < table.getRowCount());
    count.setText(S.get("tableRowsShown", getRowCount(), table.getRowCount()));

    final var gbl = new GridBagLayout();
    setLayout(gbl);
    final var gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbl.setConstraints(toolbar, gbc);
    add(toolbar);
    gbc.gridy++;
    gbl.setConstraints(count, gbc);
    add(count);
    gbc.gridy++;
    gbl.setConstraints(headerPane, gbc);
    add(headerPane);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy++;
    gbc.weighty = 1;
    gbl.setConstraints(bodyPane, gbc);
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
    this.addComponentListener(
        new ComponentAdapter() {
          boolean done;

          @Override
          public void componentShown(ComponentEvent e) {
            TableTab.this.removeComponentListener(this);
            if (done) return;
            done = true;
            // account for missing scrollbar on header portion
            final var pad = bodyPane.getVerticalScrollBar().getWidth();
            GridBagConstraints gbc = gbl.getConstraints(headerPane);
            Insets i = gbc.insets;
            gbc.insets.set(i.top, i.left, i.bottom, i.right + pad);
            gbl.setConstraints(headerPane, gbc);
            invalidate();
            repaint();
            
            setupKeyBindings();
          }
        });
    editHandler.computeEnabled();
    LocaleManager.addLocaleListener(myListener);
  }
  
  private void setupKeyBindings() {
    InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = getActionMap();

    // Zoom In (Ctrl + '+')
    KeyStroke ctrlPlus = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK);
    KeyStroke ctrlAdd = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK);
    inputMap.put(ctrlPlus, "zoomIn");
    inputMap.put(ctrlAdd, "zoomIn");
    actionMap.put("zoomIn", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
          zoomIn();
      }
    });

    // Zoom Out (Ctrl + '-')
    KeyStroke ctrlMinus = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK);
    KeyStroke ctrlSubtract = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_DOWN_MASK);
    inputMap.put(ctrlMinus, "zoomOut");
    inputMap.put(ctrlSubtract, "zoomOut");
    actionMap.put("zoomOut", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
          zoomOut();
      }
    });
    
    // Reset Zoom (Ctrl + 0)
    KeyStroke ctrlZero = KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK);
    KeyStroke ctrlNumpadZero = KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, KeyEvent.CTRL_DOWN_MASK);
    inputMap.put(ctrlZero, "zoomReset");
    inputMap.put(ctrlNumpadZero, "zoomReset");
    actionMap.put("zoomReset", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
          zoomReset();
      }
    });
  }
  
  private void updateScale() {
    final var baseSize = 18.0f;
    final var scaledSize = (float) (AppPreferences.getScaled(baseSize) * zoomFactor);
    final var myFont = new Font("Serif", Font.PLAIN, (int) scaledSize);
    headFont = myFont.deriveFont(Font.BOLD);
    bodyFont = myFont;
    headerPadding = (int) (AppPreferences.getScaled(10) * zoomFactor);
    headerVertSep = (int) (AppPreferences.getScaled(4) * zoomFactor);
    headerHorizSep = (int) (AppPreferences.getScaled(4) * zoomFactor);
    defaultCellPadding = (int) (AppPreferences.getScaled(15) * zoomFactor);
    defaultCellWidth = (int) (AppPreferences.getScaled(15) * zoomFactor);
    defaultCellHeight = (int) (AppPreferences.getScaled(22) * zoomFactor);
  }
  
  private void refreshTable() {
    updateScale();
    computePreferredSize();
    repaint();
    revalidate();
  }

  private void zoomReset() {
    zoomFactor = 1.0;
    refreshTable();
  }
  
  private void zoomIn() {
    if (zoomFactor < MAX_ZOOM) {
      zoomFactor += ZOOM_FACTOR_STEP;
      refreshTable();
    }
  }

  private void zoomOut() {
    if (zoomFactor > MIN_ZOOM) {
      zoomFactor -= ZOOM_FACTOR_STEP;
      refreshTable();
    }
  }

  public JPanel getBody() {
    return body;
  }

  private static final Canvas canvas = new Canvas();

  private void computePreferredSize() {
    inputVars = table.getInputVariables();
    outputVars = table.getOutputVariables();
    if (inputVars.isEmpty()) {
      inputVars = new ArrayList<>();
      inputVars.add(new Var(S.get("tableNoInputs"), 0));
    }

    if (outputVars.isEmpty()) {
      outputVars = new ArrayList<>();
      outputVars.add(new Var(S.get("tableNoOutputs"), 0));
    }

    cellHeight = defaultCellHeight;
    inDim.reset(inputVars);
    outDim.reset(outputVars);
    final var gfx = getGraphics();
    final var fm = (gfx != null ? gfx.getFontMetrics(headFont) : canvas.getFontMetrics(headFont));
    cellHeight = fm.getHeight();
    inDim.calculate(fm);
    outDim.calculate(fm);

    tableWidth = inDim.width + headerHorizSep + outDim.width;

    computePreferredHeight();
  }

  private void computePreferredHeight() {
    bodyHeight = cellHeight * getRowCount();
    headerHeight = headerVertSep + cellHeight + headerVertSep;
    final int tableHeight = cellHeight + headerHeight;

    header.setPreferredSize(new Dimension(tableWidth, headerHeight));
    headerPane.setMinimumSize(new Dimension(tableWidth, headerHeight));
    headerPane.setPreferredSize(new Dimension(tableWidth, headerHeight));

    body.setPreferredSize(new Dimension(tableWidth, bodyHeight));
    bodyPane.setPreferredSize(new Dimension(tableWidth, 1));

    setPreferredSize(new Dimension(tableWidth + 40, tableHeight));
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
    final var left = (body.getWidth() - tableWidth) / 2;
    final var mid = left + inDim.width + headerHorizSep;
    final var x = event.getX();
    if (x < mid) {
      return inDim.getColumn(x - left);
    } else {
      int c = outDim.getColumn(x - mid);
      return c < 0 ? -1 : table.getInputColumnCount() + c;
    }
  }

  public int getNearestColumn(MouseEvent event) {
    final var inputs = table.getInputColumnCount();
    final var outputs = table.getOutputColumnCount();
    if (inputs + outputs == 0) return -1;
    final var left = (body.getWidth() - tableWidth) / 2;
    final var mid = left + inDim.width + headerHorizSep;
    final var x = event.getX();
    if (x < left) return 0;
    else if (x >= mid + outDim.width) return inputs + outputs - 1;
    else if (x < mid - headerHorizSep / 2 && inputs > 0) return inDim.getNearestColumn(x - left);
    else return inputs + outDim.getNearestColumn(x - mid);
  }

  int getColumnCount() {
    final var inputs = table.getInputColumnCount();
    final var outputs = table.getOutputColumnCount();
    return inputs + outputs;
  }

  public int getOutputColumn(MouseEvent event) {
    final var c = getColumn(event);
    final var inputs = table.getInputColumnCount();
    return (c < inputs ? -1 : c - inputs);
  }

  public int getRow(MouseEvent event) {
    final var y = event.getY();
    if (y < 0) return -1;
    final var ret = y / cellHeight;
    final var rows = getRowCount();
    return ret >= 0 && ret < rows ? ret : -1;
  }

  public int getNearestRow(MouseEvent event) {
    final var y = event.getY();
    if (y < 0) return 0;
    final var ret = y / cellHeight;
    final var rows = getRowCount();
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
    final var row = getRow(event);
    if (row < 0) return null;
    final var col = getOutputColumn(event);
    if (col < 0) return null;
    final var entry = table.getVisibleOutputEntry(row, col);
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
        final var curY = getValue();
        final var curHeight = getVisibleAmount();
        var numCells = curHeight / cellHeight - 1;
        if (numCells <= 0) numCells = 1;
        if (direction > 0) {
          return curY > 0 ? numCells * cellHeight : numCells * cellHeight + headerVertSep;
        } else {
          return curY > cellHeight + headerVertSep
              ? numCells * cellHeight
              : numCells * cellHeight + headerVertSep;
        }
      }

      @Override
      public int getUnitIncrement(int direction) {
        final var curY = getValue();
        if (direction > 0) {
          return curY > 0 ? cellHeight : cellHeight + headerVertSep;
        } else {
          return curY > cellHeight + headerVertSep ? cellHeight : cellHeight + headerVertSep;
        }
      }
    };
  }

  int getXLeft(int col) {
    final var left = Math.max(0, (body.getWidth() - tableWidth) / 2);
    final var mid = left + inDim.width + headerHorizSep;
    final var inputs = table.getInputColumnCount();
    if (col < inputs) {
      return left + inDim.getX(col);
    } else {
      return mid + outDim.getX(col - inputs);
    }
  }

  int getXRight(int col) {
    final var left = Math.max(0, (body.getWidth() - tableWidth) / 2);
    final var mid = left + inDim.width + headerHorizSep;
    final var inputs = table.getInputColumnCount();
    if (col < inputs) {
      return left + inDim.getX(col) + inDim.cellWidth;
    } else {
      return mid + outDim.getX(col - inputs) + outDim.cellWidth;
    }
  }

  int getCellWidth(int col) {
    final var inputs = table.getInputColumnCount();
    return (col < inputs) ? inDim.cellWidth : outDim.cellWidth;
  }

  int getY(int row) {
    return row * cellHeight;
  }

  @Override
  void localeChanged() {
    computePreferredSize();
    repaint();
  }

  @Override
  void updateTab() {
    editHandler.computeEnabled();
  }

  private class TableBody extends JPanel {
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
      final var g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      if (!printView) {
        super.paintComponent(g);
        caret.paintBackground(g);
      }

      final var top = 0;
      final var left = Math.max(0, (canvasWidth - tableWidth) / 2);
      final var mid = left + inDim.width + headerHorizSep;

      g.setColor(Color.GRAY);
      final var lineX = left + inDim.width + headerHorizSep / 2;
      g.drawLine(lineX, top, lineX, top + bodyHeight);

      g.setFont(bodyFont);
      final var fm = g.getFontMetrics();
      int y = top;

      final var clip = g.getClipBounds();
      final var firstRow = Math.max(0, (clip.y - y) / cellHeight);
      final var lastRow = Math.min(getRowCount(), 2 + (clip.y + clip.height - y) / cellHeight);
      y += firstRow * cellHeight;

      for (var row = firstRow; row < lastRow; row++) {
        inDim.paintRow(g, fm, left, y, row, true);
        outDim.paintRow(g, fm, mid, y, row, false);
        y += cellHeight;
      }
      if (!printView) caret.paintForeground(g);
    }
  }

  private class TableHeader extends JPanel {
    private static final long serialVersionUID = 1L;

    @Override
    public void paintComponent(Graphics g) {
      paintComponent(g, false, getWidth(), getHeight());
    }

    public void paintComponent(Graphics g, boolean printView, int canvasWidth, int canvasHeight) {
      /* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
      final var g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      if (!printView) super.paintComponent(g);

      final var top = canvasHeight - cellHeight - headerVertSep;
      final var left = Math.max(0, (canvasWidth - tableWidth) / 2);

      g.setColor(Color.GRAY);
      final var lineX = left + inDim.width + headerHorizSep / 2;
      final var lineY = top + cellHeight + headerVertSep / 2;
      g.drawLine(left, lineY, left + tableWidth, lineY);
      g.drawLine(lineX, top, lineX, cellHeight);

      g.setColor(Color.BLACK);
      g.setFont(headFont);
      inDim.paintHeaders(g, left, top);
      outDim.paintHeaders(g, left + inDim.width + headerHorizSep, top);
    }
  }

  @Override
  EditHandler getEditHandler() {
    return editHandler;
  }

  final EditHandler editHandler =
      new EditHandler() {
        @Override
        public void computeEnabled() {
          final var sel = caret.hasSelection();
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
          final var s = caret.getSelection();
          final var inputs = table.getInputColumnCount();
          for (var c = s.x; c < s.x + s.width; c++) {
            if (c < inputs) continue; // TODO: allow input row delete?
            for (var r = s.y; r < s.y + s.height; r++) {
              table.setVisibleOutputEntry(r, c - inputs, Entry.DONT_CARE);
            }
          }
        }
      };

  @Override
  PrintHandler getPrintHandler() {
    return printHandler;
  }

  final PrintHandler printHandler =
      new PrintHandler() {
        @Override
        public Dimension getExportImageSize() {
          return new Dimension(tableWidth, headerHeight + bodyHeight);
        }

        @Override
        public void paintExportImage(BufferedImage img, Graphics2D g) {
          final var width = img.getWidth();
          final var height = img.getHeight();
          g.setClip(0, 0, width, height);
          header.paintComponent(g, true, width, headerHeight);
          g.translate(0, headerHeight);
          body.paintComponent(g, true, width, bodyHeight);
        }

        @Override
        public int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h) {
          final var fm = g.getFontMetrics();

          // shrink horizontally to fit
          var scale = 1.0;
          if (tableWidth > w) scale = w / tableWidth;

          // figure out how many pages we will need
          final var n = getRowCount();
          final var headHeight = (fm.getHeight() * 1.5 + headerHeight * scale);
          final var rowsPerPage = (int) ((h - headHeight) / (cellHeight * scale));
          final var numPages = (n + rowsPerPage - 1) / rowsPerPage;
          if (pageNum >= numPages) return Printable.NO_SUCH_PAGE;

          // g.drawRect(0, 0, (int)w-1, (int)h-1); // bage border
          GraphicsUtil.drawText(
              g,
              String.format("Combinational Analysis (page %d of %d)", pageNum + 1, numPages),
              (int) (w / 2),
              0,
              GraphicsUtil.H_CENTER,
              GraphicsUtil.V_TOP);

          g.translate(0, fm.getHeight() * 1.5);
          g.scale(scale, scale);
          header.paintComponent(g, true, (int) (w / scale), headerHeight);
          g.translate(0, headerHeight);

          final var heightY = cellHeight * rowsPerPage;
          final var topY = pageNum * heightY;
          g.translate(0, -topY);
          g.setClip(0, topY, (int) (w / scale), heightY);
          body.paintComponent(g, true, (int) (w / scale), bodyHeight);

          return Printable.PAGE_EXISTS;
        }
      };

  @Override
  public void entryDesriptionChanged() {
    this.repaint();
  }
}
