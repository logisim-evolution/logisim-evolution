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

package com.cburch.logisim.gui.main;

import java.util.List;

import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.draw.toolbar.ToolbarSeparator;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.util.UnmodifiableList;

class ProjectToolbarModel extends AbstractToolbarModel implements
		MenuListener.EnabledListener {
	private Frame frame;
	private LogisimToolbarItem itemAdd;
	private LogisimToolbarItem itemUp;
	private LogisimToolbarItem itemDown;
	private LogisimToolbarItem itemDelete;
	private LogisimToolbarItem itemLayout;
	private LogisimToolbarItem itemAppearance;
	private List<ToolbarItem> items;

	public ProjectToolbarModel(Frame frame, MenuListener menu) {
		this.frame = frame;

		itemAdd = new LogisimToolbarItem(menu, "projadd.gif",
				LogisimMenuBar.ADD_CIRCUIT,
				Strings.getter("projectAddCircuitTip"));
		itemUp = new LogisimToolbarItem(menu, "projup.gif",
				LogisimMenuBar.MOVE_CIRCUIT_UP,
				Strings.getter("projectMoveCircuitUpTip"));
		itemDown = new LogisimToolbarItem(menu, "projdown.gif",
				LogisimMenuBar.MOVE_CIRCUIT_DOWN,
				Strings.getter("projectMoveCircuitDownTip"));
		itemDelete = new LogisimToolbarItem(menu, "projdel.gif",
				LogisimMenuBar.REMOVE_CIRCUIT,
				Strings.getter("projectRemoveCircuitTip"));
		itemLayout = new LogisimToolbarItem(menu, "projlayo.gif",
				LogisimMenuBar.EDIT_LAYOUT,
				Strings.getter("projectEditLayoutTip"));
		itemAppearance = new LogisimToolbarItem(menu, "projapp.gif",
				LogisimMenuBar.EDIT_APPEARANCE,
				Strings.getter("projectEditAppearanceTip"));

		items = UnmodifiableList.create(new ToolbarItem[] { itemAdd, itemUp,
				itemDown, itemDelete, new ToolbarSeparator(4), itemLayout,
				itemAppearance, });

		menu.addEnabledListener(this);
	}

	@Override
	public List<ToolbarItem> getItems() {
		return items;
	}

	@Override
	public boolean isSelected(ToolbarItem item) {
		String view = frame.getEditorView();
		if (item == itemLayout) {
			return view.equals(Frame.EDIT_LAYOUT);
		} else if (item == itemAppearance) {
			return view.equals(Frame.EDIT_APPEARANCE);
		} else {
			return false;
		}
	}

	@Override
	public void itemSelected(ToolbarItem item) {
		if (item instanceof LogisimToolbarItem) {
			((LogisimToolbarItem) item).doAction();
		}
	}

	//
	// EnabledListener methods
	//
	public void menuEnableChanged(MenuListener source) {
		fireToolbarAppearanceChanged();
	}
}
