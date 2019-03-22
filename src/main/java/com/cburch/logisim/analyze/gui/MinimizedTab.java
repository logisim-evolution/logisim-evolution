/**
 * ***************************************************************************** This file is part
 * of logisim-evolution.
 *
 * <p>logisim-evolution is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>logisim-evolution is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with
 * logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Original code by Carl Burch (http://www.cburch.com), 2011. Subsequent modifications by: +
 * College of the Holy Cross http://www.holycross.edu + Haute École Spécialisée Bernoise/Berner
 * Fachhochschule http://www.bfh.ch + Haute École du paysage, d'ingénierie et d'architecture de
 * Genève http://hepia.hesge.ch/ + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 * http://www.heig-vd.ch/
 * *****************************************************************************
 */
package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.OutputExpressions;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class MinimizedTab extends AnalyzerTab {
  @SuppressWarnings("rawtypes")
  private static class FormatModel extends AbstractListModel implements ComboBoxModel {
    static int getFormatIndex(int choice) {
      switch (choice) {
        case AnalyzerModel.FORMAT_PRODUCT_OF_SUMS:
          return 1;
        default:
          return 0;
      }
    }

    private static final long serialVersionUID = 1L;

    private String[] choices;
    private int selected;

    private FormatModel() {
      selected = 0;
      choices = new String[2];
      localeChanged();
    }

    public Object getElementAt(int index) {
      return choices[index];
    }

    int getSelectedFormat() {
      switch (selected) {
        case 1:
          return AnalyzerModel.FORMAT_PRODUCT_OF_SUMS;
        default:
          return AnalyzerModel.FORMAT_SUM_OF_PRODUCTS;
      }
    }

    public Object getSelectedItem() {
      return choices[selected];
    }

    public int getSize() {
      return choices.length;
    }

    void localeChanged() {
      choices[0] = S.get("minimizedSumOfProducts");
      choices[1] = S.get("minimizedProductOfSums");
      fireContentsChanged(this, 0, choices.length);
    }

    public void setSelectedItem(Object value) {
      for (int i = 0; i < choices.length; i++) {
        if (choices[i].equals(value)) {
          selected = i;
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private static class StyleModel extends AbstractListModel implements ComboBoxModel {
    private static final long serialVersionUID = 1L;

    private String[] choices;
    private int selected;

    private StyleModel() {
      selected = AppPreferences.KMAP_LINED_STYLE.get() ? 1 : 0;
      choices = new String[2];
      localeChanged();
    }

    public int getSize() {
      return choices.length;
    }

    public Object getElementAt(int index) {
      return choices[index];
    }

    void localeChanged() {
      choices[0] = S.get("KmapNumberedStyle");
      choices[1] = S.get("KMapLinedStyle");
      fireContentsChanged(this, 0, choices.length);
    }

    public void setSelectedItem(Object anItem) {
      for (int i = 0; i < choices.length; i++) {
        if (choices[i].equals(anItem)) {
          selected = i;
        }
      }
    }

    public void setStyle(KarnaughMapPanel karnaughMap) {
      if (selected == 0) karnaughMap.setStyleNumbered();
      else karnaughMap.setStyleLined();
    }

    @Override
    public Object getSelectedItem() {
      return choices[selected];
    }
  }

  private class MyListener implements OutputExpressionsListener, ActionListener, ItemListener {
    public void actionPerformed(ActionEvent event) {
      String output = getCurrentVariable();
      int format = outputExprs.getMinimizedFormat(output);
      formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
      outputExprs.setExpression(output, outputExprs.getMinimalExpression(output));
    }

    public void expressionChanged(OutputExpressionsEvent event) {
      String output = getCurrentVariable();
      if (event.getType() == OutputExpressionsEvent.OUTPUT_MINIMAL
          && event.getVariable().equals(output)) {
        minimizedExpr.setExpression(outputExprs.getMinimalExpression(output));
        MinimizedTab.this.validate();
      }
      setAsExpr.setEnabled(output != null && !outputExprs.isExpressionMinimal(output));
      int format = outputExprs.getMinimizedFormat(output);
      formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
    }

    public void itemStateChanged(ItemEvent event) {
      if (event.getSource() == formatChoice) {
        String output = getCurrentVariable();
        FormatModel model = (FormatModel) formatChoice.getModel();
        outputExprs.setMinimizedFormat(output, model.getSelectedFormat());
        karnaughMap.setFormat(model.getSelectedFormat());
      } else if (event.getSource() == formatStyle) {
        StyleModel model = (StyleModel) formatStyle.getModel();
        model.setStyle(karnaughMap);
      } else {
        updateTab();
      }
    }
  }

  private static final long serialVersionUID = 1L;

  private OutputSelector selector;
  private KarnaughMapPanel karnaughMap;
  private JLabel formatLabel = new JLabel();
  private JLabel styleLabel = new JLabel();

  @SuppressWarnings({"rawtypes", "unchecked"})
  private JComboBox formatChoice = new JComboBox<>(new FormatModel());

  @SuppressWarnings({"rawtypes", "unchecked"})
  private JComboBox formatStyle = new JComboBox<>(new StyleModel());

  private ExpressionView minimizedExpr = new ExpressionView();
  private JButton setAsExpr = new JButton();

  private MyListener myListener = new MyListener();
  private AnalyzerModel model;
  private OutputExpressions outputExprs;

  public MinimizedTab(AnalyzerModel model) {
    this.model = model;
    this.outputExprs = model.getOutputExpressions();
    outputExprs.addOutputExpressionsListener(myListener);

    selector = new OutputSelector(model);
    selector.addItemListener(myListener);
    karnaughMap = new KarnaughMapPanel(model);
    karnaughMap.addMouseListener(new TruthTableMouseListener());
    setAsExpr.addActionListener(myListener);
    formatChoice.addItemListener(myListener);
    formatStyle.addItemListener(myListener);

    JPanel buttons = new JPanel(new GridLayout(1, 1));
    buttons.add(setAsExpr);

    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    setLayout(gb);
    gc.weightx = 1.0;
    gc.gridwidth = 1;
    gc.gridy = GridBagConstraints.RELATIVE;
    gc.gridx = 0;
    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.CENTER;
    JPanel cntrl = control();
    gb.setConstraints(cntrl, gc);
    add(cntrl);
    gb.setConstraints(karnaughMap, gc);
    add(karnaughMap);
    Insets oldInsets = gc.insets;
    gc.insets = new Insets(20, 0, 20, 0);
    gc.fill = GridBagConstraints.BOTH;
    gb.setConstraints(minimizedExpr, gc);
    add(minimizedExpr);
    gc.insets = oldInsets;
    gc.fill = GridBagConstraints.NONE;
    gb.setConstraints(buttons, gc);
    add(buttons);

    String selected = selector.getSelectedOutput();
    setAsExpr.setEnabled(selected != null && !outputExprs.isExpressionMinimal(selected));
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
    gb.setConstraints(selector.getLabel(), gc);
    control.add(selector.getLabel());
    gc.gridy++;
    gb.setConstraints(formatLabel, gc);
    control.add(formatLabel);
    gc.gridy++;
    gb.setConstraints(styleLabel, gc);
    control.add(styleLabel);
    gc.gridx = 1;
    gc.gridy = 0;
    gc.anchor = GridBagConstraints.WEST;
    gb.setConstraints(selector.getComboBox(), gc);
    control.add(selector.getComboBox());
    gc.gridy++;
    gb.setConstraints(formatChoice, gc);
    control.add(formatChoice);
    gc.gridy++;
    gb.setConstraints(formatStyle, gc);
    control.add(formatStyle);
    return control;
  }

  private String getCurrentVariable() {
    return selector.getSelectedOutput();
  }

  @Override
  void localeChanged() {
    selector.localeChanged();
    karnaughMap.localeChanged();
    minimizedExpr.localeChanged();
    setAsExpr.setText(S.get("minimizedSetButton"));
    formatLabel.setText(S.get("minimizedFormat"));
    styleLabel.setText(S.get("KmapStyle"));
    ((FormatModel) formatChoice.getModel()).localeChanged();
    ((StyleModel) formatStyle.getModel()).localeChanged();
  }

  @SuppressWarnings("serial")
  @Override
  void updateTab() {
    final String output = getCurrentVariable();
    if (model.getTruthTable().getRowCount() > 4096) {
      (new Analyzer.PleaseWait<Void>(S.get("expressionCalc"), this) {
            @Override
            public Void doInBackground() throws Exception {
              model.getOutputExpressions().getExpression(output);
              return null;
            }
          })
          .get();
    }
    karnaughMap.setOutput(output);
    int format = outputExprs.getMinimizedFormat(output);
    formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
    minimizedExpr.setExpression(outputExprs.getMinimalExpression(output));
    setAsExpr.setEnabled(output != null && !outputExprs.isExpressionMinimal(output));
  }
}
