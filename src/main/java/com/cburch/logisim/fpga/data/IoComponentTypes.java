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
import com.cburch.logisim.std.io.ReptarLocalBus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.EnumSet;
import java.util.Set;

public enum IoComponentTypes {
  Led,
  Button,
  Pin,
  SevenSegment,
  SevenSegmentNoDp,
  DIPSwitch,
  RgbLed,
  LedArray,
  PortIo,
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

  public static final int ROTATION_ZERO = 0;
  public static final int ROTATION_CW_90 = -90;
  public static final int ROTATION_CCW_90 = 90;

  public static IoComponentTypes getEnumFromString(String str) {
    for (var elem : KNOWN_COMPONENT_SET) {
      if (elem.name().equalsIgnoreCase(str)) {
        return elem;
      }
    }
    return IoComponentTypes.Unknown;
  }

  public static int getFpgaInOutRequirement(IoComponentTypes comp) {
    return switch (comp) {
      case PortIo -> 8;
      case LocalBus -> 16;
      default -> 0;
    };
  }

  public static int getFpgaInputRequirement(IoComponentTypes comp) {
    return switch (comp) {
      case Button -> 1;
      case DIPSwitch -> 8;
      case LocalBus -> 13;
      default -> 0;
    };
  }

  public static int getFpgaOutputRequirement(IoComponentTypes comp) {
    return switch (comp) {
      case Led -> 1;
      case SevenSegment -> 8;
      case SevenSegmentNoDp -> 7;
      case RgbLed -> 3;
      case LocalBus -> 2;
      case LedArray -> 16;
      default -> 0;
    };
  }

  public static boolean nrOfInputPinsConfigurable(IoComponentTypes comp) {
    return comp.equals(DIPSwitch);
  }

  public static boolean nrOfOutputPinsConfigurable(IoComponentTypes comp) {
    return false;
  }

  public static boolean nrOfIoPinsConfigurable(IoComponentTypes comp) {
    return comp.equals(PortIo);
  }

  public static String getInputLabel(int nrPins, int id, IoComponentTypes comp) {
    return switch (comp) {
      case DIPSwitch -> DipSwitch.getInputLabel(id);
      case LocalBus -> ReptarLocalBus.getInputLabel(id);
      default -> (nrPins > 1) ? S.get("FpgaIoPins", id) : S.get("FpgaIoPin");
    };
  }

  public static Boolean hasRotationAttribute(IoComponentTypes comp) {
    return switch (comp) {
      case DIPSwitch, SevenSegment, LedArray -> true;
      default -> false;
    };
  }

  public static String getRotationString(IoComponentTypes comp, int rotation) {
    return switch (comp) {
      case DIPSwitch -> switch (rotation) {
        case ROTATION_CW_90 -> S.get("DipSwitchCW90");
        case ROTATION_CCW_90 -> S.get("DipSwitchCCW90");
        default -> S.get("DipSwitchZero");
      };
      case SevenSegment -> switch (rotation) {
        case ROTATION_CW_90 -> S.get("SevenSegmentCW90");
        case ROTATION_CCW_90 -> S.get("SevenSegmentCCW90");
        default -> S.get("SevenSegmentZero");
      };
      case LedArray -> switch (rotation) {
        case ROTATION_CW_90 -> S.get("LEDArrayCW90");
        case ROTATION_CCW_90 -> S.get("LEDArrayCCW90");
        default -> S.get("LEDArrayZero");
      };
      default -> Integer.toString(rotation);
    };
  }

  public static String getOutputLabel(int nrPins, int nrOfRows, int nrOfColumns, int id, IoComponentTypes comp) {
    switch (comp) {
      case SevenSegmentNoDp:
      case SevenSegment:
        return com.cburch.logisim.std.io.SevenSegment.getOutputLabel(id);
      case RgbLed:
        return com.cburch.logisim.std.io.RgbLed.getLabel(id);
      case LocalBus:
        return ReptarLocalBus.getOutputLabel(id);
      case LedArray:
        if (nrOfRows != 0 && nrOfColumns != 0 && id >= 0 && id < nrPins) {
          final var row = id / nrOfColumns;
          final var col = id % nrOfColumns;
          return "Row_" + row + "_Col_" + col;
        }
      default:
        return (nrPins > 1) ? S.get("FpgaIoPins", id) : S.get("FpgaIoPin");
    }
  }

  public static String getIoLabel(int nrPins, int id, IoComponentTypes comp) {
    return comp == IoComponentTypes.LocalBus
       ? ReptarLocalBus.getIoLabel(id)
       : (nrPins > 1) ? S.get("FpgaIoPins", id) : S.get("FpgaIoPin");
  }

  public static int GetNrOfFPGAPins(IoComponentTypes comp) {
    return getFpgaInOutRequirement(comp)
        + getFpgaInputRequirement(comp)
        + getFpgaOutputRequirement(comp);
  }

  private static int[][] getSevenSegmentDisplayArray(boolean hasDp) {
    final var sa = com.cburch.logisim.std.io.SevenSegment.Segment_A;
    final var sb = com.cburch.logisim.std.io.SevenSegment.Segment_B;
    final var sc = com.cburch.logisim.std.io.SevenSegment.Segment_C;
    final var sd = com.cburch.logisim.std.io.SevenSegment.Segment_D;
    final var se = com.cburch.logisim.std.io.SevenSegment.Segment_E;
    final var sf = com.cburch.logisim.std.io.SevenSegment.Segment_F;
    final var sg = com.cburch.logisim.std.io.SevenSegment.Segment_G;
    final int[][] indexes = {
        {-1, sa, sa, -1, -1},
        {sf, -1, -1, sb, -1},
        {sf, -1, -1, sb, -1},
        {-1, sg, sg, -1, -1},
        {se, -1, -1, sc, -1},
        {se, -1, -1, sc, -1},
        {-1, sd, sd, -1, -1}
    };
    if (hasDp) indexes[6][4] = com.cburch.logisim.std.io.SevenSegment.DP;
    return indexes;
  }

  public static void getPartialMapInfo(Integer[][] partialMap,
      int width,
      int height,
      int nrOfPins,
      int nrOfRows,
      int nrOfColumns,
      int mapRotation,
      IoComponentTypes type) {
    var hasDp = false;
    var part = 0f;
    var partX = 0f;
    var partY = 0f;
    switch (type) {
      case DIPSwitch:
        switch (mapRotation) {
          case ROTATION_CCW_90:
          case ROTATION_CW_90:
            part = (float) height / (float) nrOfPins;
            break;
          default:
            part = (float) width / (float) nrOfPins;
            break;
        }
        for (var widthIndex = 0; widthIndex < width; widthIndex++)
          for (var heightIndex = 0; heightIndex < height; heightIndex++) {
            var pinIndex = 0;
            pinIndex = switch (mapRotation) {
              case ROTATION_CCW_90 -> (int) ((height - heightIndex - 1) / part);
              case ROTATION_CW_90 -> (int) (height / part);
              default -> (int) (widthIndex / part);
            };
            partialMap[widthIndex][heightIndex] = pinIndex;
          }
        break;
      case RgbLed:
        part = (float) height / (float) 3;
        for (var w = 0; w < width; w++)
          for (var h = 0; h < height; h++)
            partialMap[w][h] = (int) ((float) h / part);
        break;
      case SevenSegment: hasDp = true;
      case SevenSegmentNoDp:
        final var indexes = getSevenSegmentDisplayArray(hasDp);
        switch (mapRotation) {
          case ROTATION_CCW_90:
          case ROTATION_CW_90:
            partX = (float) width / (float) 7;
            partY = (float) height / (float) 5;
            break;
          default:
            partX = (float) width / (float) 5;
            partY = (float) height / (float) 7;
            break;
        }
        var xIndex = 0;
        var yIndex = 0;
        for (var w = 0; w < width; w++)
          for (var h = 0; h < height; h++) {
            switch (mapRotation) {
              case ROTATION_CCW_90:
                xIndex = (int) ((float) (height - h - 1) / partY);
                yIndex = (int) ((float) w / partX);
                break;
              case ROTATION_CW_90:
                xIndex = (int) ((float) h / partY);
                yIndex = (int) ((float) (width - w - 1) / partX);
                break;
              default:
                xIndex = (int) ((float) w / partX);
                yIndex = (int) ((float) h / partY);
                break;
            }
            partialMap[w][h] = indexes[yIndex][xIndex];
          }
        break;
      case LedArray:
        switch (mapRotation) {
          case ROTATION_CCW_90:
          case ROTATION_CW_90:
            partX = (float) width / (float) nrOfRows;
            partY = (float) height / (float) nrOfColumns;
            break;
          default:
            partX = (float) width / (float) nrOfColumns;
            partY = (float) height / (float) nrOfRows;
            break;
        }
        for (var w = 0; w < width; w++)
          for (var h = 0; h < height; h++) {
            var realRow = 0;
            var realColumn = 0;
            switch (mapRotation) {
              case ROTATION_CCW_90:
                realRow = (int) ((float) w / partX);
                realColumn = (int) ((float) (height - h - 1) / partY);
                break;
              case ROTATION_CW_90:
                realRow = (int) ((float) (width - w - 1) / partX);
                realColumn = (int) ((float) h / partY);
                break;
              default:
                realRow = (int) ((float) h / partY);
                realColumn = (int) ((float) w / partX);
                break;
            }
            partialMap[w][h] = (realRow * nrOfColumns) + realColumn;
          }
        break;
      default:
        for (var w = 0; w < width; w++)
          for (var h = 0; h < height; h++)
            partialMap[w][h] = -1;
        break;
    }
  }

  public static void paintPartialMap(Graphics2D g,
      int pinNr,
      int height,
      int width,
      int nrOfPins,
      int nrOfRows,
      int nrOfColumns,
      int mapRotation,
      int x,
      int y,
      Color col,
      int alpha,
      IoComponentTypes type) {
    g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha));
    var hasDp = false;
    var part = 0f;
    var boxWidth = 0;
    var boxHeight = 0;
    var boxXpos = 0;
    var boxYpos = 0;
    var partX = 0f;
    var partY = 0f;
    switch (type) {
      case DIPSwitch:
        var yPinNr = pinNr;
        switch (mapRotation) {
          case ROTATION_CCW_90: yPinNr = nrOfPins - pinNr - 1;
          case ROTATION_CW_90: {
            part = (float) height / (float) nrOfPins;
            boxXpos = x;
            boxWidth = width;
            boxYpos = y + (int) ((float) yPinNr * part);
            boxHeight = (int) ((float) (yPinNr + 1) * part) - (int) ((float) yPinNr * part);
            break;
          }
          default: {
            part = (float) width / (float) nrOfPins;
            boxXpos = x + (int) ((float) pinNr * part);
            boxYpos = y;
            boxWidth = (int) ((float) (pinNr + 1) * part) - (int) ((float) (pinNr * part));
            boxHeight = height;
            break;
          }
        }
        g.fillRect(boxXpos, boxYpos, boxWidth, boxHeight);
        break;
      case RgbLed:
        part = (float) height / (float) 3;
        final var by = y + (int) ((float) pinNr * part);
        final var bh = (int) ((float) (pinNr + 1) * part) - (int) ((float) pinNr * part);
        g.fillRect(x, by, width, bh);
        break;
      case SevenSegment: hasDp = true;
      case SevenSegmentNoDp:
        final var indexes = getSevenSegmentDisplayArray(hasDp);
        switch (mapRotation) {
          case ROTATION_CCW_90:
          case ROTATION_CW_90: {
            partX = (float) width / (float) 7;
            partY = (float) height / (float) 5;
            break;
          }
          default: {
            partX = (float) width / (float) 5;
            partY = (float) height / (float) 7;
            break;
          }
        }
        var realXIndex = 0;
        var realXIndexPlusOne = 0;
        var realYIndex = 0;
        var realYIndexPlusOne = 0;
        for (var xIndex = 0; xIndex < 5; xIndex++) {
          for (var yIndex = 0; yIndex < 7; yIndex++) {
            if (indexes[yIndex][xIndex] == pinNr) {
              switch (mapRotation) {
                case ROTATION_CCW_90:
                  realXIndex = yIndex;
                  realXIndexPlusOne = yIndex  + 1;
                  realYIndex = 4 - xIndex;
                  realYIndexPlusOne = 5 - xIndex;
                  break;
                case ROTATION_CW_90:
                  realXIndex = 6 - yIndex;
                  realXIndexPlusOne = 7 - yIndex;
                  realYIndex = xIndex;
                  realYIndexPlusOne = xIndex + 1;
                  break;
                default:
                  realXIndex = xIndex;
                  realXIndexPlusOne = xIndex + 1;
                  realYIndex = yIndex;
                  realYIndexPlusOne = yIndex + 1;
                  break;
              }
              boxXpos = x + (int) ((float) realXIndex * partX);
              boxYpos = y + (int) ((float) realYIndex * partY);
              /* the below calculation we do to avoid truncation errors causing empty lines between the segments */
              boxWidth = (int) ((float) realXIndexPlusOne * partX) - (int) ((float) realXIndex * partX);
              boxHeight = (int) ((float) realYIndexPlusOne * partY) - (int) ((float) realYIndex * partY);
              g.fillRect(boxXpos, boxYpos, boxWidth, boxHeight);
            }
          }
        }
        break;
      case LedArray:
        final var selectedColumn = pinNr % nrOfColumns;
        final var selectedRow = pinNr / nrOfColumns;
        switch (mapRotation) {
          case ROTATION_CCW_90:
          case ROTATION_CW_90: {
            partX = (float) width / (float) nrOfRows;
            partY = (float) height / (float) nrOfColumns;
            break;
          }
          default: {
            partX = (float) width / (float) nrOfColumns;
            partY = (float) height / (float) nrOfRows;
            break;
          }
        }
        var xPosition = 0;
        var nextXPosition = 0;
        var yPosition = 0;
        var nextYPosition = 0;
        switch (mapRotation) {
          case ROTATION_CCW_90: {
            xPosition = (int) ((float) selectedRow * partX);
            nextXPosition = (int) ((float) (selectedRow + 1) * partX);
            yPosition = (int) ((float) (nrOfColumns - selectedColumn - 1) * partY);
            nextYPosition = (int) ((float) (nrOfColumns - selectedColumn) * partY);
            break;
          }
          case ROTATION_CW_90: {
            xPosition = (int) ((float) (nrOfRows - selectedRow - 1) * partX);
            nextXPosition = (int) ((float) (nrOfRows - selectedRow) * partX);
            yPosition = (int) ((float) selectedColumn * partY);
            nextYPosition = (int) ((float) (selectedColumn + 1) * partY);
            break;
          }
          default: {
            xPosition = (int) ((float) selectedColumn * partX);
            nextXPosition = (int) ((float) (selectedColumn + 1) * partX);
            yPosition = (int) ((float) selectedRow * partY);
            nextYPosition = (int) ((float) (selectedRow + 1) * partY);
            break;
          }
        }
        boxXpos = x + xPosition;
        boxYpos = y + yPosition;
        boxWidth = nextXPosition - xPosition;
        boxHeight = nextYPosition - yPosition;
        g.fillRect(boxXpos, boxYpos, boxWidth, boxHeight);
        break;
      default: {
        g.fillRect(x, y, width, height);
        break;
      }
    }
  }

  public static final Set<IoComponentTypes> KNOWN_COMPONENT_SET =
      EnumSet.range(IoComponentTypes.Led, IoComponentTypes.LocalBus);

  public static final Set<IoComponentTypes> SIMPLE_INPUT_SET =
      EnumSet.range(IoComponentTypes.Led, IoComponentTypes.LocalBus);

  public static final Set<IoComponentTypes> INPUT_COMPONENT_SET =
      EnumSet.of(IoComponentTypes.Button, IoComponentTypes.Pin, IoComponentTypes.DIPSwitch);

  public static final Set<IoComponentTypes> OUTPUT_COMPONENT_SET =
      EnumSet.of(
          IoComponentTypes.Led,
          IoComponentTypes.Pin,
          IoComponentTypes.RgbLed,
          IoComponentTypes.SevenSegment,
          IoComponentTypes.LedArray,
          IoComponentTypes.SevenSegmentNoDp);

  public static final Set<IoComponentTypes> IN_OUT_COMPONENT_SET =
      EnumSet.of(IoComponentTypes.Pin, IoComponentTypes.PortIo);
}
