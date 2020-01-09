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

package com.cburch.logisim.std.hdl;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.vhdl.base.VhdlSimConstants;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

public class VhdlEntityAttributes extends AbstractAttributeSet {

  public static HdlContentEditor getContentEditor(Window source, HdlContent value, Project proj) {
    synchronized (windowRegistry) {
      HdlContentEditor ret = windowRegistry.get(value);
      if (ret == null) {
        if (source instanceof Frame) ret = new HdlContentEditor((Frame) source, proj, value);
        else ret = new HdlContentEditor((Dialog) source, proj, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }

  private static List<Attribute<?>> attributes =
      Arrays.asList(
          VhdlEntityComponent.CONTENT_ATTR,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          VhdlSimConstants.SIM_NAME_ATTR);

  private static final WeakHashMap<HdlContent, HdlContentEditor> windowRegistry =
      new WeakHashMap<HdlContent, HdlContentEditor>();

  private VhdlContentComponent content;
  private String label = "";
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisable = false;
  private String SimName = "";

  VhdlEntityAttributes() {
    content = VhdlContentComponent.create();
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    VhdlEntityAttributes attr = (VhdlEntityAttributes) dest;
    attr.labelFont = labelFont;
    attr.content = content.clone();
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attributes;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == VhdlEntityComponent.CONTENT_ATTR) {
      return (V) content;
    }
    if (attr == StdAttr.LABEL) {
      return (V) label;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) labelFont;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      return (V) labelVisable;
    }
    if (attr == VhdlSimConstants.SIM_NAME_ATTR) {
      return (V) SimName;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == VhdlEntityComponent.CONTENT_ATTR) {
      VhdlContentComponent newContent = (VhdlContentComponent) value;
      if (!content.equals(newContent)) content = newContent;
      fireAttributeValueChanged(attr, value, null);
    }
    if (attr == StdAttr.LABEL && value instanceof String) {
      String newLabel = (String) value;
      String oldlabel = label;
      if (label.equals(newLabel)) return;
      label = newLabel;
      fireAttributeValueChanged(attr, value, (V) oldlabel);
    }
    if (attr == StdAttr.LABEL_FONT && value instanceof Font) {
      Font newFont = (Font) value;
      if (labelFont.equals(newFont)) return;
      labelFont = newFont;
      fireAttributeValueChanged(attr, value, null);
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      Boolean newvis = (Boolean) value;
      if (labelVisable.equals(newvis)) return;
      labelVisable = newvis;
      fireAttributeValueChanged(attr, value, null);
    }
    if (attr == VhdlSimConstants.SIM_NAME_ATTR) {
      String Name = (String) value;
      if (value.equals(SimName)) return;
      SimName = Name;
      fireAttributeValueChanged(attr, value, null);
    }
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    if (attr == VhdlSimConstants.SIM_NAME_ATTR) {
      return false;
    }
    return true;
  }
}
