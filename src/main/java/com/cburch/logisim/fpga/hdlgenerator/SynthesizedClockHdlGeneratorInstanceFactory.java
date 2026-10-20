package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.fpga.settings.VendorSoftware;

public class SynthesizedClockHdlGeneratorInstanceFactory {
  public static SynthesizedClockHdlGeneratorFactory getSynthesizedClockHdlGeneratorFactory(
      String technology,
      char vendor,
      boolean clockScalingRequested,
      long clockFrequency,
      double preMultiplier,
      double preDivider) throws Exception {
    if (technology.endsWith("-7") && vendor == VendorSoftware.VENDOR_VIVADO && clockScalingRequested) {
      return new XilinxSeries7SynthesizedClockHdlGeneratorFactory(
        clockFrequency,
        preMultiplier,
        preDivider);
    } else {
      return new SynthesizedClockHdlGeneratorFactory();
    }
  }

  public static boolean isClockScalingSupported(String technology, char vendor) {
    if (technology.endsWith("-7") && vendor == VendorSoftware.VENDOR_VIVADO) {
      return true;
    } else {
      return false;
    }
  }
}
