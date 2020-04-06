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

package com.cburch.logisim.fpga.fpgaboardeditor;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.gui.FPGAIOInformationSettingsDialog;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.PortIO;
import com.cburch.logisim.std.io.RGBLed;
import com.cburch.logisim.std.io.ReptarLocalBus;
import com.cburch.logisim.std.io.SevenSegment;

import java.util.ArrayList;
import java.util.EnumSet;
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

  public static enum IOComponentTypes {
    LED,
    Button,
    Pin,
    SevenSegment,
    DIPSwitch,
    RGBLED,
    PortIO,
    LocalBus,
    Bus,
    Open,
    Unknown;

    public static IOComponentTypes getEnumFromString(String str) {
      for (IOComponentTypes elem : KnownComponentSet) {
        if (elem.name().equalsIgnoreCase(str)) {
          return elem;
        }
      }
      return IOComponentTypes.Unknown;
    }

    /* AMX: Localbus / Port IO / Pin led buton information about the number of input pins.
     * This is the wrong way to do it. It should be taken from the Xml file!! */
    public static final int GetFPGAInOutRequirement(IOComponentTypes comp) {
      switch (comp) {
        case PortIO:
          return nbSwitch;
        case LocalBus:
          return 16;
        default:
          return 0;
      }
    }

    /* AMX: Localbus / Port IO / Pin led buton information about the number of input pins.
     * This is the wrong way to do it. It should be taken from the Xml file!! */
    public static final int GetFPGAInputRequirement(IOComponentTypes comp) {
      switch (comp) {
        case Button:
          return 1;
        case DIPSwitch:
          return nbSwitch;
        case LocalBus:
          return 13;
        default:
          return 0;
      }
    }

    /* AMX: Localbus / Port IO / Pin led buton information about the number of output pins.
     * This is the wrong way to do it. It should be taken from the Xml file!! */
    public static final int GetFPGAOutputRequirement(IOComponentTypes comp) {
      switch (comp) {
        case LED:
          return 1;
        case SevenSegment:
          return 8;
        case RGBLED:
          return 3;
        case LocalBus:
          return 2;
        default:
          return 0;
      }
    }

    /* AMX: Localbus / Port IO / Pin led buton information about the total of pins pins.
     * This is the wrong way to do it. It should be taken from the Xml file!! */
    public static final int GetNrOfFPGAPins(IOComponentTypes comp) {
      switch (comp) {
        case LED:
        case Button:
        case Pin:
          return 1;
        case DIPSwitch:
        case PortIO:
          return nbSwitch;
        case SevenSegment:
          return 8;
        case RGBLED:
          return 3;
        case LocalBus:
          return 31;
        default:
          return 0;
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
            IOComponentTypes.SevenSegment);

    public static final EnumSet<IOComponentTypes> InOutComponentSet =
        EnumSet.of(IOComponentTypes.Pin, IOComponentTypes.PortIO);

    private static int nbSwitch = 8;

    public void setNbPins(int nb) {
      nbSwitch = nb;
    }
    
    public int getNbPins() { return nbSwitch; }
    
  }

  /*
   * Bus is just a placeholder for a multi-bit pin. It should not be used for
   * mappable components
   */

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

  public FPGAIOInformationContainer() {
    MyType = IOComponentTypes.Unknown;
    MyIdentifier = -1;
    MyRectangle = null;
    MyPinLocations = new HashMap<Integer, String>();
    NrOfPins = 0;
    MyPullBehavior = PullBehaviors.Unknown;
    MyActivityLevel = PinActivity.Unknown;
    MyIOStandard = IoStandards.Unknown;
    MyDriveStrength = DriveStrength.Unknown;
    MyLabel = null;
  }

  public FPGAIOInformationContainer(
      IOComponentTypes Type, BoardRectangle rect, BoardDialog parent) {
    MyType = Type;
    MyIdentifier = -1;
    MyRectangle = rect;
    MyPinLocations = new HashMap<Integer, String>();
    NrOfPins = 0;
    MyPullBehavior = PullBehaviors.Unknown;
    MyActivityLevel = PinActivity.Unknown;
    MyIOStandard = IoStandards.Unknown;
    MyDriveStrength = DriveStrength.Unknown;
    MyLabel = null;
    if (rect != null) rect.SetLabel(null);
    if (IOComponentTypes.SimpleInputSet.contains(Type)) {
    	FPGAIOInformationSettingsDialog.GetSimpleInformationDialog(false,parent,this);
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
    NrOfPins = 0;
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
        NrOfPins = 1;
        MyPinLocations.put(0, ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(BoardWriterClass.MultiPinInformationString)) {
        NrOfPins = Integer.parseInt(ThisAttr.getNodeValue());
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

  public void edit(Boolean deleteButton, BoardDialog parent) {
    if (!defined()) return;
    FPGAIOInformationSettingsDialog.GetSimpleInformationDialog(deleteButton,parent,this);
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

  private ArrayList<String> GetAlteraPinStrings(String direction, int StartId) {
    /*
     * for the time being we ignore the InputPins variable. It has to be
     * implemented for more complex components
     */
    ArrayList<String> Contents = new ArrayList<String>();
    for (int i = 0; i < NrOfPins; i++) {
      String NetName = "";
      if (direction == "in") {
        NetName = HDLGeneratorFactory.FPGAInputPinName + "_" + Integer.toString(StartId + i);
      } else if (direction == "inout") {
        NetName = HDLGeneratorFactory.FPGAInOutPinName + "_" + Integer.toString(StartId + i);
      } else {
        NetName = HDLGeneratorFactory.FPGAOutputPinName + "_" + Integer.toString(StartId + i);
      }
      Contents.add("    set_location_assignment " + MyPinLocations.get(i) + " -to " + NetName);
      if (MyPullBehavior == PullBehaviors.PullUp) {
        Contents.add("    set_instance_assignment -name WEAK_PULL_UP_RESISTOR ON -to " + NetName);
      }
    }
    return Contents;
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
    if (Vendor == VendorSoftware.VendorXilinx) {
      return GetXilinxUCFStrings(direction, StartId);
    }
    if (Vendor == VendorSoftware.VendorAltera) {
      return GetAlteraPinStrings(direction, StartId);
    }
    if (Vendor == VendorSoftware.VendorVivado) {
      return GetVivadoXDCStrings(direction, StartId);
    }
    return new ArrayList<String>();
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

  private ArrayList<String> GetXilinxUCFStrings(String direction, int StartId) {
    ArrayList<String> Contents = new ArrayList<String>();
    StringBuffer Temp = new StringBuffer();
    Integer start = 0;
    Integer end = NrOfPins;
    ArrayList<String> labels = null;
    if (MyType.equals(IOComponentTypes.PortIO)) {
      labels = PortIO.GetLabels(IOComponentTypes.GetNrOfFPGAPins(MyType));
    } else if (MyType.equals(IOComponentTypes.LocalBus)) {
      labels = ReptarLocalBus.GetLabels();
      if (direction.equals("in")) {
        end = IOComponentTypes.GetFPGAInputRequirement(MyType);
      } else if (direction.equals("out")) {
        // TODO: YSY
        Contents.add(
            "NET \"FPGA_LB_OUT_0\" LOC = \"R24\" | IOSTANDARD = LVCMOS18 ; # SP6_LB_WAIT3_o");
        Contents.add("NET \"FPGA_LB_OUT_1\" LOC = \"AB30\" | IOSTANDARD = LVCMOS18 ; # IRQ_o");
        return Contents;
        // start = IOComponentTypes.GetFPGAInputRequirement(MyType);
        // end = start +
        // IOComponentTypes.GetFPGAOutputRequirement(MyType);
      } else if (direction.equals("inout")) {
        start =
            IOComponentTypes.GetFPGAInputRequirement(MyType)
                + IOComponentTypes.GetFPGAOutputRequirement(MyType);
        end = start + IOComponentTypes.GetFPGAInOutRequirement(MyType);
      }
    } else if (MyType.equals(IOComponentTypes.DIPSwitch)) {
      labels = DipSwitch.GetLabels(IOComponentTypes.GetNrOfFPGAPins(MyType));
    } else if (MyType.equals(IOComponentTypes.SevenSegment)) {
      labels = SevenSegment.GetLabels();
    } else if (MyType.equals(IOComponentTypes.RGBLED)) {
      labels = RGBLed.GetLabels();
    }
    for (int i = start; i < end; i++) {
      Temp.setLength(0);
      Temp.append("LOC = \"" + MyPinLocations.get(i) + "\" ");
      if (MyPullBehavior != PullBehaviors.Unknown && MyPullBehavior != PullBehaviors.Float) {
        Temp.append("| " + PullBehaviors.getContraintedPullString(MyPullBehavior) + " ");
      }
      if (MyDriveStrength != DriveStrength.Unknown
          && MyDriveStrength != DriveStrength.DefaulStength) {
        Temp.append(
            "| DRIVE = " + DriveStrength.GetContraintedDriveStrength(MyDriveStrength) + " ");
      }
      if (MyIOStandard != IoStandards.Unknown && MyIOStandard != IoStandards.DefaulStandard) {
        Temp.append("| IOSTANDARD = " + IoStandards.GetConstraintedIoStandard(MyIOStandard) + " ");
      }
      Temp.append(";");
      if (labels != null) {
        Temp.append(" # " + labels.get(i));
      }
      String NetName = "";
      if (direction == "in") {
        NetName =
            HDLGeneratorFactory.FPGAInputPinName + "_" + Integer.toString(StartId + i - start);
      } else if (direction == "inout") {
        NetName =
            HDLGeneratorFactory.FPGAInOutPinName + "_" + Integer.toString(StartId + i - start);
      } else {
        NetName =
            HDLGeneratorFactory.FPGAOutputPinName + "_" + Integer.toString(StartId + i - start);
      }
      Contents.add("NET \"" + NetName + "\" " + Temp.toString());
    }
    return Contents;
  }

  private ArrayList<String> GetVivadoXDCStrings(String direction, int StartId) {
    ArrayList<String> contents = new ArrayList<String>();
    for (int i = 0; i < NrOfPins; i++) {
      String netName = "";
      if (direction.equals("in")) {
        netName = HDLGeneratorFactory.FPGAInputPinName + "_" + Integer.toString(StartId + i);
      } else if (direction.equals("inout")) {
        netName = HDLGeneratorFactory.FPGAInOutPinName + "_" + Integer.toString(StartId + i);
      } else {
        netName = HDLGeneratorFactory.FPGAOutputPinName + "_" + Integer.toString(StartId + i);
      }
      contents.add(
          "set_property PACKAGE_PIN " + MyPinLocations.get(i) + " [get_ports {" + netName + "}]");

      if (MyIOStandard != IoStandards.Unknown && MyIOStandard != IoStandards.DefaulStandard) {
        contents.add(
            "    set_property IOSTANDARD "
                + IoStandards.GetConstraintedIoStandard(MyIOStandard)
                + " [get_ports {"
                + netName
                + "}]");
      }
      if (MyIOStandard != IoStandards.Unknown && MyIOStandard != IoStandards.DefaulStandard) {
        contents.add(
            "    set_property IOSTANDARD "
                + IoStandards.GetConstraintedIoStandard(MyIOStandard)
                + " [get_ports {"
                + netName
                + "}]");
      }
    }
    return contents;
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
    NrOfPins = 1;
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
    NrOfPins = count;
  }

}
