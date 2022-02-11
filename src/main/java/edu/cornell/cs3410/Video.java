package edu.cornell.cs3410;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;

// 128 x 128 pixel LCD display with 8bpp color (byte addressed)
class Video extends InstanceFactory {
    private static final int P_CLK = 0;
    private static final int P_WE = 1;
    private static final int P_X = 2;
    private static final int P_Y = 3;
    private static final int P_DATA = 4;
    private static final int P_RST = 5;

    private static final String BLINK_YES = "Blinking Dot";
    private static final String BLINK_NO = "No Cursor";
    private static final String[] BLINK_OPTIONS = { BLINK_YES, BLINK_NO };
    private static final String RESET_ASYNC = "Asynchronous";
    private static final String RESET_SYNC = "Synchronous";
    private static final String[] RESET_OPTIONS = { RESET_ASYNC, RESET_SYNC };

    private static final Attribute<String> BLINK_OPTION = Attributes.forOption("cursor",
            new SimpleStringGetter("Cursor"), BLINK_OPTIONS);
    private static final Attribute<String> RESET_OPTION = Attributes.forOption("reset",
            new SimpleStringGetter("Reset Behavior"), RESET_OPTIONS);

    public Video() {
        super("LCD Video");
        setAttributes(new Attribute[] { BLINK_OPTION, RESET_OPTION },
            new String[] { BLINK_YES, RESET_ASYNC } );
        setOffsetBounds(Bounds.create(-270, -140, 270, 270));
        Port[] ports = new Port[] {
            new Port(-220, 130, Port.INPUT, 1),
            new Port(-200, 130, Port.INPUT, 1),
            new Port(-140, 130, Port.INPUT, 7),
            new Port(-130, 130, Port.INPUT, 7),
            new Port(-120, 130, Port.INPUT, 16),
            new Port(-240, 130, Port.INPUT, 1)
        };
        ports[P_WE].setToolTip(new SimpleStringGetter("Enable: if 1 write pixel to screen"));
        ports[P_X].setToolTip(new SimpleStringGetter("X coordinate"));
        ports[P_Y].setToolTip(new SimpleStringGetter("Y coordinate"));
        ports[P_DATA].setToolTip(new SimpleStringGetter("RGB: in 5-5-5 format"));
        ports[P_RST].setToolTip(new SimpleStringGetter("Reset"));
        setPorts(ports);
    }

    private static Value val(InstanceState s, int pin) {
        return s.getPortValue(pin);
    }

    private static int addr(InstanceState s, int pin) {
        return val(s, pin).toIntValue();
    }

    @Override
    public void propagate(InstanceState state) {
        Data data = Data.get(state);
        int x = addr(state, P_X);
        int y = addr(state, P_Y);
        int color = addr(state, P_DATA);
        data.last_x = x;
        data.last_y = y;
        data.color = color;

        String reset_option = state.getAttributeValue(RESET_OPTION);
        if (reset_option == null) reset_option = RESET_OPTIONS[0];

        if (data.tick(val(state, P_CLK)) && val(state, P_WE) == Value.TRUE) {
            Graphics g = data.img.getGraphics();
            g.setColor(new Color(data.img.getColorModel().getRGB(color)));
            g.fillRect(x*2, y*2, 2, 2);
            if (RESET_SYNC.equals(reset_option) && val(state, P_RST) == Value.TRUE) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, 128*2, 128*2);
            }
        }

        if (!RESET_SYNC.equals(reset_option) && val(state, P_RST) == Value.TRUE) {
            Graphics g = data.img.getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 128*2, 128*2);
        }
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        int size = painter.getBounds().getWidth();
        Data d = Data.get(painter);
        drawVideo(painter, painter.getLocation().getX(), painter.getLocation().getY(), d, 
            painter.getAttributeValue(BLINK_OPTION), painter.getAttributeValue(RESET_OPTION));
    }

    @Override
    public void paintIcon(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        g.setColor(Color.BLACK);
        g.drawRoundRect(0,0,15,15,3,3);
        g.setColor(Color.BLUE);
        g.fillRect(3,3,10,10);
        g.setColor(Color.BLACK);
    }

    private boolean blink() {
        long now = System.currentTimeMillis();
        return (now/1000 % 2 == 0);
    }

    private void drawVideo(InstancePainter painter, int x, int y, Data data, String blink_option, String reset_option) {
        Graphics g = painter.getGraphics();

        x += -270;
        y += -140;

        g.drawRoundRect(x, y, 270-1, 270-1, 6, 6);
        for (int i = P_CLK+1; i <= P_RST; i++) {
            painter.drawPort(i);
        }
        g.drawRect(x+6, y+6, 258-1, 258-1);
        painter.drawClock(P_CLK, Direction.NORTH);
        g.drawImage(data.img, x+7, y+7, null);
        // draw a little cursor for sanity
        if (blink_option == null) {
            blink_option = BLINK_OPTIONS[0];
        }
        if (BLINK_YES.equals(blink_option) && blink()) {
            g.setColor(new Color(data.img.getColorModel().getRGB(data.color)));
            g.fillRect(x+7+data.last_x*2, y+7+data.last_y*2, 2, 2);
        }
    }

    private static class Data implements InstanceData, Cloneable {
        public Value lastClock = null;
        public BufferedImage img;
        public int last_x, last_y, color;
        
        public static Data get(InstanceState state) {
            Data ret = (Data) state.getData();
            if (ret == null) {
                // If it doesn't yet exist, then we'll set it up with our default
                // values and put it into the circuit state so it can be retrieved
                // in future propagations.
                ret = new Data(new BufferedImage(256, 256, BufferedImage.TYPE_USHORT_555_RGB));
                state.setData(ret);
            }
            return ret;
        }

        private Data(BufferedImage img) {
            this.img = img;
        }

        @Override
        public Video clone() {
            try {
                return (Video) super.clone();
            }
            catch(CloneNotSupportedException e) {
                return null;
            }
        }

        private boolean tick(Value clk) {
            boolean rising = (lastClock == null || (lastClock == Value.FALSE && clk == Value.TRUE));
            lastClock = clk;
            return rising;
        }
    }
}
