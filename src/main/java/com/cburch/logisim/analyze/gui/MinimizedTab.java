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

import com.cburch.logisim.analyze.data.ExpressionRenderData;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression.Notation;
import com.cburch.logisim.analyze.model.OutputExpressions;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
import com.cburch.logisim.gui.menu.PrintHandler;
import com.cburch.logisim.prefs.AppPreferences;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.IOException;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

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
  
  @SuppressWarnings("rawtypes")
  public static class NotationModel extends AbstractListModel implements ComboBoxModel {
    private static final long serialVersionUID = 1L;
    
    private String[] choices;
    private int selected;

    public NotationModel() {
    selected = 0;
    choices = new String[5];
    localeChanged();
    }
    
    public int getSize() {
    return choices.length;
    }
    
    public Object getElementAt(int index) {
    return choices[index];
    }
    
    public void localeChanged() {
    choices[Notation.LOGIC.Id] = S.get("expressionLogicrepresentation");
    choices[Notation.MATHEMATICAL.Id] = S.get("expressionMathrepresentation");
    choices[Notation.ALTLOGIC.Id] = S.get("expressionAltLogicrepresentation");
    choices[Notation.PROGBITS.Id] = S.get("expressionProgbitsrepresentation");
    choices[Notation.PROGBOOLS.Id] = S.get("expressionProgboolsrepresentation");
    fireContentsChanged(this, 0, choices.length);
    }
    
    public void setSelectedItem(Object anItem) {
      for (int i = 0; i < choices.length; i++) {
        if (choices[i].equals(anItem)) {
          selected = i;
        }
      }
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
      } else if (event.getSource() == notationChoice) {
        Notation notation = Notation.values()[notationChoice.getSelectedIndex()];
        minimizedExpr.setNotation(notation);
        karnaughMap.setNotation(notation);
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
  private JLabel notationLabel = new JLabel();

  @SuppressWarnings({"rawtypes", "unchecked"})
  private JComboBox formatChoice = new JComboBox<>(new FormatModel());

  @SuppressWarnings({"rawtypes", "unchecked"})
  private JComboBox formatStyle = new JComboBox<>(new StyleModel());
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private JComboBox notationChoice = new JComboBox<>(new NotationModel());

  private ExpressionView minimizedExpr = new ExpressionView();
  private JButton setAsExpr = new JButton();

  private MyListener myListener = new MyListener();
  private AnalyzerModel model;
  private OutputExpressions outputExprs;

  public MinimizedTab(AnalyzerModel model, LogisimMenuBar menubar) {
    this.model = model;
    this.outputExprs = model.getOutputExpressions();
    outputExprs.addOutputExpressionsListener(myListener);

    selector = new OutputSelector(model);
    selector.addItemListener(myListener);
    karnaughMap = new KarnaughMapPanel(model,minimizedExpr);
    setAsExpr.addActionListener(myListener);
    formatChoice.addItemListener(myListener);
    formatStyle.addItemListener(myListener);
    notationChoice.addItemListener(myListener);

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
    TransferHandler ccpTab, ccpKmap, ccpExpr;
    setTransferHandler(ccpTab = new MinimizedTransferHandler());
    karnaughMap.setTransferHandler(ccpKmap = new KmapTransferHandler());
    minimizedExpr.setTransferHandler(ccpExpr = new ExpressionTransferHandler());

    InputMap inputMap1 = getInputMap();
    InputMap inputMap2 = karnaughMap.getInputMap();
    InputMap inputMap3 = minimizedExpr.getInputMap();
    for (LogisimMenuItem item: LogisimMenuBar.EDIT_ITEMS) {
      KeyStroke accel = menubar.getAccelerator(item);
      inputMap1.put(accel, item);
      inputMap2.put(accel, item);
      inputMap3.put(accel, item);
    }

    getActionMap().put(LogisimMenuBar.COPY, ccpTab.getCopyAction());
    karnaughMap.getActionMap().put(LogisimMenuBar.COPY, ccpKmap.getCopyAction());
    minimizedExpr.getActionMap().put(LogisimMenuBar.COPY, ccpExpr.getCopyAction());

    MouseMotionAdapter m = new MouseMotionAdapter() {
      public void mouseDragged(MouseEvent e) {
        JComponent c = (JComponent)e.getSource();
        TransferHandler handler = c.getTransferHandler();
        handler.exportAsDrag(c, e, TransferHandler.COPY);
      }
    };
    karnaughMap.addMouseMotionListener(m);
    minimizedExpr.addMouseMotionListener(m);

    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        requestFocusInWindow();
      }
    });

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
    minimizedExpr.addFocusListener(f);
    karnaughMap.addFocusListener(f);
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
    gc.gridy++;
    gb.setConstraints(notationLabel, gc);
    control.add(notationLabel);
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
    gc.gridy++;
    gb.setConstraints(notationChoice, gc);
    control.add(notationChoice);
    return control;
  }

  private String getCurrentVariable() {
    return selector.getSelectedOutput();
  }

  @Override
  void localeChanged() {
    selector.localeChanged();
    karnaughMap.localeChanged();
    setAsExpr.setText(S.get("minimizedSetButton"));
    formatLabel.setText(S.get("minimizedFormat"));
    styleLabel.setText(S.get("KmapStyle"));
    notationLabel.setText(S.get("ExpressionNotation"));
    ((FormatModel) formatChoice.getModel()).localeChanged();
    ((StyleModel) formatStyle.getModel()).localeChanged();
    ((NotationModel) notationChoice.getModel()).localeChanged();
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
  
  @Override
  EditHandler getEditHandler() {
    return editHandler;
  }

  EditHandler editHandler = new EditHandler() {
    @Override
    public void computeEnabled() {
      boolean viewing = minimizedExpr.isFocusOwner()
          || karnaughMap.isFocusOwner();
      setEnabled(LogisimMenuBar.CUT, false);
      setEnabled(LogisimMenuBar.COPY, viewing);
      setEnabled(LogisimMenuBar.PASTE, false);
      setEnabled(LogisimMenuBar.DELETE, false);
      setEnabled(LogisimMenuBar.DUPLICATE, false);
      setEnabled(LogisimMenuBar.SELECT_ALL, false);
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
      if (minimizedExpr.isSelected())
        minimizedExpr.getActionMap().get(action).actionPerformed(e);
      else if (karnaughMap.isSelected())
        karnaughMap.getActionMap().get(action).actionPerformed(e);
    }
  };

  private class MinimizedTransferHandler extends TransferHandler {
    @Override
    protected Transferable createTransferable(JComponent c) {
      if (minimizedExpr.isFocusOwner()) {
        return new KmapSelection(karnaughMap);
      } else if (karnaughMap.isFocusOwner()) {
        return new ExpressionSelection(minimizedExpr.getRenderData());
      } else {
        return null;
      }
    }
    @Override
    public int getSourceActions(JComponent c) { return COPY; }
    @Override
    public boolean importData(TransferHandler.TransferSupport info) { return false; }
    @Override
    protected void exportDone(JComponent c, Transferable tdata, int action) { }
    @Override
    public boolean canImport(TransferHandler.TransferSupport support) { return false; }
  }
  
  private class KmapTransferHandler extends MinimizedTransferHandler {
    @Override
    protected Transferable createTransferable(JComponent c) {
      return new KmapSelection(karnaughMap);
    }
  }
  
  private class ExpressionTransferHandler extends MinimizedTransferHandler {
    @Override
    protected Transferable createTransferable(JComponent c) {
      return new ExpressionSelection(minimizedExpr.getRenderData());
    }
  }
  
  static class ImageSelection implements Transferable {
    private Image image;

    public ImageSelection() { }

    public void setImage(Image image) {
      this.image = image;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[] { DataFlavor.imageFlavor };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return DataFlavor.imageFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
      if (!DataFlavor.imageFlavor.equals(flavor)) {
        throw new UnsupportedFlavorException(flavor);
      }
      return image;
    }
  }

  static class KmapSelection extends ImageSelection {
    public KmapSelection(KarnaughMapPanel kmap) {
      int w = kmap.getKMapDim().width;
      int h = kmap.getKMapDim().height;
      BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = img.createGraphics();
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, w, h);
      g.setColor(Color.BLACK);
      kmap.paintKmap(g,false);
      g.dispose();
      setImage(img);
    }
  }

  static class ExpressionSelection extends ImageSelection {
    public ExpressionSelection(ExpressionRenderData prettyView) {
      if (prettyView == null)
        return;
      Dimension dim = prettyView.getPreferredSize();
      int w = dim.width;
      int h = dim.height;
      BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = img.createGraphics();
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, w, h);
      g.setColor(Color.BLACK);
      prettyView.paint(g,0,0);
      g.dispose();
      setImage(img);
    }
  }
  

  @Override
  PrintHandler getPrintHandler() {
    return printHandler;
  }

  PrintHandler printHandler = new PrintHandler() {
    @Override
    public Dimension getExportImageSize() {
      int kWidth = karnaughMap.getKMapDim().width;
      int kHeight = karnaughMap.getKMapDim().height;
      int eWidth = minimizedExpr.getRenderData().getPreferredSize().width;
      int eHeight = minimizedExpr.getRenderData().getPreferredSize().height;
      int width = Math.max(kWidth, eWidth);
      int height = kHeight + 30 + eHeight;
      return new Dimension(width, height);
    }

    @Override
    public void paintExportImage(BufferedImage img, Graphics2D g) {
      int width = img.getWidth();
      int height = img.getHeight();
      g.setClip(0, 0, width, height);

      AffineTransform xform = g.getTransform();
      g.translate((width - karnaughMap.getWidth())/2, 0);
      g.setColor(Color.BLACK);
      karnaughMap.paintKmap(g,false);
      g.setTransform(xform);

      ExpressionRenderData prettyView = minimizedExpr.getRenderData();
      g.translate((width - prettyView.getWidth())/2, karnaughMap.getKMapDim().height + 30);
      g.setColor(Color.BLACK);
      prettyView.paint(g,0,0);
    }

    @Override
    public int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h) {
      if (pageNum != 0)
        return Printable.NO_SUCH_PAGE;

      AffineTransform xform = g.getTransform();
      g.translate((w - karnaughMap.getWidth())/2, 0);
      g.setColor(Color.BLACK);
      karnaughMap.paintKmap(g,false);
      g.setTransform(xform);

      ExpressionRenderData prettyView = minimizedExpr.getRenderData();
      g.translate((w - prettyView.getWidth())/2, karnaughMap.getKMapDim().height + 30);
      g.setColor(Color.BLACK);
      prettyView.paint(g,0,0);
      return Printable.PAGE_EXISTS;
    }
  };
}
