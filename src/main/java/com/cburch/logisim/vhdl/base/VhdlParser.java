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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VhdlParser {

  private static class Scanner {
    String input;
    MatchResult m;

    Scanner(String input) {
      this.input = input;
    }

    boolean next(Pattern p) {
      m = null;
      Matcher match = p.matcher(input);
      if (!match.lookingAt()) return false;
      m = match;
      if (match.hitEnd()) input = "";
      else input = input.substring(m.end());
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
      switch (type) {
        case Port.INPUT:
          return "in";
        case Port.OUTPUT:
          return "out";
        case Port.INOUT:
          return "inout";
        default:
          throw new IllegalArgumentException("Not recognized port type: " + type);
      }
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
  private static final Pattern END = regex("end  (\\w+) ;");
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
    if (type.equalsIgnoreCase("in")) return Port.INPUT;
    if (type.equalsIgnoreCase("out")) return Port.OUTPUT;
    if (type.equalsIgnoreCase("inout")) return Port.INOUT;

    throw new IllegalVhdlContentException(S.get("invalidTypeException") + ": " + type);
  }

  public void parse() throws IllegalVhdlContentException {
    Scanner input = new Scanner(removeComments());
    parseLibraries(input);
    if (!input.next(ENTITY))
      throw new IllegalVhdlContentException(S.get("CannotFindEntityException"));
    name = input.match().group(1);
    while (parsePorts(input) || parseGenerics(input)) ;
    if (!input.next(END)) throw new IllegalVhdlContentException(S.get("CannotFindEntityException"));
    if (!input.match().group(1).equals(name))
      throw new IllegalVhdlContentException(S.get("CannotFindEntityException"));
    parseArchitecture(input);
    if (input.remaining().length() > 0)
      throw new IllegalVhdlContentException(S.get("CannotFindEntityException"));
  }

  private void parseArchitecture(Scanner input) {
    if (input.next(ARCHITECTURE)) architecture = input.match().group();
    else architecture = "";
  }

  private void parseLibraries(Scanner input) {
    StringBuilder result = new StringBuilder();
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
    String names = input.match().group(1).trim();
    String ptype = getPortType(input.match().group(2).trim());
    String type = input.match().group(3).trim();

    int width;
    if (type.equalsIgnoreCase("std_logic")) {
      width = 1;
    } else {
      if (!input.next(RANGE))
        throw new IllegalVhdlContentException(S.get("portDeclarationException"));
      int upper = Integer.parseInt(input.match().group(1));
      int lower = Integer.parseInt(input.match().group(2));
      width = upper - lower + 1;
    }

    for (String name : names.split("\\s*,\\s*")) {
      if (ptype.equals(Port.INPUT)) inputs.add(new PortDescription(name, ptype, width));
      else outputs.add(new PortDescription(name, ptype, width));
    }
  }

  private boolean parsePorts(Scanner input) throws IllegalVhdlContentException {
    // Example: "port ( decl ) ;"
    // Example: "port ( decl ; decl ; decl ) ;"
    if (!input.next(PORTS)) return false;
    if (!input.next(OPENLIST))
      throw new IllegalVhdlContentException(S.get("portDeclarationException"));
    parsePort(input);
    while (input.next(SEMICOLON)) parsePort(input);
    if (!input.next(DONELIST))
      throw new IllegalVhdlContentException(S.get("portDeclarationException"));
    return true;
  }

  private void parseGeneric(Scanner input) throws IllegalVhdlContentException {
    // Example: "name : integer"
    // Example: "name : integer := constant"
    // Example: "name1, name2, name3 : integer"
    if (!input.next(GENERIC))
      throw new IllegalVhdlContentException(S.get("genericDeclarationException"));
    String names = input.match().group(1).trim();
    String type = input.match().group(2).trim();
    if (!type.equalsIgnoreCase("integer")
        && !type.equalsIgnoreCase("natural")
        && !type.equalsIgnoreCase("positive")) {
      throw new IllegalVhdlContentException(S.get("genericTypeException") + ": " + type);
    }
    type = type.toLowerCase();
    int dval = 0;
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

    for (String name : names.split("\\s*,\\s*")) {
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
