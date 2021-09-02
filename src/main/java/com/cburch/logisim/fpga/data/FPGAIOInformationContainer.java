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

package com.cburch.logisim.fpga.data;

import com.cburch.logisim.fpga.file.BoardWriterClass;
import com.cburch.logisim.fpga.gui.BoardManipulator;
import com.cburch.logisim.fpga.gui.FPGAIOInformationSettingsDialog;
import com.cburch.logisim.fpga.gui.PartialMapDialog;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.DotMatrix;
import com.cburch.logisim.std.io.LedBar;
import com.cburch.logisim.std.io.RgbLed;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class FPGAIOInformationContainer implements Cloneable {

  private static class mapType {
    private MapComponent map;
    private final int pin;

    public mapType(MapComponent map, int pin) {
      this.map = map;
      this.pin = pin;
    }

    public void unmap() {
      map.unmap(pin);
    }

    public void update(MapComponent map) {
      this.map = map;
    }

    public MapComponent getMap() {
      return map;
    }
  }

  public static class MapResultClass {
    public boolean mapResult;
    public int pinId;
  }

  public static LinkedList<String> GetComponentTypes() {
    LinkedList<String> result = new LinkedList<>();
    for (var comp : IOComponentTypes.KnownComponentSet) {
      result.add(comp.toString());
    }
    return result;
  }

  static final Logger logger = LoggerFactory.getLogger(FPGAIOInformationContainer.class);

  private IOComponentTypes MyType;
  protected BoardRectangle MyRectangle;
  protected int myRotation = IOComponentTypes.ROTATION_ZERO;
  private Map<Integer, String> MyPinLocations;
  private HashSet<Integer> MyInputPins;
  private HashSet<Integer> MyOutputPins;
  private HashSet<Integer> MyIOPins;
  private Integer[][] PartialMapArray;
  private Integer NrOfPins;
  private Integer NrOfExternalPins = 0;
  private Integer MyArrayId = -1;
  private char MyPullBehavior;
  private char MyActivityLevel;
  private char MyIOStandard;
  private char MyDriveStrength;
  private String MyLabel;
  private boolean toBeDeleted = false;
  private ArrayList<mapType> pinIsMapped;
  private int paintColor = BoardManipulator.DEFINE_COLOR_ID;
  private boolean mapMode = false;
  private boolean highlighted = false;
  private int nrOfRows = 4;
  private int nrOfColumns = 4;
  private char Driving = LedArrayDriving.LED_DEFAULT;
  protected boolean selectable = false;
  protected int selectedPin = -1;
  protected MapListModel.MapInfo selComp = null;

  public FPGAIOInformationContainer() {
    MyType = IOComponentTypes.Unknown;
    MyRectangle = null;
    MyPinLocations = new HashMap<>();
    setNrOfPins(0);
    MyPullBehavior = PullBehaviors.UNKNOWN;
    MyActivityLevel = PinActivity.Unknown;
    MyIOStandard = IoStandards.UNKNOWN;
    MyDriveStrength = DriveStrength.UNKNOWN;
    MyLabel = null;
  }

  public FPGAIOInformationContainer(IOComponentTypes Type, BoardRectangle rect,
                                    IOComponentsInformation iocomps) {
    MyType = Type;
    MyRectangle = rect;
    MyPinLocations = new HashMap<>();
    setNrOfPins(0);
    MyPullBehavior = PullBehaviors.UNKNOWN;
    MyActivityLevel = PinActivity.Unknown;
    MyIOStandard = IoStandards.UNKNOWN;
    MyDriveStrength = DriveStrength.UNKNOWN;
    MyLabel = null;
    if (rect != null) rect.SetLabel(null);
    if (IOComponentTypes.SimpleInputSet.contains(Type)) {
      FPGAIOInformationSettingsDialog.GetSimpleInformationDialog(false, iocomps, this);
      return;
    }
    MyType = IOComponentTypes.Unknown;
  }

  public FPGAIOInformationContainer(
      IOComponentTypes Type,
      BoardRectangle rect,
      String loc,
      String pull,
      String active,
      String standard,
      String drive,
      String label) {
    this.Set(Type, rect, loc, pull, active, standard, drive, label);
  }

  public FPGAIOInformationContainer(Node DocumentInfo) {
    /*
     * This constructor is used to create an element during the reading of a
     * board information xml file
     */
    MyType = IOComponentTypes.Unknown;
    MyRectangle = null;
    MyPinLocations = new HashMap<>();
    setNrOfPins(0);
    MyPullBehavior = PullBehaviors.UNKNOWN;
    MyActivityLevel = PinActivity.Unknown;
    MyIOStandard = IoStandards.UNKNOWN;
    MyDriveStrength = DriveStrength.UNKNOWN;
    MyLabel = null;
    ArrayList<String> InputLocs = new ArrayList<>();
    ArrayList<String> OutputLocs = new ArrayList<>();
    ArrayList<String> IOLocs = new ArrayList<>();
    IOComponentTypes SetId = IOComponentTypes.getEnumFromString(DocumentInfo.getNodeName());
    if (IOComponentTypes.KnownComponentSet.contains(SetId)) {
      MyType = SetId;
    } else {
      return;
    }
    var attrs = DocumentInfo.getAttributes();
    int x = -1, y = -1, width = -1, height = -1;
    for (var attributeIndex = 0; attributeIndex < attrs.getLength(); attributeIndex++) {
      final var thisAttr = attrs.item(attributeIndex);
      if (thisAttr.getNodeName().equals(BoardWriterClass.MAP_ROTATION)) {
        myRotation = Integer.parseInt(thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(BoardWriterClass.LOCATION_X_STRING)) {
        x = Integer.parseInt(thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(BoardWriterClass.LOCATION_Y_STRING)) {
        y = Integer.parseInt(thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(BoardWriterClass.WIDTH_STRING)) {
        width = Integer.parseInt(thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(BoardWriterClass.HEIGHT_STRING)) {
        height = Integer.parseInt(thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(BoardWriterClass.RECT_SET_STRING)) {
        final var vals = thisAttr.getNodeValue().split(",");
        if (vals.length == 4) {
          try {
            x = Integer.parseUnsignedInt(vals[0]);
            y = Integer.parseUnsignedInt(vals[1]);
            width = Integer.parseUnsignedInt(vals[2]);
            height = Integer.parseUnsignedInt(vals[3]);
          } catch (NumberFormatException e) {
            x = y = width = height = -1;
          }
        }
      }
      if (thisAttr.getNodeName().equals(BoardWriterClass.LED_ARRAY_INFO_STRING)) {
        final var vals = thisAttr.getNodeValue().split(",");
        if (vals.length == 3) {
          try {
            nrOfRows = Integer.parseUnsignedInt(vals[0]);
            nrOfColumns = Integer.parseUnsignedInt(vals[1]);
            Driving = LedArrayDriving.getId(vals[2]);
          } catch (NumberFormatException e) {
            nrOfRows = nrOfColumns = 4;
            Driving = LedArrayDriving.LED_DEFAULT;
          }
        }
      }
      if (thisAttr.getNodeName().equals(BoardWriterClass.PIN_LOCATION_STRING)) {
        setNrOfPins(1);
        MyPinLocations.put(0, thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(BoardWriterClass.MULTI_PIN_INFORMATION_STRING)) {
        setNrOfPins(Integer.parseInt(thisAttr.getNodeValue()));
      }
      if (thisAttr.getNodeName().startsWith(BoardWriterClass.MULTI_PIN_PREFIX_STRING)) {
        String Id =
            thisAttr.getNodeName().substring(BoardWriterClass.MULTI_PIN_PREFIX_STRING.length());
        MyPinLocations.put(Integer.parseInt(Id), thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(BoardWriterClass.LABEL_STRING)) {
        MyLabel = thisAttr.getNodeValue();
      }
      if (thisAttr.getNodeName().equals(DriveStrength.DRIVE_ATTRIBUTE_STRING)) {
        MyDriveStrength = DriveStrength.getId(thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(PullBehaviors.PULL_ATTRIBUTE_STRING)) {
        MyPullBehavior = PullBehaviors.getId(thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(IoStandards.IO_ATTRIBUTE_STRING)) {
        MyIOStandard = IoStandards.getId(thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().equals(PinActivity.ACTIVITY_ATTRIBUTE_STRING)) {
        MyActivityLevel = PinActivity.getId(thisAttr.getNodeValue());
      }
      if (thisAttr.getNodeName().contentEquals(BoardWriterClass.INPUT_SET_STRING)) {
        InputLocs.addAll(Arrays.asList(thisAttr.getNodeValue().split(",")));
      }
      if (thisAttr.getNodeName().contentEquals(BoardWriterClass.OUTPUT_SET_STRING)) {
        OutputLocs.addAll(Arrays.asList(thisAttr.getNodeValue().split(",")));
      }
      if (thisAttr.getNodeName().contentEquals(BoardWriterClass.IO_SET_STRING)) {
        IOLocs.addAll(Arrays.asList(thisAttr.getNodeValue().split(",")));
      }
    }
    if ((x < 0) || (y < 0) || (width < 1) || (height < 1)) {
      MyType = IOComponentTypes.Unknown;
      return;
    }
    var idx = 0;
    for (var loc : InputLocs) {
      MyPinLocations.put(idx, loc);
      if (MyInputPins == null) MyInputPins = new HashSet<>();
      MyInputPins.add(idx++);
    }
    for (var loc : OutputLocs) {
      MyPinLocations.put(idx, loc);
      if (MyOutputPins == null) MyOutputPins = new HashSet<>();
      MyOutputPins.add(idx++);
    }
    for (var loc : IOLocs) {
      MyPinLocations.put(idx, loc);
      if (MyIOPins == null) MyIOPins = new HashSet<>();
      MyIOPins.add(idx++);
    }
    if (idx != 0) setNrOfPins(idx);
    var PinsComplete = true;
    for (var i = 0; i < NrOfPins; i++) {
      if (!MyPinLocations.containsKey(i)) {
        logger.warn("Bizar missing pin {} of component!", i);
        PinsComplete = false;
      }
    }
    if (!PinsComplete) {
      MyType = IOComponentTypes.Unknown;
      return;
    }
    /* This code is for backward compatibility */
    if (MyInputPins == null && MyOutputPins == null && MyIOPins == null) {
      var NrInpPins = IOComponentTypes.GetFPGAInputRequirement(MyType);
      var NrOutpPins = IOComponentTypes.GetFPGAOutputRequirement(MyType);
      for (var i = 0; i < NrOfPins; i++) {
        if (i < NrInpPins) {
          if (MyInputPins == null) MyInputPins = new HashSet<>();
          MyInputPins.add(i);
        } else if (i < (NrInpPins + NrOutpPins)) {
          if (MyOutputPins == null) MyOutputPins = new HashSet<>();
          MyOutputPins.add(i);
        } else {
          if (MyIOPins == null) MyIOPins = new HashSet<>();
          MyIOPins.add(i);
        }
      }
    }
    /* End backward compatibility */
    if (MyType.equals(IOComponentTypes.Pin)) MyActivityLevel = PinActivity.ACTIVE_HIGH;
    MyRectangle = new BoardRectangle(x, y, width, height);
    if (MyLabel != null) MyRectangle.SetLabel(MyLabel);

    if (MyType.equals(IOComponentTypes.LEDArray)) {
      NrOfExternalPins = NrOfPins;
      NrOfPins = nrOfRows * nrOfColumns;
      setNrOfPins(NrOfPins);
      MyOutputPins.clear();
      for (var i = 0; i < NrOfPins; i++)
        MyOutputPins.add(i);
    }
  }

  public void setArrayId(int val) {
    MyArrayId = val;
  }

  public int getArrayId() {
    return MyArrayId;
  }

  public void setMapRotation(int val) {
    if ((val == IOComponentTypes.ROTATION_CW_90)
        || (val == IOComponentTypes.ROTATION_CCW_90)
        || (val == IOComponentTypes.ROTATION_ZERO))
      myRotation = val;
  }

  public int getMapRotation() {
    return myRotation;
  }

  public int getExternalPinCount() {
    return NrOfExternalPins;
  }

  public boolean hasMap() {
    boolean ret = false;
    for (var i = 0; i < NrOfPins; i++)
      ret |= pinIsMapped(i);
    return ret;
  }

  public int getNrOfInputPins() {
    if (MyInputPins == null) return 0;
    return MyInputPins.size();
  }

  public int getNrOfOutputPins() {
    if (MyOutputPins == null) return 0;
    return MyOutputPins.size();
  }

  public int getNrOfIOPins() {
    if (MyIOPins == null) return 0;
    return MyIOPins.size();
  }

  public int getNrOfRows() {
    return nrOfRows;
  }

  public int getNrOfColumns() {
    return nrOfColumns;
  }

  public char getArrayDriveMode() {
    return Driving;
  }

  public void setNrOfRows(int value) {
    nrOfRows = value;
  }

  public void setNrOfColumns(int value) {
    nrOfColumns = value;
  }

  public void setArrayDriveMode(char value) {
    Driving = value;
  }


  public void edit(Boolean deleteButton, IOComponentsInformation IOcomps) {
    FPGAIOInformationSettingsDialog.GetSimpleInformationDialog(deleteButton, IOcomps, this);
  }

  public void setMapMode() {
    mapMode = true;
    paintColor = BoardManipulator.TRANSPARENT_ID;
  }

  public void setToBeDeleted() {
    toBeDeleted = true;
  }

  public boolean isToBeDeleted() {
    return toBeDeleted;
  }

  public char GetActivityLevel() {
    return MyActivityLevel;
  }

  public void setActivityLevel(char activity) {
    MyActivityLevel = activity;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public String getPinLocation(int index) {
    if (MyPinLocations.containsKey(index))
      return MyPinLocations.get(index);
    return "";
  }

  public void setInputPinLocation(int index, String value) {
    if (MyOutputPins != null) MyOutputPins.remove(index);
    if (MyIOPins != null) MyIOPins.remove(index);
    if (MyInputPins == null) MyInputPins = new HashSet<>();
    MyInputPins.add(index);
    MyPinLocations.put(index, value);
  }

  public void setOutputPinLocation(int index, String value) {
    if (MyInputPins != null) MyInputPins.remove(index);
    if (MyIOPins != null) MyIOPins.remove(index);
    if (MyOutputPins == null) MyOutputPins = new HashSet<>();
    MyOutputPins.add(index);
    MyPinLocations.put(index, value);
  }

  public void setIOPinLocation(int index, String value) {
    if (MyInputPins != null) MyInputPins.remove(index);
    if (MyOutputPins != null) MyOutputPins.remove(index);
    if (MyIOPins == null) MyIOPins = new HashSet<>();
    MyIOPins.add(index);
    MyPinLocations.put(index, value);
  }

  public Element GetDocumentElement(Document doc) {
    if (MyType.equals(IOComponentTypes.Unknown)) {
      return null;
    }
    try {
      var result = doc.createElement(MyType.toString());
      result.setAttribute(
          BoardWriterClass.RECT_SET_STRING,
          MyRectangle.getXpos()
              + ","
              + MyRectangle.getYpos()
              + ","
              + MyRectangle.getWidth()
              + ","
              + MyRectangle.getHeight());
      if (MyLabel != null) {
        var label = doc.createAttribute(BoardWriterClass.LABEL_STRING);
        label.setValue(MyLabel);
        result.setAttributeNode(label);
      }
      if (MyType.equals(IOComponentTypes.LEDArray)) {
        result.setAttribute(
            BoardWriterClass.LED_ARRAY_INFO_STRING,
            nrOfRows
            + ","
            + nrOfColumns
            + ","
            + LedArrayDriving.getStrings().get(Driving));
      }
      if (IOComponentTypes.hasRotationAttribute(MyType)) {
        switch (myRotation) {
          case IOComponentTypes.ROTATION_CW_90:
          case IOComponentTypes.ROTATION_CCW_90: {
            result.setAttribute(BoardWriterClass.MAP_ROTATION, Integer.toString(myRotation));
            break;
          }
          default: break;
        }
      }
      if (MyInputPins != null && !MyInputPins.isEmpty()) {
        var Set = doc.createAttribute(BoardWriterClass.INPUT_SET_STRING);
        var s = new StringBuilder();
        var first = true;
        for (var i = 0; i < NrOfPins; i++)
          if (MyInputPins.contains(i)) {
            if (first) first = false;
            else s.append(",");
            s.append(MyPinLocations.get(i));
          }
        Set.setValue(s.toString());
        result.setAttributeNode(Set);
      }
      if (MyOutputPins != null && !MyOutputPins.isEmpty()) {
        var Set = doc.createAttribute(BoardWriterClass.OUTPUT_SET_STRING);
        var s = new StringBuilder();
        var first = true;
        for (var i = 0; i < NrOfPins; i++)
          if (MyOutputPins.contains(i)) {
            if (first) first = false;
            else s.append(",");
            s.append(MyPinLocations.get(i));
          }
        Set.setValue(s.toString());
        result.setAttributeNode(Set);
      }
      if (MyIOPins != null && !MyIOPins.isEmpty()) {
        var Set = doc.createAttribute(BoardWriterClass.IO_SET_STRING);
        var s = new StringBuilder();
        var first = true;
        for (var i = 0; i < NrOfPins; i++)
          if (MyIOPins.contains(i)) {
            if (first) first = false;
            else s.append(",");
            s.append(MyPinLocations.get(i));
          }
        Set.setValue(s.toString());
        result.setAttributeNode(Set);
      }
      if (MyDriveStrength != DriveStrength.UNKNOWN
          && MyDriveStrength != DriveStrength.DEFAULT_STENGTH) {
        var drive = doc.createAttribute(DriveStrength.DRIVE_ATTRIBUTE_STRING);
        drive.setValue(DriveStrength.BEHAVIOR_STRINGS[MyDriveStrength]);
        result.setAttributeNode(drive);
      }
      if (MyPullBehavior != PullBehaviors.UNKNOWN && MyPullBehavior != PullBehaviors.FLOAT) {
        var pull = doc.createAttribute(PullBehaviors.PULL_ATTRIBUTE_STRING);
        pull.setValue(PullBehaviors.BEHAVIOR_STRINGS[MyPullBehavior]);
        result.setAttributeNode(pull);
      }
      if (MyIOStandard != IoStandards.UNKNOWN && MyIOStandard != IoStandards.DEFAULT_STANDARD) {
        var stand = doc.createAttribute(IoStandards.IO_ATTRIBUTE_STRING);
        stand.setValue(IoStandards.Behavior_strings[MyIOStandard]);
        result.setAttributeNode(stand);
      }
      if (MyActivityLevel != PinActivity.Unknown && MyActivityLevel != PinActivity.ACTIVE_HIGH) {
        var act = doc.createAttribute(PinActivity.ACTIVITY_ATTRIBUTE_STRING);
        act.setValue(PinActivity.BEHAVIOR_STRINGS[MyActivityLevel]);
        result.setAttributeNode(act);
      }
      return result;
    } catch (Exception e) {
      /* TODO: handle exceptions */
      logger.error(
          "Exceptions not handled yet in GetDocumentElement(), but got an exception: {}",
          e.getMessage());
    }
    return null;
  }

  public String GetLabel() {
    return MyLabel;
  }

  public String GetDisplayString() {
    return MyLabel == null ? MyType.name() : MyLabel;
  }

  public void setLabel(String label) {
    MyLabel = label;
  }

  public char GetDrive() {
    return MyDriveStrength;
  }

  public void setDrive(char drive) {
    MyDriveStrength = drive;
  }

  public char GetIOStandard() {
    return MyIOStandard;
  }

  public void setIOStandard(char IOStandard) {
    MyIOStandard = IOStandard;
  }

  public int getNrOfPins() {
    return NrOfPins;
  }

  public char GetPullBehavior() {
    return MyPullBehavior;
  }

  public void setPullBehavior(char pull) {
    MyPullBehavior = pull;
  }

  public BoardRectangle GetRectangle() {
    return MyRectangle;
  }

  public IOComponentTypes GetType() {
    return MyType;
  }

  public void setType(IOComponentTypes type) {
    MyType = type;
  }

  public boolean IsInput() {
    return IOComponentTypes.InputComponentSet.contains(MyType);
  }

  public boolean IsInputOutput() {
    return IOComponentTypes.InOutComponentSet.contains(MyType);
  }

  public boolean IsKnownComponent() {
    return IOComponentTypes.KnownComponentSet.contains(MyType);
  }

  public boolean IsOutput() {
    return IOComponentTypes.OutputComponentSet.contains(MyType);
  }

  public boolean pinIsMapped(int index) {
    if (index < 0 || index >= NrOfPins) return true;
    return pinIsMapped.get(index) != null;
  }

  public MapComponent getPinMap(int index) {
    if (index < 0 || index >= NrOfPins) return null;
    return pinIsMapped.get(index).getMap();
  }

  public int getMapPin(int index) {
    if (index < 0 || index >= NrOfPins) return -1;
    return pinIsMapped.get(index).pin;
  }

  public void Set(
      IOComponentTypes Type,
      BoardRectangle rect,
      String loc,
      String pull,
      String active,
      String standard,
      String drive,
      String label) {
    MyType = Type;
    MyRectangle = rect;
    rect.SetActiveOnHigh(active.equals(PinActivity.BEHAVIOR_STRINGS[PinActivity.ACTIVE_HIGH]));
    setNrOfPins(0);
    MyPinLocations.put(0, loc);
    MyPullBehavior = PullBehaviors.getId(pull);
    MyActivityLevel = PinActivity.getId(active);
    MyIOStandard = IoStandards.getId(standard);
    MyDriveStrength = DriveStrength.getId(drive);
    MyLabel = label;
    if (rect != null) rect.SetLabel(label);
  }

  public void setNrOfPins(int count) {
    if (pinIsMapped == null) pinIsMapped = new ArrayList<>();
    NrOfPins = count;
    if (count > pinIsMapped.size()) {
      for (var i = pinIsMapped.size(); i < count; i++)
        pinIsMapped.add(null);
    } else if (count < pinIsMapped.size()) {
      for (var i = pinIsMapped.size() - 1; i >= count; i--) {
        var map = pinIsMapped.get(i);
        if (map != null) map.unmap();
        pinIsMapped.remove(i);
      }
    }
  }

  public void unmap(int pin) {
    if (pin < 0 || pin >= pinIsMapped.size()) return;
    var map = pinIsMapped.get(pin);
    pinIsMapped.set(pin, null);
    if (map != null) map.unmap();
  }

  public MapResultClass tryInputMap(MapComponent comp, int compPin, int inpPin) {
    var result = new MapResultClass();
    result.mapResult = false;
    result.pinId = inpPin;
    if (MyInputPins == null || !MyInputPins.contains(result.pinId))
      return this.tryIOMap(comp, compPin, inpPin);
    unmap(result.pinId);
    var map = new mapType(comp, compPin);
    pinIsMapped.set(result.pinId, map);
    result.mapResult = true;
    return result;
  }

  public MapResultClass tryOutputMap(MapComponent comp, int compPin, int outpPin) {
    var result = new MapResultClass();
    result.mapResult = false;
    result.pinId = outpPin + (MyInputPins == null ? 0 : MyInputPins.size());
    if (MyOutputPins == null || !MyOutputPins.contains(result.pinId))
      return this.tryIOMap(comp, compPin, outpPin);
    unmap(result.pinId);
    var map = new mapType(comp, compPin);
    pinIsMapped.set(result.pinId, map);
    result.mapResult = true;
    return result;
  }

  public MapResultClass tryIOMap(MapComponent comp, int compPin, int ioPin) {
    var result = new MapResultClass();
    result.mapResult = false;
    result.pinId =
        ioPin
            + (MyInputPins == null ? 0 : MyInputPins.size())
            + (MyOutputPins == null ? 0 : MyOutputPins.size());
    if (MyIOPins == null || !MyIOPins.contains(result.pinId)) return result;
    unmap(result.pinId);
    var map = new mapType(comp, compPin);
    pinIsMapped.set(result.pinId, map);
    result.mapResult = true;
    return result;
  }

  public boolean tryMap(MapComponent comp, int compPin, int myPin) {
    if (myPin < 0 || myPin >= NrOfPins) return false;
    unmap(myPin);
    var map = new mapType(comp, compPin);
    pinIsMapped.set(myPin, map);
    return true;
  }

  public boolean updateMap(int pin, MapComponent comp) {
    if (pin < 0 || pin >= pinIsMapped.size()) return false;
    var map = pinIsMapped.get(pin);
    if (map == null) return false;
    map.update(comp);
    return true;
  }

  public boolean isCompletelyMappedBy(MapComponent comp) {
    for (var i = 0; i < NrOfPins; i++)
      if (pinIsMapped.get(i) != null) {
        if (!pinIsMapped.get(i).map.equals(comp)) return false;
      } else return false;
    return true;
  }

  private int nrOfMaps() {
    int res = 0;
    for (var i = 0; i < NrOfPins; i++)
      if (pinIsMapped.get(i) != null)
        res++;
    return res;
  }

  public void setHighlighted() {
    if (!mapMode) paintColor = BoardManipulator.HIGHLIGHT_COLOR_ID;
    highlighted = true;
  }

  public void unsetHighlighted() {
    if (!mapMode) paintColor = BoardManipulator.DEFINE_COLOR_ID;
    highlighted = false;
  }

  public boolean hasInputs() {
    return MyInputPins != null && (MyInputPins.size() > 0);
  }

  public boolean hasOutputs() {
    return MyOutputPins != null && (MyOutputPins.size() > 0);
  }

  public boolean hasIOs() {
    return MyIOPins != null && (MyIOPins.size() > 0);
  }

  public int nrInputs() {
    return MyInputPins == null ? 0 : MyInputPins.size();
  }

  public int nrOutputs() {
    return MyOutputPins == null ? 0 : MyOutputPins.size();
  }

  public int nrIOs() {
    return MyIOPins == null ? 0 : MyIOPins.size();
  }

  public HashSet<Integer> getInputs() {
    return MyInputPins;
  }

  public HashSet<Integer> getOutputs() {
    return MyOutputPins;
  }

  public HashSet<Integer> getIOs() {
    return MyIOPins;
  }

  public String getPinName(int index) {
    if (MyInputPins != null && MyInputPins.contains(index)) {
      return IOComponentTypes.getInputLabel(NrOfPins, index, MyType);
    }
    if (MyOutputPins != null && MyOutputPins.contains(index)) {
      return IOComponentTypes.getOutputLabel(NrOfPins, nrOfRows, nrOfColumns, index, MyType);
    }
    if (MyIOPins != null && MyIOPins.contains(index)) {
      return IOComponentTypes.getIOLabel(NrOfPins, index, MyType);
    }
    return "" + index;
  }

  public boolean setSelectable(MapListModel.MapInfo comp) {
    selComp = comp;
    var map = comp.getMap();
    var connect = comp.getPin();
    selectedPin = -1;
    selectable = false;
    if (connect < 0) {
      if (map.hasInputs() && (hasIOs() || hasInputs())) selectable = true;
      if (map.hasOutputs() && (hasIOs() || hasOutputs())) selectable = true;
      if (map.hasIOs() && hasIOs()) selectable = true;
    } else {
      if (map.isInput(connect) && (hasIOs() || hasInputs())) selectable = true;
      if (map.isOutput(connect) && (hasIOs() || hasOutputs())) selectable = true;
      if (map.isIO(connect) && hasIOs()) selectable = true;
    }
    return selectable;
  }

  public boolean removeSelectable() {
    var ret = selectable;
    selComp = null;
    selectable = false;
    selectedPin = -1;
    return ret;
  }

  public void paint(Graphics2D g, float scale) {
    if (mapMode) {
      if (PartialMapArray == null) {
        PartialMapArray = new Integer[MyRectangle.getWidth()][MyRectangle.getHeight()];
        IOComponentTypes.getPartialMapInfo(PartialMapArray,
            MyRectangle.getWidth(),
            MyRectangle.getHeight(),
            NrOfPins,
            nrOfRows,
            nrOfColumns,
            myRotation,
            MyType);
      }
      mappaint(g, scale);
      return;
    }
    var PaintColor = BoardManipulator.getColor(paintColor);
    if (PaintColor == null) return;
    var c = g.getColor();
    g.setColor(PaintColor);
    g.fillRect(
        AppPreferences.getScaled(MyRectangle.getXpos(), scale),
        AppPreferences.getScaled(MyRectangle.getYpos(), scale),
        AppPreferences.getScaled(MyRectangle.getWidth(), scale),
        AppPreferences.getScaled(MyRectangle.getHeight(), scale));
    g.setColor(c);
  }

  private void mappaint(Graphics2D g, float scale) {
    var c = g.getColor();
    var i = nrOfMaps();
    if (i > 0) paintmapped(g, scale, i);
    else paintselected(g, scale);
    g.setColor(c);
  }

  private boolean containsMap() {
    if (selComp == null) return false;
    var com = selComp.getMap();
    for (var i = 0; i < NrOfPins; i++) {
      if (pinIsMapped.get(i) != null && pinIsMapped.get(i).map.equals(com)) return true;
    }
    return false;
  }

  public boolean selectedPinChanged(int xPos, int Ypos) {
    if (!(highlighted && selectable)) return false;
    if (PartialMapArray == null) {
      PartialMapArray = new Integer[MyRectangle.getWidth()][MyRectangle.getHeight()];
      IOComponentTypes.getPartialMapInfo(PartialMapArray,
          MyRectangle.getWidth(),
          MyRectangle.getHeight(),
          NrOfPins,
          nrOfRows,
          nrOfColumns,
          myRotation,
          MyType);
    }
    var selPin = PartialMapArray[xPos - MyRectangle.getXpos()][Ypos - MyRectangle.getYpos()];
    if (selPin != selectedPin) {
      selectedPin = selPin;
      return true;
    }
    return false;
  }

  public boolean isCompleteMap() {
    if (selComp == null) return true;
    var map = selComp.getMap();
    if (selComp.getPin() >= 0 && NrOfPins == 1) {
      /* single pin only */
      return true;
    }
    if (map.nrInputs() == nrInputs()
        && map.nrOutputs() == nrOutputs()
        && map.nrIOs() == nrIOs()
        && selComp.getPin() < 0) {
      return true;
    }
    if (nrInputs() == 0
        && nrOutputs() == 0
        && map.nrIOs() == 0
        && map.nrInputs() == nrIOs()
        && map.nrOutputs() == 0
        && selComp.getPin() < 0) {
      return true;
    }
    if (nrInputs() == 0
        && nrOutputs() == 0
        && map.nrIOs() == 0
        && map.nrOutputs() == nrIOs()
        && map.nrInputs() == 0
        && selComp.getPin() < 0) {
      return true;
    }
    return false;
  }

  public boolean tryLedArrayMap(JPanel parent) {
    var map = selComp.getMap();
    if (selComp.getPin() >= 0 && selectedPin >= 0) {
      /* single pin on a selected Pin */
      map.unmap(selComp.getPin());
      return map.tryMap(selComp.getPin(), this, selectedPin);
    }
    /* okay, the map component has more than one pin, then we treat first the RGB-LED,
     * DotMatrix, and LedBar, all others will be handled by a partialmapdialog
     */
    var fact = map.getComponentFactory();
    if (fact instanceof DotMatrix) {
      var nrOfMatrixRows = map.getAttributeSet().getValue(DotMatrix.ATTR_MATRIX_ROWS).getWidth();
      var nrOfMatrixColumns = map.getAttributeSet().getValue(DotMatrix.ATTR_MATRIX_COLS).getWidth();
      var startRow =  selectedPin / nrOfColumns;
      var startColumn = selectedPin % nrOfColumns;
      if (((nrOfMatrixRows + startRow) <= nrOfRows) && ((nrOfMatrixColumns + startColumn) <= nrOfColumns)) {
        var canMap = true;
        /* we can map the matrix here */
        map.unmap(); // Remove all previous maps
        for (var row = 0; row < nrOfMatrixRows; row++) {
          for (var column = 0; column < nrOfMatrixColumns; column++) {
            var SourcePin = row * nrOfMatrixColumns + column;
            var MapPin = (row + startRow) * nrOfColumns + column + startColumn;
            canMap &= map.tryMap(SourcePin, this, MapPin);
          }
        }
        if (!canMap) map.unmap();
        return canMap;
      }
    }
    if (fact instanceof LedBar) {
      var nrOfSegs = map.getAttributeSet().getValue(LedBar.ATTR_MATRIX_COLS).getWidth();
      var selCol = selectedPin % nrOfColumns;
      if ((selCol + nrOfSegs) <= nrOfColumns) {
        /* we can completely map the ledbar in this row */
        map.unmap(); /* remove all old maps */
        var canBeMapped = true;
        for (var i = 0; i < nrOfSegs; i++) {
          canBeMapped &= map.tryMap(nrOfSegs - i - 1, this, selectedPin + i);
        }
        if (!canBeMapped) map.unmap();
        return canBeMapped;
      }
    }
    if (fact instanceof RgbLed) {
      if (Driving == LedArrayDriving.RGB_COLUMN_SCANNING
          || Driving == LedArrayDriving.RGB_DEFAULT
          || Driving == LedArrayDriving.RGB_ROW_SCANNING) {
        /* only if we have an RGB-array we are going to do something special */
        map.unmap(); /* remove all previous maps */
        return map.tryCompleteMap(this, selectedPin);
      }
    }
    var diag = new PartialMapDialog(selComp, this, parent);
    return diag.doit();
  }

  public boolean tryMap(JPanel parent) {
    if (!selectable) return false;
    if (selComp == null) return false;
    if (MyType.equals(IOComponentTypes.LEDArray))
      return tryLedArrayMap(parent);
    var map = selComp.getMap();
    if (selComp.getPin() >= 0 && NrOfPins == 1) {
      /* single pin only */
      map.unmap(selComp.getPin());
      return map.tryMap(selComp.getPin(), this, 0);
    }
    if (selComp.getPin() >= 0 && selectedPin >= 0) {
      /* single pin on a selected Pin */
      map.unmap(selComp.getPin());
      return map.tryMap(selComp.getPin(), this, selectedPin);
    }
    if (isCompleteMap()) {
      /* complete map */
      map.unmap();
      return map.tryMap(this);
    }
    /* in case of a dipswitch on dipswitch we are doing some more intelligent approach */
    if (MyType.equals(IOComponentTypes.DIPSwitch) && (map.getComponentFactory() instanceof DipSwitch)) {
      var nrOfSwitches = map.getAttributeSet().getValue(DipSwitch.ATTR_SIZE).getWidth();
      if ((nrOfSwitches + selectedPin) <= NrOfPins) {
        map.unmap();
        var canMap = true;
        for (var i = 0; i < nrOfSwitches; i++)
          canMap &= map.tryMap(i, this, i + selectedPin);
        if (!canMap) map.unmap();
        return canMap;
      }
    }
    var diag = new PartialMapDialog(selComp, this, parent);
    return diag.doit();
  }

  private void paintmapped(Graphics2D g, float scale, int nrOfMaps) {
    final var x = AppPreferences.getScaled(MyRectangle.getXpos(), scale);
    final var y = AppPreferences.getScaled(MyRectangle.getYpos(), scale);
    final var width = AppPreferences.getScaled(MyRectangle.getWidth(), scale);
    final var height = AppPreferences.getScaled(MyRectangle.getHeight(), scale);
    var alpha = highlighted && selectable ? 200 : 100;
    final var color = containsMap() ? BoardManipulator.SELECTED_MAPPED_COLOR_ID :
        selectable ? BoardManipulator.SELECTABLE_MAPPED_COLOR_ID :
        BoardManipulator.MAPPED_COLOR_ID;
    var col = BoardManipulator.getColor(color);
    if (col == null) return;
    g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha));
    for (var i = 0; i < NrOfPins; i++) {
      alpha = !highlighted || !selectable ? 100 : (i == selectedPin && !isCompleteMap()) ? 255 : 150;
      if (pinIsMapped.get(i) != null) {
        col = BoardManipulator.getColor(color);
        IOComponentTypes.paintPartialMap(g, i, height, width, NrOfPins, nrOfRows, nrOfColumns,
            myRotation, x, y, col, alpha, MyType);
      } else if (selectable) {
        col = BoardManipulator.getColor(BoardManipulator.SELECTABLE_COLOR_ID);
        IOComponentTypes.paintPartialMap(g, i, height, width, NrOfPins, nrOfRows, nrOfColumns,
            myRotation, x, y, col, alpha, MyType);
      }
    }
  }

  protected void paintselected(Graphics2D g, float scale) {
    if (!selectable) return;
    final var x = AppPreferences.getScaled(MyRectangle.getXpos(), scale);
    final var y = AppPreferences.getScaled(MyRectangle.getYpos(), scale);
    final var width = AppPreferences.getScaled(MyRectangle.getWidth(), scale);
    final var height = AppPreferences.getScaled(MyRectangle.getHeight(), scale);
    var alpha = 150;
    var col = BoardManipulator.getColor(BoardManipulator.SELECTABLE_COLOR_ID);
    if (col == null) return;
    if (NrOfPins == 0 && selectable) {
      alpha = highlighted ? 150 : 100;
      IOComponentTypes.paintPartialMap(g, 0, height, width, NrOfPins, nrOfRows, nrOfColumns,
          myRotation, x, y, col, alpha, MyType);
    }
    for (var i = 0; i < NrOfPins; i++) {
      alpha = !highlighted ? 100 : (i == selectedPin && !isCompleteMap()) ? 255 : 150;
      if (pinIsMapped.get(i) != null || selectable) {
        IOComponentTypes.paintPartialMap(g, i, height, width, NrOfPins, nrOfRows, nrOfColumns,
            myRotation, x, y, col, alpha, MyType);
      }
    }
  }

}
