/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;

public class Comparator extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Comparator";

  public static final AttributeOption SIGNED_OPTION =
      new AttributeOption("twosComplement", "twosComplement", S.getter("twosComplementOption"));
  public static final AttributeOption UNSIGNED_OPTION =
      new AttributeOption("unsigned", "unsigned", S.getter("unsignedOption"));
  public static final Attribute<AttributeOption> MODE_ATTR =
      Attributes.forOption(
          "mode",
          S.getter("comparatorType"),
          new AttributeOption[] {SIGNED_OPTION, UNSIGNED_OPTION});

  public static final int IN0 = 0;
  public static final int IN1 = 1;
  public static final int GT = 2;
  public static final int EQ = 3;
  public static final int LT = 4;

  public Comparator() {
    super(_ID, S.getter("comparatorComponent"), new ComparatorHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {StdAttr.WIDTH, MODE_ATTR},
        new Object[] {BitWidth.create(8), SIGNED_OPTION});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u2276"));

    Port[] ps = new Port[5];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
    ps[GT] = new Port(0, -10, Port.OUTPUT, 1);
    ps[EQ] = new Port(0, 0, Port.OUTPUT, 1);
    ps[LT] = new Port(0, 10, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("comparatorInputATip"));
    ps[IN1].setToolTip(S.getter("comparatorInputBTip"));
    ps[GT].setToolTip(S.getter("comparatorGreaterTip"));
    ps[EQ].setToolTip(S.getter("comparatorEqualTip"));
    ps[LT].setToolTip(S.getter("comparatorLessTip"));
    setPorts(ps);
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var completeName = new StringBuilder();
    if (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) completeName.append("BitComparator");
    else completeName.append(CorrectLabel.getCorrectLabel(this.getName()));
    return completeName.toString();
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    instance.fireInvalidated();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    painter.drawBounds();
    painter.drawPort(IN0);
    painter.drawPort(IN1);
    painter.drawPort(GT, ">", Direction.WEST);
    painter.drawPort(EQ, "=", Direction.WEST);
    painter.drawPort(LT, "<", Direction.WEST);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);

    // compute outputs
    Value gt = Value.FALSE;
    Value eq = Value.TRUE;
    Value lt = Value.FALSE;

    Value a = state.getPortValue(IN0);
    Value b = state.getPortValue(IN1);
    Value[] ax = a.getAll();
    Value[] bx = b.getAll();
    int maxlen = Math.max(ax.length, bx.length);
    for (int pos = maxlen - 1; pos >= 0; pos--) {
      Value ab = pos < ax.length ? ax[pos] : Value.ERROR;
      Value bb = pos < bx.length ? bx[pos] : Value.ERROR;
      if (pos == ax.length - 1 && ab != bb) {
        Object mode = state.getAttributeValue(MODE_ATTR);
        if (mode != UNSIGNED_OPTION) {
          Value t = ab;
          ab = bb;
          bb = t;
        }
      }

      if (ab == Value.ERROR || bb == Value.ERROR) {
        gt = Value.ERROR;
        eq = Value.ERROR;
        lt = Value.ERROR;
        break;
      } else if (ab == Value.UNKNOWN || bb == Value.UNKNOWN) {
        gt = Value.UNKNOWN;
        eq = Value.UNKNOWN;
        lt = Value.UNKNOWN;
        break;
      } else if (ab != bb) {
        eq = Value.FALSE;
        if (ab == Value.TRUE) gt = Value.TRUE;
        else lt = Value.TRUE;
        break;
      }
    }

    // propagate them
    int delay = (dataWidth.getWidth() + 2) * Adder.PER_DELAY;
    state.setPort(GT, gt, delay);
    state.setPort(EQ, eq, delay);
    state.setPort(LT, lt, delay);
  }
}
