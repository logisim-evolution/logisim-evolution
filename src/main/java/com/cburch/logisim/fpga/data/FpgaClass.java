/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.util.StringUtil;
import java.util.Iterator;
import java.util.List;

public class FpgaClass {
  public static char getId(String identifier) {
    char result = 0;
    List<String> thelist = VendorSoftware.getVendorStrings();
    Iterator<String> iter = thelist.iterator();
    result = 0;
    while (iter.hasNext()) {
      if (iter.next().equalsIgnoreCase(identifier)) return result;
      result++;
    }
    return VendorSoftware.VENDOR_UNKNOWN;
  }

  private long clockFrequency;
  private String clockPinLocation;
  private char clockPullBehavior;
  private char clockIOStandard;
  private String technology;
  private String part;
  private String Package;
  private String speedGrade;
  private char vendor;
  private char unusedPinsBehavior;
  private boolean fpgaDefined;

  private boolean usbTmcDownload;

  private int jtagPos;

  private String flashName;
  private int flashPos;
  private boolean flashDefined;

  public FpgaClass() {
    clockFrequency = 0;
    clockPinLocation = null;
    clockPullBehavior = 0;
    clockIOStandard = 0;
    technology = null;
    part = null;
    Package = null;
    speedGrade = null;
    vendor = 0;
    fpgaDefined = false;
    unusedPinsBehavior = 0;
    usbTmcDownload = false;
    jtagPos = 1;
    flashName = null;
    flashPos = 2;
    flashDefined = false;
  }

  public void clear() {
    clockFrequency = 0;
    clockPinLocation = null;
    clockPullBehavior = 0;
    clockIOStandard = 0;
    technology = null;
    part = null;
    Package = null;
    speedGrade = null;
    vendor = 0;
    fpgaDefined = false;
    unusedPinsBehavior = 0;
    usbTmcDownload = false;
    jtagPos = 1;
    flashName = null;
    flashPos = 2;
    flashDefined = false;
  }

  public boolean isFpgaInfoPresent() {
    return fpgaDefined;
  }

  public long getClockFrequency() {
    return clockFrequency;
  }

  public String getClockPinLocation() {
    return clockPinLocation;
  }

  public char getClockPull() {
    return clockPullBehavior;
  }

  public char getClockStandard() {
    return clockIOStandard;
  }

  public String getPackage() {
    return Package;
  }

  public String getPart() {
    return part;
  }

  public String getSpeedGrade() {
    return speedGrade;
  }

  public String getTechnology() {
    return technology;
  }

  public char getUnusedPinsBehavior() {
    return unusedPinsBehavior;
  }

  public char getVendor() {
    return vendor;
  }

  public int getFpgaJTAGChainPosition() {
    return jtagPos;
  }

  public void set(
      long frequency,
      String pin,
      String pull,
      String standard,
      String tech,
      String device,
      String box,
      String speed,
      String vend,
      String unused,
      boolean usbTmc,
      String jtagPPos,
      String flashName,
      String flashPos) {
    clockFrequency = frequency;
    clockPinLocation = pin;
    clockPullBehavior = PullBehaviors.getId(pull);
    clockIOStandard = IoStandards.getId(standard);
    technology = tech;
    part = device;
    Package = box;
    speedGrade = speed;
    vendor = getId(vend);
    fpgaDefined = true;
    unusedPinsBehavior = PullBehaviors.getId(unused);
    usbTmcDownload = usbTmc;
    this.jtagPos = Integer.parseInt(jtagPPos);
    this.flashName = flashName;
    this.flashPos = Integer.parseInt(flashPos);
    this.flashDefined = StringUtil.isNotEmpty(flashName) && (this.flashPos != 0);
  }

  public Boolean isUsbTmcDownloadRequired() {
    return usbTmcDownload;
  }

  public String getFlashName() {
    return flashName;
  }

  public int getFlashJTAGChainPosition() {
    return flashPos;
  }

  public boolean isFlashDefined() {
    return flashDefined;
  }
}
