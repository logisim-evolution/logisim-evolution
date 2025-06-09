package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;

import java.awt.Color;
import java.awt.Graphics;

public class EEPROM extends InstanceFactory {

    // EEPROM-specific attributes
    static final Attribute<Integer> WRITE_CYCLES_ATTR =
            Attributes.forIntegerRange("writeCycles", 1000, 1000000);
    static final Attribute<Integer> WRITE_TIME_ATTR =
            Attributes.forIntegerRange("writeTimeMs", 1, 100);
    static final Attribute<Boolean> WRITE_PROTECT_ATTR =
            Attributes.forBoolean("writeProtect", new StringGetter() {
                public String get() { return "Write Protect"; }
            });

    // Inherited from RAM-like behavior
    static final Attribute<BitWidth> ADDR_ATTR = Attributes.forBitWidth("addrWidth", 1, 24);
    static final Attribute<BitWidth> DATA_ATTR = Attributes.forBitWidth("dataWidth", 1, 32);

    // Port indices
    private static final int DATA = 0;
    private static final int ADDR = 1;
    private static final int WE = 2;    // Write Enable (active low)
    private static final int OE = 3;    // Output Enable (active low)
    private static final int CE = 4;    // Chip Enable (active low)

    public EEPROM() {
        super("EEPROM", new StringGetter() {
            public String get() { return "EEPROM"; }
        });
        setAttributes(new Attribute[] {
                ADDR_ATTR, DATA_ATTR, WRITE_CYCLES_ATTR,
                WRITE_TIME_ATTR, WRITE_PROTECT_ATTR, StdAttr.LABEL
        }, new Object[] {
                BitWidth.create(8), BitWidth.create(8), 100000, 5, false, ""
        });
        setOffsetBounds(Bounds.create(-140, -40, 140, 80));
        setIconName("eeprom.gif");
    }

    @Override
    public void configureNewInstance(Instance instance) {
        Bounds bds = instance.getBounds();
        instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT,
                bds.getX() + bds.getWidth() / 2, bds.getY() - 3,
                GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);
        updatePorts(instance);
    }

    private void updatePorts(Instance instance) {
        BitWidth dataWidth = instance.getAttributeValue(DATA_ATTR);
        BitWidth addrWidth = instance.getAttributeValue(ADDR_ATTR);

        Port[] ports = new Port[5];
        ports[DATA] = new Port(0, 0, Port.INOUT, dataWidth);
        ports[ADDR] = new Port(-140, -20, Port.INPUT, addrWidth);
        ports[WE] = new Port(-140, 0, Port.INPUT, 1);
        ports[OE] = new Port(-140, 20, Port.INPUT, 1);
        ports[CE] = new Port(-70, 40, Port.INPUT, 1);

        ports[DATA].setToolTip(new StringGetter() {
            public String get() { return "Data input/output"; }
        });
        ports[ADDR].setToolTip(new StringGetter() {
            public String get() { return "Address input"; }
        });
        ports[WE].setToolTip(new StringGetter() {
            public String get() { return "Write Enable (active low)"; }
        });
        ports[OE].setToolTip(new StringGetter() {
            public String get() { return "Output Enable (active low)"; }
        });
        ports[CE].setToolTip(new StringGetter() {
            public String get() { return "Chip Enable (active low)"; }
        });

        instance.setPorts(ports);
    }


    @Override
    public void propagate(InstanceState state) {
        EEPROMState myState = getEEPROMState(state);
        BitWidth dataWidth = state.getAttributeValue(DATA_ATTR);
        BitWidth addrWidth = state.getAttributeValue(ADDR_ATTR);
        boolean writeProtected = state.getAttributeValue(WRITE_PROTECT_ATTR);

        Value addr = state.getPortValue(ADDR);
        Value we = state.getPortValue(WE);
        Value oe = state.getPortValue(OE);
        Value ce = state.getPortValue(CE);
        Value dataIn = state.getPortValue(DATA);

        // Chip must be enabled
        if (!ce.equals(Value.FALSE)) {
            state.setPort(DATA, Value.createUnknown(dataWidth), 1);
            return;
        }

        // Address must be valid
        if (!addr.isFullyDefined()) {
            state.setPort(DATA, Value.createError(dataWidth), 1);
            return;
        }

        long address = addr.toLongValue();

        // Read operation (OE low, WE high)
        if (oe.equals(Value.FALSE) && !we.equals(Value.FALSE)) {
            long dataLong = myState.getContents().get(address);
            Value data = Value.createKnown(dataWidth, dataLong);
            state.setPort(DATA, data, myState.getReadDelay());
        }
        // Write operation (WE low, OE high)
        else if (we.equals(Value.FALSE) && !oe.equals(Value.FALSE)) {
            if (!writeProtected && dataIn.isFullyDefined()) {
                // Simulate EEPROM write delay
                int writeDelay = state.getAttributeValue(WRITE_TIME_ATTR);
                myState.getContents().set(address, dataIn.toLongValue());
                myState.incrementWriteCycle((int)address);

                // Check write cycle limit
                int maxCycles = state.getAttributeValue(WRITE_CYCLES_ATTR);
                if (myState.getWriteCycles((int)address) >= maxCycles) {
                    // EEPROM cell worn out - data becomes unreliable
                    myState.markCellWornOut((int)address);
                }

                state.setPort(DATA, Value.createUnknown(dataWidth), writeDelay);
            }
        }
        // High impedance state
        else {
            state.setPort(DATA, Value.createUnknown(dataWidth), 1);
        }
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Bounds bds = painter.getBounds();

        // Draw component outline
        g.setColor(Color.WHITE);
        g.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        g.setColor(Color.BLACK);
        g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());

        // Draw EEPROM label
        GraphicsUtil.drawCenteredText(g, "EEPROM",
                bds.getX() + bds.getWidth() / 2,
                bds.getY() + bds.getHeight() / 2);

        // Draw memory size info
        BitWidth addrWidth = painter.getAttributeValue(ADDR_ATTR);
        BitWidth dataWidth = painter.getAttributeValue(DATA_ATTR);
        int memSize = 1 << addrWidth.getWidth();
        String sizeStr = memSize + "x" + dataWidth.getWidth();

        g.setFont(g.getFont().deriveFont(10.0f));
        GraphicsUtil.drawCenteredText(g, sizeStr,
                bds.getX() + bds.getWidth() / 2,
                bds.getY() + bds.getHeight() / 2 + 15);

        // Draw port labels
        g.setFont(g.getFont().deriveFont(8.0f));
        g.drawString("A", bds.getX() - 135, bds.getY() - 15);
        g.drawString("WE", bds.getX() - 135, bds.getY() + 5);
        g.drawString("OE", bds.getX() - 135, bds.getY() + 25);
        g.drawString("CE", bds.getX() - 65, bds.getY() + 35);
        g.drawString("D", bds.getX() + 5, bds.getY() + 5);

        painter.drawPorts();
    }

    private EEPROMState getEEPROMState(InstanceState state) {
        BitWidth addrWidth = state.getAttributeValue(ADDR_ATTR);
        BitWidth dataWidth = state.getAttributeValue(DATA_ATTR);

        InstanceData data = state.getData();
        EEPROMState ret = null;
        if (data instanceof EEPROMState) {
            ret = (EEPROMState) data;
            ret.setBitWidths(addrWidth, dataWidth);
        } else {
            ret = new EEPROMState(addrWidth, dataWidth);
            state.setData(ret);
        }
        return ret;
    }

    // Inner class to manage EEPROM state
    // Inner class to manage EEPROM state
    private static class EEPROMState implements InstanceData {
        private MemContents contents;
        private int[] writeCycles;
        private boolean[] wornOutCells;
        private BitWidth addrWidth;
        private BitWidth dataWidth;

        EEPROMState(BitWidth addrWidth, BitWidth dataWidth) {
            this.addrWidth = addrWidth;
            this.dataWidth = dataWidth;
            this.contents = MemContents.create(addrWidth.getWidth(), dataWidth.getWidth(), false);
            int size = 1 << addrWidth.getWidth();
            this.writeCycles = new int[size];
            this.wornOutCells = new boolean[size];
        }

        @Override
        public Object clone() {
            try {
                EEPROMState cloned = (EEPROMState) super.clone();

                // Deep clone the mutable arrays
                cloned.writeCycles = this.writeCycles.clone();
                cloned.wornOutCells = this.wornOutCells.clone();

                // Deep clone the MemContents if it's mutable
                // Assuming MemContents has its own clone method or copy constructor
                cloned.contents = (MemContents) this.contents.clone();

                return cloned;
            } catch (CloneNotSupportedException e) {
                // This should never happen since we implement Cloneable through InstanceData
                throw new AssertionError("Clone not supported", e);
            }
        }

        void setBitWidths(BitWidth addrWidth, BitWidth dataWidth) {
            if (this.addrWidth != addrWidth || this.dataWidth != dataWidth) {
                this.addrWidth = addrWidth;
                this.dataWidth = dataWidth;
                this.contents = MemContents.create(addrWidth.getWidth(), dataWidth.getWidth(), false);
                int size = 1 << addrWidth.getWidth();
                this.writeCycles = new int[size];
                this.wornOutCells = new boolean[size];
            }
        }

        MemContents getContents() {
            return contents;
        }

        void incrementWriteCycle(int address) {
            if (address >= 0 && address < writeCycles.length) {
                writeCycles[address]++;
            }
        }

        int getWriteCycles(int address) {
            return (address >= 0 && address < writeCycles.length) ?
                    writeCycles[address] : 0;
        }

        void markCellWornOut(int address) {
            if (address >= 0 && address < wornOutCells.length) {
                wornOutCells[address] = true;
            }
        }

        boolean isCellWornOut(int address) {
            return address >= 0 && address < wornOutCells.length && wornOutCells[address];
        }

        int getReadDelay() {
            return 1; // EEPROM read is typically faster than write
        }
    }
}
