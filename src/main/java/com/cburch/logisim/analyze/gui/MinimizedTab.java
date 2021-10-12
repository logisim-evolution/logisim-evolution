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
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
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
      if (choice == AnalyzerModel.FORMAT_PRODUCT_OF_SUMS) {
        return 1;
      }
      return 0;
    }

    private static final long serialVersionUID = 1L;

    private final String[] choices;
    private int selected;

    private FormatModel() {
      selected = 0;
      choices = new String[2];
      localeChanged();
    }

    @Override
    public Object getElementAt(int index) {
      return choices[index];
    }

    int getSelectedFormat() {
      if (selected == 1) {
        return AnalyzerModel.FORMAT_PRODUCT_OF_SUMS;
      }
      return AnalyzerModel.FORMAT_SUM_OF_PRODUCTS;
    }

    @Override
    public Object getSelectedItem() {
      return choices[selected];
    }

    @Override
    public int getSize() {
      return choices.length;
    }

    void localeChanged() {
      choices[0] = S.get("minimizedSumOfProducts");
      choices[1] = S.get("minimizedProductOfSums");
      fireContentsChanged(this, 0, choices.length);
    }

    @Override
    public void setSelectedItem(Object value) {
      for (var i = 0; i < choices.length; i++) {
        if (choices[i].equals(value)) {
          selected = i;
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private static class StyleModel extends AbstractListModel implements ComboBoxModel {
    private static final long serialVersionUID = 1L;

    private final String[] choices;
    private int selected;

    private StyleModel() {
      selected = AppPreferences.KMAP_LINED_STYLE.get() ? 1 : 0;
      choices = new String[2];
      localeChanged();
    }

    @Override
    public int getSize() {
      return choices.length;
    }

    @Override
    public Object getElementAt(int index) {
      return choices[index];
    }

    void localeChanged() {
      choices[0] = S.get("KmapNumberedStyle");
      choices[1] = S.get("KMapLinedStyle");
      fireContentsChanged(this, 0, choices.length);
    }

    @Override
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

    private final String[] choices;
    private int selected;

    public NotationModel() {
      selected = 0;
      choices = new String[5];
      localeChanged();
    }

    @Override
    public int getSize() {
      return choices.length;
    }

    @Override
    public Object getElementAt(int index) {
      return choices[index];
    }

    public void localeChanged() {
      choices[Notation.LOGIC.id] = S.get("expressionLogicrepresentation");
      choices[Notation.MATHEMATICAL.id] = S.get("expressionMathrepresentation");
      choices[Notation.ALTLOGIC.id] = S.get("expressionAltLogicrepresentation");
      choices[Notation.PROGBITS.id] = S.get("expressionProgbitsrepresentation");
      choices[Notation.PROGBOOLS.id] = S.get("expressionProgboolsrepresentation");
      fireContentsChanged(this, 0, choices.length);
    }

    @Override
    public void setSelectedItem(Object anItem) {
      for (var i = 0; i < choices.length; i++) {
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
    @Override
    public void actionPerformed(ActionEvent event) {
      final var output = getCurrentVariable();
      final var format = outputExprs.getMinimizedFormat(output);
      formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
      outputExprs.setExpression(output, outputExprs.getMinimalExpression(output));
    }

    @Override
    public void expressionChanged(OutputExpressionsEvent event) {
      final var output = getCurrentVariable();
      if (event.getType() == OutputExpressionsEvent.OUTPUT_MINIMAL
          && event.getVariable().equals(output)) {
        minimizedExpr.setExpression(outputExprs.getMinimalExpression(output));
        MinimizedTab.this.validate();
      }
      setAsExpr.setEnabled(output != null && !outputExprs.isExpressionMinimal(output));
      int format = outputExprs.getMinimizedFormat(output);
      formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
    }

    @Override
    public void itemStateChanged(ItemEvent event) {
      if (event.getSource() == formatChoice) {
        final var output = getCurrentVariable();
        final var model = (FormatModel) formatChoice.getModel();
        outputExprs.setMinimizedFormat(output, model.getSelectedFormat());
        karnaughMap.setFormat(model.getSelectedFormat());
      } else if (event.getSource() == formatStyle) {
        final var model = (StyleModel) formatStyle.getModel();
        model.setStyle(karnaughMap);
      } else if (event.getSource() == notationChoice) {
        final var notation = Notation.values()[notationChoice.getSelectedIndex()];
        minimizedExpr.setNotation(notation);
        karnaughMap.setNotation(notation);
      } else {
        updateTab();
      }
    }
  }

  private static final long serialVersionUID = 1L;

  private final OutputSelector selector;
  private final KarnaughMapPanel karnaughMap;
  private final JLabel formatLabel = new JLabel();
  private final JLabel styleLabel = new JLabel();
  private final JLabel notationLabel = new JLabel();

  @SuppressWarnings({"rawtypes", "unchecked"})
  private final JComboBox formatChoice = new JComboBox<>(new FormatModel());

  @SuppressWarnings({"rawtypes", "unchecked"})
  private final JComboBox formatStyle = new JComboBox<>(new StyleModel());

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private final JComboBox notationChoice = new JComboBox<>(new NotationModel());

  private final ExpressionView minimizedExpr = new ExpressionView();
  private final JButton setAsExpr = new JButton();

  private final MyListener myListener = new MyListener();
  private final AnalyzerModel model;
  private final OutputExpressions outputExprs;

  public MinimizedTab(AnalyzerModel model, LogisimMenuBar menubar) {
    this.model = model;
    this.outputExprs = model.getOutputExpressions();
    outputExprs.addOutputExpressionsListener(myListener);

    selector = new OutputSelector(model);
    selector.addItemListener(myListener);
    karnaughMap = new KarnaughMapPanel(model, minimizedExpr);
    setAsExpr.addActionListener(myListener);
    formatChoice.addItemListener(myListener);
    formatStyle.addItemListener(myListener);
    notationChoice.addItemListener(myListener);

    final var buttons = new JPanel(new GridLayout(1, 1));
    buttons.add(setAsExpr);

    final var gb = new GridBagLayout();
    final var gc = new GridBagConstraints();
    setLayout(gb);
    gc.weightx = 1.0;
    gc.gridwidth = 1;
    gc.gridy = GridBagConstraints.RELATIVE;
    gc.gridx = 0;
    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.CENTER;
    final var cntrl = control();
    gb.setConstraints(cntrl, gc);
    add(cntrl);
    gb.setConstraints(karnaughMap, gc);
    add(karnaughMap);
    final var oldInsets = gc.insets;
    gc.insets = new Insets(20, 0, 20, 0);
    gc.fill = GridBagConstraints.BOTH;
    gb.setConstraints(minimizedExpr, gc);
    add(minimizedExpr);
    gc.insets = oldInsets;
    gc.fill = GridBagConstraints.NONE;
    gb.setConstraints(buttons, gc);
    add(buttons);

    final var selected = selector.getSelectedOutput();
    setAsExpr.setEnabled(selected != null && !outputExprs.isExpressionMinimal(selected));
    setTransferHandler(new MinimizedTransferHandler());
    karnaughMap.setTransferHandler(new KmapTransferHandler());
    minimizedExpr.setTransferHandler(new ExpressionTransferHandler());

    final var inputMap1 = getInputMap();
    final var inputMap2 = karnaughMap.getInputMap();
    final var inputMap3 = minimizedExpr.getInputMap();
    for (LogisimMenuItem item : LogisimMenuBar.EDIT_ITEMS) {
      KeyStroke accel = menubar.getAccelerator(item);
      inputMap1.put(accel, item);
      inputMap2.put(accel, item);
      inputMap3.put(accel, item);
    }

    getActionMap().put(LogisimMenuBar.COPY, TransferHandler.getCopyAction());
    karnaughMap.getActionMap().put(LogisimMenuBar.COPY, TransferHandler.getCopyAction());
    minimizedExpr.getActionMap().put(LogisimMenuBar.COPY, TransferHandler.getCopyAction());

    MouseMotionAdapter m =
        new MouseMotionAdapter() {
          @Override
          public void mouseDragged(MouseEvent e) {
            JComponent c = (JComponent) e.getSource();
            TransferHandler handler = c.getTransferHandler();
            handler.exportAsDrag(c, e, TransferHandler.COPY);
          }
        };
    karnaughMap.addMouseMotionListener(m);
    minimizedExpr.addMouseMotionListener(m);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        requestFocusInWindow();
      }
    });

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
    minimizedExpr.addFocusListener(f);
    karnaughMap.addFocusListener(f);
  }

  private JPanel control() {
    final var control = new JPanel();
    final var gb = new GridBagLayout();
    final var gc = new GridBagConstraints();
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
    final var output = getCurrentVariable();
    if (model.getTruthTable().getRowCount() > 4096) {
      (new Analyzer.PleaseWait<Void>(S.get("expressionCalc"), this) {
            @Override
            public Void doInBackground() {
              model.getOutputExpressions().getExpression(output);
              return null;
            }
          })
          .get();
    }
    karnaughMap.setOutput(output);
    final var format = outputExprs.getMinimizedFormat(output);
    formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
    minimizedExpr.setExpression(outputExprs.getMinimalExpression(output));
    setAsExpr.setEnabled(output != null && !outputExprs.isExpressionMinimal(output));
  }

  @Override
  EditHandler getEditHandler() {
    return editHandler;
  }

  final EditHandler editHandler =
      new EditHandler() {
        @Override
        public void computeEnabled() {
          boolean viewing = minimizedExpr.isFocusOwner() || karnaughMap.isFocusOwner();
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
          final var action = e.getSource();
          if (minimizedExpr.isSelected())
            minimizedExpr.getActionMap().get(action).actionPerformed(e);
          else if (karnaughMap.isSelected())
            karnaughMap.getActionMap().get(action).actionPerformed(e);
        }
      };

  private class MinimizedTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;

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
    public int getSourceActions(JComponent c) {
      return COPY;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
      return false;
    }

    @Override
    protected void exportDone(JComponent c, Transferable tdata, int action) {
      // dummy
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      return false;
    }
  }

  private class KmapTransferHandler extends MinimizedTransferHandler {
    private static final long serialVersionUID = 1L;

    @Override
    protected Transferable createTransferable(JComponent c) {
      return new KmapSelection(karnaughMap);
    }
  }

  private class ExpressionTransferHandler extends MinimizedTransferHandler {
    private static final long serialVersionUID = 1L;

    @Override
    protected Transferable createTransferable(JComponent c) {
      return new ExpressionSelection(minimizedExpr.getRenderData());
    }
  }

  static class ImageSelection implements Transferable {
    private Image image;

    public ImageSelection() {
      // dummy
    }

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
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (!DataFlavor.imageFlavor.equals(flavor)) {
        throw new UnsupportedFlavorException(flavor);
      }
      return image;
    }
  }

  static class KmapSelection extends ImageSelection {
    public KmapSelection(KarnaughMapPanel kmap) {
      final var w = kmap.getKMapDim().width;
      final var h = kmap.getKMapDim().height;
      final var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      final var g = img.createGraphics();
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, w, h);
      g.setColor(Color.BLACK);
      kmap.paintKmap(g, false);
      g.dispose();
      setImage(img);
    }
  }

  static class ExpressionSelection extends ImageSelection {
    public ExpressionSelection(ExpressionRenderData prettyView) {
      if (prettyView == null) return;
      final var dim = prettyView.getPreferredSize();
      final var w = dim.width;
      final var h = dim.height;
      final var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      final var g = img.createGraphics();
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, w, h);
      g.setColor(Color.BLACK);
      prettyView.paint(g, 0, 0);
      g.dispose();
      setImage(img);
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
          final var kWidth = karnaughMap.getKMapDim().width;
          final var kHeight = karnaughMap.getKMapDim().height;
          final var eWidth = minimizedExpr.getRenderData().getPreferredSize().width;
          final var eHeight = minimizedExpr.getRenderData().getPreferredSize().height;

          final var width = Math.max(kWidth, eWidth);
          final var height = kHeight + 30 + eHeight;

          return new Dimension(width, height);
        }

        @Override
        public void paintExportImage(BufferedImage img, Graphics2D g) {
          final var width = img.getWidth();
          final var height = img.getHeight();
          g.setClip(0, 0, width, height);

          doPrint(g, width);
        }

        @Override
        public int print(Graphics2D g, PageFormat pf, int pageNum, double width, double height) {
          if (pageNum != 0) return Printable.NO_SUCH_PAGE;
          return doPrint(g, width);
        }

        private int doPrint(Graphics2D g, double width) {
          final var xform = g.getTransform();
          g.translate((width - karnaughMap.getWidth()) / 2, 0);
          g.setColor(Color.BLACK);
          karnaughMap.paintKmap(g, false);
          g.setTransform(xform);

          final var prettyView = minimizedExpr.getRenderData();
          g.translate((width - prettyView.getWidth()) / 2, karnaughMap.getKMapDim().height + 30);
          g.setColor(Color.BLACK);
          prettyView.paint(g, 0, 0);
          return Printable.PAGE_EXISTS;
        }
      };
}
