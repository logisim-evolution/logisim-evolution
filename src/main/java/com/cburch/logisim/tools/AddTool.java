/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import static com.cburch.logisim.tools.Strings.S;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.gui.main.ToolAttributeAction;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.std.gates.GateKeyboardModifier;
import com.cburch.logisim.std.wiring.ProbeAttributes;
import com.cburch.logisim.tools.key.KeyConfigurationEvent;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.util.AutoLabel;
import com.cburch.logisim.util.SyntaxChecker;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

public class AddTool extends Tool implements Transferable, PropertyChangeListener {
  private class MyAttributeListener implements AttributeListener {
    @Override
    public void attributeListChanged(AttributeEvent e) {
      bounds = null;
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      bounds = null;
    }
  }

  private static final int INVALID_COORD = Integer.MIN_VALUE;
  private static final int SHOW_NONE = 0;
  private static final int SHOW_GHOST = 1;
  private static final int SHOW_ADD = 2;

  private static final int SHOW_ADD_NO = 3;

  private static final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

  private Class<? extends Library> descriptionBase;
  private final FactoryDescription description;
  private boolean sourceLoadAttempted;
  private ComponentFactory factory;
  private final AttributeSet attrs;
  private Bounds bounds;
  private boolean shouldSnap;
  private int lastX = INVALID_COORD;
  private int lastY = INVALID_COORD;
  private int state = SHOW_GHOST;
  private Action lastAddition;
  private boolean keyHandlerTried;
  private boolean matrixPlace = false;
  private KeyConfigurator keyHandler;
  private final AutoLabel autoLabeler = new AutoLabel();

  private AddTool(AddTool base) {
    this.descriptionBase = base.descriptionBase;
    this.description = base.description;
    this.sourceLoadAttempted = base.sourceLoadAttempted;
    this.factory = base.factory;
    this.bounds = base.bounds;
    this.shouldSnap = base.shouldSnap;
    this.attrs = (AttributeSet) base.attrs.clone();
    attrs.addAttributeListener(new MyAttributeListener());
    if (this.attrs.containsAttribute(StdAttr.APPEARANCE)) {
      AppPreferences.DefaultAppearance.addPropertyChangeListener(this);
    }
    if (this.attrs.containsAttribute(ProbeAttributes.PROBEAPPEARANCE)) {
      AppPreferences.NEW_INPUT_OUTPUT_SHAPES.addPropertyChangeListener(this);
    }
  }

  public AddTool(Class<? extends Library> base, FactoryDescription description) {
    this.descriptionBase = base;
    this.description = description;
    this.sourceLoadAttempted = false;
    this.shouldSnap = true;
    this.attrs = new FactoryAttributes(base, description);
    attrs.addAttributeListener(new MyAttributeListener());
    this.keyHandlerTried = false;
    if (this.attrs.containsAttribute(StdAttr.APPEARANCE)) {
      AppPreferences.DefaultAppearance.addPropertyChangeListener(this);
    }
    if (this.attrs.containsAttribute(ProbeAttributes.PROBEAPPEARANCE)) {
      AppPreferences.NEW_INPUT_OUTPUT_SHAPES.addPropertyChangeListener(this);
    }
  }

  public AddTool(ComponentFactory source) {
    this.description = null;
    this.sourceLoadAttempted = true;
    this.factory = source;
    this.bounds = null;
    this.attrs = new FactoryAttributes(source);
    attrs.addAttributeListener(new MyAttributeListener());
    final var value = (Boolean) source.getFeature(ComponentFactory.SHOULD_SNAP, attrs);
    this.shouldSnap = value == null || value;
    if (this.attrs.containsAttribute(StdAttr.APPEARANCE)) {
      AppPreferences.DefaultAppearance.addPropertyChangeListener(this);
    }
    if (this.attrs.containsAttribute(ProbeAttributes.PROBEAPPEARANCE)) {
      AppPreferences.NEW_INPUT_OUTPUT_SHAPES.addPropertyChangeListener(this);
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (AppPreferences.DefaultAppearance.isSource(evt))
      attrs.setValue(StdAttr.APPEARANCE, AppPreferences.getDefaultAppearance());
    else if (AppPreferences.NEW_INPUT_OUTPUT_SHAPES.isSource(evt))
      attrs.setValue(ProbeAttributes.PROBEAPPEARANCE, ProbeAttributes.getDefaultProbeAppearance());
  }

  @Override
  public Tool cloneTool() {
    return new AddTool(this);
  }

  @Override
  public void deselect(Canvas canvas) {
    setState(canvas, SHOW_GHOST);
    moveTo(canvas, canvas.getGraphics(), INVALID_COORD, INVALID_COORD);
    bounds = null;
    lastAddition = null;
    matrixPlace = false;
  }

  private Tool determineNext(Project proj) {
    final var afterAdd = AppPreferences.ADD_AFTER.get();
    if (afterAdd.equals(AppPreferences.ADD_AFTER_UNCHANGED)) {
      return null;
    } else { // switch to Edit Tool
      final var base = proj.getLogisimFile().getLibrary(BaseLibrary._ID);
      if (base == null) {
        return null;
      } else {
        return base.getTool(EditTool._ID);
      }
    }
  }

  @Override
  public void draw(Canvas canvas, ComponentDrawContext context) {
    // next "if" suggested roughly by Kevin Walsh of Cornell to take care of
    // repaint problems on OpenJDK under Ubuntu
    final var x = lastX;
    final var y = lastY;
    if (x == INVALID_COORD || y == INVALID_COORD) return;
    final var source = getFactory();
    if (source == null) return;
    final var base = getBaseAttributes();
    final var bds = source.getOffsetBounds(base);
    Color DrawColor;
    /* take care of coloring the components differently that require a label */
    if (state == SHOW_GHOST) {
      DrawColor = autoLabeler.isActive(canvas.getCircuit()) ? Color.MAGENTA : Color.GRAY;
      source.drawGhost(context, DrawColor, x, y, getBaseAttributes());
      if (matrixPlace) {
        source.drawGhost(context, DrawColor, x + bds.getWidth() + 3, y, getBaseAttributes());
        source.drawGhost(context, DrawColor, x, y + bds.getHeight() + 3, getBaseAttributes());
        source.drawGhost(
            context,
            DrawColor,
            x + bds.getWidth() + 3,
            y + bds.getHeight() + 3,
            getBaseAttributes());
      }
    } else if (state == SHOW_ADD) {
      DrawColor = autoLabeler.isActive(canvas.getCircuit()) ? Color.BLUE : Color.BLACK;
      source.drawGhost(context, DrawColor, x, y, getBaseAttributes());
      if (matrixPlace) {
        source.drawGhost(context, DrawColor, x + bds.getWidth() + 3, y, getBaseAttributes());
        source.drawGhost(context, DrawColor, x, y + bds.getHeight() + 3, getBaseAttributes());
        source.drawGhost(
            context,
            DrawColor,
            x + bds.getWidth() + 3,
            y + bds.getHeight() + 3,
            getBaseAttributes());
      }
    }
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof AddTool)) return false;
    final var o = (AddTool) other;
    if (this.description != null) {
      return this.descriptionBase == o.descriptionBase && this.description.equals(o.description);
    } else {
      return this.factory.equals(o.factory);
    }
  }

  private void expose(java.awt.Component c, int x, int y) {
    final var bds = getBounds();
    c.repaint(x + bds.getX(), y + bds.getY(), bds.getWidth(), bds.getHeight());
  }

  @Override
  public AttributeSet getAttributeSet() {
    return attrs;
  }

  private AttributeSet getBaseAttributes() {
    var ret = attrs;
    if (ret instanceof FactoryAttributes) {
      ret = ((FactoryAttributes) ret).getBase();
    }
    return ret;
  }

  private Bounds getBounds() {
    var ret = bounds;
    if (ret == null) {
      var source = getFactory();
      if (source == null) {
        ret = Bounds.EMPTY_BOUNDS;
      } else {
        final var base = getBaseAttributes();
        final var bds = source.getOffsetBounds(base);
        final var mbds = Bounds.create(bds.getX(), bds.getY(), bds.getWidth() * 2, bds.getHeight() * 2);
        ret = mbds.expand(5);
      }
      bounds = ret;
    }
    return ret;
  }

  @Override
  public Cursor getCursor() {
    return cursor;
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    return getFactory().getDefaultAttributeValue(attr, ver);
  }

  @Override
  public String getDescription() {
    String ret;
    final var desc = description;
    if (desc != null) {
      ret = desc.getToolTip();
    } else {
      final var source = getFactory();
      if (source != null) {
        ret = (String) source.getFeature(ComponentFactory.TOOL_TIP, getAttributeSet());
      } else {
        ret = null;
      }
    }
    if (ret == null) {
      ret = S.get("addToolText", getDisplayName());
    }
    return ret;
  }

  @Override
  public String getDisplayName() {
    final var desc = description;
    return desc == null ? factory.getDisplayName() : desc.getDisplayName();
  }

  public ComponentFactory getFactory() {
    var ret = factory;
    if (ret == null && !sourceLoadAttempted) {
      ret = description.getFactory(descriptionBase);
      if (ret != null) {
        final var base = getBaseAttributes();
        final var value = (Boolean) ret.getFeature(ComponentFactory.SHOULD_SNAP, base);
        shouldSnap = value == null || value;
      }
      factory = ret;
      sourceLoadAttempted = true;
    }
    return ret;
  }

  public ComponentFactory getFactory(boolean forceLoad) {
    return forceLoad ? getFactory() : factory;
  }

  @Override
  public String getName() {
    FactoryDescription desc = description;
    return desc == null ? factory.getName() : desc.getName();
  }

  @Override
  public int hashCode() {
    FactoryDescription desc = description;
    return desc != null ? desc.hashCode() : factory.hashCode();
  }

  @Override
  public boolean isAllDefaultValues(AttributeSet attributeSet, LogisimVersion ver) {
    return attrs == attributeSet
        && attributeSet instanceof FactoryAttributes factAttrs
        && !factAttrs.isFactoryInstantiated();
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent event) {
    processKeyEvent(canvas, event, KeyConfigurationEvent.KEY_PRESSED);

    if (!event.isConsumed() && event.getModifiersEx() == 0) {
      final var keyEventB = event.getKeyCode();
      final var component = getFactory().getDisplayName();
      if (!GateKeyboardModifier.tookKeyboardStrokes(keyEventB, null, attrs, canvas, null, false))
        if (autoLabeler.labelKeyboardHandler(keyEventB,
            getAttributeSet(),
            component,
            null,
            getFactory(),
            canvas.getCircuit(),
            null,
            false)) {
          canvas.repaint();
        } else {
          switch (keyEventB) {
            case KeyEvent.VK_X:
              matrixPlace = !matrixPlace;
              canvas.repaint();
              break;
            case KeyEvent.VK_UP:
              setFacing(canvas, Direction.NORTH);
              break;
            case KeyEvent.VK_DOWN:
              setFacing(canvas, Direction.SOUTH);
              break;
            case KeyEvent.VK_LEFT:
              setFacing(canvas, Direction.WEST);
              break;
            case KeyEvent.VK_RIGHT:
              setFacing(canvas, Direction.EAST);
              break;
            case KeyEvent.VK_R:
              final var current = getFacing();
              if (current == Direction.NORTH) setFacing(canvas, Direction.EAST);
              else if (current == Direction.EAST) setFacing(canvas, Direction.SOUTH);
              else if (current == Direction.SOUTH) setFacing(canvas, Direction.WEST);
              else setFacing(canvas, Direction.NORTH);
              break;
            case KeyEvent.VK_ESCAPE:
              final var proj = canvas.getProject();
              final var base = proj.getLogisimFile().getLibrary(BaseLibrary._ID);
              final var next = (base == null) ? null : base.getTool(EditTool._ID);
              if (next != null) {
                proj.setTool(next);
                final var act = SelectionActions.dropAll(canvas.getSelection());
                if (act != null) {
                  proj.doAction(act);
                }
              }
              break;
            case KeyEvent.VK_BACK_SPACE:
              if (lastAddition != null && canvas.getProject().getLastAction() == lastAddition) {
                canvas.getProject().undoAction();
                lastAddition = null;
              }
          }
        }
    }
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent event) {
    processKeyEvent(canvas, event, KeyConfigurationEvent.KEY_RELEASED);
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent event) {
    processKeyEvent(canvas, event, KeyConfigurationEvent.KEY_TYPED);
  }

  @Override
  public void mouseDragged(Canvas canvas, Graphics gfx, MouseEvent event) {
    if (state != SHOW_NONE) {
      if (shouldSnap) Canvas.snapToGrid(event);
      moveTo(canvas, gfx, event.getX(), event.getY());
    }
  }

  @Override
  public void mouseEntered(Canvas canvas, Graphics gfx, MouseEvent event) {
    if (state == SHOW_GHOST || state == SHOW_NONE) {
      setState(canvas, SHOW_GHOST);
      canvas.requestFocusInWindow();
    } else if (state == SHOW_ADD_NO) {
      setState(canvas, SHOW_ADD);
      canvas.requestFocusInWindow();
    }
  }

  @Override
  public void mouseExited(Canvas canvas, Graphics gfx, MouseEvent event) {
    if (state == SHOW_GHOST) {
      moveTo(canvas, canvas.getGraphics(), INVALID_COORD, INVALID_COORD);
      setState(canvas, SHOW_NONE);
    } else if (state == SHOW_ADD) {
      moveTo(canvas, canvas.getGraphics(), INVALID_COORD, INVALID_COORD);
      setState(canvas, SHOW_ADD_NO);
    }
  }

  @Override
  public void mouseMoved(Canvas canvas, Graphics gfx, MouseEvent event) {
    if (state != SHOW_NONE) {
      if (shouldSnap) Canvas.snapToGrid(event);
      moveTo(canvas, gfx, event.getX(), event.getY());
    }
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    // verify the addition would be valid
    final var circ = canvas.getCircuit();
    if (!canvas.getProject().getLogisimFile().contains(circ)) {
      canvas.setErrorMessage(S.getter("cannotModifyError"));
      return;
    }
    if (factory instanceof SubcircuitFactory circFact) {
      final var depends = canvas.getProject().getDependencies();
      if (!depends.canAdd(circ, circFact.getSubcircuit())) {
        canvas.setErrorMessage(S.getter("circularError"));
        return;
      }
    }

    if (shouldSnap) Canvas.snapToGrid(e);
    moveTo(canvas, g, e.getX(), e.getY());
    setState(canvas, SHOW_ADD);
  }

  @Override
  public void mouseReleased(Canvas canvas, Graphics gfx, MouseEvent event) {
    final var added = new ArrayList<Component>();
    if (state == SHOW_ADD) {
      final var circ = canvas.getCircuit();
      if (!canvas.getProject().getLogisimFile().contains(circ)) return;
      if (shouldSnap) Canvas.snapToGrid(event);
      moveTo(canvas, gfx, event.getX(), event.getY());

      final var source = getFactory();
      if (source == null) return;
      String label = null;
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        label = attrs.getValue(StdAttr.LABEL);
        /* Here we make sure to not overrride labels that have default value */
        if (autoLabeler.isActive(canvas.getCircuit()) && ((label == null) || label.isEmpty())) {
          label = autoLabeler.getCurrent(canvas.getCircuit(), source);
          if (autoLabeler.hasNext(canvas.getCircuit()))
            autoLabeler.getNext(canvas.getCircuit(), source);
          else autoLabeler.stop(canvas.getCircuit());
        }
        if (!autoLabeler.isActive(canvas.getCircuit()))
          autoLabeler.setLabel("", canvas.getCircuit(), source);
      }

      final var matrix = new MatrixPlacerInfo(label);
      if (matrixPlace) {
        final var base = getBaseAttributes();
        final var bds = source.getOffsetBounds(base).expand(5);
        matrix.setBounds(bds);
        final var dialog = new MatrixPlacerDialog(matrix, source.getName(), autoLabeler.isActive(canvas.getCircuit()));
        var okay = false;
        while (!okay) {
          if (!dialog.execute()) return;
          if (SyntaxChecker.isVariableNameAcceptable(matrix.getLabel(), true)) {
            autoLabeler.setLabel(matrix.getLabel(), canvas.getCircuit(), source);
            okay =
                autoLabeler.correctMatrixBaseLabel(
                    canvas.getCircuit(),
                    source,
                    matrix.getLabel(),
                    matrix.getCopiesCountX(),
                    matrix.getCopiesCountY());
            autoLabeler.setLabel(label, canvas.getCircuit(), source);
            if (!okay) {
              OptionPane.showMessageDialog(
                  null,
                  "Base label either has wrong syntax or is contained in circuit",  // FIXME: hardcoded string
                  "Matrixplacer",
                  OptionPane.ERROR_MESSAGE);
              matrix.undoLabel();
            }
          } else matrix.undoLabel();
        }
      }

      try {
        final var mutation = new CircuitMutation(circ);

        for (var x = 0; x < matrix.getCopiesCountX(); x++) {
          for (var y = 0; y < matrix.getCopiesCountY(); y++) {
            final var loc = Location.create(event.getX() + (matrix.getDeltaX() * x),
                event.getY() + (matrix.getDeltaY() * y));
            final var attrsCopy = (AttributeSet) attrs.clone();
            if (matrix.getLabel() != null) {
              if (matrixPlace)
                attrsCopy.setValue(StdAttr.LABEL, autoLabeler.getMatrixLabel(canvas.getCircuit(),
                    source, matrix.getLabel(), x, y));
              else {
                attrsCopy.setValue(StdAttr.LABEL, matrix.getLabel());
              }
            }
            final var comp = source.createComponent(loc, attrsCopy);

            if (circ.hasConflict(comp)) {
              canvas.setErrorMessage(S.getter("exclusiveError"));
              return;
            }

            final var bds = comp.getBounds(gfx);
            if (bds.getX() < 0 || bds.getY() < 0) {
              canvas.setErrorMessage(S.getter("negativeCoordError"));
              return;
            }

            mutation.add(comp);
            added.add(comp);
          }
        }
        final var action = mutation.toAction(S.getter("addComponentAction", factory.getDisplayGetter()));
        canvas.getProject().doAction(action);
        lastAddition = action;
        canvas.repaint();
      } catch (CircuitException ex) {
        OptionPane.showMessageDialog(canvas.getProject().getFrame(), ex.getMessage());
        added.clear();
      }
      setState(canvas, SHOW_GHOST);
      matrixPlace = false;
    } else if (state == SHOW_ADD_NO) {
      setState(canvas, SHOW_NONE);
    }

    final var proj = canvas.getProject();
    final var next = determineNext(proj);
    if (next != null) {
      proj.setTool(next);
      final var act = SelectionActions.dropAll(canvas.getSelection());
      if (act != null) {
        proj.doAction(act);
      }
      if (!added.isEmpty()) canvas.getSelection().addAll(added);
    }
  }

  private synchronized void moveTo(Canvas canvas, Graphics g, int x, int y) {
    if (state != SHOW_NONE) expose(canvas, lastX, lastY);
    lastX = x;
    lastY = y;
    if (state != SHOW_NONE) expose(canvas, lastX, lastY);
  }

  @Override
  public void paintIcon(ComponentDrawContext c, int x, int y) {
    final var desc = description;
    if (desc != null && !desc.isFactoryLoaded()) {
      final var icon = desc.getIcon();
      if (icon != null) {
        icon.paintIcon(c.getDestination(), c.getGraphics(), x + 2, y + 2);
        return;
      }
    }

    ComponentFactory source = getFactory();
    if (source != null) {
      final var base = getBaseAttributes();
      source.paintIcon(c, x, y, base);
    }
  }

  private void processKeyEvent(Canvas canvas, KeyEvent event, int type) {
    var handler = keyHandler;
    if (!keyHandlerTried) {
      final var source = getFactory();
      final var baseAttrs = getBaseAttributes();
      handler = (KeyConfigurator) source.getFeature(KeyConfigurator.class, baseAttrs);
      keyHandler = handler;
      keyHandlerTried = true;
    }

    if (handler != null) {
      final var baseAttrs = getBaseAttributes();
      final var e = new KeyConfigurationEvent(type, baseAttrs, event, this);
      final var r = handler.keyEventReceived(e);
      if (r != null) {
        final var act = ToolAttributeAction.create(r);
        canvas.getProject().doAction(act);
      }
    }
  }

  @Override
  public void select(Canvas canvas) {
    setState(canvas, SHOW_GHOST);
    bounds = null;
  }

  private void setFacing(Canvas canvas, Direction facing) {
    final var source = getFactory();
    if (source == null) return;
    final var base = getBaseAttributes();
    Object feature = source.getFeature(ComponentFactory.FACING_ATTRIBUTE_KEY, base);
    @SuppressWarnings("unchecked")
    Attribute<Direction> attr = (Attribute<Direction>) feature;
    if (attr != null) {
      final var act = ToolAttributeAction.create(this, attr, facing);
      canvas.getProject().doAction(act);
    }
  }

  private Direction getFacing() {
    final var source = getFactory();
    if (source == null) return Direction.NORTH;
    final var base = getBaseAttributes();
    Object feature = source.getFeature(ComponentFactory.FACING_ATTRIBUTE_KEY, base);
    @SuppressWarnings("unchecked")
    Attribute<Direction> attr = (Attribute<Direction>) feature;
    if (attr != null) return base.getValue(attr);
    else return Direction.NORTH;
  }

  private void setState(Canvas canvas, int value) {
    if (value == SHOW_GHOST) {
      if (canvas.getProject().getLogisimFile().contains(canvas.getCircuit())
          && AppPreferences.ADD_SHOW_GHOSTS.getBoolean()) {
        state = SHOW_GHOST;
      } else {
        state = SHOW_NONE;
      }
    } else {
      state = value;
    }
  }

  @Override
  public boolean sharesSource(Tool other) {
    if (!(other instanceof AddTool)) return false;
    final var o = (AddTool) other;
    if (this.sourceLoadAttempted && o.sourceLoadAttempted) {
      return this.factory.equals(o.factory);
    } else if (this.description == null) {
      return o.description == null;
    } else {
      return this.description.equals(o.description);
    }
  }

  public static final DataFlavor dataFlavor;
  static {
    DataFlavor f = null;
    try {
      f = new DataFlavor(
          String.format("%s;class=\"%s\"",
            DataFlavor.javaJVMLocalObjectMimeType,
            AddTool.class.getName()));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    dataFlavor = f;
  }
  public static final DataFlavor[] dataFlavors = new DataFlavor[] { dataFlavor };

  @Override
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
    if (!isDataFlavorSupported(flavor))
      throw new UnsupportedFlavorException(flavor);
    return this;
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    return dataFlavors;
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return dataFlavor.equals(flavor);
  }
}
