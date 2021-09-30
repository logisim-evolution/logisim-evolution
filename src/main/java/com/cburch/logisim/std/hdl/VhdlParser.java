/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.Port;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class VhdlParser {

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

  // NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
  // getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`. We may change
  // that in future, but for now it looks stupid in this file only.
  public record PortDescription(String getName, String getType, int getWidthInt, BitWidth getWidth) {
    public PortDescription(String name, String type, int width) {
      this(name, type, width, BitWidth.create(width));
    }
  }

  private static final String ENTITY_PATTERN = "\\s*entity\\s+(\\w+)\\s+is\\s+(.*?end)\\s+(\\w+)\\s*;";
  private static final String ARCH_PATTERN = "\\s*architecture.*";
  private static final String LIBRARY_PATTERN = "\\s*library\\s+\\w+\\s*;";
  private static final String USING_PATTERN = "\\s*use\\s+\\S+\\s*;";

  private static final String PORTS_PATTERN = "\\s*port\\s*[(](.*)[)]\\s*;\\s*end";
  private static final String PORT_PATTERN = "\\s*(\\w+)\\s*";
  private static final String LINE_PATTERN = ":\\s*(\\w+)\\s+std_logic";
  private static final String VECTOR_PATTERN = ":\\s*(\\w+)\\s+std_logic_vector\\s*[(]\\s*(\\d+)\\s+downto\\s+(\\d+)\\s*[)]";

  private final List<PortDescription> inputs;
  private final List<PortDescription> outputs;
  private final String source;
  private String name;
  private String libraries;
  private String architecture;

  public VhdlParser(String source) {
    this.source = source;
    this.inputs = new ArrayList<>();
    this.outputs = new ArrayList<>();
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

  public String getLibraries() {
    return libraries;
  }

  public String getName() {
    return name;
  }

  public List<PortDescription> getOutputs() {
    return outputs;
  }

  private String getType(String type) throws IllegalVhdlContentException {
    if (type.equals("in")) return Port.INPUT;
    if (type.equals("out")) return Port.OUTPUT;
    if (type.equals("inout")) return Port.INOUT;

    throw new IllegalVhdlContentException(S.get("invalidTypeException"));
  }

  public void parse() throws IllegalVhdlContentException {
    final var input = removeComments();
    final var pattern = Pattern.compile(ENTITY_PATTERN, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    final var parts = pattern.split(input);
    final var matcher = pattern.matcher(input);

    if (parts.length > 2) {
      throw new IllegalVhdlContentException(S.get("duplicatedEntityException"));
    }
    if (!matcher.find()
        || matcher.groupCount() != 3
        || !matcher.group(1).equals(matcher.group(3))) {
      throw new IllegalVhdlContentException(S.get("CannotFindEntityException"));
    }

    name = matcher.group(1);
    parsePorts(matcher.group(2));

    parseLibraries(parts[0]);
    parseContent(parts.length == 2 ? parts[1] : "");
  }

  private void parseContent(String input) {
    final var matcher =
        Pattern.compile(ARCH_PATTERN, Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(input);

    if (matcher.find()) {
      architecture = matcher.group().trim();
    } else {
      architecture = "";
    }
  }

  private void parseLibraries(String input) {
    final var result = new StringBuilder();
    final var library = Pattern.compile(LIBRARY_PATTERN, Pattern.CASE_INSENSITIVE).matcher(input);
    while (library.find()) {
      result.append(library.group().trim().replaceAll("\\s+", " "));
      result.append(System.getProperty("line.separator"));
    }

    final var using = Pattern.compile(USING_PATTERN, Pattern.CASE_INSENSITIVE).matcher(input);
    while (using.find()) {
      result.append(using.group().trim().replaceAll("\\s+", " "));
      result.append(System.getProperty("line.separator"));
    }

    libraries = result.toString();
  }

  private int parseLine(Scanner scanner, StringBuilder type) throws IllegalVhdlContentException {
    if (scanner.findWithinHorizon(Pattern.compile(LINE_PATTERN, Pattern.CASE_INSENSITIVE), 0)
        == null) throw new IllegalVhdlContentException(S.get("lineDeclarationException"));
    MatchResult result = scanner.match();

    if (result.groupCount() != 1)
      throw new IllegalVhdlContentException(S.get("lineDeclarationException"));
    type.append(getType(result.group(1).toLowerCase()));

    return 1;
  }

  private void parseMultiplePorts(String line) throws IllegalVhdlContentException {
    final var index = line.indexOf(':');
    if (index == -1)
      throw new IllegalVhdlContentException(S.get("multiplePortsDeclarationException"));

    var local = new Scanner(line.substring(0, index));
    local.useDelimiter(",");

    final var names = new ArrayList<String>();
    while (local.hasNext()) names.add(local.next().trim());

    local.close();
    local = new Scanner(line);

    int width;
    final var type = new StringBuilder();
    if (line.toLowerCase().contains("std_logic_vector"))
      width = parseVector(local, type);
    else
      width = parseLine(local, type);

    for (final var name : names) {
      if (type.toString().equals(Port.INPUT))
        inputs.add(new PortDescription(name, type.toString(), width));
      else outputs.add(new PortDescription(name, type.toString(), width));
    }

    local.close();
  }

  private void parsePort(String line) throws IllegalVhdlContentException {
    final var local = new Scanner(line);

    if (local.findWithinHorizon(Pattern.compile(PORT_PATTERN, Pattern.CASE_INSENSITIVE), 0)
        == null) {
      local.close();
      throw new IllegalVhdlContentException(S.get("portDeclarationException"));
    }
    final var name = local.match().group().trim();

    int width;
    final var type = new StringBuilder();
    if (line.toLowerCase().contains("std_logic_vector"))
      width = parseVector(local, type);
    else
      width = parseLine(local, type);

    if (type.toString().equals(Port.INPUT))
      inputs.add(new PortDescription(name, type.toString(), width));
    else
      outputs.add(new PortDescription(name, type.toString(), width));

    local.close();
  }

  private void parsePorts(String input) throws IllegalVhdlContentException {
    final var matcher =
        Pattern.compile(PORTS_PATTERN, Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(input);
    if (!matcher.find() || matcher.groupCount() != 1) return;
    final var ports = matcher.group(1);

    final var scanner = new Scanner(ports);
    scanner.useDelimiter(";");
    while (scanner.hasNext()) {
      final var statement = scanner.next();
      if (statement.contains(",")) parseMultiplePorts(statement.trim());
      else parsePort(statement.trim());
    }

    scanner.close();
  }

  private int parseVector(Scanner scanner, StringBuilder type) throws IllegalVhdlContentException {
    if (scanner.findWithinHorizon(Pattern.compile(VECTOR_PATTERN, Pattern.CASE_INSENSITIVE), 0)
        == null) throw new IllegalVhdlContentException(S.get("vectorDeclarationException"));
    final var result = scanner.match();

    if (result.groupCount() != 3)
      throw new IllegalVhdlContentException(S.get("vectorDeclarationException"));
    type.append(getType(result.group(1).toLowerCase()));

    return Integer.parseInt(result.group(2)) - Integer.parseInt(result.group(3)) + 1;
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
