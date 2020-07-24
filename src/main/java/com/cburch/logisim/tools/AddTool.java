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

package com.cburch.logisim.tools;

import static com.cburch.logisim.tools.Strings.S;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
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
import com.cburch.logisim.gui.icons.AnnimatedIcon;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.gui.main.ToolAttributeAction;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Dependencies;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.gates.GateKeyboardModifier;
import com.cburch.logisim.std.wiring.ProbeAttributes;
import com.cburch.logisim.tools.key.KeyConfigurationEvent;
import com.cburch.logisim.tools.key.KeyConfigurationResult;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.util.AutoLabel;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.SyntaxChecker;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.Icon;

public class AddTool extends Tool implements PropertyChangeListener {
  private class MyAttributeListener implements AttributeListener {
    public void attributeListChanged(AttributeEvent e) {
      bounds = null;
    }

    public void attributeValueChanged(AttributeEvent e) {
      bounds = null;
    }
  }

  private static int INVALID_COORD = Integer.MIN_VALUE;
  private static int SHOW_NONE = 0;
  private static int SHOW_GHOST = 1;
  private static int SHOW_ADD = 2;

  private static int SHOW_ADD_NO = 3;

  private static Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

  private Class<? extends Library> descriptionBase;
  private FactoryDescription description;
  private boolean sourceLoadAttempted;
  private ComponentFactory factory;
  private AttributeSet attrs;
  private Bounds bounds;
  private boolean shouldSnap;
  private int lastX = INVALID_COORD;
  private int lastY = INVALID_COORD;
  private int state = SHOW_GHOST;
  private Action lastAddition;
  private boolean keyHandlerTried;
  private boolean MatrixPlace = false;
  private KeyConfigurator keyHandler;
  private AutoLabel AutoLabler = new AutoLabel();

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
    Boolean value = (Boolean) source.getFeature(ComponentFactory.SHOULD_SNAP, attrs);
    this.shouldSnap = value == null ? true : value.booleanValue();
    if (this.attrs.containsAttribute(StdAttr.APPEARANCE)) {
      AppPreferences.DefaultAppearance.addPropertyChangeListener(this);
    }
    if (this.attrs.containsAttribute(ProbeAttributes.PROBEAPPEARANCE)) {
      AppPreferences.NEW_INPUT_OUTPUT_SHAPES.addPropertyChangeListener(this);
    }
  }
  
  @Override
  public void registerParrent(java.awt.Component parrent) {
    ComponentFactory fac = getFactory();
    if (fac != null && fac instanceof InstanceFactory) {
      InstanceFactory f = (InstanceFactory) fac;
      if (f.getIcon() instanceof AnnimatedIcon) {
        AnnimatedIcon i = (AnnimatedIcon) f.getIcon();
        i.registerParrent(parrent);
      }
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (AppPreferences.DefaultAppearance.isSource(evt))
      attrs.setValue(StdAttr.APPEARANCE, AppPreferences.getDefaultAppearance());
    else if (AppPreferences.NEW_INPUT_OUTPUT_SHAPES.isSource(evt))
      attrs.setValue(ProbeAttributes.PROBEAPPEARANCE, ProbeAttributes.GetDefaultProbeAppearance());
  }

  public void cancelOp() {}

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
    MatrixPlace = false;
  }

  private Tool determineNext(Project proj) {
    String afterAdd = AppPreferences.ADD_AFTER.get();
    if (afterAdd.equals(AppPreferences.ADD_AFTER_UNCHANGED)) {
      return null;
    } else { // switch to Edit Tool
      Library base = proj.getLogisimFile().getLibrary("Base");
      if (base == null) {
        return null;
      } else {
        return base.getTool("Edit Tool");
      }
    }
  }

  @Override
  public void draw(Canvas canvas, ComponentDrawContext context) {
    // next "if" suggested roughly by Kevin Walsh of Cornell to take care of
    // repaint problems on OpenJDK under Ubuntu
    int x = lastX;
    int y = lastY;
    if (x == INVALID_COORD || y == INVALID_COORD) return;
    ComponentFactory source = getFactory();
    if (source == null) return;
    AttributeSet base = getBaseAttributes();
    Bounds bds = source.getOffsetBounds(base);
    Color DrawColor;
    /* take care of coloring the components differently that require a label */
    if (state == SHOW_GHOST) {
      DrawColor = AutoLabler.IsActive(canvas.getCircuit()) ? Color.MAGENTA : Color.GRAY;
      source.drawGhost(context, DrawColor, x, y, getBaseAttributes());
      if (MatrixPlace) {
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
      DrawColor = AutoLabler.IsActive(canvas.getCircuit()) ? Color.BLUE : Color.BLACK;
      source.drawGhost(context, DrawColor, x, y, getBaseAttributes());
      if (MatrixPlace) {
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
    AddTool o = (AddTool) other;
    if (this.description != null) {
      return this.descriptionBase == o.descriptionBase && this.description.equals(o.description);
    } else {
      return this.factory.equals(o.factory);
    }
  }

  private void expose(java.awt.Component c, int x, int y) {
    Bounds bds = getBounds();
    c.repaint(x + bds.getX(), y + bds.getY(), bds.getWidth(), bds.getHeight());
  }

  @Override
  public AttributeSet getAttributeSet() {
    return attrs;
  }

  private AttributeSet getBaseAttributes() {
    AttributeSet ret = attrs;
    if (ret instanceof FactoryAttributes) {
      ret = ((FactoryAttributes) ret).getBase();
    }
    return ret;
  }

  private Bounds getBounds() {
    Bounds ret = bounds;
    if (ret == null) {
      ComponentFactory source = getFactory();
      if (source == null) {
        ret = Bounds.EMPTY_BOUNDS;
      } else {
        AttributeSet base = getBaseAttributes();
        Bounds bds = source.getOffsetBounds(base);
        Bounds mbds =
            Bounds.create(bds.getX(), bds.getY(), bds.getWidth() * 2, bds.getHeight() * 2);
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
    FactoryDescription desc = description;
    if (desc != null) {
      ret = desc.getToolTip();
    } else {
      ComponentFactory source = getFactory();
      if (source != null) {
        ret = (String) source.getFeature(ComponentFactory.TOOL_TIP, getAttributeSet());
      } else {
        ret = null;
      }
    }
    if (ret == null) {
      ret = StringUtil.format(S.get("addToolText"), getDisplayName());
    }
    return ret;
  }

  @Override
  public String getDisplayName() {
    FactoryDescription desc = description;
    return desc == null ? factory.getDisplayName() : desc.getDisplayName();
  }

  public ComponentFactory getFactory() {
    ComponentFactory ret = factory;
    if (ret != null || sourceLoadAttempted) {
      return ret;
    } else {
      ret = description.getFactory(descriptionBase);
      if (ret != null) {
        AttributeSet base = getBaseAttributes();
        Boolean value = (Boolean) ret.getFeature(ComponentFactory.SHOULD_SNAP, base);
        shouldSnap = value == null ? true : value.booleanValue();
      }
      factory = ret;
      sourceLoadAttempted = true;
      return ret;
    }
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
  public boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver) {
    return this.attrs == attrs
        && attrs instanceof FactoryAttributes
        && !((FactoryAttributes) attrs).isFactoryInstantiated();
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent event) {
    processKeyEvent(canvas, event, KeyConfigurationEvent.KEY_PRESSED);

    if (!event.isConsumed() && event.getModifiersEx() == 0) {
      int KeybEvent = event.getKeyCode();
      String Component = getFactory().getDisplayName();
      if (!GateKeyboardModifier.TookKeyboardStrokes(KeybEvent, null, attrs, canvas, null, false))
        if (AutoLabler.LabelKeyboardHandler(
            KeybEvent,
            getAttributeSet(),
            Component,
            null,
            getFactory(),
            canvas.getCircuit(),
            null,
            false)) {
          canvas.repaint();
        } else
          switch (KeybEvent) {
            case KeyEvent.VK_X:
              MatrixPlace = !MatrixPlace;
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
              Direction current = getFacing();
              if (current == Direction.NORTH) setFacing(canvas, Direction.EAST);
              else if (current == Direction.EAST) setFacing(canvas, Direction.SOUTH);
              else if (current == Direction.SOUTH) setFacing(canvas, Direction.WEST);
              else setFacing(canvas, Direction.NORTH);
              break;
            case KeyEvent.VK_ESCAPE:
              Project proj = canvas.getProject();
              Library base = proj.getLogisimFile().getLibrary("Base");
              Tool next = (base == null) ? null : base.getTool("Edit Tool");
              if (next != null) {
                proj.setTool(next);
                Action act = SelectionActions.dropAll(canvas.getSelection());
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

  @Override
  public void keyReleased(Canvas canvas, KeyEvent event) {
    processKeyEvent(canvas, event, KeyConfigurationEvent.KEY_RELEASED);
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent event) {
    processKeyEvent(canvas, event, KeyConfigurationEvent.KEY_TYPED);
  }

  @Override
  public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
    if (state != SHOW_NONE) {
      if (shouldSnap) Canvas.snapToGrid(e);
      moveTo(canvas, g, e.getX(), e.getY());
    }
  }

  @Override
  public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) {
    if (state == SHOW_GHOST || state == SHOW_NONE) {
      setState(canvas, SHOW_GHOST);
      canvas.requestFocusInWindow();
    } else if (state == SHOW_ADD_NO) {
      setState(canvas, SHOW_ADD);
      canvas.requestFocusInWindow();
    }
  }

  @Override
  public void mouseExited(Canvas canvas, Graphics g, MouseEvent e) {
    if (state == SHOW_GHOST) {
      moveTo(canvas, canvas.getGraphics(), INVALID_COORD, INVALID_COORD);
      setState(canvas, SHOW_NONE);
    } else if (state == SHOW_ADD) {
      moveTo(canvas, canvas.getGraphics(), INVALID_COORD, INVALID_COORD);
      setState(canvas, SHOW_ADD_NO);
    }
  }

  @Override
  public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) {
    if (state != SHOW_NONE) {
      if (shouldSnap) Canvas.snapToGrid(e);
      moveTo(canvas, g, e.getX(), e.getY());
    }
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    // verify the addition would be valid
    Circuit circ = canvas.getCircuit();
    if (!canvas.getProject().getLogisimFile().contains(circ)) {
      canvas.setErrorMessage(S.getter("cannotModifyError"));
      return;
    }
    if (factory instanceof SubcircuitFactory) {
      SubcircuitFactory circFact = (SubcircuitFactory) factory;
      Dependencies depends = canvas.getProject().getDependencies();
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
  public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
    ArrayList<Component> added = new ArrayList<Component>();
    if (state == SHOW_ADD) {
      Circuit circ = canvas.getCircuit();
      if (!canvas.getProject().getLogisimFile().contains(circ)) return;
      if (shouldSnap) Canvas.snapToGrid(e);
      moveTo(canvas, g, e.getX(), e.getY());

      ComponentFactory source = getFactory();
      if (source == null) return;
      String Label = null;
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        Label = attrs.getValue(StdAttr.LABEL);
        /* Here we make sure to not overrride labels that have default value */
        if (AutoLabler.IsActive(canvas.getCircuit()) & ((Label == null) | Label.isEmpty())) {
          Label = AutoLabler.GetCurrent(canvas.getCircuit(), source);
          if (AutoLabler.hasNext(canvas.getCircuit()))
            AutoLabler.GetNext(canvas.getCircuit(), source);
          else AutoLabler.Stop(canvas.getCircuit());
        }
        if (!AutoLabler.IsActive(canvas.getCircuit()))
          AutoLabler.SetLabel("", canvas.getCircuit(), source);
      }

      MatrixPlacerInfo matrix = new MatrixPlacerInfo(Label);
      if (MatrixPlace) {
        AttributeSet base = getBaseAttributes();
        Bounds bds = source.getOffsetBounds(base).expand(5);
        matrix.SetBounds(bds);
        MatrixPlacerDialog diag =
            new MatrixPlacerDialog(
                matrix, source.getName(), AutoLabler.IsActive(canvas.getCircuit()));
        boolean okay = false;
        while (!okay) {
          if (!diag.execute()) return;
          if (SyntaxChecker.isVariableNameAcceptable(matrix.GetLabel(), true)) {
            AutoLabler.SetLabel(matrix.GetLabel(), canvas.getCircuit(), source);
            okay =
                AutoLabler.CorrectMatrixBaseLabel(
                    canvas.getCircuit(),
                    source,
                    matrix.GetLabel(),
                    matrix.getNrOfXCopies(),
                    matrix.getNrOfYCopies());
            AutoLabler.SetLabel(Label, canvas.getCircuit(), source);
            if (!okay) {
              OptionPane.showMessageDialog(
                  null,
                  "Base label either has wrong syntax or is contained in circuit",
                  "Matrixplacer",
                  OptionPane.ERROR_MESSAGE);
              matrix.UndoLabel();
            }
          } else matrix.UndoLabel();
        }
      }

      try {
        CircuitMutation mutation = new CircuitMutation(circ);

        for (int x = 0; x < matrix.getNrOfXCopies(); x++) {
          for (int y = 0; y < matrix.getNrOfYCopies(); y++) {
            Location loc =
                Location.create(
                    e.getX() + matrix.GetDeltaX() * x, e.getY() + matrix.GetDeltaY() * y);
            AttributeSet attrsCopy = (AttributeSet) attrs.clone();
            if (matrix.GetLabel() != null) {
              if (MatrixPlace)
                attrsCopy.setValue(
                    StdAttr.LABEL,
                    AutoLabler.GetMatrixLabel(
                        canvas.getCircuit(), source, matrix.GetLabel(), x, y));
              else {
                attrsCopy.setValue(StdAttr.LABEL, matrix.GetLabel());
              }
            }
            Component c = source.createComponent(loc, attrsCopy);

            if (circ.hasConflict(c)) {
              canvas.setErrorMessage(S.getter("exclusiveError"));
              return;
            }

            Bounds bds = c.getBounds(g);
            if (bds.getX() < 0 || bds.getY() < 0) {
              canvas.setErrorMessage(S.getter("negativeCoordError"));
              return;
            }

            mutation.add(c);
            added.add(c);
          }
        }
        Action action =
            mutation.toAction(S.getter("addComponentAction", factory.getDisplayGetter()));
        canvas.getProject().doAction(action);
        lastAddition = action;
        canvas.repaint();
      } catch (CircuitException ex) {
        OptionPane.showMessageDialog(canvas.getProject().getFrame(), ex.getMessage());
        added.clear();
      }
      setState(canvas, SHOW_GHOST);
      MatrixPlace = false;
    } else if (state == SHOW_ADD_NO) {
      setState(canvas, SHOW_NONE);
    }

    Project proj = canvas.getProject();
    Tool next = determineNext(proj);
    if (next != null) {
      proj.setTool(next);
      Action act = SelectionActions.dropAll(canvas.getSelection());
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
    FactoryDescription desc = description;
    if (desc != null && !desc.isFactoryLoaded()) {
      Icon icon = desc.getIcon();
      if (icon != null) {
        icon.paintIcon(c.getDestination(), c.getGraphics(), x + 2, y + 2);
        return;
      }
    }

    ComponentFactory source = getFactory();
    if (source != null) {
      AttributeSet base = getBaseAttributes();
      source.paintIcon(c, x, y, base);
    }
  }

  private void processKeyEvent(Canvas canvas, KeyEvent event, int type) {
    KeyConfigurator handler = keyHandler;
    if (!keyHandlerTried) {
      ComponentFactory source = getFactory();
      AttributeSet baseAttrs = getBaseAttributes();
      handler = (KeyConfigurator) source.getFeature(KeyConfigurator.class, baseAttrs);
      keyHandler = handler;
      keyHandlerTried = true;
    }

    if (handler != null) {
      AttributeSet baseAttrs = getBaseAttributes();
      KeyConfigurationEvent e = new KeyConfigurationEvent(type, baseAttrs, event, this);
      KeyConfigurationResult r = handler.keyEventReceived(e);
      if (r != null) {
        Action act = ToolAttributeAction.create(r);
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
    ComponentFactory source = getFactory();
    if (source == null) return;
    AttributeSet base = getBaseAttributes();
    Object feature = source.getFeature(ComponentFactory.FACING_ATTRIBUTE_KEY, base);
    @SuppressWarnings("unchecked")
    Attribute<Direction> attr = (Attribute<Direction>) feature;
    if (attr != null) {
      Action act = ToolAttributeAction.create(this, attr, facing);
      canvas.getProject().doAction(act);
    }
  }

  private Direction getFacing() {
    ComponentFactory source = getFactory();
    if (source == null) return Direction.NORTH;
    AttributeSet base = getBaseAttributes();
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
    AddTool o = (AddTool) other;
    if (this.sourceLoadAttempted && o.sourceLoadAttempted) {
      return this.factory.equals(o.factory);
    } else if (this.description == null) {
      return o.description == null;
    } else {
      return this.description.equals(o.description);
    }
  }
}
