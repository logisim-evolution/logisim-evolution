/* Adapted by hwang from kwalsh's ram to construct a memory + IO subsystem */
/* Adapted by kwalsh from Logisim's standard RAM, which is... */
/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package edu.cornell.cs3410;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.MemContents;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;


public class RamIO extends Mem {

  private static final int DATA_ATTR = 32;
  private static final BitWidth dataBits = BitWidth.create(32);

  private static Attribute<?>[] ATTRIBUTES = {
    Mem.ADDR_ATTR
  };
  private static Object[] DEFAULTS = {
    BitWidth.create(20)
  };

  private static final int OE  = MEM_INPUTS + 0;
  private static final int CLR = MEM_INPUTS + 1;
  private static final int CLK = MEM_INPUTS + 2;
  private static final int WE  = MEM_INPUTS + 3;
  private static final int DIN = MEM_INPUTS + 4;

  private static final int TTY = MEM_INPUTS + 5;
  private static final int TE = MEM_INPUTS + 6;
  private static final int TCLR = MEM_INPUTS + 7;
  private static final int KEYB = MEM_INPUTS + 8;
  private static final int KAVAIL = MEM_INPUTS + 9;
  private static final int KREAD = MEM_INPUTS + 10;

  private static final int IRQ_OUT = MEM_INPUTS + 11;

  private static int cntr = 0;

  private static Object[][] logOptions = new Object[9][];

  public RamIO() {
    super("MIPS RAM and IO", new SimpleStringGetter("MIPS RAM and IO Controller"), 3);
    setInstanceLogger(Logger.class);
    setOffsetBounds(Bounds.create(-140, -40, 240, 180));
  }

  // Uncomment to debug
  private static <printabletostring> void p(printabletostring... args) {
    for(printabletostring pts: args)
      System.out.print(pts);
    System.out.println();
  }

  private static <printabletostring> void pf(String format, printabletostring... args) {
    System.out.format(format, args);
  }


  @Override
    protected void configureNewInstance(Instance instance) {
      super.configureNewInstance(instance);
      instance.addAttributeListener();
    }

  @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
      super.instanceAttributeChanged(instance, attr);
      configurePorts(instance);
    }

  @Override
    void configurePorts(Instance instance) {
      boolean asynch = false;
      boolean separate = true;

      int portCount = MEM_INPUTS;
      if(asynch) portCount += 9;
      else if(separate) portCount += 12;
      else portCount += 10;
      Port[] ps = new Port[portCount];

      ps[ADDR] = new Port(-140,  -20, Port.INPUT, ADDR_ATTR);
      ps[ADDR].setToolTip(Strings.getter("memAddrTip"));
      ps[DATA] = new Port(-140,  20, Port.INOUT, DATA_ATTR);
      ps[DATA].setToolTip(Strings.getter("memDataTip"));
      ps[CS]   = new Port(-140,  60, Port.INPUT, 4);
      ps[CS].setToolTip(new SimpleStringGetter("Byte selects: each 0 disables access to one byte of the addressed word."));
      ps[OE]  = new Port(-140, 80, Port.INPUT, 1);
      ps[OE].setToolTip(Strings.getter("ramOETip"));
      ps[CLR] = new Port(-140, 100, Port.INPUT, 1);
      ps[CLR].setToolTip(Strings.getter("ramClrTip"));

      if(!asynch) {
        ps[CLK] = new Port(-140, 120, Port.INPUT, 1);
        ps[CLK].setToolTip(Strings.getter("ramClkTip"));
      }
      
      if(separate) {
        ps[DIN] = new Port(-140, 0, Port.INPUT, DATA_ATTR);
        ps[WE] = new Port(-140, 40, Port.INPUT, 1);
        ps[WE].setToolTip(Strings.getter("ramWETip"));
        ps[DIN].setToolTip(Strings.getter("ramInTip"));
      } else {
        ps[DATA].setToolTip(Strings.getter("ramBusTip"));
      }

      ps[TTY]  = new Port(100, 0, Port.OUTPUT, 7);
      ps[TE] = new Port(100, 20, Port.OUTPUT, 1);
      ps[TCLR] = new Port(100, 40, Port.OUTPUT, 1);
      ps[KEYB] = new Port(100, 80, Port.INPUT, 7);
      ps[KAVAIL] = new Port(100, 100, Port.INPUT, 1);
      ps[KREAD] = new Port(100, 120, Port.OUTPUT, 1);
      ps[IRQ_OUT] = new Port(-100, 140, Port.OUTPUT, 1);
      instance.setPorts(ps);
    }

  @Override
    public AttributeSet createAttributeSet() {
      return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
    }

  @Override
    MemState getState(InstanceState state) {
      BitWidth addrBits = state.getAttributeValue(ADDR_ATTR);

      RamState myState = (RamState) state.getData();
      if(myState == null) {
        MemContents contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth());
        Instance instance = state.getInstance();
        myState = new RamState(instance, contents, new MemListener(instance));
        state.setData(myState);
      } else {
        myState.setRam(state.getInstance());
      }
      return myState;
    }

  @Override
    MemState getState(Instance instance, CircuitState state) {
      BitWidth addrBits = instance.getAttributeValue(ADDR_ATTR);

      RamState myState = (RamState) instance.getData(state);
      if(myState == null) {
        MemContents contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth());
        myState = new RamState(instance, contents, new MemListener(instance));
        instance.setData(state, myState);
      } else {
        myState.setRam(instance);
      }
      return myState;
    }

  @Override
    HexFrame getHexFrame(Project proj, Instance instance, CircuitState circState) {
      RamState state = (RamState) getState(instance, circState);
      return state.getHexFrame(proj, instance);
    }

  static final Value[] vmask = new Value[] {
    /*0:xxxxxxxx*/ Value.createUnknown(BitWidth.create(32)),
      /*1:xxxxxx00*/ Value.createKnown(BitWidth.create(8), 0).extendWidth(32, Value.UNKNOWN),
      /*2:xxxx00xx*/ Value.createUnknown(BitWidth.create(8)).extendWidth(16, Value.FALSE).extendWidth(32, Value.UNKNOWN),
      /*3:xxxx0000*/ Value.createKnown(BitWidth.create(16), 0).extendWidth(32, Value.UNKNOWN),
      /*4:xx00xxxx*/ Value.createUnknown(BitWidth.create(16)).extendWidth(24, Value.FALSE).extendWidth(32, Value.UNKNOWN),
      /*5:xx00xx00*/ Value.createKnown(BitWidth.create(8), 0).extendWidth(16, Value.UNKNOWN).extendWidth(24, Value.FALSE).extendWidth(32, Value.UNKNOWN),
      /*6:xx0000xx*/ Value.createUnknown(BitWidth.create(8)).extendWidth(24, Value.FALSE).extendWidth(32, Value.UNKNOWN),
      /*7:xx000000*/ Value.createKnown(BitWidth.create(24), 0).extendWidth(32, Value.UNKNOWN),
      /*8:00xxxxxx*/ Value.createUnknown(BitWidth.create(24)).extendWidth(32, Value.FALSE),
      /*9:00xxxx00*/ Value.createKnown(BitWidth.create(8), 0).extendWidth(24, Value.UNKNOWN).extendWidth(32, Value.FALSE),
      /*a:00xx00xx*/ Value.createUnknown(BitWidth.create(8)).extendWidth(16, Value.FALSE).extendWidth(24, Value.UNKNOWN).extendWidth(32, Value.FALSE),
      /*b:00xx0000*/ Value.createKnown(BitWidth.create(16), 0).extendWidth(24, Value.UNKNOWN).extendWidth(32, Value.FALSE),
      /*c:0000xxxx*/ Value.createUnknown(BitWidth.create(16)).extendWidth(32, Value.FALSE),
      /*d:0000xx00*/ Value.createKnown(BitWidth.create(8), 0).extendWidth(16, Value.UNKNOWN).extendWidth(32, Value.FALSE),
      /*e:000000xx*/ Value.createUnknown(BitWidth.create(8)).extendWidth(32, Value.FALSE),
      /*f:00000000*/ Value.createKnown(BitWidth.create(32), 0)
  };

  @Override
    public void propagate(InstanceState state) {
      
      RamState myState = (RamState) getState(state);
      boolean asynch = false;
      boolean separate = true;

      Value addrValue = state.getPortValue(ADDR);
      Value maskValue = state.getPortValue(CS);
      boolean triggered = asynch || myState.setClock(state.getPortValue(CLK), StdAttr.TRIG_RISING);
      boolean outputEnabled = state.getPortValue(OE) != Value.FALSE;
      boolean shouldClear = state.getPortValue(CLR) == Value.TRUE;

      if(shouldClear) {
        myState.getContents().clear();
      }

      // write value from keyboard to RDR and raise an interrupt.
      // assume RDR at 0xFF0000
      // assume RCR at 0xFF0004
      if(triggered){
        p("----read------");
        int rcr = 0xFF0000;
        int rdr = 0xFF0004;
        int _rcr = (int)myState.getContents().get(rcr);
        p("rcr value=" + Integer.toHexString(_rcr));
        int _rdr = (int)myState.getContents().get(rdr);
        p("rdr value=" + Integer.toHexString(_rdr));
        // handle rx and generate irq.
        Value ka_value = state.getPortValue(KAVAIL);
        Value irq_pending = state.getPortValue(IRQ_OUT);
        if ((ka_value.toIntValue() == 1) && (irq_pending.toIntValue() != 1)){ // hardware read one byte into memory.
          Value kv = state.getPortValue(KEYB);
          myState.getContents().set(rdr, kv.toIntValue());
          state.setPort(KREAD, Value.createKnown(BitWidth.create(1), 1), 0);
          state.setPort(IRQ_OUT, Value.createKnown(BitWidth.create(1), 1), 0);
        } else {
          state.setPort(KREAD, Value.createKnown(BitWidth.create(1), 0), 0);
        }

        // handle mem_rd from software and read next character from keyboard.
        if ((state.getPortValue(OE).toIntValue() == 1) && 
            (state.getPortValue(ADDR).toIntValue() == 0xFF0004)) 
        {
          state.setPort(IRQ_OUT, Value.createKnown(BitWidth.create(1), 0), 0);
        }
      }

      // read value from TCR and write to tty.
      // assume TCR at 0xFF0008
      // assume TDR at 0xFF000C
      if(triggered){
        p("----write-----");
        int tcr = 0xFF0008;
        int tdr = 0xFF000C;
        int _tcr = (int)myState.getContents().get(tcr) ;
        int _tdr = (int)myState.getContents().get(tdr) ;
        p("tcr value = " + Integer.toHexString(_tcr));
        p("tdr value = " + Integer.toHexString(_tdr));

        // handle tx and ready signal
        if ((_tcr & 0x1) > 0) {
          if ((_tdr & 0xFF) > 0){
            state.setPort(TTY, Value.createKnown(BitWidth.create(7), _tdr), 0);
            state.setPort(TE, Value.createKnown(BitWidth.create(1), 1), 0);
            _tcr ^= 0x1;
            cntr = 0;
          }
        } else {
          // simulate delay, 20 cycle
          if (cntr > 20) {
            _tcr |= 0x1;
            myState.getContents().set(tcr, _tcr);
          } else {
            cntr++;
          }
          state.setPort(TE, Value.createKnown(BitWidth.create(1), 0), 0);
        }

        // handle clr signal
        if ((_tcr & 0x4) > 0){
          state.setPort(TCLR, Value.createKnown(BitWidth.create(1), 1), 0);
          _tcr ^= 0x4;
          myState.getContents().set(tcr, _tcr);
        } else {
          state.setPort(TCLR, Value.createKnown(BitWidth.create(1), 0), 0);
        }
      }

      int mask = 0, bmask = 0;
      if (maskValue.get(0) != Value.FALSE) { mask |= 0x1<<0; bmask |= 0xff<<0; }
      if (maskValue.get(1) != Value.FALSE) { mask |= 0x1<<1; bmask |= 0xff<<8; }
      if (maskValue.get(2) != Value.FALSE) { mask |= 0x1<<2; bmask |= 0xff<<16; }
      if (maskValue.get(3) != Value.FALSE) { mask |= 0x1<<3; bmask |= 0xff<<24; }

      if (mask == 0) {
        myState.setCurrent(-1, 0);
        state.setPort(DATA, Value.createUnknown(dataBits), DELAY);
        return;
      }

      int addr = addrValue.toIntValue();
      if(!addrValue.isFullyDefined() || addr < 0) {
        return;
      }
      if(addr != myState.getCurrent()) {
        myState.setCurrent(addr, mask);
        myState.scrollToShow(addr);
      } else if (mask != myState.getCurrentMask()) {
        myState.setCurrent(addr, mask);
      }

      if(!shouldClear && triggered) {
        boolean shouldStore;
        if(separate) {
          shouldStore = state.getPortValue(WE) != Value.FALSE;
        } else {
          shouldStore = !outputEnabled;
        }
        if(shouldStore) {
          Value dataValue = state.getPortValue(separate ? DIN : DATA);
          int newVal = dataValue.toIntValue();
          int oldVal = (int)myState.getContents().get(addr);
          newVal = (newVal & bmask) | (oldVal & ~bmask);
          myState.getContents().set(addr, newVal);
        }
      }

      if(outputEnabled) {
        int val = (int)myState.getContents().get(addr);
        Value[] vals = vmask[mask].getAll();
        // vmask[mask] is x's and zeroes right now.
        // Just need to change any zeroes to ones if they are 1 in val.
        // For every group of 4 bits:
        for (int i = 0; i < 4; ++i) {
          // Masked out. This group of bits can be skipped.
          if ((mask & (1 << i)) == 0) {
            continue;
          }
          // For each of the 8 bits in this group,
          // set if this bit is set in val.
          for (int j = 0, pos = i * 8; j < 8; ++j, ++pos) {
            if ((val & (1 << pos)) != 0) {
              vals[pos] = Value.TRUE;
            }
          }
        }
        state.setPort(DATA, Value.create(vals), DELAY);
      } else {
        state.setPort(DATA, vmask[0], DELAY);
      }

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
      
      // draw tty ctrl and keyboard ctrl
      g.drawRect(bds.getX()+ bds.getWidth()/ 3 * 2 , bds.getY() + 30, bds.getWidth()/3, bds.getHeight()/2 - 30);
      GraphicsUtil.drawCenteredText(g, "TTY", bds.getX() + bds.getWidth()
          / 4 * 3, bds.getY() + 5 + bds.getHeight() / 4 );
      GraphicsUtil.drawCenteredText(g, "CNTL", bds.getX() + bds.getWidth()
          / 4 * 3, bds.getY() + 25 + bds.getHeight() / 4 );

      g.drawRect(bds.getX()+ bds.getWidth()/ 3 * 2 , bds.getY() + bds.getHeight()/2 + 20, bds.getWidth()/3, bds.getHeight()/2 - 30);
      GraphicsUtil.drawCenteredText(g, "KEYB", bds.getX() + bds.getWidth()
          / 4 * 3, bds.getY() + bds.getHeight() / 4 * 3 - 5);
      GraphicsUtil.drawCenteredText(g, "CNTL", bds.getX() + bds.getWidth()
          / 4 * 3, bds.getY() + bds.getHeight() / 4 * 3 + 15);

       GraphicsUtil.drawCenteredText(g, "RAM + IO CNTL", bds.getX() + bds.getWidth()
          / 4 + 25, bds.getY() + bds.getHeight() / 2 + 15);

      // draw input and output ports
      painter.drawPort(DATA, Strings.get("ramDataLabel"), Direction.EAST);
      painter.drawPort(ADDR, Strings.get("ramAddrLabel"), Direction.EAST);
      g.setColor(Color.GRAY);
      painter.drawPort(CS, Strings.get("ramCSLabel"), Direction.EAST);

      boolean asynch = false;
      boolean separate = true;

      if(!asynch) painter.drawClock(CLK, Direction.EAST);
      painter.drawPort(OE, Strings.get("ramOELabel"), Direction.EAST);
      painter.drawPort(CLR, Strings.get("ramClrLabel"), Direction.EAST);

      if(separate) {
        painter.drawPort(WE, Strings.get("ramWELabel"), Direction.EAST);
        painter.getGraphics().setColor(Color.BLACK);
        painter.drawPort(DIN, Strings.get("ramDataLabel"), Direction.EAST);
      }

      painter.drawPort(TTY, "TD", Direction.WEST);
      painter.drawPort(TE, "TE", Direction.WEST);
      painter.drawPort(TCLR, "TC", Direction.WEST);
      painter.drawPort(KEYB, "KD", Direction.WEST);
      painter.drawPort(KAVAIL, "KA", Direction.WEST);
      painter.drawPort(KREAD, "KR", Direction.WEST);
      painter.drawPort(IRQ_OUT, "IRQ_OUT", Direction.SOUTH);
    }

  @Override
    public void paintIcon(InstancePainter painter) {
      Graphics g = painter.getGraphics();
      Font old = g.getFont();
      g.setFont(old.deriveFont(9.0f));
      GraphicsUtil.drawCenteredText(g, "RAM", 10, 9);
      g.setFont(old);
      g.drawRect(0, 4, 19, 12);
      for(int dx = 2; dx < 20; dx += 5) {
        g.drawLine(dx,  2, dx,  4);
        g.drawLine(dx, 16, dx, 18);
      }
    }

  private static class RamState extends MemState
      implements InstanceData, AttributeListener {
      private Instance parent;
      private MemListener listener;
      private HexFrame hexFrame = null;
      private ClockState clockState;

      RamState(Instance parent, MemContents contents, MemListener listener) {
        super(contents);
        this.parent = parent;
        this.listener = listener;
        this.clockState = new ClockState();
        if(parent != null) parent.getAttributeSet().addAttributeListener(this);
        contents.addHexModelListener(listener);
      }

      void setRam(Instance value) {
        if(parent == value) return;
        if(parent != null) parent.getAttributeSet().removeAttributeListener(this);
        parent = value;
        if(value != null) value.getAttributeSet().addAttributeListener(this);
      }

      @Override
        public RamState clone() {
          RamState ret = (RamState) super.clone();
          ret.parent = null;
          ret.clockState = this.clockState.clone();
          ret.getContents().addHexModelListener(listener);
          return ret;
        }

      // Retrieves a HexFrame for editing within a separate window
      public HexFrame getHexFrame(Project proj, Instance instance) {
        if(hexFrame == null) {
          hexFrame = new HexFrame(proj, instance ,getContents());
          hexFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
              hexFrame = null;
            }
          });
        }
        return hexFrame;
      }

      //
      // methods for accessing the write-enable data
      //
      public boolean setClock(Value newClock, Object trigger) {
        return clockState.updateClock(newClock, trigger);
      }

      public void attributeListChanged(AttributeEvent e) { }

      public void attributeValueChanged(AttributeEvent e) {
        AttributeSet attrs = e.getSource();
        BitWidth addrBits = attrs.getValue(Mem.ADDR_ATTR);
        getContents().setDimensions(addrBits.getWidth(), dataBits.getWidth());
      }
  }

  public static class Logger extends InstanceLogger {
    @Override
      public Object[] getLogOptions(InstanceState state) {
        int addrBits = state.getAttributeValue(ADDR_ATTR).getWidth();
        if(addrBits >= logOptions.length) addrBits = logOptions.length - 1;
        synchronized(logOptions) {
          Object[] ret = logOptions[addrBits];
          if(ret == null) {
            ret = new Object[1 << addrBits];
            logOptions[addrBits] = ret;
            for(int i = 0; i < ret.length; i++) {
              ret[i] = Integer.valueOf(i);
            }
          }
          return ret;
        }
      }

    @Override
      public String getLogName(InstanceState state, Object option) {
        if(option instanceof Integer) {
          String disp = "MIPSRAM";
          Location loc = state.getInstance().getLocation();
          return disp + loc + "[" + option + "]";
        } else {
          return null;
        }
      }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return null;
    }

    @Override
      public Value getLogValue(InstanceState state, Object option) {
        if(option instanceof Integer) {
          MemState s = (MemState) state.getData();
          int addr = ((Integer) option).intValue();
          return Value.createKnown(BitWidth.create(s.getDataBits()),
              s.getContents().get(addr));
        } else {
          return Value.NIL;
        }
      }
  }
}
