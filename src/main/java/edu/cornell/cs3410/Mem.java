/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package edu.cornell.cs3410;

import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.util.WeakHashMap;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

abstract class Mem extends InstanceFactory {
    // Note: The code is meant to be able to handle up to 32-bit addresses, but it
    // hasn't been debugged thoroughly. There are two definite changes I would
    // make if I were to extend the address bits: First, there would need to be some
    // modification to the memory's graphical representation, because there isn't
    // room in the box to include such long memory addresses with the current font
    // size. And second, I'd alter the MemContents class's PAGE_SIZE_BITS constant
    // to 14 so that its "page table" isn't quite so big.
    public static final Attribute<BitWidth> ADDR_ATTR = Attributes.forBitWidth(
            "addrWidth", Strings.getter("ramAddrWidthAttr"), 2, 24);
    private static final int DATA_ATTR = 32;

    // port-related constants
    static final int DATA = 0;
    static final int ADDR = 1;
    static final int CS = 2;
    static final int MEM_INPUTS = 3;

    // other constants
    static final int DELAY = 10;

    private WeakHashMap<Instance,File> currentInstanceFiles;

    Mem(String name, StringGetter desc, int extraPorts) {
        super(name, desc);
        currentInstanceFiles = new WeakHashMap<Instance, File>();
        setInstancePoker(MemPoker.class);
        setKeyConfigurator(new BitWidthConfigurator(ADDR_ATTR, 2, 24, 0));

        setOffsetBounds(Bounds.create(-140, -40, 140, 80));
    }
    
    abstract void configurePorts(Instance instance);
    @Override
    public abstract AttributeSet createAttributeSet();
    abstract MemState getState(InstanceState state);
    abstract MemState getState(Instance instance, CircuitState state);
    abstract HexFrame getHexFrame(Project proj, Instance instance, CircuitState state);
    @Override
    public abstract void propagate(InstanceState state);

    @Override
    protected void configureNewInstance(Instance instance) {
        configurePorts(instance);
    }
    
    void configureStandardPorts(Instance instance, Port[] ps) {
        ps[DATA] = new Port(   0,  0, Port.INOUT, DATA_ATTR);
        ps[ADDR] = new Port(-140,  0, Port.INPUT, ADDR_ATTR);
        ps[CS]   = new Port( -90, 40, Port.INPUT, 4);
        ps[DATA].setToolTip(Strings.getter("memDataTip"));
        ps[ADDR].setToolTip(Strings.getter("memAddrTip"));
        ps[CS].setToolTip(new SimpleStringGetter("Byte selects: each 0 disables access to one byte of the addressed word."));
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Bounds bds = painter.getBounds();

        // draw boundary
        painter.drawBounds();

        // draw contents
        if(painter.getShowState()) {
            MemState state = getState(painter);
            state.paint(painter.getGraphics(), bds.getX(), bds.getY());
        } else {
            BitWidth addr = painter.getAttributeValue(ADDR_ATTR);
            int addrBits = addr.getWidth();
            int bytes = 1 << (addrBits+2); // count bytes, not words
            String label;
            if(addrBits >= 30) {
                label = StringUtil.format(Strings.get("ramGigabyteLabel"), ""
                        + (bytes >>> 30));
            } else if(addrBits >= 20) {
                label = StringUtil.format(Strings.get("ramMegabyteLabel"), ""
                        + (bytes >> 20));
            } else if(addrBits >= 10) {
                label = StringUtil.format(Strings.get("ramKilobyteLabel"), ""
                        + (bytes >> 10));
            } else {
                label = StringUtil.format(Strings.get("ramByteLabel"), ""
                        + bytes);
            }
            GraphicsUtil.drawCenteredText(g, label, bds.getX() + bds.getWidth()
                    / 2, bds.getY() + bds.getHeight() / 2);
        }

        // draw input and output ports
        painter.drawPort(DATA, Strings.get("Data"), Direction.WEST);
        painter.drawPort(ADDR, Strings.get("Addr"), Direction.EAST);
        g.setColor(Color.GRAY);
        painter.drawPort(CS, Strings.get("CS"), Direction.SOUTH);
    }
    
    File getCurrentImage(Instance instance) {
        return currentInstanceFiles.get(instance);
    }
    
    void setCurrentImage(Instance instance, File value) {
        currentInstanceFiles.put(instance, value);
    }

    public void loadImage(InstanceState instanceState, File imageFile)
            throws IOException {
        MemState s = this.getState(instanceState);
        HexFile.open(s.getContents(), imageFile);
        this.setCurrentImage(instanceState.getInstance(), imageFile);
    }

    @Override
    protected Object getInstanceFeature(Instance instance, Object key) {
        if(key == MenuExtender.class) return new MemMenu(this, instance);
        return super.getInstanceFeature(instance, key);
    }
    
    static class MemListener implements HexModelListener {
        Instance instance;
        
        MemListener(Instance instance) { this.instance = instance; }


        @Override
        public void bytesChanged(HexModel source, long start, long numBytes, long[] oldValues) {
            instance.fireInvalidated();
        }

        public void metainfoChanged(HexModel source) { }

    }
}
