/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.chrono;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.gui.log.LogPanel;
import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.log.Signal;
import com.cburch.logisim.gui.log.SignalInfo;
import com.cburch.logisim.gui.main.SimulationToolbarModel;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.PrintHandler;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

public class ChronoPanel extends LogPanel implements Model.Listener {
  private static final long serialVersionUID = 1L;
  public static final int HEADER_HEIGHT = 20;
  public static final int SIGNAL_HEIGHT = 30;
  public static final int GAP = 2;
  public static final int INITIAL_SPLIT = 150;
  private Model model;
  private RightPanel rightPanel;
  private LeftPanel leftPanel;
  private JScrollPane leftScroll;
  private JScrollPane rightScroll;
  private JSplitPane splitPane;
  private JButton selButton;

  // listeners

  public ChronoPanel(LogFrame logFrame) {
    super(logFrame);
    selectBg = UIManager.getDefaults().getColor("List.selectionBackground");
    selectHi = darker(selectBg);
    selectColors =
        new Color[] {
          selectBg, selectHi, SELECT_LINE, SELECT_ERR, SELECT_ERRLINE, SELECT_UNK, SELECT_UNKLINE
        };
    setModel(logFrame.getModel());
    configure();
    resplit();
    editHandler.computeEnabled();
  }

  private void configure() {
    setLayout(new BorderLayout());
    final var logFrame = getLogFrame();
    final var simTools = new SimulationToolbarModel(getProject(), logFrame.getMenuListener());
    final var toolbar = new Toolbar(simTools);
    final var toolpanel = new JPanel();
    final var gbl = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    toolpanel.setLayout(gbl);
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = gbc.weighty = 0.0;
    gbc.gridx = gbc.gridy = 0;
    gbl.setConstraints(toolbar, gbc);
    toolpanel.add(toolbar);

    selButton = logFrame.makeSelectionButton();
    selButton.setFont(selButton.getFont().deriveFont(10.0f));
    Insets insets = gbc.insets;
    gbc.insets = new Insets(2, 0, 2, 0);
    gbc.gridx = 1;
    gbl.setConstraints(selButton, gbc);
    toolpanel.add(selButton);
    gbc.insets = insets;

    final var filler = Box.createHorizontalGlue();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx = 2;
    gbl.setConstraints(filler, gbc);
    toolpanel.add(filler);
    add(toolpanel, BorderLayout.NORTH);

    // panels
    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setDividerSize(5);
    splitPane.setResizeWeight(0.0);
    add(BorderLayout.CENTER, splitPane);
    final var inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final var actionMap = getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ClearSelection");
    actionMap.put(
        "ClearSelection",
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            System.out.println("chrono clear");
            leftPanel.clearSelection();
          }
        });
  }

  private void resplit() {
    leftPanel = new LeftPanel(this);
    leftScroll =
        new JScrollPane(
            leftPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

    final var p = rightScroll == null ? 0 : rightScroll.getHorizontalScrollBar().getValue();
    if (rightPanel == null) rightPanel = new RightPanel(this, leftPanel.getSelectionModel());
    rightScroll =
        new JScrollPane(
            rightPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

    // Synchronize the two scrollbars
    leftScroll.getVerticalScrollBar().setUI(null);
    leftScroll.getVerticalScrollBar().setModel(rightScroll.getVerticalScrollBar().getModel());

    // zoom on control+scrollwheel
    final var zoomer =
        new MouseAdapter() {
          @Override
          public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown()) {
              e.consume();
              rightPanel.zoom(e.getWheelRotation() > 0 ? -1 : +1, e.getPoint().x);
            } else e.getComponent().getParent().dispatchEvent(e);
          }
        };
    // We can't put it on the scroll pane, because ordering of listeners isn's
    // specified and we need to be first to prevent default scroll behavior
    // when control is down.
    // leftScroll.addMouseWheelListener(zoomer);
    // rightScroll.addMouseWheelListener(zoomer);
    leftPanel.addMouseWheelListener(zoomer);
    rightPanel.addMouseWheelListener(zoomer);
    leftPanel.getTableHeader().addMouseWheelListener(zoomer);
    rightPanel.getTimelineHeader().addMouseWheelListener(zoomer);

    splitPane.setLeftComponent(leftScroll);
    splitPane.setRightComponent(rightScroll);

    leftScroll.setWheelScrollingEnabled(true);
    rightScroll.setWheelScrollingEnabled(true);

    setSignalCursorX(Integer.MAX_VALUE);
    // put right scrollbar into same position
    rightScroll.getHorizontalScrollBar().setValue(p);

    leftPanel.getSelectionModel().addListSelectionListener(e -> editHandler.computeEnabled());
  }
  @Override
  public void localeChanged() {
    selButton.setText(S.get("addRemoveSignals"));
  }
  public LeftPanel getLeftPanel() {
    return leftPanel;
  }

  public RightPanel getRightPanel() {
    return rightPanel;
  }

  public JScrollBar getVerticalScrollBar() {
    return rightScroll == null ? null : rightScroll.getVerticalScrollBar();
  }

  public JScrollBar getHorizontalScrollBar() {
    return rightScroll == null ? null : rightScroll.getHorizontalScrollBar();
  }

  public JViewport getRightViewport() {
    return rightScroll == null ? null : rightScroll.getViewport();
  }

  public int getVisibleSignalsWidth() {
    return splitPane.getRightComponent().getWidth();
  }

  @Override
  public String getTitle() {
    return S.get("ChronoTitle");
  }

  @Override
  public String getHelpText() {
    return S.get("ChronoTitle");
  }

  @Override
  public void modelChanged(Model oldModel, Model newModel) {
    setModel(newModel);
    rightPanel.setModel(newModel);
    leftPanel.setModel(newModel);
    editHandler.computeEnabled();
  }

  public void changeSpotlight(Signal s) {
    final var old = model.setSpotlight(s);
    if (old == s) return;
    rightPanel.changeSpotlight(old, s);
    leftPanel.changeSpotlight(old, s);
  }

  public void setSignalCursorX(int posX) {
    rightPanel.setSignalCursorX(posX);
    leftPanel.updateSignalValues();
  }

  @Override
  public void signalsExtended(Model.Event event) {
    leftPanel.updateSignalValues();
    rightPanel.updateWaveforms(true);
  }

  @Override
  public void signalsReset(Model.Event event) {
    setSignalCursorX(Integer.MAX_VALUE);
    rightPanel.updateWaveforms(true);
  }

  @Override
  public void historyLimitChanged(Model.Event event) {
    setSignalCursorX(Integer.MAX_VALUE);
    rightPanel.updateWaveforms(false);
  }

  @Override
  public void selectionChanged(Model.Event event) {
    leftPanel.updateSignals();
    rightPanel.updateSignals();
    editHandler.computeEnabled();
  }

  @Override
  public Model getModel() {
    return model;
  }

  public void setModel(Model newModel) {
    if (model != null) model.removeModelListener(this);
    model = newModel;
    if (model == null) return;
    model.addModelListener(this);
  }

  private static final Color PLAIN_BG = new Color(0xbb, 0xbb, 0xbb);
  private static final Color PLAIN_HI = darker(PLAIN_BG);
  private static final Color PLAIN_LINE = Color.BLACK;
  private static final Color PLAIN_ERR = new Color(0xdb, 0x9d, 0x9d);
  private static final Color PLAIN_ERRLINE = Color.BLACK;
  private static final Color PLAIN_UNK = new Color(0xea, 0xaa, 0x6c);
  private static final Color PLAIN_UNKLINE = Color.BLACK;
  private static final Color SPOT_BG = new Color(0xaa, 0xff, 0xaa);
  private static final Color SPOT_HI = darker(SPOT_BG);
  private static final Color SPOT_LINE = Color.BLACK;
  private static final Color SPOT_ERR = new Color(0xf9, 0x76, 0x76);
  private static final Color SPOT_ERRLINE = Color.BLACK;
  private static final Color SPOT_UNK = new Color(0xea, 0x98, 0x49);
  private static final Color SPOT_UNKLINE = Color.BLACK;
  private final Color selectBg; // set in constructor
  private final Color selectHi; // set in constructor
  private static final Color SELECT_LINE = Color.BLACK;
  private static final Color SELECT_ERR = new Color(0xe5, 0x80, 0x80);
  private static final Color SELECT_ERRLINE = Color.BLACK;
  private static final Color SELECT_UNK = new Color(0xee, 0x99, 0x44);
  private static final Color SELECT_UNKLINE = Color.BLACK;
  private static final Color[] SPOT = {
    SPOT_BG, SPOT_HI, SPOT_LINE, SPOT_ERR, SPOT_ERRLINE, SPOT_UNK, SPOT_UNKLINE
  };
  private static final Color[] PLAIN = {
    PLAIN_BG, PLAIN_HI, PLAIN_LINE, PLAIN_ERR, PLAIN_ERRLINE, PLAIN_UNK, PLAIN_UNKLINE
  };
  private final Color[] selectColors; // set in constructor

  public Color[] rowColors(SignalInfo item, boolean isSelected) {
    if (isSelected) return selectColors;
    final var spotlight = model.getSpotlight();
    return (spotlight != null && spotlight.info == item) ? SPOT : PLAIN;
  }

  private static Color darker(Color c) {
    if (c == null) return null;
    final var hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    final var s = 0.8f;
    return (hsb[1] == 0.0)
        ? Color.getHSBColor(hsb[0], hsb[1] + hsb[1], hsb[2] * s)
        : Color.getHSBColor(hsb[0], 1.0f - (1.0f - hsb[1]) * s, hsb[2]);
  }

  @Override
  public EditHandler getEditHandler() {
    return editHandler;
  }

  final EditHandler editHandler =
      new EditHandler() {
        @Override
        public void computeEnabled() {
          boolean empty = model.getSignalCount() == 0;
          boolean sel = !empty && !leftPanel.getSelectionModel().isSelectionEmpty();
          setEnabled(LogisimMenuBar.CUT, sel);
          setEnabled(LogisimMenuBar.COPY, sel);
          setEnabled(LogisimMenuBar.PASTE, true);
          setEnabled(LogisimMenuBar.DELETE, sel);
          setEnabled(LogisimMenuBar.DUPLICATE, false);
          setEnabled(LogisimMenuBar.SELECT_ALL, !empty);
          // TODO: raise/lower handlers
          setEnabled(LogisimMenuBar.RAISE, sel);
          setEnabled(LogisimMenuBar.LOWER, sel);
          setEnabled(LogisimMenuBar.RAISE_TOP, sel);
          setEnabled(LogisimMenuBar.LOWER_BOTTOM, sel);
          setEnabled(LogisimMenuBar.ADD_CONTROL, false);
          setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
          final var action = e.getSource();
          leftPanel.getActionMap().get(action).actionPerformed(e);
        }
      };

  @Override
  public PrintHandler getPrintHandler() {
    return printHandler;
  }

  final PrintHandler printHandler =
      new PrintHandler() {
        @Override
        public Dimension getExportImageSize() {
          final var l = leftPanel.getPreferredSize();
          final var r = rightPanel.getPreferredSize();
          final var width = l.width + 3 + r.width;
          final var height = HEADER_HEIGHT + l.height;
          return new Dimension(width, height);
        }

        @Override
        public void paintExportImage(BufferedImage img, Graphics2D g) {
          final var l = leftPanel.getPreferredSize();
          final var r = rightPanel.getPreferredSize();

          g.setClip(0, 0, l.width, HEADER_HEIGHT);
          leftPanel.getTableHeader().print(g);

          g.setClip(l.width + 3, 0, r.width, HEADER_HEIGHT);
          g.translate(l.width + 3, 0);
          rightPanel.getTimelineHeader().print(g);
          g.translate(-(l.width + 3), 0);

          g.setClip(0, HEADER_HEIGHT, l.width, l.height);
          g.translate(0, HEADER_HEIGHT);
          leftPanel.print(g);
          g.translate(0, -HEADER_HEIGHT);

          g.setClip(l.width + 3, HEADER_HEIGHT, r.width, l.height);
          g.translate(l.width + 3, HEADER_HEIGHT);
          rightPanel.print(g);
          g.translate(-(l.width + 3), -HEADER_HEIGHT);
        }

        @Override
        public int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h) {
          if (pageNum != 0) return Printable.NO_SUCH_PAGE;

          // shrink horizontally to fit
          final var fm = g.getFontMetrics();
          final var d = getExportImageSize();
          final var headerHeight = fm.getHeight() * 1.5;
          var scale = 1.0D;
          if (d.width > w || d.height > (h - headerHeight))
            scale = Math.min(w / d.width, (h - headerHeight) / d.height);

          GraphicsUtil.drawText(
              g,
              S.get(
                  "ChronoPrintTitle",
                  model.getCircuit().getName(),
                  getProject().getLogisimFile().getDisplayName()),
              (int) (w / 2),
              0,
              GraphicsUtil.H_CENTER,
              GraphicsUtil.V_TOP);

          g.translate(0, fm.getHeight() * 1.5);
          g.scale(scale, scale);
          paintExportImage(null, g);

          return Printable.PAGE_EXISTS;
        }
      };
}
