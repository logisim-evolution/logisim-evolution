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

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.gui.icons.AppearEditIcon;
import com.cburch.logisim.gui.icons.FatArrowIcon;
import com.cburch.logisim.gui.icons.ProjectAddIcon;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.util.UnmodifiableList;
import java.util.List;

class ToolboxToolbarModel extends AbstractToolbarModel implements MenuListener.EnabledListener {
  private Frame frame;
  private LogisimToolbarItem itemAdd;
  private LogisimToolbarItem itemAddVhdl;
  private LogisimToolbarItem itemUp;
  private LogisimToolbarItem itemDown;
  private LogisimToolbarItem itemAppearance;
  private LogisimToolbarItem itemDelete;
  private List<ToolbarItem> items;

  public ToolboxToolbarModel(Frame frame, MenuListener menu) {
    this.frame = frame;
    itemAdd = new LogisimToolbarItem(menu, new ProjectAddIcon(false), LogisimMenuBar.ADD_CIRCUIT, S.getter("projectAddCircuitTip"));
    itemAddVhdl = new LogisimToolbarItem(menu, new ProjectAddIcon(), LogisimMenuBar.ADD_VHDL, S.getter("projectAddVhdlItem"));
    itemUp = new LogisimToolbarItem(menu, new FatArrowIcon(Direction.NORTH), LogisimMenuBar.MOVE_CIRCUIT_UP, S.getter("projectMoveCircuitUpTip"));
    itemDown = new LogisimToolbarItem(menu, new FatArrowIcon(Direction.SOUTH), LogisimMenuBar.MOVE_CIRCUIT_DOWN, S.getter("projectMoveCircuitDownTip"));
    itemAppearance = new LogisimToolbarItem(menu, new AppearEditIcon(),LogisimMenuBar.TOGGLE_APPEARANCE,S.getter("projectEditAppearanceTip"));
    itemDelete = new LogisimToolbarItem(menu, new ProjectAddIcon(true), LogisimMenuBar.REMOVE_CIRCUIT, S.getter("projectRemoveCircuitTip"));

    items = UnmodifiableList.create( new ToolbarItem[] {
              itemAdd, itemAddVhdl, itemUp, itemDown, itemAppearance, itemDelete,
            });

    menu.addEnabledListener(this);
  }

  @Override
  public List<ToolbarItem> getItems() {
    return items;
  }

  @Override
  public boolean isSelected(ToolbarItem item) {
	  return (item == itemAppearance)
		        && frame.getEditorView().equals(Frame.EDIT_APPEARANCE);
  }

  @Override
  public void itemSelected(ToolbarItem item) {
    if (item instanceof LogisimToolbarItem) {
      ((LogisimToolbarItem) item).doAction();
    }
  }

  public void menuEnableChanged(MenuListener source) {
    fireToolbarAppearanceChanged();
  }
}
