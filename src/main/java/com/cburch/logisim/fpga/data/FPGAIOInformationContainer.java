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

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.download.Download;
import com.cburch.logisim.fpga.file.BoardWriterClass;
import com.cburch.logisim.fpga.gui.BoardManipulator;
import com.cburch.logisim.fpga.gui.FPGAIOInformationSettingsDialog;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.PortIO;
import com.cburch.logisim.std.io.RGBLed;
import com.cburch.logisim.std.io.ReptarLocalBus;
import com.cburch.logisim.std.io.SevenSegment;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class FPGAIOInformationContainer implements Cloneable {

  public static LinkedList<String> GetComponentTypes() {
    LinkedList<String> result = new LinkedList<String>();
    for (IOComponentTypes comp : IOComponentTypes.KnownComponentSet) {
      result.add(comp.toString());
    }
    return result;
  };

  static final Logger logger = LoggerFactory.getLogger(FPGAIOInformationContainer.class);

  private IOComponentTypes MyType;
  private long MyIdentifier;
  private BoardRectangle MyRectangle;
  private Map<Integer, String> MyPinLocations;
  private Integer NrOfPins;
  private char MyPullBehavior;
  private char MyActivityLevel;
  private char MyIOStandard;
  private char MyDriveStrength;
  private String MyLabel;
  private boolean toBeDeleted = false;
  private ArrayList<Boolean> pinIsMapped;
  private int paintColor = BoardManipulator.TRANSPARENT_ID;

  public FPGAIOInformationContainer() {
    MyType = IOComponentTypes.Unknown;
    MyIdentifier = -1;
    MyRectangle = null;
    MyPinLocations = new HashMap<Integer, String>();
    setNrOfPins(0);
    MyPullBehavior = PullBehaviors.Unknown;
    MyActivityLevel = PinActivity.Unknown;
    MyIOStandard = IoStandards.Unknown;
    MyDriveStrength = DriveStrength.Unknown;
    MyLabel = null;
  }

  public FPGAIOInformationContainer(IOComponentTypes Type, BoardRectangle rect, 
                                    IOComponentsInformation iocomps) {
    MyType = Type;
    MyIdentifier = -1;
    MyRectangle = rect;
    MyPinLocations = new HashMap<Integer, String>();
    setNrOfPins(0);
    MyPullBehavior = PullBehaviors.Unknown;
    MyActivityLevel = PinActivity.Unknown;
    MyIOStandard = IoStandards.Unknown;
    MyDriveStrength = DriveStrength.Unknown;
    MyLabel = null;
    if (rect != null) rect.SetLabel(null);
    if (IOComponentTypes.SimpleInputSet.contains(Type)) {
    	FPGAIOInformationSettingsDialog.GetSimpleInformationDialog(false,iocomps,this);
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
    MyIdentifier = -1;
    MyRectangle = null;
    MyPinLocations = new HashMap<Integer, String>();
    setNrOfPins(0);
    MyPullBehavior = PullBehaviors.Unknown;
    MyActivityLevel = PinActivity.Unknown;
    MyIOStandard = IoStandards.Unknown;
    MyDriveStrength = DriveStrength.Unknown;
    MyLabel = null;
    IOComponentTypes SetId = IOComponentTypes.getEnumFromString(DocumentInfo.getNodeName());
    if (IOComponentTypes.KnownComponentSet.contains(SetId)) {
      MyType = SetId;
    } else {
      return;
    }
    NamedNodeMap Attrs = DocumentInfo.getAttributes();
    int x = -1, y = -1, width = -1, height = -1;
    for (int i = 0; i < Attrs.getLength(); i++) {
      Node ThisAttr = Attrs.item(i);
      if (ThisAttr.getNodeName().equals(BoardWriterClass.LocationXString)) {
        x = Integer.parseInt(ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(BoardWriterClass.LocationYString)) {
        y = Integer.parseInt(ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(BoardWriterClass.WidthString)) {
        width = Integer.parseInt(ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(BoardWriterClass.HeightString)) {
        height = Integer.parseInt(ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(BoardWriterClass.PinLocationString)) {
        setNrOfPins(1);
        MyPinLocations.put(0, ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(BoardWriterClass.MultiPinInformationString)) {
        setNrOfPins(Integer.parseInt(ThisAttr.getNodeValue()));
      }
      if (ThisAttr.getNodeName().startsWith(BoardWriterClass.MultiPinPrefixString)) {
        String Id =
            ThisAttr.getNodeName().substring(BoardWriterClass.MultiPinPrefixString.length());
        MyPinLocations.put(Integer.parseInt(Id), ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(BoardWriterClass.LabelString)) {
        MyLabel = ThisAttr.getNodeValue();
      }
      if (ThisAttr.getNodeName().equals(DriveStrength.DriveAttributeString)) {
        MyDriveStrength = DriveStrength.getId(ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(PullBehaviors.PullAttributeString)) {
        MyPullBehavior = PullBehaviors.getId(ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(IoStandards.IOAttributeString)) {
        MyIOStandard = IoStandards.getId(ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(PinActivity.ActivityAttributeString)) {
        MyActivityLevel = PinActivity.getId(ThisAttr.getNodeValue());
      }
    }
    if ((x < 0) || (y < 0) || (width < 1) || (height < 1)) {
      MyType = IOComponentTypes.Unknown;
      return;
    }
    boolean PinsComplete = true;
    for (int i = 0; i < NrOfPins; i++) {
      if (!MyPinLocations.containsKey(i)) {
        logger.warn("Bizar missing pin {} of component!", i);
        PinsComplete = false;
      }
    }
    if (!PinsComplete) {
      MyType = IOComponentTypes.Unknown;
      return;
    }
    if (MyType.equals(IOComponentTypes.DIPSwitch) || MyType.equals(IOComponentTypes.PortIO)) {
      MyType.setNbPins(NrOfPins);
    }
    if (MyType.equals(IOComponentTypes.Pin)) MyActivityLevel = PinActivity.ActiveHigh;
    MyRectangle = new BoardRectangle(x, y, width, height);
    if (MyLabel != null) MyRectangle.SetLabel(MyLabel);
  }

  public void edit(Boolean deleteButton, IOComponentsInformation IOcomps) {
    if (!defined()) return;
    FPGAIOInformationSettingsDialog.GetSimpleInformationDialog(deleteButton,IOcomps,this);
  }
  
  public void setToBeDeleted() { toBeDeleted = true; }
  public boolean isToBeDeleted() { return toBeDeleted; }

  public Boolean defined() {
    return MyIdentifier != -1;
  }

  public char GetActivityLevel() {
    return MyActivityLevel;
  }
  
  public void setActivityLevel(char activity ) {
    MyActivityLevel = activity;
  }
  
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
  
  public String getPinLocation(int index) {
    if (MyPinLocations.containsKey(index))
      return MyPinLocations.get(index);
    return "";
  }
  
  public void setPinLocation(int index, String Value) {
    MyPinLocations.put(index, Value);
  }


  public Element GetDocumentElement(Document doc) {
    if (MyType.equals(IOComponentTypes.Unknown)) {
      return null;
    }
    try {
      Element result = doc.createElement(MyType.toString());
      result.setAttribute(
          BoardWriterClass.LocationXString, Integer.toString(MyRectangle.getXpos()));
      Attr ypos = doc.createAttribute(BoardWriterClass.LocationYString);
      ypos.setValue(Integer.toString(MyRectangle.getYpos()));
      result.setAttributeNode(ypos);
      Attr width = doc.createAttribute(BoardWriterClass.WidthString);
      width.setValue(Integer.toString(MyRectangle.getWidth()));
      result.setAttributeNode(width);
      Attr height = doc.createAttribute(BoardWriterClass.HeightString);
      height.setValue(Integer.toString(MyRectangle.getHeight()));
      result.setAttributeNode(height);
      if (NrOfPins == 1) {
        Attr loc = doc.createAttribute(BoardWriterClass.PinLocationString);
        loc.setValue(MyPinLocations.get(0));
        result.setAttributeNode(loc);
      } else {
        Attr NrPins = doc.createAttribute(BoardWriterClass.MultiPinInformationString);
        NrPins.setValue(NrOfPins.toString());
        result.setAttributeNode(NrPins);
        for (int i = 0; i < NrOfPins; i++) {
          String PinName = BoardWriterClass.MultiPinPrefixString + Integer.toString(i);
          Attr PinX = doc.createAttribute(PinName);
          PinX.setValue(MyPinLocations.get(i));
          result.setAttributeNode(PinX);
        }
      }
      if (MyLabel != null) {
        Attr label = doc.createAttribute(BoardWriterClass.LabelString);
        label.setValue(MyLabel);
        result.setAttributeNode(label);
      }
      if (MyDriveStrength != DriveStrength.Unknown) {
        Attr drive = doc.createAttribute(DriveStrength.DriveAttributeString);
        drive.setValue(DriveStrength.Behavior_strings[MyDriveStrength]);
        result.setAttributeNode(drive);
      }
      if (MyPullBehavior != PullBehaviors.Unknown) {
        Attr pull = doc.createAttribute(PullBehaviors.PullAttributeString);
        pull.setValue(PullBehaviors.Behavior_strings[MyPullBehavior]);
        result.setAttributeNode(pull);
      }
      if (MyIOStandard != IoStandards.Unknown) {
        Attr stand = doc.createAttribute(IoStandards.IOAttributeString);
        stand.setValue(IoStandards.Behavior_strings[MyIOStandard]);
        result.setAttributeNode(stand);
      }
      if (MyActivityLevel != PinActivity.Unknown) {
        Attr act = doc.createAttribute(PinActivity.ActivityAttributeString);
        act.setValue(PinActivity.Behavior_strings[MyActivityLevel]);
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
  
  public void setLabel(String label) {
    MyLabel = label;
  }

  public char GetDrive() {
    return MyDriveStrength;
  }
  
  public void setDrive(char drive) {
    MyDriveStrength = drive;
  }

  public long GetId() {
    return MyIdentifier;
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

  public ArrayList<String> GetPinlocStrings(int Vendor, String direction, int StartId) {
    return Download.GetPinlocStrings(Vendor, direction, StartId, this);
  }

  public char GetPullBehavior() {
    return MyPullBehavior;
  }
  
  public void setPullBehavior( char pull ) {
    MyPullBehavior = pull;
  }

  public BoardRectangle GetRectangle() {
    return MyRectangle;
  }
  
  public static ArrayList<String> getPinLabels(FPGAIOInformationContainer info) {
    IOComponentTypes MyType = info.GetType();
    int NrOfDevicePins = MyType.getNbPins();
    ArrayList<String> PinLabels = new ArrayList<String>();
    switch (MyType) {
      case SevenSegment:
        PinLabels.addAll(SevenSegment.GetLabels());
        break;
      case RGBLED:
        PinLabels.addAll(RGBLed.GetLabels());
        break;
      case DIPSwitch:
        PinLabels.addAll(DipSwitch.GetLabels(NrOfDevicePins));
        break;
      case PortIO:
        PinLabels.addAll(PortIO.GetLabels(NrOfDevicePins));
        break;
      case LocalBus:
        PinLabels.addAll(ReptarLocalBus.GetLabels());
        break;
      default:
        if (NrOfDevicePins == 1) {
          PinLabels.add(S.get("FpgaIoPin"));
        } else {
          for (int i = 0; i < NrOfDevicePins; i++) {
            PinLabels.add(S.fmt("FpgaIoPins", i));
          }
        }
    }
    return PinLabels;
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
    return pinIsMapped.get(index);
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
    rect.SetActiveOnHigh(active.equals(PinActivity.Behavior_strings[PinActivity.ActiveHigh]));
    setNrOfPins(0);
    MyPinLocations.put(0, loc);
    MyPullBehavior = PullBehaviors.getId(pull);
    MyActivityLevel = PinActivity.getId(active);
    MyIOStandard = IoStandards.getId(standard);
    MyIdentifier = 0;
    MyDriveStrength = DriveStrength.getId(drive);
    MyLabel = label;
    if (rect != null) rect.SetLabel(label);
  }

  public void SetId(long id) {
    MyIdentifier = id;
  }

  public void setNrOfPins(int count) {
	if (pinIsMapped == null) pinIsMapped = new ArrayList<Boolean>();
    NrOfPins = count;
    if (count > pinIsMapped.size()) {
      for (int i = pinIsMapped.size(); i < count ; i++)
        pinIsMapped.add(false);
    } else if (count < pinIsMapped.size()) {
      for (int i = pinIsMapped.size()-1 ; i >= count ; i--) 
        pinIsMapped.remove(i);
    }
  }
  
  public void setPaintColor( int colid ) {
    paintColor = colid;
  }
  
  public void paint(Graphics2D g , float scale) {
	Color PaintColor = BoardManipulator.getColor(paintColor);
    Color c = g.getColor();
    g.setColor(PaintColor);
    g.fillRect(AppPreferences.getScaled(MyRectangle.getXpos(),scale), 
               AppPreferences.getScaled(MyRectangle.getYpos(),scale), 
               AppPreferences.getScaled(MyRectangle.getWidth(),scale), 
               AppPreferences.getScaled(MyRectangle.getHeight(),scale));
    g.setColor(c);
  }

}
