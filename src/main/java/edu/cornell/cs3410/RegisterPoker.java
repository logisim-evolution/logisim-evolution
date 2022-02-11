package edu.cornell.cs3410;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;

import static edu.cornell.cs3410.RegisterUtils.*;

/**
 * This class handles poking interactions for RegisterFile32.
 */
public class RegisterPoker extends InstancePoker {
    private int idx, idx2;

    public RegisterPoker() { }

    @Override
    public boolean init(InstanceState state, MouseEvent e) {
        return state.getInstance().getBounds().contains(e.getX(), e.getY());
    }

    @Override
    public void paint(InstancePainter painter) {
        if (idx < 1) {
            return;
        }
        Bounds bounds = painter.getInstance().getBounds();
        drawBox(painter.getGraphics(), bounds, Color.RED, idx);
    }

    @Override
    public void keyTyped(InstanceState state, KeyEvent e) {
        // convert it to a hex digit; if it isn't a hex digit, abort.
        RegisterData data = RegisterData.get(state);
        if (idx < 1) {
            return;
        }
        int nue = Character.digit(e.getKeyChar(), 16);
        if (nue < 0) {
            return;
        }
        int old = (data.regs[idx].isFullyDefined() ? data.regs[idx].toIntValue() : 0);

        // getMask had a bug, and isn't needed
        Value val = Value.createKnown(WIDTH, ((old<<4) | nue) /* & WIDTH.getMask() */);

        data.regs[idx] = val;
        // check if need to propagate to P_RDATA1 or P_RDATA2
        int a1 = addr(state, P_RADDR1);
        if (a1 == idx) {
            state.setPort(P_RDATA1, val, 1);
        }
        int a2 = addr(state, P_RADDR2);
        if (a2 == idx) {
            state.setPort(P_RDATA2, val, 1);
        }
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
        Bounds bounds = state.getInstance().getBounds();
        idx2 = getRIndex(bounds, e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
        Bounds bounds = state.getInstance().getBounds();
        int idx3 = getRIndex(bounds, e.getX(), e.getY());
        if (idx3 < 1 || idx2 != idx3) {
            idx = 0;
            return;
        }
        idx = idx3;
    }

    @Override
    public void keyPressed(InstanceState state, KeyEvent e) {
        if (idx < 0) {
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
                if (idx < NUM_REGISTERS - 1) idx++;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
                if (0 < idx && idx < NUM_REGISTERS/2) idx += NUM_REGISTERS/2;
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
                if (idx > 1) idx--;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
                if (idx > NUM_REGISTERS/2) idx -= NUM_REGISTERS/2;
                break;
        }
    }

    private int getRIndex(Bounds bounds, int x, int y) {
        x -= bounds.getX();
        y -= bounds.getY();
        if (x < boxX(0) - 1 || x > boxX(NUM_REGISTERS-1) + BOX_WIDTH + 1)
            return -1;
        if (x > boxX(0) + BOX_WIDTH + 1 && x < boxX(NUM_REGISTERS-1) - 1)
            return -1;
        if (y <  boxY(0) || y >  boxY(NUM_REGISTERS-1) + BOX_HEIGHT) {
            return -1;
        }
        int i = y / BOX_HEIGHT;
        if (x > CHIP_WIDTH/2) {
            i += NUM_REGISTERS/2;
        }
        if (i < 0 || i >= NUM_REGISTERS) {
            return -1;
        }
        return i;
    }
}
