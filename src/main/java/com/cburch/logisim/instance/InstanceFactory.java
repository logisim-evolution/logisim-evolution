/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.log.Loggable;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a category of components that appear in a circuit. This class and <code>Component
 * </code> share the same sort of relationship as the relation between <em>classes</em> and
 * <em>instances</em> in Java. Normally, there is only one ComponentFactory created for any
 * particular category.
 */
public abstract class InstanceFactory extends AbstractComponentFactory {

  static final Logger logger = LoggerFactory.getLogger(InstanceFactory.class);

  @Getter private final String name;
  @Getter private final StringGetter displayGetter; // displayName
  @Getter @Setter private StringGetter defaultToolTip;
  private String iconName;
  @Getter private Icon icon;
  private Attribute<?>[] attrs;
  private Object[] defaults;
  private AttributeSet defaultSet;
  @Getter @Setter private Bounds offsetBounds;
  @Getter private List<Port> ports;
  @Getter @Setter private Attribute<Direction> facingAttribute;
  @Setter private Boolean shouldSnap;
  @Getter @Setter private KeyConfigurator keyConfigurator;
  @Setter private Class<? extends InstancePoker> instancePoker;
  @Setter private Class<? extends InstanceLogger> instanceLogger;

  public InstanceFactory(String name) {
    this(name, StringUtil.constantGetter(name));
  }

  public InstanceFactory(String name, StringGetter displayNameGetter) {
    this.name = name;
    this.displayGetter = displayNameGetter;
    this.iconName = null;
    this.icon = null;
    this.attrs = null;
    this.defaults = null;
    this.offsetBounds = Bounds.EMPTY_BOUNDS;
    this.ports = Collections.emptyList();
    this.keyConfigurator = null;
    this.facingAttribute = null;
    this.shouldSnap = Boolean.TRUE;
  }

  // event methods
  protected void configureNewInstance(Instance instance) {
    // dummy imlementation
  }

  public boolean contains(Location loc, AttributeSet attrs) {
    val bds = getOffsetBounds(attrs);
    if (bds == null) return false;
    return bds.contains(loc, 1);
  }

  @Override
  public AttributeSet createAttributeSet() {
    val as = attrs;
    return as == null ? AttributeSets.EMPTY : AttributeSets.fixedSet(as, defaults);
  }

  @Override
  public Component createComponent(Location loc, AttributeSet attrs) {
    val ret = new InstanceComponent(this, loc, attrs);
    configureNewInstance(ret.getInstance());
    return ret;
  }

  public final InstanceState createInstanceState(CircuitState state, Component comp) {
    return createInstanceState(state, ((InstanceComponent) comp).getInstance());
  }

  public final InstanceState createInstanceState(CircuitState state, Instance instance) {
    return new InstanceStateImpl(state, instance.getComponent());
  }

  @Override
  public final void drawGhost(ComponentDrawContext context, Color color, int x, int y, AttributeSet attrs) {
    val painter = context.getInstancePainter();
    val gfx = painter.getGraphics();
    gfx.setColor(color);
    gfx.translate(x, y);
    painter.setFactory(this, attrs);
    paintGhost(painter);
    gfx.translate(-x, -y);
    if (painter.getFactory() == null) {
      super.drawGhost(context, color, x, y, attrs);
    }
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    val as = attrs;
    if (as != null) {
      for (var i = 0; i < as.length; i++) {
        if (as[i] == attr) {
          return defaults[i];
        }
      }
      return null;
    } else {
      var dfltSet = defaultSet;
      if (dfltSet == null) {
        dfltSet = createAttributeSet();
        defaultSet = dfltSet;
      }
      return ((AttributeSet) dfltSet.clone()).getValue(attr);
    }
  }

  @Override
  public String getDisplayName() {
    return getDisplayGetter().toString();
  }

  @Override
  public final Object getFeature(Object key, AttributeSet attrs) {
    if (key == FACING_ATTRIBUTE_KEY) return facingAttribute;
    if (key == KeyConfigurator.class) return keyConfigurator;
    if (key == SHOULD_SNAP) return shouldSnap;
    return super.getFeature(key, attrs);
  }

  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == Pokable.class && instancePoker != null)
      return new InstancePokerAdapter(instance.getComponent(), instancePoker);
    if (key == Loggable.class && instanceLogger != null)
      return new InstanceLoggerAdapter(instance.getComponent(), instanceLogger);
    return null;
  }

  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    // no-op implementation
  }

  private boolean isClassOk(Class<?> sub, Class<?> sup) {
    val isSub = sup.isAssignableFrom(sub);
    if (!isSub) {
      logger.error("{} must be a subclass of {}", sub.getName(), sup.getName());
      return false;
    }
    try {
      sub.getConstructor();
    } catch (SecurityException e) {
      logger.error("{} needs its no-args constructor to be public", sub.getName());
    } catch (NoSuchMethodException e) {
      logger.error("{} is missing a no-arguments constructor", sub.getName());
    }
    return true;
  }

  public void paintGhost(InstancePainter painter) {
    painter.setFactory(null, null);
  }

  @Override
  public final void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs) {
    val painter = context.getInstancePainter();
    painter.setFactory(this, attrs);
    val gfx = painter.getGraphics();
    gfx.translate(x, y);
    paintIcon(painter);
    gfx.translate(-x, -y);

    if (painter.getFactory() == null) {
      var i = icon;
      if (i == null) {
        var n = iconName;
        if (n != null) {
          i = Icons.getIcon(n);
          if (i == null) {
            n = null;
          }
        }
      }
      if (i != null) {
        i.paintIcon(context.getDestination(), gfx, x + 2, y + 2);
      } else {
        super.paintIcon(context, x, y, attrs);
      }
    }
  }

  public void paintIcon(InstancePainter painter) {
    painter.setFactory(null, null);
  }

  public abstract void paintInstance(InstancePainter painter);

  public abstract void propagate(InstanceState state);

  public void setAttributes(Attribute<?>[] attrs, Object[] defaults) {
    this.attrs = attrs;
    this.defaults = defaults;
  }

  // FIXME: why is setIcon() touching iconName?
  public void setIcon(Icon value) {
    iconName = "";
    icon = value;
  }

  // FIXME: why is setIconName() nulling icon?
  public void setIconName(String value) {
    iconName = value;
    icon = null;
  }

  public void setPorts(List<Port> ports) {
    this.ports = Collections.unmodifiableList(ports);
  }

  public void setPorts(Port[] ports) {
    this.ports = new UnmodifiableList<>(ports);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    val ret = offsetBounds;
    if (ret == null)
      throw new RuntimeException(
          "offset bounds unknown: use setOffsetBounds() or override getOffsetBounds()");
    return ret;
  }

  public boolean providesSubCircuitMenu() {
    return false;
  }
}
