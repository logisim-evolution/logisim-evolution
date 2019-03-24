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

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;
import com.cburch.logisim.analyze.model.Parser;
import com.cburch.logisim.analyze.model.ParserException;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.StringGetter;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

class ExpressionTab extends AnalyzerTab {
  private class MyListener extends AbstractAction
      implements DocumentListener, OutputExpressionsListener, ItemListener {
    private static final long serialVersionUID = 1L;
    boolean edited = false;

    public void actionPerformed(ActionEvent event) {
      Object src = event.getSource();
      if (src == clear) {
        setError(null);
        field.setText("");
        field.grabFocus();
      } else if (src == revert) {
        setError(null);
        field.setText(getCurrentString());
        field.grabFocus();
      } else if ((src == field || src == enter) && enter.isEnabled()) {
        try {
          String exprString = field.getText();
          Expression expr = Parser.parse(field.getText(), model);
          setError(null);
          model.getOutputExpressions().setExpression(getCurrentVariable(), expr, exprString);
          insertUpdate(null);
        } catch (ParserException ex) {
          setError(ex.getMessageGetter());
          field.setCaretPosition(ex.getOffset());
          field.moveCaretPosition(ex.getEndOffset());
        }
        field.grabFocus();
      }
    }

    public void changedUpdate(DocumentEvent event) {
      insertUpdate(event);
    }

    private void currentStringChanged() {
      String output = getCurrentVariable();
      String exprString = model.getOutputExpressions().getExpressionString(output);
      curExprStringLength = exprString.length();
      if (!edited) {
        setError(null);
        field.setText(getCurrentString());
      } else {
        insertUpdate(null);
      }
    }

    public void expressionChanged(OutputExpressionsEvent event) {
      if (event.getType() == OutputExpressionsEvent.OUTPUT_EXPRESSION) {
        String output = event.getVariable();
        if (output.equals(getCurrentVariable())) {
          prettyView.setExpression(model.getOutputExpressions().getExpression(output));
          currentStringChanged();
        }
      }
    }

    private String getCurrentString() {
      String output = getCurrentVariable();
      return output == null ? "" : model.getOutputExpressions().getExpressionString(output);
    }

    public void insertUpdate(DocumentEvent event) {
      String curText = field.getText();
      edited = curText.length() != curExprStringLength || !curText.equals(getCurrentString());

      boolean enable = (edited && getCurrentVariable() != null);
      clear.setEnabled(curText.length() > 0);
      revert.setEnabled(enable);
      enter.setEnabled(enable);
    }

    public void itemStateChanged(ItemEvent event) {
      updateTab();
    }

    public void removeUpdate(DocumentEvent event) {
      insertUpdate(event);
    }
  }

  private static final long serialVersionUID = 1L;

  private OutputSelector selector;
  private ExpressionView prettyView = new ExpressionView();
  private JTextArea field = new JTextArea(4, 25);
  private JButton clear = new JButton();
  private JButton revert = new JButton();
  private JButton enter = new JButton();
  private JLabel error = new JLabel();

  private MyListener myListener = new MyListener();
  private AnalyzerModel model;
  private int curExprStringLength = 0;
  private StringGetter errorMessage;

  public ExpressionTab(AnalyzerModel model) {
    this.model = model;
    selector = new OutputSelector(model);

    model.getOutputExpressions().addOutputExpressionsListener(myListener);
    selector.addItemListener(myListener);
    clear.addActionListener(myListener);
    revert.addActionListener(myListener);
    enter.addActionListener(myListener);
    field.setLineWrap(true);
    field.setWrapStyleWord(true);
    field.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), myListener);
    field.getDocument().addDocumentListener(myListener);
    field.setFont(AppPreferences.getScaledFont(new Font("Monospaced", Font.PLAIN, 14)));
    field.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) { 
        editHandler.computeEnabled();
      }
      public void focusLost(FocusEvent e) {
        if (!e.isTemporary())
          editHandler.computeEnabled();
      }
    });

    JPanel buttons = new JPanel();
    buttons.add(clear);
    buttons.add(revert);
    buttons.add(enter);

    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    setLayout(gb);
    gc.weightx = 1.0;
    gc.gridx = 0;
    gc.gridy = GridBagConstraints.RELATIVE;
    gc.fill = GridBagConstraints.BOTH;
    JPanel selectorPanel = selector.createPanel();
    gb.setConstraints(selectorPanel, gc);
    add(selectorPanel);
    gb.setConstraints(prettyView, gc);
    add(prettyView);
    Insets oldInsets = gc.insets;
    gc.insets = new Insets(10, 10, 0, 10);
    JScrollPane fieldPane =
        new JScrollPane(
            field,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    gb.setConstraints(fieldPane, gc);
    add(fieldPane);
    gc.insets = oldInsets;
    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.LINE_END;
    gb.setConstraints(buttons, gc);
    add(buttons);
    gc.fill = GridBagConstraints.BOTH;
    gb.setConstraints(error, gc);
    add(error);

    myListener.insertUpdate(null);
    setError(null);
  }

  String getCurrentVariable() {
    return selector.getSelectedOutput();
  }

  @Override
  void localeChanged() {
    selector.localeChanged();
    prettyView.localeChanged();
    clear.setText(S.get("exprClearButton"));
    revert.setText(S.get("exprRevertButton"));
    enter.setText(S.get("exprEnterButton"));
    if (errorMessage != null) {
      error.setText(errorMessage.toString());
    }
  }

  void registerDefaultButtons(DefaultRegistry registry) {
    registry.registerDefaultButton(field, enter);
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
    final String output = getCurrentVariable();
    if (model.getTruthTable().getRowCount() > 4096) {
      (new Analyzer.PleaseWait<Expression>(S.get("expressionCalc"), this) {
            @Override
            public Expression doInBackground() throws Exception {
              return model.getOutputExpressions().getExpression(output);
            }
          })
          .get();
    }
    prettyView.setExpression(model.getOutputExpressions().getExpression(output));
    myListener.currentStringChanged();
  }
  
  @Override
  EditHandler getEditHandler() {
    return editHandler;
  }

  EditHandler editHandler = new EditHandler() {
    public void computeEnabled() {
      boolean canEdit = field.hasFocus();
      setEnabled(LogisimMenuBar.CUT, canEdit);
      setEnabled(LogisimMenuBar.COPY, canEdit);
      setEnabled(LogisimMenuBar.PASTE, canEdit);
      setEnabled(LogisimMenuBar.DELETE, canEdit);
      setEnabled(LogisimMenuBar.DUPLICATE, false);
      setEnabled(LogisimMenuBar.SELECT_ALL, canEdit);
      setEnabled(LogisimMenuBar.RAISE, false);
      setEnabled(LogisimMenuBar.LOWER, false);
      setEnabled(LogisimMenuBar.RAISE_TOP, false);
      setEnabled(LogisimMenuBar.LOWER_BOTTOM, false);
      setEnabled(LogisimMenuBar.ADD_CONTROL, false);
      setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
    }

    public void cut() {
      field.requestFocus();
      field.cut();
    }
    public void copy() { 
      field.requestFocus();
      field.copy();
    }
    public void paste() {
      field.requestFocus();
      field.paste();
    }
    public void delete() {
      field.requestFocus();
      field.replaceSelection("");
    }
    public void duplicate() {
    }
    public void selectAll() {
      field.requestFocus();
      field.selectAll();
    }

    public void raise() { }
    public void lower() { }
    public void raiseTop() { }
    public void lowerBottom() { }

    public void addControlPoint() { }
    public void removeControlPoint() { }
  };

}
