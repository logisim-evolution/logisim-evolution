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

package com.cburch.logisim.std.gates;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Constant;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class CircuitBuilder {

  private static int SPINE_DISTANCE = 10;
  private static int BUS_SPINE_TO_WIRE_SPINE_DISTANCE = 20;
  private static int MINIMAL_PIN_DISTANCE = 30;
  private static int SPLITTER_HEIGHT = 20;
  private static int TOP_BORDER = 40; // minimal value due to constants
  private static int INVERTER_WIDTH = 30;
  private static int NAND_WIDTH = 40;
  private static int GATE_HEIGHT = 40;

  private static class CompareYs implements Comparator<Location> {
    public int compare(Location a, Location b) {
      return a.getY() - b.getY();
    }
  }

  private static class InputData {
    int startX;
    int startY;
    int pinX;
    private ArrayList<String> names = new ArrayList<String>();
    private HashMap<String, SingleInput> inputs = new HashMap<String, SingleInput>();
    private HashMap<String, SingleInput> inverted_inputs = new HashMap<String, SingleInput>();

    InputData() {}

    public void addInput(String Name, SingleInput Info) {
      inputs.put(Name, Info);
      names.add(Name);
    }

    public int CreateInvertedLocs(int SpineX) {
      int cur = SpineX + SPINE_DISTANCE;
      addInput("0", new SingleInput(cur)); // Constant zero line
      cur += 20;
      addInput("1", new SingleInput(cur)); // Constant one line
      cur += NAND_WIDTH + 20;
      for (int i = 0; i < NrOfInputs(); i++) {
        inverted_inputs.put(names.get(i), new SingleInput(cur));
        cur += SPINE_DISTANCE;
      }
      return cur;
    }

    public int GetInverterXLoc() {
      boolean hasOne = !inputs.get("1").ys.isEmpty();
      boolean hasZero = !inputs.get("0").ys.isEmpty();
      if (hasOne) return inverted_inputs.get(names.get(0)).spineX - 10;
      if (hasZero) return inverted_inputs.get(names.get(0)).spineX - 20;
      else return inverted_inputs.get(names.get(0)).spineX - 20 - SPINE_DISTANCE;
    }

    public boolean hasInvertedConnections(String name) {
      SingleInput inp = inverted_inputs.get(name);
      if (inp == null) return false;
      return !inp.ys.isEmpty();
    }

    int NrOfInputs() {
      return names.size();
    }

    int getSpineX(String input, boolean inverted) {
      SingleInput data = (inverted) ? inverted_inputs.get(input) : inputs.get(input);
      return data.spineX;
    }

    int getStartX() {
      return startX;
    }

    int getStartY() {
      return startY;
    }

    int getPinX() {
      return pinX;
    }

    public int InverterHeight() {
      int nr = names.size();
      if (names.contains("0")) nr--;
      if (names.contains("1")) nr--;
      return nr * GATE_HEIGHT;
    }

    public SingleInput getInputLocs(String Name, boolean inverted) {
      return inverted ? inverted_inputs.get(Name) : inputs.get(Name);
    }

    public String getInputName(int index) {
      if ((index < 0) || (index >= NrOfInputs())) return null;
      return names.get(index);
    }

    void registerConnection(String input, Location loc, boolean inverted) {
      SingleInput data = (inverted) ? inverted_inputs.get(input) : inputs.get(input);
      data.ys.add(loc);
    }
  }

  private static class Layout {
    // initialized by parent
    int y; // top edge relative to parent's top edge
    // (or edge corresponding to input)

    // initialized by self
    int width;
    int height;
    ComponentFactory factory;
    AttributeSet attrs;
    int outputY; // where output is relative to my top edge
    int subX; // where right edge of sublayouts should be relative to my
    // left edge
    Layout[] subLayouts;
    String inputName; // for references directly to inputs
    boolean inverted;

    Layout(
        int width,
        int height,
        int outputY,
        ComponentFactory factory,
        AttributeSet attrs,
        Layout[] subLayouts,
        int subX) {
      this.width = width;
      this.height = roundUp(height);
      this.outputY = outputY;
      this.factory = factory;
      this.attrs = attrs;
      this.subLayouts = subLayouts;
      this.subX = subX;
      this.inputName = null;
    }

    Layout(String inputName, boolean inverted) {
      this(0, 0, 0, null, null, null, 0);
      this.inputName = inputName;
      this.inverted = inverted;
    }
  }

  private static class SingleInput {
    int spineX;
    int spineY;
    ArrayList<Location> ys = new ArrayList<Location>();

    SingleInput(int spineX) {
      this.spineX = spineX;
    }

    SingleInput(int spineX, int spineY) {
      this.spineX = spineX;
      this.spineY = spineY;
    }
  }

  public static CircuitMutation build(
      Circuit destCirc, AnalyzerModel model, boolean twoInputs, boolean useNands) {
    CircuitMutation result = new CircuitMutation(destCirc);
    result.clear();

    Layout[] layouts = new Layout[model.getOutputs().bits.size()];
    int maxWidth = 0;
    for (int i = 0; i < layouts.length; i++) {
      String output = model.getOutputs().bits.get(i);
      Expression expr = model.getOutputExpressions().getExpression(output);
      CircuitDetermination det = CircuitDetermination.create(expr);
      if (det != null) {
        if (twoInputs) det.convertToTwoInputs();
        if (useNands) det.convertToNands();
        det.repair();
        layouts[i] = layoutGates(det);
        maxWidth = Math.max(maxWidth, layouts[i].width);
      } else {
        layouts[i] = null;
      }
    }

    InputData inputData = computeInputData(model);
    InputData outputData = new InputData();
    outputData.startY = inputData.startY;
    int x = inputData.getStartX();
    int y = inputData.getStartY() + inputData.InverterHeight();
    int outputX = x + maxWidth + 20;
    for (int i = 0; i < layouts.length; i++) {
      String outputName = model.getOutputs().bits.get(i);
      Layout layout = layouts[i];
      Location output;
      int height;
      if (layout == null) {
        outputData.addInput(outputName, null);
        height = -10;
      } else {
        int dy = 0;
        if (layout.outputY < 20) dy = 20 - layout.outputY;
        height = Math.max(dy + layout.height, 40);
        output = Location.create(outputX, y + dy + layout.outputY);
        outputData.addInput(outputName, new SingleInput(outputX, y + dy + layout.outputY));
        placeComponents(result, layouts[i], x, y + dy, inputData, output);
      }
      y += height + 10;
    }
    placeInputs(model, result, inputData, useNands);
    placeOutputs(model, result, outputData);
    return result;
  }

  //
  // computeInputData
  //
  private static InputData computeInputData(AnalyzerModel model) {
    InputData ret = new InputData();
    VariableList inputs = model.getInputs();
    int NameLength = 1;
    int BusLength = 1;
    int NrOfBusses = 0;
    for (int i = 0; i < inputs.vars.size(); i++) {
      if (inputs.vars.get(i).name.length() > NameLength)
        NameLength = inputs.vars.get(i).name.length();
      if (inputs.vars.get(i).width > BusLength) {
        BusLength = inputs.vars.get(i).width;
      }
      if (inputs.vars.get(i).width > 1) NrOfBusses++;
    }
    int spineX = 100 + NameLength * 10 + (BusLength - 1) * 10;
    ret.pinX = spineX - 10;
    if (NrOfBusses > 0) spineX += NrOfBusses * SPINE_DISTANCE + BUS_SPINE_TO_WIRE_SPINE_DISTANCE;
    int cnt = 0;
    for (int i = 0; i < inputs.vars.size(); i++) {
      Var inp = inputs.vars.get(i);
      if (inp.width == 1) {
        String name = inputs.bits.get(cnt++);
        ret.addInput(name, new SingleInput(spineX));
        spineX += SPINE_DISTANCE;
      } else {
        for (int idx = inp.width - 1; idx >= 0; idx--) {
          String name = inputs.bits.get(cnt++);
          ret.addInput(name, new SingleInput(spineX));
          spineX += SPINE_DISTANCE;
        }
      }
    }
    /* do the same for the inverted inputs */
    spineX = ret.CreateInvertedLocs(spineX);
    spineX += SPINE_DISTANCE;
    VariableList outputs = model.getOutputs();
    int NrOutBusses = 0;
    for (int i = 0; i < outputs.vars.size(); i++) if (outputs.vars.get(i).width > 1) NrOutBusses++;
    NrOfBusses = Math.max(NrOfBusses, NrOutBusses);
    ret.startX = spineX;
    ret.startY = TOP_BORDER + NrOfBusses * SPLITTER_HEIGHT + (NrOfBusses > 0 ? 10 : 0);
    return ret;
  }

  //
  // layoutGates
  //
  private static Layout layoutGates(CircuitDetermination det) {
    return layoutGatesSub(det);
  }

  private static Layout layoutGatesSub(CircuitDetermination det) {
    if (det instanceof CircuitDetermination.Input) {
      CircuitDetermination.Input input = (CircuitDetermination.Input) det;
      return new Layout(input.getName(), input.IsInvertedVersion());
    } else if (det instanceof CircuitDetermination.Value) {
      CircuitDetermination.Value value = (CircuitDetermination.Value) det;
      if ((value.getValue() == 1) || (value.getValue() == 0)) {
        return new Layout(Integer.toString(value.getValue()), false);
      }
      ComponentFactory factory = Constant.FACTORY;
      AttributeSet attrs = factory.createAttributeSet();
      attrs.setValue(Constant.ATTR_VALUE, Long.valueOf(value.getValue()));
      Bounds bds = factory.getOffsetBounds(attrs);
      return new Layout(
          bds.getWidth(), bds.getHeight(), -bds.getY(), factory, attrs, new Layout[0], 0);
    }

    // We know det is a Gate. Determine sublayouts.
    CircuitDetermination.Gate gate = (CircuitDetermination.Gate) det;
    ComponentFactory factory = gate.getFactory();
    ArrayList<CircuitDetermination> inputs = gate.getInputs();

    // Handle a NOT implemented with a NAND as a special case
    if (gate.isNandNot()) {
      CircuitDetermination subDet = inputs.get(0);
      if (!(subDet instanceof CircuitDetermination.Input)
          && !(subDet instanceof CircuitDetermination.Value)) {
        Layout[] sub = new Layout[1];
        sub[0] = layoutGatesSub(subDet);
        sub[0].y = 0;

        AttributeSet attrs = factory.createAttributeSet();
        attrs.setValue(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_NARROW);
        attrs.setValue(GateAttributes.ATTR_INPUTS, Integer.valueOf(2));

        // determine layout's width
        Bounds bds = factory.getOffsetBounds(attrs);
        int betweenWidth = 40;
        if (sub[0].width == 0) betweenWidth = 0;
        int width = sub[0].width + betweenWidth + bds.getWidth();

        // determine outputY and layout's height.
        int outputY = sub[0].y + sub[0].outputY;
        int height = sub[0].height;
        int minOutputY = roundUp(-bds.getY());
        if (minOutputY > outputY) {
          // we have to shift everything down because otherwise
          // the component will peek over the rectangle's top.
          int dy = minOutputY - outputY;
          sub[0].y += dy;
          height += dy;
          outputY += dy;
        }
        int minHeight = outputY + bds.getY() + bds.getHeight();
        if (minHeight > height) height = minHeight;

        // ok; create and return the layout.
        return new Layout(width, height, outputY, factory, attrs, sub, sub[0].width);
      }
    }

    Layout[] sub = new Layout[inputs.size()];
    int subWidth = 0; // maximum width of sublayouts
    int subHeight = 0; // total height of sublayouts
    for (int i = 0; i < sub.length; i++) {
      sub[i] = layoutGatesSub(inputs.get(i));
      if (sub.length % 2 == 0
          && i == (sub.length + 1) / 2
          && sub[i - 1].height + sub[i].height == 0) {
        // if there are an even number of inputs, then there is a
        // 20-tall gap between the middle two inputs. Ensure the two
        // middle inputs are at least 20 pixels apart.
        subHeight += 10;
      }
      sub[i].y = subHeight;
      subWidth = Math.max(subWidth, sub[i].width);
      subHeight += sub[i].height + 10;
    }
    subHeight -= 10;

    AttributeSet attrs = factory.createAttributeSet();
    if (factory == NotGate.FACTORY) {
      attrs.setValue(NotGate.ATTR_SIZE, NotGate.SIZE_NARROW);
    } else {
      attrs.setValue(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_NARROW);

      int ins = sub.length;
      attrs.setValue(GateAttributes.ATTR_INPUTS, Integer.valueOf(ins));
    }

    // determine layout's width
    Bounds bds = factory.getOffsetBounds(attrs);
    int betweenWidth = 40 + 10 * (sub.length / 2 - 1);
    if (sub.length == 1) betweenWidth = 20;
    if (subWidth == 0) betweenWidth = 0;
    int width = subWidth + betweenWidth + bds.getWidth();

    // determine outputY and layout's height.
    int outputY;
    if (sub.length % 2 == 1) { // odd number - match the middle input
      int i = (sub.length - 1) / 2;
      outputY = sub[i].y + sub[i].outputY;
    } else { // even number - halfway between middle two inputs
      int i0 = (sub.length / 2) - 1;
      int i1 = (sub.length / 2);
      int o0 = sub[i0].y + sub[i0].outputY;
      int o1 = sub[i1].y + sub[i1].outputY;
      outputY = roundDown((o0 + o1) / 2);
    }
    int height = subHeight;
    int minOutputY = roundUp(-bds.getY());
    if (minOutputY > outputY) {
      // we have to shift everything down because otherwise
      // the component will peek over the rectangle's top.
      int dy = minOutputY - outputY;
      for (int i = 0; i < sub.length; i++) sub[i].y += dy;
      height += dy;
      outputY += dy;
    }
    int minHeight = outputY + bds.getY() + bds.getHeight();
    if (minHeight > height) height = minHeight;

    // ok; create and return the layout.
    return new Layout(width, height, outputY, factory, attrs, sub, subWidth);
  }

  //
  // placeComponents
  //
  /**
   * @param circuit the circuit where to place the components.
   * @param layout the layout specifying the gates to place there.
   * @param x the left edge of where the layout should be placed.
   * @param y the top edge of where the layout should be placed.
   * @param inputData information about how to reach inputs.
   * @param output a point to which the output should be connected.
   */
  private static void placeComponents(
      CircuitMutation result, Layout layout, int x, int y, InputData inputData, Location output) {
    if (layout.inputName != null) {
      int inputX = inputData.getSpineX(layout.inputName, layout.inverted);
      Location input = Location.create(inputX, output.getY());
      inputData.registerConnection(layout.inputName, input, layout.inverted);
      result.add(Wire.create(input, output));
      return;
    }

    Location compOutput = Location.create(x + layout.width, output.getY());
    Component parent = layout.factory.createComponent(compOutput, layout.attrs);
    result.add(parent);
    if (!compOutput.equals(output)) {
      result.add(Wire.create(compOutput, output));
    }

    // handle a NOT gate pattern implemented with NAND as a special case
    if (layout.factory == NandGate.FACTORY
        && layout.subLayouts.length == 1
        && layout.subLayouts[0].inputName == null) {
      Layout sub = layout.subLayouts[0];

      Location input0 = parent.getEnd(1).getLocation();
      Location input1 = parent.getEnd(2).getLocation();

      int midX = input0.getX() - 20;
      Location subOutput = Location.create(midX, output.getY());
      Location midInput0 = Location.create(midX, input0.getY());
      Location midInput1 = Location.create(midX, input1.getY());
      result.add(Wire.create(subOutput, midInput0));
      result.add(Wire.create(midInput0, input0));
      result.add(Wire.create(subOutput, midInput1));
      result.add(Wire.create(midInput1, input1));

      int subX = x + layout.subX - sub.width;
      placeComponents(result, sub, subX, y + sub.y, inputData, subOutput);
      return;
    }

    if (layout.subLayouts.length == parent.getEnds().size() - 2) {
      int index = layout.subLayouts.length / 2 + 1;
      Object factory = parent.getFactory();
      if (factory instanceof AbstractGate) {
        Value val = ((AbstractGate) factory).getIdentity();
        Long valLong = Long.valueOf(val.toLongValue());
        Location loc = parent.getEnd(index).getLocation();
        AttributeSet attrs = Constant.FACTORY.createAttributeSet();
        attrs.setValue(Constant.ATTR_VALUE, valLong);
        result.add(Constant.FACTORY.createComponent(loc, attrs));
      }
    }

    for (int i = 0; i < layout.subLayouts.length; i++) {
      Layout sub = layout.subLayouts[i];

      int inputIndex = i + 1;
      Location subDest = parent.getEnd(inputIndex).getLocation();

      int subOutputY = y + sub.y + sub.outputY;
      if (sub.inputName != null) {
        int destY = subDest.getY();
        if (i == 0 && destY < subOutputY
            || i == layout.subLayouts.length - 1 && destY > subOutputY) {
          subOutputY = destY;
        }
      }

      Location subOutput;
      int numSubs = layout.subLayouts.length;
      if (subOutputY == subDest.getY()) {
        subOutput = subDest;
      } else {
        int back;
        if (i < numSubs / 2) {
          if (subOutputY < subDest.getY()) { // bending upward
            back = i;
          } else {
            back = ((numSubs - 1) / 2) - i;
          }
        } else {
          if (subOutputY > subDest.getY()) { // bending downward
            back = numSubs - 1 - i;
          } else {
            back = i - (numSubs / 2);
          }
        }
        int subOutputX = subDest.getX() - 20 - 10 * back;
        subOutput = Location.create(subOutputX, subOutputY);
        Location mid = Location.create(subOutputX, subDest.getY());
        result.add(Wire.create(subOutput, mid));
        result.add(Wire.create(mid, subDest));
      }

      int subX = x + layout.subX - sub.width;
      int subY = y + sub.y;
      placeComponents(result, sub, subX, subY, inputData, subOutput);
    }
  }

  private static void placeInputInverters(
      CircuitMutation result, InputData inputData, boolean UseNands) {
    int InvYpos = inputData.getStartY() + GATE_HEIGHT / 2;
    for (int i = 0; i < inputData.NrOfInputs(); i++) {
      String iName = inputData.getInputName(i);
      if (inputData.hasInvertedConnections(iName)) {
        if (UseNands) {
          ComponentFactory fact = NandGate.FACTORY;
          AttributeSet attrs = fact.createAttributeSet();
          attrs.setValue(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_NARROW);
          Location IPloc1 = Location.create(inputData.getSpineX("1", false), InvYpos - 10);
          inputData.registerConnection("1", IPloc1, false);
          Location Ploc = Location.create(inputData.GetInverterXLoc(), InvYpos);
          result.add(fact.createComponent(Ploc, attrs));
          Location IPloc2 = Location.create(inputData.GetInverterXLoc() - NAND_WIDTH, InvYpos - 10);
          result.add(Wire.create(IPloc1, IPloc2));
          IPloc1 = Location.create(inputData.getSpineX(iName, false), InvYpos + 10);
          IPloc2 = Location.create(inputData.GetInverterXLoc() - NAND_WIDTH, InvYpos + 10);
          result.add(Wire.create(IPloc1, IPloc2));
          inputData.registerConnection(iName, IPloc1, false);
          Location IPloc3 = Location.create(inputData.getSpineX(iName, true), InvYpos);
          result.add(Wire.create(Ploc, IPloc3));
          inputData.registerConnection(iName, IPloc3, true);
        } else {
          ComponentFactory fact = NotGate.FACTORY;
          AttributeSet attrs = fact.createAttributeSet();
          Location Ploc = Location.create(inputData.GetInverterXLoc(), InvYpos);
          result.add(fact.createComponent(Ploc, attrs));
          Location IPloc1 = Location.create(inputData.getSpineX(iName, false), InvYpos);
          Location IPloc2 = Location.create(inputData.GetInverterXLoc() - INVERTER_WIDTH, InvYpos);
          result.add(Wire.create(IPloc1, IPloc2));
          inputData.registerConnection(iName, IPloc1, false);
          Location IPloc3 = Location.create(inputData.getSpineX(iName, true), InvYpos);
          result.add(Wire.create(Ploc, IPloc3));
          inputData.registerConnection(iName, IPloc3, true);
        }
        /* Here we draw the inverted spine */
        createSpine(result, inputData.getInputLocs(iName, true).ys, new CompareYs());
        InvYpos += GATE_HEIGHT;
      }
    }
  }

  private static void placeConstants(CircuitMutation result, InputData inputData) {
    ComponentFactory fact = Constant.FACTORY;
    if (!inputData.getInputLocs("0", false).ys.isEmpty()) {
      AttributeSet attrs = fact.createAttributeSet();
      attrs.setValue(StdAttr.FACING, Direction.SOUTH);
      attrs.setValue(Constant.ATTR_VALUE, 0L);
      Location loc = Location.create(inputData.getSpineX("0", false), inputData.startY - 10);
      result.add(fact.createComponent(loc, attrs));
      inputData.registerConnection("0", loc, false);
      createSpine(result, inputData.getInputLocs("0", false).ys, new CompareYs());
    }
    if (!inputData.getInputLocs("1", false).ys.isEmpty()) {
      AttributeSet attrs = fact.createAttributeSet();
      attrs.setValue(StdAttr.FACING, Direction.SOUTH);
      attrs.setValue(Constant.ATTR_VALUE, 1L);
      Location loc = Location.create(inputData.getSpineX("1", false), inputData.startY - 10);
      result.add(fact.createComponent(loc, attrs));
      inputData.registerConnection("1", loc, false);
      createSpine(result, inputData.getInputLocs("1", false).ys, new CompareYs());
    }
  }

  //
  // placeInputs
  //
  private static void placeInputs(
      AnalyzerModel model, CircuitMutation result, InputData inputData, boolean UseNands) {
    ArrayList<Location> forbiddenYs = new ArrayList<Location>();
    Comparator<Location> compareYs = new CompareYs();
    int curX = inputData.getPinX();
    int curY = inputData.getStartY() + 20;
    VariableList inputs = model.getInputs();

    /* we start with placing the inverters */
    placeInputInverters(result, inputData, UseNands);
    /* now we do the constants */
    placeConstants(result, inputData);

    int idx = 0;
    int busNr = 0;
    int busY = inputData.startY - 10;
    for (int nr = 0; nr < inputs.vars.size(); nr++) {
      Var inp = inputs.vars.get(nr);
      if (inp.width == 1) {
        String name = inputData.getInputName(idx++);
        SingleInput singleInput = inputData.getInputLocs(name, false);

        // determine point where we can intersect with spine
        int spineX = singleInput.spineX;
        Location spineLoc = Location.create(spineX, curY);
        if (singleInput.ys.size() > 0) {
          // search for a Y that won't intersect with others
          // (we needn't bother if the pin doesn't connect
          // with anything anyway.)
          Collections.sort(forbiddenYs, compareYs);
          while (Collections.binarySearch(forbiddenYs, spineLoc, compareYs) >= 0) {
            curY += 10;
            spineLoc = Location.create(spineX, curY);
          }
          singleInput.ys.add(spineLoc);
        }
        Location loc = Location.create(curX, curY);

        // now create the pin
        placeInput(result, loc, name, 1);

        ArrayList<Location> spine = singleInput.ys;
        if (spine.size() > 0) {
          // create wire connecting pin to spine
          result.add(Wire.create(loc, spineLoc));

          // create spine
          createSpine(result, spine, compareYs);
        }

        // advance y and forbid spine intersections for next pin
        forbiddenYs.addAll(singleInput.ys);
        curY += MINIMAL_PIN_DISTANCE;
      } else {
        /* first place the input and the splitter */
        String name = inp.name;
        Location ploc = Location.create(curX, curY);
        /* create the pin */
        placeInput(result, ploc, name, inp.width);
        /* determine the position of the splitter */
        String MSBname = inputData.getInputName(idx);
        SingleInput singleInput = inputData.getInputLocs(MSBname, false);
        int spineX = singleInput.spineX;
        Location sloc = Location.create(spineX - 10, busY - SPLITTER_HEIGHT);
        placeSplitter(result, sloc, inp.width, true);
        /* place the bus connection */
        Location BI1 = Location.create(ploc.getX() + 10 + busNr * SPINE_DISTANCE, ploc.getY());
        Location BI2 = Location.create(BI1.getX(), sloc.getY());
        result.add(Wire.create(ploc, BI1));
        result.add(Wire.create(BI1, BI2));
        result.add(Wire.create(BI2, sloc));
        busNr++;
        /* Now connect to the spines */
        for (int bit = inp.width - 1; bit >= 0; bit--) {
          MSBname = inputData.getInputName(idx++);
          singleInput = inputData.getInputLocs(MSBname, false);
          spineX = singleInput.spineX;
          ArrayList<Location> spine = singleInput.ys;
          if (spine.size() > 0) {
            /* add a location for the bus entry */
            Location bloc = Location.create(spineX, busY);
            spine.add(bloc);
            Collections.sort(forbiddenYs, compareYs);
            // create spine
            createSpine(result, spine, compareYs);
          }
          forbiddenYs.addAll(singleInput.ys);
        }
        busY -= SPLITTER_HEIGHT;
        curY += MINIMAL_PIN_DISTANCE;
      }
    }
  }

  private static void createSpine(
      CircuitMutation result, ArrayList<Location> spine, Comparator<Location> compareYs) {
    Collections.sort(spine, compareYs);
    Location prev = spine.get(0);
    for (int k = 1, n = spine.size(); k < n; k++) {
      Location cur = spine.get(k);
      if (!cur.equals(prev)) {
        result.add(Wire.create(prev, cur));
        prev = cur;
      }
    }
  }

  private static void placeInput(CircuitMutation result, Location loc, String name, int NrOfBits) {
    ComponentFactory factory = Pin.FACTORY;
    AttributeSet attrs = factory.createAttributeSet();
    attrs.setValue(StdAttr.FACING, Direction.EAST);
    attrs.setValue(Pin.ATTR_TYPE, Boolean.FALSE);
    attrs.setValue(Pin.ATTR_TRISTATE, Boolean.FALSE);
    attrs.setValue(StdAttr.LABEL, name);
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(NrOfBits));
    result.add(factory.createComponent(loc, attrs));
  }

  private static void placeSplitter(
      CircuitMutation result, Location loc, int NrOfBits, boolean input) {
    ComponentFactory factory = SplitterFactory.instance;
    AttributeSet attrs = factory.createAttributeSet();
    attrs.setValue(StdAttr.FACING, Direction.SOUTH);
    attrs.setValue(SplitterAttributes.ATTR_FANOUT, NrOfBits);
    attrs.setValue(SplitterAttributes.ATTR_WIDTH, BitWidth.create(NrOfBits));
    attrs.setValue(
        SplitterAttributes.ATTR_APPEARANCE,
        input ? SplitterAttributes.APPEAR_LEFT : SplitterAttributes.APPEAR_RIGHT);
    attrs.setValue(SplitterAttributes.ATTR_SPACING, SPINE_DISTANCE / 10);
    result.add(factory.createComponent(loc, attrs));
  }

  private static void placeOutputs(
      AnalyzerModel model, CircuitMutation result, InputData outputData) {
    int startX = 0;
    int nrOfBusses = 0;
    VariableList outputs = model.getOutputs();
    for (int idx = 0; idx < outputData.NrOfInputs(); idx++) {
      String name = outputData.getInputName(idx);
      int posX =
          (outputData.getInputLocs(name, false) == null)
              ? 0
              : outputData.getInputLocs(name, false).spineX;
      if (posX > startX) startX = posX;
    }
    for (int idx = 0; idx < outputs.vars.size(); idx++) {
      if (outputs.vars.get(idx).width > 1) nrOfBusses++;
    }
    int pinX = startX + outputData.NrOfInputs() * SPINE_DISTANCE + 10;
    if (nrOfBusses > 0)
      pinX += (nrOfBusses - 1) * SPINE_DISTANCE + BUS_SPINE_TO_WIRE_SPINE_DISTANCE;
    int pinY = outputData.getStartY() + 20;
    int busX = pinX - 10;
    int busID = 0;
    /* first we place the outputs with at least one connection, in the second pass we place the empty
     * outputs.
     */
    int cnt = 0;
    for (int idx = 0; idx < outputs.vars.size(); idx++) {
      Var outp = outputs.vars.get(idx);
      String name = outputData.getInputName(cnt);
      if (outp.width == 1) {
        Location Ppoint = Location.create(pinX, pinY);
        placeOutput(result, Ppoint, outp.name, 1);
        SingleInput singleOutput = outputData.getInputLocs(name, false);
        if (singleOutput != null) {
          Location Cpoint = Location.create(singleOutput.spineX, singleOutput.spineY);
          int Xoff = startX + cnt * SPINE_DISTANCE;
          Location Ipoint1 = Location.create(Xoff, Cpoint.getY());
          Location Ipoint2 = Location.create(Xoff, Ppoint.getY());
          if (Cpoint.getX() != Ipoint1.getX()) result.add(Wire.create(Cpoint, Ipoint1));
          if (Ipoint1.getY() != Ipoint2.getY()) result.add(Wire.create(Ipoint1, Ipoint2));
          if (Ipoint2.getX() != Ppoint.getX()) result.add(Wire.create(Ipoint2, Ppoint));
        }
        cnt++;
      } else {
        Location Ppoint = Location.create(pinX, pinY);
        placeOutput(result, Ppoint, outp.name, outp.width);
        /* process the splitter */
        int SStartX = startX + cnt * SPINE_DISTANCE;
        Location Spoint =
            Location.create(
                SStartX + (outp.width - 1) * SPINE_DISTANCE + 10,
                TOP_BORDER + busID * SPLITTER_HEIGHT);
        placeSplitter(result, Spoint, outp.width, false);
        // process the bus connection
        Location Ipoint1 = Location.create(busX - busID * SPINE_DISTANCE, Spoint.getY());
        Location Ipoint2 = Location.create(Ipoint1.getX(), Ppoint.getY());
        busID++;
        if (Spoint.getX() != Ipoint1.getX()) result.add(Wire.create(Spoint, Ipoint1));
        if (Ipoint1.getY() != Ipoint2.getY()) result.add(Wire.create(Ipoint1, Ipoint2));
        if (Ipoint2.getX() != Ppoint.getX()) result.add(Wire.create(Ipoint2, Ppoint));
        // process the connections
        for (int bit = 0; bit < outp.width; bit++) {
          Location SEpoint = Location.create(SStartX + bit * SPINE_DISTANCE, Spoint.getY() + 20);
          String tname = outputData.getInputName(cnt + bit);
          SingleInput singleOutput = outputData.getInputLocs(tname, false);
          if (singleOutput != null) {
            Location Cpoint = Location.create(singleOutput.spineX, singleOutput.spineY);
            if (SEpoint.getX() == Cpoint.getX()) {
              result.add(Wire.create(SEpoint, Cpoint));
            } else {
              Location Ipoint = Location.create(SEpoint.getX(), Cpoint.getY());
              result.add(Wire.create(Cpoint, Ipoint));
              result.add(Wire.create(SEpoint, Ipoint));
            }
          }
        }
        // all done
        cnt += outp.width;
      }
      pinY += MINIMAL_PIN_DISTANCE;
    }
  }
  //
  // placeOutput
  //
  private static void placeOutput(CircuitMutation result, Location loc, String name, int NrOfBits) {
    ComponentFactory factory = Pin.FACTORY;
    AttributeSet attrs = factory.createAttributeSet();
    attrs.setValue(StdAttr.FACING, Direction.WEST);
    attrs.setValue(Pin.ATTR_TYPE, Boolean.TRUE);
    attrs.setValue(StdAttr.LABEL, name);
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(NrOfBits));
    result.add(factory.createComponent(loc, attrs));
  }

  private static int roundDown(int value) {
    return value / 10 * 10;
  }

  private static int roundUp(int value) {
    return (value + 9) / 10 * 10;
  }

  private CircuitBuilder() {}
}
