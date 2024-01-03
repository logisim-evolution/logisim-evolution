/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.Port;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class VhdlParser {

  private static class Scanner {
    String input;
    MatchResult m;

    Scanner(String input) {
      this.input = input;
    }

    boolean next(Pattern pat) {
      m = null;
      final var match = pat.matcher(input);
      if (!match.lookingAt()) return false;
      m = match;
      input = match.hitEnd()
              ? ""
              : input.substring(m.end());
      return true;
    }

    MatchResult match() {
      return m;
    }

    String remaining() {
      return input;
    }
  }

  public static class IllegalVhdlContentException extends Exception {

    private static final long serialVersionUID = 1L;

    public IllegalVhdlContentException() {
      super();
    }

    public IllegalVhdlContentException(String message) {
      super(message);
    }

    public IllegalVhdlContentException(String message, Throwable cause) {
      super(message, cause);
    }

    public IllegalVhdlContentException(Throwable cause) {
      super(cause);
    }
  }

  public static class PortDescription {

    private final String name;
    private final String type;
    private final BitWidth width;

    public PortDescription(String name, String type, int width) {
      this.name = name;
      this.type = type;
      this.width = BitWidth.create(width);
    }

    public String getName() {
      return this.name;
    }

    public String getType() {
      return this.type;
    }

    public String getVhdlType() {
      return switch (type) {
        case Port.INPUT -> "in";
        case Port.OUTPUT -> "out";
        case Port.INOUT -> "inout";
        default -> throw new IllegalArgumentException("Not recognized port type: " + type);
      };
    }

    public BitWidth getWidth() {
      return this.width;
    }
  }

  public static class GenericDescription {

    protected final String name;
    protected final String type;
    protected final int dval;

    public GenericDescription(String name, String type, int dval) {
      this.name = name;
      this.type = type;
      this.dval = dval;
    }

    public GenericDescription(String name, String type) {
      this.name = name;
      this.type = type;
      if (type.equals("positive")) dval = 1;
      else dval = 0;
    }

    public String getName() {
      return this.name;
    }

    public String getType() {
      return this.type;
    }

    public int getDefaultValue() {
      return this.dval;
    }
  }

  private static Pattern regex(String pattern) {
    pattern = pattern.trim();
    pattern = "^ " + pattern;
    pattern = pattern.replaceAll(" {2}", "\\\\s+"); // Two spaces = required whitespace
    pattern = pattern.replaceAll(" ", "\\\\s*"); // One space = optional whitespace
    return Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  }

  private static final Pattern LIBRARY = regex("library  \\w+ ;");
  private static final Pattern USING = regex("use  \\S+ ;");
  private static final Pattern ENTITY = regex("entity  (\\w+)  is");
  private static final Pattern END_KEYWORD = regex("end  (\\w+) ;");
  private static final Pattern END_ENTITY = regex("end entity  (\\w+) ;");
  private static final Pattern END = regex("end;");
  private static final Pattern ARCHITECTURE = regex("architecture .*");

  private static final Pattern SEMICOLON = regex(";");
  private static final Pattern OPENLIST = regex("[(]");
  private static final Pattern DONELIST = regex("[)] ;");

  private static final Pattern PORTS = regex("port");
  private static final Pattern PORT = regex("(\\w+(?: , \\w+)*) : (\\w+)  (\\w+)");
  private static final Pattern RANGE = regex("[(] (\\d+) downto (\\d+) [)]");

  private static final Pattern GENERICS = regex("generic");
  private static final Pattern GENERIC = regex("(\\w+(?: , \\w+)*) : (\\w+)");
  private static final Pattern DVALUE = regex(":= (\\w+)");
  private static final Pattern UNIT = regex("(\\w+)");

  private final List<PortDescription> inputs;
  private final List<PortDescription> outputs;
  private final List<GenericDescription> generics;
  private final String source;
  private String name;
  private String libraries;
  private String architecture;

  public VhdlParser(String source) {
    this.source = source;
    this.inputs = new ArrayList<>();
    this.outputs = new ArrayList<>();
    this.generics = new ArrayList<>();
  }

  public String getArchitecture() {
    return architecture;
  }

  private int getEOLIndex(String input, int from) {
    int index;

    index = input.indexOf("\n", from);
    if (index != -1) return index;

    index = input.indexOf("\r\n", from);
    if (index != -1) return index;

    index = input.indexOf("\r", from);
    if (index != -1) return index;

    return input.length();
  }

  public List<PortDescription> getInputs() {
    return inputs;
  }

  public List<PortDescription> getOutputs() {
    return outputs;
  }

  public List<GenericDescription> getGenerics() {
    return generics;
  }

  public String getLibraries() {
    return libraries;
  }

  public String getName() {
    return name;
  }

  private String getPortType(String type) throws IllegalVhdlContentException {
    if ("in".equalsIgnoreCase(type)) return Port.INPUT;
    if ("out".equalsIgnoreCase(type)) return Port.OUTPUT;
    if ("input".equalsIgnoreCase(type)) return Port.INOUT;

    throw new IllegalVhdlContentException(S.get("invalidTypeException") + ": " + type);
  }

  public void parse() throws IllegalVhdlContentException {
    final var input = new Scanner(removeComments());
    parseLibraries(input);
    if (!input.next(ENTITY)) throw new IllegalVhdlContentException(S.get("CannotFindEntityException"));
    name = input.match().group(1);
    while (parsePorts(input) || parseGenerics(input)) ;
    final var justEndForEntity = input.next(END);
    if ((!input.next(END_KEYWORD) && !input.next(END_ENTITY) && !justEndForEntity)
        || (!justEndForEntity && !input.match().group(1).equals(name))) throw new IllegalVhdlContentException(S.get("CannotFindEntityException"));
    parseArchitecture(input);
    if (input.remaining().length() > 0) throw new IllegalVhdlContentException(S.get("CannotFindEntityException"));
  }

  private void parseArchitecture(Scanner input) {
    if (input.next(ARCHITECTURE)) architecture = input.match().group();
    else architecture = "";
  }

  private void parseLibraries(Scanner input) {
    final var result = new StringBuilder();
    while (input.next(LIBRARY) || input.next(USING)) {
      result.append(input.match().group().trim().replaceAll("\\s+", " "));
      result.append(System.getProperty("line.separator"));
    }

    libraries = result.toString();
  }

  private void parsePort(Scanner input) throws IllegalVhdlContentException {
    // Example: "name : IN std_logic"
    // Example: "name : OUT std_logic_vector(expr downto expr)"
    // Example: "name1, name2, name3 : IN std_logic"
    // Example: "name1, name2, name3 : OUT std_logic_vector(expr downto expr)"

    if (!input.next(PORT)) throw new IllegalVhdlContentException(S.get("portDeclarationException"));
    final var names = input.match().group(1).trim();
    final var ptype = getPortType(input.match().group(2).trim());
    final var type = input.match().group(3).trim();
    final var isOneBit = type.equalsIgnoreCase("std_logic");
    final var isBitVector = type.equalsIgnoreCase("std_logic_vector");
    if (!isOneBit && !isBitVector) throw new IllegalVhdlContentException(S.get("portTypeException", type));
    var width = 1;
    if (isBitVector) {
      if (!input.next(RANGE)) throw new IllegalVhdlContentException(S.get("portDeclarationException"));
      final var upper = Integer.parseInt(input.match().group(1));
      final var lower = Integer.parseInt(input.match().group(2));
      width = upper - lower + 1;
    }

    for (final var name : names.split("\\s*,\\s*")) {
      if (ptype.equals(Port.INPUT)) inputs.add(new PortDescription(name, ptype, width));
      else outputs.add(new PortDescription(name, ptype, width));
    }
  }

  private boolean parsePorts(Scanner input) throws IllegalVhdlContentException {
    // Example: "port ( decl ) ;"
    // Example: "port ( decl ; decl ; decl ) ;"
    if (!input.next(PORTS)) return false;
    if (!input.next(OPENLIST)) throw new IllegalVhdlContentException(S.get("portDeclarationException"));
    parsePort(input);
    while (input.next(SEMICOLON)) parsePort(input);
    if (!input.next(DONELIST)) throw new IllegalVhdlContentException(S.get("portDeclarationException") + " before " + input.remaining());
    return true;
  }

  private void parseGeneric(Scanner input) throws IllegalVhdlContentException {
    // Example: "name : integer"
    // Example: "name : integer := constant"
    // Example: "name1, name2, name3 : integer"
    if (!input.next(GENERIC))
      throw new IllegalVhdlContentException(S.get("genericDeclarationException"));
    final var names = input.match().group(1).trim();
    var type = input.match().group(2).trim();
    if (!type.equalsIgnoreCase("integer")
        && !type.equalsIgnoreCase("natural")
        && !type.equalsIgnoreCase("positive")
        && !type.equalsIgnoreCase("time")
      ) {
      throw new IllegalVhdlContentException(S.get("genericTypeException") + ": " + type);
    }
    type = type.toLowerCase();
    var dval = 0;
    if (type.equals("positive")) {
      dval = 1;
    }
    if (input.next(DVALUE)) {
      String s = input.match().group(1);
      try {
        dval = Integer.decode(s);
      } catch (NumberFormatException e) {
        throw new IllegalVhdlContentException(S.get("genericValueException") + ": " + s);
      }
      if (type.equals("natural") && dval < 0 || type.equals("positive") && dval < 1)
        throw new IllegalVhdlContentException(S.get("genericValueException") + ": " + dval);
    }
    if (type.equalsIgnoreCase("time")) {
      if (input.next(UNIT)) {
        String s = input.match().group(1);
        if (s.equals("fs")) {
          // default base unit, femtoseconds
        }
        else if (s.equals("ps")) {
          dval *= 1000;
        }
        else if (s.equals("ns")) {
          dval *= 1000000;
        }
        else if (s.equals("us")) {
          dval *= 1000000000;
        }
        // these will overflow unless dval type is changed from int to long or larger
        // else if (s.equals("ms")) {
        //   dval *= 1000000000000;
        // }
        // else if (s.equals("sec")) {
        //   dval *= 1000000000000000;
        // }
        // else if (s.equals("min")) {
        //   dval *= 60000000000000000;
        // }
        // else if (s.equals("hr")) {
        //   dval *= 3600000000000000000;
        // }
        else {
          throw new IllegalVhdlContentException("Unrecognized time unit: " + dval);
        }
      }
    }

    for (final var name : names.split("\\s*,\\s*")) {
      generics.add(new GenericDescription(name, type, dval));
    }
  }

  private boolean parseGenerics(Scanner input) throws IllegalVhdlContentException {
    // Example: generic ( decl ) ;
    // Example: generic ( decl ; decl ; decl ) ;
    if (!input.next(GENERICS)) return false;
    if (!input.next(OPENLIST))
      throw new IllegalVhdlContentException(S.get("genericDeclarationException"));
    parseGeneric(input);
    while (input.next(SEMICOLON)) parseGeneric(input);
    if (!input.next(DONELIST))
      throw new IllegalVhdlContentException(S.get("genericDeclarationException"));
    return true;
  }

  private String removeComments() throws IllegalVhdlContentException {
    StringBuilder input;
    try {
      input = new StringBuilder(source);
    } catch (NullPointerException ex) {
      throw new IllegalVhdlContentException(S.get("emptySourceException"));
    }

    int from;
    while ((from = input.indexOf("--")) != -1) {
      int to = getEOLIndex(input.toString(), from);
      input.delete(from, to);
    }

    return input.toString().trim();
  }
}
