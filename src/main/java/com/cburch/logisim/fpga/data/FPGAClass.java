/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
      if (iter.next().toUpperCase().equals(identifier.toUpperCase())) return result;
      result++;
    }
    return VendorSoftware.VendorUnknown;
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
    this.JTAGPos = Integer.valueOf(JTAGPos);
    this.FlashName = flashName;
    this.FlashPos = Integer.valueOf(flashPos);
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
