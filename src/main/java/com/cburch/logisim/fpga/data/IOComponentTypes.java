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

import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.RgbLed;
import com.cburch.logisim.std.io.ReptarLocalBus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.EnumSet;

public enum IOComponentTypes {
  LED,
  Button,
  Pin,
  SevenSegment,
  SevenSegmentNoDp,
  DIPSwitch,
  RGBLED,
  LEDArray,
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

  public static int GetFPGAInOutRequirement(IOComponentTypes comp) {
    switch (comp) {
      case PortIO:
        return 8;
      case LocalBus:
        return 16;
      default:
        return 0;
    }
  }

  public static int GetFPGAInputRequirement(IOComponentTypes comp) {
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

  public static int GetFPGAOutputRequirement(IOComponentTypes comp) {
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
      case LEDArray:
        return 16;
      default:
        return 0;
    }
  }

  public static boolean nrOfInputPinsConfigurable(IOComponentTypes comp) {
    return comp.equals(DIPSwitch);
  }

  public static boolean nrOfOutputPinsConfigurable(IOComponentTypes comp) {
    return false;
  }

  public static boolean nrOfIOPinsConfigurable(IOComponentTypes comp) {
    return comp.equals(PortIO);
  }

  public static String getInputLabel(int nrPins, int id, IOComponentTypes comp) {
    switch (comp) {
      case DIPSwitch : return DipSwitch.getInputLabel(id);
      case LocalBus  : return ReptarLocalBus.getInputLabel(id);
      default        : return (nrPins > 1) ? S.get("FpgaIoPins", id) : S.get("FpgaIoPin");
    }
  }

  public static String getOutputLabel(int nrPins, int nrOfRows, int nrOfColumns, int id, IOComponentTypes comp) {
    switch (comp) {
      case SevenSegmentNoDp:
      case SevenSegment:
        return com.cburch.logisim.std.io.SevenSegment.getOutputLabel(id);
      case RGBLED:
        return RgbLed.getLabel(id);
      case LocalBus:
        return ReptarLocalBus.getOutputLabel(id);
      case LEDArray: {
        if (nrOfRows != 0 && nrOfColumns != 0 && id >= 0 && id < nrPins) {
          int row = id / nrOfColumns;
          int col = id % nrOfColumns;
          return "Row_" + row + "_Col_" + col;
        }
      }
      default:
        return (nrPins > 1) ? S.get("FpgaIoPins", id) : S.get("FpgaIoPin");
    }
  }

  public static String getIOLabel(int nrPins, int id, IOComponentTypes comp) {
    if (comp == IOComponentTypes.LocalBus) {
      return ReptarLocalBus.getIOLabel(id);
    }
    return (nrPins > 1) ? S.get("FpgaIoPins", id) : S.get("FpgaIoPin");
  }

  public static int GetNrOfFPGAPins(IOComponentTypes comp) {
    return GetFPGAInOutRequirement(comp)
        + GetFPGAInputRequirement(comp)
        + GetFPGAOutputRequirement(comp);
  }
  
  public static void getPartialMapInfo(Integer[][] PartialMap,
      int width,
      int height,
      int nrOfPins,
      int nrOfRows,
      int nrOfColumns,
      IOComponentTypes type) {
    boolean hasDp = false;
    switch (type) {
      case DIPSwitch: {
        float part = (width > height) ? (float) width / (float) nrOfPins : (float) height / (float) nrOfPins;
        for (int w = 0; w < width; w++)
          for (int h = 0; h < height; h++) {
            float index = (width > height) ? (float) w / part : (float) h / part;
            PartialMap[w][h] = (int) index;
          }
        break;
      }
      case RGBLED: {
        float part = height / 3;
        for (int w = 0; w < width; w++)
          for (int h = 0; h < height; h++) 
            PartialMap[w][h] = (int) ((float) h / part);
        break;
      }
      case SevenSegment: hasDp = true;
      case SevenSegmentNoDp : {
        int sa = com.cburch.logisim.std.io.SevenSegment.Segment_A;
        int sb = com.cburch.logisim.std.io.SevenSegment.Segment_B;
        int sc = com.cburch.logisim.std.io.SevenSegment.Segment_C;
        int sd = com.cburch.logisim.std.io.SevenSegment.Segment_D;
        int se = com.cburch.logisim.std.io.SevenSegment.Segment_E;
        int sf = com.cburch.logisim.std.io.SevenSegment.Segment_F;
        int sg = com.cburch.logisim.std.io.SevenSegment.Segment_G;
        int[][] indexes = {
            {-1, sa, sa, -1, -1}, 
            {sf, -1, -1, sb, -1}, 
            {sf, -1, -1, sb, -1}, 
            {-1, sg, sg, -1, -1}, 
            {se, -1, -1, sc, -1}, 
            {se, -1, -1, sc, -1}, 
            {-1, sd, sd, -1, -1}
        };
        if (hasDp) indexes[6][4] = com.cburch.logisim.std.io.SevenSegment.DP;
        float partx = (width > height) ? (float) height / (float) 5.0 : (float) width / (float) 5.0;
        float party = (width > height) ? (float) width / (float) 7.0 : (float) height / (float) 7.0;
        for (int w = 0; w < width; w++)
          for (int h = 0; h < height; h++) {
            int xpos = (width > height) ? (int) ((float) h / partx) : (int) ((float) w / partx);
            int ypos = (width > height) ? (int) ((float) w / party) : (int) ((float) h / party);
            PartialMap[w][h] = indexes[ypos][xpos];
          }
        break;
      }
      case LEDArray: {
        /* TODO: for the moment we assume that the columns are on the x-axis and the rows on the y-axis 
         * rotated array's are not taking into account */
        float partx = (float) width / (float) nrOfColumns;
        float party = (float) height / (float) nrOfRows;
        for (int w = 0; w < width; w++) 
          for (int h = 0; h < height; h++) {
            int xPos = (int) ((float) w / partx);
            int yPos = (int) ((float) h / party);
            PartialMap[w][h] = (yPos * nrOfColumns) + xPos;
          }
        break;
      }
      default: {
        for (int w = 0; w < width; w++)
          for (int h = 0; h < height; h++)
            PartialMap[w][h] = -1;
        break;
      }
    }
  }
  
  public static void paintPartialMap(Graphics2D g,
      int pinNr,
      int height,
      int width,
      int nrOfPins,
      int nrOfRows,
      int nrOfColumns,
      int x,
      int y,
      Color col,
      int alpha,
      IOComponentTypes type) {
    g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha));
    boolean hasDp = false;
    switch (type) {
      case DIPSwitch: {
        float part = (width > height) ? (float) width / (float) nrOfPins : (float) height / (float) nrOfPins;
        int bx = (width > height) ? x + (int) ((float) pinNr * part) : x;
        int by = (width > height) ? y : y + (int) ((float) pinNr * part);
        int bw = (width > height) ? (int) ((float) (pinNr + 1) * part) - (int) ((float) pinNr * part) : width;
        int bh = (width > height) ? height : (int) ((float) (pinNr + 1) * part) - (int) ((float) pinNr * part);
        g.fillRect(bx, by, bw, bh);
        break;
      }
      case RGBLED : {
        float part = height / 3;
        int by = y + (int) ((float) pinNr * part);
        int bh = (int) ((float) (pinNr + 1) * part) - (int) ((float) pinNr * part);
        g.fillRect(x, by, width, bh);
        break;
      }
      case SevenSegment: hasDp = true;
      case SevenSegmentNoDp : {
        int sa = com.cburch.logisim.std.io.SevenSegment.Segment_A;
        int sb = com.cburch.logisim.std.io.SevenSegment.Segment_B;
        int sc = com.cburch.logisim.std.io.SevenSegment.Segment_C;
        int sd = com.cburch.logisim.std.io.SevenSegment.Segment_D;
        int se = com.cburch.logisim.std.io.SevenSegment.Segment_E;
        int sf = com.cburch.logisim.std.io.SevenSegment.Segment_F;
        int sg = com.cburch.logisim.std.io.SevenSegment.Segment_G;
        int[][] indexes = {
            {-1, sa, sa, -1, -1},
            {sf, -1, -1, sb, -1},
            {sf, -1, -1, sb, -1},
            {-1, sg, sg, -1, -1},
            {se, -1, -1, sc, -1},
            {se, -1, -1, sc, -1},
            {-1, sd, sd, -1, -1},
        };
        if (hasDp) indexes[6][4] = com.cburch.logisim.std.io.SevenSegment.DP;
        float partx = (width > height) ? (float) height / (float) 5.0 : (float) width / (float) 5.0;
        float party = (width > height) ? (float) width / (float) 7.0 : (float) height / (float) 7.0;
        for (int xpos = 0; xpos < 5; xpos++) {
          for (int ypos = 0; ypos < 7; ypos++) {
            if (indexes[ypos][xpos] == pinNr) {
              int bx = (width > height) ? x + (int) ((float) ypos * party) : x + (int) ((float) xpos * partx);
              int by = (width > height) ? y + (int) ((float) xpos * partx) : y + (int) ((float) ypos * party);
              int bw = (width > height) ? x + (int) ((float) (ypos + 1) * party) - bx :
                  x + (int) ((float) (xpos + 1) * partx) - bx;
              int bh = (width > height) ? y + (int) ((float) (xpos + 1) * partx) - by : 
                  y + (int) ((float) (ypos + 1) * party) - by;
              g.fillRect(bx, by, bw, bh);
            }
          }
        }
        break;
      }
      case LEDArray: {
        float partx = (float) width / (float) nrOfColumns;
        float party = (float) height / (float) nrOfRows;
        int xPos = pinNr % nrOfColumns;
        int yPos = pinNr / nrOfColumns;
        int bx = x + (int) ((float) xPos * partx);
        int by = y + (int) ((float) yPos * party);
        int bw = x + (int) ((float) (xPos + 1) * partx) - bx;
        int bh = y + (int) ((float) (yPos + 1) * party) - by;
        g.fillRect(bx, by, bw, bh);
        break;
      }
      default: {
        g.fillRect(x, y, width, height);
        break;
      }
    }
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
          IOComponentTypes.LEDArray,
          IOComponentTypes.SevenSegmentNoDp);

  public static final EnumSet<IOComponentTypes> InOutComponentSet =
      EnumSet.of(IOComponentTypes.Pin, IOComponentTypes.PortIO);
}
