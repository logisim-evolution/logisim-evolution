/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.gates.GateAttributes;
import com.cburch.logisim.std.gates.NegateAttribute;

public class HDLParameters {
  
  public static final int MAP_DEFAULT = 0;
  public static final int MAP_CONSTANT = 1;
  public static final int MAP_OFFSET = 2;
  public static final int MAP_MULTIPLY = 3;
  public static final int MAP_ATTRIBUTE_OPTION = 4;
  public static final int MAP_BITS_REQUIRED = 5;
  public static final int MAP_INT_ATTRIBUTE = 6;
  public static final int MAP_GATE_INPUT_BUBLE = 7;
  
  private class ParameterInfo {
    private final boolean isOnlyUsedForBusses;
    private final String parameterName;
    private final int parameterId;
    private int myMapType = MAP_DEFAULT;
    private int parameterValue = -1;
    private int multiplyValue = 1;
    private int offsetValue = 0;
    private List<Attribute<?>> attributesList = new ArrayList<>();
    private Map<AttributeOption, Integer> attributeOptionMap = new HashMap<>();
    private final Attribute<BitWidth> attributeToCheckForBus;

    public ParameterInfo(String name, int id) {
      this(false, StdAttr.WIDTH, name, id);
    }

    public ParameterInfo(String name, int id, int type, Object... args) {
      this(false, StdAttr.WIDTH, name, id);
      myMapType = type;
      switch (type) {
        case MAP_CONSTANT:
          parameterValue = getCorrectIntValue(args);
          break;
        case MAP_OFFSET:
          offsetValue = getCorrectIntValue(args);
          break;
        case MAP_MULTIPLY:
          multiplyValue = getCorrectIntValue(args);
          if (multiplyValue == 0) throw new NumberFormatException("multiply value cannot be zero");
          break;
        case MAP_ATTRIBUTE_OPTION:
          if (args.length != 2) throw new IllegalArgumentException("Attribute map Type requires 2 argument");
          if (!(args[0] instanceof Attribute<?>)) throw new IllegalArgumentException("Attribute map Type: first argument needs to be an Attribute<?>");
          if (!(args[1] instanceof Map<?, ?>)) throw new IllegalArgumentException("Attribute map Type: second argument needs to be an Map<?,?>");
          final var map = (Map<?, ?>) args[1];
          if (map.isEmpty()) throw new IllegalArgumentException("Attribute map Type: Map<?,?> cannot be empty");
          for (var key : map.keySet()) {
            if (!(key instanceof AttributeOption)) throw new IllegalArgumentException("Attribute map Type: Map<?,?> keys need to be an AttributeOption");
            if (!(map.get(key) instanceof Integer))throw new IllegalArgumentException("Attribute map Type: Map<?,?> value need to be an Integer");
          }
          attributesList.add((Attribute<?>) args[0]);
          attributeOptionMap = (Map<AttributeOption, Integer>) args[1];
          break;
        case MAP_INT_ATTRIBUTE:
          if (args.length != 1) throw new IllegalArgumentException("Attribute map Type requires only 1 argument");
          attributesList.add((Attribute<?>) args[0]);
          break;
        case MAP_BITS_REQUIRED:
          for (var arg : args) {
            if (!(arg instanceof Attribute<?>)) throw new IllegalArgumentException("Bits required map Type: argument needs to be an Attribute<?>");
            attributesList.add((Attribute<?>) arg);
          }
          break;
        default: return;
      }
    }

    public ParameterInfo(boolean forBusOnly, String name, int id) {
      this(forBusOnly, StdAttr.WIDTH, name, id);
    }

    public ParameterInfo(boolean forBusOnly, Attribute<BitWidth> checkAttr, String name, int id) {
      isOnlyUsedForBusses = forBusOnly;
      parameterName = name;
      parameterId = id;
      attributeToCheckForBus = checkAttr;
    }

    public boolean isUsed(AttributeSet attrs) {
      final var nrOfBits = (attrs != null) && attrs.containsAttribute(attributeToCheckForBus) ? attrs.getValue(attributeToCheckForBus).getWidth() : 0;
      return (!isOnlyUsedForBusses || (nrOfBits > 1));
    }

    public int getParameterId(AttributeSet attrs) {
      return isUsed(attrs) ? parameterId : 0;
    }

    public String getParameterString(AttributeSet attrs) {
      return isUsed(attrs) ? parameterName : null;
    }

    public long getParameterValue(AttributeSet attrs) {
      switch (myMapType) {
        case MAP_CONSTANT: 
          return parameterValue;
        case MAP_ATTRIBUTE_OPTION: 
          if (!attrs.containsAttribute(attributesList.get(0))) throw new UnsupportedOperationException("Component has not the required attribute");
          final var value = attrs.getValue(attributesList.get(0));
          if (!(value instanceof AttributeOption)) throw new UnsupportedOperationException("Requested attribute is not an attributeOption");
          if (!attributeOptionMap.containsKey(value)) throw new UnsupportedOperationException("Map does not contain the requested attributeOption");
          return attributeOptionMap.get(value);
        case MAP_BITS_REQUIRED:
          var totalValue = 0;
          for (var attr :attributesList) {
            if (!attrs.containsAttribute(attr)) throw new UnsupportedOperationException("Component has not the required attribute");
            final var intValue = attrs.getValue(attr);
            if (intValue instanceof Integer) {
              totalValue += (int) intValue;
            } else throw new UnsupportedOperationException("Requested attribute is not an Integer");
          }
          final var logValue = Math.log(totalValue) / Math.log(2d);
          return (int) Math.ceil(logValue);
        case MAP_INT_ATTRIBUTE:
          if (!attrs.containsAttribute(attributesList.get(0))) throw new UnsupportedOperationException("Component has not the required attribute");
          final var intValue = attrs.getValue(attributesList.get(0));
          if (intValue instanceof Integer)
            return (int) intValue;
          else throw new UnsupportedOperationException("Requested attribute is not an Integer");
        case MAP_GATE_INPUT_BUBLE:
          if (!attrs.containsAttribute(GateAttributes.ATTR_INPUTS)) throw new UnsupportedOperationException("Component has not the required attribute");
          final var nrOfInputs = attrs.getValue(GateAttributes.ATTR_INPUTS);
          var bubbleMask = 0;
          var mask = 1;
          for (var i = 0; i < nrOfInputs; i++) {
            final var inputIsInverted = attrs.getValue(new NegateAttribute(i, null));
            if (inputIsInverted) bubbleMask |= mask;
            mask <<= 1;
          }
          return bubbleMask;
        default: 
          return attrs.getValue(attributeToCheckForBus).getWidth() * multiplyValue + offsetValue; 
      }
    }
    
    private int getCorrectIntValue(Object... args) {
      if (args.length != 1) throw new IllegalArgumentException("Map Type requires a single argument");
      if (!(args[0] instanceof Integer)) throw new IllegalArgumentException("Map Type requires an Integer");
      final int value = (int) args[0];
      if (value < 0) throw new NumberFormatException("Integer value must be positive");
      return value;
    }
  }

  private final List<ParameterInfo> myParameters = new ArrayList<>();

  /**
   * Constructs a module parameter where the map-value of the parameter is the Value stored in
   * the attribute StdAttr.BitWidth
   * 
   * @param name Name used for the parameter
   * @param id Identifier of the parameter (must be negative)
   */
  public HDLParameters add(String name, int id) {
    myParameters.add(new ParameterInfo(name, id));
    return this;
  }

  /**
   * Constructs a module parameter where the map-value of the parameter is dependent
   * on the type
   * 
   * Current supported types:
   * 
   * Constant: Map a constant args[0], example:
   *           add("ExampleConstant", -1 , MAP_CONSTANT , 5);
   * 
   * Offset: Map a StdAttr.BitWidth + args[0] to the generic, example:
   *         add("ExampleOffset", -1 , MAP_OFFSET , 1);
   * 
   * Multiply: Map a StdAttr.BitWidth * args[0] to the generic, example:
   *           add("ExampleMultiply", -1 , MAP_OFFSET , 2);
   * 
   * Attribute_Option: Map an AttributeOption to the generic, requires 2 parameters, namely
   *                   (1) An Attribute<AttributeOption>, the selected attribute
   *                   (2) A Map from AttributeOption to Integer values
   *                   Example:
   *                   add("ExampleOption", -1, MAP_ATTRIBUTE_OPTION, Comparator.MODE_ATTR, 
   *                     new HashMap<AttributeOption, Integer>() {{ 
   *                       put(Comparator.UNSIGNED_OPTION, 0); 
   *                       put(Comparator.SIGNED_OPTION, 1); 
   *                     }}
   *                    );
   *
   * intAttribute: Map an Attribute<Integer> to the generic, example:
   *               add(HIGH_TICK_STR, HIGH_TICK_ID, HDLParameters.MAP_INT_ATTRIBUTE, Clock.ATTR_HIGH)
   * 
   * gateinputbubble: special case onl for the standard gates, see AbtractGateHDLGenerator for details.
   * 
   * @param name Name used for the parameter
   * @param id Identifier of the parameter (must be negative)
   * @param type Type of the map value
   * @param args Arguments required for the type
   */
  public HDLParameters add(String name, int id, int type, Object... args) {
    myParameters.add(new ParameterInfo(name, id, type, args));
    return this;
  }

  /**
   * Constructs a conditional module parameter where the map-value of the parameter is the Value stored in
   * the attribute StdAttr.BitWidth. This parameter is only used if the StdAttr.BitWidth > 1 
   * 
   * @param name Name used for the parameter
   * @param id Identifier of the parameter (must be negative)
   */
  public HDLParameters addBusOnly(String name, int id) {
    myParameters.add(new ParameterInfo(true, name, id));
    return this;
  }

  /**
   * Constructs a conditional module parameter where the map-value of the parameter is the Value stored in
   * the attribute checkAttr. This parameter is only used if the checkAttr > 1 
   * 
   * @param name Name used for the parameter
   * @param id Identifier of the parameter (must be negative)
   */
  public HDLParameters addBusOnly(Attribute<BitWidth> checkAttr, String name, int id) {
    myParameters.add(new ParameterInfo(true, checkAttr, name, id));
    return this;
  }

  public boolean containsKey(int id, AttributeSet attrs) {
    for (var parameter : myParameters) 
      if (id == parameter.getParameterId(attrs)) return true;
    return false;
  }

  public String get(int id, AttributeSet attrs) {
    for (var parameter : myParameters) 
      if (id == parameter.getParameterId(attrs)) return parameter.getParameterString(attrs);
    return null;
  }

  public Map<String, Integer> getMaps(AttributeSet attrs) {
    final var contents = new TreeMap<String, Integer>();
    for (var parameter : myParameters) {
      if (parameter.isUsed(attrs)) {
        final var value = parameter.getParameterValue(attrs);
        if (value >= 0)
          contents.put(parameter.getParameterString(attrs), value);
      }
    }
    return contents;
  }

  public boolean isEmpty(AttributeSet attrs) {
    var count = 0;
    for (var parameter : myParameters)
      if (parameter.isUsed(attrs)) count++;
    return count == 0;
  }

  public List<Integer> keySet(AttributeSet attrs) {
    final var keySet = new ArrayList<Integer>();
    for (var parameter : myParameters) {
      if (parameter.isUsed(attrs)) keySet.add(parameter.getParameterId(attrs));
    }
    return keySet;
  }
}
