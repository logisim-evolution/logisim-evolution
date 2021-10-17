/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.vga;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusMasterInterface;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSlaveListener;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.util.StringUtil;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class VgaState implements SocBusSlaveInterface, SocBusSnifferInterface, SocBusMasterInterface {

  public class VgaDisplayState implements InstanceData, Cloneable {

    private int mode, lineSize, nrOfLines;
    private boolean modeSetBySoftware = false;
    private BufferedImage myImage;
    private boolean reload = true;

    public VgaDisplayState() {
      mode = displayMode;
      sizeChanged(true);
    }

    public int getMode() {
      if (modeSetBySoftware)
        return mode;
      else
        return displayMode;
    }

    public boolean setSoftMode(int softMode) {
      modeSetBySoftware = true;
      if (softMode != mode) {
        mode = softMode;
        return sizeChanged(false);
      }
      return false;
    }

    public BufferedImage getImage(CircuitState cState) {
      loadImage(cState);
      return myImage;
    }

    public boolean sizeChanged(boolean initialSize) {
      if (initialSize && modeSetBySoftware)
        return false;
      clear();
      switch (getMode()) {
        case VgaAttributes.MODE_160_120:
          lineSize = 160;
          nrOfLines = 120;
          break;
        case VgaAttributes.MODE_320_240:
          lineSize = 320;
          nrOfLines = 240;
          break;
        case VgaAttributes.MODE_640_480:
          lineSize = 640;
          nrOfLines = 480;
          break;
        case VgaAttributes.MODE_800_600:
          lineSize = 800;
          nrOfLines = 600;
          break;
        case VgaAttributes.MODE_1024_768:
          lineSize = 1024;
          nrOfLines = 768;
          break;
      }
      myImage = new BufferedImage(lineSize, nrOfLines, BufferedImage.TYPE_INT_RGB);
      return true;
    }

    public int getDataSize() {
      if (myImage == null)
        return 0;
      else
        return myImage.getHeight() * myImage.getWidth();
    }

    public void clear() {
      reload = true;
    }

    @Override
    public VgaDisplayState clone() {
      try {
        return (VgaDisplayState) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    private void loadImage(CircuitState cState) {
      if (reload) {
        for (int line = 0; line < nrOfLines; line++)
          for (int pixel = 0; pixel < lineSize; pixel++) {
            int index = line * lineSize + pixel;
            SocBusTransaction trans =
                new SocBusTransaction(
                    SocBusTransaction.READ_TRANSACTION,
                    vgaBufferAddress + index * 4,
                    0,
                    SocBusTransaction.WORD_ACCESS,
                    "vgadma");
            trans.setAsHiddenTransaction();
            initializeTransaction(trans, attachedBus.getBusId(), cState);
            int data = trans.hasError() ? 0 : trans.getReadData();
            myImage.setRGB(pixel, line, data);
          }
        reload = false;
      }
    }

    public void paint(Graphics g, CircuitState cState) {
      loadImage(cState);
      g.drawImage(myImage, LEFT_MARGIN, TOP_MARGIN, null);
    }
  }

  public static final int TOP_MARGIN = 20;
  public static final int BOTTOM_MARGIN = 20;
  public static final int LEFT_MARGIN = 5;
  public static final int RIGHT_MARGIN = 5;

  private Integer startAddress = 0;
  private Integer vgaBufferAddress = 0;
  private int displayMode = VgaAttributes.MODE_160_120;
  private final SocBusInfo attachedBus = new SocBusInfo("");
  private String label = "";
  private Boolean soft160x120 = true;
  private Boolean soft320x240 = true;
  private Boolean soft640x480 = true;
  private Boolean soft800x600 = false;
  private Boolean soft1024x768 = false;
  private final ArrayList<SocBusSlaveListener> listeners = new ArrayList<>();

  public AttributeOption getInitialMode() {
    return VgaAttributes.MODES.get(displayMode);
  }

  public AttributeOption getCurrentMode() {
    VgaDisplayState data = getRegCurrentState();
    int mode = (data == null) ?  displayMode : data.getMode();
    return VgaAttributes.MODES.get(mode);
  }

  public Integer getVgaBufferStartAddress() {
    return vgaBufferAddress;
  }

  public String getLabel() {
    return label;
  }

  public SocBusInfo getBusInfo() {
    return attachedBus;
  }

  public Boolean getSoft160x120() {
    return soft160x120;
  }

  public Boolean getSoft320x240() {
    return soft320x240;
  }

  public Boolean getSoft640x480() {
    return soft640x480;
  }

  public Boolean getSoft800x600() {
    return soft800x600;
  }

  public Boolean getSoft1024x768() {
    return soft1024x768;
  }

  public boolean setStartAddress(Integer value) {
    if (startAddress == value)
      return false;
    startAddress = value;
    firememMapChanged();
    return true;
  }

  public boolean setInitialMode(AttributeOption value) {
    int mode = VgaAttributes.MODES.contains(value) ? VgaAttributes.MODES.indexOf(value) : 0;
    if (displayMode == mode)
      return false;
    displayMode = mode;
    VgaDisplayState data = getRegCurrentState();
    if (data != null)
      if (data.sizeChanged(true)) {
        InstanceComponent comp = (InstanceComponent) attachedBus.getComponent();
        ((SocVga) comp.getFactory()).setTextField(comp.getInstance());
        comp.getInstance().fireInvalidated();
      }
    return true;
  }

  public boolean setVgaBufferStartAddress(Integer value) {
    if (value == vgaBufferAddress)
      return false;
    vgaBufferAddress = value;
    VgaDisplayState data = getRegCurrentState();
    if (data != null) {
      data.clear();
    }
    return true;
  }

  public boolean setLabel(String l) {
    if (label.equals(l))
      return false;
    label = l;
    fireNameChanged();
    return true;
  }

  public boolean setBusInfo(SocBusInfo bus) {
    if (bus.getBusId().equals(attachedBus.getBusId()))
      return false;
    attachedBus.setBusId(bus.getBusId());
    VgaDisplayState data = getRegCurrentState();
    if (data != null) {
      data.clear();
    }
    return true;
  }

  public boolean setSoft160x120(Boolean b) {
    if (soft160x120 == b)
      return false;
    soft160x120 = b;
    return true;
  }

  public boolean setSoft320x240(Boolean b) {
    if (soft320x240 == b)
      return false;
    soft320x240 = b;
    return true;
  }

  public boolean setSoft640x480(Boolean b) {
    if (soft640x480 == b)
      return false;
    soft640x480 = b;
    return true;
  }

  public boolean setSoft800x600(Boolean b) {
    if (soft800x600 == b)
      return false;
    soft800x600 = b;
    return true;
  }

  public boolean setSoft1024x768(Boolean b) {
    if (soft1024x768 == b)
      return false;
    soft1024x768 = b;
    return true;
  }

  public void copyInto(VgaState dest) {
    dest.setStartAddress(getStartAddress());
    dest.setInitialMode(getInitialMode());
    dest.soft160x120 = soft160x120;
    dest.soft320x240 = soft320x240;
    dest.soft640x480 = soft640x480;
    dest.soft800x600 = soft800x600;
    dest.soft1024x768 = soft1024x768;
    dest.setVgaBufferStartAddress(getVgaBufferStartAddress());
    dest.setLabel(getLabel());
    dest.setBusInfo(getBusInfo());
  }

  public static Bounds getSize(int mode) {
    switch (mode) {
      case VgaAttributes.MODE_320_240:
        return Bounds.create(0, 0, LEFT_MARGIN + 320 + RIGHT_MARGIN, TOP_MARGIN + 240 + BOTTOM_MARGIN);
      case VgaAttributes.MODE_640_480:
        return Bounds.create(0, 0, LEFT_MARGIN + 640 + RIGHT_MARGIN, TOP_MARGIN + 480 + BOTTOM_MARGIN);
      case VgaAttributes.MODE_800_600:
        return Bounds.create(0, 0, LEFT_MARGIN + 800 + RIGHT_MARGIN, TOP_MARGIN + 600 + BOTTOM_MARGIN);
      case VgaAttributes.MODE_1024_768:
        return Bounds.create(0, 0, LEFT_MARGIN + 1024 + RIGHT_MARGIN, TOP_MARGIN + 768 + BOTTOM_MARGIN);
      default:
        return Bounds.create(0, 0, LEFT_MARGIN + 160 + RIGHT_MARGIN, TOP_MARGIN + 120 + BOTTOM_MARGIN);
    }
  }

  public VgaDisplayState getNewState() {
    return new VgaDisplayState();
  }

  public VgaDisplayState getRegCurrentState() {
    InstanceComponent comp = (InstanceComponent) attachedBus.getComponent();
    if (comp == null)
      return null;
    InstanceState state = comp.getInstanceStateImpl();
    if (state == null)
      return null;
    return (VgaDisplayState) state.getProject().getCircuitState().getData(comp);
  }

  public VgaDisplayState getRegPropagateState() {
    return (VgaDisplayState) attachedBus.getSocSimulationManager().getdata(attachedBus.getComponent());
  }

  /* here all Socbus interface handles are defined */
  @Override
  public void initializeTransaction(SocBusTransaction trans, String busId, CircuitState cState) {
    if (attachedBus.getSocSimulationManager() == null)
      return;
    attachedBus.getSocSimulationManager().initializeTransaction(trans, busId, cState);
  }

  @Override
  public void sniffTransaction(SocBusTransaction trans) {
    if (!trans.isWriteTransaction())
      return;
    long start = SocSupport.convUnsignedInt(vgaBufferAddress);
    VgaDisplayState state = getRegPropagateState();
    if (state == null)
      return;
    long end = start + state.getDataSize() * 4;
    long addr = SocSupport.convUnsignedInt(trans.getAddress());
    if (addr >= start && addr < end) {
      int index = SocSupport.convUnsignedLong(addr - start) >> 2;
      state.myImage.setRGB(index % state.lineSize, index / state.lineSize, trans.getWriteData());
    }
  }

  @Override
  public boolean canHandleTransaction(SocBusTransaction trans) {
    long addr = SocSupport.convUnsignedInt(trans.getAddress());
    long start = SocSupport.convUnsignedInt(startAddress);
    long end = startAddress + 4;
    return addr >= start && addr < end;
  }

  @Override
  public void handleTransaction(SocBusTransaction trans) {
    if (!canHandleTransaction(trans))
      return;
    trans.setTransactionResponder(attachedBus.getComponent());
    if (trans.getAccessType() != SocBusTransaction.WORD_ACCESS) {
      trans.setError(SocBusTransaction.ACCESS_TYPE_NOT_SUPPORTED_ERROR);
      return;
    }
    if (trans.isReadTransaction()) {
      int data = 0;
      if (soft160x120) data |= VgaAttributes.MODE_160_120_MASK;
      if (soft320x240) data |= VgaAttributes.MODE_320_240_MASK;
      if (soft640x480) data |= VgaAttributes.MODE_640_480_MASK;
      if (soft800x600) data |= VgaAttributes.MODE_800_600_MASK;
      if (soft1024x768) data |= VgaAttributes.MODE_1024_768_MASK;
      trans.setReadData(data);
    }
    if (trans.isWriteTransaction()) {
      int mode = displayMode;
      int data = trans.getWriteData();
      if (data == VgaAttributes.MODE_160_120_MASK && soft160x120) mode = VgaAttributes.MODE_160_120;
      if (data == VgaAttributes.MODE_320_240_MASK && soft320x240) mode = VgaAttributes.MODE_320_240;
      if (data == VgaAttributes.MODE_640_480_MASK && soft640x480) mode = VgaAttributes.MODE_640_480;
      if (data == VgaAttributes.MODE_800_600_MASK && soft800x600) mode = VgaAttributes.MODE_800_600;
      if (data == VgaAttributes.MODE_1024_768_MASK && soft1024x768) mode = VgaAttributes.MODE_1024_768;
      VgaDisplayState disp = getRegPropagateState();
      if (disp != null && disp.setSoftMode(mode)) {
        InstanceComponent comp = (InstanceComponent) attachedBus.getComponent();
        ((SocVga) comp.getFactory()).setTextField(comp.getInstance());
        comp.getInstance().fireInvalidated();
      }
    }
  }

  @Override
  public Integer getStartAddress() {
    return startAddress;
  }

  @Override
  public Integer getMemorySize() {
    return 4;
  }

  @Override
  public String getName() {
    var name = label;
    if (StringUtil.isNullOrEmpty(name)) {
      final var loc = attachedBus.getComponent().getLocation();
      name = attachedBus.getComponent().getFactory().getDisplayName() + "@" + loc.getX() + "," + loc.getY();
    }
    return name;
  }

  @Override
  public void registerListener(SocBusSlaveListener l) {
    if (!listeners.contains(l)) listeners.add(l);
  }

  @Override
  public void removeListener(SocBusSlaveListener l) {
    listeners.remove(l);
  }

  @Override
  public InstanceComponent getComponent() {
    return (InstanceComponent) attachedBus.getComponent();
  }

  private void fireNameChanged() {
    for (SocBusSlaveListener l : listeners)
      l.labelChanged();
  }

  private void firememMapChanged() {
    for (SocBusSlaveListener l : listeners)
      l.memoryMapChanged();
  }

}
