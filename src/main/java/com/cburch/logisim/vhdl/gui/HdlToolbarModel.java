/*
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

package com.cburch.logisim.vhdl.gui;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.gui.icons.HdlIcon;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.vhdl.base.HdlModel;
import com.cburch.logisim.vhdl.base.HdlModelListener;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;

public class HdlToolbarModel extends AbstractToolbarModel implements HdlModelListener {
  private final HdlContentView editor;
  private final List<ToolbarItem> items;

  final HdlToolbarItem hdlImport;
  final HdlToolbarItem hdlExport;
  final HdlToolbarItem hdlValidate;

  public static final String HDL_IMPORT = "hdlImport";
  public static final String HDL_EXPORT = "hdlEmport";
  public static final String HDL_VALIDATE = "hdlValidate";

  public HdlToolbarModel(Project proj, HdlContentView editor) {
    this.editor = editor;

    final var rawItems = new ArrayList<ToolbarItem>();
    hdlImport = new HdlToolbarItem(new HdlIcon(HDL_IMPORT), HDL_IMPORT, S.getter("hdlOpenButton"));
    hdlExport = new HdlToolbarItem(new HdlIcon(HDL_EXPORT), HDL_EXPORT, S.getter("hdlSaveButton"));
    hdlValidate = new HdlToolbarItem(new HdlIcon(HDL_VALIDATE), HDL_VALIDATE, S.getter("validateButton"));
    rawItems.add(hdlImport);
    rawItems.add(hdlExport);
    rawItems.add(hdlValidate);
    items = Collections.unmodifiableList(rawItems);
  }

  @Override
  public List<ToolbarItem> getItems() {
    return items;
  }

  @Override
  public boolean isSelected(ToolbarItem item) {
    return false;
  }

  @Override
  public void itemSelected(ToolbarItem item) {
    doAction(((HdlToolbarItem) item).action);
  }

  boolean validateEnabled = false;

  void doAction(String action) {
    switch (action) {
      case HDL_IMPORT:
        editor.doImport();
        break;
      case HDL_EXPORT:
        editor.doExport();
        break;
      case HDL_VALIDATE:
        editor.doValidate();
        break;
      default:
        // nothing to do here
        break;
    }
  }

  boolean isEnabled(String action) {
    return (action.equals(HDL_VALIDATE)) ? validateEnabled : true;
  }

  void setDirty(boolean dirty) {
    if (validateEnabled != dirty) {
      validateEnabled = dirty;
      fireToolbarContentsChanged();
    }
  }

  @Override
  public void contentSet(HdlModel source) {
    if (validateEnabled) {
      validateEnabled = false;
      fireToolbarContentsChanged();
    }
  }

  @Override
  public void aboutToSave(HdlModel source) {
    // dummy
  }

  @Override
  public void displayChanged(HdlModel source) {
    // dummy
  }

  @Override
  public void appearanceChanged(HdlModel source) {
    // dummy
  }

  private class HdlToolbarItem implements ToolbarItem {
    final Icon icon;
    final String action;
    final StringGetter toolTip;

    public HdlToolbarItem(Icon icon, String action, StringGetter toolTip) {
      this.icon = icon;
      this.action = action;
      this.toolTip = toolTip;
    }

    @Override
    public Dimension getDimension(Object orientation) {
      var w = 16;
      var h = 16;
      if (icon != null) {
        w = icon.getIconWidth();
        h = icon.getIconHeight() + 2;
      }
      return new Dimension(w, h);
    }

    @Override
    public String getToolTip() {
      return toolTip == null ? null : toolTip.toString();
    }

    @Override
    public boolean isSelectable() {
      return isEnabled(action);
    }

    @Override
    public void paintIcon(Component destination, Graphics gfx) {
      if (!isSelectable() && gfx instanceof Graphics2D) {
        ((Graphics2D) gfx).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
      }

      if (icon == null) {
        gfx.setColor(new Color(255, 128, 128));
        gfx.fillRect(4, 4, 8, 8);
        gfx.setColor(Color.BLACK);
        gfx.drawLine(4, 4, 12, 12);
        gfx.drawLine(4, 12, 12, 4);
        gfx.drawRect(4, 4, 8, 8);
      } else {
        icon.paintIcon(destination, gfx, 0, 1);
      }
    }
  }
}
