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
 *   http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *   http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *   http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *   http://www.heig-vd.ch/
 */

package com.cburch.logisim.fpga.data;

import static com.cburch.logisim.fpga.Strings.S;

import java.util.EnumSet;

import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.RGBLed;
import com.cburch.logisim.std.io.ReptarLocalBus;

public enum IOComponentTypes {
  LED,
  Button,
  Pin,
  SevenSegment,
  SevenSegmentNoDp,
  DIPSwitch,
  RGBLED,
  PortIO,
  LocalBus,
  Bus,
  Open,
  Constant,
  Unknown;

  /* 
   * Important note:
   * The number of pins specified in this enum are the defaults used when
   * using the board editor. The real number of pins are read from the
   * xml file and stored inside the FPGAIOInformation container.
   * 
   * Bus is just a placeholder for a multi-bit pin. It should not be used for
   * mappable components
   * 
   * Open and constant are used in the map dialog to be able to map IOcomponents
   * to a constant or a open (hence no connection external of the FPGA/CPLD).
   */

  public static IOComponentTypes getEnumFromString(String str) {
    for (IOComponentTypes elem : KnownComponentSet) {
    if (elem.name().equalsIgnoreCase(str)) {
      return elem;
    }
    }
    return IOComponentTypes.Unknown;
  }

  public static final int GetFPGAInOutRequirement(IOComponentTypes comp) {
    switch (comp) {
    case PortIO:
      return 8;
    case LocalBus:
      return 16;
    default:
      return 0;
    }
  }

  public static final int GetFPGAInputRequirement(IOComponentTypes comp) {
    switch (comp) {
    case Button:
      return 1;
    case DIPSwitch:
      return 8;
    case LocalBus:
      return 13;
    default:
      return 0;
    }
  }

  public static final int GetFPGAOutputRequirement(IOComponentTypes comp) {
    switch (comp) {
    case LED:
      return 1;
    case SevenSegment:
      return 8;
    case SevenSegmentNoDp:
      return 7;
    case RGBLED:
      return 3;
    case LocalBus:
      return 2;
    default:
      return 0;
    }
  }
  
  public static final boolean nrOfInputPinsConfigurable(IOComponentTypes comp) {
    return comp.equals(DIPSwitch);
  }

  public static final boolean nrOfOutputPinsConfigurable(IOComponentTypes comp) {
    return false;
  }

  public static final boolean nrOfIOPinsConfigurable(IOComponentTypes comp) {
    return comp.equals(PortIO);
  }
  
  public static final String getInputLabel(int nrPins, int id, IOComponentTypes comp) {
    switch (comp) {
      case DIPSwitch : return DipSwitch.getInputLabel(id);
      case LocalBus  : return ReptarLocalBus.getInputLabel(id);
      default        : return (nrPins > 1) ? S.fmt("FpgaIoPins", id) : S.get("FpgaIoPin");
    }
  }
  
  public static final String getOutputLabel(int nrPins, int id, IOComponentTypes comp) {
    switch (comp) {
      case SevenSegmentNoDp : 
      case SevenSegment     : return com.cburch.logisim.std.io.SevenSegment.getOutputLabel(id);
      case RGBLED           : return RGBLed.getLabel(id);
      case LocalBus         : return ReptarLocalBus.getOutputLabel(id);
      default               : return (nrPins > 1) ? S.fmt("FpgaIoPins", id) : S.get("FpgaIoPin");
    }
  }
  
  public static final String getIOLabel(int nrPins, int id, IOComponentTypes comp) {
    switch (comp) {
      case LocalBus : return ReptarLocalBus.getIOLabel(id);
      default       : return (nrPins > 1) ? S.fmt("FpgaIoPins", id) : S.get("FpgaIoPin");
    }
  }
  
  public static final int GetNrOfFPGAPins(IOComponentTypes comp) {
    return  GetFPGAInOutRequirement(comp)+ GetFPGAInputRequirement(comp)+GetFPGAOutputRequirement(comp);
  }

  public static final EnumSet<IOComponentTypes> KnownComponentSet =
    EnumSet.range(IOComponentTypes.LED, IOComponentTypes.LocalBus);

  public static final EnumSet<IOComponentTypes> SimpleInputSet =
    EnumSet.range(IOComponentTypes.LED, IOComponentTypes.LocalBus);

  public static final EnumSet<IOComponentTypes> InputComponentSet =
    EnumSet.of(IOComponentTypes.Button, IOComponentTypes.Pin, IOComponentTypes.DIPSwitch);

  public static final EnumSet<IOComponentTypes> OutputComponentSet =
    EnumSet.of(
      IOComponentTypes.LED,
      IOComponentTypes.Pin,
      IOComponentTypes.RGBLED,
      IOComponentTypes.SevenSegment,
      IOComponentTypes.SevenSegmentNoDp);

  public static final EnumSet<IOComponentTypes> InOutComponentSet =
    EnumSet.of(IOComponentTypes.Pin, IOComponentTypes.PortIO);

}
