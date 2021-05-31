/**
 * This file is part of logisim-evolution.
 * <p>
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * <p>
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 * + College of the Holy Cross
 * http://www.holycross.edu
 * + Haute École Spécialisée Bernoise/Berner Fachhochschule
 * http://www.bfh.ch
 * + Haute École du paysage, d'ingénierie et d'architecture de Genève
 * http://hepia.hesge.ch/
 * + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 * http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;
import java.awt.event.MouseEvent;

import static com.cburch.logisim.data.Value.FALSE_COLOR;
import static com.cburch.logisim.data.Value.TRUE_COLOR;

public class Ttl74161 extends AbstractTtlGate {

    public static final int PORT_INDEX_nCLR = 0;
    public static final int PORT_INDEX_CLK = 1;
    public static final int PORT_INDEX_A = 2;
    public static final int PORT_INDEX_B = 3;
    public static final int PORT_INDEX_C = 4;
    public static final int PORT_INDEX_D = 5;
    public static final int PORT_INDEX_EnP = 6;
    public static final int PORT_INDEX_nLOAD = 7;
    public static final int PORT_INDEX_EnT = 8;
    public static final int PORT_INDEX_QD = 9;
    public static final int PORT_INDEX_QC = 10;
    public static final int PORT_INDEX_QB = 11;
    public static final int PORT_INDEX_QA = 12;
    public static final int PORT_INDEX_RC0 = 13;

    public Ttl74161() {
        super(
                "74161",
                (byte) 16,
                new byte[]{11, 12, 13, 14, 15},
                new String[]{
                        "nClear",
                        "Clock",
                        "A",
                        "B",
                        "C",
                        "D",
                        "ENP",
                        "nLoad",
                        "Ent",
                        "QD",
                        "QC",
                        "QB",
                        "QA",
                        "RC0"
                });
        super.setInstancePoker(Poker.class);
    }

    public static class Poker extends InstancePoker {
        boolean isPressed = true;

        private boolean isInside(InstanceState state, MouseEvent e) {
            Point p = TTLGetTranslatedXY(state, e);
            boolean inside = false;
            for (int i = 0; i < 4; i++) {
                int dx = p.x - (56 + i * 10);
                int dy = p.y - 30;
                int d2 = dx * dx + dy * dy;
                inside |= (d2 < 4 * 4);
            }
            return inside;
        }

        private int getIndex(InstanceState state, MouseEvent e) {
            Point p = TTLGetTranslatedXY(state, e);
            for (int i = 0; i < 4; i++) {
                int dx = p.x - (56 + i * 10);
                int dy = p.y - 30;
                int d2 = dx * dx + dy * dy;
                if (d2 < 4 * 4) return 3 - i;
            }
            return 0;
        }

        @Override
        public void mousePressed(InstanceState state, MouseEvent e) {
            isPressed = isInside(state, e);
        }

        @Override
        public void mouseReleased(InstanceState state, MouseEvent e) {
            if (!state.getAttributeValue(TTL.DRAW_INTERNAL_STRUCTURE).booleanValue()) return;
            if (isPressed && isInside(state, e)) {
                int index = getIndex(state, e);

                Point p = TTLGetTranslatedXY(state, e);
                System.out.print("x=");
                System.out.print(p.x);
                System.out.print(",y=");
                System.out.print(p.y);
                System.out.print(",i=");
                System.out.print(index);
                System.out.println(index);

                TTLRegisterData data = (TTLRegisterData) state.getData();
                if (data == null) return;
                long current = data.getValue().toLongValue();
                long bitValue = 1 << index;
                current ^= bitValue;
                data.setValue(Value.createKnown(4, current));
                state.fireInvalidated();
            }
            isPressed = false;
        }
    }

    private TTLRegisterData getData(InstanceState state) {
        TTLRegisterData data = (TTLRegisterData) state.getData();
        if (data == null) {
            data = new TTLRegisterData(BitWidth.create(4));
            state.setData(data);
        }
        return data;
    }

    @Override
    public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
        Graphics2D g = (Graphics2D) painter.getGraphics();
        super.paintBase(painter, false, false);
        Drawgates.paintPortNames(
                painter,
                x,
                y,
                height,
                new String[]{
                        "nClr", "Clk", "A", "B", "C", "D", "EnP", "nLD", "EnT", "Qd", "Qc", "Qb", "Qa", "RC0"
                });
        TTLRegisterData data = (TTLRegisterData) painter.getData();
        drawState(g, x, y, height, data);
    }

    private void drawState(Graphics2D g, int x, int y, int height, TTLRegisterData state) {
        if (state != null) {
            long value = state.getValue().toLongValue();
            for (int i = 0; i < 4; i++) {
                boolean isSetBitValue = (value & (1 << (3 - i))) != 0;
                g.setColor(isSetBitValue ? TRUE_COLOR : FALSE_COLOR);
                g.fillOval(x + 52 + i * 10, y + height / 2 - 4, 8, 8);
                g.setColor(Color.WHITE);
                GraphicsUtil.drawCenteredText(
                        g, isSetBitValue ? "1" : "0", x + 56 + i * 10, y + height / 2);
            }
            g.setColor(Color.BLACK);
        }
    }

    @Override
    public void ttlpropagate(InstanceState state) {

        TTLRegisterData data = (TTLRegisterData) state.getData();
        if (data == null) {
            data = new TTLRegisterData(BitWidth.create(4));
            state.setData(data);
        }

        boolean triggered = data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING);
        if (triggered) {

            Value nClear = state.getPortValue(PORT_INDEX_nCLR);
            Value nLoad = state.getPortValue(PORT_INDEX_nLOAD);

            long counter;

            if (nClear.toLongValue() == 0) {
                counter = 0;
            } else if (nLoad.toLongValue() == 0) {
                counter = state.getPortValue(PORT_INDEX_A).toLongValue();
                counter += state.getPortValue(PORT_INDEX_B).toLongValue() << 1;
                counter += state.getPortValue(PORT_INDEX_C).toLongValue() << 2;
                counter += state.getPortValue(PORT_INDEX_D).toLongValue() << 3;
            } else {
                counter = data.getValue().toLongValue();
                Value enpAndEnt = state.getPortValue(PORT_INDEX_EnP).and(state.getPortValue(PORT_INDEX_EnT));
                if (enpAndEnt.toLongValue() == 1) {
                    counter++;
                    if (counter > 15) {
                        counter = 0;
                    }
                }
            }
            data.setValue(Value.createKnown(BitWidth.create(4), counter));

        }

        Value vA = data.getValue().get(0);
        Value vB = data.getValue().get(1);
        Value vC = data.getValue().get(2);
        Value vD = data.getValue().get(3);

        state.setPort(PORT_INDEX_QA, vA, 1);
        state.setPort(PORT_INDEX_QB, vB, 1);
        state.setPort(PORT_INDEX_QC, vC, 1);
        state.setPort(PORT_INDEX_QD, vD, 1);

        // RC0 = QA AND QB AND QC AND QD AND ENT
        state.setPort(PORT_INDEX_RC0,
                state.getPortValue(PORT_INDEX_EnT)
                        .and(vA)
                        .and(vB)
                        .and(vC)
                        .and(vD), 1);
    }

    @Override
    public boolean CheckForGatedClocks(NetlistComponent comp) {
        return true;
    }

    @Override
    public int[] ClockPinIndex(NetlistComponent comp) {
        return new int[]{1};
    }
}
