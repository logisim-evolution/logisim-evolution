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

package com.cburch.logisim.tools;

import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;

/**
 * This class allows an object to be created holding all the information
 * essential to showing a ComponentFactory in the explorer window, but without
 * actually loading the ComponentFactory unless a program genuinely gets around
 * to needing to use it. Note that for this to work, the relevant
 * ComponentFactory class must be in the same package as its Library class, the
 * ComponentFactory class must be public, and it must include a public
 * no-arguments constructor.
 */
public class FactoryDescription {

	public static List<Tool> getTools(Class<? extends Library> base,
			FactoryDescription[] descriptions) {
		Tool[] tools = new Tool[descriptions.length];
		for (int i = 0; i < tools.length; i++) {
			tools[i] = new AddTool(base, descriptions[i]);
		}
		return Arrays.asList(tools);
	}

	final static Logger logger = LoggerFactory
			.getLogger(FactoryDescription.class);

	private String name;
	private StringGetter displayName;
	private String iconName;
	private boolean iconLoadAttempted;
	private Icon icon;
	private String factoryClassName;
	private boolean factoryLoadAttempted;
	private ComponentFactory factory;
	private StringGetter toolTip;

	public FactoryDescription(String name, StringGetter displayName, Icon icon,
			String factoryClassName) {
		this(name, displayName, factoryClassName);
		this.iconName = "???";
		this.iconLoadAttempted = true;
		this.icon = icon;
	}

	public FactoryDescription(String name, StringGetter displayName,
			String factoryClassName) {
		this.name = name;
		this.displayName = displayName;
		this.iconName = "???";
		this.iconLoadAttempted = true;
		this.icon = null;
		this.factoryClassName = factoryClassName;
		this.factoryLoadAttempted = false;
		this.factory = null;
		this.toolTip = null;
	}

	public FactoryDescription(String name, StringGetter displayName,
			String iconName, String factoryClassName) {
		this(name, displayName, factoryClassName);
		this.iconName = iconName;
		this.iconLoadAttempted = false;
		this.icon = null;
	}

	public String getDisplayName() {
		return displayName.toString();
	}

	public ComponentFactory getFactory(Class<? extends Library> libraryClass) {
		ComponentFactory ret = factory;
		if (factory != null || factoryLoadAttempted) {
			return ret;
		} else {
			String msg = "";
			try {
				msg = "getting class loader";
				ClassLoader loader = libraryClass.getClassLoader();
				msg = "getting package name";
				String name;
				Package pack = libraryClass.getPackage();
				if (pack == null) {
					name = factoryClassName;
				} else {
					name = pack.getName() + "." + factoryClassName;
				}
				msg = "loading class";
				Class<?> factoryClass = loader.loadClass(name);
				msg = "creating instance";
				Object factoryValue = factoryClass.newInstance();
				msg = "converting to factory";
				if (factoryValue instanceof ComponentFactory) {
					ret = (ComponentFactory) factoryValue;
					factory = ret;
					factoryLoadAttempted = true;
					return ret;
				}
			} catch (Exception t) {
				String name = t.getClass().getName();
				String m = t.getMessage();
				if (m != null)
					msg = msg + ": " + name + ": " + m;
				else
					msg = msg + ": " + name;
			}
			logger.error("Error while {}", msg);
			factory = null;
			factoryLoadAttempted = true;
			return null;
		}
	}

	public Icon getIcon() {
		Icon ret = icon;
		if (ret != null || iconLoadAttempted) {
			return ret;
		} else {
			ret = Icons.getIcon(iconName);
			icon = ret;
			iconLoadAttempted = true;
			return ret;
		}
	}

	public String getName() {
		return name;
	}

	public String getToolTip() {
		StringGetter getter = toolTip;
		return getter == null ? null : getter.toString();
	}

	public boolean isFactoryLoaded() {
		return factoryLoadAttempted;
	}

	public FactoryDescription setToolTip(StringGetter getter) {
		toolTip = getter;
		return this;
	}
}
