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
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.analyze.model.VariableListEvent;
import com.cburch.logisim.analyze.model.VariableListListener;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
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
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
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
  private AnalyzerModel model;
  private StringGetter errorMessage;
  private ExpressionTableModel tableModel;

  private JTable table = new JTable(1, 1);
  private JLabel error = new JLabel();

  public class ExpressionTableModel extends AbstractTableModel implements VariableListListener, OutputExpressionsListener {
    private static final long serialVersionUID = 1L;
    NamedExpression[] listCopy;
    
    public ExpressionTableModel() {
        updateCopy();
        model.getOutputs().addVariableListListener(this);
        model.getOutputExpressions().addOutputExpressionsListener(this);
    }
    
    @Override
    public void fireTableChanged(TableModelEvent event) {
      int    index;
      TableModelListener listener;
      Object[] list = listenerList.getListenerList();
      for (index = 0; index < list.length; index += 2)
      {
        listener = (TableModelListener) list [index + 1];
        listener.tableChanged(event);
      }
    }

    @Override
    public void setValueAt(Object o, int row, int column) {
      NamedExpression ne = listCopy[row];
      if (o == null || !(o instanceof NamedExpression))
        return;
      NamedExpression e = (NamedExpression)o;
      if (ne != e && !ne.name.equals(e.name))
        return;
      listCopy[row] = e;
      if (e.expr != null)
        model.getOutputExpressions().setExpression(e.name, e.expr, e.exprString);
    }

    @Override
    public Object getValueAt(int row, int col) {
      return listCopy[row];
    }

    @Override
    public boolean isCellEditable(int row, int column) { return true; }
    @Override
    public int getColumnCount() { return 1; };
    @Override
    public String getColumnName(int column) { return ""; }
    @Override
    public Class<?> getColumnClass(int columnIndex) { return NamedExpression.class; }
    @Override
    public int getRowCount() { return listCopy.length; }

    @Override
    public void expressionChanged(OutputExpressionsEvent event) {
      if (event.getType() == OutputExpressionsEvent.OUTPUT_EXPRESSION) {
    	String name = event.getVariable();
        int idx = -1;
        for (NamedExpression e : listCopy) {
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
      Integer idx = event.getIndex();
      switch (event.getType()) {
      case VariableListEvent.ALL_REPLACED:
        fireTableDataChanged();
        return;
      case VariableListEvent.ADD:
        fireTableRowsInserted(idx, idx);
        return;
      case VariableListEvent.REMOVE:
        fireTableRowsDeleted(idx, idx);
        return;
      case VariableListEvent.MOVE:
        fireTableDataChanged();
        return;
      case VariableListEvent.REPLACE:
        fireTableRowsUpdated(idx, idx);
        return;
      }
    }

    void update() {
        updateCopy();
        fireTableDataChanged();
    }

    void updateCopy() {
      VariableList outputs = model.getOutputs();
      int n = outputs.bits.size();
      listCopy = new NamedExpression[n];
      int i = -1;
      for (String name : outputs.bits) {
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
    public Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Color fg, bg;
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
      int h = prettyView.getExpressionHeight();
      prettyView.setNotation(notation);
      if (table.getRowHeight(row)!=h)
    	  table.setRowHeight(row, h);
      return prettyView;
    }
  }

  public class ExpressionEditor extends AbstractCellEditor implements TableCellEditor {
    private static final long serialVersionUID = 1L;
    JTextField field = new JTextField();
    JLabel label = new JLabel();
    NamedExpression oldExpr, newExpr;

    public ExpressionEditor() {
      field.setBorder(BorderFactory.createCompoundBorder(field.getBorder(),BorderFactory.createEmptyBorder(1, 3, 1, 3)));
      BuddySupport.addLeft(label, field);
    }

    @Override
    public Object getCellEditorValue() {
      return newExpr;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table,
        Object value, boolean isSelected, int row, int column) {
      newExpr = null;
      oldExpr = (NamedExpression)value;
      label.setText(" " + Expressions.variable(oldExpr.name).toString() + " = ");
      if (oldExpr.expr != null)
        field.setText(oldExpr.expr.toString());
      else
        field.setText("");
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
      String exprString = field.getText();
      try {
        Expression expr = Parser.parse(exprString, model);
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
      if (e instanceof MouseEvent) {
        MouseEvent me = (MouseEvent) e;
        return me.getClickCount() >= 2;
      }
      if (e instanceof KeyEvent) {
        KeyEvent ke = (KeyEvent) e;
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
	        Notation not = Notation.values()[notationChoice.getSelectedIndex()];
	        if (not != notation) {
	        	notation = not;
	        	tableModel.fireTableStructureChanged();
	        }
		}
	}
	  
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private JComboBox notationChoice = new JComboBox<>(new NotationModel());
  private JLabel notationLabel = new JLabel();
  private JLabel infoLabel = new JLabel();
  private Notation notation = Notation.MATHEMATICAL;
  private MyListener myListener = new MyListener();
  private ExpressionView prettyView = new ExpressionView();
  
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
    TransferHandler ccp = new ExpressionTransferHandler();
    table.setTransferHandler(ccp);
    table.setDropMode(DropMode.ON);
    
    InputMap inputMap = table.getInputMap();
    for (LogisimMenuItem item: LogisimMenuBar.EDIT_ITEMS) {
      KeyStroke accel = menubar.getAccelerator(item);
      inputMap.put(accel, item);
    }

    ActionMap actionMap = table.getActionMap();
    actionMap.put(LogisimMenuBar.COPY, ccp.getCopyAction());
    actionMap.put(LogisimMenuBar.PASTE, ccp.getPasteAction());
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    setLayout(gb);
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    
    gc.weightx = 1.0;
    gc.gridx = 0;
    gc.gridy = GridBagConstraints.RELATIVE;
    gc.weighty = 0.0;
    gc.fill = GridBagConstraints.BOTH;
    JPanel control = control();
    gb.setConstraints(control, gc);
    add(control);

    gc.fill = GridBagConstraints.HORIZONTAL;
    gb.setConstraints(infoLabel, gc);
    add(infoLabel);

    JScrollPane scroll = new JScrollPane(table,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setPreferredSize(new Dimension(AppPreferences.getScaled(60), AppPreferences.getScaled(100)));
    gc.weighty=1.0;
    gc.fill = GridBagConstraints.BOTH;
    gb.setConstraints(scroll, gc);
    add(scroll);

    gc.weighty = 0.0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gb.setConstraints(error, gc);
    add(error);
    
    FocusListener f = new FocusListener() {
      public void focusGained(FocusEvent e) {
        if (e.isTemporary()) return;
        editHandler.computeEnabled();
      }
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
    JPanel control = new JPanel();
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    control.setLayout(gb);
    gc.weightx = 1.0;
    gc.gridwidth = 1;
    gc.gridy = 0;
    gc.gridx = 0;
    gc.fill = GridBagConstraints.VERTICAL;
    gc.anchor = GridBagConstraints.EAST;
    gc.insets = new Insets(3, 10, 3, 10);
    gb.setConstraints(notationLabel, gc);
    control.add(notationLabel);
    gc.gridx = 1;
    gc.gridy = 0;
    gc.anchor = GridBagConstraints.WEST;
    gb.setConstraints(notationChoice, gc);
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

  EditHandler editHandler = new EditHandler() {
    public void computeEnabled() {
      boolean viewing = table.getSelectedRow() >= 0;
      boolean editing = table.isEditing();
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
      Object action = e.getSource();
      if (table.getSelectedRow() < 0)
        return;
      table.getActionMap().get(action).actionPerformed(e);
    }
  };

  private class ExpressionTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;

    public boolean importData(TransferHandler.TransferSupport info) {
      String s;
      try {
        s = (String)info.getTransferable().getTransferData(DataFlavor.stringFlavor);
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

      int idx = -1;
      if (table.getRowCount() == 0) {
          return false;
      } if (table.getRowCount() == 1) {
          idx = 0;
      } else if (info.isDrop()) {
        try {
          JTable.DropLocation dl = (JTable.DropLocation)info.getDropLocation();
          idx = dl.getRow();
        } catch (ClassCastException e) {
        }
      } else {
          idx = table.getSelectedRow();
          if (idx < 0 && Expression.isAssignment(expr)) {
            String v = Expression.getAssignmentVariable(expr);
            for (idx = table.getRowCount()-1; idx >= 0; idx--) {
              NamedExpression ne = (NamedExpression)table.getValueAt(idx, 0);
              if (v.equals(ne.name))
                break;
            }
          }
      }
      if (idx < 0 || idx >= table.getRowCount())
        return false;

      if (Expression.isAssignment(expr))
        expr = Expression.getAssignmentExpression(expr);

      NamedExpression ne = (NamedExpression)table.getValueAt(idx, 0);
      ne.exprString = s;
      ne.expr = expr;
      ne.err = null;
      table.setValueAt(ne, idx, 0);

      return true;
    }

    protected Transferable createTransferable(JComponent c) {
      int idx = table.getSelectedRow();
      if (idx < 0)
        return null;
      NamedExpression ne = (NamedExpression)table.getValueAt(idx, 0);
      String s = ne.expr != null ? ne.expr.toString(notation) : ne.err;
      return s == null ? null : new StringSelection(ne.name + " = " + s);
    }

    public int getSourceActions(JComponent c) {
      return COPY;
    }

    protected void exportDone(JComponent c, Transferable tdata, int action) { }

    public boolean canImport(TransferHandler.TransferSupport support) {
      return table.getRowCount() > 0
          && support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }
  }

  @Override
  PrintHandler getPrintHandler() {
    return printHandler;
  }

  PrintHandler printHandler = new PrintHandler() {
    @Override
    public Dimension getExportImageSize() {
      int width = table.getWidth();
      int height = 14;
      int n = table.getRowCount();
      for (int i = 0; i < n; i++) {
        NamedExpression ne = (NamedExpression)table.getValueAt(i, 0);
        prettyView.setWidth(width);
        prettyView.setExpression(ne);
        height += prettyView.getExpressionHeight() + 14;
      }
      return new Dimension(width + 6, height);
    }

    @Override
    public void paintExportImage(BufferedImage img, Graphics2D g) {
      int width = img.getWidth();
      int height = img.getHeight();
      g.setClip(0, 0, width, height);
      g.translate(6/2, 14);
      int n = table.getRowCount();
      for (int i = 0; i < n; i++) {
        NamedExpression ne = (NamedExpression)table.getValueAt(i, 0);
        prettyView.setForeground(Color.BLACK);
        prettyView.setBackground(Color.WHITE);
        prettyView.setWidth(width - 6);
        prettyView.setExpression(ne);
        int rh = prettyView.getExpressionHeight();
        prettyView.setSize(new Dimension(width-6, rh));
        prettyView.paintComponent(g);
        g.translate(0, rh + 14);
      }
    }

    @Override
    public int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h) {

      int width = (int)Math.ceil(w);
      g.translate(6/2, 14/2);

      int n = table.getRowCount();
      double y = 0;
      int pg = 0;
      for (int i = 0; i < n; i++) {
        NamedExpression ne = (NamedExpression)table.getValueAt(i, 0);
        prettyView.setWidth(width - 6);
        prettyView.setForeground(Color.BLACK);
        prettyView.setBackground(Color.WHITE);
        prettyView.setExpression(ne);
        int rh = prettyView.getExpressionHeight();
        if (y > 0 && y + rh > h) {
          // go to next page
          y = 0;
          pg++;
          if (pg > pageNum)
            return Printable.PAGE_EXISTS; // done the page we wanted
        }
        if (pg == pageNum) {
          prettyView.setSize(new Dimension(width-6, rh));
          prettyView.paintComponent(g);
          g.translate(0, rh + 14);
        }
        y += rh + 14;
      }
      return (pg < pageNum ? Printable.NO_SUCH_PAGE : Printable.PAGE_EXISTS);
    }
  };
}
