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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import com.cburch.logisim.circuit.CircuitMapInfo;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.fpga.designrulecheck.BubbleInformationContainer;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.std.io.SevenSegment;

public class MapComponent {

  public static final String MAP_KEY = "key";
  public static final String COMPLETE_MAP = "map";
  public static final String OPEN_KEY = "open";
  public static final String CONSTANT_KEY = "vconst";
  public static final String PIN_MAP = "pmap";
  public static final String NO_MAP = "u";
  
  private class MapClass {
    private FPGAIOInformationContainer IOcomp;
    private Integer pin;
    
    public MapClass(FPGAIOInformationContainer IOcomp,Integer pin) {
      this.IOcomp = IOcomp;
      this.pin = pin;
    }

    public void unmap() {
      IOcomp.unmap(pin);
    }
    
    public boolean update(MapComponent comp) {
      return IOcomp.updateMap(pin, comp);
    }
    
    public FPGAIOInformationContainer getIOComp() { return IOcomp; }
    public int getIOPin() { return pin; }
    public void setIOPin(int value) { pin = value; }
  }

  /* 
   * In the below structure the first Integer is the pin identifier, the second is the global bubble id
   */
  private HashMap<Integer,Integer> MyInputBubles = new HashMap<Integer,Integer>();
  private HashMap<Integer,Integer> MyOutputBubles = new HashMap<Integer,Integer>();
  private HashMap<Integer,Integer> MyIOBubles = new HashMap<Integer,Integer>();
  /*
   * The following structure defines if the pin is mapped
   */
  private ComponentFactory myFactory;
  
  private ArrayList<String> myName;
  
  private ArrayList<MapClass> maps = new ArrayList<MapClass>();
  private ArrayList<Boolean> opens = new ArrayList<Boolean>();
  private ArrayList<Integer> constants = new ArrayList<Integer>();
  private ArrayList<String> pinLabels = new ArrayList<String>();
  
  private int NrOfPins;
  
  public MapComponent(ArrayList<String> name, NetlistComponent comp) {
    myFactory = comp.GetComponent().getFactory();
    myName = name;
    ComponentMapInformationContainer mapInfo = comp.GetMapInformationContainer();
    ArrayList<String> bName = new ArrayList<String>();
    for (int i = 1 ; i < name.size() ; i++) bName.add(name.get(i));
    BubbleInformationContainer BubbleInfo = comp.GetGlobalBubbleId(bName);
    NrOfPins = 0;
    for (int i = 0 ; i < mapInfo.GetNrOfInports() ; i++) {
      maps.add(null);
      opens.add(false);
      constants.add(-1);
      int idx = BubbleInfo == null ? -1 : BubbleInfo.GetInputStartIndex()+i;
      pinLabels.add(mapInfo.GetInportLabel(i));
      MyInputBubles.put(NrOfPins++, idx);
    }
    for (int i = 0 ; i < mapInfo.GetNrOfOutports() ; i++) {
      maps.add(null);
      opens.add(false);
      constants.add(-1);
      int idx = BubbleInfo == null ? -1 : BubbleInfo.GetOutputStartIndex()+i; 
      pinLabels.add(mapInfo.GetOutportLabel(i));
      MyOutputBubles.put(NrOfPins++, idx);
    }
    for (int i = 0 ; i < mapInfo.GetNrOfInOutports() ; i++) {
      maps.add(null);
      opens.add(false);
      constants.add(-1);
      int idx = BubbleInfo == null ? -1 : BubbleInfo.GetInOutStartIndex()+i; 
      pinLabels.add(mapInfo.GetInOutportLabel(i));
      MyIOBubles.put(NrOfPins++, idx);
    }
  }
  
  public int getNrOfPins() { return NrOfPins; }
  public boolean hasInputs() { return MyInputBubles.keySet().size() != 0; }
  public boolean hasOutputs() { return MyOutputBubles.keySet().size() != 0; }
  public boolean hasIOs() { return MyIOBubles.keySet().size() != 0; }
  public boolean isInput(int pin) { return MyInputBubles.containsKey(pin); }
  public boolean isOutput(int pin) { return MyOutputBubles.containsKey(pin); }
  public boolean isIO(int pin) { return MyIOBubles.containsKey(pin); }
  public int nrInputs() { return MyInputBubles.keySet().size(); }
  public int nrOutputs() { return MyOutputBubles.keySet().size(); }
  public int nrIOs() { return MyIOBubles.keySet().size(); }
  
  public int getIOBublePinId(int id) {
	for (int key : MyIOBubles.keySet())
	  if (MyIOBubles.get(key) == id) return key;
    return -1;
  }
  
  public String getPinLocation(int pin) {
    if (pin < 0 || pin >= NrOfPins) return null;
    if (maps.get(pin) == null) return null;
    int iopin = maps.get(pin).getIOPin();
    return maps.get(pin).getIOComp().getPinLocation(iopin);
  }
  
  public boolean isMapped(int pin) {
    if (pin < 0 || pin >= NrOfPins) return false;
    if (maps.get(pin)!= null) return true;
    if (opens.get(pin)) return true;
    if (constants.get(pin) >= 0) return true;
    return false;
  }
  
  public boolean isBoardMapped(int pin) {
    if (pin < 0 || pin >= NrOfPins) return false;
    if (maps.get(pin)!= null) return true;
    return false;
  }
  
  public boolean isExternalInverted(int pin) {
    if (pin < 0 || pin >= NrOfPins) return false;
    if (maps.get(pin) == null) return false;
    if (maps.get(pin).getIOComp().GetActivityLevel()==PinActivity.ActiveLow) return true;
    return false;
  }
  
  public boolean requiresPullup(int pin) {
    if (pin < 0 || pin >= NrOfPins) return false;
    if (maps.get(pin) == null) return false;
    if (maps.get(pin).getIOComp().GetPullBehavior() == PullBehaviors.PullUp) return true;
    return false;
  }
  
  public FPGAIOInformationContainer getFpgaInfo(int pin) {
    if (pin < 0 || pin >= NrOfPins) return null;
    if (maps.get(pin) == null) return null;
    return maps.get(pin).getIOComp();
  }
  
  public boolean equalsType(NetlistComponent comp) {
    return myFactory.equals(comp.GetComponent().getFactory());
  }
  
  public void unmap(int pin) {
    if (pin < 0 || pin >= maps.size()) return;
    MapClass map = maps.get(pin);
    maps.set(pin,null);
    if (map != null) map.unmap();
    opens.set(pin, false);
    constants.set(pin, -1);
  }
  
  public void unmap() {
    for (int i = 0 ; i < NrOfPins ; i++) {
      MapClass map = maps.get(i);
      if (map != null) {
        map.unmap();
      }
      opens.set(i, false);
      constants.set(i, -1);
    }
  }
  
  public void copyMapFrom(MapComponent comp) {
    if (comp.NrOfPins != NrOfPins || !comp.myFactory.equals(myFactory)) {
      comp.unmap();
      return;
    }
    maps = comp.maps;
    opens = comp.opens;
    constants = comp.constants;
    for (int i = 0 ; i < NrOfPins ; i++) {
      MapClass map = maps.get(i);
      if (map != null) 
        if (!map.update(this))
          unmap(i);
    }
  }
  
  public void tryMap(CircuitMapInfo cmap, List<FPGAIOInformationContainer> IOcomps) {
    if (cmap.isOpen()) {
      if (cmap.isSinglePin()) {
        int pin = cmap.getPinId();
        if (pin < 0 || pin >= NrOfPins) return;
        unmap(pin);
        constants.set(pin, -1);
        opens.set(pin, true);
      } else {
        for (int i = 0 ; i < NrOfPins ; i++) {
          unmap(i);
          constants.set(i, -1);
          opens.set(i, true);
        }
      }
    } else if (cmap.isConst()) {
      if (cmap.isSinglePin()) {
        int pin = cmap.getPinId();
        if (pin < 0 || pin >= NrOfPins) return;
        unmap(pin);
        opens.set(pin, false);
        constants.set(pin, cmap.getConstValue().intValue()&1);
      } else {
        long mask = 1L;
        long val = cmap.getConstValue();
        for (int i = 0 ; i < NrOfPins; i++) {
          unmap(i);
          opens.set(i, false);
          int value = (val&mask) == 0 ? 0 : 1;
          constants.set(i, value);
          mask <<= 1;
        }
      }
    } if (cmap.getPinMaps()==null) {
      BoardRectangle rect = cmap.getRectangle();
      for (FPGAIOInformationContainer comp : IOcomps) {
        if (comp.GetRectangle().PointInside(rect.getXpos(), rect.getYpos())) {
          if (cmap.isSinglePin()) {
            tryMap(cmap.getPinId(), comp, cmap.getIOId());
          } else {
            tryMap(comp);
          }
          break;
        }
      }
    } else {
      ArrayList<CircuitMapInfo> pmaps = cmap.getPinMaps();
      if (pmaps.size() != NrOfPins) return;
      for (int i = 0 ; i < NrOfPins ; i++) {
    	opens.set(i, false);
    	constants.set(i, -1);
    	if (maps.get(i) != null) maps.get(i).unmap();
        if (pmaps.get(i) == null) continue;
        if (pmaps.get(i).isOpen()) {
          opens.set(i, true);
          continue;
        }
        if (pmaps.get(i).isConst()) {
          constants.set(i, pmaps.get(i).getConstValue().intValue());
          continue;
        }
        if (pmaps.get(i).isSinglePin()) {
          tryMap(pmaps.get(i),IOcomps);
        }
      }
    }
  }
  
  public boolean tryMap(int myPin, FPGAIOInformationContainer comp, int compPin) {
    if (myPin < 0 || myPin >= NrOfPins) return false;
    MapClass map = new MapClass(comp,compPin);
    if (!comp.tryMap(this, myPin, compPin)) return false;
    maps.set(myPin, map);
    opens.set(myPin, false);
    constants.set(myPin, -1);
    return true;
  }
  
  public boolean tryMap(String PinKey, CircuitMapInfo cmap, List<FPGAIOInformationContainer> IOcomps) {
    /* this is for backward compatibility */
	String[] parts = PinKey.split("#");
	String number = null;
	if (parts.length != 2) return false;
	if (parts[1].contains("Pin")) {
	  number = parts[1].substring(3);
	} else if (parts[1].contains("Button")) {
	  number = parts[1].substring(6);
	} else {
	  int id = 0;
	  for (String key : SevenSegment.GetLabels()) {
	    if (parts[1].equals(key)) number = Integer.toString(id);
	    id++;
	  }
	}
	if (number != null) {
	  try {
	    int pinId = Integer.parseUnsignedInt(number);
	    for (FPGAIOInformationContainer comp : IOcomps) {
	      if (comp.GetRectangle().PointInside(cmap.getRectangle().getXpos(), cmap.getRectangle().getYpos())) {
	        return tryMap(pinId,comp,0);
	      }
	    }
	  } catch (NumberFormatException e) {
	    return false;
	  }
	}
    return false;
  }
  
  public boolean tryConstantMap(int pin, long value) {
    if (pin < 0) {
      long maskinp = 1L;
      boolean change = false;
      for (int i = 0 ; i < NrOfPins; i++) {
        if (MyInputBubles.containsKey(i)) {
          if (maps.get(i) != null) maps.get(i).unmap();
          maps.set(i, null);
          constants.set(i, (value&maskinp) == 0 ? 0 : 1);
          opens.set(i, false);
          maskinp <<= 1;
          change = true;
        }
      }
      return change;
    } else {
      if (MyInputBubles.containsKey(pin)) {
        if (maps.get(pin) != null) maps.get(pin).unmap();
        maps.set(pin, null);
        constants.set(pin, (int)(value&1));
        opens.set(pin, false);
        return true;
      }
    }
    return false;
  }
  
  public boolean tryOpenMap(int pin) {
    if (pin < 0) {
      for (int i = 0 ; i < NrOfPins ; i++) {
        if (MyOutputBubles.containsKey(i) || MyIOBubles.containsKey(i)) {
          if (maps.get(i) != null)  maps.get(i).unmap();
          maps.set(i, null);
          constants.set(i, -1);
          opens.set(i, true);
        }
      }
      return true;
    } else  if (MyOutputBubles.containsKey(pin) || MyIOBubles.containsKey(pin)) {
      if (maps.get(pin) != null) {
        maps.get(pin).unmap();
      }
      maps.set(pin, null);
      constants.set(pin, -1);
      opens.set(pin, true);
      return true;
    }
    return false;
  }
   
  public boolean tryMap(FPGAIOInformationContainer comp) {
    /* first we make a copy of the current map in case we have to restore */
    ArrayList<MapClass> oldmaps = new ArrayList<MapClass>();
    ArrayList<Boolean> oldOpens = new ArrayList<Boolean>();
    ArrayList<Integer> oldConstants = new ArrayList<Integer>();
    for (int i = 0 ; i < NrOfPins; i++) {
      oldmaps.add(maps.get(i));
      oldOpens.add(opens.get(i));
      oldConstants.add(constants.get(i));
    }
    boolean success = true;
    for (int i = 0 ; i < NrOfPins; i++) {
      MapClass newMap = new MapClass(comp,-1);
      MapClass oldMap = maps.get(i);
      if (oldMap != null) oldMap.unmap();
      if (MyInputBubles.containsKey(i)) {
        FPGAIOInformationContainer.MapResultClass res = comp.tryInputMap(this, i, i);
        success &= res.mapResult;
        newMap.setIOPin(res.pinId);
      } else if (MyOutputBubles.containsKey(i)) {
        int outputid = i-(MyInputBubles == null ? 0 : MyInputBubles.size()); 
        FPGAIOInformationContainer.MapResultClass res = comp.tryOutputMap(this, i, outputid);
        success &= res.mapResult;
        newMap.setIOPin(res.pinId);
      } else if (MyIOBubles.containsKey(i)) {
        int ioid = i-(MyInputBubles == null ? 0 : MyInputBubles.size())-(MyOutputBubles == null ? 0 : MyOutputBubles.size());
        FPGAIOInformationContainer.MapResultClass res = comp.tryIOMap(this, i, ioid);
        success &= res.mapResult;
        newMap.setIOPin(res.pinId);
      } else {
        success = false;
        break;
      }
      if (success) {
        maps.set(i, newMap);
        opens.set(i, false);
        constants.set(i, -1);
      }
    }
    if (!success) {
      /* restore the old situation */
      for (int i = 0 ; i < NrOfPins; i++) {
    	maps.get(i).unmap();
        MapClass map = oldmaps.get(i);
        if (map != null) {
          if (tryMap(i,map.getIOComp(),map.getIOPin()))
            maps.set(i, map);
        }
        opens.set(i, oldOpens.get(i));
        constants.set(i, oldConstants.get(i));
      }
    }
    return success;
  }
  
  public boolean hasMap() {
    for (int i = 0 ; i < NrOfPins ; i++) {
      if (opens.get(i)) return true;
      if (constants.get(i) >= 0) return true;
      if (maps.get(i) != null) return true;
    }
    return false;
  }
  
  public boolean isNotMapped() {
    for (int i = 0 ; i < NrOfPins ; i++) {
      if (opens.get(i)) return false;
      if (constants.get(i) >= 0) return false;
      if (maps.get(i) != null) return false;
    }
    return true;
  }
  
  public boolean IsOpenMapped(int pin) {
    if (pin < 0 || pin >= NrOfPins) return true;
    return opens.get(pin);
  }
  
  public boolean IsConstantMapped(int pin) {
    if (pin < 0 || pin >= NrOfPins) return false;
    return (constants.get(pin) >= 0);
  }
  
  public boolean isZeroConstantMap(int pin) {
    if (pin < 0 || pin >= NrOfPins) return true;
    return constants.get(pin) == 0;
  }
  
  public boolean isCompleteMap(boolean bothSides) {
	FPGAIOInformationContainer io = null;
	int nrConstants = 0;
	int nrOpens = 0;
	int nrMaps = 0;
    for (int i = 0 ; i < NrOfPins; i++) {
      if (opens.get(i)) nrOpens++;
      else if (constants.get(i)>= 0) nrConstants++;
      else if (maps.get(i) != null) {
    	nrMaps++;
        if (io==null) io = maps.get(i).IOcomp;
        else if (!io.equals(maps.get(i).IOcomp)) return false;
      } else return false;
    }
    if (nrOpens != 0 && nrOpens == NrOfPins) return true;
    if (nrConstants != 0 && nrConstants == NrOfPins) return true;
    if (nrMaps != 0 && nrMaps == NrOfPins) return bothSides ? io.isCompletelyMappedBy(this) : true;
    return false;
  }
  
  public String getHdlString(int pin) {
	if (pin < 0 || pin >= NrOfPins) return null;
    StringBuffer s = new StringBuffer();
    /* The first element is the BoardName, so we skip */
    for (int i = 1 ; i < myName.size() ; i++) s.append((i==1?"":"_")+myName.get(i));
    s.append((s.length()==0 ? "" : "_")+pinLabels.get(pin));
    return s.toString();
  }
  
  public String getHdlSignalName(int pin, String HDLType) {
	if (pin < 0 || pin >= NrOfPins) return null;
    String BracketOpen = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? "(" : "[";
    String BracketClose = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? ")" : "]";
    if (MyInputBubles.containsKey(pin) && MyInputBubles.get(pin) >= 0) {
      return "s_"+HDLGeneratorFactory.LocalInputBubbleBusname+BracketOpen+Integer.toString(MyInputBubles.get(pin))+BracketClose;
    }
    if (MyOutputBubles.containsKey(pin) && MyOutputBubles.get(pin) >= 0) {
      return "s_"+HDLGeneratorFactory.LocalOutputBubbleBusname+BracketOpen+Integer.toString(MyOutputBubles.get(pin))+BracketClose;
    }
    StringBuffer s = new StringBuffer();
    s.append("s_");
    /* The first element is the BoardName, so we skip */
    for (int i = 1 ; i < myName.size() ; i++) s.append((i==1?"":"_")+myName.get(i));
    if (NrOfPins > 1)
      s.append(BracketOpen+Integer.toString(pin)+BracketClose);
    return s.toString();
  }
  
  public String getDisplayString(int pin) {
	StringBuffer s = new StringBuffer();
	/* The first element is the BoardName, so we skip */
	for (int i = 1 ; i < myName.size() ; i++) s.append("/"+myName.get(i));
    if (pin >= 0) {
      if (pin < NrOfPins) s.append("#"+pinLabels.get(pin));
      else s.append("#unknown"+pin);
      if (opens.get(pin)) s.append("->"+S.get("MapOpen"));
      if (constants.get(pin)>=0) s.append("->"+(constants.get(pin)&1));
    } else {
      boolean outAllOpens = nrOutputs()>0;
      boolean ioAllOpens = nrIOs()>0;
      boolean inpAllConst = nrInputs()>0;
      boolean ioAllConst = ioAllOpens;
      long inpConst = 0;
      long ioConst = 0;
      String open = S.get("MapOpen");
      for (int i = NrOfPins-1 ; i >= 0 ; i--) {
        if (MyInputBubles.containsKey(i)) {
          inpAllConst &= constants.get(i) >= 0;
          inpConst <<= 1;
          inpConst |= constants.get(i)&1;
        }
        if (MyOutputBubles.containsKey(i)) {
          outAllOpens &= opens.get(i);
        }
        if (MyIOBubles.containsKey(i)) {
          ioAllOpens &= opens.get(i);
          ioAllConst &= constants.get(i) >= 0;
          ioConst <<= 1;
          ioConst |= constants.get(i)&1;
        }
      }
      if (outAllOpens || ioAllOpens || inpAllConst || ioAllConst) s.append("->");
      boolean addcomma = false;
      if (inpAllConst) {
        s.append("0x"+Long.toHexString(inpConst));
        addcomma = true;
      }
      if (outAllOpens) {
        if (addcomma) s.append(",");
        else addcomma=true;
        s.append(open);
      }
      if (ioAllOpens) {
        if (addcomma) s.append(",");
        else addcomma=true;
        s.append(open);
      }
      if (ioAllConst) {
        if (addcomma) s.append(",");
        else addcomma=true;
        s.append("0x"+Long.toHexString(ioConst));
      }
    }
    return s.toString();
  }
  
  public void getMapElement(Element Map) throws DOMException {
	if (!hasMap()) return;
    Map.setAttribute(MAP_KEY, getDisplayString(-1));
    if (isCompleteMap(true)) {
      if (opens.get(0)) {
        Map.setAttribute("open", "open");
      } else if (constants.get(0) >= 0) {
        long value = 0L;
        for (int i = NrOfPins-1 ; i >= 0 ; i--) {
          value <<= 1L;
          value += constants.get(i);
        }
        Map.setAttribute(CONSTANT_KEY, Long.toString(value));
      } else {
    	BoardRectangle rect = maps.get(0).IOcomp.GetRectangle();
        Map.setAttribute(COMPLETE_MAP, rect.getXpos()+","+rect.getYpos());
      }
    } else {
      StringBuffer s = null;
      for (int i = 0 ; i < NrOfPins ; i++) {
        if (s == null) s = new StringBuffer(); 
        else s.append(",");
        if (opens.get(i)) s.append(OPEN_KEY);
        else if (constants.get(i)>= 0) s.append(Integer.toString(constants.get(i)));
        else if (maps.get(i) != null){
          MapClass map = maps.get(i);
          s.append(map.IOcomp.GetRectangle().getXpos()+"_"+map.IOcomp.GetRectangle().getYpos()+"_"+
            Integer.toString(map.pin));
        } else s.append(NO_MAP);
      }
      Map.setAttribute(PIN_MAP, s.toString());
    }
  }
  
  public static void getComplexMap(Element Map, CircuitMapInfo cmap) throws DOMException {
    ArrayList<CircuitMapInfo> pinmaps = cmap.getPinMaps();
    if (pinmaps != null) {
      StringBuffer s = null;
      for (int i = 0 ; i < pinmaps.size() ; i++) {
        if (s == null) s= new StringBuffer();
        else s.append(",");
        if (pinmaps.get(i) == null) {
          s.append(NO_MAP);
        } else {
          CircuitMapInfo map = pinmaps.get(i);
          if (map.isConst())
            s.append(Long.toString(map.getConstValue()));
          else if (map.isOpen()) 
        	s.append(OPEN_KEY);
          else if (map.isSinglePin())
            s.append(map.getRectangle().getXpos()+"_"+map.getRectangle().getYpos()+"_"+map.getIOId());
          else s.append(NO_MAP);
        }
      }
      Map.setAttribute(PIN_MAP, s.toString());
    } else {
      BoardRectangle br = cmap.getRectangle();
      if (br == null) return;
      Map.setAttribute(COMPLETE_MAP, br.getXpos()+","+br.getYpos());
    }
  }
  
  public static CircuitMapInfo getMapInfo(Element map) throws DOMException {
    if (map.hasAttribute(COMPLETE_MAP)) {
      String[] xy = map.getAttribute(COMPLETE_MAP).split(",");
      if (xy.length != 2) return null;
      try {
        int x = Integer.parseUnsignedInt(xy[0]);
        int y = Integer.parseUnsignedInt(xy[1]);
        return new CircuitMapInfo(x,y);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    if (map.hasAttribute(PIN_MAP)) {
      String[] maps = map.getAttribute(PIN_MAP).split(",");
      CircuitMapInfo complexI = new CircuitMapInfo();
      for (int i = 0 ; i < maps.length ; i++) {
        if (maps[i].equals(NO_MAP)) {
          complexI.addPinMap(null);
        } else if (maps[i].equals(OPEN_KEY)) {
          complexI.addPinMap(new CircuitMapInfo());
        } else if (maps[i].contains("_")) {
          String[] parts = maps[i].split("_");
          if (parts.length != 3) return null;
          try {
            int x = Integer.parseUnsignedInt(parts[0]);
            int y = Integer.parseUnsignedInt(parts[1]);
            int pin = Integer.parseUnsignedInt(parts[2]);
            complexI.addPinMap(x,y,pin);
          } catch (NumberFormatException e) {
            return null;
          }
        } else {
          try {
            long c = Long.parseUnsignedLong(maps[i]);
            complexI.addPinMap(new CircuitMapInfo(c));
          } catch (NumberFormatException e) {
            return null;
          }
        }
      }
      return complexI;
    }
    return null;
  }
  
}
