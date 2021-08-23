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
import lombok.val;

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

  @Override
  public void paintInstance(InstancePainter painter) {
    val gfx = painter.getGraphics();
    val nrOfBits = painter.getAttributeValue(bin2bcd.ATTR_BinBits);
    int nrOfPorts = (int) (Math.log10(Math.pow(2.0, nrOfBits.getWidth())) + 1.0);

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
    if (attr == bin2bcd.ATTR_BinBits) {
      instance.recomputeBounds();
      updatePorts(instance);
    }
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    val nrOfBits = attrs.getValue(bin2bcd.ATTR_BinBits);
    val nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    return Bounds.create((int) (-0.5 * InnerDistance), -20, nrOfPorts * InnerDistance, 40);
  }

  @Override
  public void propagate(InstanceState state) {
    var binValue =
        (state.getPortValue(BINin).isFullyDefined()
                & !state.getPortValue(BINin).isUnknown()
                & !state.getPortValue(BINin).isErrorValue()
            ? (int) state.getPortValue(BINin).toLongValue()
            : -1);
    val nrOfBits = state.getAttributeValue(bin2bcd.ATTR_BinBits);
    val nrOfPorts = (int) (Math.log10(Math.pow(2.0, nrOfBits.getWidth())) + 1.0);
    for (var i = nrOfPorts; i > 0; i--) {
      val value = (int) (Math.pow(10, i - 1));
      val number = binValue / value;
      state.setPort(i, Value.createKnown(BitWidth.create(4), number), PER_DELAY);
      binValue -= number * value;
    }
  }

  private void updatePorts(Instance instance) {
    val nrOfBits = instance.getAttributeValue(bin2bcd.ATTR_BinBits);
    val nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    val ps = new Port[nrOfPorts + 1];
    ps[BINin] = new Port((int) (-0.5 * InnerDistance), 0, Port.INPUT, bin2bcd.ATTR_BinBits);
    ps[BINin].setToolTip(S.getter("BinaryInputTip"));
    for (var i = nrOfPorts; i > 0; i--) {
      ps[i] = new Port((nrOfPorts - i) * InnerDistance, -20, Port.OUTPUT, 4);
      val value = (int) Math.pow(10.0, i - 1);
      ps[i].setToolTip(S.getter(Integer.toString(value)));
    }
    instance.setPorts(ps);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    val completeName = new StringBuilder();
    BitWidth nrofbits = attrs.getValue(bin2bcd.ATTR_BinBits);
    val nrOfPorts = (int) (Math.log10(1 << nrofbits.getWidth()) + 1.0);
    completeName.append(CorrectLabel.getCorrectLabel(this.getName()));
    completeName.append("_").append(nrOfPorts).append("_bcd_ports");
    return completeName.toString();
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new bin2bcdHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }

}
