/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.bfh;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import java.awt.Color;

public class BinToBcd extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Binary_to_BCD_converter";

  public static final int PER_DELAY = 1;
  private static final int BINin = 0;
  private static final int InnerDistance = 60;

  public static final Attribute<BitWidth> ATTR_BinBits = Attributes.forBitWidth("binvalue", S.getter("BinaryDataBits"), 4, 13);

  public BinToBcd() {
    super(_ID, S.getter("Bin2BCD"), new BinToBcdHdlGeneratorFactory());
    setAttributes(new Attribute[] {BinToBcd.ATTR_BinBits}, new Object[] {BitWidth.create(9)});
    setKeyConfigurator(new BitWidthConfigurator(BinToBcd.ATTR_BinBits, 4, 13, 0));
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var gfx = painter.getGraphics();
    final var nrOfBits = painter.getAttributeValue(BinToBcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(Math.pow(2.0, nrOfBits.getWidth())) + 1.0);

    gfx.setColor(Color.GRAY);
    painter.drawBounds();
    painter.drawPort(BINin, "Bin", Direction.EAST);
    for (var i = nrOfPorts; i > 0; i--)
      painter.drawPort(
          (nrOfPorts - i) + 1,
          Integer.toString((int) Math.pow(10.0, nrOfPorts - i)),
          Direction.NORTH);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == BinToBcd.ATTR_BinBits) {
      instance.recomputeBounds();
      updatePorts(instance);
    }
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(BinToBcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    return Bounds.create((int) (-0.5 * InnerDistance), -20, nrOfPorts * InnerDistance, 40);
  }

  @Override
  public void propagate(InstanceState state) {
    int binValue =
        (state.getPortValue(BINin).isFullyDefined()
                & !state.getPortValue(BINin).isUnknown()
                & !state.getPortValue(BINin).isErrorValue()
            ? (int) state.getPortValue(BINin).toLongValue()
            : -1);
    final var nrOfBits = state.getAttributeValue(BinToBcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(Math.pow(2.0, nrOfBits.getWidth())) + 1.0);
    for (var i = nrOfPorts; i > 0; i--) {
      final var value = (int) (Math.pow(10, i - 1));
      final var number = binValue / value;
      state.setPort(i, Value.createKnown(BitWidth.create(4), number), PER_DELAY);
      binValue -= number * value;
    }
  }

  private void updatePorts(Instance instance) {
    final var nrOfbits = instance.getAttributeValue(BinToBcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfbits.getWidth()) + 1.0);
    final var ps = new Port[nrOfPorts + 1];
    ps[BINin] = new Port((int) (-0.5 * InnerDistance), 0, Port.INPUT, BinToBcd.ATTR_BinBits);
    ps[BINin].setToolTip(S.getter("BinaryInputTip"));
    for (int i = nrOfPorts; i > 0; i--) {
      ps[i] = new Port((nrOfPorts - i) * InnerDistance, -20, Port.OUTPUT, 4);
      final var value = (int) Math.pow(10.0, i - 1);
      ps[i].setToolTip(S.getter(Integer.toString(value)));
    }
    instance.setPorts(ps);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var completeName = new StringBuilder();
    final var nrofbits = attrs.getValue(BinToBcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrofbits.getWidth()) + 1.0);
    completeName.append(CorrectLabel.getCorrectLabel(this.getName()));
    completeName.append("_").append(nrOfPorts).append("_bcd_ports");
    return completeName.toString();
  }
}
