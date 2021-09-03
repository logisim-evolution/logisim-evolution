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
import java.util.Iterator;
import java.util.LinkedList;

public class FPGAClass {
  public static char getId(String identifier) {
    char result = 0;
    LinkedList<String> thelist = VendorSoftware.getVendorStrings();
    Iterator<String> iter = thelist.iterator();
    result = 0;
    while (iter.hasNext()) {
      if (iter.next().equalsIgnoreCase(identifier)) return result;
      result++;
    }
    return VendorSoftware.VENDOR_UNKNOWN;
  }

  private long ClockFrequency;
  private String ClockPinLocation;
  private char ClockPullBehavior;
  private char ClockIOStandard;
  private String Technology;
  private String Part;
  private String Package;
  private String SpeedGrade;
  private char Vendor;
  private char UnusedPinsBehavior;
  private boolean FPGADefined;

  private boolean USBTMCDownload;

  private int JTAGPos;

  private String FlashName;
  private int FlashPos;
  private boolean FlashDefined;

  public FPGAClass() {
    ClockFrequency = 0;
    ClockPinLocation = null;
    ClockPullBehavior = 0;
    ClockIOStandard = 0;
    Technology = null;
    Part = null;
    Package = null;
    SpeedGrade = null;
    Vendor = 0;
    FPGADefined = false;
    UnusedPinsBehavior = 0;
    USBTMCDownload = false;
    JTAGPos = 1;
    FlashName = null;
    FlashPos = 2;
    FlashDefined = false;
  }

  public void clear() {
    ClockFrequency = 0;
    ClockPinLocation = null;
    ClockPullBehavior = 0;
    ClockIOStandard = 0;
    Technology = null;
    Part = null;
    Package = null;
    SpeedGrade = null;
    Vendor = 0;
    FPGADefined = false;
    UnusedPinsBehavior = 0;
    USBTMCDownload = false;
    JTAGPos = 1;
    FlashName = null;
    FlashPos = 2;
    FlashDefined = false;
  }

  public boolean FpgaInfoPresent() {
    return FPGADefined;
  }

  public long getClockFrequency() {
    return ClockFrequency;
  }

  public String getClockPinLocation() {
    return ClockPinLocation;
  }

  public char getClockPull() {
    return ClockPullBehavior;
  }

  public char getClockStandard() {
    return ClockIOStandard;
  }

  public String getPackage() {
    return Package;
  }

  public String getPart() {
    return Part;
  }

  public String getSpeedGrade() {
    return SpeedGrade;
  }

  public String getTechnology() {
    return Technology;
  }

  public char getUnusedPinsBehavior() {
    return UnusedPinsBehavior;
  }

  public char getVendor() {
    return Vendor;
  }

  public int getFpgaJTAGChainPosition() {
    return JTAGPos;
  }

  public void Set(
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
      boolean UsbTmc,
      String JTAGPos,
      String flashName,
      String flashPos) {
    ClockFrequency = frequency;
    ClockPinLocation = pin;
    ClockPullBehavior = PullBehaviors.getId(pull);
    ClockIOStandard = IoStandards.getId(standard);
    Technology = tech;
    Part = device;
    Package = box;
    SpeedGrade = speed;
    Vendor = getId(vend);
    FPGADefined = true;
    UnusedPinsBehavior = PullBehaviors.getId(unused);
    USBTMCDownload = UsbTmc;
    this.JTAGPos = Integer.parseInt(JTAGPos);
    this.FlashName = flashName;
    this.FlashPos = Integer.parseInt(flashPos);
    this.FlashDefined = (flashName != null) && (!flashName.isEmpty()) && (this.FlashPos != 0);
  }

  public Boolean USBTMCDownloadRequired() {
    return USBTMCDownload;
  }

  public String getFlashName() {
    return FlashName;
  }

  public int getFlashJTAGChainPosition() {
    return FlashPos;
  }

  public boolean isFlashDefined() {
    return FlashDefined;
  }
}
