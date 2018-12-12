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

package com.cburch.logisim.comp;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Icon;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

public abstract class AbstractComponentFactory implements ComponentFactory {
	private static final Icon toolIcon = Icons.getIcon("subcirc.gif");

	private AttributeSet defaultSet;
	protected HDLGeneratorFactory MyHDLGenerator;
	protected IOComponentInformationContainer MyIOInformation;

	protected AbstractComponentFactory() {
		defaultSet = null;
		MyHDLGenerator = null;
		MyIOInformation = null;
	}

	public boolean ActiveOnHigh(AttributeSet attrs) {
		return true;
	}

	public AttributeSet createAttributeSet() {
		return AttributeSets.EMPTY;
	}

	public abstract Component createComponent(Location loc, AttributeSet attrs);

	//
	// user interface methods
	//
	public void drawGhost(ComponentDrawContext context, Color color, int x,
			int y, AttributeSet attrs) {
		Graphics g = context.getGraphics();
		Bounds bds = getOffsetBounds(attrs);
		g.setColor(color);
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(x + bds.getX(), y + bds.getY(), bds.getWidth(),
				bds.getHeight());
	}

	public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
		AttributeSet dfltSet = defaultSet;
		if (dfltSet == null) {
			dfltSet = (AttributeSet) createAttributeSet().clone();
			defaultSet = dfltSet;
		}
		return dfltSet.getValue(attr);
	}

	public StringGetter getDisplayGetter() {
		return StringUtil.constantGetter(getName());
	}

	public String getDisplayName() {
		return getDisplayGetter().toString();
	}

	public Object getFeature(Object key, AttributeSet attrs) {
		return null;
	}

	public HDLGeneratorFactory getHDLGenerator(String HDLIdentifier,
			AttributeSet attrs) {
		if (HDLSupportedComponent(HDLIdentifier, attrs))
			return MyHDLGenerator;
		else
			return null;
	}

	public String getHDLName(AttributeSet attrs) {
		return CorrectLabel.getCorrectLabel(this.getName());
	}

	public String getHDLTopName(AttributeSet attrs) {
		return getHDLName(attrs);
	}
	
	public boolean CheckForGatedClocks(NetlistComponent comp) {
		return false;
	}
	
	public int[] ClockPinIndex(NetlistComponent comp) {
		return new int[] {0};
	}

	public IOComponentInformationContainer getIOInformation() {
		return MyIOInformation;
	}

	public abstract String getName();

	public abstract Bounds getOffsetBounds(AttributeSet attrs);

	public boolean HasThreeStateDrivers(AttributeSet attrs) {
		return false;
	}

	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		return false;
	}

	public boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver) {
		return false;
	}

	public void paintIcon(ComponentDrawContext context, int x, int y,
			AttributeSet attrs) {
		Graphics g = context.getGraphics();
		if (toolIcon != null) {
			toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
		} else {
			g.setColor(Color.black);
			g.drawRect(x + 5, y + 2, 11, 17);
			Value[] v = { Value.TRUE, Value.FALSE };
			for (int i = 0; i < 3; i++) {
				g.setColor(v[i % 2].getColor());
				g.fillOval(x + 5 - 1, y + 5 + 5 * i - 1, 3, 3);
				g.setColor(v[(i + 1) % 2].getColor());
				g.fillOval(x + 16 - 1, y + 5 + 5 * i - 1, 3, 3);
			}
		}
	}

	public boolean RequiresGlobalClock() {
		return false;
	}

	/* HDL Methods */
	public boolean RequiresNonZeroLabel() {
		return false;
	}

	@Override
	public String toString() {
		return getName();
	}

}
