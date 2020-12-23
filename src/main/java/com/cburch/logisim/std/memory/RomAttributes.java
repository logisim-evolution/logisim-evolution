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

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

class RomAttributes extends AbstractAttributeSet {

  static HexFrame getHexFrame(MemContents value, Project proj, Instance instance) {
    synchronized (windowRegistry) {
      HexFrame ret = windowRegistry.get(value);
      if (ret == null) {
        ret = new HexFrame(proj, instance, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }
  
  static void closeHexFrame(MemContents value) {
    HexFrame ret;
    synchronized (windowRegistry) {
      ret = windowRegistry.remove(value);
    }
    if (ret != null) ret.closeAndDispose();
  }

  static void register(MemContents value, Project proj) {
    if (proj == null || listenerRegistry.containsKey(value)) {
      return;
    }
    RomContentsListener l = new RomContentsListener(proj);
    value.addHexModelListener(l);
    listenerRegistry.put(value, l);
  }

  private static List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          new Attribute<?>[] {
            Mem.ADDR_ATTR,
            Mem.DATA_ATTR,
            Mem.LINE_ATTR,
            Mem.ALLOW_MISALIGNED,
            Rom.CONTENTS_ATTR,
            StdAttr.LABEL,
            StdAttr.LABEL_FONT,
            StdAttr.LABEL_VISIBILITY,
            StdAttr.APPEARANCE
          });

  private static WeakHashMap<MemContents, RomContentsListener> listenerRegistry =
      new WeakHashMap<MemContents, RomContentsListener>();

  private static WeakHashMap<MemContents, HexFrame> windowRegistry =
      new WeakHashMap<MemContents, HexFrame>();
  private BitWidth addrBits = BitWidth.create(8);
  private BitWidth dataBits = BitWidth.create(8);
  private MemContents contents;
  private AttributeOption lineSize = Mem.SINGLE;
  private Boolean allowMisaligned = false;
  private String Label = "";
  private Font LabelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean LabelVisable = false;
  private AttributeOption Appearance = AppPreferences.getDefaultAppearance();

  RomAttributes() {
    contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth());
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    RomAttributes d = (RomAttributes) dest;
    d.addrBits = addrBits;
    d.dataBits = dataBits;
    d.lineSize = lineSize;
    d.allowMisaligned = allowMisaligned;
    d.contents = contents.clone();
    d.LabelFont = LabelFont;
    d.LabelVisable = LabelVisable;
    d.Appearance = Appearance;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == Mem.ADDR_ATTR) {
      return (V) addrBits;
    }
    if (attr == Mem.DATA_ATTR) {
      return (V) dataBits;
    }
    if (attr == Mem.LINE_ATTR) {
      return (V) lineSize;
    }
    if (attr == Mem.ALLOW_MISALIGNED) {
      return (V) allowMisaligned;
    }
    if (attr == Rom.CONTENTS_ATTR) {
      return (V) contents;
    }
    if (attr == StdAttr.LABEL) {
      return (V) Label;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) LabelFont;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      return (V) LabelVisable;
    }
    if (attr == StdAttr.APPEARANCE) {
      return (V) Appearance;
    }
    return null;
  }

  void setProject(Project proj) {
    register(contents, proj);
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == Mem.ADDR_ATTR) {
      BitWidth newAddr = (BitWidth) value;
      if (newAddr == addrBits) return;
      addrBits = newAddr;
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.DATA_ATTR) {
      BitWidth newData = (BitWidth) value;
      if (newData == dataBits) return;
      dataBits = newData;
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.LINE_ATTR) {
      AttributeOption val = (AttributeOption) value;
      if (lineSize.equals(val)) return;
      lineSize = val;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.ALLOW_MISALIGNED) {
      Boolean val = (Boolean) value;
      if (allowMisaligned.equals(val)) return;
      allowMisaligned = val;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Rom.CONTENTS_ATTR) {
      MemContents newContents = (MemContents) value;
      if (contents.equals(newContents)) return;
      contents = newContents;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.LABEL) {
      String NewLabel = (String) value;
      if (Label.equals(NewLabel)) return;
      @SuppressWarnings("unchecked")
      V Oldlabel = (V) Label;
      Label = NewLabel;
      fireAttributeValueChanged(attr, value, Oldlabel);
    } else if (attr == StdAttr.LABEL_FONT) {
      Font NewFont = (Font) value;
      if (LabelFont.equals(NewFont)) return;
      LabelFont = NewFont;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.LABEL_VISIBILITY) {
      Boolean newVis = (Boolean) value;
      if (LabelVisable.equals(newVis)) return;
      LabelVisable = newVis;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.APPEARANCE) {
      AttributeOption NewAppearance = (AttributeOption) value;
      if (Appearance.equals(NewAppearance)) return;
      Appearance = NewAppearance;
      fireAttributeValueChanged(attr, value, null);
    }
  }
}
