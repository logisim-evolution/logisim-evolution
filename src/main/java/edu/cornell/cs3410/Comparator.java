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

public class Comparator extends InstanceFactory {
    public Comparator() {
        super("Comparator Black Box");
        setOffsetBounds(Bounds.create(-30, -50, 70, 100));
        setPorts(new Port[] {
            new Port(-30, -30, Port.INPUT, 32), //A
            new Port(-30, 0, Port.INPUT, 32), //B
            new Port(-30, 30, Port.INPUT, 1), //Signed
            new Port(40, -30, Port.OUTPUT, 1), //A>B
            new Port(40, 0, Port.OUTPUT, 1), //A==B
            new Port(40, 30, Port.OUTPUT, 1), //A<B

        });
    }
    
    // Shifts A for risc-v not B
    @Override
    public void propagate(InstanceState state) {
        int A = state.getPortValue(0).toIntValue();
        int B = state.getPortValue(1).toIntValue();
        int signed = state.getPortValue(2).toIntValue();
        /*
        if(A==B) {
        	Value out = Value.createKnown(BitWidth.create(32), 1);
        	state.setPort(4, out, 32);
        	return;
        }
        
        if (signed==1) {
        	Value out = Value.createKnown(BitWidth.create(32), (A>B)? 1 : 0);
        	state.setPort(3, out, 32);
        	out = Value.createKnown(BitWidth.create(32), (A<B)? 1 : 0);
        	state.setPort(5, out, 32);
        }
        else { //Using if statements avoids absolute value overflow with MIN_INT
        	
        }*/
        int result;
        if (signed==1) {
        	result = Integer.compare(A, B);
        }
        else {
        	result = Integer.compareUnsigned(A, B);
        }
        
        
        Value out = Value.createKnown(BitWidth.create(32), result>0 ? 1 : 0);
        state.setPort(3, out, 1);
        out = Value.createKnown(BitWidth.create(32), result==0 ? 1 : 0);
        state.setPort(4, out, 1);
        out = Value.createKnown(BitWidth.create(32), result<0 ? 1 : 0);
        state.setPort(5, out, 1);
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
        painter.drawPort(0, "A", Direction.EAST);
        painter.drawPort(1, "B", Direction.EAST);
        painter.drawPort(2, "Signed", Direction.EAST);
        painter.drawPort(3, "A>B", Direction.WEST);
        painter.drawPort(4, "A==B", Direction.WEST);
        painter.drawPort(5, "A<B", Direction.WEST);

        
    }
    

}
