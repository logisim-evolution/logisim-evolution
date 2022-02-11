// from Carl Burch's com.cburch.incr.Incrementer
// Fixed up to conform to InstanceFactory API
package edu.cornell.cs3410;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;

class Incrementer extends InstanceFactory {
    Incrementer() {
        super("Incrementer");
        setAttributes(new Attribute[] { StdAttr.WIDTH },
            new Object[] { BitWidth.create(8) });
        setOffsetBounds(Bounds.create(-30, -15, 30, 30));
        setPorts(new Port[] {
            new Port(-30, 0, Port.INPUT, StdAttr.WIDTH),
            new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH),
        });
    }

    @Override
    public void propagate(InstanceState state) {
        Value in = state.getPortValue(0);
        Value out;
        if (in.isFullyDefined()) {
            out = Value.createKnown(in.getBitWidth(), in.toIntValue() + 1);
        }
        else if(in.isErrorValue()) {
            out = Value.createError(in.getBitWidth());
        }
        else {
            out = Value.createUnknown(in.getBitWidth());
        }
        state.setPort(1, out, out.getWidth() + 1);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        painter.drawRectangle(painter.getBounds(), "+1");
        painter.drawPorts();
    }
}
