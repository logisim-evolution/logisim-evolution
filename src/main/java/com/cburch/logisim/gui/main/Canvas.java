/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.contracts.BaseMouseInputListenerContract;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.MouseMappings;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.CanvasPaneContents;
import com.cburch.logisim.gui.generic.GridPainter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.vhdl.base.HdlModel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class Canvas extends JPanel implements LocaleListener, CanvasPaneContents, AdjustmentListener {

  public static final byte ZOOM_BUTTON_SIZE = 52;
  public static final byte ZOOM_BUTTON_MARGIN = 30;
  public static final Color HALO_COLOR = new Color(255, 0, 255);
  // don't bother to update the size if it hasn't changed more than this
  static final double SQRT_2 = Math.sqrt(2.0);
  private static final long serialVersionUID = 1L;
  // pixels shown in canvas beyond outermost boundaries
  private static final int THRESH_SIZE_UPDATE = 10;
  private static final int BUTTONS_MASK =
      InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;
  private static final Color DEFAULT_ERROR_COLOR = new Color(192, 0, 0);
  private static final Color OSC_ERR_COLOR = DEFAULT_ERROR_COLOR;
  private static final Color SIM_EXCEPTION_COLOR = DEFAULT_ERROR_COLOR;
  private static final Font ERR_MSG_FONT = new Font("Sans Serif", Font.BOLD, 18);
  private static final Color TICK_RATE_COLOR = new Color(0, 0, 92, 92);
  private static final Font TICK_RATE_FONT = new Font("Monospaced", Font.PLAIN, 28);
  private static final Color SINGLE_STEP_MSG_COLOR = Color.BLUE;
  private static final Font SINGLE_STEP_MSG_FONT = new Font("Sans Serif", Font.BOLD, 12);
  public static final Color DEFAULT_ZOOM_BUTTON_COLOR = Color.WHITE;
  // public static BufferedImage image;
  private final Project proj;
  private final Selection selection;
  private final MyListener myListener = new MyListener();
  private final MyViewport viewport = new MyViewport();
  private final MyProjectListener myProjectListener = new MyProjectListener();
  private final TickCounter tickCounter;
  private final CanvasPaintThread paintThread;
  private final CanvasPainter painter;
  private final Object repaintLock = new Object(); // for waitForRepaintDone
  private Tool dragTool;
  private Tool tempTool;
  private MouseMappings mappings;
  private CanvasPane canvasPane;
  private Bounds oldPreferredSize;
  private volatile boolean paintDirty = false; // only for within paintComponent
  private boolean inPaint = false; // only for within paintComponent

  public Canvas(Project proj) {
    this.proj = proj;
    this.selection = new Selection(proj, this);
    this.painter = new CanvasPainter(this);
    this.oldPreferredSize = null;
    this.paintThread = new CanvasPaintThread(this);
    this.mappings = proj.getOptions().getMouseMappings();
    this.canvasPane = null;
    this.tickCounter = new TickCounter();

    setBackground(new Color(AppPreferences.CANVAS_BG_COLOR.get()));
    addMouseListener(myListener);
    addMouseMotionListener(myListener);
    addMouseWheelListener(myListener);
    addKeyListener(myListener);

    proj.addProjectListener(myProjectListener);
    proj.addLibraryListener(myProjectListener);
    proj.addCircuitListener(myProjectListener);
    proj.getSimulator().addSimulatorListener(tickCounter);
    selection.addListener(myProjectListener);
    LocaleManager.addLocaleListener(this);

    final var options = proj.getOptions().getAttributeSet();
    options.addAttributeListener(myProjectListener);
    AppPreferences.COMPONENT_TIPS.addPropertyChangeListener(myListener);
    AppPreferences.GATE_SHAPE.addPropertyChangeListener(myListener);
    AppPreferences.SHOW_TICK_RATE.addPropertyChangeListener(myListener);
    AppPreferences.CANVAS_BG_COLOR.addPropertyChangeListener(myListener);
    loadOptions(options);
    paintThread.start();
  }

  public static boolean autoZoomButtonClicked(final Dimension sz,
                                              final double x, final double y) {
    return Point2D.distance(
            x,
            y,
            sz.width - ZOOM_BUTTON_SIZE / 2 - ZOOM_BUTTON_MARGIN,
            sz.height - ZOOM_BUTTON_MARGIN - ZOOM_BUTTON_SIZE / 2)
        <= ZOOM_BUTTON_SIZE / 2;
  }

  public static void paintAutoZoomButton(final Graphics g,
                                         final Dimension sz, final Color zoomButtonColor) {
    final var oldColor = g.getColor();
    g.setColor(TICK_RATE_COLOR);
    g.fillOval(
            sz.width - ZOOM_BUTTON_SIZE - 33,
            sz.height - ZOOM_BUTTON_SIZE - 33,
            ZOOM_BUTTON_SIZE + 6,
            ZOOM_BUTTON_SIZE + 6);
    g.setColor(zoomButtonColor);
    g.fillOval(
        sz.width - ZOOM_BUTTON_SIZE - 30,
        sz.height - ZOOM_BUTTON_SIZE - 30,
        ZOOM_BUTTON_SIZE,
        ZOOM_BUTTON_SIZE);
    g.setColor(Value.unknownColor);
    GraphicsUtil.switchToWidth(g, 3);
    int width = sz.width - ZOOM_BUTTON_MARGIN;
    int height = sz.height - ZOOM_BUTTON_MARGIN;
    g.drawOval(
            width - ZOOM_BUTTON_SIZE * 3 / 4,
            height - ZOOM_BUTTON_SIZE * 3 / 4,
            ZOOM_BUTTON_SIZE / 2,
            ZOOM_BUTTON_SIZE / 2);
    g.drawLine(
        width - ZOOM_BUTTON_SIZE / 4 + 4,
        height - ZOOM_BUTTON_SIZE / 2,
        width - ZOOM_BUTTON_SIZE * 3 / 4 - 4,
        height - ZOOM_BUTTON_SIZE / 2);
    g.drawLine(
        width - ZOOM_BUTTON_SIZE / 2,
        height - ZOOM_BUTTON_SIZE / 4 + 4,
        width - ZOOM_BUTTON_SIZE / 2,
        height - ZOOM_BUTTON_SIZE * 3 / 4 - 4);
    g.setColor(oldColor);
  }

  public static void snapToGrid(final MouseEvent e) {
    final var oldX = e.getX();
    final var oldY = e.getY();
    final var newX = snapXToGrid(oldX);
    final var newY = snapYToGrid(oldY);
    e.translatePoint(newX - oldX, newY - oldY);
  }

  //
  // static methods
  //
  public static int snapXToGrid(int x) {
    return x < 0
      ? -((-x + 5) / 10) * 10
      : ((x + 5) / 10) * 10;
  }

  public static int snapYToGrid(int y) {
    return y < 0
      ? -((-y + 5) / 10) * 10
      : ((y + 5) / 10) * 10;
  }

  public CanvasPane getCanvasPane() {
    return canvasPane;
  }

  //
  // CanvasPaneContents methods
  //
  @Override
  public void setCanvasPane(final CanvasPane value) {
    canvasPane = value;
    canvasPane.setViewport(viewport);
    canvasPane.getHorizontalScrollBar().addAdjustmentListener(this);
    canvasPane.getVerticalScrollBar().addAdjustmentListener(this);
    viewport.setView(this);
    setOpaque(false);
    computeSize(true);
  }

  @Override
  public void center() {
    final var g = getGraphics();
    final var bounds = (g != null)
        ? proj.getCurrentCircuit().getBounds(getGraphics())
        : proj.getCurrentCircuit().getBounds();
    if (bounds.getHeight() == 0 || bounds.getWidth() == 0) {
      setScrollBar(0, 0);
      return;
    }
    final var xpos =
        (int)
            (Math.round(
                bounds.getX() * getZoomFactor()
                    - (canvasPane.getViewport().getSize().getWidth()
                            - bounds.getWidth() * getZoomFactor())
                        / 2));
    final var ypos =
        (int)
            (Math.round(
                bounds.getY() * getZoomFactor()
                    - (canvasPane.getViewport().getSize().getHeight()
                            - bounds.getHeight() * getZoomFactor())
                        / 2));
    setScrollBar(xpos, ypos);
  }

  public void closeCanvas() {
    paintThread.requestStop();
  }

  private void completeAction() {
    if (proj.getCurrentCircuit() == null) return;
    computeSize(false);
    // After any interaction, nudge the simulator, which in autoPropagate mode
    // will (if needed) eventually, fire a propagateCompleted event, which will
    // cause a repaint. If not in autoPropagate mode, do the repaint here
    // instead.
    if (!proj.getSimulator().nudge()) paintThread.requestRepaint();
  }

  public void computeSize(final boolean immediate) {
    if (proj.getCurrentCircuit() == null) return;
    final var g = getGraphics();
    final var bounds = (g != null)
            ? proj.getCurrentCircuit().getBounds(getGraphics())
            : proj.getCurrentCircuit().getBounds();
    var height = 0;
    var width = 0;
    if (bounds != null && viewport != null) {
      width = bounds.getX() + bounds.getWidth() + viewport.getWidth();
      height = bounds.getY() + bounds.getHeight() + viewport.getHeight();
    }
    final var dim = (canvasPane == null)
            ? new Dimension(width, height)
            : canvasPane.supportPreferredSize(width, height);
    if (!immediate) {
      final var old = oldPreferredSize;
      if (old != null
          && Math.abs(old.getWidth() - dim.width) < THRESH_SIZE_UPDATE
          && Math.abs(old.getHeight() - dim.height) < THRESH_SIZE_UPDATE) {
        return;
      }
    }
    oldPreferredSize = Bounds.create(0, 0, dim.width, dim.height);
    setPreferredSize(dim);
    revalidate();
  }

  public Rectangle getViewableRect() {
    Rectangle viewableBase;
    Rectangle viewable;
    if (canvasPane != null) {
      viewableBase = canvasPane.getViewport().getViewRect();
    } else {
      final var bds = proj.getCurrentCircuit().getBounds();
      viewableBase = new Rectangle(0, 0, bds.getWidth(), bds.getHeight());
    }
    final var zoom = getZoomFactor();
    if (zoom == 1.0) {
      viewable = viewableBase;
    } else {
      viewable =
          new Rectangle(
              (int) (viewableBase.x / zoom),
              (int) (viewableBase.y / zoom),
              (int) (viewableBase.width / zoom),
              (int) (viewableBase.height / zoom));
    }
    return viewable;
  }

  private void computeViewportContents() {
    final var exceptions = proj.getCurrentCircuit().getWidthIncompatibilityData();
    if (exceptions == null || exceptions.size() == 0) {
      viewport.setWidthMessage(null);
      return;
    }
    viewport.setWidthMessage(
        S.get("canvasWidthError") + (exceptions.size() == 1 ? "" : " (" + exceptions.size() + ")"));
    for (final var ex : exceptions) {
      final var p = ex.getPoint(0);
      setArrows(p.getX(), p.getY(), p.getX(), p.getY());
    }
  }

  //
  // access methods
  //
  public Circuit getCircuit() {
    return proj.getCurrentCircuit();
  }

  public HdlModel getCurrentHdl() {
    return proj.getCurrentHdl();
  }

  public CircuitState getCircuitState() {
    return proj.getCircuitState();
  }

  Tool getDragTool() {
    return dragTool;
  }

  public StringGetter getErrorMessage() {
    return viewport.errorMessage;
  }

  public void setErrorMessage(final StringGetter message) {
    viewport.setErrorMessage(message, null);
  }

  public void setErrorMessage(final StringGetter message, final Color color) {
    viewport.setErrorMessage(message, color);
  }

  GridPainter getGridPainter() {
    return painter.getGridPainter();
  }

  Component getHaloedComponent() {
    return painter.getHaloedComponent();
  }

  public int getHorizontalScrollBar() {
    return canvasPane.getHorizontalScrollBar().getValue();
  }

  public int getVerticalScrollBar() {
    return canvasPane.getVerticalScrollBar().getValue();
  }

  public void setVerticalScrollBar(int posY) {
    canvasPane.getVerticalScrollBar().setValue(posY);
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  public Project getProject() {
    return proj;
  }

  @Override
  public int getScrollableBlockIncrement(final Rectangle visibleRect,
                                         final int orientation, final int direction) {
    return canvasPane.supportScrollableBlockIncrement(visibleRect, orientation, direction);
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return false;
  }

  @Override
  public int getScrollableUnitIncrement(final Rectangle visibleRect,
                                        final int orientation, final int direction) {
    return canvasPane.supportScrollableUnitIncrement(visibleRect, orientation, direction);
  }

  public Selection getSelection() {
    return selection;
  }

  @Override
  public String getToolTipText(final MouseEvent event) {
    boolean showTips = AppPreferences.COMPONENT_TIPS.getBoolean();
    if (showTips) {
      Canvas.snapToGrid(event);
      final var loc = Location.create(event.getX(), event.getY(), false);
      ComponentUserEvent e = null;
      for (final var comp : getCircuit().getAllContaining(loc)) {
        Object makerObj = comp.getFeature(ToolTipMaker.class);
        if (makerObj instanceof ToolTipMaker maker) {
          if (e == null) {
            e = new ComponentUserEvent(this, loc.getX(), loc.getY());
          }
          final var ret = maker.getToolTip(e);
          if (ret != null) {
            unrepairMouseEvent(event);
            return ret;
          }
        }
      }
    }
    return null;
  }

  //
  // graphics methods
  //
  double getZoomFactor() {
    final var pane = canvasPane;
    return pane == null ? 1.0 : pane.getZoomFactor();
  }

  boolean ifPaintDirtyReset() {
    if (paintDirty) {
      paintDirty = false;
      return false;
    } else {
      return true;
    }
  }

  boolean isPopupMenuUp() {
    return myListener.menuOn;
  }

  private void loadOptions(final AttributeSet options) {
    final var showTips = AppPreferences.COMPONENT_TIPS.getBoolean();
    setToolTipText(showTips ? "" : null);

    proj.getSimulator().removeSimulatorListener(myProjectListener);
    proj.getSimulator().addSimulatorListener(myProjectListener);
  }

  @Override
  public void localeChanged() {
    paintThread.requestRepaint();
  }

  @Override
  public void paintComponent(final Graphics g) {
    if (AppPreferences.AntiAliassing.getBoolean()) {
      final var g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    inPaint = true;
    try {
      super.paintComponent(g);
      boolean clear = false;
      do {
        if (clear) {
          /* Kevin Walsh:
           * Clear the screen so we don't get
           * artifacts due to aliasing (e.g. where
           * semi-transparent (gray) pixels on the
           * edges of a line turn would darker if
           * painted a second time.
           */
          g.setColor(Color.WHITE);
          g.fillRect(0, 0, getWidth(), getHeight());
        }
        clear = true;
        painter.paintContents(g, proj);
      } while (paintDirty);
      if (canvasPane == null) {
        viewport.paintContents(g);
      }
    } finally {
      inPaint = false;
      synchronized (repaintLock) {
        repaintLock.notifyAll();
      }
    }
  }

  @Override
  protected void processMouseEvent(final MouseEvent e) {
    repairMouseEvent(e);
    super.processMouseEvent(e);
  }

  @Override
  protected void processMouseMotionEvent(final MouseEvent e) {
    repairMouseEvent(e);
    super.processMouseMotionEvent(e);
  }

  @Override
  public void recomputeSize() {
    computeSize(true);
  }

  @Override
  public void repaint() {
    if (inPaint) {
      paintDirty = true;
    } else {
      super.repaint();
    }
  }

  @Override
  public void repaint(int x, int y, int width, int height) {
    final var zoom = getZoomFactor();
    if (zoom < 1.0) {
      final var newX = (int) Math.floor(x * zoom);
      final var newY = (int) Math.floor(y * zoom);
      width += x - newX;
      height += y - newY;
      x = newX;
      y = newY;
    } else if (zoom > 1.0) {
      final var x1 = (int) Math.ceil((x + width) * zoom);
      final var y1 = (int) Math.ceil((y + height) * zoom);
      width = x1 - x;
      height = y1 - y;
    }
    super.repaint(x, y, width, height);
  }

  @Override
  public void repaint(Rectangle r) {
    final var zoom = getZoomFactor();
    if (zoom == 1.0) {
      super.repaint(r);
    } else {
      this.repaint(r.x, r.y, r.width, r.height);
    }
  }

  private void repairMouseEvent(MouseEvent e) {
    final var zoom = getZoomFactor();
    if (zoom != 1.0) {
      zoomEvent(e, zoom);
    }
  }

  public void updateArrows() {
    /* Disable for VHDL content */
    if (proj.getCurrentCircuit() == null) return;
    final var g = getGraphics();
    final var circBds = (g != null)
            ? proj.getCurrentCircuit().getBounds(getGraphics())
            : proj.getCurrentCircuit().getBounds();
    // no circuit
    if (circBds == null || circBds.getHeight() == 0 || circBds.getWidth() == 0) return;
    var x = circBds.getX();
    var y = circBds.getY();
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    setArrows(x, y, x + circBds.getWidth(), y + circBds.getHeight());
  }

  public void setArrows(int x0, int y0, int x1, int y1) {
    /* Disable for VHDL content */
    if (proj.getCurrentCircuit() == null) return;
    viewport.clearArrows();
    Rectangle viewableBase;
    Rectangle viewable;
    if (canvasPane != null) {
      viewableBase = canvasPane.getViewport().getViewRect();
    } else {
      final var g = getGraphics();
      final var bds = (g != null)
            ? proj.getCurrentCircuit().getBounds(getGraphics())
            : proj.getCurrentCircuit().getBounds();
      viewableBase = new Rectangle(0, 0, bds.getWidth(), bds.getHeight());
    }
    double zoom = getZoomFactor();
    if (zoom == 1.0) {
      viewable = viewableBase;
    } else {
      viewable =
          new Rectangle(
              (int) (viewableBase.x / zoom),
              (int) (viewableBase.y / zoom),
              (int) (viewableBase.width / zoom),
              (int) (viewableBase.height / zoom));
    }
    final var isWest = x0 < viewable.x;
    final var isEast = x1 >= viewable.x + viewable.width;
    final var isNorth = y0 < viewable.y;
    final var isSouth = y1 >= viewable.y + viewable.height;

    if (isNorth) {
      if (isEast) viewport.setNortheast(true);
      if (isWest) viewport.setNorthwest(true);
      if (!isWest && !isEast) viewport.setNorth(true);
    }
    if (isSouth) {
      if (isEast) viewport.setSoutheast(true);
      if (isWest) viewport.setSouthwest(true);
      if (!isWest && !isEast) viewport.setSouth(true);
    }
    if (isEast && !viewport.isSoutheast && !viewport.isNortheast) viewport.setEast(true);
    if (isWest && !viewport.isSouthwest && !viewport.isNorthwest) viewport.setWest(true);
  }

  void setHaloedComponent(Circuit circ, Component comp) {
    painter.setHaloedComponent(circ, comp);
  }

  public void setHighlightedWires(WireSet value) {
    painter.setHighlightedWires(value);
  }

  public void setHorizontalScrollBar(int posX) {
    canvasPane.getHorizontalScrollBar().setValue(posX);
  }

  public void setScrollBar(int posX, int posY) {
    setHorizontalScrollBar(posX);
    setVerticalScrollBar(posY);
  }

  public void showPopupMenu(JPopupMenu menu, int x, int y) {
    final var zoom = getZoomFactor();
    if (zoom != 1.0) {
      x = (int) Math.round(x * zoom);
      y = (int) Math.round(y * zoom);
    }
    myListener.menuOn = true;
    menu.addPopupMenuListener(myListener);
    menu.show(this, x, y);
  }

  private void unrepairMouseEvent(MouseEvent e) {
    final var zoom = getZoomFactor();
    if (zoom != 1.0) {
      zoomEvent(e, 1.0 / zoom);
    }
  }

  private void waitForRepaintDone() {
    synchronized (repaintLock) {
      try {
        while (inPaint) {
          repaintLock.wait();
        }
      } catch (InterruptedException ignored) {
      }
    }
  }

  private void zoomEvent(MouseEvent e, double zoom) {
    final var oldx = e.getX();
    final var oldy = e.getY();
    final var newx = (int) Math.round(e.getX() / zoom);
    final var newy = (int) Math.round(e.getY() / zoom);
    e.translatePoint(newx - oldx, newy - oldy);
  }

  @Override
  public void adjustmentValueChanged(AdjustmentEvent e) {
    updateArrows();
  }

  private void doZoom(Point mouseLocation, boolean zoomIn) {
    var zoomControl = proj.getFrame().getZoomControl();
    if (zoomIn) {
      zoomControl.zoomIn();
    } else {
      zoomControl.zoomOut();
    }
    if (mouseLocation != null) {
      final var rect = getViewableRect();
      final var zoom = proj.getFrame().getZoomModel().getZoomFactor();
      setHorizontalScrollBar((int) ((mouseLocation.getX() - rect.width / 2) * zoom));
      setVerticalScrollBar((int) ((mouseLocation.getY() - rect.height / 2) * zoom));
    }
  }

  private class MyListener
      implements BaseMouseInputListenerContract,
          KeyListener,
          PopupMenuListener,
          PropertyChangeListener,
          MouseWheelListener {

    boolean menuOn = false;

    private Tool getToolFor(MouseEvent e) {
      if (menuOn) {
        return null;
      }

      Tool ret = mappings.getToolFor(e);
      if (ret == null) {
        return proj.getTool();
      } else {
        return ret;
      }
    }

    //
    // KeyListener methods
    //
    @Override
    public void keyPressed(KeyEvent e) {
      if (e.isControlDown()) { // If CTRL is pressed, check for + or -
        final var ml = Canvas.this.getMousePosition(); // Determine mouse location
        if (ml != null) { // Handle the Cursor not being on the component
          final var oldx = ml.x;
          final var oldy = ml.y;
          final var newx = (int) Math.round(ml.getX() / getZoomFactor());
          final var newy = (int) Math.round(ml.getY() / getZoomFactor());
          ml.translate(newx - oldx, newy - oldy);
        }
        switch (e.getKeyCode()) {
          case KeyEvent.VK_PLUS: // Accept keycode for plus on main block
          case KeyEvent.VK_ADD: // Also accept for the plus on the num-pad
            doZoom(ml, true);
            return;
          case KeyEvent.VK_MINUS: // Keycode for minus on main block
          case KeyEvent.VK_SUBTRACT: // Keycode for minus on num-pad
            doZoom(ml, false); // For - zoom out
            return;
          default: // If another key was pressed do nothing
        }
      }
      final var tool = proj.getTool();
      if (tool != null) {
        tool.keyPressed(Canvas.this, e);
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      final var tool = proj.getTool();
      if (tool != null) {
        tool.keyReleased(Canvas.this, e);
      }
    }

    @Override
    public void keyTyped(KeyEvent e) {
      final var tool = proj.getTool();
      if (tool != null) {
        tool.keyTyped(Canvas.this, e);
      }
    }

    //
    // MouseListener methods
    //
    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
      // do nothing
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      if (dragTool != null) {
        dragTool.mouseDragged(Canvas.this, getGraphics(), e);
        final var zoomModel = proj.getFrame().getZoomModel();
        double zoomFactor = zoomModel.getZoomFactor();
        scrollRectToVisible(
            new Rectangle((int) (e.getX() * zoomFactor), (int) (e.getY() * zoomFactor), 1, 1));
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      if (dragTool != null) {
        dragTool.mouseEntered(Canvas.this, getGraphics(), e);
      } else {
        final var tool = getToolFor(e);
        if (tool != null) {
          tool.mouseEntered(Canvas.this, getGraphics(), e);
        }
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      if (dragTool != null) {
        dragTool.mouseExited(Canvas.this, getGraphics(), e);
      } else {
        final var tool = getToolFor(e);
        if (tool != null) {
          tool.mouseExited(Canvas.this, getGraphics(), e);
        }
      }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      if ((e.getModifiersEx() & BUTTONS_MASK) != 0) {
        // If the control key is down while the mouse is being
        // dragged, mouseMoved is called instead. This may well be
        // an issue specific to the MacOS Java implementation,
        // but it exists there in the 1.4 and 5.0 versions.
        mouseDragged(e);
        return;
      }

      final var tool = getToolFor(e);
      if (tool != null) {
        tool.mouseMoved(Canvas.this, getGraphics(), e);
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      viewport.setErrorMessage(null, null);
      if (proj.isStartupScreen()) {
        final var g = getGraphics();
        final var bounds = (g != null)
              ? proj.getCurrentCircuit().getBounds(getGraphics())
              : proj.getCurrentCircuit().getBounds();
        // set the project as dirty only if it contains something
        if (bounds.getHeight() != 0 || bounds.getWidth() != 0) proj.setStartupScreen(false);
      }
      if (e.getButton() == MouseEvent.BUTTON1
          && viewport.zoomButtonVisible
          && autoZoomButtonClicked(
              viewport.getSize(),
              e.getX() * getZoomFactor() - getHorizontalScrollBar(),
              e.getY() * getZoomFactor() - getVerticalScrollBar())) {
        viewport.zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR.darker();
        viewport.repaint();
      } else {
        Canvas.this.requestFocus();
        dragTool = getToolFor(e);
        if (dragTool != null) {
          dragTool.mousePressed(Canvas.this, getGraphics(), e);
          if (e.getButton() != MouseEvent.BUTTON1) {
            tempTool = proj.getTool();
            proj.setTool(dragTool);
          }
        }
        completeAction();
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if ((e.getButton() == MouseEvent.BUTTON1
              && viewport.zoomButtonVisible
              && autoZoomButtonClicked(
                  viewport.getSize(),
                  e.getX() * getZoomFactor() - getHorizontalScrollBar(),
                  e.getY() * getZoomFactor() - getVerticalScrollBar())
              && viewport.zoomButtonColor != DEFAULT_ZOOM_BUTTON_COLOR)
          || e.getButton() == MouseEvent.BUTTON2 && e.getClickCount() == 2) {
        center();
        setCursor(proj.getTool().getCursor());
      }
      if (dragTool != null) {
        dragTool.mouseReleased(Canvas.this, getGraphics(), e);
        dragTool = null;
      }
      if (tempTool != null) {
        proj.setTool(tempTool);
        tempTool = null;
      }
      final var tool = proj.getTool();
      if (tool != null && !(tool instanceof EditTool)) {
        tool.mouseMoved(Canvas.this, getGraphics(), e);
        setCursor(tool.getCursor());
      }
      completeAction();

      viewport.zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mwe) {
      final var tool = proj.getTool();
      if (mwe.isControlDown()) {
        repairMouseEvent(mwe);
        doZoom(mwe.getPoint(), mwe.getWheelRotation() < 0);
      } else if (tool instanceof PokeTool && ((PokeTool) tool).isScrollable()) {
        final var id = (mwe.getWheelRotation() < 0) ? KeyEvent.VK_UP : KeyEvent.VK_DOWN;
        final var e = new KeyEvent(mwe.getComponent(), KeyEvent.KEY_PRESSED, mwe.getWhen(), 0, id, '\0');
        tool.keyPressed(Canvas.this, e);
      } else {
        if (mwe.isShiftDown()) {
          canvasPane.getHorizontalScrollBar().setValue(scrollValue(canvasPane.getHorizontalScrollBar(), mwe.getWheelRotation()));
        } else {
          canvasPane.getVerticalScrollBar().setValue(scrollValue(canvasPane.getVerticalScrollBar(), mwe.getWheelRotation()));
        }
      }
    }

    //
    // PopupMenuListener mtehods
    //
    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
      menuOn = false;
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      menuOn = false;
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      // do nothing
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.GATE_SHAPE.isSource(event)
          || AppPreferences.SHOW_TICK_RATE.isSource(event)
          || AppPreferences.AntiAliassing.isSource(event)) {
        paintThread.requestRepaint();
      } else if (AppPreferences.COMPONENT_TIPS.isSource(event)) {
        final var showTips = AppPreferences.COMPONENT_TIPS.getBoolean();
        setToolTipText(showTips ? "" : null);
      } else if (AppPreferences.CANVAS_BG_COLOR.isSource(event)) {
        setBackground(new Color(AppPreferences.CANVAS_BG_COLOR.get()));
      }
    }

    private int scrollValue(JScrollBar bar, int val) {
      if (val > 0) {
        if (bar.getValue() < bar.getMaximum() + val * 2 * bar.getBlockIncrement()) {
          return bar.getValue() + val * 2 * bar.getBlockIncrement();
        }
      } else {
        if (bar.getValue() > bar.getMinimum() + val * 2 * bar.getBlockIncrement()) {
          return bar.getValue() + val * 2 * bar.getBlockIncrement();
        }
      }
      return 0;
    }
  }

  private class MyProjectListener
      implements ProjectListener,
          LibraryListener,
          CircuitListener,
          AttributeListener,
          Simulator.Listener,
          Selection.Listener {

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      Attribute<?> attr = e.getAttribute();
      if (attr == Options.ATTR_GATE_UNDEFINED) {
        final var circState = getCircuitState();
        circState.markComponentsDirty(getCircuit().getNonWires());
        // TODO actually, we'd want to mark all components in
        // subcircuits as dirty as well
      }
    }

    @Override
    public void circuitChanged(CircuitEvent event) {
      int act = event.getAction();
      if (act == CircuitEvent.ACTION_REMOVE) {
        final var c = (Component) event.getData();
        if (c == painter.getHaloedComponent()) {
          proj.getFrame().viewComponentAttributes(null, null);
        }
      } else if (act == CircuitEvent.ACTION_CLEAR) {
        if (painter.getHaloedComponent() != null) {
          proj.getFrame().viewComponentAttributes(null, null);
        }
      } else if (act == CircuitEvent.ACTION_INVALIDATE) {
        completeAction();
      }
    }

    private Tool findTool(List<? extends Tool> opts) {
      Tool ret = null;
      for (Tool o : opts) {
        if (ret == null && o != null) {
          ret = o;
        } else if (o instanceof EditTool) {
          ret = o;
        }
      }
      return ret;
    }

    @Override
    public void libraryChanged(LibraryEvent event) {
      if (event.getAction() == LibraryEvent.REMOVE_TOOL) {
        Object t = event.getData();
        Circuit circ = null;
        if (t instanceof AddTool) {
          t = ((AddTool) t).getFactory();
          if (t instanceof SubcircuitFactory subFact) {
            circ = subFact.getSubcircuit();
          }
        }

        if (t == proj.getCurrentCircuit() && t != null) {
          proj.setCurrentCircuit(proj.getLogisimFile().getMainCircuit());
        }

        if (proj.getTool() == event.getData()) {
          var next = findTool(proj.getLogisimFile().getOptions().getToolbarData().getContents());
          if (next == null) {
            for (Library lib : proj.getLogisimFile().getLibraries()) {
              next = findTool(lib.getTools());
              if (next != null) {
                break;
              }
            }
          }
          proj.setTool(next);
        }

        if (circ != null) {
          var state = getCircuitState();
          var last = state;
          while (state != null && state.getCircuit() != circ) {
            last = state;
            state = state.getParentState();
          }
          if (state != null) {
            getProject().setCircuitState(last.cloneState());
          }
        }
      }
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      int act = event.getAction();
      if (act == ProjectEvent.ACTION_SET_CURRENT) {
        viewport.setErrorMessage(null, null);
        if (painter.getHaloedComponent() != null) {
          proj.getFrame().viewComponentAttributes(null, null);
        }
      } else if (act == ProjectEvent.ACTION_SET_FILE) {
        final var old = (LogisimFile) event.getOldData();
        if (old != null) {
          old.getOptions().getAttributeSet().removeAttributeListener(this);
        }
        final var file = (LogisimFile) event.getData();
        if (file != null) {
          AttributeSet attrs = file.getOptions().getAttributeSet();
          attrs.addAttributeListener(this);
          loadOptions(attrs);
          mappings = file.getOptions().getMouseMappings();
        }
      } else if (act == ProjectEvent.ACTION_SET_TOOL) {
        viewport.setErrorMessage(null, null);

        final var t = event.getTool();
        if (t == null) {
          setCursor(Cursor.getDefaultCursor());
        } else {
          setCursor(t.getCursor());
        }
      } else if (act == ProjectEvent.ACTION_SET_STATE) {
        final var oldState = (CircuitState) event.getOldData();
        final var newState = (CircuitState) event.getData();
        if (oldState != null && newState != null) {
          final var oldProp = oldState.getPropagator();
          final var newProp = newState.getPropagator();
          if (oldProp != newProp) {
            tickCounter.clear();
          }
        }
      }

      if (act != ProjectEvent.ACTION_SELECTION
          && act != ProjectEvent.ACTION_START
          && act != ProjectEvent.UNDO_START) {
        completeAction();
      }
    }

    @Override
    public void selectionChanged(Selection.Event event) {
      repaint();
    }

    @Override
    public void propagationCompleted(Simulator.Event e) {
      paintThread.requestRepaint();
      if (e.didTick()) waitForRepaintDone();
    }

    @Override
    public void simulatorStateChanged(Simulator.Event e) {
      // do nothing
    }

    @Override
    public void simulatorReset(Simulator.Event e) {
      waitForRepaintDone();
    }
  }

  private class MyViewport extends JViewport {

    private static final long serialVersionUID = 1L;
    StringGetter errorMessage = null;
    String widthMessage = null;
    Color errorColor = DEFAULT_ERROR_COLOR;
    boolean isNorth = false;
    boolean isSouth = false;
    boolean isWest = false;
    boolean isEast = false;
    boolean isNortheast = false;
    boolean isNorthwest = false;
    boolean isSoutheast = false;
    boolean isSouthwest = false;
    boolean zoomButtonVisible = false;
    Color zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR;

    MyViewport() {
      // dummy
    }

    void clearArrows() {
      isNorth = false;
      isSouth = false;
      isWest = false;
      isEast = false;
      isNortheast = false;
      isNorthwest = false;
      isSoutheast = false;
      isSouthwest = false;
    }

    @Override
    public Color getBackground() {
      return getView() == null ? super.getBackground() : getView().getBackground();
    }

    @Override
    public void paintChildren(Graphics g) {
      super.paintChildren(g);
      paintContents(g);
    }

    void paintContents(Graphics g) {
      /*
       * TODO this is for the SimulatorPrototype class int speed =
       * proj.getSimulator().getSimulationSpeed(); String speedStr; if
       * (speed >= 10000000) { speedStr = (speed / 1000000) + " MHz"; }
       * else if (speed >= 1000000) { speedStr = (speed / 100000) / 10.0 +
       * " MHz"; } else if (speed >= 10000) { speedStr = (speed / 1000) +
       * " kHz"; } else if (speed >= 10000) { speedStr = (speed / 100) /
       * 10.0 + " kHz"; } else { speedStr = speed + " Hz"; } FontMetrics
       * fm = g.getFontMetrics(); g.drawString(speedStr, getWidth() - 10 -
       * fm.stringWidth(speedStr), getHeight() - 10);
       */
      if (AppPreferences.AntiAliassing.getBoolean()) {
        final var g2 = (Graphics2D) g;
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      }

      int msgY = getHeight() - 23;
      final var message = errorMessage;
      if (message != null) {
        g.setColor(errorColor);
        msgY = paintString(g, msgY, message.toString());
      }

      if (proj.getSimulator().isOscillating()) {
        g.setColor(OSC_ERR_COLOR);
        msgY = paintString(g, msgY, S.get("canvasOscillationError"));
      }

      if (proj.getSimulator().isExceptionEncountered()) {
        g.setColor(SIM_EXCEPTION_COLOR);
        msgY = paintString(g, msgY, S.get("canvasExceptionError"));
      }

      computeViewportContents();
      final var sz = getSize();

      if (widthMessage != null) {
        g.setColor(Value.widthErrorColor);
        msgY = paintString(g, msgY, widthMessage);
      } else g.setColor(TICK_RATE_COLOR);

      if (isNorth
          || isSouth
          || isEast
          || isWest
          || isNortheast
          || isNorthwest
          || isSoutheast
          || isSouthwest) {
        zoomButtonVisible = true;
        paintAutoZoomButton(g, getSize(), zoomButtonColor);
        if (isNorth)
          GraphicsUtil.drawArrow2(g, sz.width / 2 - 20, 15, sz.width / 2, 5, sz.width / 2 + 20, 15);
        if (isSouth)
          GraphicsUtil.drawArrow2(
              g,
              sz.width / 2 - 20,
              sz.height - 15,
              sz.width / 2,
              sz.height - 5,
              sz.width / 2 + 20,
              sz.height - 15);
        if (isEast)
          GraphicsUtil.drawArrow2(
              g,
              sz.width - 15,
              sz.height / 2 + 20,
              sz.width - 5,
              sz.height / 2,
              sz.width - 15,
              sz.height / 2 - 20);
        if (isWest)
          GraphicsUtil.drawArrow2(
              g, 15, sz.height / 2 + 20, 5, sz.height / 2, 15, sz.height / 2 + (-20));
        if (isNortheast)
          GraphicsUtil.drawArrow2(g, sz.width - 30, 5, sz.width - 5, 5, sz.width - 5, 30);
        if (isNorthwest) GraphicsUtil.drawArrow2(g, 30, 5, 5, 5, 5, 30);
        if (isSoutheast)
          GraphicsUtil.drawArrow2(
              g,
              sz.width - 30,
              sz.height - 5,
              sz.width - 5,
              sz.height - 5,
              sz.width - 5,
              sz.height - 30);
        if (isSouthwest)
          GraphicsUtil.drawArrow2(g, 30, sz.height - 5, 5, sz.height - 5, 5, sz.height - 30);
      } else zoomButtonVisible = false;
      if (AppPreferences.SHOW_TICK_RATE.getBoolean()) {
        final var hz = tickCounter.getTickRate();
        if (hz != null && !hz.equals("")) {
          final var fm = g.getFontMetrics();
          final var x = 10;
          final var y = fm.getAscent() + 10;

          g.setColor(new Color(AppPreferences.CLOCK_FREQUENCY_COLOR.get()));
          g.setFont(TICK_RATE_FONT);
          g.drawString(hz, x, y);
        }
      }

      if (!proj.getSimulator().isAutoPropagating()) {
        g.setColor(SINGLE_STEP_MSG_COLOR);
        final var old = g.getFont();
        g.setFont(SINGLE_STEP_MSG_FONT);
        g.drawString(proj.getSimulator().getSingleStepMessage(), 10, 15);
        g.setFont(old);
      }

      g.setColor(Color.BLACK);
    }

    private int paintString(Graphics g, int y, String msg) {
      final var old = g.getFont();
      g.setFont(ERR_MSG_FONT);
      final var fm = g.getFontMetrics();
      var x = (getWidth() - fm.stringWidth(msg)) / 2;
      if (x < 0) {
        x = 0;
      }
      g.drawString(msg, x, y);
      g.setFont(old);
      return y - 23;
    }

    void setEast(boolean value) {
      isEast = value;
    }

    void setErrorMessage(StringGetter msg, Color color) {
      if (errorMessage != msg) {
        errorMessage = msg;
        errorColor = color == null ? DEFAULT_ERROR_COLOR : color;
        paintThread.requestRepaint();
      }
    }

    void setNorth(boolean value) {
      isNorth = value;
    }

    void setNortheast(boolean value) {
      isNortheast = value;
    }

    void setNorthwest(boolean value) {
      isNorthwest = value;
    }

    void setSouth(boolean value) {
      isSouth = value;
    }

    void setSoutheast(boolean value) {
      isSoutheast = value;
    }

    void setSouthwest(boolean value) {
      isSouthwest = value;
    }

    void setWest(boolean value) {
      isWest = value;
    }

    void setWidthMessage(String msg) {
      widthMessage = msg;
    }
  }
}
