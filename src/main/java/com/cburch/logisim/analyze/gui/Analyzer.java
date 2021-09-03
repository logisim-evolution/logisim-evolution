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

import com.cburch.logisim.analyze.file.AnalyzerTexWriter;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Parser;
import com.cburch.logisim.analyze.model.TruthTableEvent;
import com.cburch.logisim.analyze.model.TruthTableListener;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Analyzer extends LFrame.SubWindow {
  private final AnalyzerMenuListener menuListener;

  private class MyChangeListener implements ChangeListener {
    @Override
    public void stateChanged(ChangeEvent e) {

      Object selected = tabbedPane.getSelectedComponent();
      if (selected instanceof JScrollPane) {
        selected = ((JScrollPane) selected).getViewport().getView();
      }
      if (selected instanceof JPanel) {
        ((JPanel) selected).requestFocus();
      }
      if (selected instanceof AnalyzerTab) {
        final var tab = (AnalyzerTab) selected;
        menuListener.setEditHandler(tab.getEditHandler());
        menuListener.setPrintHandler(tab.getPrintHandler());
        model.getOutputExpressions().enableUpdates();
        tab.updateTab();
      } else {
        model.getOutputExpressions().disableUpdates();
      }
    }
  }

  private class MyLocaleListener implements LocaleListener {
    @Override
    public void localeChanged() {
      Analyzer.this.setTitle(S.get("analyzerWindowTitle"));
      tabbedPane.setTitleAt(IO_TAB, S.get("inputsOutputsTab"));
      tabbedPane.setTitleAt(TABLE_TAB, S.get("tableTab"));
      tabbedPane.setTitleAt(EXPRESSION_TAB, S.get("expressionTab"));
      tabbedPane.setTitleAt(MINIMIZED_TAB, S.get("minimizedTab"));
      tabbedPane.setToolTipTextAt(IO_TAB, S.get("inputsOutputsTabTip"));
      tabbedPane.setToolTipTextAt(TABLE_TAB, S.get("tableTabTip"));
      tabbedPane.setToolTipTextAt(EXPRESSION_TAB, S.get("expressionTabTip"));
      tabbedPane.setToolTipTextAt(MINIMIZED_TAB, S.get("minimizedTabTip"));
      importTable.setText(S.get("importTableButton"));
      buildCircuit.setText(S.get("buildCircuitButton"));
      exportTable.setText(S.get("exportTableButton"));
      exportTex.setText(S.get("exportLatexButton"));
      ioPanel.localeChanged();
      truthTablePanel.localeChanged();
      expressionPanel.localeChanged();
      minimizedPanel.localeChanged();
      importTable.localeChanged();
      buildCircuit.localeChanged();
      exportTable.localeChanged();
      exportTex.localeChanged();
    }
  }

  private class TableListener implements TruthTableListener {
    @Override
    public void rowsChanged(TruthTableEvent event) {
      update();
    }

    @Override
    public void cellsChanged(TruthTableEvent event) {
      // dummy
    }

    @Override
    public void structureChanged(TruthTableEvent event) {
      update();
    }

    private void update() {
      final var tt = model.getTruthTable();
      buildCircuit.setEnabled(tt.getInputColumnCount() > 0 && tt.getOutputColumnCount() > 0);
      exportTable.setEnabled(tt.getInputColumnCount() > 0 && tt.getOutputColumnCount() > 0);
      exportTex.setEnabled(tt.getInputColumnCount() > 0 && tt.getOutputColumnCount() > 0
              && tt.getRowCount() <= AnalyzerTexWriter.MAX_TRUTH_TABLE_ROWS);
      ioPanel.updateTab();
    }
  }

  public static void main(String[] args) throws Exception {
    final var frame = new Analyzer();
    final var model = frame.getModel();

    if (args.length >= 2) {
      final var inputs = new ArrayList<Var>();
      final var outputs = new ArrayList<Var>();
      for (String s : args[0].split(",")) inputs.add(Var.parse(s));
      for (String s : args[1].split(",")) outputs.add(Var.parse(s));
      model.setVariables(inputs, outputs);
    }
    for (var i = 2; i < args.length; i++) {
      final var s = args[i];
      final var idx = s.indexOf('=');
      if (idx >= 0) {
        final var name = s.substring(0, idx);
        final var exprString = s.substring(idx + 1);
        final var expr = Parser.parse(exprString, model);
        model.getOutputExpressions().setExpression(name, expr, exprString);
      } else {
        Parser.parse(s, model); // for testing Parser.parse
      }
    }
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
  }

  private static final long serialVersionUID = 1L;
  // used by circuit analysis to select the relevant tab automatically.
  public static final int IO_TAB = 0;
  public static final int TABLE_TAB = 1;
  public static final int EXPRESSION_TAB = 2;
  public static final int MINIMIZED_TAB = 3;

  private final AnalyzerModel model = new AnalyzerModel();

  private JTabbedPane tabbedPane = new JTabbedPane();
  private final VariableTab ioPanel;
  private final TableTab truthTablePanel;
  private final ExpressionTab expressionPanel;
  private final MinimizedTab minimizedPanel;

  private final BuildCircuitButton buildCircuit;
  private final ImportTableButton importTable;
  private final ExportTableButton exportTable;
  private final ExportLatexButton exportTex;

  Analyzer() {
    super(null);
    final var tableListener = new TableListener();
    model.getTruthTable().addTruthTableListener(tableListener);
    menuListener = new AnalyzerMenuListener(menubar);
    ioPanel = new VariableTab(model.getInputs(), model.getOutputs(), menubar);
    truthTablePanel = new TableTab(model.getTruthTable());
    expressionPanel = new ExpressionTab(model, menubar);
    minimizedPanel = new MinimizedTab(model, menubar);
    importTable = new ImportTableButton(this, model);
    buildCircuit = new BuildCircuitButton(this, model);
    buildCircuit.setEnabled(false);
    exportTable = new ExportTableButton(this, model);
    exportTable.setEnabled(false);
    exportTex = new ExportLatexButton(this, model);
    exportTex.setEnabled(false);

    tabbedPane = new JTabbedPane();
    addTab(IO_TAB, ioPanel);
    addTab(TABLE_TAB, truthTablePanel);
    addTab(EXPRESSION_TAB, expressionPanel);
    addTab(MINIMIZED_TAB, minimizedPanel);

    final var contents = getContentPane();
    final var vertStrut = new JPanel(null);
    vertStrut.setPreferredSize(new Dimension(0, AppPreferences.getScaled(300)));
    final var horzStrut = new JPanel(null);
    horzStrut.setPreferredSize(new Dimension(AppPreferences.getScaled(450), 0));
    final var buttonPanel = new JPanel();
    buttonPanel.add(importTable);
    buttonPanel.add(buildCircuit);
    buttonPanel.add(exportTable);
    buttonPanel.add(exportTex);
    contents.add(vertStrut, BorderLayout.WEST);
    contents.add(horzStrut, BorderLayout.NORTH);
    contents.add(tabbedPane, BorderLayout.CENTER);
    contents.add(buttonPanel, BorderLayout.SOUTH);

    final var myLocaleListener = new MyLocaleListener();
    LocaleManager.addLocaleListener(myLocaleListener);
    myLocaleListener.localeChanged();
    final var myChangeListener = new MyChangeListener();
    tabbedPane.addChangeListener(myChangeListener);
    setSelectedTab(0);
    myChangeListener.stateChanged(null);
  }

  private void addTab(int index, final JComponent comp) {
    if (comp instanceof TableTab || comp instanceof VariableTab || comp instanceof ExpressionTab) {
      tabbedPane.insertTab(S.get("untitled"), null, comp, null, index);
      return;
    }
    final var pane = new JScrollPane(comp,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    pane.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent event) {
        final var width = pane.getViewport().getWidth();
        comp.setSize(new Dimension(width, comp.getHeight()));
      }
    });
    tabbedPane.insertTab(S.get("untitled"), null, pane, null, index);
  }

  public AnalyzerModel getModel() {
    return model;
  }

  public void setSelectedTab(int index) {
    Object found = tabbedPane.getComponentAt(index);
    if (found instanceof AnalyzerTab) {
      model.getOutputExpressions().enableUpdates();
      ((AnalyzerTab) found).updateTab();
    } else {
      model.getOutputExpressions().disableUpdates();
    }
    tabbedPane.setSelectedIndex(index);
  }

  public abstract static class PleaseWait<T> extends JDialog {
    private static final long serialVersionUID = 1L;

    private final SwingWorker<T, Void> worker;
    private final java.awt.Component parent;

    public abstract T doInBackground();

    private boolean alreadyFinished = false;

    public PleaseWait(String title, java.awt.Component parent) {
      super(null, title, ModalityType.APPLICATION_MODAL);
      this.parent = parent;
      worker =
          new SwingWorker<>() {
            @Override
            protected T doInBackground() {
              return PleaseWait.this.doInBackground();
            }

            @Override
            protected void done() {
              if (PleaseWait.this.isVisible()) {
                PleaseWait.this.dispose();
              } else {
                PleaseWait.this.alreadyFinished = true;
              }
            }
          };
    }

    public T get() {
      worker.execute();
      final var progressBar = new JProgressBar();
      progressBar.setIndeterminate(true);
      final var panel = new JPanel(new BorderLayout());
      panel.add(progressBar, BorderLayout.CENTER);
      panel.add(new JLabel(S.get("analyzePleaseWait")), BorderLayout.PAGE_START);
      add(panel);
      setPreferredSize(new Dimension(300, 70));
      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      pack();
      setLocationRelativeTo(parent);
      try {
        try {
          return worker.get(300, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignored) {
          // do nothing
        }
        if (!alreadyFinished) setVisible(true);
        return worker.get();
      } catch (Exception e) {
        return null;
      }
    }
  }
}
