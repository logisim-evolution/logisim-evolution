/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.settings;

import com.cburch.logisim.prefs.AppPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VendorSoftware {
  public static final char VENDOR_ALTERA = 0;
  public static final char VENDOR_XILINX = 1;
  public static final char VENDOR_VIVADO = 2;
  public static final char VENDOR_OPENFPGA = 3;
  public static final char VENDOR_UNKNOWN = 255;
  public static final String[] VENDORS = {"Altera", "Xilinx", "Vivado", "openFPGA"};

  private static final String XilinxName = "XilinxToolsPath";
  private static final String AlteraName = "AlteraToolsPath";
  private static final String VivadoName = "VivadoToolsPath";
  private static final String OpenFpgaName = "OpenFpgaToolsPath";
  public static final String UNKNOWN = "Unknown";

  private final char vendor;
  private final String name;
  private final String[] bin;

  public VendorSoftware(char vendor, String name, String[] bin) {
    this.vendor = vendor;
    this.name = name;
    this.bin = bin;
  }

  public String getToolPath() {
    return switch (vendor) {
      case VENDOR_ALTERA -> AppPreferences.QuartusToolPath.get();
      case VENDOR_XILINX -> AppPreferences.ISEToolPath.get();
      case VENDOR_VIVADO -> AppPreferences.VivadoToolPath.get();
      case VENDOR_OPENFPGA -> AppPreferences.OpenFpgaToolPath.get();
      default -> "Unknown";
    };
  }

  public String getName() {
    return name;
  }

  public char getVendor() {
    return vendor;
  }

  public String[] getBinaries() {
    return bin;
  }

  public String getBinaryPath(int binPos) {
    return getToolPath() + File.separator + bin[binPos];
  }

  public static List<String> getVendorStrings() {
    LinkedList<String> result = new LinkedList<>();

    result.add(VendorSoftware.VENDORS[0]);
    result.add(VendorSoftware.VENDORS[1]);
    result.add(VendorSoftware.VENDORS[2]);
    result.add(VendorSoftware.VENDORS[3]);

    return result;
  }

  public static String getVendorString(char vendor) {
    return switch (vendor) {
      case VENDOR_ALTERA -> VENDORS[0];
      case VENDOR_XILINX -> VENDORS[1];
      case VENDOR_VIVADO -> VENDORS[2];
      case VENDOR_OPENFPGA -> VENDORS[3];
      default -> "Unknown";
    };
  }

  public static VendorSoftware getSoftware(char vendor) {
    return switch (vendor) {
      case VENDOR_ALTERA -> new VendorSoftware(VENDOR_ALTERA, AlteraName, load(VENDOR_ALTERA));
      case VENDOR_XILINX -> new VendorSoftware(VENDOR_XILINX, XilinxName, load(VENDOR_XILINX));
      case VENDOR_VIVADO -> new VendorSoftware(VENDOR_VIVADO, VivadoName, load(VENDOR_VIVADO));
      case VENDOR_OPENFPGA -> new VendorSoftware(VENDOR_OPENFPGA, OpenFpgaName, load(VENDOR_OPENFPGA));
      default -> null;
    };
  }

  public static String getToolPath(char vendor) {
    return switch (vendor) {
      case VENDOR_ALTERA -> AppPreferences.QuartusToolPath.get();
      case VENDOR_XILINX -> AppPreferences.ISEToolPath.get();
      case VENDOR_VIVADO -> AppPreferences.VivadoToolPath.get();
      case VENDOR_OPENFPGA -> AppPreferences.OpenFpgaToolPath.get();
      default -> null;
    };
  }

  public static boolean setToolPath(char vendor, String path) {
    if (!toolsPresent(vendor, path)) return false;
    switch (vendor) {
      case VENDOR_ALTERA:
        AppPreferences.QuartusToolPath.set(path);
        return true;
      case VENDOR_XILINX:
        AppPreferences.ISEToolPath.set(path);
        return true;
      case VENDOR_VIVADO:
        AppPreferences.VivadoToolPath.set(path);
        return true;
      case VENDOR_OPENFPGA:
        AppPreferences.OpenFpgaToolPath.set(path);
        return true;
      default:
        return false;
    }
  }

  private static String correctPath(String path) {
    if (path.endsWith(File.separator)) return path;
    else return path + File.separator;
  }

  private static String[] load(char vendor) {
    ArrayList<String> progs = new ArrayList<>();
    String windowsExtension = ".exe";
    if (vendor == VENDOR_ALTERA) {
      progs.add("quartus_sh");
      progs.add("quartus_pgm");
      progs.add("quartus_map");
      progs.add("quartus_cpf");
    } else if (vendor == VENDOR_XILINX) {
      progs.add("xst");
      progs.add("ngdbuild");
      progs.add("map");
      progs.add("par");
      progs.add("bitgen");
      progs.add("impact");
      progs.add("cpldfit");
      progs.add("hprep6");
    } else if (vendor == VENDOR_VIVADO) {
      progs.add("vivado");
      windowsExtension = ".bat";
    } else if (vendor == VENDOR_OPENFPGA) {
      progs.add("ghdl");
      progs.add("yosys");
      progs.add("nextpnr-ecp5");
      progs.add("ecppack");
      progs.add("openFPGALoader");
    }

    String[] progsArray = progs.toArray(new String[0]);
    String osname = System.getProperty("os.name");
    if (osname == null) throw new IllegalArgumentException("no os.name");
    else {
      if (osname.toLowerCase().contains("windows")) {
        for (int i = 0; i < progsArray.length; i++) {
          progsArray[i] += windowsExtension;
        }
      }
    }
    return progsArray;
  }

  public static boolean toolsPresent(char vendor, String path) {
    String[] tools = load(vendor);
    for (String tool : tools) {
      File test = new File(correctPath(path + tool));
      if (!test.exists())
        return false;
    }
    return true;
  }
}
