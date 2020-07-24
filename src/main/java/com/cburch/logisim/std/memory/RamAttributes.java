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

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class RamAttributes extends AbstractAttributeSet {

  /* here the rest is defined */
  static final AttributeOption VOLATILE = new AttributeOption("volatile",S.getter("ramTypeVolatile"));
  static final AttributeOption NONVOLATILE = new AttributeOption("nonvolatile",S.getter("ramTypeNonVolatile"));
  static final Attribute<AttributeOption> ATTR_TYPE = Attributes.forOption(
         "type", S.getter("ramTypeAttr"), new AttributeOption[] {VOLATILE, NONVOLATILE });
  static final AttributeOption BUS_BIDIR = new AttributeOption("bidir", S.getter("ramBidirDataBus"));
  static final AttributeOption BUS_SEP = new AttributeOption("bibus", S.getter("ramSeparateDataBus"));
  static final Attribute<AttributeOption> ATTR_DBUS = Attributes.forOption(
          "databus", S.getter("ramDataAttr"), new AttributeOption[] {BUS_BIDIR, BUS_SEP});
  static final AttributeOption BUS_WITH_BYTEENABLES = new AttributeOption("byteEnables", S.getter("ramWithByteEnables"));
  static final AttributeOption BUS_WITHOUT_BYTEENABLES = new AttributeOption("NobyteEnables", S.getter("ramNoByteEnables"));
  static final Attribute<AttributeOption> ATTR_ByteEnables = Attributes.forOption(
          "byteenables", S.getter("ramByteEnables"), new AttributeOption[] {BUS_WITH_BYTEENABLES, BUS_WITHOUT_BYTEENABLES});
  static final Attribute<Boolean> CLEAR_PIN = Attributes.forBoolean("clearpin", S.getter("RamClearPin"));
  private ArrayList<Attribute<?>> myAttributes = new ArrayList<Attribute<?>>();


  private BitWidth addrBits = BitWidth.create(8);
  private BitWidth dataBits = BitWidth.create(8);
  private String Label = "";
  private AttributeOption Trigger = StdAttr.TRIG_RISING;
  private AttributeOption BusStyle = BUS_SEP;
  private Font LabelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean LabelVisable = false;
  private AttributeOption ByteEnables = BUS_WITHOUT_BYTEENABLES;
  private Boolean AsynchronousRead = false;
  private AttributeOption Appearance = AppPreferences.getDefaultAppearance();
  private AttributeOption RWBehavior = Mem.READAFTERWRITE;
  private Boolean ClearPin = false;
  private AttributeOption lineSize = Mem.SINGLE;
  private Boolean allowMisaligned = false;
  private AttributeOption typeOfEnables = Mem.USEBYTEENABLES;
  private AttributeOption ramType = VOLATILE;

  RamAttributes() { updateAttributes(); }
  
  public boolean updateAttributes() {
    ArrayList<Attribute<?>>newList = new ArrayList<Attribute<?>>();
    boolean changes = false;
    newList.add(Mem.ADDR_ATTR);
    newList.add(Mem.DATA_ATTR);
    newList.add(Mem.ENABLES_ATTR);
    newList.add(ATTR_TYPE);
    newList.add(CLEAR_PIN);
    if (typeOfEnables.equals(Mem.USEBYTEENABLES)) {
      newList.add(StdAttr.TRIGGER);
      if (Trigger.equals(StdAttr.TRIG_RISING) || Trigger.equals(StdAttr.TRIG_FALLING)) {
        changes |= !myAttributes.contains(Mem.ASYNC_READ);
        newList.add(Mem.ASYNC_READ);
        if (!AsynchronousRead) {
          changes |= !myAttributes.contains(Mem.READ_ATTR);
          newList.add(Mem.READ_ATTR);
        } else changes |= myAttributes.contains(Mem.READ_ATTR);
        if (dataBits.getWidth() > 8) {
          changes |= !myAttributes.contains(ATTR_ByteEnables);
          newList.add(ATTR_ByteEnables);
        } else changes |= myAttributes.contains(ATTR_ByteEnables);
      } else changes |= myAttributes.contains(Mem.ASYNC_READ);
      newList.add(ATTR_DBUS);
      changes |= myAttributes.contains(Mem.LINE_ATTR);
      changes |= myAttributes.contains(Mem.ALLOW_MISALIGNED);
    } else {
      newList.add(Mem.LINE_ATTR);
      newList.add(Mem.ALLOW_MISALIGNED);
      newList.add(StdAttr.TRIGGER);
      newList.add(ATTR_DBUS);
      changes |= !myAttributes.contains(Mem.LINE_ATTR);
      changes |= !myAttributes.contains(Mem.ALLOW_MISALIGNED);
    }
    newList.add(StdAttr.LABEL);
    newList.add(StdAttr.LABEL_FONT);
    newList.add(StdAttr.LABEL_VISIBILITY);
    newList.add(StdAttr.APPEARANCE);
    if (changes) {
      myAttributes.clear();
      myAttributes.addAll(newList);
    }
    return changes;
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    RamAttributes d = (RamAttributes) dest;
    d.addrBits = addrBits;
    d.dataBits = dataBits;
    d.Trigger = Trigger;
    d.BusStyle = BusStyle;
    d.LabelFont = LabelFont;
    d.Appearance = Appearance;
    d.ByteEnables = ByteEnables;
    d.AsynchronousRead = AsynchronousRead;
    d.RWBehavior = RWBehavior;
    d.ClearPin = ClearPin;
    d.lineSize = lineSize;
    d.allowMisaligned = allowMisaligned;
    d.typeOfEnables = typeOfEnables;
    d.ramType = ramType;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return myAttributes;
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
    if (attr == ATTR_TYPE) {
      return (V) ramType;
    }
    if (attr == StdAttr.LABEL) {
      return (V) Label;
    }
    if (attr == StdAttr.TRIGGER) {
      return (V) Trigger;
    }
    if (attr == Mem.ASYNC_READ) {
      return (V) AsynchronousRead;
    }
    if (attr == Mem.READ_ATTR) {
      return (V) RWBehavior;
    }
    if (attr == ATTR_DBUS) {
      return (V) BusStyle;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) LabelFont;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      return (V) LabelVisable;
    }
    if (attr == ATTR_ByteEnables) {
      return (V) ByteEnables;
    }
    if (attr == StdAttr.APPEARANCE) {
      return (V) Appearance;
    }
    if (attr == Mem.LINE_ATTR) {
      return (V) lineSize;
    }
    if (attr == Mem.ALLOW_MISALIGNED) {
      return (V) allowMisaligned;
    }
    if (attr == CLEAR_PIN) {
      return (V) ClearPin;
    }
    if (attr == Mem.ENABLES_ATTR) {
      return (V) typeOfEnables;
    }
    return null;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == Mem.ADDR_ATTR) {
      BitWidth newAddr = (BitWidth) value;
      if (addrBits == newAddr) {
        return;
      }
      addrBits = newAddr;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.DATA_ATTR) {
      BitWidth newData = (BitWidth) value;
      if (dataBits == newData) {
        return;
      }
      dataBits = newData;
      if (typeOfEnables.equals(Mem.USEBYTEENABLES) && updateAttributes())
        fireAttributeListChanged();
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.ENABLES_ATTR) {
      AttributeOption val = (AttributeOption) value;
      if (!typeOfEnables.equals(val)) {
        typeOfEnables = val;
        if (updateAttributes()) fireAttributeListChanged();
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == ATTR_TYPE) {
      AttributeOption val = (AttributeOption) value;
      if (!ramType.equals(val)) {
        ramType = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == StdAttr.LABEL) {
      String NewLabel = (String) value;
      if (Label.equals(NewLabel)) {
        return;
      }
      @SuppressWarnings("unchecked")
      V Oldlabel = (V) Label;
      Label = NewLabel;
      fireAttributeValueChanged(attr, value, Oldlabel);
    } else if (attr == StdAttr.TRIGGER) {
      AttributeOption newTrigger = (AttributeOption) value;
      if (Trigger.equals(newTrigger)) {
        return;
      }
      Trigger = newTrigger;
      if (typeOfEnables.equals(Mem.USEBYTEENABLES) && updateAttributes())
        fireAttributeListChanged();
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.ASYNC_READ) {
      Boolean val = (Boolean) value;
      if (AsynchronousRead != val) {
        AsynchronousRead = val;
        if (updateAttributes()) fireAttributeListChanged();
        fireAttributeValueChanged(attr,value,null);
      }
    } else if (attr == Mem.READ_ATTR) {
      AttributeOption val = (AttributeOption) value;
      if (!RWBehavior.equals(val)) {
        RWBehavior = val;
        fireAttributeValueChanged(attr,value,null);
      }
    } else if (attr == ATTR_DBUS) {
      AttributeOption NewStyle = (AttributeOption) value;
      if (BusStyle.equals(NewStyle)) {
        return;
      }
      BusStyle = NewStyle;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.LABEL_FONT) {
      Font NewFont = (Font) value;
      if (LabelFont.equals(NewFont)) {
        return;
      }
      LabelFont = NewFont;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.LABEL_VISIBILITY) {
      Boolean newVis = (Boolean) value;
      if (LabelVisable.equals(newVis)) return;
      LabelVisable = newVis;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == ATTR_ByteEnables) {
      AttributeOption NewBE = (AttributeOption) value;
      if (ByteEnables.equals(NewBE)) {
        return;
      }
      if (dataBits.getWidth() < 9) {
        NewBE = BUS_WITHOUT_BYTEENABLES;
      }
      ByteEnables = NewBE;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == CLEAR_PIN) {
      Boolean val = (Boolean) value;
      if (ClearPin != val) {
        ClearPin = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == Mem.LINE_ATTR) {
      AttributeOption val = (AttributeOption) value;
      if (!lineSize.equals(val)) {
        lineSize = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == Mem.ALLOW_MISALIGNED) {
      Boolean val = (Boolean) value;
      if (allowMisaligned != val) {
        allowMisaligned = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == StdAttr.APPEARANCE) {
      AttributeOption NewAppearance = (AttributeOption) value;
      if (Appearance.equals(NewAppearance)) return;
      Appearance = NewAppearance;
      fireAttributeValueChanged(attr, value, null);
    }
  }
}
