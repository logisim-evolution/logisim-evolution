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

package com.cburch.logisim.fpga.library;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;
import java.awt.Graphics;

public class bcd2sevenseg extends InstanceFactory {

  static final int PER_DELAY = 1;
  public static final int Segment_A = 0;
  public static final int Segment_B = 1;
  public static final int Segment_C = 2;
  public static final int Segment_D = 3;
  public static final int Segment_E = 4;
  public static final int Segment_F = 5;
  public static final int Segment_G = 6;
  public static final int BCDin = 7;

  public bcd2sevenseg() {
    super("BCD_to_7_Segment_decoder", S.getter("BCD2SevenSegment"));
    setAttributes(new Attribute[] {StdAttr.DUMMY}, new Object[] {""});
    setOffsetBounds(Bounds.create(-10, -20, 50, 100));
    Port[] ps = new Port[8];
    ps[Segment_A] = new Port(20, 0, Port.OUTPUT, 1);
    ps[Segment_B] = new Port(30, 0, Port.OUTPUT, 1);
    ps[Segment_C] = new Port(20, 60, Port.OUTPUT, 1);
    ps[Segment_D] = new Port(10, 60, Port.OUTPUT, 1);
    ps[Segment_E] = new Port(0, 60, Port.OUTPUT, 1);
    ps[Segment_F] = new Port(10, 0, Port.OUTPUT, 1);
    ps[Segment_G] = new Port(0, 0, Port.OUTPUT, 1);
    ps[BCDin] = new Port(10, 80, Port.INPUT, 4);
    ps[Segment_A].setToolTip(S.getter("Segment_A"));
    ps[Segment_B].setToolTip(S.getter("Segment_B"));
    ps[Segment_C].setToolTip(S.getter("Segment_C"));
    ps[Segment_D].setToolTip(S.getter("Segment_D"));
    ps[Segment_E].setToolTip(S.getter("Segment_E"));
    ps[Segment_F].setToolTip(S.getter("Segment_F"));
    ps[Segment_G].setToolTip(S.getter("Segment_G"));
    ps[BCDin].setToolTip(S.getter("BCDValue"));
    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    Bounds MyBounds = painter.getBounds();
    if (!painter.isPrintView())
      g.setColor(Color.BLUE);
    painter.drawRectangle(MyBounds, "");
    painter.drawPort(BCDin, "BCD", Direction.SOUTH);
    for (int i = 0; i < 7; i++) painter.drawPort(i);
    g.setColor(Color.BLACK);
    painter.drawRectangle(
        MyBounds.getX() + 5,
        MyBounds.getY() + 20,
        MyBounds.getWidth() - 10,
        MyBounds.getHeight() - 40,
        "");
  }

  @Override
  public void propagate(InstanceState state) {
    if (state.getPortValue(BCDin).isFullyDefined()
        & !state.getPortValue(BCDin).isErrorValue()
        & !state.getPortValue(BCDin).isUnknown()) {
      int value = (int)state.getPortValue(BCDin).toLongValue();
      switch (value) {
        case 0:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          break;
        case 1:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          break;
        case 2:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 3:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 4:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 5:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 6:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 7:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          break;
        case 8:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 9:
          state.setPort(Segment_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(Segment_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(Segment_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        default:
          state.setPort(Segment_A, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(Segment_B, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(Segment_C, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(Segment_D, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(Segment_E, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(Segment_F, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(Segment_G, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          break;
      }
    } else {
      for (int i = 0; i < 7; i++)
        state.setPort(i, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
    }
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new bcd2sevensegHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
