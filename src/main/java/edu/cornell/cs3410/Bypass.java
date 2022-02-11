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

public class Bypass extends InstanceFactory {
    public Bypass() {
        super("Bypass Black Box");
        setOffsetBounds(Bounds.create(-30, -50, 180, 100));
        setPorts(new Port[] {
            new Port(-30, -30, Port.INPUT, 5), //rs1
            new Port(-30, 30, Port.INPUT, 5), //rs2
            new Port(0, -50, Port.INPUT, 1), //XMwen
            new Port(40, -50, Port.INPUT, 5), //XMrd
            new Port(80, -50, Port.INPUT, 1), //MWwen
            new Port(120, -50, Port.INPUT, 5), //MwRD
            new Port(150, -30, Port.OUTPUT, 2), //FA
            new Port(150, 30, Port.OUTPUT, 2), //FB
        });
    }
    
    // Shifts A for risc-v not B
    @Override
    public void propagate(InstanceState state) {
        int rs1 = state.getPortValue(0).toIntValue();
        int rs2 = state.getPortValue(1).toIntValue();
        int XMwen = state.getPortValue(2).toIntValue();
        int XMrd = state.getPortValue(3).toIntValue();
        int MWwen = state.getPortValue(4).toIntValue();
        int MWrd = state.getPortValue(5).toIntValue();
        int FA = 0;
        int FB = 0;
        if (MWwen==1 && MWrd!=0 && !(XMwen==1 && XMrd!=0 && XMrd==rs1) && MWrd==rs1) {
        	FA = 0b01;
        }
        if (MWwen==1 && MWrd!=0 && !(XMwen==1 && XMrd!=0 && XMrd==rs2) && MWrd==rs2) {
        	FB = 0b01;
        }
        if (XMwen==1 && XMrd!=0 && XMrd==rs1) {  // Should be XMrd not MWrd
        	FA = 0b10;
        }
        if (XMwen==1 && XMrd!=0 && XMrd==rs2) {  // Should be XMrd not MWrd
        	FB = 0b10;
        }

        Value out = Value.createKnown(BitWidth.create(32), FA);
        // Eh, delay of 32? Sure...
        state.setPort(6, out, 32);
        out = Value.createKnown(BitWidth.create(32), FB);
        state.setPort(7, out, 32);
    }


    @Override
    public void paintInstance(InstancePainter painter) {
        Bounds bounds = painter.getBounds();
        int x0 = bounds.getX();
        int x1 = x0 + bounds.getWidth();
        int y0 = bounds.getY();
        int y1 = y0 + bounds.getHeight();
        int xp[] = {
            x0,x1, x1, x0//, x0, x0 + 20, x0
        };
        int yp[] = {
            y0, y0, y1, y1//, y1 - 40, y1 - 50, y1 - 60
        };
        GraphicsUtil.switchToWidth(painter.getGraphics(), 2);
        painter.getGraphics().drawPolygon(xp, yp, 4);
        painter.drawPort(0, "rs1", Direction.EAST);
        painter.drawPort(1, "rs2", Direction.EAST);
        painter.drawPort(2, "XMwen", Direction.NORTH);
        painter.drawPort(3, "XMrd", Direction.NORTH);
        painter.drawPort(4, "MWwen", Direction.NORTH);
        painter.drawPort(5, "MWrd", Direction.NORTH);
        painter.drawPort(6, "FA", Direction.WEST);
        painter.drawPort(7, "FB", Direction.WEST);
        
    }
    

}
