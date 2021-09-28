/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
  private final Frame frame;
  private final LogisimToolbarItem itemAdd;
  private final LogisimToolbarItem itemAddVhdl;
  private final LogisimToolbarItem itemUp;
  private final LogisimToolbarItem itemDown;
  private final LogisimToolbarItem itemAppearance;
  private final LogisimToolbarItem itemDelete;
  private final List<ToolbarItem> items;

  public ToolboxToolbarModel(Frame frame, MenuListener menu) {
    this.frame = frame;
    itemAdd =
        new LogisimToolbarItem(
            menu,
            new ProjectAddIcon(false),
            LogisimMenuBar.ADD_CIRCUIT,
            S.getter("projectAddCircuitTip"));
    itemAddVhdl =
        new LogisimToolbarItem(
            menu, new ProjectAddIcon(), LogisimMenuBar.ADD_VHDL, S.getter("projectAddVhdlItem"));
    itemUp =
        new LogisimToolbarItem(
            menu,
            new FatArrowIcon(Direction.NORTH),
            LogisimMenuBar.MOVE_CIRCUIT_UP,
            S.getter("projectMoveCircuitUpTip"));
    itemDown =
        new LogisimToolbarItem(
            menu,
            new FatArrowIcon(Direction.SOUTH),
            LogisimMenuBar.MOVE_CIRCUIT_DOWN,
            S.getter("projectMoveCircuitDownTip"));
    itemAppearance =
        new LogisimToolbarItem(
            menu,
            new AppearEditIcon(),
            LogisimMenuBar.TOGGLE_APPEARANCE,
            S.getter("projectEditAppearanceTip"));
    itemDelete =
        new LogisimToolbarItem(
            menu,
            new ProjectAddIcon(true),
            LogisimMenuBar.REMOVE_CIRCUIT,
            S.getter("projectRemoveCircuitTip"));

    items =
        UnmodifiableList.create(
            new ToolbarItem[] {
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
    return (item == itemAppearance) && frame.getEditorView().equals(Frame.EDIT_APPEARANCE);
  }

  @Override
  public void itemSelected(ToolbarItem item) {
    if (item instanceof LogisimToolbarItem toolbarItem) toolbarItem.doAction();
  }

  @Override
  public void menuEnableChanged(MenuListener source) {
    fireToolbarAppearanceChanged();
  }
}
