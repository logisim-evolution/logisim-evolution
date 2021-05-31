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

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.plexers.Plexers;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

public class DisplayDecoder extends InstanceFactory {

  private static final Attribute<Boolean> MULTI_BIT =
      Attributes.forBoolean("multibit", S.getter("ioMultiBit"));

  public static void ComputeDisplayDecoderOutputs(
      InstanceState state,
      int inputvalue,
      int aPortIndex,
      int bPortIndex,
      int cPortIndex,
      int dPortIndex,
      int ePortIndex,
      int fPortIndex,
      int gPortIndex,
      int LTPortIndex,
      int BIPortIndex,
      int RBIPortIndex) {
    if (state.getPortValue(BIPortIndex) == Value.FALSE) inputvalue = 16;
    else if (state.getPortValue(LTPortIndex) == Value.FALSE) inputvalue = 8;
    else if (state.getPortValue(RBIPortIndex) == Value.FALSE && inputvalue == 0) inputvalue = 16;
    switch (inputvalue) {
      case 0:
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.FALSE, Plexers.DELAY);
        break;
      case 1:
        state.setPort(aPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.FALSE, Plexers.DELAY);
        break;
      case 2:
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 3:
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 4:
        state.setPort(aPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 5:
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 6:
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 7:
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.FALSE, Plexers.DELAY);
        break;
      case 8:
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 9:
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 10: // a
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 11: // b
        state.setPort(aPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 12: // c
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.FALSE, Plexers.DELAY);
        break;
      case 13: // d
        state.setPort(aPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 14: // e
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 15: // f
        state.setPort(aPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.TRUE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.TRUE, Plexers.DELAY);
        break;
      case 16: // off
        state.setPort(aPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(bPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(cPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(dPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(ePortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(fPortIndex, Value.FALSE, Plexers.DELAY);
        state.setPort(gPortIndex, Value.FALSE, Plexers.DELAY);
        break;
      default:
        state.setPort(aPortIndex, Value.UNKNOWN, Plexers.DELAY);
        state.setPort(bPortIndex, Value.UNKNOWN, Plexers.DELAY);
        state.setPort(cPortIndex, Value.UNKNOWN, Plexers.DELAY);
        state.setPort(dPortIndex, Value.UNKNOWN, Plexers.DELAY);
        state.setPort(ePortIndex, Value.UNKNOWN, Plexers.DELAY);
        state.setPort(fPortIndex, Value.UNKNOWN, Plexers.DELAY);
        state.setPort(gPortIndex, Value.UNKNOWN, Plexers.DELAY);
        break;
    }
  }

  public static byte getdecval(
      InstanceState state,
      boolean multibit,
      int MultibitInputIndex,
      int Aindex,
      int Bindex,
      int Cindex,
      int Dindex) {
    byte decval = -1, powval = 0;
    int[] inputindex = {Aindex, Bindex, Cindex, Dindex};
    if (!multibit
        && state.getPortValue(Aindex) != Value.UNKNOWN
        && state.getPortValue(Bindex) != Value.UNKNOWN
        && state.getPortValue(Cindex) != Value.UNKNOWN
        && state.getPortValue(Dindex) != Value.UNKNOWN) {
      for (byte i = 0; i < 4; i++)
        if (state.getPortValue(inputindex[i]) == Value.TRUE) // if true input
          // for example 1101 --> 8+4+1= 13(decimal)
          powval |= 1 << i;
      decval += (byte) (powval + 1);
    } else if (multibit && state.getPortValue(MultibitInputIndex) != Value.UNKNOWN)
      decval = (byte) state.getPortValue(MultibitInputIndex).toLongValue();
    return decval;
  }

  public DisplayDecoder() {
    super("DisplayDecoder", S.getter("DisplayDecoderComponent"));
    setAttributes(
        new Attribute[] {StdAttr.FACING, MULTI_BIT}, new Object[] {Direction.EAST, Boolean.TRUE});
    setFacingAttribute(StdAttr.FACING);
    setIconName("displaydecoder.gif");
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction dir = attrs.getValue(StdAttr.FACING);
    int len = 80; // lenght
    int offs = -len / 2; // to get y=0 in middle height
    if (dir == Direction.NORTH) {
      return Bounds.create(offs, 0, len, 40);
    } else if (dir == Direction.SOUTH) {
      return Bounds.create(offs, -40, len, 40);
    } else if (dir == Direction.WEST) {
      return Bounds.create(0, offs, 40, len);
    } else { // dir == Direction.EAST
      return Bounds.create(-40, offs, 40, len);
    }
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    instance.recomputeBounds();
    updatePorts(instance);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Direction dir = painter.getAttributeValue(StdAttr.FACING);
    Graphics g = painter.getGraphics();
    painter.drawBounds();
    Bounds bds = painter.getBounds();
    byte nports = (byte) (11 + (painter.getAttributeValue(MULTI_BIT) ? 1 : 4));
    boolean multibit = painter.getAttributeValue(MULTI_BIT);
    String text =
        (painter.getPortValue(7) == Value.FALSE)
            ? "!" + S.get("memEnableLabel")
            : painter.getPortValue(nports - 2) == Value.FALSE
                ? "BI"
                : painter.getPortValue(nports - 3) == Value.FALSE
                    ? "LI"
                    : painter.getPortValue(nports - 1) == Value.FALSE
                            && getdecval(painter, multibit, 8, 8, 9, 10, 11) == 0
                        ? "RBI"
                        : (getdecval(painter, multibit, 8, 8, 9, 10, 11) != -1)
                            ? Integer.toString(getdecval(painter, multibit, 8, 8, 9, 10, 11))
                            : "-";
    GraphicsUtil.drawCenteredText(
        g, text, bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2);
    for (byte i = 0; i < nports - 3; i++) {
      if (i != 7) painter.drawPort(i);
    }
    g.setColor(Color.GRAY);
    painter.drawPort(
        7,
        S.get("memEnableLabel"),
        (dir == Direction.NORTH || dir == Direction.SOUTH) ? Direction.EAST : Direction.NORTH);
    if (dir == Direction.NORTH
        || dir == Direction.SOUTH) { // write the port name only if horizontal to not overlap
      painter.drawPort(nports - 3, S.get("LT"), Direction.WEST);
      painter.drawPort(nports - 2, S.get("BI"), Direction.WEST);
      painter.drawPort(nports - 1, S.get("RBI"), Direction.WEST);
    } else {
      painter.drawPort(nports - 3);
      painter.drawPort(nports - 2);
      painter.drawPort(nports - 1);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    boolean multibit = state.getAttributeValue(MULTI_BIT);
    byte nports = (byte) (11 + (state.getAttributeValue(MULTI_BIT) ? 1 : 4));
    if (state.getPortValue(7) != Value.FALSE) { // enabled
      ComputeDisplayDecoderOutputs(
          state,
          getdecval(state, multibit, 8, 8, 9, 10, 11),
          0,
          1,
          2,
          3,
          4,
          5,
          6,
          nports - 3,
          nports - 2,
          nports - 1);
    }
  }

  private void updatePorts(Instance instance) {
    Direction dir = instance.getAttributeValue(StdAttr.FACING);
    boolean multibit = instance.getAttributeValue(MULTI_BIT) == Boolean.TRUE;
    byte in = (byte) (multibit ? 1 : 4); // number of input ports
    byte out = 7; // number of output ports
    char cin = 65; // Letter A (to D in for)
    char cout = 97; // Letter a (to g in for)
    Port[] ps = new Port[in + out + 4];
    if (dir == Direction.NORTH || dir == Direction.SOUTH) { // horizzontal
      byte y = (byte) (dir == Direction.NORTH ? 40 : -40);
      if (!multibit) {
        for (byte i = 8; i < in + 8; i++) { // inputs
          // total lenght should be 80(10-A-20-B-20-C-20-D-10)
          ps[i] = new Port(20 * (i - 8) - 30, y, Port.INPUT, 1);
          ps[i].setToolTip(S.getter("DisplayDecoderInTip", "" + cin));
          cin++;
        }
      } else {
        ps[8] = new Port(0, y, Port.INPUT, 4);
        ps[8].setToolTip(S.getter("DisplayDecoderInTip", "" + cin));
      }
      for (byte i = 0; i < out; i++) { // outputs
        // total lenght should be 80(10-A-20-B-20-C-20-D-10)
        ps[i] = new Port(10 * i - 30, 0, Port.OUTPUT, 1);
        ps[i].setToolTip(S.getter("DisplayDecoderOutTip", "" + cout));
        cout++;
      }
      ps[out] = new Port(-40, y / 2, Port.INPUT, 1); // enable input
      ps[ps.length - 3] =
          new Port(40, y + (dir == Direction.NORTH ? -10 : 10), Port.INPUT, 1); // Lamp Test
      ps[ps.length - 2] =
          new Port(40, y + (dir == Direction.NORTH ? -20 : 20), Port.INPUT, 1); // Blanking Input
      ps[ps.length - 1] =
          new Port(40, y + (dir == Direction.NORTH ? -30 : 30), Port.INPUT, 1); // Ripple Blanking
      // Input
    } else { // vertical
      int x = dir == Direction.EAST ? -40 : 40;
      if (!multibit) {
        for (byte i = 8; i < in + 8; i++) { // inputs
          ps[i] = new Port(x, 20 * (i - 8) - 30, Port.INPUT, 1);
          ps[i].setToolTip(S.getter("DisplayDecoderInTip", "" + cin));
          cin++;
        }
      } else {
        ps[8] = new Port(x, 0, Port.INPUT, 4);
        ps[8].setToolTip(S.getter("DisplayDecoderInTip", "" + cin));
      }
      for (byte i = 0; i < out; i++) { // outputs
        ps[i] = new Port(0, 10 * i - 30, Port.OUTPUT, 1);
        ps[i].setToolTip(S.getter("DisplayDecoderOutTip", "" + cout));
        cout++;
      }
      ps[out] = new Port(x / 2, -40, Port.INPUT, 1); // enable input
      ps[ps.length - 3] =
          new Port(x + (dir == Direction.EAST ? 10 : -10), 40, Port.INPUT, 1); // Lamp Test
      ps[ps.length - 2] =
          new Port(x + (dir == Direction.EAST ? 20 : -20), 40, Port.INPUT, 1); // Blanking Input
      ps[ps.length - 1] =
          new Port(x + (dir == Direction.EAST ? 30 : -30), 40, Port.INPUT, 1); // Ripple Blanking
      // Input
    }
    ps[out].setToolTip(S.getter("priorityEncoderEnableInTip"));
    ps[ps.length - 3].setToolTip(S.getter("LampTestInTip"));
    ps[ps.length - 2].setToolTip(S.getter("BlankingInputInTip"));
    ps[ps.length - 1].setToolTip(S.getter("RippleBlankingInputInTip"));
    instance.setPorts(ps);
  }
}
