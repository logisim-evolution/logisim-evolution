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
import lombok.Getter;

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

  @Getter private long clockFrequency;
  @Getter private String clockPinLocation;
  @Getter private char clockPullBehavior;
  @Getter private char clockStandard;
  @Getter private String technology;
  @Getter private String part;
  private String Package;
  @Getter private String speedGrade;
  @Getter private char vendor;
  @Getter private char unusedPinsBehavior;
  @Getter private boolean fpgaInfoPresent;

  @Getter private boolean usbTmcDownloadRequired;

  @Getter private int fpgaJtagChainPosition;

  @Getter private String flashName;
  @Getter private int flashJtagChainPosition;
  @Getter private boolean flashDefined;

  public FpgaClass() {
    clockFrequency = 0;
    clockPinLocation = null;
    clockPullBehavior = 0;
    clockStandard = 0;
    technology = null;
    part = null;
    Package = null;
    speedGrade = null;
    vendor = 0;
    fpgaInfoPresent = false;
    unusedPinsBehavior = 0;
    usbTmcDownloadRequired = false;
    fpgaJtagChainPosition = 1;
    flashName = null;
    flashJtagChainPosition = 2;
    flashDefined = false;
  }

  public void clear() {
    clockFrequency = 0;
    clockPinLocation = null;
    clockPullBehavior = 0;
    clockStandard = 0;
    technology = null;
    part = null;
    Package = null;
    speedGrade = null;
    vendor = 0;
    fpgaInfoPresent = false;
    unusedPinsBehavior = 0;
    usbTmcDownloadRequired = false;
    fpgaJtagChainPosition = 1;
    flashName = null;
    flashJtagChainPosition = 2;
    flashDefined = false;
  }

  public String getPackage() {
    return Package;
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
    clockStandard = IoStandards.getId(standard);
    technology = tech;
    part = device;
    Package = box;
    speedGrade = speed;
    vendor = getId(vend);
    fpgaInfoPresent = true;
    unusedPinsBehavior = PullBehaviors.getId(unused);
    usbTmcDownloadRequired = usbTmc;
    this.fpgaJtagChainPosition = Integer.parseInt(jtagPPos);
    this.flashName = flashName;
    this.flashJtagChainPosition = Integer.parseInt(flashPos);
    this.flashDefined = StringUtil.isNotEmpty(flashName) && (this.flashJtagChainPosition != 0);
  }

}
