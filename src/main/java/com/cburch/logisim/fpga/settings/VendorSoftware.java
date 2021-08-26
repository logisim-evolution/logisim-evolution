/*
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

package com.cburch.logisim.fpga.settings;

import com.cburch.logisim.prefs.AppPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

public class VendorSoftware {
  public static final char VENDOR_ALTERA = 0;
  public static final char VENDOR_XILINX = 1;
  public static final char VENDOR_VIVADO = 2;
  public static final char VENDOR_UNKNOWN = 255;
  public static final String[] VENDORS = {"Altera", "Xilinx", "Vivado"};

  private static final String XilinxName = "XilinxToolsPath";
  private static final String AlteraName = "AlteraToolsPath";
  private static final String VivadoName = "VivadoToolsPath";
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
    switch (vendor) {
      case VENDOR_ALTERA:
        return AppPreferences.QuartusToolPath.get();
      case VENDOR_XILINX:
        return AppPreferences.ISEToolPath.get();
      case VENDOR_VIVADO:
        return AppPreferences.VivadoToolPath.get();
    }
    return "Unknown";
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

  public static LinkedList<String> getVendorStrings() {
    LinkedList<String> result = new LinkedList<>();

    result.add(VendorSoftware.VENDORS[0]);
    result.add(VendorSoftware.VENDORS[1]);
    result.add(VendorSoftware.VENDORS[2]);

    return result;
  }

  public static String getVendorString(char vendor) {
    switch (vendor) {
      case VENDOR_ALTERA:
        return VENDORS[0];
      case VENDOR_XILINX:
        return VENDORS[1];
      case VENDOR_VIVADO:
        return VENDORS[2];
      default:
        return "Unknown";
    }
  }

  public static VendorSoftware getSoftware(char vendor) {
    switch (vendor) {
      case VENDOR_ALTERA:
        return new VendorSoftware(VENDOR_ALTERA, AlteraName, load(VENDOR_ALTERA));
      case VENDOR_XILINX:
        return new VendorSoftware(VENDOR_XILINX, XilinxName, load(VENDOR_XILINX));
      case VENDOR_VIVADO:
        return new VendorSoftware(VENDOR_VIVADO, VivadoName, load(VENDOR_VIVADO));
      default:
        return null;
    }
  }

  public static String GetToolPath(char vendor) {
    switch (vendor) {
      case VENDOR_ALTERA:
        return AppPreferences.QuartusToolPath.get();
      case VENDOR_XILINX:
        return AppPreferences.ISEToolPath.get();
      case VENDOR_VIVADO:
        return AppPreferences.VivadoToolPath.get();
      default:
        return null;
    }
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
      default:
        return false;
    }
  }

  private static String CorrectPath(String path) {
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
      File test = new File(CorrectPath(path + tool));
      if (!test.exists())
        return false;
    }
    return true;
  }
}
