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
import java.awt.Graphics;

public class bin2bcd extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Binary_to_BCD_converter";

  static final int PER_DELAY = 1;
  private static final int BINin = 0;
  private static final int InnerDistance = 60;

  public static final Attribute<BitWidth> ATTR_BinBits =
      Attributes.forBitWidth("binvalue", S.getter("BinaryDataBits"), 4, 13);

  public bin2bcd() {
    super(_ID, S.getter("Bin2BCD"));
    setAttributes(new Attribute[] {bin2bcd.ATTR_BinBits}, new Object[] {BitWidth.create(9)});
    setKeyConfigurator(new BitWidthConfigurator(bin2bcd.ATTR_BinBits, 4, 13, 0));
  }

  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    BitWidth nrofbits = painter.getAttributeValue(bin2bcd.ATTR_BinBits);
    int NrOfPorts = (int) (Math.log10(Math.pow(2.0, nrofbits.getWidth())) + 1.0);

    g.setColor(Color.GRAY);
    painter.drawBounds();
    painter.drawPort(BINin, "Bin", Direction.EAST);
    for (int i = NrOfPorts; i > 0; i--)
      painter.drawPort(
          (NrOfPorts - i) + 1,
          Integer.toString((int) Math.pow(10.0, NrOfPorts - i)),
          Direction.NORTH);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == bin2bcd.ATTR_BinBits) {
      instance.recomputeBounds();
      updatePorts(instance);
    }
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    BitWidth nrofbits = attrs.getValue(bin2bcd.ATTR_BinBits);
    int NrOfPorts = (int) (Math.log10(1 << nrofbits.getWidth()) + 1.0);
    return Bounds.create((int) (-0.5 * InnerDistance), -20, NrOfPorts * InnerDistance, 40);
  }

  @Override
  public void propagate(InstanceState state) {
    int bin_value =
        (state.getPortValue(BINin).isFullyDefined()
                & !state.getPortValue(BINin).isUnknown()
                & !state.getPortValue(BINin).isErrorValue()
            ? (int) state.getPortValue(BINin).toLongValue()
            : -1);
    BitWidth NrOfBits = state.getAttributeValue(bin2bcd.ATTR_BinBits);
    int NrOfPorts = (int) (Math.log10(Math.pow(2.0, NrOfBits.getWidth())) + 1.0);
    for (int i = NrOfPorts; i > 0; i--) {
      int value = (int) (Math.pow(10, i - 1));
      int number = bin_value / value;
      state.setPort(i, Value.createKnown(BitWidth.create(4), number), PER_DELAY);
      bin_value -= number * value;
    }
  }

  private void updatePorts(Instance instance) {
    BitWidth nrofbits = instance.getAttributeValue(bin2bcd.ATTR_BinBits);
    int NrOfPorts = (int) (Math.log10(1 << nrofbits.getWidth()) + 1.0);
    Port[] ps = new Port[NrOfPorts + 1];
    ps[BINin] = new Port((int) (-0.5 * InnerDistance), 0, Port.INPUT, bin2bcd.ATTR_BinBits);
    ps[BINin].setToolTip(S.getter("BinaryInputTip"));
    for (int i = NrOfPorts; i > 0; i--) {
      ps[i] = new Port((NrOfPorts - i) * InnerDistance, -20, Port.OUTPUT, 4);
      int value = (int) Math.pow(10.0, i - 1);
      ps[i].setToolTip(S.getter(Integer.toString(value)));
    }
    instance.setPorts(ps);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    StringBuilder CompleteName = new StringBuilder();
    BitWidth nrofbits = attrs.getValue(bin2bcd.ATTR_BinBits);
    int NrOfPorts = (int) (Math.log10(1 << nrofbits.getWidth()) + 1.0);
    CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()));
    CompleteName.append("_").append(NrOfPorts).append("_bcd_ports");
    return CompleteName.toString();
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new bin2bcdHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }

}
