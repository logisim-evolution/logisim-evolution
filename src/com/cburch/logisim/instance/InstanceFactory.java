/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.instance;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
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
import com.cburch.logisim.std.io.ReptarLocalBus;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UnmodifiableList;

/**
 * Represents a category of components that appear in a circuit. This class and
 * <code>Component</code> share the same sort of relationship as the relation
 * between <em>classes</em> and <em>instances</em> in Java. Normally, there is
 * only one ComponentFactory created for any particular category.
 */
public abstract class InstanceFactory extends AbstractComponentFactory {

	final static Logger logger = LoggerFactory.getLogger(InstanceFactory.class);

	private String name;
	private StringGetter displayName;
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

	public InstanceFactory(String name, StringGetter displayName) {
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
	}

	public boolean contains(Location loc, AttributeSet attrs) {
		Bounds bds = getOffsetBounds(attrs);
		if (bds == null) {
			return false;
		}
		return bds.contains(loc, 1);
	}

	@Override
	public AttributeSet createAttributeSet() {
		Attribute<?>[] as = attrs;
		AttributeSet ret = as == null ? AttributeSets.EMPTY : AttributeSets
				.fixedSet(as, defaults);
		return ret;
	}

	@Override
	public Component createComponent(Location loc, AttributeSet attrs) {
		if (this instanceof ReptarLocalBus) {
			attrs.setReadOnly(StdAttr.LABEL, true);
		}

		InstanceComponent ret = new InstanceComponent(this, loc, attrs);
		configureNewInstance(ret.getInstance());
		return ret;
	}

	public final InstanceState createInstanceState(CircuitState state,
			Component comp) {
		return createInstanceState(state,
				((InstanceComponent) comp).getInstance());
	}

	public final InstanceState createInstanceState(CircuitState state,
			Instance instance) {
		return new InstanceStateImpl(state, instance.getComponent());
	}

	@Override
	public final void drawGhost(ComponentDrawContext context, Color color,
			int x, int y, AttributeSet attrs) {
		InstancePainter painter = context.getInstancePainter();
		Graphics g = painter.getGraphics();
		g.setColor(color);
		g.translate(x, y);
		painter.setFactory(this, attrs);
		paintGhost(painter);
		g.translate(-x, -y);
		if (painter.getFactory() == null) {
			super.drawGhost(context, color, x, y, attrs);
		}
	}

	@Override
	public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
		Attribute<?>[] as = attrs;
		if (as != null) {
			for (int i = 0; i < as.length; i++) {
				if (as[i] == attr) {
					return defaults[i];
				}
			}
			return null;
		} else {
			AttributeSet dfltSet = defaultSet;
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
		} else if (key == Loggable.class && loggerClass != null) {
			return new InstanceLoggerAdapter(instance.getComponent(),
					loggerClass);
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
		Bounds ret = bounds;
		if (ret == null) {
			throw new RuntimeException("offset bounds unknown: "
					+ "use setOffsetBounds or override getOffsetBounds");
		}
		return ret;
	}

	public List<Port> getPorts() {
		return portList;
	}

	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
	}

	private boolean isClassOk(Class<?> sub, Class<?> sup) {
		boolean isSub = sup.isAssignableFrom(sub);
		if (!isSub) {
			logger.error("{}  must be a subclass of {}", sub.getName(),
					sup.getName());
			return false;
		}
		try {
			sub.getConstructor(new Class[0]);
			return true;
		} catch (SecurityException e) {
			logger.error("{} needs its no-args constructor to be public",
					sub.getName());
		} catch (NoSuchMethodException e) {
			logger.error("{} is missing a no-arguments constructor",
					sub.getName());
		}
		return true;
	}

	public void paintGhost(InstancePainter painter) {
		painter.setFactory(null, null);
	}

	@Override
	public final void paintIcon(ComponentDrawContext context, int x, int y,
			AttributeSet attrs) {
		InstancePainter painter = context.getInstancePainter();
		painter.setFactory(this, attrs);
		Graphics g = painter.getGraphics();
		g.translate(x, y);
		paintIcon(painter);
		g.translate(-x, -y);

		if (painter.getFactory() == null) {
			Icon i = icon;
			if (i == null) {
				String n = iconName;
				if (n != null) {
					i = Icons.getIcon(n);
					if (i == null) {
						n = null;
					}
				}
			}
			if (i != null) {
				i.paintIcon(context.getDestination(), g, x + 2, y + 2);
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
		portList = new UnmodifiableList<Port>(ports);
	}

	public void setShouldSnap(boolean value) {
		shouldSnap = Boolean.valueOf(value);
	}
}
