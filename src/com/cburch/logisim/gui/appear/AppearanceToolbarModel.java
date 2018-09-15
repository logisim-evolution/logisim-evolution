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

package com.cburch.logisim.gui.appear;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.CurveTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.LineTool;
import com.cburch.draw.tools.OvalTool;
import com.cburch.draw.tools.PolyTool;
import com.cburch.draw.tools.RectangleTool;
import com.cburch.draw.tools.RoundRectangleTool;
import com.cburch.draw.tools.TextTool;
import com.cburch.draw.tools.ToolbarToolItem;

class AppearanceToolbarModel extends AbstractToolbarModel implements
		PropertyChangeListener {
	private Canvas canvas;
	private List<ToolbarItem> items;

	public AppearanceToolbarModel(AbstractTool selectTool, Canvas canvas,
			DrawingAttributeSet attrs) {
		this.canvas = canvas;

		AbstractTool[] tools = { selectTool, new TextTool(attrs),
				new LineTool(attrs), new CurveTool(attrs),
				new PolyTool(false, attrs), new RectangleTool(attrs),
				new RoundRectangleTool(attrs), new OvalTool(attrs),
				new PolyTool(true, attrs), };

		ArrayList<ToolbarItem> rawItems = new ArrayList<ToolbarItem>();
		for (AbstractTool tool : tools) {
			rawItems.add(new ToolbarToolItem(tool));
		}
		items = Collections.unmodifiableList(rawItems);
		canvas.addPropertyChangeListener(Canvas.TOOL_PROPERTY, this);
	}

	AbstractTool getFirstTool() {
		ToolbarToolItem item = (ToolbarToolItem) items.get(0);
		return item.getTool();
	}

	@Override
	public List<ToolbarItem> getItems() {
		return items;
	}

	@Override
	public boolean isSelected(ToolbarItem item) {
		if (item instanceof ToolbarToolItem) {
			AbstractTool tool = ((ToolbarToolItem) item).getTool();
			return canvas != null && tool == canvas.getTool();
		} else {
			return false;
		}
	}

	@Override
	public void itemSelected(ToolbarItem item) {
		if (item instanceof ToolbarToolItem) {
			AbstractTool tool = ((ToolbarToolItem) item).getTool();
			canvas.setTool(tool);
			fireToolbarAppearanceChanged();
		}
	}

	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		if (Canvas.TOOL_PROPERTY.equals(prop)) {
			fireToolbarAppearanceChanged();
		}
	}
}
