/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import java.io.File;
import java.util.WeakHashMap;

public abstract class Mem extends InstanceFactory {
  // Note: The code is meant to be able to handle up to 32-bit addresses, but
  // it
  // hasn't been debugged thoroughly. There are two definite changes I would
  // make if I were to extend the address bits: First, there would need to be
  // some
  // modification to the memory's graphical representation, because there
  // isn't
  // room in the box to include such long memory addresses with the current
  // font
  // size. And second, I'd alter the MemContents class's PAGE_SIZE_BITS
  // constant
  // to 14 so that its "page table" isn't quite so big.

  static class MemListener implements HexModelListener {

    final Instance instance;

    MemListener(Instance instance) {
      this.instance = instance;
    }

    @Override
    public void bytesChanged(HexModel source, long start, long numBytes, long[] values) {
      instance.fireInvalidated();
    }

    @Override
    public void metainfoChanged(HexModel source) {}
  }

  public static final int SymbolWidth = 200;
  public static final Attribute<BitWidth> ADDR_ATTR = Attributes.forBitWidth("addrWidth", S.getter("ramAddrWidthAttr"), 2, 24);

  public static final Attribute<BitWidth> DATA_ATTR = Attributes.forBitWidth("dataWidth", S.getter("ramDataWidthAttr"));
  public static final AttributeOption SEL_HIGH = new AttributeOption("high", S.getter("stdTriggerHigh"));

  public static final AttributeOption SEL_LOW = new AttributeOption("low", S.getter("stdTriggerLow"));

  public static final Attribute<AttributeOption> ATTR_SELECTION =
      Attributes.forOption("Select", S.getter("ramSelAttr"), new AttributeOption[] {SEL_HIGH, SEL_LOW});

  public static final AttributeOption SINGLE = new AttributeOption("single", S.getter("memSingle"));
  public static final AttributeOption DUAL = new AttributeOption("dual", S.getter("memDual"));
  public static final AttributeOption QUAD = new AttributeOption("quad", S.getter("memQuad"));
  public static final AttributeOption OCTO = new AttributeOption("octo", S.getter("memOcto"));
  public static final Attribute<AttributeOption> LINE_ATTR =
      Attributes.forOption("line", S.getter("memLineSize"), new AttributeOption[] {SINGLE, DUAL, QUAD, OCTO});
  public static final Attribute<Boolean> ALLOW_MISALIGNED = Attributes.forBoolean("misaligned", S.getter("memMisaligned"));
  static final AttributeOption WRITEAFTERREAD = new AttributeOption("war", S.getter("memWar"));
  static final AttributeOption READAFTERWRITE = new AttributeOption("raw", S.getter("memRaw"));
  static final Attribute<AttributeOption> READ_ATTR = Attributes.forOption("readbehav", S.getter("memReadBehav"),
               new AttributeOption[] {WRITEAFTERREAD, READAFTERWRITE});
  public static final AttributeOption USEBYTEENABLES = new AttributeOption("byte", S.getter("memByte"));
  public static final AttributeOption USELINEENABLES = new AttributeOption("line", S.getter("memLine"));
  public static final Attribute<AttributeOption> ENABLES_ATTR = Attributes.forOption("enables", S.getter("memEnables"),
               new AttributeOption[] {USEBYTEENABLES, USELINEENABLES});
  static final Attribute<Boolean> ASYNC_READ = Attributes.forBoolean("asyncread", S.getter("memAsyncRead"));

  // other constants
  public static final int DELAY = 10;

  private final WeakHashMap<Instance, File> currentInstanceFiles;

  Mem(String name, StringGetter desc, int extraPorts, HdlGeneratorFactory generator, boolean needsLabel) {
    super(name, desc, generator, needsLabel);
    currentInstanceFiles = new WeakHashMap<>();
    setInstancePoker(MemPoker.class);
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(ADDR_ATTR, 2, 24, 0), new BitWidthConfigurator(DATA_ATTR)));

    setOffsetBounds(Bounds.create(-140, -40, 140, 80));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    final var bds = instance.getBounds();
    final var x = bds.getX() + bds.getWidth() / 2;
    final var y = bds.getY() - 2;
    final var halign = GraphicsUtil.H_CENTER;
    final var valign = GraphicsUtil.V_BOTTOM;
    instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign, valign);
  }

  abstract void configurePorts(Instance instance);

  @Override
  public abstract AttributeSet createAttributeSet();

  public File getCurrentImage(Instance instance) {
    return currentInstanceFiles.get(instance);
  }

  abstract HexFrame getHexFrame(Project proj, Instance instance, CircuitState state);

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    return (key == MenuExtender.class)
        ? new MemMenu(this, instance)
        : super.getInstanceFeature(instance, key);
  }

  protected static String getSizeLabel(int NrAddressBits) {
    String[] labels = {"", "K", "M", "G", "T", "P", "E"};
    var pass = 0;
    var addrBits = NrAddressBits;
    while (addrBits > 9) {
      pass++;
      addrBits -= 10;
    }
    int size = 1 << addrBits;
    return size + labels[pass];
  }

  abstract MemState getState(Instance instance, CircuitState state);

  abstract MemState getState(InstanceState state);

  @Override
  public abstract void propagate(InstanceState state);

  public void setCurrentImage(Instance instance, File value) {
    currentInstanceFiles.put(instance, value);
  }

}
