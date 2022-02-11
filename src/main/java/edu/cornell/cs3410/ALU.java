package edu.cornell.cs3410;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;

import com.cburch.logisim.util.GraphicsUtil;

public class ALU extends InstanceFactory {
    public ALU() {
        super("ALU");
        setOffsetBounds(Bounds.create(-30, -50, 60, 100));
        setPorts(new Port[] { new Port(-30, -30, Port.INPUT, 32), new Port(-30, 30, Port.INPUT, 32),
                new Port(-10, 40, Port.INPUT, 4), new Port(10, 30, Port.INPUT, 5), new Port(30, 0, Port.OUTPUT, 32), });
    }

    // Shifts A for risc-v not B
    @Override
    public void propagate(InstanceState state) {
        int A = state.getPortValue(0).toIntValue();
        int B = state.getPortValue(1).toIntValue();
        int op = state.getPortValue(2).toIntValue();
        int shift = state.getPortValue(3).toIntValue();
        int ans = 0;
        switch (op) {

        /*
        case 0x0: //0b0000
        case 0x1: //0b0001
        case 0x2: //0b0010
        case 0x3: //0b0011
        case 0x4: //0b0100
        case 0x5: //0b0101
        case 0x6: //0b0110
        case 0x7: //0b0111
        case 0x8: //0b1000
        case 0x9: //0b1001
        case 0xa: //0b1010
        case 0xb: //0b1011
        case 0xc: //0b1100
        case 0xd: //0b1101
        case 0xe: //0b1110
        case 0xf: //0b1111
        */

        case 0x0: //0b0000
        case 0x1: //0b0001
            //ADD
            ans = A + B;
            break;

        case 0x2: //0b0010
        case 0x3: //0b0011
            //SLL
            ans = A << shift;
            break;

        case 0x4: //0b0100
        case 0x5: //0b0101
            //SUB
            ans = A - B;
            break;

        case 0x6: //0b0110
            //SRL
            ans = A >>> shift;
            break;

        case 0x7: //0b0111
            //SRA
            ans = A >> shift;
            break;

        case 0x8: //0b1000
            //LE
            ans = (A <= 0) ? 0x1 : 0x0;
            break;

        case 0x9: //0b1001
            //GT
            ans = (A > 0) ? 0x1 : 0x0;
            break;

        case 0xa: //0b1010
            //NE
            ans = (A != B) ? 0x1 : 0x0;
            break;

        case 0xb: //0b1011
            //EQ
            ans = (A == B) ? 0x1 : 0x0;
            break;

        case 0xc: //0b1100
            //XOR
            ans = A ^ B;
            break;

        case 0xd: //0b1101
            //NOR
            ans = ~(A | B);
            break;

        case 0xe: //0b1110
            //OR
            ans = A | B;
            break;

        case 0xf: //0b1111
            //AND
            ans = A & B;
            break;
        }
        Value out = Value.createKnown(BitWidth.create(32), ans);
        // Eh, delay of 32? Sure...
        state.setPort(4, out, 32);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Bounds bounds = painter.getBounds();
        int x0 = bounds.getX();
        int x1 = x0 + bounds.getWidth();
        int y0 = bounds.getY();
        int y1 = y0 + bounds.getHeight();
        int xp[] = { x0, x1, x1, x0, x0, x0 + 20, x0 };
        int yp[] = { y0, y0 + 30, y1 - 30, y1, y1 - 40, y1 - 50, y1 - 60 };
        GraphicsUtil.switchToWidth(painter.getGraphics(), 2);
        painter.getGraphics().drawPolygon(xp, yp, 7);
        painter.drawPort(0, "A", Direction.EAST);
        painter.drawPort(1, "B", Direction.EAST);
        painter.drawPort(2, "OP", Direction.SOUTH);
        painter.drawPort(3, "SA", Direction.SOUTH);
        painter.drawPort(4, "C", Direction.WEST);
    }

    @Override
    public void paintIcon(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        g.setColor(Color.BLACK);
        int xp[] = { 0, 15, 15, 0, 0, 3, 0 };
        int yp[] = { 0, 5, 10, 15, 10, 8, 6 };
        g.drawPolygon(xp, yp, 7);
    }
}
