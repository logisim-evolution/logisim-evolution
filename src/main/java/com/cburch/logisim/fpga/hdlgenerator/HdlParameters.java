/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.gates.GateAttributes;
import com.cburch.logisim.std.gates.NegateAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HdlParameters {

  public static final int MAP_DEFAULT = 0;
  public static final int MAP_CONSTANT = 1;
  public static final int MAP_OFFSET = 2;
  public static final int MAP_MULTIPLY = 3;
  public static final int MAP_ATTRIBUTE_OPTION = 4;
  public static final int MAP_LN2 = 5;
  public static final int MAP_INT_ATTRIBUTE = 6;
  public static final int MAP_GATE_INPUT_BUBLE = 7;
  public static final int MAP_POW2 = 8;

  private static class ParameterInfo {
    private final boolean isOnlyUsedForBusses;
    private boolean isIntParameter = true;
    private final String parameterName;
    private final int parameterId;
    private int myMapType = MAP_DEFAULT;
    private long parameterValue = -1;
    private long multiplyValue = 1;
    private long offsetValue = 0;
    private final List<Attribute<?>> attributesList = new ArrayList<>();
    private Map<AttributeOption, Integer> attributeOptionMap = new HashMap<>();
    private final Attribute<BitWidth> attributeToCheckForBus;

    public ParameterInfo(String name, int id) {
      this(false, StdAttr.WIDTH, name, id);
    }

    @SuppressWarnings("unchecked")
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
          if (args.length == 2) {
            for (var arg = 0; arg < 2; arg++) {
              if (!(args[arg] instanceof Attribute<?> attr))
                throw new IllegalArgumentException("Mutliply map Type: argument needs to be an Attribute<?>");
              attributesList.add(attr);
            }
          } else {
            multiplyValue = getCorrectIntValue(args);
            if (multiplyValue == 0) throw new NumberFormatException("multiply value cannot be zero");
          }
          break;

        case MAP_ATTRIBUTE_OPTION:
          if (args.length != 2) throw new IllegalArgumentException("Attribute map Type requires 2 argument");
          if (!(args[0] instanceof Attribute<?> arg0)) throw new IllegalArgumentException("Attribute map Type: first argument needs to be an Attribute<?>");
          if (!(args[1] instanceof Map<?, ?> map)) throw new IllegalArgumentException("Attribute map Type: second argument needs to be an Map<?,?>");
          if (map.isEmpty()) throw new IllegalArgumentException("Attribute map Type: Map<?,?> cannot be empty");
          for (final var key : map.keySet()) {
            if (!(key instanceof AttributeOption)) throw new IllegalArgumentException("Attribute map Type: Map<?,?> keys need to be an AttributeOption");
            if (!(map.get(key) instanceof Integer)) throw new IllegalArgumentException("Attribute map Type: Map<?,?> value need to be an Integer");
          }
          attributesList.add(arg0);
          attributeOptionMap = (Map<AttributeOption, Integer>) args[1];
          break;

        case MAP_INT_ATTRIBUTE:
          if (args.length == 2) {
            if (args[1] instanceof Integer) {
              offsetValue = (int) args[1];
              if (offsetValue < 0) throw new NumberFormatException("Integer value must be positive");
            } else throw new IllegalArgumentException("Attribute map Type requires only 1 argument");
          } else if (args.length != 1) throw new IllegalArgumentException("Attribute map Type requires only 1 argument");
          attributesList.add((Attribute<?>) args[0]);
          break;

        case MAP_POW2:
        case MAP_LN2:
          for (final var arg : args) {
            if (arg instanceof Integer) {
              offsetValue = (int) arg;
              if (offsetValue < 0) throw new NumberFormatException("Integer value must be positive");
              continue;
            }
            if (!(arg instanceof Attribute<?>)) throw new IllegalArgumentException("Bits required map Type: argument needs to be an Attribute<?>");
            attributesList.add((Attribute<?>) arg);
          }
          break;

        default:
          return;
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
      return !isOnlyUsedForBusses || (nrOfBits > 1);
    }

    public int getParameterId(AttributeSet attrs) {
      return isUsed(attrs) ? parameterId : 0;
    }

    public String getParameterString(AttributeSet attrs) {
      return isUsed(attrs) ? parameterName : null;
    }

    public String getParameterValue(AttributeSet attrs) {
      var totalValue = 0L;
      var selectedValue = 0L;

      final var noReqAttrExMsg = "Component has not the required attribute";
      final var notAnIntExMsg = "Requested attribute is not an Integer";

      switch (myMapType) {
        case MAP_CONSTANT:
          selectedValue = parameterValue;
          break;

        case MAP_ATTRIBUTE_OPTION:
          if (!attrs.containsAttribute(attributesList.get(0))) throw new UnsupportedOperationException(noReqAttrExMsg);
          final var value = attrs.getValue(attributesList.get(0));
          if (!(value instanceof AttributeOption)) throw new UnsupportedOperationException("Requested attribute is not an attributeOption");
          if (!attributeOptionMap.containsKey(value)) throw new UnsupportedOperationException("Map does not contain the requested attributeOption");
          selectedValue = attributeOptionMap.get(value);
          break;

        case MAP_POW2:
          for (var attr : attributesList) {
            if (!attrs.containsAttribute(attr)) throw new UnsupportedOperationException(noReqAttrExMsg);
            final var intValue = attrs.getValue(attr);
            if (intValue instanceof Integer) {
              totalValue += (int) intValue;
            } else if (intValue instanceof BitWidth bitWidth) {
              totalValue += bitWidth.getWidth();
            } else throw new UnsupportedOperationException(notAnIntExMsg);
          }
          selectedValue = (long) Math.pow(2D, totalValue);
          break;

        case MAP_LN2:
          for (var attr : attributesList) {
            if (!attrs.containsAttribute(attr)) throw new UnsupportedOperationException(noReqAttrExMsg);
            final var intValue = attrs.getValue(attr);
            if (intValue instanceof Integer) {
              totalValue += (int) intValue;
            } else if (intValue instanceof BitWidth bitWidth) {
              totalValue += bitWidth.getWidth();
            } else throw new UnsupportedOperationException(notAnIntExMsg);
          }
          final var logValue = Math.log(totalValue) / Math.log(2D);
          selectedValue = (long) Math.ceil(logValue) + offsetValue;
          break;

        case MAP_INT_ATTRIBUTE:
          if (!attrs.containsAttribute(attributesList.get(0))) throw new UnsupportedOperationException(noReqAttrExMsg);
          final var intValue = attrs.getValue(attributesList.get(0));
          if (intValue instanceof Integer) selectedValue = (int) intValue + offsetValue;
          else if (intValue instanceof Long) selectedValue = (long) intValue + offsetValue;
          else if (intValue instanceof BitWidth bitWidth) selectedValue = bitWidth.getWidth() + offsetValue;
          else throw new UnsupportedOperationException(notAnIntExMsg);
          break;

        case MAP_GATE_INPUT_BUBLE:
          if (!attrs.containsAttribute(GateAttributes.ATTR_INPUTS)) throw new UnsupportedOperationException(noReqAttrExMsg);
          final var nrOfInputs = attrs.getValue(GateAttributes.ATTR_INPUTS);
          var bubbleMask = 0L;
          var mask = 1L;
          for (var i = 0; i < nrOfInputs; i++) {
            // VHDL is particular with the general type std_logic_vector, as it does an upto, so we have to exchange the bits
            final var realIndex = Hdl.isVhdl() ? nrOfInputs - i - 1 : i;   
            final var inputIsInverted = attrs.getValue(new NegateAttribute(realIndex, null));
            if (Boolean.TRUE.equals(inputIsInverted)) bubbleMask |= mask;
            mask <<= 1L;
          }
          selectedValue = bubbleMask;
          break;

        case MAP_MULTIPLY:
          if (attributesList.isEmpty()) {
            selectedValue = attrs.getValue(attributeToCheckForBus).getWidth() * multiplyValue + offsetValue;
          } else {
            selectedValue = 1;

            for (final var attr : attributesList) {
              if (attrs.containsAttribute(attr)) {
                final var attrValue = attrs.getValue(attr);
                if (attrValue instanceof Integer intVal) selectedValue *= intVal;
                else if (attrValue instanceof Long longVal) selectedValue *= longVal;
                else if (attrValue instanceof BitWidth width) selectedValue *= width.getWidth();
                else throw new UnsupportedOperationException("Requested attribute is not an Integer or Long");
              }
            }
          }
          break;

        default:
          selectedValue = attrs.getValue(attributeToCheckForBus).getWidth() * multiplyValue + offsetValue;
          break;
      }

      return isIntParameter
        ? Integer.toString((int) selectedValue)
        : Hdl.getConstantVector(selectedValue, getNumberOfVectorBits(attrs));
    }

    public boolean isRepresentedByInteger() {
      return isIntParameter;
    }

    public void setVectorRepresentation() {
      isIntParameter = false;
    }

    public int getNumberOfVectorBits(AttributeSet attrs) {
      if (isIntParameter) throw new UnsupportedOperationException("Parameter is not a bit vector!");
      var nrOfVectorBits = -1;
      if (myMapType == MAP_GATE_INPUT_BUBLE) {
        if (!attrs.containsAttribute(GateAttributes.ATTR_INPUTS)) throw new UnsupportedOperationException("Component has not the required attribute");
        nrOfVectorBits = attrs.getValue(GateAttributes.ATTR_INPUTS);
      }
      if (offsetValue > 0) nrOfVectorBits = (int) offsetValue;
      if (nrOfVectorBits < 0) {
        if (attrs.containsAttribute(attributeToCheckForBus)) {
          nrOfVectorBits = attrs.getValue(attributeToCheckForBus).getWidth();
        } else {
          throw new UnsupportedOperationException("Cannot determine the number of bits required for the vector");
        }
      }
      return nrOfVectorBits;
    }

    private long getCorrectIntValue(Object... args) {
      if (args.length != 1) throw new IllegalArgumentException("Map Type requires a single argument");
      var value = 0L;
      if (args[0] instanceof Integer intVal) value = intVal;
      else if (args[0] instanceof Long longVal) value = longVal;
      else throw new IllegalArgumentException("Map Type requires an Integer or Long");
      if (value < 0) throw new NumberFormatException("Integer/long value must not be negative");
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
  public HdlParameters add(String name, int id) {
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
   *                   (1) An Attribute (AttributeOption), the selected attribute
   *                   (2) A Map from AttributeOption to Integer values
   *
   * ln2: Map the log base 2 value of the addition of all args to the generic, example:
   *      add("exampleln2", -1, MAP_LN2, Clock.ATTR_HIGH, Clock.ATTR_LOW)
   *
   * intAttribute: Map an Attribute (Integer) to the generic, example:
   *               add(HIGH_TICK_STR, HIGH_TICK_ID, MAP_INT_ATTRIBUTE, Clock.ATTR_HIGH)
   *
   * gateinputbubble: special case only for the standard gates, see AbtractGateHDLGenerator for details.
   *
   * @param name Name used for the parameter
   * @param id Identifier of the parameter (must be negative)
   * @param type Type of the map value
   * @param args Arguments required for the type
   */
  public HdlParameters add(String name, int id, int type, Object... args) {
    myParameters.add(new ParameterInfo(name, id, type, args));
    return this;
  }

  /**
   * Constructs a module parameter where the map-value of the parameter is dependent
   * on the type; furthermore this parameter is represented by a std_logic_vector
   * instead of an Integer (for VHDL only and for values with more than 32 bits)
   *
   * @param name Name used for the parameter
   * @param id Identifier of the parameter (must be negative)
   * @param type Type of the map value
   * @param args Arguments required for the type
   */
  public HdlParameters addVector(String name, int id, int type, Object... args) {
    final var newParameter = new ParameterInfo(name, id, type, args);
    newParameter.setVectorRepresentation();
    myParameters.add(newParameter);
    return this;
  }

  /**
   * Constructs a conditional module parameter where the map-value of the parameter is the Value stored in
   * the attribute StdAttr.BitWidth. This parameter is only used if the StdAttr.BitWidth > 1
   *
   * @param name Name used for the parameter
   * @param id Identifier of the parameter (must be negative)
   */
  public HdlParameters addBusOnly(String name, int id) {
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
  public HdlParameters addBusOnly(Attribute<BitWidth> checkAttr, String name, int id) {
    myParameters.add(new ParameterInfo(true, checkAttr, name, id));
    return this;
  }

  public boolean containsKey(int id, AttributeSet attrs) {
    for (final var parameter : myParameters) {
      if (id == parameter.getParameterId(attrs)) return true;
    }
    return false;
  }

  public String get(int id, AttributeSet attrs) {
    for (final var parameter : myParameters) {
      if (id == parameter.getParameterId(attrs)) return parameter.getParameterString(attrs);
    }
    return null;
  }

  public int getNumberOfVectorBits(int id, AttributeSet attrs) {
    for (final var parameter : myParameters) {
      if (id == parameter.getParameterId(attrs)) return parameter.getNumberOfVectorBits(attrs);
    }
    throw new UnsupportedOperationException("Parameter not found");
  }

  public boolean isPresentedByInteger(int id, AttributeSet attrs) {
    for (final var parameter : myParameters) {
      if (id == parameter.getParameterId(attrs)) return parameter.isRepresentedByInteger();
    }
    return true;
  }

  public Map<String, String> getMaps(AttributeSet attrs) {
    final var contents = new TreeMap<String, String>();
    for (final var parameter : myParameters) {
      if (parameter.isUsed(attrs)) {
        final var value = parameter.getParameterValue(attrs);
        if (!value.isEmpty()) contents.put(parameter.getParameterString(attrs), value);
      }
    }
    return contents;
  }

  public boolean isEmpty(AttributeSet attrs) {
    var count = 0;
    for (final var parameter : myParameters) {
      if (parameter.isUsed(attrs)) count++;
    }
    return count == 0;
  }

  public List<Integer> keySet(AttributeSet attrs) {
    final var keySet = new ArrayList<Integer>();
    for (final var parameter : myParameters) {
      if (parameter.isUsed(attrs)) keySet.add(parameter.getParameterId(attrs));
    }
    return keySet;
  }
}
