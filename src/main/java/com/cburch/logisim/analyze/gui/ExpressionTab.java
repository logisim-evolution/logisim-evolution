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

import com.cburch.logisim.analyze.gui.ExpressionView.NamedExpression;
import com.cburch.logisim.analyze.gui.MinimizedTab.NotationModel;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expression.Notation;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;
import com.cburch.logisim.analyze.model.Parser;
import com.cburch.logisim.analyze.model.ParserException;
import com.cburch.logisim.analyze.model.VariableListEvent;
import com.cburch.logisim.analyze.model.VariableListListener;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.PrintHandler;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.EventObject;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import org.jdesktop.swingx.prompt.BuddySupport;

class ExpressionTab extends AnalyzerTab {
  private static final long serialVersionUID = 1L;
  private final AnalyzerModel model;
  private StringGetter errorMessage;
  private final ExpressionTableModel tableModel;

  private final JTable table = new JTable(1, 1);
  private final JLabel error = new JLabel();

  public class ExpressionTableModel extends AbstractTableModel
      implements VariableListListener, OutputExpressionsListener {
    private static final long serialVersionUID = 1L;
    NamedExpression[] listCopy;

    public ExpressionTableModel() {
      updateCopy();
      model.getOutputs().addVariableListListener(this);
      model.getOutputExpressions().addOutputExpressionsListener(this);
    }

    @Override
    public void fireTableChanged(TableModelEvent event) {
      TableModelListener listener;
      final var list = listenerList.getListenerList();
      for (var index = 0; index < list.length; index += 2) {
        listener = (TableModelListener) list[index + 1];
        listener.tableChanged(event);
      }
    }

    @Override
    public void setValueAt(Object obj, int row, int column) {
      final var ne = listCopy[row];
      if (!(obj instanceof NamedExpression e)) return;
      if (ne != e && !ne.name.equals(e.name)) return;
      listCopy[row] = e;
      if (e.expr != null) model.getOutputExpressions().setExpression(e.name, e.expr, e.exprString);
    }

    @Override
    public Object getValueAt(int row, int col) {
      return listCopy[row];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return true;
    }

    @Override
    public int getColumnCount() {
      return 1;
    }

    @Override
    public String getColumnName(int column) {
      return "";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return NamedExpression.class;
    }

    @Override
    public int getRowCount() {
      return listCopy.length;
    }

    @Override
    public void expressionChanged(OutputExpressionsEvent event) {
      if (event.getType() == OutputExpressionsEvent.OUTPUT_EXPRESSION) {
        final var name = event.getVariable();
        int idx = -1;
        for (final var e : listCopy) {
          idx++;
          if (e.name.equals(name)) {
            try {
              e.expr = model.getOutputExpressions().getExpression(name);
              e.err = null;
            } catch (Exception ex) {
              e.expr = null;
              e.err = ex.getMessage();
            }
            fireTableRowsUpdated(idx, idx);
            break;
          }
        }
      }
    }

    @Override
    public void listChanged(VariableListEvent event) {
      updateCopy();
      final var idx = event.getIndex();
      switch (event.getType()) {
        case VariableListEvent.ALL_REPLACED, VariableListEvent.MOVE -> {
          fireTableDataChanged();
          return;
        }
        case VariableListEvent.ADD -> {
          fireTableRowsInserted(idx, idx);
          return;
        }
        case VariableListEvent.REMOVE -> {
          fireTableRowsDeleted(idx, idx);
          return;
        }
        case VariableListEvent.REPLACE -> {
          fireTableRowsUpdated(idx, idx);
          return;
        }
        default -> {
          // none
        }
      }
    }

    void update() {
      updateCopy();
      fireTableDataChanged();
    }

    void updateCopy() {
      final var outputs = model.getOutputs();
      final var n = outputs.bits.size();
      listCopy = new NamedExpression[n];
      var i = -1;
      for (final var name : outputs.bits) {
        i++;
        listCopy[i] = new NamedExpression(name);
        try {
          listCopy[i].expr = model.getOutputExpressions().getExpression(name);
          listCopy[i].err = null;
        } catch (Exception e) {
          listCopy[i].err = e.getMessage();
        }
      }
    }
  }

  public class ExpressionTableCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Color fg;
      Color bg;
      if (isSelected) {
        fg = table.getSelectionForeground();
        bg = table.getSelectionBackground();
      } else {
        fg = table.getForeground();
        bg = table.getBackground();
      }
      prettyView.setWidth(table.getColumnModel().getColumn(0).getWidth());
      prettyView.setExpression((NamedExpression) value);
      prettyView.setForeground(fg);
      prettyView.setBackground(bg);
      final var h = prettyView.getExpressionHeight();
      prettyView.setNotation(notation);
      if (table.getRowHeight(row) != h) table.setRowHeight(row, h);
      return prettyView;
    }
  }

  public class ExpressionEditor extends AbstractCellEditor implements TableCellEditor {
    private static final long serialVersionUID = 1L;
    final JTextField field = new JTextField();
    final JLabel label = new JLabel();
    NamedExpression oldExpr;
    NamedExpression newExpr;

    public ExpressionEditor() {
      field.setBorder(
          BorderFactory.createCompoundBorder(
              field.getBorder(), BorderFactory.createEmptyBorder(1, 3, 1, 3)));
      BuddySupport.addLeft(label, field);
    }

    @Override
    public Object getCellEditorValue() {
      return newExpr;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table,
        Object value, boolean isSelected, int row, int column) {
      oldExpr = (NamedExpression) value;
      label.setText(" " + Expressions.variable(oldExpr.name) + " = ");
      field.setText((oldExpr.expr != null) ? oldExpr.expr.toString() : "");
      return field;
    }

    @Override
    public boolean stopCellEditing() {
      if (ok()) {
        super.stopCellEditing();
        oldExpr = null;
        return true;
      } else {
        return false;
      }
    }

    boolean ok() {
      final var exprString = field.getText();
      try {
        final var expr = Parser.parse(exprString, model);
        setError(null);
        newExpr = new NamedExpression(oldExpr.name, expr, exprString);
        return true;
      } catch (ParserException ex) {
        setError(ex.getMessageGetter());
        field.setCaretPosition(ex.getOffset());
        field.moveCaretPosition(ex.getEndOffset());
        newExpr = null;
        return false;
      }
    }

    @Override
    public boolean isCellEditable(EventObject e) {
      if (e instanceof MouseEvent me) {
        return me.getClickCount() >= 2;
      }
      if (e instanceof KeyEvent ke) {
        return (ke.getKeyCode() == KeyEvent.VK_F2
            || ke.getKeyCode() == KeyEvent.VK_ENTER);
      }
      return false;
    }
  }

  private class MyListener implements ItemListener {

    @Override
    public void itemStateChanged(ItemEvent event) {
      if (event.getSource() == notationChoice) {
        final var not = Notation.values()[notationChoice.getSelectedIndex()];
        if (not != notation) {
          notation = not;
          tableModel.fireTableStructureChanged();
        }
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private final JComboBox notationChoice = new JComboBox<>(new NotationModel());
  private final JLabel notationLabel = new JLabel();
  private final JLabel infoLabel = new JLabel();
  private Notation notation = Notation.MATHEMATICAL;
  private final MyListener myListener = new MyListener();
  private final ExpressionView prettyView = new ExpressionView();

  public ExpressionTab(AnalyzerModel model, LogisimMenuBar menubar) {
    localeChanged();
    this.model = model;
    tableModel = new ExpressionTableModel();
    table.setModel(tableModel);
    table.setShowGrid(false);
    table.setTableHeader(null);
    table.setDefaultRenderer(NamedExpression.class, new ExpressionTableCellRenderer());
    table.setDefaultEditor(NamedExpression.class, new ExpressionEditor());
    table.setDragEnabled(true);
    final var ccp = new ExpressionTransferHandler();
    table.setTransferHandler(ccp);
    table.setDropMode(DropMode.ON);

    final var inputMap = table.getInputMap();
    for (final var item : LogisimMenuBar.EDIT_ITEMS) {
      final var accel = menubar.getAccelerator(item);
      inputMap.put(accel, item);
    }

    final var actionMap = table.getActionMap();
    actionMap.put(LogisimMenuBar.COPY, TransferHandler.getCopyAction());
    actionMap.put(LogisimMenuBar.PASTE, TransferHandler.getPasteAction());
    final var gbl = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    setLayout(gbl);
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    gbc.weightx = 1.0;
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.BOTH;
    final var control = control();
    gbl.setConstraints(control, gbc);
    add(control);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(infoLabel, gbc);
    add(infoLabel);

    final var scroll = new JScrollPane(table,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setPreferredSize(new Dimension(AppPreferences.getScaled(60), AppPreferences.getScaled(100)));
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(scroll, gbc);
    add(scroll);

    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(error, gbc);
    add(error);

    final var f =
        new FocusListener() {
          @Override
          public void focusGained(FocusEvent e) {
            if (e.isTemporary()) return;
            editHandler.computeEnabled();
          }

          @Override
          public void focusLost(FocusEvent e) {
            if (e.isTemporary()) return;
            editHandler.computeEnabled();
          }
        };
    addFocusListener(f);
    table.addFocusListener(f);

    setError(null);
  }

  private JPanel control() {
    final var control = new JPanel();
    final var gbl = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    control.setLayout(gbl);
    gbc.weightx = 1.0;
    gbc.gridwidth = 1;
    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.VERTICAL;
    gbc.anchor = GridBagConstraints.EAST;
    gbc.insets = new Insets(3, 10, 3, 10);
    gbl.setConstraints(notationLabel, gbc);
    control.add(notationLabel);
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbl.setConstraints(notationChoice, gbc);
    control.add(notationChoice);
    notationChoice.addItemListener(myListener);
    return control;
  }

  @Override
  void localeChanged() {
    if (errorMessage != null) {
      error.setText(errorMessage.toString());
    }
    infoLabel.setText(S.get("outputExpressionEdit"));
    notationLabel.setText(S.get("ExpressionNotation"));
  }

  private void setError(StringGetter msg) {
    if (msg == null) {
      errorMessage = null;
      error.setText(" ");
    } else {
      errorMessage = msg;
      error.setText(msg.toString());
    }
  }

  @Override
  void updateTab() {
    tableModel.update();
  }

  @Override
  EditHandler getEditHandler() {
    return editHandler;
  }

  final EditHandler editHandler = new EditHandler() {
    @Override
    public void computeEnabled() {
      final var viewing = table.getSelectedRow() >= 0;
      final var editing = table.isEditing();
      setEnabled(LogisimMenuBar.CUT, editing);
      setEnabled(LogisimMenuBar.COPY, viewing);
      setEnabled(LogisimMenuBar.PASTE, viewing);
      setEnabled(LogisimMenuBar.DELETE, editing);
      setEnabled(LogisimMenuBar.DUPLICATE, false);
      setEnabled(LogisimMenuBar.SELECT_ALL, editing);
      setEnabled(LogisimMenuBar.RAISE, false);
      setEnabled(LogisimMenuBar.LOWER, false);
      setEnabled(LogisimMenuBar.RAISE_TOP, false);
      setEnabled(LogisimMenuBar.LOWER_BOTTOM, false);
      setEnabled(LogisimMenuBar.ADD_CONTROL, false);
      setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final var action = e.getSource();
      if (table.getSelectedRow() < 0) return;
      table.getActionMap().get(action).actionPerformed(e);
    }
  };

  private class ExpressionTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
      String s;
      try {
        s = (String) info.getTransferable().getTransferData(DataFlavor.stringFlavor);
      } catch (Exception e) {
        setError(S.getter("cantImportFormatError"));
        return false;
      }

      Expression expr;
      try {
        expr = Parser.parseMaybeAssignment(s, model);
        setError(null);
      } catch (ParserException ex) {
        setError(ex.getMessageGetter());
        return false;
      }
      if (expr == null)
        return false;

      var idx = -1;
      if (table.getRowCount() == 0) {
        return false;
      }
      if (table.getRowCount() == 1) {
        idx = 0;
      } else if (info.isDrop()) {
        try {
          final var dl = (JTable.DropLocation) info.getDropLocation();
          idx = dl.getRow();
        } catch (ClassCastException ignored) {
          // do nothing
        }
      } else {
        idx = table.getSelectedRow();
        if (idx < 0 && Expression.isAssignment(expr)) {
          final var v = Expression.getAssignmentVariable(expr);
          for (idx = table.getRowCount() - 1; idx >= 0; idx--) {
            final var ne = (NamedExpression) table.getValueAt(idx, 0);
            if (v.equals(ne.name)) break;
          }
        }
      }
      if (idx < 0 || idx >= table.getRowCount()) return false;
      if (Expression.isAssignment(expr)) expr = Expression.getAssignmentExpression(expr);

      final var ne = (NamedExpression) table.getValueAt(idx, 0);
      ne.exprString = s;
      ne.expr = expr;
      ne.err = null;
      table.setValueAt(ne, idx, 0);

      return true;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
      final var idx = table.getSelectedRow();
      if (idx < 0) return null;
      final var ne = (NamedExpression) table.getValueAt(idx, 0);
      final var s = ne.expr != null ? ne.expr.toString(notation) : ne.err;
      return s == null ? null : new StringSelection(ne.name + " = " + s);
    }

    @Override
    public int getSourceActions(JComponent c) {
      return COPY;
    }

    @Override
    protected void exportDone(JComponent c, Transferable tdata, int action) {
      // dummy
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      return table.getRowCount() > 0 && support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }
  }

  @Override
  PrintHandler getPrintHandler() {
    return printHandler;
  }

  final PrintHandler printHandler =
      new PrintHandler() {
        @Override
        public Dimension getExportImageSize() {
          final var width = table.getWidth();
          var height = 14;
          final var n = table.getRowCount();
          for (var i = 0; i < n; i++) {
            final var ne = (NamedExpression) table.getValueAt(i, 0);
            prettyView.setWidth(width);
            prettyView.setExpression(ne);
            height += prettyView.getExpressionHeight() + 14;
          }
          return new Dimension(width + 6, height);
        }

        @Override
        public void paintExportImage(BufferedImage img, Graphics2D g) {
          final var width = img.getWidth();
          final var height = img.getHeight();
          g.setClip(0, 0, width, height);
          g.translate(6 / 2, 14);
          final var n = table.getRowCount();
          for (var i = 0; i < n; i++) {
            final var ne = (NamedExpression) table.getValueAt(i, 0);
            prettyView.setForeground(Color.BLACK);
            prettyView.setBackground(Color.WHITE);
            prettyView.setWidth(width - 6);
            prettyView.setExpression(ne);
            final var rh = prettyView.getExpressionHeight();
            prettyView.setSize(new Dimension(width - 6, rh));
            prettyView.paintComponent(g);
            g.translate(0, rh + 14);
          }
        }

        @Override
        public int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h) {
          final var width = (int) Math.ceil(w);
          g.translate(6 / 2, 14 / 2);

          final var n = table.getRowCount();
          var y = 0;
          var pg = 0;
          for (var i = 0; i < n; i++) {
            final var ne = (NamedExpression) table.getValueAt(i, 0);
            prettyView.setWidth(width - 6);
            prettyView.setForeground(Color.BLACK);
            prettyView.setBackground(Color.WHITE);
            prettyView.setExpression(ne);
            int rh = prettyView.getExpressionHeight();
            if (y > 0 && y + rh > h) {
              // go to next page
              y = 0;
              pg++;
              if (pg > pageNum) return Printable.PAGE_EXISTS; // done the page we wanted
            }
            if (pg == pageNum) {
              prettyView.setSize(new Dimension(width - 6, rh));
              prettyView.paintComponent(g);
              g.translate(0, rh + 14);
            }
            y += rh + 14;
          }
          return (pg < pageNum ? Printable.NO_SUCH_PAGE : Printable.PAGE_EXISTS);
        }
      };
}
