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
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.gui.log.LoggableContract;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.util.IconsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;
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

  private final String name;
  private final StringGetter displayName;
  private StringGetter defaultToolTip;
  private String iconName;
  private Icon icon;
  private Attribute<?>[] attrs;
  private Object[] defaults;
  private AttributeSet defaultSet;
  private Bounds bounds;
  private List<Port> portList;
  private Attribute<Direction> facingAttribute;
  private Boolean shouldSnap;
  private KeyConfigurator keyConfigurator;
  private Class<? extends InstancePoker> pokerClass;
  private Class<? extends InstanceLogger> loggerClass;

  public InstanceFactory(String name) {
    this(name, StringUtil.constantGetter(name));
  }

  public InstanceFactory(String name, HdlGeneratorFactory generator) {
    this(name, StringUtil.constantGetter(name), generator, false, false);
  }

  public InstanceFactory(String name, HdlGeneratorFactory generator, boolean requiresGlobalClock) {
    this(name, StringUtil.constantGetter(name), generator, requiresGlobalClock, false);
  }

  public InstanceFactory(
      String name,
      HdlGeneratorFactory generator,
      boolean requiresLabel,
      boolean requiresGlobalClock) {
    this(
        name, StringUtil.constantGetter(name), generator, requiresGlobalClock, requiresGlobalClock);
  }

  public InstanceFactory(String name, StringGetter displayName) {
    this(name, displayName, null, false, false);
  }

  public InstanceFactory(String name, StringGetter displayName, HdlGeneratorFactory generator) {
    this(name, displayName, generator, false, false);
  }

  public InstanceFactory(
      String name,
      StringGetter displayName,
      HdlGeneratorFactory generator,
      boolean requiresGlobalClock) {
    this(name, displayName, generator, requiresGlobalClock, false);
  }

  public InstanceFactory(
      String name,
      StringGetter displayName,
      HdlGeneratorFactory generator,
      boolean requiresLabel,
      boolean requiresGlobalClock) {
    super(generator, requiresLabel, requiresGlobalClock);
    this.name = name;
    this.displayName = displayName;
    this.iconName = null;
    this.icon = null;
    this.attrs = null;
    this.defaults = null;
    this.bounds = Bounds.EMPTY_BOUNDS;
    this.portList = Collections.emptyList();
    this.keyConfigurator = null;
    this.facingAttribute = null;
    this.shouldSnap = Boolean.TRUE;
  }
  // event methods
  protected void configureNewInstance(Instance instance) {
    // dummy imlementation
  }

  public boolean contains(Location loc, AttributeSet attrs) {
    Bounds bds = getOffsetBounds(attrs);
    if (bds == null) {
      return false;
    }
    return bds.contains(loc, 1);
  }

  public Icon getIcon() {
    return icon;
  }

  @Override
  public AttributeSet createAttributeSet() {
    final var as = attrs;
    return as == null ? AttributeSets.EMPTY : AttributeSets.fixedSet(as, defaults);
  }

  @Override
  public Component createComponent(Location loc, AttributeSet attrs) {
    final var ret = new InstanceComponent(this, loc, attrs);
    configureNewInstance(ret.getInstance());
    return ret;
  }

  @Override
  public final void drawGhost(
      ComponentDrawContext context, Color color, int x, int y, AttributeSet attrs) {
    final var painter = context.getInstancePainter();
    final var gfx = painter.getGraphics();
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
    final var as = attrs;
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

  public StringGetter getDefaultToolTip() {
    return defaultToolTip;
  }

  @Override
  public StringGetter getDisplayGetter() {
    return displayName;
  }

  @Override
  public String getDisplayName() {
    return getDisplayGetter().toString();
  }

  public Attribute<Direction> getFacingAttribute() {
    return facingAttribute;
  }

  @Override
  public final Object getFeature(Object key, AttributeSet attrs) {
    if (key == FACING_ATTRIBUTE_KEY) {
      return facingAttribute;
    }
    if (key == KeyConfigurator.class) {
      return keyConfigurator;
    }
    if (key == SHOULD_SNAP) {
      return shouldSnap;
    }
    return super.getFeature(key, attrs);
  }

  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == Pokable.class && pokerClass != null) {
      return new InstancePokerAdapter(instance.getComponent(), pokerClass);
    } else if (key == LoggableContract.class && loggerClass != null) {
      return new InstanceLoggerAdapter(instance.getComponent(), loggerClass);
    } else {
      return null;
    }
  }

  public KeyConfigurator getKeyConfigurator() {
    return keyConfigurator;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    if (bounds != null) return bounds;
    throw new RuntimeException(
        "offset bounds unknown: use setOffsetBounds() or override getOffsetBounds()");
  }

  public List<Port> getPorts() {
    return portList;
  }

  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    // no-op implementation
  }

  private boolean isClassOk(Class<?> sub, Class<?> sup) {
    final var isSub = sup.isAssignableFrom(sub);
    if (!isSub) {
      logger.error("{}  must be a subclass of {}", sub.getName(), sup.getName());
      return false;
    }
    try {
      sub.getConstructor();
      return true;
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
    final var painter = context.getInstancePainter();
    painter.setFactory(this, attrs);
    final var gfx = painter.getGraphics();
    gfx.translate(x, y);
    gfx.setColor(new Color(AppPreferences.COMPONENT_ICON_COLOR.get()));
    paintIcon(painter);
    gfx.translate(-x, -y);

    if (painter.getFactory() == null) {
      var i = icon;
      if (i == null) {
        var n = iconName;
        if (n != null) {
          i = IconsUtil.getIcon(n);
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

  public void setDefaultToolTip(StringGetter value) {
    defaultToolTip = value;
  }

  public void setFacingAttribute(Attribute<Direction> value) {
    facingAttribute = value;
  }

  public void setIcon(Icon value) {
    iconName = "";
    icon = value;
  }

  public void setIconName(String value) {
    iconName = value;
    icon = null;
  }

  public void setInstanceLogger(Class<? extends InstanceLogger> loggerClass) {
    if (isClassOk(loggerClass, InstanceLogger.class)) {
      this.loggerClass = loggerClass;
    }
  }

  public void setInstancePoker(Class<? extends InstancePoker> pokerClass) {
    if (isClassOk(pokerClass, InstancePoker.class)) {
      this.pokerClass = pokerClass;
    }
  }

  public void setKeyConfigurator(KeyConfigurator value) {
    keyConfigurator = value;
  }

  public void setOffsetBounds(Bounds value) {
    bounds = value;
  }

  public void setPorts(List<Port> ports) {
    portList = Collections.unmodifiableList(ports);
  }

  public void setPorts(Port[] ports) {
    portList = new UnmodifiableList<>(ports);
  }

  public void setShouldSnap(boolean value) {
    shouldSnap = value;
  }

  public boolean providesSubCircuitMenu() {
    return false;
  }
}
