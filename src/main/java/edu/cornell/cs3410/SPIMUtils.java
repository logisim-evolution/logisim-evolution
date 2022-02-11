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
public class SPIMUtils {

    private SPIMUtils() {
        // Private: cannot construct.
    }

    // storage capacity
    static final int NUM_REGISTERS = 32;
    static final int NUM_BITS = 5;
    static final BitWidth _WIDTH = BitWidth.create(NUM_REGISTERS);
    static final BitWidth _DEPTH = BitWidth.create(NUM_BITS);

    static final int BOX_HEIGHT = 10;
    static final int BOX_WIDTH = 50;
    static final int COL_WIDTH = BOX_WIDTH + 15;
    static final int BOX_SEP = 10;

    // size
    static final int WIDTH = 240;
    static final int HEIGHT = 260;

    static final int CLK = 0;
    static final int OP = 1;
    static final int PC = 2;
    static final int ADDR = 3;
    static final int DOUT = 4;
    static final int DIN = 5;
    static final int STR = 6;
    static final int SEL = 7;
    static final int LD = 8;
    static final int IRQ_IN = 9;

    static final int PROG_CNTR=32;
    static final int BADVADDR=34;
    static final int STATUS=35;
    static final int CAUSE=36;
    static final int EPC=37;

    static final int E_CODE_HW = 0;
    static final int E_CODE_SYS = 8;
    static final int E_CODE_OV = 12;

    static final int KEYBOARD_IRQ = 8;

    static final Value zero = Value.createKnown(_WIDTH, 0);
    static final Value xxxx = Value.createError(_WIDTH);
    static final Value zzzz = Value.createUnknown(_WIDTH);

    static int boxX(int i) {
        if (i < NUM_REGISTERS / 2) {
            return WIDTH /3 - BOX_SEP / 2 - BOX_WIDTH;
        }
        else {
            return WIDTH /3 + BOX_SEP / 2 + COL_WIDTH - BOX_WIDTH;
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
        return i*BOX_HEIGHT+10;
    }

    static int cp_x(int i) {
      return BOX_SEP/2;
    }

    static int cp_y(int i){
      return (i - 16) * BOX_HEIGHT;
    }

    static void drawBox(Graphics g, Bounds bounds, Color color, int i) {
        g.setColor(color);
        g.drawRect(bounds.getX() + boxX(i), bounds.getY() + boxY(i), BOX_WIDTH, BOX_HEIGHT);
        g.setColor(Color.BLACK);
    }
  
    static void drawCPBox(Graphics g, Bounds bounds, Color color, int i) {
        g.setColor(color);
        g.drawRect(bounds.getX() + cp_x(i) + BOX_WIDTH , bounds.getY() + cp_y(i), BOX_WIDTH, BOX_HEIGHT);
        g.setColor(Color.BLACK);
    }

}
