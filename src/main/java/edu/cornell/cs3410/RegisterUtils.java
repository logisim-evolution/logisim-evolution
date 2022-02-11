package edu.cornell.cs3410;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceState;

/**
 * This class provides a bunch of static constants and vars that are used in
 * RegisterFile32, RegisterPoker, and RegisterData.
 */
public class RegisterUtils {

    private RegisterUtils() {
        // Private: cannot construct.
    }

    // storage capacity
    static final int NUM_REGISTERS = 32;
    static final int NUM_BITS = 5;
    static final BitWidth WIDTH = BitWidth.create(NUM_REGISTERS);
    static final BitWidth DEPTH = BitWidth.create(NUM_BITS);

    static final int BOX_HEIGHT = 10;
    static final int BOX_WIDTH = 50;
    static final int COL_WIDTH = BOX_WIDTH + 15;
    static final int BOX_SEP = 10;

    // size
    static final int CHIP_WIDTH = 160;
    static final int CHIP_DEPTH = 180;

    static final int P_WDATA = 0;
    static final int P_RDATA1 = 1;
    static final int P_RDATA2 = 2;
    static final int P_WE = 3;
    static final int P_CLK = 4;
    static final int P_WADDR = 5;
    static final int P_RADDR1 = 6;
    static final int P_RADDR2 = 7;
    static final int NUM_PINS = 8;

    static final Value zero = Value.createKnown(WIDTH, 0);
    static final Value xxxx = Value.createError(WIDTH);
    static final Value zzzz = Value.createUnknown(WIDTH);

    static int boxX(int i) {
        if (i < NUM_REGISTERS / 2) {
            return CHIP_WIDTH / 2 - BOX_SEP / 2 - BOX_WIDTH;
        }
        else {
            return CHIP_WIDTH / 2 + BOX_SEP / 2 + COL_WIDTH - BOX_WIDTH;
        }
    }

    static Value val(InstanceState s, int pin) {
        return s.getPortValue(pin);
    }

    static int addr(InstanceState s, int pin) {
        return s.getPortValue(pin).toIntValue();
    }

    static int boxY(int i) {
        i = i % (NUM_REGISTERS / 2);
        return i*BOX_HEIGHT+2;
    }

    static void drawBox(Graphics g, Bounds bounds, Color color, int i) {
        g.setColor(color);
        g.drawRect(bounds.getX() + boxX(i), bounds.getY() + boxY(i), BOX_WIDTH, BOX_HEIGHT);
        g.setColor(Color.BLACK);
    }
}
