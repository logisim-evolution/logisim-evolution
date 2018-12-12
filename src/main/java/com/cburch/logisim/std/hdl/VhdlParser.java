/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.std.hdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.Port;

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

	public static class PortDescription {

		private String name;
		private String type;
		private BitWidth width;

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

		public BitWidth getWidth() {
			return this.width;
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

	private List<PortDescription> inputs;
	private List<PortDescription> outputs;
	private String source;
	private String name;
	private String libraries;
	private String architecture;

	public VhdlParser(String source) {
		this.source = source;
		this.inputs = new ArrayList<PortDescription>();
		this.outputs = new ArrayList<PortDescription>();
	}

	public String getArchitecture() {
		return architecture;
	}

	private int getEOLIndex(String input, int from) {
		int index;

		index = input.indexOf("\n", from);
		if (index != -1)
			return index;

		index = input.indexOf("\r\n", from);
		if (index != -1)
			return index;

		index = input.indexOf("\r", from);
		if (index != -1)
			return index;

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
		if (type.equals("in"))
			return Port.INPUT;
		if (type.equals("out"))
			return Port.OUTPUT;
		if (type.equals("inout"))
			return Port.INOUT;

		throw new IllegalVhdlContentException(
				Strings.get("invalidTypeException"));
	}

	public void parse() throws IllegalVhdlContentException {
		String input = removeComments();
		Pattern pattern = Pattern.compile(ENTITY_PATTERN, Pattern.DOTALL
				| Pattern.CASE_INSENSITIVE);

		String[] parts = pattern.split(input);
		Matcher matcher = pattern.matcher(input);

		if (parts.length > 2) {
			throw new IllegalVhdlContentException(
					Strings.get("duplicatedEntityException"));
		}
		if (!matcher.find() || matcher.groupCount() != 3
				|| !matcher.group(1).equals(matcher.group(3))) {
			throw new IllegalVhdlContentException(
					Strings.get("CannotFindEntityException"));
		}

		name = matcher.group(1);
		parsePorts(matcher.group(2));

		parseLibraries(parts[0]);
		parseContent(parts.length == 2 ? parts[1] : "");
	}

	private void parseContent(String input) throws IllegalVhdlContentException {
		Matcher matcher = Pattern.compile(ARCH_PATTERN,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(input);

		if (matcher.find()) {
			architecture = matcher.group().trim();
		} else {
			architecture = "";
		}
	}

	private void parseLibraries(String input)
			throws IllegalVhdlContentException {
		StringBuilder result = new StringBuilder();

		Matcher library = Pattern.compile(LIBRARY_PATTERN,
				Pattern.CASE_INSENSITIVE).matcher(input);
		while (library.find()) {
			result.append(library.group().trim().replaceAll("\\s+", " "));
			result.append(System.getProperty("line.separator"));
		}

		Matcher using = Pattern
				.compile(USING_PATTERN, Pattern.CASE_INSENSITIVE)
				.matcher(input);
		while (using.find()) {
			result.append(using.group().trim().replaceAll("\\s+", " "));
			result.append(System.getProperty("line.separator"));
		}

		libraries = result.toString();
	}

	private int parseLine(Scanner scanner, StringBuilder type)
			throws IllegalVhdlContentException {
		if (scanner.findWithinHorizon(
				Pattern.compile(LINE_PATTERN, Pattern.CASE_INSENSITIVE), 0) == null)
			throw new IllegalVhdlContentException(
					Strings.get("lineDeclarationException"));
		MatchResult result = scanner.match();

		if (result.groupCount() != 1)
			throw new IllegalVhdlContentException(
					Strings.get("lineDeclarationException"));
		type.append(getType(result.group(1).toLowerCase()));

		return 1;
	}

	private void parseMultiplePorts(String line)
			throws IllegalVhdlContentException {
		int index = line.indexOf(':');
		if (index == -1)
			throw new IllegalVhdlContentException(
					Strings.get("multiplePortsDeclarationException"));

		Scanner local = new Scanner(line.substring(0, index));
		local.useDelimiter(",");

		List<String> names = new ArrayList<String>();
		while (local.hasNext())
			names.add(local.next().trim());

		local.close();
		local = new Scanner(line);

		int width;
		StringBuilder type = new StringBuilder();
		if (line.toLowerCase().contains("std_logic_vector"))
			width = parseVector(local, type);
		else
			width = parseLine(local, type);

		for (String name : names) {
			if (type.toString().equals(Port.INPUT))
				inputs.add(new PortDescription(name, type.toString(), width));
			else
				outputs.add(new PortDescription(name, type.toString(), width));
		}

		local.close();
	}

	private void parsePort(String line) throws IllegalVhdlContentException {
		Scanner local = new Scanner(line);

		if (local.findWithinHorizon(
				Pattern.compile(PORT_PATTERN, Pattern.CASE_INSENSITIVE), 0) == null) {
			local.close();
			throw new IllegalVhdlContentException(
					Strings.get("portDeclarationException"));
		}
		String name = local.match().group().trim();

		int width;
		StringBuilder type = new StringBuilder();
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
		Matcher matcher = Pattern.compile(PORTS_PATTERN,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(input);
		if (!matcher.find() || matcher.groupCount() != 1)
			return;
		String ports = matcher.group(1);

		Scanner scanner = new Scanner(ports);
		scanner.useDelimiter(";");
		while (scanner.hasNext()) {
			String statement = scanner.next();
			if (statement.contains(","))
				parseMultiplePorts(statement.trim());
			else
				parsePort(statement.trim());
		}

		scanner.close();
	}

	private int parseVector(Scanner scanner, StringBuilder type)
			throws IllegalVhdlContentException {
		if (scanner.findWithinHorizon(
				Pattern.compile(VECTOR_PATTERN, Pattern.CASE_INSENSITIVE), 0) == null)
			throw new IllegalVhdlContentException(
					Strings.get("vectorDeclarationException"));
		MatchResult result = scanner.match();

		if (result.groupCount() != 3)
			throw new IllegalVhdlContentException(
					Strings.get("vectorDeclarationException"));
		type.append(getType(result.group(1).toLowerCase()));

		return Integer.parseInt(result.group(2))
				- Integer.parseInt(result.group(3)) + 1;
	}

	private String removeComments() throws IllegalVhdlContentException {
		StringBuffer input;
		try {
			input = new StringBuffer(source);
		} catch (NullPointerException ex) {
			throw new IllegalVhdlContentException(
					Strings.get("emptySourceException"));
		}

		int from;
		while ((from = input.indexOf("--")) != -1) {
			int to = getEOLIndex(input.toString(), from);
			input.delete(from, to);
		}

		return input.toString().trim();
	}

}
