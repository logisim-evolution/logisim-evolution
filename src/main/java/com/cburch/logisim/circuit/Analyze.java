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

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class Analyze {
  public static class LocationBit {
    Location loc;
    int bit;

    public LocationBit(Location l, int b) {
      loc = l;
      bit = b;
    }

    @Override
    public boolean equals(Object other) {
      if (other == null || !(other instanceof LocationBit)) return false;
      LocationBit that = (LocationBit) other;
      return (that.loc.equals(this.loc) && that.bit == this.bit);
    }

    @Override
    public int hashCode() {
      return loc.hashCode() + bit;
    }
  }

  private static class ExpressionMap extends HashMap<LocationBit, Expression>
      implements ExpressionComputer.Map {
    private static final long serialVersionUID = 1L;
    private Circuit circuit;
    private Set<LocationBit> dirtyPoints = new HashSet<LocationBit>();
    private Map<LocationBit, Component> causes = new HashMap<LocationBit, Component>();
    private Component currentCause = null;

    ExpressionMap(Circuit circuit) {
      this.circuit = circuit;
    }

    @Override
    public Expression put(LocationBit point, Expression expression) {
      Expression ret = super.put(point, expression);
      if (currentCause != null) causes.put(point, currentCause);
      if (ret == null ? expression != null : !ret.equals(expression)) {
        dirtyPoints.add(point);
      }
      return ret;
    }

    public Expression put(Location point, int bit, Expression expression) {
      return put(new LocationBit(point, bit), expression);
    }

    public Expression get(Location point, int bit) {
      return get(new LocationBit(point, bit));
    }
  }

  /**
   * Checks whether any of the recently placed expressions in the expression map are
   * self-referential; if so, return it.
   */
  private static Expression checkForCircularExpressions(ExpressionMap expressionMap)
      throws AnalyzeException {
    for (LocationBit point : expressionMap.dirtyPoints) {
      Expression expr = expressionMap.get(point);
      if (expr.isCircular()) return expr;
    }
    return null;
  }

  //
  // computeExpression
  //
  /**
   * Computes the expression corresponding to the given circuit, or raises ComputeException if
   * difficulties arise.
   */
  public static void computeExpression(
      AnalyzerModel model, Circuit circuit, Map<Instance, String> pinNames)
      throws AnalyzeException {
    ExpressionMap expressionMap = new ExpressionMap(circuit);

    ArrayList<Var> inputVars = new ArrayList<Var>();
    ArrayList<Var> outputVars = new ArrayList<Var>();
    ArrayList<Instance> outputPins = new ArrayList<Instance>();
    for (Map.Entry<Instance, String> entry : pinNames.entrySet()) {
      Instance pin = entry.getKey();
      String label = entry.getValue();
      int width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
      if (Pin.FACTORY.isInputPin(pin)) {
        expressionMap.currentCause = Instance.getComponentFor(pin);
        for (int b = 0; b < width; b++) {
          Expression e = Expressions.variable(width > 1 ? label + "[" + b + "]" : label);
          expressionMap.put(new LocationBit(pin.getLocation(), b), e);
        }
        inputVars.add(new Var(label, width));
      } else {
        outputPins.add(pin);
        outputVars.add(new Var(label, width));
      }
    }

    propagateComponents(expressionMap, circuit.getNonWires());

    for (int iterations = 0; !expressionMap.dirtyPoints.isEmpty(); iterations++) {
      if (iterations > MAX_ITERATIONS) {
        throw new AnalyzeException.Circular();
      }

      propagateWires(expressionMap, new HashSet<LocationBit>(expressionMap.dirtyPoints));

      HashSet<Component> dirtyComponents = getDirtyComponents(circuit, expressionMap.dirtyPoints);
      expressionMap.dirtyPoints.clear();
      propagateComponents(expressionMap, dirtyComponents);

      Expression expr = checkForCircularExpressions(expressionMap);
      if (expr != null) throw new AnalyzeException.Circular();
    }

    model.setVariables(inputVars, outputVars);
    for (int i = 0; i < outputPins.size(); i++) {
      Instance pin = outputPins.get(i);
      String label = pinNames.get(pin);
      int width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
      for (int b = 0; b < width; b++) {
        LocationBit loc = new LocationBit(pin.getLocation(), b);
        String name = (width > 1 ? label + "[" + b + "]" : label);
        model.getOutputExpressions().setExpression(name, expressionMap.get(loc));
      }
    }
  }

  //
  // ComputeTable
  //
  /** Returns a truth table corresponding to the circuit. */
  public static void computeTable(
      AnalyzerModel model, Project proj, Circuit circuit, Map<Instance, String> pinLabels) {
    ArrayList<Instance> inputPins = new ArrayList<Instance>();
    ArrayList<Var> inputVars = new ArrayList<Var>();
    ArrayList<String> inputNames = new ArrayList<String>();
    ArrayList<Instance> outputPins = new ArrayList<Instance>();
    ArrayList<Var> outputVars = new ArrayList<Var>();
    ArrayList<String> outputNames = new ArrayList<String>();
    for (Map.Entry<Instance, String> entry : pinLabels.entrySet()) {
      Instance pin = entry.getKey();
      int width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
      Var var = new Var(entry.getValue(), width);
      if (Pin.FACTORY.isInputPin(pin)) {
        inputPins.add(pin);
        for (String name : var) inputNames.add(name);
        inputVars.add(var);
      } else {
        outputPins.add(pin);
        for (String name : var) outputNames.add(name);
        outputVars.add(var);
      }
    }

    int inputCount = inputNames.size();
    int rowCount = 1 << inputCount;
    Entry[][] columns = new Entry[outputNames.size()][rowCount];

    for (int i = 0; i < rowCount; i++) {
      CircuitState circuitState = new CircuitState(proj, circuit);
      int incol = 0;
      for (int j = 0; j < inputPins.size(); j++) {
        Instance pin = inputPins.get(j);
        int width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
        Value v[] = new Value[width];
        for (int b = width - 1; b >= 0; b--) {
          boolean value = TruthTable.isInputSet(i, incol++, inputCount);
          v[b] = value ? Value.TRUE : Value.FALSE;
        }
        InstanceState pinState = circuitState.getInstanceState(pin);
        Pin.FACTORY.setValue(pinState, Value.create(v));
      }

      Propagator prop = circuitState.getPropagator();
      prop.propagate();
      /*
       * TODO for the SimulatorPrototype class do { prop.step(); } while
       * (prop.isPending());
       */
      // TODO: Search for circuit state

      if (prop.isOscillating()) {
        for (int j = 0; j < columns.length; j++) {
          columns[j][i] = Entry.OSCILLATE_ERROR;
        }
      } else {
        int outcol = 0;
        for (int j = 0; j < outputPins.size(); j++) {
          Instance pin = outputPins.get(j);
          int width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
          InstanceState pinState = circuitState.getInstanceState(pin);
          Entry out;
          for (int b = width - 1; b >= 0; b--) {
            Value outValue = Pin.FACTORY.getValue(pinState).get(b);
            if (outValue == Value.TRUE) out = Entry.ONE;
            else if (outValue == Value.FALSE) out = Entry.ZERO;
            else if (outValue == Value.ERROR) out = Entry.BUS_ERROR;
            else out = Entry.DONT_CARE;
            columns[outcol++][i] = out;
          }
        }
      }
    }

    model.setVariables(inputVars, outputVars);
    for (int i = 0; i < columns.length; i++) {
      model.getTruthTable().setOutputColumn(i, columns[i]);
    }
  }

  // computes outputs of affected components
  private static HashSet<Component> getDirtyComponents(
      Circuit circuit, Set<LocationBit> pointsToProcess) throws AnalyzeException {
    HashSet<Component> dirtyComponents = new HashSet<Component>();
    for (LocationBit point : pointsToProcess) {
      for (Component comp : circuit.getNonWires(point.loc)) {
        dirtyComponents.add(comp);
      }
    }
    return dirtyComponents;
  }

  //
  // getPinLabels
  //
  /**
   * Returns a sorted map from Pin objects to String objects, listed in canonical order (top-down
   * order, with ties broken left-right).
   */
  public static SortedMap<Instance, String> getPinLabels(Circuit circuit) {
    Comparator<Instance> locOrder =
        new Comparator<Instance>() {
          public int compare(Instance ac, Instance bc) {
            Location a = ac.getLocation();
            Location b = bc.getLocation();
            if (a.getY() < b.getY()) return -1;
            if (a.getY() > b.getY()) return 1;
            if (a.getX() < b.getX()) return -1;
            if (a.getX() > b.getX()) return 1;
            return a.hashCode() - b.hashCode();
          }
        };
    SortedMap<Instance, String> ret = new TreeMap<Instance, String>(locOrder);

    // Put the pins into the TreeMap, with null labels
    for (Instance pin : circuit.getAppearance().getPortOffsets(Direction.EAST).values()) {
      ret.put(pin, null);
    }

    // Process first the pins that the user has given labels.
    ArrayList<Instance> pinList = new ArrayList<Instance>(ret.keySet());
    HashSet<String> labelsTaken = new HashSet<String>();
    for (Instance pin : pinList) {
      String label = pin.getAttributeSet().getValue(StdAttr.LABEL);
      label = toValidLabel(label);
      if (label != null) {
        if (labelsTaken.contains(label)) {
          int i = 2;
          while (labelsTaken.contains(label + i)) i++;
          label = label + i;
        }
        ret.put(pin, label);
        labelsTaken.add(label);
      }
    }

    // Now process the unlabeled pins.
    for (Instance pin : pinList) {
      if (ret.get(pin) != null) continue;

      String defaultList;
      if (Pin.FACTORY.isInputPin(pin)) {
        defaultList = S.get("defaultInputLabels");
        if (defaultList.indexOf(",") < 0) {
          defaultList = "a,b,c,d,e,f,g,h";
        }
      } else {
        defaultList = S.get("defaultOutputLabels");
        if (defaultList.indexOf(",") < 0) {
          defaultList = "x,y,z,u,v,w,s,t";
        }
      }

      String[] options = defaultList.split(",");
      String label = null;
      for (int i = 0; label == null && i < options.length; i++) {
        if (!labelsTaken.contains(options[i])) {
          label = options[i];
        }
      }
      if (label == null) {
        // This is an extreme measure that should never happen
        // if the default labels are defined properly and the
        // circuit doesn't exceed the maximum number of pins.
        int i = 1;
        do {
          i++;
          label = "x" + i;
        } while (labelsTaken.contains(label));
      }

      labelsTaken.add(label);
      ret.put(pin, label);
    }

    return ret;
  }

  private static void propagateComponents(
      ExpressionMap expressionMap, Collection<Component> components) throws AnalyzeException {
    for (Component comp : components) {
      ExpressionComputer computer = (ExpressionComputer) comp.getFeature(ExpressionComputer.class);
      if (computer != null) {
        try {
          expressionMap.currentCause = comp;
          computer.computeExpression(expressionMap);
        } catch (UnsupportedOperationException e) {
          throw new AnalyzeException.CannotHandle(comp.getFactory().getDisplayName());
        }
      } else if (comp.getFactory() instanceof Pin) {; // pins are handled elsewhere
      } else if (comp.getFactory() instanceof SplitterFactory) {; // splitters are handled elsewhere
      } else {
        throw new AnalyzeException.CannotHandle(comp.getFactory().getDisplayName());
      }
    }
  }

  // propagates expressions down wires
  private static void propagateWires(
      ExpressionMap expressionMap, HashSet<LocationBit> pointsToProcess) throws AnalyzeException {
    expressionMap.currentCause = null;
    for (LocationBit p : pointsToProcess) {
      Expression e = expressionMap.get(p);
      expressionMap.currentCause = expressionMap.causes.get(p);
      WireBundle bundle = expressionMap.circuit.wires.getWireBundle(p.loc);
      if (e != null && bundle != null && bundle.isValid() && bundle.threads != null) {
        if (bundle.threads.length <= p.bit) {
          throw new AnalyzeException.CannotHandle("incompatible widths");
        }
        WireThread t = bundle.threads[p.bit];
        for (CircuitWires.ThreadBundle tb : t.getBundles()) {
          for (Location p2 : tb.b.points) {
            if (p2.equals(p)) continue;
            LocationBit p2b = new LocationBit(p2, tb.loc);
            Expression old = expressionMap.get(p2b);
            if (old != null) {
              Component eCause = expressionMap.currentCause;
              Component oldCause = expressionMap.causes.get(p2b);
              if (eCause != oldCause && !old.equals(e)) {
                throw new AnalyzeException.Conflict();
              }
            }
            expressionMap.put(p2b, e);
          }
        }
      }
    }
  }

  private static String toValidLabel(String label) {
    if (label == null) return null;
    StringBuilder end = null;
    StringBuilder ret = new StringBuilder();
    boolean afterWhitespace = false;
    for (int i = 0; i < label.length(); i++) {
      char c = label.charAt(i);
      if (Character.isJavaIdentifierStart(c)) {
        if (afterWhitespace) {
          // capitalize words after the first one
          c = Character.toTitleCase(c);
          afterWhitespace = false;
        }
        ret.append(c);
      } else if (Character.isJavaIdentifierPart(c)) {
        // If we can't place it at the start, we'll dump it
        // onto the end.
        if (ret.length() > 0) {
          ret.append(c);
        } else {
          if (end == null) end = new StringBuilder();
          end.append(c);
        }
        afterWhitespace = false;
      } else if (Character.isWhitespace(c)) {
        afterWhitespace = true;
      } else {; // just ignore any other characters
      }
    }
    if (end != null && ret.length() > 0) ret.append(end.toString());
    if (ret.length() == 0) return null;
    return ret.toString();
  }

  private static final int MAX_ITERATIONS = 100;

  private Analyze() {}
}
