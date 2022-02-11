package edu.cornell.cs3410;

import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

import static edu.cornell.cs3410.RegisterUtils.*;

/** Represents a N-bit M-way dual port register file.
 * Draws heavily from com.cburch.incr.Counter example.
 */
class RegisterFile32 extends InstanceFactory {

    private static final Attribute[] ATTRIBUTES = { StdAttr.TRIGGER };

    RegisterFile32() {
        super("RegisterFile", new SimpleStringGetter("Register File"));
        setAttributes(new Attribute[] { StdAttr.TRIGGER }, new AttributeOption[] { StdAttr.TRIG_RISING });
        setOffsetBounds(Bounds.create(-1*CHIP_WIDTH, -1*CHIP_DEPTH/2, CHIP_WIDTH, CHIP_DEPTH));
        int left = -1 * CHIP_WIDTH;
        int right = 0;
        int top = -1 * CHIP_DEPTH / 2;
        int bottom = CHIP_DEPTH / 2;
        setPorts(new Port []{
            new Port(left, -10, Port.INPUT, NUM_REGISTERS),
            new Port(right, top + 40, Port.OUTPUT, NUM_REGISTERS),
            new Port(right, bottom - 40, Port.OUTPUT, NUM_REGISTERS),
            new Port(left + CHIP_WIDTH / 2 - 40, bottom, Port.INPUT, 1),
            new Port(left, bottom - 10, Port.INPUT, 1),
            new Port(left + CHIP_WIDTH / 2 - 10, bottom, Port.INPUT, NUM_BITS),
            new Port(left + CHIP_WIDTH / 2 + 30, bottom, Port.INPUT, NUM_BITS),
            new Port(left + CHIP_WIDTH / 2 + 50, bottom, Port.INPUT, NUM_BITS)
        });
        setInstancePoker(RegisterPoker.class);
    }

    @Override
    public void propagate(InstanceState state) {
        RegisterData data = RegisterData.get(state);
        AttributeOption triggerType = state.getAttributeValue(StdAttr.TRIGGER);

        if (data.updateClock(val(state, P_CLK), triggerType) && val(state, P_WE) != Value.FALSE) {
            int a = addr(state, P_WADDR);
            Value v = val(state, P_WDATA);
            if (a < 0) {
                data.reset(zzzz); // clobber all
            }
            else if (a == 0) {
                /* skip */
            }
            else if (a < NUM_REGISTERS) {
                data.regs[a] = v;
            }
            else {
                throw new IllegalArgumentException("Write address invalid: Please email kwalsh@cs and tell him!");
            }
        }
        int a1 = addr(state, P_RADDR1);
        int a2 = addr(state, P_RADDR2);
        if (a1 >= NUM_REGISTERS || a2 >= NUM_REGISTERS) {
            throw new IllegalArgumentException("Read address invalid: Please email kwalsh@cs and tell him!");
        }
        Value v1 = (a1 < 0 ? zzzz : (a1 < NUM_REGISTERS ? data.regs[a1] : xxxx));
        Value v2 = (a2 < 0 ? zzzz : (a2 < NUM_REGISTERS ? data.regs[a2] : xxxx));
        state.setPort(P_RDATA1, v1, 9);
        state.setPort(P_RDATA2, v2, 9);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        painter.drawRectangle(painter.getBounds(), "");
        painter.drawClock(P_CLK, Direction.EAST);
        painter.drawPort(P_WADDR);
        painter.drawPort(P_WDATA);
        painter.drawPort(P_WE);
        painter.drawPort(P_RADDR1);
        painter.drawPort(P_RDATA1);
        painter.drawPort(P_RADDR2);
        painter.drawPort(P_RDATA2);

        Graphics g = painter.getGraphics();
        Bounds bounds = painter.getBounds();

        Font font = g.getFont().deriveFont(9f);;

        // draw some pin labels
        int left = bounds.getX();
        int right = bounds.getX() + CHIP_WIDTH;
        int top = bounds.getY();
        int bottom =  bounds.getY() + CHIP_DEPTH;
        GraphicsUtil.drawText(g, font, "W", left+2, top+CHIP_DEPTH/2-10,
                GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
        GraphicsUtil.drawText(g, font, "A", right-2, top+40,
                GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
        GraphicsUtil.drawText(g, font, "B", right-2, bottom-40,
                GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
        GraphicsUtil.drawText(g, "WE", left+CHIP_WIDTH/2-40, bottom-1,
                GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
        GraphicsUtil.drawText(g, "xW", left+CHIP_WIDTH/2-10, bottom-1,
                GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
        GraphicsUtil.drawText(g, "xA", left+CHIP_WIDTH/2+30, bottom-1,
                GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
        GraphicsUtil.drawText(g, "xB", left+CHIP_WIDTH/2+50, bottom-1,
                GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);

        // draw some rectangles
        for (int i = 0; i < NUM_REGISTERS; i++) {
            drawBox(g, bounds, Color.GRAY, i);
        }

        // draw register labels
        for (int i = 0; i < NUM_REGISTERS; i++) {
            GraphicsUtil.drawText(g, font, "x"+i,
                bounds.getX() + boxX(i) - 1,
                bounds.getY() + boxY(i) + (BOX_HEIGHT-1)/2,
                GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
        }

        if (!painter.getShowState()) {
            return;
        }

        // draw state
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(bounds.getX() + boxX(0)+1, bounds.getY() + boxY(0)+1, BOX_WIDTH-1, BOX_HEIGHT-1);
        g.setColor(Color.BLACK);
        RegisterData data = RegisterData.get(painter);
        for (int i = 0; i < NUM_REGISTERS; i++) {
            int v = data.regs[i].toIntValue();
            String s = (data.regs[i].isFullyDefined() ? StringUtil.toHexString(WIDTH.getWidth(), v) : "?");
            GraphicsUtil.drawText(g, font, s,
                bounds.getX() + boxX(i) + BOX_WIDTH/2,
                bounds.getY() + boxY(i) + (BOX_HEIGHT-1)/2,
                GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
        }
    }

}
