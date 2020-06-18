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

import com.cburch.logisim.fpga.file.BoardWriterClass;
import com.cburch.logisim.fpga.gui.BoardManipulator;
import com.cburch.logisim.fpga.gui.FPGAIOInformationSettingsDialog;
import com.cburch.logisim.fpga.gui.PartialMapDialog;
import com.cburch.logisim.prefs.AppPreferences;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class FPGAIOInformationContainer implements Cloneable {

  private class mapType {
    private MapComponent map;
    private int pin;
    
    public mapType(MapComponent map , int pin) {
      this.map = map;
      this.pin = pin;
    }
    
    public void unmap() {
      map.unmap(pin);
    }
    
    public void update(MapComponent map) {
      this.map = map;
    }
  }
  
  public class MapResultClass {
    public boolean mapResult;
    public int pinId;
  }
  
  public static LinkedList<String> GetComponentTypes() {
    LinkedList<String> result = new LinkedList<String>();
    for (IOComponentTypes comp : IOComponentTypes.KnownComponentSet) {
      result.add(comp.toString());
    }
    return result;
  };

  static final Logger logger = LoggerFactory.getLogger(FPGAIOInformationContainer.class);

  private IOComponentTypes MyType;
  protected BoardRectangle MyRectangle;
  private Map<Integer, String> MyPinLocations;
  private HashSet<Integer> MyInputPins;
  private HashSet<Integer> MyOutputPins;
  private HashSet<Integer> MyIOPins;
  private Integer NrOfPins;
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
  protected boolean selectable = false;
  protected MapListModel.MapInfo selComp = null;

  public FPGAIOInformationContainer() {
    MyType = IOComponentTypes.Unknown;
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
    MyRectangle = null;
    MyPinLocations = new HashMap<Integer, String>();
    setNrOfPins(0);
    MyPullBehavior = PullBehaviors.Unknown;
    MyActivityLevel = PinActivity.Unknown;
    MyIOStandard = IoStandards.Unknown;
    MyDriveStrength = DriveStrength.Unknown;
    MyLabel = null;
    ArrayList<String> InputLocs = new ArrayList<String>();
    ArrayList<String> OutputLocs = new ArrayList<String>();
    ArrayList<String> IOLocs = new ArrayList<String>();
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
      if (ThisAttr.getNodeName().equals(BoardWriterClass.RectSetString)) {
        String[] vals = ThisAttr.getNodeValue().split(",");
        if (vals.length == 4) {
          try {
            x = Integer.parseUnsignedInt(vals[0]);
            y = Integer.parseUnsignedInt(vals[1]);
            width = Integer.parseUnsignedInt(vals[2]);
            height = Integer.parseUnsignedInt(vals[3]);
          } catch (NumberFormatException e) {
            x=y=width=height=-1;
          }
        }
      }
      if (ThisAttr.getNodeName().equals(BoardWriterClass.PinLocationString)) {
        setNrOfPins(1);
        MyPinLocations.put(0, ThisAttr.getNodeValue());
      }
      if (ThisAttr.getNodeName().equals(BoardWriterClass.MultiPinInformationString)) {
        setNrOfPins(Integer.parseInt(ThisAttr.getNodeValue()));
      }
      if (ThisAttr.getNodeName().startsWith(BoardWriterClass.MultiPinPrefixString)) {
        String Id = ThisAttr.getNodeName().substring(BoardWriterClass.MultiPinPrefixString.length());
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
      if (ThisAttr.getNodeName().contentEquals(BoardWriterClass.InputSetString)) {
    	for (String loc : ThisAttr.getNodeValue().split(",")) InputLocs.add(loc);
      }
      if (ThisAttr.getNodeName().contentEquals(BoardWriterClass.OutputSetString)) {
      	for (String loc : ThisAttr.getNodeValue().split(",")) OutputLocs.add(loc);
      }
      if (ThisAttr.getNodeName().contentEquals(BoardWriterClass.IOSetString)) {
      	for (String loc : ThisAttr.getNodeValue().split(",")) IOLocs.add(loc);
      }
    }
    if ((x < 0) || (y < 0) || (width < 1) || (height < 1)) {
      MyType = IOComponentTypes.Unknown;
      return;
    }
    int idx = 0;
    for (String loc : InputLocs) {
      MyPinLocations.put(idx, loc);
      if (MyInputPins == null) MyInputPins = new HashSet<Integer>();
      MyInputPins.add(idx++);
    }
    for (String loc : OutputLocs) {
      MyPinLocations.put(idx, loc);
      if (MyOutputPins == null) MyOutputPins = new HashSet<Integer>();
      MyOutputPins.add(idx++);
    }
    for (String loc : IOLocs) {
      MyPinLocations.put(idx, loc);
      if (MyIOPins == null) MyIOPins = new HashSet<Integer>();
      MyIOPins.add(idx++);
    }
    if (idx != 0) setNrOfPins(idx);
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
    /* This code is for backward compatibility */
    if (MyInputPins == null && MyOutputPins == null && MyIOPins == null) {
      int NrInpPins = IOComponentTypes.GetFPGAInputRequirement(MyType);
      int NrOutpPins = IOComponentTypes.GetFPGAOutputRequirement(MyType);
      for (int i = 0 ; i < NrOfPins; i++) {
        if (i < NrInpPins) {
          if (MyInputPins == null) MyInputPins = new HashSet<Integer>();
          MyInputPins.add(i);
        } else if (i < (NrInpPins+NrOutpPins)) {
          if (MyOutputPins == null) MyOutputPins = new HashSet<Integer>();
          MyOutputPins.add(i);
        } else {
          if (MyIOPins == null) MyIOPins = new HashSet<Integer>();
          MyIOPins.add(i);
        }
      }
    }
    /* End backward compatibility */
    if (MyType.equals(IOComponentTypes.Pin)) MyActivityLevel = PinActivity.ActiveHigh;
    MyRectangle = new BoardRectangle(x, y, width, height);
    if (MyLabel != null) MyRectangle.SetLabel(MyLabel);
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
  
  public void edit(Boolean deleteButton, IOComponentsInformation IOcomps) {
    FPGAIOInformationSettingsDialog.GetSimpleInformationDialog(deleteButton,IOcomps,this);
  }
  
  public void setMapMode() { 
    mapMode = true;
    paintColor = BoardManipulator.TRANSPARENT_ID;
  }
  
  public void setToBeDeleted() { toBeDeleted = true; }
  public boolean isToBeDeleted() { return toBeDeleted; }

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
  
  public void setInputPinLocation(int index , String value) {
    if (MyOutputPins != null) MyOutputPins.remove(index);
    if (MyIOPins != null) MyIOPins.remove(index);
    if (MyInputPins == null) MyInputPins = new HashSet<Integer>();
    MyInputPins.add(index);
    MyPinLocations.put(index, value);
  }
  
  public void setOutputPinLocation(int index , String value) {
    if (MyInputPins != null) MyInputPins.remove(index);
    if (MyIOPins != null) MyIOPins.remove(index);
    if (MyOutputPins == null) MyOutputPins = new HashSet<Integer>();
    MyOutputPins.add(index);
    MyPinLocations.put(index, value);
  }
  
  public void setIOPinLocation(int index , String value) {
    if (MyInputPins != null) MyInputPins.remove(index);
    if (MyOutputPins != null) MyOutputPins.remove(index);
    if (MyIOPins == null) MyIOPins = new HashSet<Integer>();
    MyIOPins.add(index);
    MyPinLocations.put(index, value);
  }
  
  public Element GetDocumentElement(Document doc) {
    if (MyType.equals(IOComponentTypes.Unknown)) {
      return null;
    }
    try {
      Element result = doc.createElement(MyType.toString());
      result.setAttribute(BoardWriterClass.RectSetString, MyRectangle.getXpos()+","+MyRectangle.getYpos()+
          ","+MyRectangle.getWidth()+","+MyRectangle.getHeight());
      if (MyLabel != null) {
        Attr label = doc.createAttribute(BoardWriterClass.LabelString);
        label.setValue(MyLabel);
        result.setAttributeNode(label);
      }
      if (MyInputPins != null && !MyInputPins.isEmpty()) {
        Attr Set = doc.createAttribute(BoardWriterClass.InputSetString);
        StringBuffer s = new StringBuffer();
        boolean first = true;
        for (int i = 0 ; i < NrOfPins ; i++)
          if (MyInputPins.contains(i)) {
            if (first) first = false;
            else s.append(",");
            s.append(MyPinLocations.get(i));
          }
        Set.setValue(s.toString());
        result.setAttributeNode(Set);
      }
      if (MyOutputPins != null && !MyOutputPins.isEmpty()) {
        Attr Set = doc.createAttribute(BoardWriterClass.OutputSetString);
        StringBuffer s = new StringBuffer();
        boolean first = true;
        for (int i = 0 ; i < NrOfPins; i++)
          if (MyOutputPins.contains(i)) {
            if (first) first = false;
            else s.append(",");
            s.append(MyPinLocations.get(i));
          }
        Set.setValue(s.toString());
        result.setAttributeNode(Set);
      }
      if (MyIOPins != null && !MyIOPins.isEmpty()) {
        Attr Set = doc.createAttribute(BoardWriterClass.IOSetString);
        StringBuffer s = new StringBuffer();
        boolean first = true;
        for (int i = 0 ; i < NrOfPins; i++)
          if (MyIOPins.contains(i)) {
            if (first) first = false;
            else s.append(",");
            s.append(MyPinLocations.get(i));
          }
        Set.setValue(s.toString());
        result.setAttributeNode(Set);
      }
      if (MyDriveStrength != DriveStrength.Unknown && MyDriveStrength != DriveStrength.DefaulStength) {
        Attr drive = doc.createAttribute(DriveStrength.DriveAttributeString);
        drive.setValue(DriveStrength.Behavior_strings[MyDriveStrength]);
        result.setAttributeNode(drive);
      }
      if (MyPullBehavior != PullBehaviors.Unknown && MyPullBehavior != PullBehaviors.Float) {
        Attr pull = doc.createAttribute(PullBehaviors.PullAttributeString);
        pull.setValue(PullBehaviors.Behavior_strings[MyPullBehavior]);
        result.setAttributeNode(pull);
      }
      if (MyIOStandard != IoStandards.Unknown && MyIOStandard != IoStandards.DefaulStandard) {
        Attr stand = doc.createAttribute(IoStandards.IOAttributeString);
        stand.setValue(IoStandards.Behavior_strings[MyIOStandard]);
        result.setAttributeNode(stand);
      }
      if (MyActivityLevel != PinActivity.Unknown && MyActivityLevel != PinActivity.ActiveHigh) {
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
  
  public void setPullBehavior( char pull ) {
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
    MyDriveStrength = DriveStrength.getId(drive);
    MyLabel = label;
    if (rect != null) rect.SetLabel(label);
  }

  public void setNrOfPins(int count) {
    if (pinIsMapped == null) pinIsMapped = new ArrayList<mapType>();
    NrOfPins = count;
    if (count > pinIsMapped.size()) {
      for (int i = pinIsMapped.size(); i < count ; i++)
        pinIsMapped.add(null);
    } else if (count < pinIsMapped.size()) {
      for (int i = pinIsMapped.size()-1 ; i >= count ; i--) {
    	mapType map = pinIsMapped.get(i); 
    	if (map != null) map.unmap();
        pinIsMapped.remove(i);
      }
    }
  }
  
  public void unmap(int pin) {
    if (pin < 0 || pin >= pinIsMapped.size()) return;
    mapType map = pinIsMapped.get(pin);
    pinIsMapped.set(pin,null);
    if (map != null) map.unmap();
  }
  
  public MapResultClass tryInputMap(MapComponent comp,int compPin,int inpPin) {
    MapResultClass result = new MapResultClass();
    result.mapResult = false;
    result.pinId = inpPin;
    if (MyInputPins == null || !MyInputPins.contains(result.pinId)) return result;
    unmap(result.pinId);
    mapType map = new mapType(comp,compPin);
    pinIsMapped.set(result.pinId, map);
    result.mapResult = true;
    return result;
  }
  
  public MapResultClass tryOutputMap(MapComponent comp,int compPin,int outpPin) {
    MapResultClass result = new MapResultClass();
    result.mapResult = false;
    result.pinId = outpPin+(MyInputPins == null ? 0 : MyInputPins.size());
    if (MyOutputPins == null || !MyOutputPins.contains(result.pinId)) return result;
    unmap(result.pinId);
    mapType map = new mapType(comp,compPin);
    pinIsMapped.set(result.pinId, map);
    result.mapResult = true;
    return result;
  }
	  
  public MapResultClass tryIOMap(MapComponent comp,int compPin,int ioPin) {
    MapResultClass result = new MapResultClass();
    result.mapResult = false;
    result.pinId = ioPin+(MyInputPins == null ? 0 : MyInputPins.size())+
        (MyOutputPins == null ? 0 : MyOutputPins.size());
    if (MyIOPins == null || !MyIOPins.contains(result.pinId)) return result;
    unmap(result.pinId);
    mapType map = new mapType(comp,compPin);
    pinIsMapped.set(result.pinId, map);
    result.mapResult = true;
    return result;
  }
  
  public boolean tryMap(MapComponent comp, int compPin, int myPin) {
    if (myPin < 0 || myPin >=NrOfPins) return false;
    unmap(myPin);
    mapType map = new mapType(comp,compPin);
    pinIsMapped.set(myPin, map);
    return true;
  }
		  
  public boolean updateMap(int pin , MapComponent comp) {
    if (pin < 0 || pin >= pinIsMapped.size()) return false;
    mapType map = pinIsMapped.get(pin);
    if (map == null) return false;
    map.update(comp);
    return true;
  }
  
  public boolean isCompletelyMappedBy(MapComponent comp) {
    for (int i = 0 ; i < NrOfPins ; i++)
      if (pinIsMapped.get(i) != null) {
        if (!pinIsMapped.get(i).map.equals(comp)) return false;
      } else return false;
    return true;
  }
  
  private int nrOfMaps() {
    int res = 0;
    for (int i = 0 ; i < NrOfPins ; i++)
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
  
  public boolean hasInputs() { return MyInputPins != null && (MyInputPins.size() > 0); } 
  public boolean hasOutputs() { return MyOutputPins != null && (MyOutputPins.size() > 0); }
  public boolean hasIOs() { return MyIOPins != null && (MyIOPins.size() > 0); }
  public int nrInputs() { return MyInputPins == null ? 0 : MyInputPins.size(); }
  public int nrOutputs() { return MyOutputPins == null ? 0 : MyOutputPins.size(); }
  public int nrIOs() { return MyIOPins == null ? 0 : MyIOPins.size(); }
  public HashSet<Integer> getInputs() { return MyInputPins; }
  public HashSet<Integer> getOutputs() { return MyOutputPins; }
  public HashSet<Integer> getIOs() { return MyIOPins; }
  public String getPinName(int index) {
    if (MyInputPins != null && MyInputPins.contains(index)) {
      return IOComponentTypes.getInputLabel(NrOfPins, index, MyType);
    }
    if (MyOutputPins != null && MyOutputPins.contains(index)) {
      return IOComponentTypes.getOutputLabel(NrOfPins, index, MyType);
    }
    if (MyIOPins != null && MyIOPins.contains(index)) {
      return IOComponentTypes.getIOLabel(NrOfPins, index, MyType);
    }
    return ""+index;
  }

  
  public boolean setSelectable(MapListModel.MapInfo comp) {
    selComp = comp;
    MapComponent map = comp.getMap();
    int connect = comp.getPin();
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
    boolean ret = selectable;
    selComp = null;
    selectable = false;
    return ret;
  }
  
  public void paint(Graphics2D g , float scale) {
	if (mapMode) {
	  mappaint(g,scale);
	  return;
	}
    Color PaintColor = BoardManipulator.getColor(paintColor);
    if (PaintColor == null) return;
    Color c = g.getColor();
    g.setColor(PaintColor);
    g.fillRect(AppPreferences.getScaled(MyRectangle.getXpos(),scale), 
               AppPreferences.getScaled(MyRectangle.getYpos(),scale), 
               AppPreferences.getScaled(MyRectangle.getWidth(),scale), 
               AppPreferences.getScaled(MyRectangle.getHeight(),scale));
    g.setColor(c);
  }
  
  private void mappaint(Graphics2D g, float scale) {
	Color c = g.getColor();
    int i = nrOfMaps();
    if (i > 0) paintmapped(g,scale,i);
    else paintselected(g,scale);
    if (highlighted && (i>0 || selectable)) paintinfo(g,scale);
    g.setColor(c);
  }
  
  private boolean containsMap() {
    if (selComp == null) return false;
    MapComponent com = selComp.getMap();
    for (int i = 0 ; i < NrOfPins ; i++) {
      if (pinIsMapped.get(i) != null && pinIsMapped.get(i).map.equals(com)) return true;
    }
    return false;
  }
  
  public boolean tryMap(JPanel parent) {
	if (!selectable) return false;
	if (selComp == null) return false;
	MapComponent map = selComp.getMap();
	if (selComp.getPin() >= 0 && NrOfPins == 1) {
      /* single pin only */
      return map.tryMap(selComp.getPin(), this, 0);
	} 
	if (map.nrInputs() == nrInputs() && 
        map.nrOutputs() == nrOutputs() &&
        map.nrIOs() == nrIOs()&&
        selComp.getPin() < 0) {
	  /* complete map */
	  return map.tryMap(this);
	}
	PartialMapDialog diag = new PartialMapDialog(selComp,this,parent);
    return diag.doit();
  }
  
  private void paintmapped(Graphics2D g, float scale, int nrOfMaps) {
    int x = AppPreferences.getScaled(MyRectangle.getXpos(),scale);
    int y = AppPreferences.getScaled(MyRectangle.getYpos(),scale);
    int width = AppPreferences.getScaled(MyRectangle.getWidth(),scale);
    int height = AppPreferences.getScaled(MyRectangle.getHeight(),scale);
    int alpha = highlighted&&selectable ? 200 : 100;
    int color = containsMap() ? BoardManipulator.SELECTED_MAPPED_COLOR_ID :
        selectable ? BoardManipulator.SELECTABLE_MAPPED_COLOR_ID :
        BoardManipulator.MAPPED_COLOR_ID;
    Color col = BoardManipulator.getColor(color);
    if (col == null) return;
    g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),alpha));
    if (nrOfMaps == NrOfPins) {
      g.fillRect(x, y, width, height);
    } else {
      g.setStroke(new BasicStroke(AppPreferences.getScaled(2, scale)));
      g.drawRect(x, y, width, height);
      g.setStroke(new BasicStroke(1));
      if (height > width) {
        int y1 = y+((height*nrOfMaps)/NrOfPins);
        int y2 = y+((height*(nrOfMaps-1))/NrOfPins);
        int[] xpoints = {x,x+width,x+width,x};
        int[] ypoints = {y,y,y1,y2};
        g.fillPolygon(xpoints, ypoints, 4);
        if (selectable) {
          col = BoardManipulator.getColor(BoardManipulator.SELECTABLE_COLOR_ID);
          if (col == null) return;
          g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),alpha));
          ypoints[0] += height;
          ypoints[1] += height;
          g.fillPolygon(xpoints, ypoints, 4);
        }
      } else {
    	int x1 = x+((width*nrOfMaps)/NrOfPins);
    	int x2 = x+((width*(nrOfMaps-1))/NrOfPins);
    	int[] xpoints = {x,x1,x2,x};
    	int[] ypoints = {y,y,y+height,y+height};
        g.fillPolygon(xpoints, ypoints, 4);
        if (selectable) {
          col = BoardManipulator.getColor(BoardManipulator.SELECTABLE_COLOR_ID);
          if (col == null) return;
          g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),alpha));
          xpoints[0] += width;
          xpoints[3] += width;
          g.fillPolygon(xpoints, ypoints, 4);
        }
      }
    }
  }
  
  protected void paintselected(Graphics2D g, float scale) {
	if (!selectable) return;
    int x = AppPreferences.getScaled(MyRectangle.getXpos(),scale);
    int y = AppPreferences.getScaled(MyRectangle.getYpos(),scale);
    int width = AppPreferences.getScaled(MyRectangle.getWidth(),scale);
    int height = AppPreferences.getScaled(MyRectangle.getHeight(),scale);
    int alpha = highlighted ? 200 : 100;
    Color col = BoardManipulator.getColor(BoardManipulator.SELECTABLE_COLOR_ID);
    if (col == null) return;
    g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),alpha));
    g.fillRect(x, y, width, height);
  }
  
  private void paintinfo(Graphics2D g, float scale) {

  }
}
