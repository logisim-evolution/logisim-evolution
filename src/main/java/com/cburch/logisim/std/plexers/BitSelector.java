/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.plexers;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.icons.PlexerIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;

public class BitSelector extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "BitSelector";

  public static final Attribute<BitWidth> GROUP_ATTR =
      Attributes.forBitWidth("group", S.getter("bitSelectorGroupAttr"));
  public static final Attribute<Integer> SELECT_ATTR = Attributes.forNoSave();
  public static final Attribute<Integer> EXTENDED_ATTR = Attributes.forNoSave();

  public BitSelector() {
    super(_ID, S.getter("bitSelectorComponent"), new BitSelectorHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {
          StdAttr.FACING, StdAttr.SELECT_LOC, StdAttr.WIDTH, GROUP_ATTR, SELECT_ATTR, EXTENDED_ATTR
        },
        new Object[] {
          Direction.EAST, StdAttr.SELECT_BOTTOM_LEFT, BitWidth.create(8), BitWidth.ONE, 3, 9
        });
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(GROUP_ATTR, 1, Value.MAX_WIDTH, 0),
            new BitWidthConfigurator(StdAttr.WIDTH)));

    setIcon(new PlexerIcon(false, true));
    setFacingAttribute(StdAttr.FACING);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var completeName = new StringBuilder();
    completeName.append(CorrectLabel.getCorrectLabel(this.getName()));
    if (attrs.getValue(GROUP_ATTR).getWidth() > 1) completeName.append("_bus");
    return completeName.toString();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var facing = attrs.getValue(StdAttr.FACING);
    final var base = Bounds.create(-30, -15, 30, 30);
    return base.rotate(Direction.EAST, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updatePorts(instance);
    } else if (attr == StdAttr.WIDTH || attr == GROUP_ATTR || attr == StdAttr.SELECT_LOC) {
      updatePorts(instance);
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    PlexersLibrary.drawTrapezoid(
        painter.getGraphics(), painter.getBounds(), painter.getAttributeValue(StdAttr.FACING), 9);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var facing = painter.getAttributeValue(StdAttr.FACING);
    
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    PlexersLibrary.drawTrapezoid(g, painter.getBounds(), facing, 9);
    final var bds = painter.getBounds();
    GraphicsUtil.drawCenteredText(
        g, "Sel", bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    final var data = state.getPortValue(1);
    final var select = state.getPortValue(2);
    final var groupBits = state.getAttributeValue(GROUP_ATTR);
    Value group;
    if (!select.isFullyDefined()) {
      group = Value.createUnknown(groupBits);
    } else {
      int shift = (int) select.toLongValue() * groupBits.getWidth();
      if (shift >= data.getWidth()) {
        group = Value.createKnown(groupBits, 0);
      } else if (groupBits.getWidth() == 1) {
        group = data.get(shift);
      } else {
        Value[] bits = new Value[groupBits.getWidth()];
        for (int i = 0; i < bits.length; i++) {
          if (shift + i >= data.getWidth()) {
            bits[i] = Value.FALSE;
          } else {
            bits[i] = data.get(shift + i);
          }
        }
        group = Value.create(bits);
      }
    }
    state.setPort(0, group, PlexersLibrary.DELAY);
  }

  private void updatePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    final var selectLoc = instance.getAttributeValue(StdAttr.SELECT_LOC);
    final var data = instance.getAttributeValue(StdAttr.WIDTH);
    final var group = instance.getAttributeValue(GROUP_ATTR);
    var groups = (data.getWidth() + group.getWidth() - 1) / group.getWidth() - 1;
    var selectBits = 1;
    if (groups > 0) {
      while (groups != 1) {
        groups >>= 1;
        selectBits++;
      }
    }
    final var select = BitWidth.create(selectBits);
    instance.getAttributeSet().setValue(SELECT_ATTR, select.getWidth());
    final var maxGroups = (int) Math.pow(2d, select.getWidth());
    instance.getAttributeSet().setValue(EXTENDED_ATTR, maxGroups * group.getWidth() + 1);

    Location inPt;
    Location selPt;
    if (facing == Direction.WEST) {
      inPt = Location.create(30, 0, true);
      if (selectLoc == StdAttr.SELECT_BOTTOM_LEFT) selPt = Location.create(10, -10, true);
      else selPt = Location.create(10, 10, true);
    } else if (facing == Direction.NORTH) {
      inPt = Location.create(0, 30, true);
      if (selectLoc == StdAttr.SELECT_BOTTOM_LEFT) selPt = Location.create(-10, 10, true);
      else selPt = Location.create(10, 10, true);
    } else if (facing == Direction.SOUTH) {
      inPt = Location.create(0, -30, true);
      if (selectLoc == StdAttr.SELECT_BOTTOM_LEFT) selPt = Location.create(-10, -10, true);
      else selPt = Location.create(10, -10, true);
    } else {
      inPt = Location.create(-30, 0, true);
      if (selectLoc == StdAttr.SELECT_BOTTOM_LEFT) selPt = Location.create(-10, 10, true);
      else selPt = Location.create(-10, -10, true);
    }

    final var ps = new Port[3];
    ps[0] = new Port(0, 0, Port.OUTPUT, group.getWidth());
    ps[1] = new Port(inPt.getX(), inPt.getY(), Port.INPUT, data.getWidth());
    ps[2] = new Port(selPt.getX(), selPt.getY(), Port.INPUT, select.getWidth());
    ps[0].setToolTip(S.getter("bitSelectorOutputTip"));
    ps[1].setToolTip(S.getter("bitSelectorDataTip"));
    ps[2].setToolTip(S.getter("bitSelectorSelectTip"));
    instance.setPorts(ps);
  }
}
